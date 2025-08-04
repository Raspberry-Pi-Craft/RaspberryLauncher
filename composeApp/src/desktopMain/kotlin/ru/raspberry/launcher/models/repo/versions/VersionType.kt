package ru.raspberry.launcher.models.repo.versions

import kotlinx.serialization.Serializable

@Serializable
enum class VersionType {
    Modified,
    Pending,
    Snapshot,
    Release,
    OldBeta,
    OldAlpha,
}