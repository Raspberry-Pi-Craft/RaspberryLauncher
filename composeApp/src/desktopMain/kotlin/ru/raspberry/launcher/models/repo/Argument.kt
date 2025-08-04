package ru.raspberry.launcher.models.repo

import kotlinx.serialization.Serializable
import ru.raspberry.launcher.models.OS
import ru.raspberry.launcher.serializers.ArgumentSerializer

@Serializable(with = ArgumentSerializer::class)
data class Argument(
    val value: String,
    val rules: List<Rule> = emptyList(),
) {
    fun isActive(
        os: OS,
        features: Map<String, Boolean> = emptyMap()
    ): Boolean {
        return rules.isEmpty() || rules.any { it.isApplicable(os, features) }
    }
}