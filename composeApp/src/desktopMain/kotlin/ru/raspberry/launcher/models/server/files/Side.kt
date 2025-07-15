package ru.raspberry.launcher.models.server.files

import kotlinx.serialization.Serializable

@Serializable
enum class Side {
    Client,
    Server,
}