package ru.raspberry.launcher.models.repo

import kotlinx.serialization.Serializable
import ru.raspberry.launcher.models.OS

@Serializable
data class OSRestriction(
    val name: OS? = null
)
