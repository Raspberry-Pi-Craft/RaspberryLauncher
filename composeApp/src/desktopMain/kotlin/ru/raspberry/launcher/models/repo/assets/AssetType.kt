package ru.raspberry.launcher.models.repo.assets

import kotlinx.serialization.Serializable

@Serializable
enum class AssetType {
    File,
    Directory,
}