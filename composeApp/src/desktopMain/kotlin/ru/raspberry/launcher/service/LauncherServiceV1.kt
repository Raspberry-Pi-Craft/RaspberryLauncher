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
import ru.raspberry.launcher.models.OS
import ru.raspberry.launcher.models.server.BasicServerData
import ru.raspberry.launcher.models.WindowData
import ru.raspberry.launcher.models.dtos.LauncherInfo
import ru.raspberry.launcher.models.java.Java
import ru.raspberry.launcher.models.java.JavaChanges
import ru.raspberry.launcher.models.java.JavaData
import ru.raspberry.launcher.models.minecraft.MinecraftChanges
import ru.raspberry.launcher.models.minecraft.MinecraftLoader
import ru.raspberry.launcher.models.redirect.RedirectChanges
import ru.raspberry.launcher.models.redirect.RedirectData
import ru.raspberry.launcher.models.server.AdvancedServerData
import ru.raspberry.launcher.models.server.Server
import ru.raspberry.launcher.models.server.ServerChanges
import ru.raspberry.launcher.models.users.UserInfo

class LauncherServiceV1<S>(
        private val state: WindowData<S>
) {

    private var client = HttpClient(OkHttp) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
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
                    val userInfo: UserInfo = response.body()
                    state.isAccountAdmin = userInfo.isAdmin
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

    suspend fun logout() {
        val response = client.get(
            urlString = "${state.config.host}/api/v1/auth/logout"
        )
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
    }

    suspend fun info() : UserInfo? {
        val response = client.get(
            urlString = "${state.config.host}/api/v1/user/info"
        )
        if (!response.status.isSuccess()) return null
        return response.body()
    }

    suspend fun op(username : String) : HttpResponse {
        return client.post(
            urlString = "${state.config.host}/api/v1/user/op/${username.encodeURLPath()}"
        )
    }

    suspend fun deop(username : String) : HttpResponse {
        return client.post(
            urlString = "${state.config.host}/api/v1/user/deop/${username.encodeURLPath()}"
        )
    }

    suspend fun ban(username : String) : HttpResponse {
        return client.post(
            urlString = "${state.config.host}/api/v1/user/ban/${username.encodeURLPath()}"
        )
    }

    suspend fun unban(username : String) : HttpResponse {
        return client.post(
            urlString = "${state.config.host}/api/v1/user/unban/${username.encodeURLPath()}"
        )
    }

    // =====================================================
    //                     Server API
    // =====================================================
    suspend fun getServers() : List<String> {
        val response = client.get(
            urlString = "${state.config.host}/api/v1/server/list"
        )
        if (!response.status.isSuccess()) return emptyList()
        return response.body()
    }

    suspend fun getServerInfo(serverName: String, locale: String? = null) : BasicServerData? {
        val response = client.get(
            urlString = "${state.config.host}/api/v1/server/${serverName.encodeURLPath()}${if (locale != null) "?locale=${locale.encodeURLQueryComponent()}" else ""}"
        )
        if (!response.status.isSuccess()) return null
        return response.body()
    }

    suspend fun hasAccessToServer(serverName: String) : Boolean {
        val response = client.get(
            urlString = "${state.config.host}/api/v1/server/${serverName.encodeURLPath()}/access"
        )
        return response.status.isSuccess()
    }

    suspend fun getServerData(serverName: String, os: OS) : AdvancedServerData? {
        val response = client.get(
            urlString = "${state.config.host}/api/v1/server/${serverName.encodeURLPath()}/data?side=client&os=${os.name.encodeURLQueryComponent()}"
        )
        if (!response.status.isSuccess()) return null
        return response.body()
    }

    suspend fun getServerAllData(serverName: String) : Server? {
        val response = client.get(
            urlString = "${state.config.host}/api/v1/server/${serverName.encodeURLPath()}/all"
        )
        if (!response.status.isSuccess()) return null
        return response.body()
    }

    suspend fun createServer(
        serverName: String,
        serverAddress: String,
        java: String,
        minecraft: String
    ) : HttpResponse {
        val response = client.post(
            urlString = "${state.config.host}/api/v1/server/create"
        ) {
            contentType(ContentType.Application.Json)
            setBody(ServerChanges(
                name = serverName,
                address = serverAddress,
                java = java,
                minecraft = minecraft
            ))
        }
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
        return response
    }

    suspend fun removeServer(serverName: String) : HttpResponse {
        val response = client.delete(
            urlString = "${state.config.host}/api/v1/server/${serverName.encodeURLPath()}"
        )
        return response
    }

    // =====================================================
    //                     Redirect API
    // =====================================================
    suspend fun redirect(name: String) : HttpResponse {
        return client.get("${state.config.host}/api/v1/redirect?name=${name.encodeURLQueryComponent()}")
    }

    suspend fun listOfRedirects() : List<String>? {
        val response = client.get("${state.config.host}/api/v1/redirect/list")
        return if (response.status.isSuccess()) response.body() else null
    }

    suspend fun getRedirectData(name: String) : RedirectData? {
        val response = client.get("${state.config.host}/api/v1/redirect/get?name=${name.encodeURLQueryComponent()}")
        return if (response.status.isSuccess()) response.body() else null
    }

    suspend fun createRedirect(name: String, url: String) : HttpResponse {
        return client.post("${state.config.host}/api/v1/redirect/create") {
            contentType(ContentType.Application.Json)
            setBody(
                RedirectChanges(
                    name = name,
                    url = url
                )
            )
        }
    }

    suspend fun updateRedirect(name: String, changes: RedirectChanges) : HttpResponse {
        return client.post("${state.config.host}/api/v1/redirect/update?name=${name.encodeURLQueryComponent()}") {
            contentType(ContentType.Application.Json)
            setBody(changes)
        }
    }

    suspend fun removeRedirect(name: String) : HttpResponse {
        return client.post("${state.config.host}/api/v1/redirect/update?name=${name.encodeURLQueryComponent()}") {
            contentType(ContentType.Application.Json)
        }
    }

    // =====================================================
    //                     Java API
    // =====================================================

    suspend fun listJava() : List<String>? {
        val response = client.get("${state.config.host}/api/v1/java/list")
        if (!response.status.isSuccess()) return null
        return response.body()
    }
    suspend fun listJavaVersions(java : String) : List<String>? {
        val response = client.get("${state.config.host}/api/v1/java/${java.encodeURLPath()}/versions")
        if (!response.status.isSuccess()) return null
        return response.body()
    }
    fun javaTag(java: String, version: String): String = "$java:$version"

    suspend fun getJavaData(tag: String, os: OS) : JavaData? {
        val response = client.get("${state.config.host}/api/v1/java/${tag.encodeURLPath()}?os=${os.name.encodeURLQueryComponent()}")
        if (!response.status.isSuccess()) return null
        return response.body()
    }

    suspend fun getJavaAllData(tag: String) : Java? {
        val response = client.get("${state.config.host}/api/v1/java/${tag.encodeURLPath()}/all")
        if (!response.status.isSuccess()) return null
        return response.body()
    }

    suspend fun createJava(java: String, version: String) : HttpResponse {
        return client.post("${state.config.host}/api/v1/java/create") {
            contentType(ContentType.Application.Json)
            setBody(
                JavaChanges(
                    name = java,
                    version = version
                )
            )
        }
    }

    suspend fun updateJava(tag: String, os: OS, changes: JavaChanges) : HttpResponse {
        return client.patch("${state.config.host}/api/v1/java/${tag.encodeURLPath()}?os=${os.name.encodeURLQueryComponent()}") {
            contentType(ContentType.Application.Json)
            setBody(changes)
        }
    }

    suspend fun removeJava(tag: String) : HttpResponse {
        return client.delete("${state.config.host}/api/v1/java/${tag.encodeURLPath()}")
    }

    // =====================================================
    //                     Minecraft API
    // =====================================================

    suspend fun listMinecraft() : List<String>? {
        val response = client.get("${state.config.host}/api/v1/minecraft/list")
        if (!response.status.isSuccess()) return null
        return response.body()
    }
    suspend fun listMinecraftLoaders(version : String) : List<String>? {
        val response = client.get("${state.config.host}/api/v1/minecraft/${version.encodeURLPath()}/loaders")
        if (!response.status.isSuccess()) return null
        return response.body()
    }
    fun minecraftTag(java: String, version: String): String = "$java:$version"

    suspend fun getMinecraftData(tag: String, os: OS) : JavaData? {
        val response = client.get("${state.config.host}/api/v1/minecraft/${tag.encodeURLPath()}?os=${os.name.encodeURLQueryComponent()}")
        if (!response.status.isSuccess()) return null
        return response.body()
    }

    suspend fun getMinecraftAllData(tag: String) : Java? {
        val response = client.get("${state.config.host}/api/v1/minecraft/${tag.encodeURLPath()}/all")
        if (!response.status.isSuccess()) return null
        return response.body()
    }

    suspend fun createMinecraft(version: String, loader: MinecraftLoader) : HttpResponse {
        return client.post("${state.config.host}/api/v1/minecraft/create") {
            contentType(ContentType.Application.Json)
            setBody(
                MinecraftChanges(
                    version = version,
                    loader = loader
                )
            )
        }
    }

    suspend fun updateMinecraft(tag: String, os: OS, changes: MinecraftChanges) : HttpResponse {
        return client.patch("${state.config.host}/api/v1/minecraft/${tag.encodeURLPath()}?os=${os.name.encodeURLQueryComponent()}") {
            contentType(ContentType.Application.Json)
            setBody(changes)
        }
    }

    suspend fun removeMinecraft(tag: String) : HttpResponse {
        return client.delete("${state.config.host}/api/v1/minecraft/${tag.encodeURLPath()}")
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
        return response
    }
}