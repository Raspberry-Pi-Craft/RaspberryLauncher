package ru.raspberry.launcher.models.repo.library

import kotlinx.serialization.Serializable
import ru.raspberry.launcher.models.repo.DownloadInfo

@Serializable
data class LibraryDownloadInfo(
    val artifact: DownloadInfo? = null,
    val classifiers: Map<String, DownloadInfo>? = null,
)
