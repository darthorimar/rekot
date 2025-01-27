package me.darthorimar.rekot.config.stdlib

import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.file.Path
import kotlin.io.path.div

internal object KotlinStdLibDownloader {
    fun download(toDir: Path): Path {
        println("Downloading kotlin stdlib...")
        val fileName = "kotlin-stdlib-$DEFAULT_VERSION.jar"
        val url = "https://repo1.maven.org/maven2/org/jetbrains/kotlin/kotlin-stdlib/$DEFAULT_VERSION/$fileName"

        val client = HttpClient.newHttpClient()
        val request = HttpRequest.newBuilder().uri(URI.create(url)).build()
        val file = toDir / fileName
        val response = client.send<Path?>(request, HttpResponse.BodyHandlers.ofFile(file))

        if (response.statusCode() == 200) {
            println("Downloaded to: $file")
            return file
        } else {
            error("Failed to download file. HTTP Status: " + response.statusCode())
        }
    }

    private const val DEFAULT_VERSION = "2.1.0"
}
