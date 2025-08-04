package ru.raspberry.launcher.service.repositories

import ru.raspberry.launcher.service.AsyncRepository

class MapChangeRepository<K, U, D, A>(
    val data: Map<K, D>,
    val changes: MutableMap<K, U>,
    val addToUpdate: (A) -> U,
    val addToKey: (A) -> K,
    val removeToUpdate: (K) -> U,
) : AsyncRepository<K, U, D, A> {
    override suspend fun list(): List<K> = data.keys.toList()
    override suspend fun get(key: K): D? = data[key]


    override suspend fun add(data: A): Boolean {
        if (addToKey(data) in this.data) return false
        changes[addToKey(data)] = addToUpdate(data)
        return true
    }

    override suspend fun edit(
        key: K,
        changes: U
    ): Boolean {
        if (key !in this.data) return false
        this.changes[key] = changes
        return true
    }

    override suspend fun remove(key: K): Boolean {
        if (key !in this.data) return false
        changes[key] = removeToUpdate(key)
        return true
    }
}