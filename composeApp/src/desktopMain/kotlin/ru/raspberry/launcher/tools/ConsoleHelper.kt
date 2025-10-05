package ru.raspberry.launcher.tools

import java.io.File
import java.util.concurrent.TimeUnit


fun String.runCommand(
    workingDir: File = File("."),
    env : Map<String, String> = emptyMap(),
    hooks: List<(ProcessBuilder) -> Unit> = emptyList(),
    timeoutAmount: Long = 60,
    timeoutUnit: TimeUnit = TimeUnit.SECONDS,
) : Process? = runCatching {
    val builder = ProcessBuilder(*split(" ").toTypedArray())
        .directory(workingDir)
        .redirectErrorStream(true)
    builder.environment().putAll(env)
    hooks.forEach { it(builder) }
    val process = builder.start()
    Thread {
        process.inputStream.bufferedReader().useLines { lines ->
            lines.forEach { line -> println(line) }
        }
    }.start()
    process.also { it.waitFor(timeoutAmount, timeoutUnit) }
}.onFailure { it.printStackTrace() }.getOrNull()

fun String.runCommandWithoutTimeout(
    workingDir: File = File("."),
    env : Map<String, String> = emptyMap(),
    hooks: List<(ProcessBuilder) -> Unit> = emptyList(),
) : Process? = runCatching {
    val builder = ProcessBuilder(*split(" ").toTypedArray())
        .directory(workingDir)
        .redirectErrorStream(true)
    builder.environment().putAll(env)
    hooks.forEach { it(builder) }
    val process = builder.start()
    Thread {
        process.inputStream.bufferedReader().useLines { lines ->
            lines.forEach { line -> println(line) }
        }
    }.start()
    process
}.onFailure { it.printStackTrace() }.getOrNull()

fun String.runCommandWithResult(
    workingDir: File = File("."),
    env : Map<String, String> = emptyMap(),
    hooks: List<(ProcessBuilder) -> Unit> = emptyList(),
    timeoutAmount: Long = 60,
    timeoutUnit: TimeUnit = TimeUnit.SECONDS,
): String? = runCatching {
    val builder = ProcessBuilder(*split(" ").toTypedArray())
        .directory(workingDir)
        .redirectErrorStream(true)
    builder.environment().putAll(env)
    hooks.forEach { it(builder) }
    val process = builder.start()
    Thread {
        process.inputStream.bufferedReader().useLines { lines ->
            lines.forEach { line -> println(line) }
        }
    }.start()
    process.also { it.waitFor(timeoutAmount, timeoutUnit) }
        .inputStream.bufferedReader().readText()
}.onFailure { it.printStackTrace() }.getOrNull()

fun List<String>.runCommand(
    workingDir: File = File("."),
    env : Map<String, String> = emptyMap(),
    hooks: List<(ProcessBuilder) -> Unit> = emptyList(),
    timeoutAmount: Long = 60,
    timeoutUnit: TimeUnit = TimeUnit.SECONDS,
) : Process? = runCatching {
    val builder = ProcessBuilder(this)
        .directory(workingDir)
        .redirectErrorStream(true)
    builder.environment().putAll(env)
    hooks.forEach { it(builder) }
    val process = builder.start()
    Thread {
        process.inputStream.bufferedReader().useLines { lines ->
            lines.forEach { line -> println(line) }
        }
    }.start()
    process.also { it.waitFor(timeoutAmount, timeoutUnit) }
}.onFailure { it.printStackTrace() }.getOrNull()

fun List<String>.runCommandWithoutTimeout(
    workingDir: File = File("."),
    env : Map<String, String> = emptyMap(),
    hooks: List<(ProcessBuilder) -> Unit> = emptyList(),
) : Process? = runCatching {
    val builder = ProcessBuilder(this)
        .directory(workingDir)
        .redirectErrorStream(true)
    builder.environment().putAll(env)
    hooks.forEach { it(builder) }
    val process = builder.start()
    Thread {
        process.inputStream.bufferedReader().useLines { lines ->
            lines.forEach { line -> println(line) }
        }
    }.start()
    process
}.onFailure { it.printStackTrace() }.getOrNull()

fun List<String>.runCommandWithResult(
    workingDir: File = File("."),
    env : Map<String, String> = emptyMap(),
    hooks: List<(ProcessBuilder) -> Unit> = emptyList(),
    timeoutAmount: Long = 60,
    timeoutUnit: TimeUnit = TimeUnit.SECONDS,
): String? = runCatching {
    val builder = ProcessBuilder(this)
        .directory(workingDir)
        .redirectErrorStream(true)
    builder.environment().putAll(env)
    hooks.forEach { it(builder) }
    val process = builder.start()
    Thread {
        process.inputStream.bufferedReader().useLines { lines ->
            lines.forEach { line -> println(line) }
        }
    }.start()
    process.also { it.waitFor(timeoutAmount, timeoutUnit) }
        .inputStream.bufferedReader().readText()
}.onFailure { it.printStackTrace() }.getOrNull()