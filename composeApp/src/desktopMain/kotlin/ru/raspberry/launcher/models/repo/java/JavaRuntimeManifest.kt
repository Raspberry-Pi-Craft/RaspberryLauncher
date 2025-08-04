package ru.raspberry.launcher.models.repo.java

import kotlinx.serialization.Serializable
import ru.raspberry.launcher.models.repo.RemoteAsset

@Serializable
data class JavaRuntimeManifest(
    val files: Map<String, RemoteAsset>
)