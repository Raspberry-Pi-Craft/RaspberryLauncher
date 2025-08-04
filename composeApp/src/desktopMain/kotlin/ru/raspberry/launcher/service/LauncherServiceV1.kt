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
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import ru.raspberry.launcher.models.OS
import ru.raspberry.launcher.models.server.BasicServerData
import ru.raspberry.launcher.models.WindowData
import ru.raspberry.launcher.models.dtos.LauncherInfo
import ru.raspberry.launcher.models.redirect.RedirectChanges
import ru.raspberry.launcher.models.redirect.RedirectData
import ru.raspberry.launcher.models.repo.versions.MinecraftVersionInfo
import ru.raspberry.launcher.models.server.AdvancedServerData
import ru.raspberry.launcher.models.server.Server
import ru.raspberry.launcher.models.server.ServerChanges
import ru.raspberry.launcher.models.users.UserInfo

class LauncherServiceV1<S>(
    private val state: WindowData<S>
) {
    private var withToken = false
    private var lastResponse: HttpResponse? = null
    var response: HttpResponse?
        get() = lastResponse
        private set(value) {
            lastResponse = value
            if (!state.config.debug) return
            println("${value?.request?.method} ${value?.request?.url} -> ${value?.status}")
            println(runBlocking { value?.bodyAsText() ?: "No response body" })
        }

    private var client = HttpClient(OkHttp) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                explicitNulls = false
            })
        }
        headers {
            set(HttpHeaders.UserAgent, "Raspberry Launcher")
        }
        followRedirects = true
    }

    enum class LauncherAuthResult{
        Success,
        NoAccount,
        ConnectionFailed,
        InvalidCredentials,
        FailedToVerify
    }
    // =====================================================
    //                     User API
    // =====================================================
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
        this.response = response
        return when (response.status) {
            HttpStatusCode.OK -> {
                val token = response.bodyAsText().replace("\"", "")
                client = client.config {
                    defaultRequest {
                        headers {
                            set(HttpHeaders.Authorization, "Bearer $token")
                        }
                    }
                }
                withToken = true

                val info = info()
                if (info != null) {
                    state.isAccountAdmin = info.isAdmin
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
    suspend fun refresh() {
        if (!withToken) return
        val response = client.get(
            urlString = "${state.config.host}/api/v1/auth/refresh"
        )
        this.response = response
        if (!response.status.isSuccess()) return
        val token = response.bodyAsText().replace("\"", "")
        client = client.config {
            defaultRequest {
                headers {
                    set(HttpHeaders.Authorization, "Bearer $token")
                }
            }
        }
    }

    suspend fun logout() {
        val response = client.get(
            urlString = "${state.config.host}/api/v1/auth/logout"
        )
        this.response = response
        if (!response.status.isSuccess()) return
        client = client.config {
            defaultRequest {
                headers {
                    remove(HttpHeaders.Authorization)
                }
            }
        }
        state.isAccountAdmin = false
        state.adminMode = false
        withToken = false
    }

    suspend fun info() : UserInfo? {
        val response = client.get(
            urlString = "${state.config.host}/api/v1/user/info"
        )
        this.response = response
        if (!response.status.isSuccess()) return null
        return response.body()
    }

    suspend fun op(username : String) : HttpResponse {
        val response = client.post(
            urlString = "${state.config.host}/api/v1/user/op/${username.encodeURLPath()}"
        )
        this.response = response
        return response
    }

    suspend fun deop(username : String) : HttpResponse {
        val response = client.post(
            urlString = "${state.config.host}/api/v1/user/deop/${username.encodeURLPath()}"
        )
        this.response = response
        return response
    }

    suspend fun ban(username : String) : HttpResponse {
        val response = client.post(
            urlString = "${state.config.host}/api/v1/user/ban/${username.encodeURLPath()}"
        )
        this.response = response
        return response
    }

    suspend fun unban(username : String) : HttpResponse {
        val response = client.post(
            urlString = "${state.config.host}/api/v1/user/unban/${username.encodeURLPath()}"
        )
        this.response = response
        return response
    }

    // =====================================================
    //                     Server API
    // =====================================================
    suspend fun listServers() : List<String> {
        val response = client.get(
            urlString = "${state.config.host}/api/v1/server/list"
        )
        this.response = response
        if (!response.status.isSuccess()) return emptyList()
        return response.body()
    }

    suspend fun getServerInfo(serverName: String, locale: String? = null) : BasicServerData? {
        val response = client.get(
            urlString = "${state.config.host}/api/v1/server/${serverName.encodeURLPath()}${if (locale != null) "?locale=${locale.encodeURLQueryComponent()}" else ""}"
        )
        this.response = response
        if (!response.status.isSuccess()) return null
        return response.body()
    }

    suspend fun hasAccessToServer(serverName: String) : Boolean {
        val response = client.get(
            urlString = "${state.config.host}/api/v1/server/${serverName.encodeURLPath()}/access"
        )
        this.response = response
        return response.status.isSuccess()
    }

    suspend fun getServerData(serverName: String, os: OS) : AdvancedServerData? {
        val response = client.get(
            urlString = "${state.config.host}/api/v1/server/${serverName.encodeURLPath()}/data?side=client&os=${os.name.encodeURLQueryComponent()}"
        )
        this.response = response
        if (!response.status.isSuccess()) return null
        return response.body()
    }

    suspend fun getServer(serverName: String) : Server? {
        val response = client.get(
            urlString = "${state.config.host}/api/v1/server/${serverName.encodeURLPath()}/all"
        )
        this.response = response
        if (!response.status.isSuccess()) return null
        return response.body()
    }

    suspend fun createServer(
        serverName: String,
        serverAddress: String,
        minecraft: String
    ) : HttpResponse {
        val response = client.post(
            urlString = "${state.config.host}/api/v1/server/create"
        ) {
            contentType(ContentType.Application.Json)
            setBody(ServerChanges(
                name = serverName,
                address = serverAddress,
                minecraft = minecraft
            ))
        }
        this.response = response
        return response
    }

    suspend fun updateServer(
        serverName:String,
        serverData: ServerChanges,
        locale: String
    ) : HttpResponse {
        val response = client.patch(
            urlString = "${state.config.host}/api/v1/server/${serverName.encodeURLPath()}?locale=${locale.encodeURLQueryComponent()}"
        ) {
            contentType(ContentType.Application.Json)
            setBody(serverData)
        }
        this.response = response
        return response
    }

    suspend fun removeServer(serverName: String) : HttpResponse {
        val response = client.delete(
            urlString = "${state.config.host}/api/v1/server/${serverName.encodeURLPath()}"
        )
        this.response = response
        return response
    }

    // =====================================================
    //                     Redirect API
    // =====================================================
    suspend fun redirect(name: String) : HttpResponse {
        val response = client.get("${state.config.host}/api/v1/redirect?name=${name.encodeURLQueryComponent()}")
        this.response = response
        return response
    }

    suspend fun listRedirects() : List<String> {
        val response = client.get("${state.config.host}/api/v1/redirect/list")
        this.response = response
        return if (response.status.isSuccess()) response.body() else emptyList()
    }

    suspend fun getRedirectData(name: String) : RedirectData? {
        val response = client.get("${state.config.host}/api/v1/redirect/get?name=${name.encodeURLQueryComponent()}")
        this.response = response
        return if (response.status.isSuccess()) response.body() else null
    }

    suspend fun createRedirect(name: String, url: String) : HttpResponse {
        val response = client.post("${state.config.host}/api/v1/redirect/create") {
            contentType(ContentType.Application.Json)
            setBody(
                RedirectChanges(
                    name = name,
                    url = url
                )
            )
        }
        this.response = response
        return response
    }

    suspend fun updateRedirect(name: String, changes: RedirectChanges) : HttpResponse {
        val response = client.patch("${state.config.host}/api/v1/redirect/update?name=${name.encodeURLQueryComponent()}") {
            contentType(ContentType.Application.Json)
            setBody(changes)
        }
        this.response = response
        return response
    }

    suspend fun removeRedirect(name: String) : HttpResponse {
        val response = client.delete("${state.config.host}/api/v1/redirect/delete?name=${name.encodeURLQueryComponent()}") {
            contentType(ContentType.Application.Json)
        }
        this.response = response
        return response
    }

    // =====================================================
    //                     Minecraft API
    // =====================================================

    suspend fun listMinecraft() : List<String> {
        val response = client.get("${state.config.host}/api/v1/minecraft/list")
        this.response = response
        if (!response.status.isSuccess()) return emptyList()
        return response.body()
    }
    suspend fun getMinecraftData(tag: String) : MinecraftVersionInfo? {
        val response = client.get("${state.config.host}/api/v1/minecraft/${tag.encodeURLPath()}")
        this.response = response
        if (!response.status.isSuccess()) return null
        return response.body()
    }

    // =====================================================
    //                     Config API
    // =====================================================
    suspend fun releaseNewLauncherVersion(dto: LauncherInfo) : HttpResponse {
        val response = client.patch(
            urlString = "${state.config.host}/api/v1/info/launcher"
        ) {
            contentType(ContentType.Application.Json)
            setBody(dto)
        }
        this.response = response
        return response
    }
}