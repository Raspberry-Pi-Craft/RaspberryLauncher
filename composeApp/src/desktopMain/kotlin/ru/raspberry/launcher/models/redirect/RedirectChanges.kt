package ru.raspberry.launcher.models.redirect

import kotlinx.serialization.Serializable
import ru.raspberry.launcher.models.DataAccess
import ru.raspberry.launcher.models.users.access.AccessChange
import ru.raspberry.launcher.models.users.access.UserAccess

@Serializable
data class RedirectChanges(
    var name: String? = null,
    var url: String? = null,
    var headers: Map<String, HeaderChange>? = null,
    var access: UserAccess? = null,
    var accessUsers: List<AccessChange>? = null,
    var dataAccess: DataAccess? = null,
    var dataAccessUsers: List<AccessChange>? = null,
)
