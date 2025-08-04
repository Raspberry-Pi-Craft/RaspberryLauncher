package ru.raspberry.launcher.models.repo.assets

import kotlinx.serialization.Serializable

@Serializable
data class AssetList(
    val objects: Map<String, Asset>,
    val virtual: Boolean = false,
    val mapToResources: Boolean = false,
)