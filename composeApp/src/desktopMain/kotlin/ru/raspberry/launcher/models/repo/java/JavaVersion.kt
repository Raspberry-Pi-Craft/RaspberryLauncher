package ru.raspberry.launcher.models.repo.java

import kotlinx.serialization.Serializable

@Serializable
data class JavaVersion(
    val component: String,
    val majorVersion: Int,
)