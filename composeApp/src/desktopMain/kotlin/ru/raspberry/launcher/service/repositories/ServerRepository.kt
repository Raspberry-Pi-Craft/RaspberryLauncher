package ru.raspberry.launcher.service.repositories

import io.ktor.http.*
import ru.raspberry.launcher.models.server.Server
import ru.raspberry.launcher.models.server.ServerChanges
import ru.raspberry.launcher.service.AsyncRepository
import ru.raspberry.launcher.service.LauncherServiceV1

class ServerRepository<S>(
    val service: LauncherServiceV1<S>,
) : AsyncRepository<String, Pair<ServerChanges, String>, Server, Triple<String, String, String>> {
    override suspend fun list(): List<String> =
        service.listServers()


    override suspend fun get(key: String): Server? =
        service.getServer(key)


    override suspend fun add(data: Triple<String, String, String>): Boolean =
        service.createServer(
            data.first,
            data.second,
            data.third
        ).status.isSuccess()

    override suspend fun edit(
        key: String,
        changes: Pair<ServerChanges, String>
    ): Boolean =
        service.updateServer(key, changes.first, changes.second).status.isSuccess()

    override suspend fun remove(key: String): Boolean =
        service.removeServer(key).status.isSuccess()
}