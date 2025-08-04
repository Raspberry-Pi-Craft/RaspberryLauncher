package ru.raspberry.launcher.models.repo.assets

import kotlinx.serialization.Serializable

@Serializable
data class AssetIndex (
    val id: String,
    val sha1: String,
    val size: Long,
    val url: String,
)