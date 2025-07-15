package ru.raspberry.launcher.models.redirect

import kotlinx.serialization.Serializable
import ru.raspberry.launcher.models.users.access.AccessChange
import ru.raspberry.launcher.models.users.access.UserAccess

@Serializable
data class RedirectChanges(
    val name: String? = null,
    val url: String? = null,
    val headers: Map<String, HeaderChange>? = null,
    val access: UserAccess? = null,
    val accessUsers: List<AccessChange>? = null,
    val headerAccess: RedirectAccess? = null,
    val headerAccessUsers: List<AccessChange>? = null,
)
