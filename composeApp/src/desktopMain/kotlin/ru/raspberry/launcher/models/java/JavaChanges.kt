package ru.raspberry.launcher.models.java

import kotlinx.serialization.Serializable

@Serializable
data class JavaChanges(
    val name: String? = null,
    val version: String? = null,
    val downloadUrl: String? = null,
    val executablePath: String? = null,
    val arguments: String? = null,
    val osSpecificArguments: String? = null,
)