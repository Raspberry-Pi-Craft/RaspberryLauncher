package ru.raspberry.launcher.tools

import java.security.MessageDigest

fun ByteArray.sha1(): String {
    val md = MessageDigest.getInstance("SHA-1")
    val digest = md.digest(this)
    return digest.joinToString("") { "%02x".format(it) }
}
fun ByteArray.sha256(): String {
    val md = MessageDigest.getInstance("SHA-256")
    val digest = md.digest(this)
    return digest.joinToString("") { "%02x".format(it) }
}
fun ByteArray.sha512(): String {
    val md = MessageDigest.getInstance("SHA-512")
    val digest = md.digest(this)
    return digest.joinToString("") { "%02x".format(it) }
}
fun ByteArray.md5(): String {
    val md = MessageDigest.getInstance("MD5")
    val digest = md.digest(this)
    return digest.joinToString("") { "%02x".format(it) }
}