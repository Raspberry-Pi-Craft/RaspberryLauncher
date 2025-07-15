package ru.raspberry.launcher.models.server.files

import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable

@Serializable
data class ServerFile(
    val fileId: String,
    val path: String,
    val downloadUrl: String,
    val size: Long,
    val side: List<Side>,
    val hashAlgorithm: String = "SHA256",
    val hashType: String = "Binary",
    val hash: String,
    val createdAt: String,
    val updatedAt: String,
    val updatedBy: String = "system"
)