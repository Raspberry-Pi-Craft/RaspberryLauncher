package ru.raspberry.launcher.service

import com.sun.security.ntlm.Server
import io.ktor.client.*
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import ru.raspberry.launcher.models.ServerData
import ru.raspberry.launcher.models.WindowData
import ru.raspberry.launcher.models.dtos.auth.JoinDto
import java.util.Locale

class LauncherServiceV1<S>(
        private val state: WindowData<S>
) {

    private var token: String? = null
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

    enum class LauncherAuthResult{
        Success,
        NoAccount,
        ConnectionFailed,
        InvalidCredentials,
        FailedToVerify
    }
    suspend fun auth(): LauncherAuthResult {
        val account = state.activeAccount
        if (account == null) return LauncherAuthResult.NoAccount
        client.config {
            headers {
                remove("Authorization")
            }
        }


        var response = client.get(
            urlString = "${state.config.host}/api/v1/auth/id?username=${account.username}",
        )
        if (!response.status.isSuccess()) return LauncherAuthResult.ConnectionFailed
        val serverId = response.bodyAsText().replace("\"", "")
        if (!state.minecraftService.join(account, serverId)) return LauncherAuthResult.ConnectionFailed
        response = client.get(
            urlString = "${state.config.host}/api/v1/auth/verify?id=${serverId}",
        )
        return when (response.status) {
            HttpStatusCode.OK -> {
                token = response.bodyAsText().replace("\"", "")
                client.config {
                    headers {
                        append("Authorization", "Bearer $token")
                    }
                }
                LauncherAuthResult.Success
            }
            HttpStatusCode.BadRequest -> LauncherAuthResult.InvalidCredentials
            HttpStatusCode.Unauthorized -> LauncherAuthResult.FailedToVerify
            else -> LauncherAuthResult.ConnectionFailed
        }
    }

    suspend fun getServerNames() : List<String> {
        val response = client.get(
            urlString = "${state.config.host}/api/v1/server/list"
        )
        if (!response.status.isSuccess()) return emptyList()
        return response.body()
    }

    suspend fun getServerData(serverName: String) : ServerData? {
        val response = client.get(
            urlString = "${state.config.host}/api/v1/server/list?serverName=${serverName}&locale=${Locale.getDefault().language}"
        )
        if (!response.status.isSuccess()) return null
        return response.body()
    }
}