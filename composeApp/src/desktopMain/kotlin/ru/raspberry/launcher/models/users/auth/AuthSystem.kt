package ru.raspberry.launcher.models.users.auth

import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.DrawableResource
import raspberrylauncher.composeapp.generated.resources.Res
import raspberrylauncher.composeapp.generated.resources.ely_by

@Serializable
enum class AuthSystem(
    val displayName: String,
    val authUrl: String,
    val refreshTokenUrl: String,
    val validateTokenUrl: String,
    val signOutUrl: String,
    val invalidateTokenUrl: String,
    val joinUrl: String,
    val skinUrl: String,
    val drawableResource: DrawableResource
) {
    ELY_BY(
        "Ely.by",
        "https://authserver.ely.by/auth/authenticate",
        "https://authserver.ely.by/auth/refresh",
        "https://authserver.ely.by/auth/validate",
        "https://authserver.ely.by/auth/signout",
        "https://authserver.ely.by/auth/invalidate",
        "https://authserver.ely.by/session/join",
        "http://skinsystem.ely.by/skins/%s",
        Res.drawable.ely_by
    )
}