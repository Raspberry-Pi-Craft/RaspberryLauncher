package ru.raspberry.launcher.models.redirect

import kotlinx.serialization.Serializable

@Serializable
enum class RedirectAccess {
    Everyone,
    Whitelist,
    Blacklist,
    AuthorOnly
}