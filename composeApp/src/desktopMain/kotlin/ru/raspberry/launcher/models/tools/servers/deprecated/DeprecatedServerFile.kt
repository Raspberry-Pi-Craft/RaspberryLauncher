package ru.raspberry.launcher.models.tools.servers.deprecated

import kotlinx.serialization.Serializable
import ru.raspberry.launcher.models.server.files.Side

@Serializable
data class DeprecatedServerFile(
    val path: String,
    val env: List<Side>,
    val download: String,
)