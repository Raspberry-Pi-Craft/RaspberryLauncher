package ru.raspberry.launcher.models

import kotlinx.serialization.Serializable

@Serializable
enum class ChangeAction {
    Add,
    Remove,
    Change
}