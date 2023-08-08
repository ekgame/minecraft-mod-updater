import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import java.math.BigInteger
import java.nio.file.FileVisitResult
import java.nio.file.Path
import java.security.MessageDigest
import java.util.zip.ZipFile
import kotlin.io.path.*

object ModProvider {
    private val JSON = Json { ignoreUnknownKeys = true }

    @OptIn(ExperimentalPathApi::class)
    fun scanDirectory(path: Path): List<ModFile> {
        require(path.isDirectory()) { "Path must be a directory, got ${path.toAbsolutePath()}." }
        val mods = mutableListOf<ModFile>()
        path.visitFileTree {
            onPreVisitDirectory { directory, attributes ->
                if (path == directory) {
                    FileVisitResult.CONTINUE
                }
                else {
                    FileVisitResult.SKIP_SUBTREE
                }
            }
            onVisitFile { file, attributes ->
                if (file.extension == "jar") {
                    mods.add(readModFile(file))
                }
                FileVisitResult.CONTINUE
            }
        }
        return mods
    }

    fun readModFile(path: Path): ModFile {
        if (!path.isRegularFile()) {
            return ModFile.InvalidModFile(path, "Path must be a file, got ${path.toAbsolutePath()}.")
        }

        try {
            ZipFile(path.toFile()).use { zip ->
                val fabricDefinitionFile = zip.getEntry("fabric.mod.json")
                if (fabricDefinitionFile != null) {
                     return readFabricModFile(path, zip)
                }

                // TODO: Possibly support more mode file formats?
                //       Forge seems non-trivial, because mod data is in class metadata.

                return ModFile.InvalidModFile(path, "Unsupported mod format.")
            }
        } catch (e: Exception) {
            return ModFile.InvalidModFile(path, e.message ?: "Unknown error.")
        }
    }

    @OptIn(ExperimentalSerializationApi::class)
    fun readFabricModFile(path: Path, file: ZipFile): ModFile {
        val definitionFile = file.getEntry("fabric.mod.json")
            ?: error("Mod file does not contain fabric.mod.json.")

        val definition = definitionFile
            .let { file.getInputStream(it) }
            .use { JSON.decodeFromStream<FabricDefinitionFile>(it) }

        return ModFile.FabricModFile(
            path = path,
            id = definition.id,
            name = definition.name,
            author = definition.authors.firstOrNull(),
            version = definition.version,
            sha512 = path.sha512(),
        )
    }
}