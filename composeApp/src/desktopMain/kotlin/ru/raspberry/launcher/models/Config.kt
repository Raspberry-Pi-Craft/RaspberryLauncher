package ru.raspberry.launcher.models

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.security.SecureRandom
import java.util.Base64


@Serializable
data class Config(
    var host: String = "http://localhost:5285",
    var minecraftPath: String = ".minecraft",
    var launcherDataPath: String = System.getProperty("compose.application.resources.dir") ?: "../assets/common",
    val secret: String = Base64.getEncoder().encodeToString(ByteArray(16).apply {
        SecureRandom().nextBytes(this)
    }),
    var activeAccountId: Int = -1,
    var useDiscreteGPU: Boolean = true,
    var minecraftWindowWidth: Int = 925,
    var minecraftWindowHeight: Int = 530,
    var ram: Int = 4096,
    var theme: String = "Dark Raspberry"
) {
    fun save() {
        val configFile = File("config.json")
        configFile.writeText(Json.encodeToString(serializer(), this))
    }
}