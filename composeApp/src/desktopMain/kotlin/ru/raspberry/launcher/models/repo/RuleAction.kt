package ru.raspberry.launcher.models.repo

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class RuleAction {
    @SerialName("allow")
    Allow,

    @SerialName("disallow")
    Disallow
}
