package ru.raspberry.launcher.tools

import java.io.File
import java.util.concurrent.TimeUnit


fun String.runCommand(
    workingDir: File = File("."),
    timeoutAmount: Long = 60,
    timeoutUnit: TimeUnit = TimeUnit.SECONDS
) = runCatching {
    ProcessBuilder(*split(" ").toTypedArray())
        .directory(workingDir)
        .redirectOutput(ProcessBuilder.Redirect.INHERIT)
        .redirectError(ProcessBuilder.Redirect.INHERIT)
        .start().also { it.waitFor(timeoutAmount, timeoutUnit) }
}.onFailure { it.printStackTrace() }

fun String.runCommandWithoutTimeout(
    workingDir: File = File(".")
) = runCatching {
    ProcessBuilder(*split(" ").toTypedArray())
        .directory(workingDir)
        .redirectOutput(ProcessBuilder.Redirect.INHERIT)
        .redirectError(ProcessBuilder.Redirect.INHERIT)
        .start()
}.onFailure { it.printStackTrace() }

fun String.runCommandWithResult(
    workingDir: File = File("."),
    timeoutAmount: Long = 60,
    timeoutUnit: TimeUnit = TimeUnit.SECONDS
): String? = runCatching {
    ProcessBuilder("\\s".toRegex().split(this))
        .directory(workingDir)
        .redirectOutput(ProcessBuilder.Redirect.PIPE)
        .redirectError(ProcessBuilder.Redirect.PIPE)
        .start().also { it.waitFor(timeoutAmount, timeoutUnit) }
        .inputStream.bufferedReader().readText()
}.onFailure { it.printStackTrace() }.getOrNull()


class CommandBuilder(private val command: String) {
    private val args = mutableListOf<String>()

    fun addArg(arg: String): CommandBuilder {
        args.add(arg)
        return this
    }
    fun addVariableArg(arg: String, value: String): CommandBuilder {
        args.add("--$arg=$value")
        return this
    }
    fun addShortVariableArg(arg: String, value: String): CommandBuilder {
        args.add("-$arg=$value")
        return this
    }


    fun build(): String {
        return "$command ${args.joinToString(" ")}"
    }

    fun runCommand(
        workingDir: File = File("."),
        timeoutAmount: Long = 60,
        timeoutUnit: TimeUnit = TimeUnit.SECONDS
    ) = build().runCommand(workingDir, timeoutAmount, timeoutUnit)
    fun runCommandWithoutTimeout(
        workingDir: File = File(".")
    ) = build().runCommandWithoutTimeout(workingDir)
    fun runCommandWithResult(
        workingDir: File = File("."),
        timeoutAmount: Long = 60,
        timeoutUnit: TimeUnit = TimeUnit.SECONDS
    ) = build().runCommandWithResult(workingDir, timeoutAmount, timeoutUnit)
}