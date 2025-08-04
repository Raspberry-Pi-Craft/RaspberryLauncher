package ru.raspberry.launcher.models

import kotlinx.serialization.Serializable

@Serializable
enum class Arch {
    Arm,
    Arm64,
    X64,
    X86,
}