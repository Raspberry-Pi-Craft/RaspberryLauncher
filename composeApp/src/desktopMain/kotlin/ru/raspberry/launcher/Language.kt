package ru.raspberry.launcher

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient


@Serializable
data class Language(
    val name: String,
    val translation: Map<String, String> = emptyMap()
) {
    @Transient
    lateinit var id: String
    fun get(key: String, default: String) = translation.getOrDefault(key, default)
}