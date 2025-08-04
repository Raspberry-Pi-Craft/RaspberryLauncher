package ru.raspberry.launcher.models.repo

import kotlinx.serialization.Serializable

@Serializable
data class ExtractRules(
    val exclude: List<String> = emptyList()
) {
    fun shouldExtract(path: String): Boolean {
        for (rule in exclude) {
            if (path.startsWith(rule)) {
                return false
            }
        }

        return true
    }
}
