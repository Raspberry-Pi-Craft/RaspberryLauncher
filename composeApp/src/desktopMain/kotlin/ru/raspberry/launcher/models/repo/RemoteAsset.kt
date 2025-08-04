package ru.raspberry.launcher.models.repo

import kotlinx.serialization.Serializable
import ru.raspberry.launcher.models.repo.assets.AssetType

@Serializable
data class RemoteAsset(
    val type: AssetType,
    val downloads: Map<String, DownloadInfo>? = null,
    val executable: Boolean = false
)