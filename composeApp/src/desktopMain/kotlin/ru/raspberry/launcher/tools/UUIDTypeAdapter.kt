package ru.raspberry.launcher.tools

import java.util.*

object UUIDTypeAdapter {
    fun toUUID(value: String?): String? {
        return value?.replace("-", "")
    }

    fun fromUUID(value: UUID?): String? {
        return toUUID(value?.toString())
    }

    fun fromString(input: String): UUID {
        return UUID.fromString(
            input.replaceFirst(
                "(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})".toRegex(),
                "$1-$2-$3-$4-$5"
            )
        )
    }
}
