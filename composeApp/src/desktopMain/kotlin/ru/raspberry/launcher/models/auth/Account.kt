package ru.raspberry.launcher.models.auth

import kotlinx.serialization.Serializable
import kotlin.random.Random

@Serializable
data class Account(
    val authSystem: AuthSystem = AuthSystem.ELY_BY,
    val clientToken: String = Random.nextBytes(32).joinToString("") { "%02x".format(it) },
    val id: String,
    val username: String,
    var accessToken: String
)