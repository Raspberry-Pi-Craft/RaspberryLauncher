package ru.raspberry.launcher.util

import java.util.*
import java.util.concurrent.Callable


class Lazy<T> private constructor(// gc collectable
    private var initializer: Callable<T?>?
) : Callable<T?> {
    var isInitialized: Boolean = false
        private set
    private var exception: Throwable? = null
    private var value: T? = null

    @Synchronized
    @Throws(LazyInitException::class)
    fun get(): T? {
        if (exception != null) {
            throw LazyInitException(exception)
        }
        if (this.isInitialized) {
            return value
        } else {
            this.isInitialized = true
        }
        var value: T?
        try {
            value = initializer!!.call()
        } catch (e: Exception) {
            exception = e
            throw LazyInitException(e)
        } catch (e: Throwable) {
            exception = e
            throw LazyInitException(e)
        } finally {
            initializer = null
        }
        return value.also { this.value = it }
    }

    @Throws(Exception::class)
    override fun call(): T? {
        try {
            return get()
        } catch (e: LazyInitException) {
            val cause = e.cause
            if (cause is Exception) {
                throw cause
            } else {
                throw Error(cause) // should never happen
            }
        }
    }

    fun value(): T? {
        val value: T?
        try {
            value = get()
        } catch (e: LazyInitException) {
            return null
        }
        return value
    }

    fun valueIfInitialized(): T? = if (this.isInitialized) value() else null

    override fun toString(): String {
        // we don't really care here about a race condition
        return "Lazy{" +
                (if (this.isInitialized) "value=$value" else "initializer=$initializer") +
                '}'
    }

    companion object {
        fun <T> of(callable: Callable<T?>?): Lazy<T?> {
            return Lazy(Objects.requireNonNull<Callable<T?>?>(callable))
        }
    }
}
