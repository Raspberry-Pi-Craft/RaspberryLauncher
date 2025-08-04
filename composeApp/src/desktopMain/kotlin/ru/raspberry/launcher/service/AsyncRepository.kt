package ru.raspberry.launcher.service

interface AsyncRepository<K, U, D, A> {
    suspend fun list() : List<K>
    suspend fun get(key: K): D?
    suspend fun add(data: A): Boolean
    suspend fun edit(key: K, changes: U): Boolean
    suspend fun remove(key: K): Boolean
}