package ru.raspberry.launcher.tools.jna

import com.sun.jna.Platform
import ru.raspberry.launcher.models.Arch
import ru.raspberry.launcher.models.OS

object JNA {
    var isEnabled: Boolean

    init {
        var enabled = true
        try {
            Class.forName("com.sun.jna.Platform")
        } catch (e: ClassNotFoundException) {
            enabled = false
        }
        isEnabled = enabled
    }

    val is64Bit: Boolean? = if (isEnabled) Platform.is64Bit() else null
    val isARM: Boolean? = if (isEnabled) Platform.isARM() else null

    fun arch(): Arch? {
        if (!isEnabled) {
            return null
        }
        return if (Platform.isARM())
            if (Platform.is64Bit()) Arch.Arm64 else Arch.Arm
        else if (Platform.is64Bit()) Arch.X64 else Arch.X86
    }

    val currentOs: OS?
        get() {
            if (!isEnabled) {
                return null
            }
            var current = OS.Unknown
            if (Platform.isWindows()) {
                current = OS.Windows
            } else if (Platform.isLinux()) {
                current = OS.Linux
            } else if (Platform.isMac()) {
                current = OS.OSX
            }
            return current
        }

    val arch: String? = if (isEnabled) Platform.ARCH else null
}
