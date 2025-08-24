package me.darthorimar.rekot.projectStructure

import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import me.darthorimar.rekot.app.AppComponent
import me.darthorimar.rekot.projectStructure.index.IndexStorage
import org.jetbrains.kotlin.analysis.api.platform.packages.KotlinCachingPackageProviderFactory
import org.jetbrains.kotlin.analysis.api.platform.packages.KotlinPackageProvider
import org.jetbrains.kotlin.analysis.api.standalone.base.packages.KotlinStandalonePackageProviderFactory
import org.jetbrains.kotlin.psi.KtFile
import org.koin.core.component.get
import org.koin.core.component.inject
import java.util.concurrent.ConcurrentHashMap

class RekotPackageProviderFactory(private val project: Project) :
    KotlinCachingPackageProviderFactory(project), AppComponent {
    private val indexStorage: IndexStorage get()= get()

    private val cache = ConcurrentHashMap<GlobalSearchScope, KotlinPackageProvider>()

    private val delegate by lazy {
        KotlinStandalonePackageProviderFactory(
            project,
            indexStorage.indexes.flatMap { index ->
                index.stubTable.files.mapNotNull { fileStub -> fileStub.psi as? KtFile }
            },
        )
    }

    override fun createNewPackageProvider(searchScope: GlobalSearchScope): KotlinPackageProvider {
        return cache.getOrPut(searchScope) {
            delegate.createPackageProvider(searchScope)
        }
    }
}
