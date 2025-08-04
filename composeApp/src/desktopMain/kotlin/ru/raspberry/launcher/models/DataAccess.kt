package ru.raspberry.launcher.models

import kotlinx.serialization.Serializable

@Serializable
enum class DataAccess {
    Everyone,
    Whitelist,
    Blacklist,
    AuthorOnly
}