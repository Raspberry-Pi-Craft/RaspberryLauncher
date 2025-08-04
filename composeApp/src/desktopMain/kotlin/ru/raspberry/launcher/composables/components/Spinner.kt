package ru.raspberry.launcher.composables.components

import androidx.compose.material.DropdownMenuItem
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import kotlin.collections.associateWith
import kotlin.math.max
import kotlin.math.min


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun<T> Spinner(
    label: @Composable () -> Unit,
    options: Iterable<T>,
    selectedOption: T,
    onOptionSelected: (T) -> Unit,
    modifier: Modifier = Modifier,
    readOnly: Boolean = false,
    isError: Boolean = false,
    supportingText: @Composable (() -> Unit)? = null,
    toText: (T?) -> String = { it?.toString() ?: "None" },
    searchable: Boolean = false,
) {
    fun tokenize(text: String) : Set<String> {
        val set = mutableSetOf<String>()
        val dim = 3
        if (text.length < dim)
            set.add(text.lowercase())
        else for (i in 0..text.length - dim)
            set.add(text.substring(i, i + dim))
        return set
    }
    var expanded by remember { mutableStateOf(false) }
    val optionsTokens = remember { options.associateWith { tokenize(toText(it)) } }
    var text by remember { mutableStateOf( toText(selectedOption) ) }
    val queryTokens = remember(text) { tokenize(text.lowercase()) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = {
            if (!readOnly) expanded = it
        },
        modifier = modifier
    ) {
        TextField(
            modifier = modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable, true),
            value = text,
            onValueChange = {
                text = it
            },
            readOnly = !searchable,
            singleLine = true,
            label = label,
            isError = isError,
            supportingText = supportingText,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            val queried = if (searchable) options.sortedBy {
                val tokens = optionsTokens[it]
                if (tokens == null) Int.MAX_VALUE
                else {
                    queryTokens.size - tokens.intersect(queryTokens).size
                }
            } else options
            queried.forEach { option ->
                val optionText = toText(option)
                DropdownMenuItem(
                    onClick = {
                        text = optionText
                        expanded = false
                        onOptionSelected(option)
                    }
                ) {
                    Text(optionText, style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
    }
}