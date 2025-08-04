package ru.raspberry.launcher.service.repositories

import ru.raspberry.launcher.service.AsyncRepository

class ListChangeRepository<K, U, D, A>(
    val data: List<D>,
    val dataToKey: (D) -> K,
    val changes: MutableMap<K, U>,
    val addToUpdate: (A) -> U,
    val addToKey: (A) -> K,
    val removeToUpdate: (K) -> U,
) : AsyncRepository<K, U, D, A> {

    // Cache to avoid recalculating keys for each data item
    private val cache = mutableMapOf<D, K>()
    private fun D.getKey(): K =
        cache.getOrPut(this) { dataToKey(this) }

    override suspend fun list(): List<K> = data.map { value -> value.getKey() }.toList()
    override suspend fun get(key: K): D? = data.find { value -> value.getKey() == key }


    override suspend fun add(data: A): Boolean {
        if (this.data.any { value -> value.getKey() == addToKey(data) }) return false
        changes[addToKey(data)] = addToUpdate(data)
        return true
    }

    override suspend fun edit(
        key: K,
        changes: U
    ): Boolean {
        if (this.data.all { value -> value.getKey() != key }) return false
        this.changes[key] = changes
        return true
    }

    override suspend fun remove(key: K): Boolean {
        if (this.data.all { value -> value.getKey() != key }) return false
        changes[key] = removeToUpdate(key)
        return true
    }
}