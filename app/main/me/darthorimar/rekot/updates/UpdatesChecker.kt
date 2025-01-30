package me.darthorimar.rekot.updates

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import me.darthorimar.rekot.app.App
import me.darthorimar.rekot.app.AppComponent
import me.darthorimar.rekot.app.SubscriptionContext
import me.darthorimar.rekot.app.subscribe
import me.darthorimar.rekot.config.APP_GITHUB_REPO
import me.darthorimar.rekot.config.APP_VERSION
import me.darthorimar.rekot.config.AppConfig
import me.darthorimar.rekot.events.Event
import me.darthorimar.rekot.logging.error
import me.darthorimar.rekot.logging.logger
import org.koin.core.component.inject
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.util.logging.Level
import kotlin.concurrent.thread
import kotlin.io.path.*

private val logger = logger<App>()

class UpdatesChecker : AppComponent {
    private val config: AppConfig by inject()

    context(SubscriptionContext)
    override fun performSubscriptions() {
        subscribe<Event.AppStarted> { scheduleUpdateCheck() }
    }

    private fun scheduleUpdateCheck() {
        if ((config.appDir / DO_NO_UPDATE_FILE).exists()) return
        thread(isDaemon = true) {
            try {
                val latestVersion = getLatestRelease() ?: return@thread
                if (latestVersion == APP_VERSION) return@thread
                if (versionIsIgnored(latestVersion)) return@thread
                markThatUpdateIsNeeded()
            } catch (e: Exception) {
                logger.error("Error while checking for app updates", e)
            }
        }
    }

    private fun markThatUpdateIsNeeded() {
        val updateFile = config.appDir / UPDATE_FILENAME
        if (!updateFile.exists()) {
            updateFile.createFile()
        }
    }

    private fun versionIsIgnored(version: String): Boolean {
        val ignoredVersionsFile = config.appDir / IGNORED_VERSIONS_FILENAME
        if (!ignoredVersionsFile.isRegularFile()) return false
        return ignoredVersionsFile.readLines().any { it.trim() == version }
    }

    private fun getLatestRelease(): String? {
        val url = "https://api.github.com/repos/$APP_GITHUB_REPO/releases/latest"
        val client = HttpClient.newHttpClient()
        val request =
            HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Accept", "application/vnd.github.v3+json")
                .GET()
                .build()

        val response = client.send(request, HttpResponse.BodyHandlers.ofString())
        if (response.statusCode() == 200) {
            val jsonResponse = Json.parseToJsonElement(response.body()).jsonObject
            return jsonResponse["tag_name"]?.jsonPrimitive?.content
        } else {
            logger.log(Level.WARNING, "Error while checking for app updates", response.statusCode())
            return null
        }
    }

    companion object {
        private const val IGNORED_VERSIONS_FILENAME = "IGNORED_VERSIONS"
        private const val UPDATE_FILENAME = "UPDATE"
        private const val DO_NO_UPDATE_FILE = "DO_NOT_UPDATE"
    }
}
