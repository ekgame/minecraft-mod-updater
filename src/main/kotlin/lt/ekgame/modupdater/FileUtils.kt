package lt.ekgame.modupdater

import java.math.BigInteger
import java.nio.file.Path
import java.security.MessageDigest
import kotlin.io.path.readBytes

fun Path.sha512(): String {
    val digest = MessageDigest
        .getInstance("SHA-512")
        .digest(this.readBytes())

    var hash = BigInteger(1, digest).toString(16)

    while (hash.length < 32) {
        hash = "0$hash"
    }

    return hash
}