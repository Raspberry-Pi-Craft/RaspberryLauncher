package ru.raspberry.launcher.models.dtos

import kotlinx.serialization.Serializable

@Serializable
data class ServerFile(
    val fileId: String,
    val path: String,
    val downloadUrl: String,
    val size: Long,
    val hashAlgorithm: String,
    val hashType: String,
    val hash: String
)
