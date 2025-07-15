package ru.raspberry.launcher.models.users.access

enum class UserAccess {
    Everyone,
    NotBanned,
    AdminsOnly,
    Whitelist,
    Blacklist,
    None
}