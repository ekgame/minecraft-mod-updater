package lt.ekgame.modupdater

import java.nio.file.Path

sealed class ModFile(
    open val path: Path,
) {
    interface ValidModFile {
        val id: String
        val name: String
        val author: String?
        val version: String
        val sha512: String
    }

    data class FabricModFile(
        override val path: Path,
        override val id: String,
        override val name: String,
        override var author: String?,
        override val version: String,
        override val sha512: String,
    ) : ModFile(path), ValidModFile

    data class InvalidModFile(
        override val path: Path,
        val reason: String,
    ) : ModFile(path)
}