package ru.raspberry.launcher.models.minecraft

import kotlinx.serialization.Serializable

@Serializable
enum class MinecraftLoader {
    Vanilla,
    Fabric,
    Forge,
    Quilt,
    NeoForge,
    OptiFine,
    MultiMC,
    Unknown
}