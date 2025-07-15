package ru.raspberry.launcher.models.java

import kotlinx.serialization.Serializable

@Serializable
data class JavaData(
    val name: String,
    val version: String,
    val downloadUrl: String?,
    val executablePath: String?,
    val arguments: String,
    val osSpecificArguments: String?
)