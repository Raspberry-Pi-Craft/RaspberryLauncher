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
        .redirectOutput(ProcessBuilder.Redirect.INHERIT)
        .redirectError(ProcessBuilder.Redirect.INHERIT)
    builder.environment().putAll(env)
    hooks.forEach { it(builder) }
    builder.start().also { it.waitFor(timeoutAmount, timeoutUnit) }
}.onFailure { it.printStackTrace() }.getOrNull()

fun String.runCommandWithoutTimeout(
    workingDir: File = File("."),
    env : Map<String, String> = emptyMap(),
    hooks: List<(ProcessBuilder) -> Unit> = emptyList(),
) : Process? = runCatching {
    val builder = ProcessBuilder(*split(" ").toTypedArray())
        .directory(workingDir)
        .redirectOutput(ProcessBuilder.Redirect.INHERIT)
        .redirectError(ProcessBuilder.Redirect.INHERIT)
    builder.environment().putAll(env)
    hooks.forEach { it(builder) }
    builder.start()
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
        .redirectOutput(ProcessBuilder.Redirect.INHERIT)
        .redirectError(ProcessBuilder.Redirect.INHERIT)
    builder.environment().putAll(env)
    hooks.forEach { it(builder) }
    builder.start().also { it.waitFor(timeoutAmount, timeoutUnit) }
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
        .redirectOutput(ProcessBuilder.Redirect.INHERIT)
        .redirectError(ProcessBuilder.Redirect.INHERIT)
    builder.environment().putAll(env)
    hooks.forEach { it(builder) }
    builder.start().also { it.waitFor(timeoutAmount, timeoutUnit) }
}.onFailure { it.printStackTrace() }.getOrNull()

fun List<String>.runCommandWithoutTimeout(
    workingDir: File = File("."),
    env : Map<String, String> = emptyMap(),
    hooks: List<(ProcessBuilder) -> Unit> = emptyList(),
) : Process? = runCatching {
    val builder = ProcessBuilder(this)
        .directory(workingDir)
        .redirectOutput(ProcessBuilder.Redirect.INHERIT)
        .redirectError(ProcessBuilder.Redirect.INHERIT)
    builder.environment().putAll(env)
    hooks.forEach { it(builder) }
    builder.start()
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
        .redirectOutput(ProcessBuilder.Redirect.INHERIT)
        .redirectError(ProcessBuilder.Redirect.INHERIT)
    builder.environment().putAll(env)
    hooks.forEach { it(builder) }
    builder.start().also { it.waitFor(timeoutAmount, timeoutUnit) }
        .inputStream.bufferedReader().readText()
}.onFailure { it.printStackTrace() }.getOrNull()