package ru.raspberry.launcher.service

import io.ktor.client.*
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import ru.raspberry.launcher.models.ServerData
import ru.raspberry.launcher.models.WindowData
import ru.raspberry.launcher.models.dtos.LauncherInfoDto
import ru.raspberry.launcher.models.dtos.ServerFile
import ru.raspberry.launcher.models.dtos.auth.AdminInfoDto

class LauncherServiceV1<S>(
        private val state: WindowData<S>
) {

    private var token: String? = null
    private var client = HttpClient(OkHttp) {
        install(ContentNegotiation) {
            json(Json)
        }
        headers {
            set(HttpHeaders.UserAgent, "Raspberry Launcher")
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
        client = client.config {
            defaultRequest {
                headers {
                    remove(HttpHeaders.Authorization)
                }
            }
        }
        state.minecraftService.refreshToken(account)

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
                client = client.config {
                    defaultRequest {
                        headers {
                            set(HttpHeaders.Authorization, "Bearer $token")
                        }
                    }
                }

                val response = client.get(urlString = "${state.config.host}/api/v1/admin/info")
                if (response.status.isSuccess()) {
                    val adminInfo: AdminInfoDto = response.body()
                    state.isAccountAdmin = adminInfo.isAdmin
                    state.adminMode = false
                }
                else {
                    state.isAccountAdmin = false
                    state.adminMode = false
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

    suspend fun getServerData(serverName: String, locale: String) : ServerData? {
        val response = client.get(
            urlString = "${state.config.host}/api/v1/server/data?serverName=${serverName}&locale=${locale}"
        )
        if (!response.status.isSuccess()) return null
        return response.body()
    }

    suspend fun getFiles(serverName: String) : List<ServerFile>? {
        val response = client.get(
            urlString = "${state.config.host}/api/v1/server/files?serverName=${serverName}&side=client"
        )
        if (!response.status.isSuccess()) return null
        return response.body()
    }


    suspend fun releaseNewLauncherVersion(dto: LauncherInfoDto) : HttpResponse {
        val response = client.patch(
            urlString = "${state.config.host}/api/v1/admin/config/launcher"
        ) {
            contentType(ContentType.Application.Json)
            setBody(dto)
        }
        return response
    }
}