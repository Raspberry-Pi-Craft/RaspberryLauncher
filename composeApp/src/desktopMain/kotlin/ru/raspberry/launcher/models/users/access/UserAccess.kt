package ru.raspberry.launcher.models.users.access

import kotlinx.serialization.Serializable

@Serializable
enum class UserAccess {
    Everyone,
    NotBanned,
    AdminsOnly,
    Whitelist,
    Blacklist,
    None
}