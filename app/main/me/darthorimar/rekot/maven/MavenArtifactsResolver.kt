package me.darthorimar.rekot.maven

import java.io.File
import java.io.FileNotFoundException
import java.net.URI
import java.nio.file.Path
import kotlinx.serialization.json.*
import org.apache.maven.repository.internal.MavenRepositorySystemUtils
import org.eclipse.aether.RepositorySystem
import org.eclipse.aether.artifact.Artifact
import org.eclipse.aether.artifact.DefaultArtifact
import org.eclipse.aether.collection.CollectRequest
import org.eclipse.aether.connector.basic.BasicRepositoryConnectorFactory
import org.eclipse.aether.graph.Dependency
import org.eclipse.aether.repository.LocalRepository
import org.eclipse.aether.repository.RemoteRepository
import org.eclipse.aether.resolution.DependencyRequest
import org.eclipse.aether.resolution.DependencyResult
import org.eclipse.aether.spi.connector.RepositoryConnectorFactory
import org.eclipse.aether.spi.connector.transport.TransporterFactory
import org.eclipse.aether.transport.http.HttpTransporterFactory

object MavenArtifactsResolver {
    fun resolveMavenArtifacts(
        coordinates: List<String>,
    ): List<Path> {
        val common = resolveOnlyMavenArtifacts(coordinates)
        val commonAndJvm =
            buildList {
                    for (maven in common) {
                        addAll(resolveGradleJvmVariant(maven))
                    }
                    for (maven in common) {
                        add("${maven.groupId}:${maven.artifactId}:${maven.version}")
                    }
                }
                .distinct()
        return resolveOnlyMavenArtifacts(commonAndJvm).map { it.file.toPath() }
    }

    private fun resolveGradleJvmVariant(artifact: Artifact): List<String> {
        val moduleUrl = buildString {
            append("https://repo1.maven.org/maven2/")
            append(artifact.groupId.replace('.', '/'))
            append("/")
            append(artifact.artifactId)
            append("/")
            append(artifact.version)
            append("/")
            append(artifact.artifactId)
            append("-")
            append(artifact.version)
            append(".module")
        }

        val json =
            try {
                URI(moduleUrl).toURL().openStream().bufferedReader().use { it.readText() }
            } catch (e: FileNotFoundException) {
                return emptyList()
            }

        val root = Json.parseToJsonElement(json).jsonObject
        val variants = root["variants"]?.jsonArray ?: return emptyList()

        val result = mutableListOf<String>()
        for (variant in variants) {
            val attrs = variant.jsonObject["attributes"]?.jsonObject ?: continue
            if (attrs["org.jetbrains.kotlin.platform.type"]?.jsonPrimitive?.content == "jvm") {
                val jvmCoordinates = variant.jsonObject["available-at"] as? JsonObject ?: continue
                result += buildString {
                    append(jvmCoordinates["group"]?.jsonPrimitive?.content ?: continue)
                    append(":")
                    append(jvmCoordinates["module"]?.jsonPrimitive?.content ?: continue)
                    append(":")
                    append(jvmCoordinates["version"]?.jsonPrimitive?.content ?: continue)
                }
            }
        }

        return result
    }

    private fun resolveOnlyMavenArtifacts(
        coordinates: List<String>,
    ): List<Artifact> {
        val locator =
            MavenRepositorySystemUtils.newServiceLocator().apply {
                addService(RepositoryConnectorFactory::class.java, BasicRepositoryConnectorFactory::class.java)
                addService(TransporterFactory::class.java, HttpTransporterFactory::class.java)
            }

        val system = locator.getService(RepositorySystem::class.java)
        val session = MavenRepositorySystemUtils.newSession()

        val localRepo = LocalRepository(File(System.getProperty("user.home"), ".m2/repository"))
        session.localRepositoryManager = system.newLocalRepositoryManager(session, localRepo)

        val dependencies = coordinates.map { Dependency(DefaultArtifact(it), "runtime") }

        val repositories =
            listOf(
                RemoteRepository.Builder("central", "default", "https://repo1.maven.org/maven2/").build(),
            )

        val collectRequest = CollectRequest(null as Dependency?, dependencies, repositories)
        val dependencyRequest = DependencyRequest(collectRequest, null)

        val result: DependencyResult = system.resolveDependencies(session, dependencyRequest)

        return result.artifactResults.map { it.artifact }
    }
}
