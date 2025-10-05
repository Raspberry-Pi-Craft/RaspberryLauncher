package ru.raspberry.launcher.util

import java.io.OutputStream

class MultiOutputStream(
    private val out: Collection<OutputStream>
) : OutputStream() {
    override fun write(b: Int) = out.forEach { it.write(b) }
    override fun write(b: ByteArray) = out.forEach { it.write(b) }
    override fun write(b: ByteArray, off: Int, len: Int) = out.forEach { it.write(b, off, len) }
    override fun flush() = out.forEach { it.flush() }
    override fun close() = out.forEach { it.close() }
}