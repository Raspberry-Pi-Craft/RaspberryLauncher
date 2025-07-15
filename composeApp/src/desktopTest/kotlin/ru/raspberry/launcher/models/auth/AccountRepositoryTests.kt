package ru.raspberry.launcher.models.auth

import ru.raspberry.launcher.models.Config
import ru.raspberry.launcher.models.users.auth.Account
import ru.raspberry.launcher.models.users.auth.AccountRepository
import java.nio.file.Files
import kotlin.io.path.Path
import kotlin.test.AfterTest
import kotlin.test.Test

class AccountRepositoryTests {

    private var config = Config()


    @Test
    fun `add account`() {
        val repository = AccountRepository(config)
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
        val repository = AccountRepository(config)
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
        val repository = AccountRepository(config)
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