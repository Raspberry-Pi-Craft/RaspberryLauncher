package ru.raspberry.launcher.models.auth

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.window.WindowState
import ru.raspberry.launcher.models.Config
import ru.raspberry.launcher.models.OS
import ru.raspberry.launcher.models.WindowData
import ru.raspberry.launcher.models.users.auth.Account
import ru.raspberry.launcher.service.AccountRepository
import ru.raspberry.launcher.service.DiscordIntegration
import java.nio.file.Files
import kotlin.io.path.Path
import kotlin.test.AfterTest
import kotlin.test.Test

class AccountRepositoryTests {

    private val config = Config()
    private val state = WindowData(
        currentScreen = mutableStateOf(null),
        windowState = WindowState(),
        discord = DiscordIntegration(),
        close = {},
        maximize = {},
        minimize = {},
        themes = emptyMap(),
        languages = emptyMap(),
        recompose = {},
        os = OS.Windows
    )


    @Test
    fun `add account`() {
        val repository = AccountRepository(state)
        val account = Account(
            username = "user",
            accessToken = "token",
            id = "12345",
        )
        repository.add(account)
        repository.dropCache()
        val accounts = repository.getMeta()
        assert("user" in accounts.map { it.username }) { "Account not found in repository" }
    }

    @Test
    fun `bulk add accounts`() {
        val repository = AccountRepository(state)
        val accounts = listOf(
            Account(username = "user1", accessToken = "token1", id = "123451"),
            Account(username = "user2", accessToken = "token2", id = "123452"),
            Account(username = "user3", accessToken = "token3", id = "123453")
        )
        accounts.forEach { repository.add(it) }
        repository.dropCache()
        val meta = repository.getMeta()
        assert(meta.size == 3) { "Expected 3 accounts, found ${meta.size}" }
        assert(meta.map { it.username }.containsAll(accounts.map { it.username })) {
            "Not all accounts found in repository"
        }
    }

    @Test
    fun `non linear add accounts`() {
        val repository = AccountRepository(state)
        val accounts = listOf(
            Account(username = "user1", accessToken = "token1", id = "123451"),
            Account(username = "user2", accessToken = "token2", id = "123452"),
            Account(username = "user3", accessToken = "token3", id = "123453")
        )
        accounts.forEach {
            repository.add(it)
            repository.dropCache()
        }
        val meta = repository.getMeta()
        assert(meta.size == 3) { "Expected 3 accounts, found ${meta.size}" }
        assert(meta.map { it.username }.containsAll(accounts.map { it.username })) {
            "Not all accounts found in repository"
        }
    }
    @AfterTest
    fun cleanup() {
        // Clean up the test data
        Files.deleteIfExists(Path(config.launcherDataPath + "/accounts.bin"))
    }
}