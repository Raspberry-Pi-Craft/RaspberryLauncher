package ru.raspberry.launcher.service

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.headers
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import io.ktor.util.reflect.typeInfo
import kotlinx.serialization.json.Json
import ru.raspberry.launcher.models.WindowData
import ru.raspberry.launcher.models.auth.Account
import ru.raspberry.launcher.models.auth.AccountRepository
import ru.raspberry.launcher.models.auth.AuthSystem
import ru.raspberry.launcher.models.dtos.ErrorDto
import ru.raspberry.launcher.models.dtos.auth.AuthRequestDto
import ru.raspberry.launcher.models.dtos.auth.AuthResponseDto
import ru.raspberry.launcher.models.dtos.auth.JoinDto
import ru.raspberry.launcher.models.dtos.auth.TokenRequestDto
import ru.raspberry.launcher.models.dtos.auth.SignOutRequestDto
import kotlin.random.Random

class MinecraftApiService<S>(
    private val state: WindowData<S>
) {
    private val client = HttpClient(OkHttp) {
        install(ContentNegotiation) {
            json(Json {
                prettyPrint = true
                isLenient = true
            })
        }
        headers {
            append("User-Agent", "Raspberry Launcher")
        }
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

        val response = state.client.post(
            urlString = authSystem.authUrl
        ) {
            contentType(ContentType.Application.Json)
            setBody(AuthRequestDto(
                username = username,
                password = if (totp != null) "$password:$totp" else password,
                clientToken = Random.nextBytes(32).joinToString("") { "%02x".format(it) },
            ))
        }
        when (response.status.value) {
            401 -> {
                val dto: ErrorDto = response.body()
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
                val account: AuthResponseDto = response.body()
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
        val response = state.client.post(
            urlString = account.authSystem.refreshTokenUrl
        ) {
            contentType(ContentType.Application.Json)
            setBody(TokenRequestDto(
                accessToken = account.accessToken,
                clientToken = account.clientToken,
            ))
        }
        return when (response.status.value) {
            200 -> {
                val response: AuthResponseDto = response.body()
                account.accessToken = response.accessToken
                true
            }
            else -> false
        }
    }

    suspend fun isTokenValid(account: Account): Boolean {
        val response = state.client.post(
            urlString = account.authSystem.validateTokenUrl
        ) {
            contentType(ContentType.Application.Json)
            setBody(TokenRequestDto(
                accessToken = account.accessToken
            ))
        }
        return response.status.isSuccess()
    }

    suspend fun invalidateToken(account: Account): Boolean {
        val response = state.client.post(
            urlString = account.authSystem.invalidateTokenUrl
        ) {
            contentType(ContentType.Application.Json)
            setBody(TokenRequestDto(
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
        val response = state.client.post(
            urlString = authSystem.signOutUrl
        ) {
            contentType(ContentType.Application.Json)
            setBody(SignOutRequestDto(
                    username = username,
                    password = password,
                ))
        }
        return response.status.isSuccess()
    }

    suspend fun join(account: Account, serverId: String): Boolean {
        val response = client.post(account.authSystem.joinUrl) {
            contentType(ContentType.Application.Json)
            setBody(JoinDto(
                accessToken = account.accessToken,
                serverId = serverId,
                selectedProfile = account.id
            ))
        }
        return response.status.isSuccess() || response.status == HttpStatusCode.Found;
    }

}