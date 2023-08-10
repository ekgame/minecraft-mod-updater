package lt.ekgame.modupdater

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.nio.file.Path

class ModrinthApi {
    private val client = HttpClient(CIO) {
        defaultRequest {
            url("https://api.modrinth.com/v2/")
        }
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                coerceInputValues = true
            })
        }
        install(UserAgent) {
            agent = "ekgame/minecraft-mod-updater (ekgame1@gmail.com)"
        }
    }

    suspend fun downloadFile(url: String, target: Path) {
        client.get {
            url(url)
        }.body<ByteArray>().let { bytes ->
            target.toFile().writeBytes(bytes)
        }
    }

    suspend fun searchMods(query: String): ProjectSearchResult {
        return client.get("search") {
            parameter("query", query)
            parameter("facets", "[[\"project_type:mod\"]]")
        }.body()
    }

    suspend fun getProject(projectId: String): Project {
        return client.get("project/$projectId").body()
    }

    suspend fun getFabricModVersions(projectId: String, mcVersion: String): List<ProjectVersion> {
        return client.get("project/$projectId/version") {
            parameter("loaders", "[\"fabric\"]")
            parameter("facets", "[\"$mcVersion\"]")
        }.body()
    }

    @Serializable
    data class ProjectSearchResult(
        val hits: List<Project>,
        val offset: Int,
        val limit: Int,
        @SerialName("total_hits")
        val totalHits: Int,
    )

    @Serializable
    data class Project(
        val slug: String,
        val title: String,
        val description: String,
        val categories: List<String>,
        @SerialName("client_side")
        val clientSide: String,
        @SerialName("server_side")
        val serverSide: String,
        @SerialName("project_type")
        val projectType: String,
        val downloads: Int,
        val versions: List<String>,
    )

    @Serializable
    data class ProjectVersion(
        val id: String,
        @SerialName("version_number")
        val versionNumber: String,
        val files: List<ProjectVersionFile>,
        @SerialName("game_versions")
        val gameVersion: List<String>,
        val loaders: List<String>,
        @SerialName("version_type")
        val versionType: VersionType,
    )

    enum class VersionType {
        @SerialName("release")
        RELEASE,
        @SerialName("beta")
        BETA,
        @SerialName("alpha")
        ALPHA,
    }

    @Serializable
    data class ProjectVersionFile(
        val url: String,
        val filename: String,
        val hashes: ProjectVersionFileHashes,
    )

    @Serializable
    data class ProjectVersionFileHashes(
        val sha512: String,
        val sha1: String,
    )
}
