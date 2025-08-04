package ru.raspberry.launcher.service.repositories

import io.ktor.http.isSuccess
import ru.raspberry.launcher.models.redirect.RedirectChanges
import ru.raspberry.launcher.models.redirect.RedirectData
import ru.raspberry.launcher.service.AsyncRepository
import ru.raspberry.launcher.service.LauncherServiceV1

class RedirectRepository<S>(
    val service: LauncherServiceV1<S>,
) : AsyncRepository<String, RedirectChanges, RedirectData, Pair<String, String>> {
    override suspend fun list(): List<String> =
        service.listRedirects()


    override suspend fun get(key: String): RedirectData? =
        service.getRedirectData(key)


    override suspend fun add(data: Pair<String, String>): Boolean =
        service.createRedirect(data.first, data.second).status.isSuccess()

    override suspend fun edit(
        key: String,
        changes: RedirectChanges
    ): Boolean =
        service.updateRedirect(key, changes).status.isSuccess()


    override suspend fun remove(key: String): Boolean =
        service.removeRedirect(key).status.isSuccess()
}