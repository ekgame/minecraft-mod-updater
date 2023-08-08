package lt.ekgame.modupdater

import com.charleskorn.kaml.Yaml
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.prompt
import com.github.ajalt.clikt.parameters.types.path
import kotlinx.coroutines.runBlocking
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.inputStream
import kotlin.io.path.isRegularFile
import kotlin.io.path.moveTo

class Hello : CliktCommand() {

    val mc: String by option(help = "The Minecraft version to update/downgrade mods for.")
        .prompt("Enter the minecraft version to update/downgrade mods to")

    val dir: Path by argument(help = "The path to the mods folder.")
        .path(canBeFile = false, canBeDir = true, mustExist = true)
        .default(Path.of("."))

    val api = ModrinthApi()

    lateinit var metaProvider: MetaResolver

    override fun run() {
        metaProvider = loadMetaSettings()
        runBlocking {
            ModProvider.scanDirectory(dir)
                .forEach { handleModFile(it) }
        }
    }

    fun loadMetaSettings(): MetaResolver {
        val nativeMetaStream = this::class.java.getResource(".mod-meta.yml")!!.openStream()
        val nativeMeta = Yaml.default.decodeFromStream(ModMetaDefinitionFile.serializer(), nativeMetaStream)

        val userMetaFile = dir.resolve(".mod-meta.yml")
        val userMeta = when {
            userMetaFile.isRegularFile() && userMetaFile.exists() -> {
                Yaml.default.decodeFromStream(ModMetaDefinitionFile.serializer(), userMetaFile.inputStream())
            }
            else -> null
        }

        return MetaResolver(nativeMeta, userMeta)
    }

    suspend fun handleModFile(file: ModFile) {
        when (file) {
            is ModFile.FabricModFile -> handleFabricModFile(file)
            is ModFile.InvalidModFile -> {
                echo("Invalid mod \"${file.path.fileName}\" (skipped) - ${file.reason}", err = true)
            }
        }
    }

    suspend fun handleFabricModFile(file: ModFile.FabricModFile) {
        echo("Updating fabric mod: ${file.name}")
        val mod = findOnlineMod(file)
        if (mod == null) {
            echo("Could not find online mod for ${file.name} (skipped)", err = true)
            return
        }
        val versions = api.getFabricModVersions(mod.slug, mcVersion = mc)
            .filter { it.versionType == ModrinthApi.VersionType.RELEASE }
            .filter { it.gameVersion.contains(mc) }

        val latestVersion = versions.firstOrNull() ?: run {
            echo("Could not find a stable version for ${file.name} (skipped)", err = true)
            return
        }

        if (latestVersion.files.count() > 1) {
            echo("Latest version of ${file.name} has multiple files online, we do not know what to do yet (skipped)", err = true)
            return
        }

        val latestFile = latestVersion.files.firstOrNull() ?: run {
            echo("Latest version of ${file.name} has no files online (skipped)", err = true)
            return
        }

        val currentHash = file.sha512
        val onlineHash = latestFile.hashes.sha512

        if (currentHash == onlineHash) {
            echo("Latest version of ${file.name} is already installed (skipped)", err = true)
            return
        }

        file.path.moveTo(file.path.resolveSibling("${file.path.fileName}.disabled"))
        val newPath = file.path.resolveSibling(latestFile.filename)
        echo("Downloading new version of ${file.name} to ${newPath.fileName}")
        api.downloadFile(latestFile.url, newPath)
        echo("Updated ${file.name}")
    }

    suspend fun findOnlineMod(file: ModFile.ValidModFile): ModrinthApi.Project? {
        val knownProjectId = metaProvider.resolveKnownModId(file.name)
        if (knownProjectId != null) {
            return api.getProject(knownProjectId)
        }

        val searchQuery = when (file.author) {
            null -> file.name
            else -> "${file.name} ${file.author}"
        }

        val mods = api.searchMods(searchQuery)
        return mods.hits.find { it.title == file.name }
    }
}

fun main(args: Array<String>) = Hello().main(args)