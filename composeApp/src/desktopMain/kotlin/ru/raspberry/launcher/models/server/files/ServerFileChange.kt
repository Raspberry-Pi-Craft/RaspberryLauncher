package ru.raspberry.launcher.models.server.files

import kotlinx.serialization.Serializable
import ru.raspberry.launcher.models.ChangeAction

@Serializable
data class ServerFileChange(
    val action: ChangeAction,
    val fileId: String,

    val newFileId: String? = null,
    val path: String? = null,
    val downloadUrl: String? = null,
    val side: List<Side>? = null,
    val hashAlgorithm: String? = null,
    val hashType: String? = null,
)