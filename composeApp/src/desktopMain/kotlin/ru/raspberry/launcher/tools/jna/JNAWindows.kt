package ru.raspberry.launcher.tools.jna

import com.sun.jna.Platform
import com.sun.jna.platform.win32.Advapi32Util
import com.sun.jna.platform.win32.Kernel32
import com.sun.jna.platform.win32.WinNT.OSVERSIONINFOEX
import com.sun.jna.platform.win32.WinReg.HKEY
import ru.raspberry.launcher.tools.jna.JNA.isEnabled
import ru.raspberry.launcher.util.Lazy
import java.util.*
import java.util.concurrent.Callable

object JNAWindows {
    private val OSVERSIONINFOEX: Lazy<*> = Lazy.of<Any?>(Callable {
        val vex = OSVERSIONINFOEX()
        if (Kernel32.INSTANCE.GetVersionEx(vex)) vex as Any else null
    })

    private val BUILD_NUMBER: Lazy<Int?> =
        Lazy.of<Int?>(Callable { (OSVERSIONINFOEX.get() as OSVERSIONINFOEX).getBuildNumber() }
        )

    private val REGISTRY: Lazy<Registry?> =
        Lazy.of<Registry?>(Callable { if (Platform.isWindows()) Registry() else null })

    val buildNumber: Int?
        get() = if (isEnabled) BUILD_NUMBER.value() else null

    val registry: Registry?
        get() = if (isEnabled) REGISTRY.value() else null

    class Registry constructor() {
        @Throws(JNAException::class)
        fun exists(root: HKEY?, key: String?): Boolean {
            try {
                return Advapi32Util.registryKeyExists(root, key)
            } catch (e: Exception) {
                throw JNAException(e)
            }
        }

        @Throws(JNAException::class)
        fun exists(root: HKEY?, key: String?, name: String?): Boolean {
            try {
                return Advapi32Util.registryValueExists(root, key, name)
            } catch (e: Exception) {
                throw JNAException(e)
            }
        }

        @Throws(JNAException::class)
        fun getString(root: HKEY?, key: String?, name: String?): String? {
            try {
                if (!exists(root, key, name)) {
                    return null
                }
                return Advapi32Util.registryGetStringValue(root, key, name)
            } catch (e: JNAException) {
                throw e
            } catch (e: Exception) {
                throw JNAException(e)
            }
        }

        @Throws(JNAException::class)
        fun setString(root: HKEY?, key: String?, name: String?, value: String?) {
            try {
                if (!exists(root, key)) {
                    Advapi32Util.registryCreateKey(root, key)
                }
                Advapi32Util.registrySetStringValue(root, key, name, value)
            } catch (e: JNAException) {
                throw e
            } catch (e: Exception) {
                throw JNAException(e)
            }
        }
    }
}
