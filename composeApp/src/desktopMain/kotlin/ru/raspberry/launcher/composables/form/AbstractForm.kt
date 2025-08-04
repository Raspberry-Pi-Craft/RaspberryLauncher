package ru.raspberry.launcher.composables.form

import androidx.compose.runtime.Composable

interface AbstractForm<K> {
    @Composable
    fun Add(): () -> Unit
    @Composable
    fun Update(key: K): () -> Unit
}