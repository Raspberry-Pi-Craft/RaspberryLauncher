package ru.raspberry.launcher.models.dtos

import kotlinx.serialization.Serializable

@Serializable
data class Error(
    val error: String,
    val errorMessage: String,
)