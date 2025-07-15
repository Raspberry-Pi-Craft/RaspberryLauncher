package ru.raspberry.launcher.models.java

import kotlinx.serialization.Serializable
import ru.raspberry.launcher.models.OS

@Serializable
data class Java(
    val name: String,
    val version: String,
    val downloadUrl: Map<OS, String>,
    val executablePath: Map<OS, String>,
    val arguments: String,
    val osSpecificArguments: Map<OS, String>
)