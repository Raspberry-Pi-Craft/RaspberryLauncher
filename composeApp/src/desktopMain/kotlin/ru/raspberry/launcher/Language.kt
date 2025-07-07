package ru.raspberry.launcher

import java.util.Locale

enum class  Language(
    val displayName: String,
    val englishName: String,
    val locale: Locale
) {
    Russian("Русский", "Russian", Locale.of("rus")),
    English("English", "English", Locale.of("en")),
}