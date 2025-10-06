package ru.raspberry.launcher.models

import kotlinx.serialization.Serializable
import ru.raspberry.launcher.GoodJson
import java.io.File
import java.security.SecureRandom
import java.util.Base64


@Serializable
data class Config(
    var host: String = "https://raspberry.rellomine.ru",
    var minecraftPath: String = ".minecraft",
    var launcherDataPath: String = System.getProperty("compose.application.resources.dir") ?: "data",
    val secret: String = Base64.getEncoder().encodeToString(ByteArray(16).apply {
        SecureRandom().nextBytes(this)
    }),
    var activeAccountId: Int = -1,
    var useDiscreteGPU: Boolean = true,
    var minecraftWindowWidth: Int = 925,
    var minecraftWindowHeight: Int = 530,
    var ram: Int = 4096,
    var theme: String = "Dark",
    var language: String = "ru",
    var debug: Boolean = false,
    var richPresence: Boolean = false,
    var autoCheckForUpdates: Boolean = true,
) {

    fun save() {
        val configFile = File("config.json")
        configFile.writeText(GoodJson.encodeToString(serializer(), this))
    }
}