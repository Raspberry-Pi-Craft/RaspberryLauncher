package ru.raspberry.launcher.windows.dialogs.admin

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import kotlinx.coroutines.runBlocking
import ru.raspberry.launcher.models.WindowData
import ru.raspberry.launcher.service.AsyncRepository
import ru.raspberry.launcher.windows.MainWindowScreens
import ru.raspberry.launcher.windows.dialogs.TwoVariantDialog


@Composable
private fun<K, U, D, A> RepositoryRemoveDialog(
    repository: AsyncRepository<K, U, D, A>,
    state: WindowData<MainWindowScreens>,
    visible: MutableState<Boolean>,
    selected: K?,
    reload: () -> Unit,
) {
    if (selected == null) return
    TwoVariantDialog(
        state,
        { visible.value = false },
        "Delete Confirmation",
        {
            Text("Are you sure you want to delete this?")
        },
        {
            Text("Delete")
        },
        {
            runBlocking {
                repository.remove(selected)
                reload()
            }
            visible.value = false
        },
        {
            Text("Cancel")
        },
        {
            visible.value = false
        },
        reload,
    )
}