package ru.raspberry.launcher.models.dtos

import kotlinx.serialization.Serializable

@Serializable
data class ApiInfoDto(
    val version: String,
    val apiVersion: String,
    val description: String,
    val documentationUrl: String
)