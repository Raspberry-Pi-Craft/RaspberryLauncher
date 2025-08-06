package ru.raspberry.launcher.models.repo.library

import kotlinx.serialization.Serializable
import ru.raspberry.launcher.models.repo.ExtractRules
import ru.raspberry.launcher.models.repo.Rule
import java.util.*
import java.util.regex.Pattern

@Serializable
data class LibraryReplace(
    val replaces: String? = null,
    val args: String = "",
    val mainClass: String? = null,
    val requires: List<Library> = emptyList(),
    val supports: List<String> = emptyList(),

    val name: String,
    val url: String? = null,
    val exact_url: String? = null,
    val checksum: String? = null,
    val rules: List<Rule> = emptyList(),
    var extract: ExtractRules? = null,
    val downloads: LibraryDownloadInfo? = null,
) {
    fun pattern(): Pattern? = if (replaces == null) null else Pattern.compile(replaces)

    fun replaces(lib: Library): Boolean {
        return replaces != null && (pattern()?.matcher(lib.name)?.matches() ?: true)
    }
    @Transient
    val library: Library = Library(
        name = name,
        url = url,
        exact_url = exact_url,
        checksum = checksum,
        rules = rules,
        extract = extract,
        downloads = downloads
    )
}
