package ru.raspberry.launcher.service

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.decodeToImageBitmap
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.readRawBytes
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import ru.raspberry.launcher.models.WindowData
import ru.raspberry.launcher.models.users.auth.Account
import ru.raspberry.launcher.models.users.auth.AccountMeta
import ru.raspberry.launcher.models.users.auth.AccountRepository
import ru.raspberry.launcher.models.users.auth.AuthSystem
import ru.raspberry.launcher.models.dtos.Error
import ru.raspberry.launcher.models.dtos.auth.*
import kotlin.random.Random

class MinecraftApiService(
    private val repository: AccountRepository
) {
    @OptIn(ExperimentalSerializationApi::class)
    private val client = HttpClient(OkHttp) {
        install(ContentNegotiation) {
            json(Json{

                decodeEnumsCaseInsensitive = true
            })
        }
        headers {
            set(HttpHeaders.UserAgent, "Raspberry Launcher")
        }
        followRedirects = true
    }

    enum class MinecraftAuthResult {
        RequestedTOTP,
        InvalidTOTP,
        InvalidCredentials,
        UnexpectedError,
        Successful
    }
    suspend fun auth(
        repository: AccountRepository,
        authSystem: AuthSystem,
        username: String,
        password: String,
        totp: String? = null
    ): MinecraftAuthResult {

        val response = client.post(
            urlString = authSystem.authUrl
        ) {
            contentType(ContentType.Application.Json)
            setBody(AuthRequest(
                username = username,
                password = if (totp != null) "$password:$totp" else password,
                clientToken = Random.nextBytes(32).joinToString("") { "%02x".format(it) },
            ))
        }
        when (response.status.value) {
            401 -> {
                val dto: Error = response.body()
                when (dto.errorMessage) {
                    "Account protected with two factor auth." -> {
                        return MinecraftAuthResult.RequestedTOTP
                    }
                    "Invalid credentials. Invalid email or password." -> {
                        if (totp != null) return MinecraftAuthResult.InvalidTOTP
                        else return MinecraftAuthResult.InvalidCredentials
                    }
                    else -> {
                        return MinecraftAuthResult.UnexpectedError
                    }
                }
            }
            200 -> {
                val account: AuthResponse = response.body()
                repository.add(Account(
                    authSystem = authSystem,
                    id = account.selectedProfile.id,
                    username = account.selectedProfile.name,
                    accessToken = account.accessToken,
                    clientToken = account.clientToken
                ))
                return MinecraftAuthResult.Successful
            }
            else -> {
                return MinecraftAuthResult.UnexpectedError
            }
        }
    }

    suspend fun refreshToken(
        account: Account
    ): Boolean {
        val response = client.post(
            urlString = account.authSystem.refreshTokenUrl
        ) {
            contentType(ContentType.Application.Json)
            setBody(TokenRequest(
                accessToken = account.accessToken,
                clientToken = account.clientToken,
            ))
        }
        return when (response.status.value) {
            200 -> {
                val response: AuthResponse = response.body()
                repository.remove(account)
                account.accessToken = response.accessToken
                repository.add(account)
                true
            }
            else -> false
        }
    }

    suspend fun isTokenValid(account: Account): Boolean {
        val response = client.post(
            urlString = account.authSystem.validateTokenUrl
        ) {
            contentType(ContentType.Application.Json)
            setBody(TokenRequest(
                accessToken = account.accessToken
            ))
        }
        return response.status.isSuccess()
    }

    suspend fun invalidateToken(account: Account): Boolean {
        val response = client.post(
            urlString = account.authSystem.invalidateTokenUrl
        ) {
            contentType(ContentType.Application.Json)
            setBody(TokenRequest(
                accessToken = account.accessToken,
                clientToken = account.clientToken,
            ))
        }
        return response.status.isSuccess()
    }

    suspend fun signOut(
        authSystem: AuthSystem,
        username: String,
        password: String
    ): Boolean {
        val response = client.post(
            urlString = authSystem.signOutUrl
        ) {
            contentType(ContentType.Application.Json)
            setBody(SignOutRequest(
                    username = username,
                    password = password,
                ))
        }
        return response.status.isSuccess()
    }

    suspend fun join(account: Account, serverId: String): Boolean {
        val response = client.post(account.authSystem.joinUrl) {
            contentType(ContentType.Application.Json)
            setBody(Join(
                accessToken = account.accessToken,
                serverId = serverId,
                selectedProfile = account.id
            ))
        }
        return response.status.isSuccess()
    }

    suspend fun getSkin(account: AccountMeta): ImageBitmap {
        val response = client.get(
            urlString = account.skinUrl
        )
        val bytes: ByteArray = response.readRawBytes()
        return bytes.decodeToImageBitmap()
    }

}