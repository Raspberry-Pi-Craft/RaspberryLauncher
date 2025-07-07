package ru.raspberry.launcher.models.dtos

import kotlinx.serialization.Serializable

@Serializable
data class ErrorDto(
    val error: String,
    val errorMessage: String,
)