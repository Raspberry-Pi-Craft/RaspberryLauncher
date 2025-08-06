package ru.raspberry.launcher.models.repo.library

import kotlinx.serialization.Serializable

@Serializable
data class LibraryReplaceList(
    val version: Float = 0f,
    val allowElyEveywhere: Boolean = false,
    val libraries: Map<String, List<LibraryReplace>> = emptyMap()
)