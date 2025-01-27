@file:Suppress("UnstableApiUsage", "INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")

package me.darthorimar.rekot.projectStructure

import com.intellij.core.CoreApplicationEnvironment
import com.intellij.mock.MockProject
import com.intellij.openapi.extensions.LoadingOrder
import com.intellij.openapi.util.Disposer
import com.intellij.psi.PsiTreeChangeAdapter
import com.intellij.psi.PsiTreeChangeListener
import com.intellij.psi.SmartPointerManager
import com.intellij.psi.SmartTypePointerManager
import com.intellij.psi.compiled.ClassFileDecompilers
import com.intellij.psi.impl.smartPointers.SmartPointerManagerImpl
import com.intellij.psi.impl.smartPointers.SmartTypePointerManagerImpl
import me.darthorimar.rekot.config.AppConfig
import org.jetbrains.kotlin.analysis.api.impl.base.permissions.KaBaseAnalysisPermissionRegistry
import org.jetbrains.kotlin.analysis.api.impl.base.util.LibraryUtils
import org.jetbrains.kotlin.analysis.api.permissions.KaAnalysisPermissionRegistry
import org.jetbrains.kotlin.analysis.api.platform.KotlinDeserializedDeclarationsOrigin
import org.jetbrains.kotlin.analysis.api.platform.KotlinMessageBusProvider
import org.jetbrains.kotlin.analysis.api.platform.KotlinPlatformSettings
import org.jetbrains.kotlin.analysis.api.platform.KotlinProjectMessageBusProvider
import org.jetbrains.kotlin.analysis.api.platform.declarations.KotlinAnnotationsResolverFactory
import org.jetbrains.kotlin.analysis.api.platform.declarations.KotlinDeclarationProviderFactory
import org.jetbrains.kotlin.analysis.api.platform.declarations.KotlinDeclarationProviderMerger
import org.jetbrains.kotlin.analysis.api.platform.lifetime.KotlinAlwaysAccessibleLifetimeTokenFactory
import org.jetbrains.kotlin.analysis.api.platform.lifetime.KotlinLifetimeTokenFactory
import org.jetbrains.kotlin.analysis.api.platform.modification.KotlinGlobalModificationService
import org.jetbrains.kotlin.analysis.api.platform.modification.KotlinModificationTrackerFactory
import org.jetbrains.kotlin.analysis.api.platform.packages.KotlinPackagePartProviderFactory
import org.jetbrains.kotlin.analysis.api.platform.packages.KotlinPackageProviderFactory
import org.jetbrains.kotlin.analysis.api.platform.permissions.KotlinAnalysisPermissionOptions
import org.jetbrains.kotlin.analysis.api.platform.projectStructure.KotlinByModulesResolutionScopeProvider
import org.jetbrains.kotlin.analysis.api.platform.projectStructure.KotlinModuleDependentsProvider
import org.jetbrains.kotlin.analysis.api.platform.projectStructure.KotlinProjectStructureProvider
import org.jetbrains.kotlin.analysis.api.platform.projectStructure.KotlinResolutionScopeProvider
import org.jetbrains.kotlin.analysis.api.resolve.extensions.KaResolveExtensionProvider
import org.jetbrains.kotlin.analysis.api.standalone.KotlinStaticPackagePartProviderFactory
import org.jetbrains.kotlin.analysis.api.standalone.base.declarations.KotlinStandaloneAnnotationsResolverFactory
import org.jetbrains.kotlin.analysis.api.standalone.base.declarations.KotlinStandaloneDeclarationProviderMerger
import org.jetbrains.kotlin.analysis.api.standalone.base.modification.KotlinStandaloneGlobalModificationService
import org.jetbrains.kotlin.analysis.api.standalone.base.modification.KotlinStandaloneModificationTrackerFactory
import org.jetbrains.kotlin.analysis.api.standalone.base.packages.KotlinStandalonePackageProviderFactory
import org.jetbrains.kotlin.analysis.api.standalone.base.permissions.KotlinStandaloneAnalysisPermissionOptions
import org.jetbrains.kotlin.analysis.api.standalone.base.projectStructure.FirStandaloneServiceRegistrar
import org.jetbrains.kotlin.analysis.api.standalone.base.projectStructure.KtStaticModuleDependentsProvider
import org.jetbrains.kotlin.analysis.api.standalone.base.projectStructure.StandaloneProjectFactory
import org.jetbrains.kotlin.analysis.decompiler.psi.KotlinBuiltInDecompiler
import org.jetbrains.kotlin.analysis.decompiler.psi.KotlinClassFileDecompiler
import org.jetbrains.kotlin.analysis.low.level.api.fir.providers.LLSealedInheritorsProvider
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreApplicationEnvironmentMode
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreProjectEnvironment
import org.jetbrains.kotlin.config.LanguageVersionSettingsImpl
import org.jetbrains.kotlin.fir.declarations.SealedClassInheritorsProvider
import org.jetbrains.kotlin.scripting.definitions.ScriptDefinitionProvider
import kotlin.io.path.absolutePathString
import kotlin.io.path.createDirectory
import kotlin.io.path.exists

// this class is a singleton as it initiates Application which is static itself
object ProjectStructureInitiator {
    fun initiateProjectStructure(appConfig: AppConfig): ProjectStructure {
        val key = "idea.home.path"
        if (System.getProperty(key) == null) {
            System.setProperty(
                key,
                appConfig.tmpDir
                    .resolve("ideaHomePath")
                    .also {
                        if (!it.exists()) {
                            it.createDirectory()
                        }
                    }
                    .absolutePathString(),
            )
        }

        val projectDisposable = Disposer.newDisposable("AnalysisAPI.project")
        val kotlinCoreProjectEnvironment =
            StandaloneProjectFactory.createProjectEnvironment(
                projectDisposable,
                KotlinCoreApplicationEnvironmentMode.Production,
            )

        val project: MockProject = kotlinCoreProjectEnvironment.project

        CoreApplicationEnvironment.registerExtensionPoint(
            project.extensionArea,
            KaResolveExtensionProvider.EP_NAME.name,
            KaResolveExtensionProvider::class.java,
        )

        KotlinCoreEnvironment.underApplicationLock {
            val applicationEnvironment = kotlinCoreProjectEnvironment.environment
            val application = applicationEnvironment.application
            if (application.getServiceIfCreated(KotlinAnalysisPermissionOptions::class.java) == null) {
                applicationEnvironment.registerApplicationService(
                    KotlinAnalysisPermissionOptions::class.java,
                    KotlinStandaloneAnalysisPermissionOptions(),
                )
            }

            if (application.getServiceIfCreated(KaAnalysisPermissionRegistry::class.java) == null) {
                applicationEnvironment.registerApplicationService(
                    KaAnalysisPermissionRegistry::class.java,
                    @Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER") KaBaseAnalysisPermissionRegistry(),
                )
            }

            ClassFileDecompilers.getInstance().EP_NAME.point.apply {
                registerExtension(
                    KotlinClassFileDecompiler(),
                    LoadingOrder.FIRST,
                    applicationEnvironment.parentDisposable,
                )
                registerExtension(
                    KotlinBuiltInDecompiler(),
                    LoadingOrder.FIRST,
                    applicationEnvironment.parentDisposable,
                )
            }
        }

        val essentialLibraries = createEssentialLibraries(kotlinCoreProjectEnvironment, appConfig)

        val projectStructureProvider = ProjectStructureProviderImpl()

        for (library in essentialLibraries.allLibraries) {
            for (file in library.files) {
                projectStructureProvider.setModule(file, library.kaModule)
            }
        }
        KotlinCoreEnvironment.registerProjectExtensionPoints(project.extensionArea)

        project.registerService(SmartTypePointerManager::class.java, SmartTypePointerManagerImpl::class.java)
        project.registerService(SmartPointerManager::class.java, SmartPointerManagerImpl::class.java)

        project.registerService(KotlinProjectStructureProvider::class.java, projectStructureProvider)
        project.registerService(
            KotlinModuleDependentsProvider::class.java,
            KtStaticModuleDependentsProvider(emptyList()),
        )

        val declarationFactory = ProjectDeclarationFactoryImpl()

        registerProjectServices(kotlinCoreProjectEnvironment, essentialLibraries, declarationFactory)

        CoreApplicationEnvironment.registerExtensionPoint(
            project.extensionArea,
            PsiTreeChangeListener.EP.name,
            PsiTreeChangeAdapter::class.java,
        )

        return ProjectStructure(
            kotlinCoreProjectEnvironment,
            essentialLibraries,
            Builtins(project),
            projectStructureProvider,
            projectDisposable,
        )
    }

    private fun createEssentialLibraries(
        kotlinCoreProjectEnvironment: KotlinCoreProjectEnvironment,
        appConfig: AppConfig,
    ): ProjectEssentialLibraries =
        ProjectEssentialLibraries(
            stdlib =
                EssentialLibrary.create(
                    listOf(appConfig.stdlibPath),
                    kotlinCoreProjectEnvironment,
                    "stdlib",
                    isSdk = false,
                ),
            jdk =
                EssentialLibrary.create(
                    buildList {
                        addAll(LibraryUtils.findClassesFromJdkHome(appConfig.javaHome, isJre = true))
                        addAll(LibraryUtils.findClassesFromJdkHome(appConfig.javaHome, isJre = false))
                    },
                    kotlinCoreProjectEnvironment,
                    "JDK",
                    isSdk = true,
                ),
        )

    private fun registerProjectServices(
        kotlinCoreProjectEnvironment: KotlinCoreProjectEnvironment,
        essentialLibraries: ProjectEssentialLibraries,
        declarationFactory: ProjectDeclarationFactoryImpl,
    ) {
        val project = kotlinCoreProjectEnvironment.project
        project.apply {
            registerService(KotlinMessageBusProvider::class.java, KotlinProjectMessageBusProvider::class.java)
            FirStandaloneServiceRegistrar.registerProjectServices(project)
            FirStandaloneServiceRegistrar.registerProjectExtensionPoints(project)
            FirStandaloneServiceRegistrar.registerProjectModelServices(
                project,
                kotlinCoreProjectEnvironment.parentDisposable,
            )

            registerService(
                KotlinModificationTrackerFactory::class.java,
                KotlinStandaloneModificationTrackerFactory::class.java,
            )
            registerService(
                KotlinGlobalModificationService::class.java,
                KotlinStandaloneGlobalModificationService::class.java,
            )
            registerService(
                KotlinLifetimeTokenFactory::class.java,
                KotlinAlwaysAccessibleLifetimeTokenFactory::class.java,
            )

            registerService(
                KotlinAnnotationsResolverFactory::class.java,
                KotlinStandaloneAnnotationsResolverFactory(project, emptyList()),
            )
            registerService(
                KotlinResolutionScopeProvider::class.java,
                KotlinByModulesResolutionScopeProvider::class.java,
            )

            registerService(KotlinDeclarationProviderFactory::class.java, declarationFactory)
            registerService(ScriptDefinitionProvider::class.java, CellScriptDefinitionProvider(project))
            //        registerService(
            //            KotlinDirectInheritorsProvider::class.java,
            //            KotlinStandaloneFirDirectInheritorsProvider(this),
            //        )
            registerService(
                KotlinDeclarationProviderMerger::class.java,
                KotlinStandaloneDeclarationProviderMerger(this),
            )
            registerService(
                KotlinPackageProviderFactory::class.java,
                KotlinStandalonePackageProviderFactory(project, emptyList()), /*TODO ???*/
            )

            registerService(
                SealedClassInheritorsProvider::class.java,
                @Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER") LLSealedInheritorsProvider(project),
            )

            registerService(
                KotlinPackagePartProviderFactory::class.java,
                KotlinStaticPackagePartProviderFactory(
                    StandaloneProjectFactory.createPackagePartsProvider(
                        StandaloneProjectFactory.getAllBinaryRoots(
                            essentialLibraries.kaModules,
                            kotlinCoreProjectEnvironment,
                        ))),
            )

            StandaloneProjectFactory.initialiseVirtualFileFinderServices(
                kotlinCoreProjectEnvironment,
                essentialLibraries.kaModules,
                emptyList(),
                LanguageVersionSettingsImpl.DEFAULT,
                null,
            )

            registerService(
                KotlinPlatformSettings::class.java,
                object : KotlinPlatformSettings {
                    override val deserializedDeclarationsOrigin: KotlinDeserializedDeclarationsOrigin
                        get() = KotlinDeserializedDeclarationsOrigin.STUBS
                },
            )
        }
    }
}
