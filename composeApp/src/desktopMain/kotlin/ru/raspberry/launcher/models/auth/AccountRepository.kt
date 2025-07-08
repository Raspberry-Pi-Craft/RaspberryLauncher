package ru.raspberry.launcher.models.auth

import kotlinx.serialization.json.Json
import ru.raspberry.launcher.models.Config
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import java.nio.file.Files
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

class AccountRepository {
    private val cache: MutableList<Account> = mutableListOf()
    private val config: Config
    private val filepath: File
        get () = File(config.launcherDataPath + "/accounts.bin")
    private var lastUpdate: Long
    constructor(
        config: Config,
    ) {
        this.config = config
        lastUpdate = System.currentTimeMillis()
        load()
    }
    private fun load() {
        if (!filepath.exists()) {
            Files.createDirectories(filepath.parentFile.toPath())
            return
        }

        val reader = FileReader(filepath, Charsets.ISO_8859_1)
        var data = reader.readText().toByteArray(Charsets.ISO_8859_1);
        reader.close()

        val iv = data.copyOfRange(0, 16)
        data = data.copyOfRange(16, data.size)

        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        cipher.init(
            Cipher.DECRYPT_MODE,
            SecretKeySpec(Base64.getDecoder().decode(config.secret), "AES"),
            IvParameterSpec(iv)
        )
        data = cipher.doFinal(data)

        cache.clear()
        cache.addAll(Json.decodeFromString<List<Account>>(String(data, Charsets.UTF_8)))
    }
    fun save() {
        var data = Json.encodeToString(cache)

        val iv = ByteArray(16).apply { SecureRandom().nextBytes(this) }
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        cipher.init(
            Cipher.ENCRYPT_MODE,
            SecretKeySpec(Base64.getDecoder().decode(config.secret), "AES"),
            IvParameterSpec(iv)
        )
        val encryptedBytes = cipher.doFinal(data.toByteArray(Charsets.UTF_8))
        val bytes = iv + encryptedBytes
        data = String(bytes, Charsets.ISO_8859_1)

        val writer = FileWriter(filepath, Charsets.ISO_8859_1, false)
        writer.write(data)
        writer.close()
        lastUpdate = System.currentTimeMillis()
    }
    private fun checkUpdate() {
        if (!filepath.exists()) return
        val lastModified = Files.getLastModifiedTime(filepath.toPath()).toMillis()
        if (lastModified > lastUpdate) {
            lastUpdate = lastModified
            load()
        }
    }

    fun add(account: Account) {
        checkUpdate()
        cache.add(account)
        save()
    }

    fun getMeta(): List<AccountMeta> {
        checkUpdate()
        return cache.map { AccountMeta(it.authSystem, it.username) }
    }
    fun getByAuthSystem(authSystem: AuthSystem): List<Account> {
        checkUpdate()
        return cache.filter { it.authSystem == authSystem }
    }
    fun getByUsername(username: String): List<Account> {
        checkUpdate()
        return cache.filter { it.username == username }
    }
    fun getByMeta(meta: AccountMeta?): Account? {
        if (meta == null) return null
        checkUpdate()
        return cache.find { it.authSystem == meta.authSystem && it.username == meta.username }
    }
    fun dropCache() {
        cache.clear()
        lastUpdate -= 1 // Force reload on next access
    }

    fun remove(account: Account) {
        checkUpdate()
        cache.removeIf { it.authSystem == account.authSystem
                    && it.username == account.username
                    && it.clientToken == account.clientToken
                    && it.accessToken == account.accessToken
        }
        save()
    }
}

data class AccountMeta(
    val authSystem: AuthSystem,
    val username: String
) {
    val skinUrl: String
        get() = authSystem.skinUrl.format(username)
}