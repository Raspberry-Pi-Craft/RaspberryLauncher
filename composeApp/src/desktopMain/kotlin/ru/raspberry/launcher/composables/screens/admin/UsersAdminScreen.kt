package ru.raspberry.launcher.composables.screens.admin

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SearchBar
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.ui.tooling.preview.Preview
import ru.raspberry.launcher.models.WindowData
import ru.raspberry.launcher.windows.MainWindowScreens

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
fun UsersAdminScreen(state: WindowData<MainWindowScreens>) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
    ) {
        var searchQuery by remember { mutableStateOf("") }
        var expanded by remember { mutableStateOf(false) }
        SearchBar(
            inputField = {
                TextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    singleLine = true,
                    placeholder = {
                        Text(state.translation("admin.users.search", "Search users..."))
                    },
                    modifier = Modifier.fillMaxSize(),
                )
            },
            expanded = expanded,
            onExpandedChange = {
                expanded = it
            },
            modifier = Modifier
        ) {

        }
    }
}