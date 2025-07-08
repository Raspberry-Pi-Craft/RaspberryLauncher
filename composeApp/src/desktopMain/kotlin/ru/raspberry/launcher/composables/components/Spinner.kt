package ru.raspberry.launcher.composables.components

import androidx.compose.material.DropdownMenuItem
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun<T> Spinner(
    label: String,
    options: List<T>,
    selectedOption: Int,
    onOptionSelected: (T) -> Unit,
    toText: @Composable (T?) -> String,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    val tex = toText(options.getOrNull(selectedOption))
    var text by remember { mutableStateOf(tex) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier
    ) {
        TextField(
            modifier = modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable, true),
            value = text,
            onValueChange = {},
            readOnly = true,
            singleLine = true,
            label = { Text(text = label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            options.forEach { option ->
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