package ru.raspberry.launcher.models.repo

import kotlinx.serialization.Serializable

@Serializable
data class VersionInfo(
    val name: String,
    val released: String,
)