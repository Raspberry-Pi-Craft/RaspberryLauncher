package ru.raspberry.launcher.models.repo

import kotlinx.serialization.Serializable

@Serializable
data class DownloadInfo(
    val sha1: String? = null,
    val size: Long? = null,
    val url: String,
)
