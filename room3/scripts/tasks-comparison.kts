/*
 * Copyright 2023 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.BufferedReader
import java.io.File
import java.util.concurrent.TimeUnit

val currentDir = File(".").absolutePath
check(currentDir.endsWith("/frameworks/support/.")) {
    "Script needs to be executed from '<check-out>/frameworks/support', was '$currentDir'."
}
val scriptDir = File(currentDir, "room/scripts")

check(args.size >= 6) { "Expected at least 6 args. See usage instructions."}
val taskIds = args.count { it == "-t" }
if (taskIds != 2) {
    error("Exactly two tags are required per invocation. Found $taskIds")
}

val firstTagIndex = args.indexOfFirst { it == "-t" } + 1
val firstTag = args[firstTagIndex]
val firstTasks = extractTasks(firstTagIndex, args)
check(firstTasks.isNotEmpty()) { "Task list for a tag must not be empty." }

val secondTagIndex = args.indexOfLast { it == "-t" } + 1
val secondTag = args[secondTagIndex]
val secondTasks = extractTasks(secondTagIndex, args)
check(secondTasks.isNotEmpty()) { "Task list for a tag must not be empty." }

println("Comparing tasks groups!")
println("First tag: $firstTag")
println("Task list:\n${firstTasks.joinToString(separator = "\n")}")
println("Second tag: $secondTag")
println("Task list\n${secondTasks.joinToString(separator = "\n")}")

cleanBuild(firstTasks)
val firstResult = profile(firstTag, firstTasks)

cleanBuild(secondTasks)
val secondResult = profile(secondTag, secondTasks)

crunchNumbers(firstResult)
crunchNumbers(secondResult)

fun extractTasks(tagIndex: Int, args: Array<String>): List<String> {
   return buildList {
       for (i in (tagIndex + 1) until args.size) {
           if (args[i] == "-t") {
               break
           }
           add(args[i])
       }
   }
}

fun cleanBuild(tasks: List<String>) {
    println("Running initial build to cook cache...")
    runCommand("./gradlew --stop")
    runCommand("./gradlew ${tasks.joinToString(separator = " ")}")
}

fun profile(
    tag: String,
    tasks: List<String>,
    amount: Int = 10
): ProfileResult {
    println("Profiling tasks for '$tag'...")
    val allRunTimes = List(amount) { runNumber ->
        val profileCmd = buildString {
            append("./gradlew ")
            append("--init-script $scriptDir/rerun-requested-task-init-script.gradle ")
            append("--no-configuration-cache ")
            append("--profile ")
            append(tasks.joinToString(separator = " "))
        }
        val reportPath = runCommand(profileCmd, returnOutputStream = true)?.use { stream ->
            stream.lineSequence().forEach { line ->
                if (line.startsWith("See the profiling report at:")) {
                    val scheme = "file://"
                    return@use line.substring(
                        line.indexOf(scheme) + scheme.length
                    )
                }
            }
            return@use null
        }
        checkNotNull(reportPath) { "Couldn't get report path!" }
        println("Result at: $reportPath")
        val taskTimes = mutableMapOf<String, Float>()
        File(reportPath).bufferedReader().use { reader ->
            while (true) {
                val line = reader.readLine()
                if (line == null) {
                    return@use
                }
                tasks.forEach { taskName ->
                    if (line.contains(">$taskName<")) {
                        val timeValue = checkNotNull(reader.readLine())
                            .drop("<td class=\"numeric\">".length)
                            .let { it.substring(0, it.indexOf("s</td>")) }
                            .toFloat()
                        taskTimes[taskName] = taskTimes.getOrDefault(taskName, 0.0f) + timeValue
                    }
                }
            }
        }
        println("Result of run #${runNumber + 1} of '$tag':")
        taskTimes.forEach { taskName, time ->
            println("$time - $taskName")
        }
        return@List taskTimes
    }
    return ProfileResult(tag, allRunTimes)
}

fun crunchNumbers(result: ProfileResult) {
    println("--------------------")
    println("Summary of profile for '${result.tag}'")
    println("--------------------")
    println("Total time (${result.numOfRuns} runs):")
    println("  Min: ${result.minTotal()}")
    println("  Avg: ${result.avgTotal()}")
    println("  Max: ${result.maxTotal()}")
    println("Per task times:")
    result.tasks.forEach { taskName ->
        println("  $taskName")
        println(buildString {
            append("  Min: ${result.minTask(taskName)}")
            append("  Avg: ${result.avgTask(taskName)}")
            append("  Max: ${result.maxTask(taskName)}")
        })
    }
}

fun runCommand(
    command: String,
    workingDir: File = File("."),
    timeoutAmount: Long = 60,
    timeoutUnit: TimeUnit = TimeUnit.SECONDS,
    returnOutputStream: Boolean = false
): BufferedReader? = runCatching {
    println("Executing: $command")
    val proc = ProcessBuilder("\\s".toRegex().split(command))
        .directory(workingDir)
        .apply {
            if (returnOutputStream) {
                redirectOutput(ProcessBuilder.Redirect.PIPE)
            } else {
                redirectOutput(ProcessBuilder.Redirect.INHERIT)
            }
        }
        .redirectError(ProcessBuilder.Redirect.INHERIT)
        .start()
    proc.waitFor(timeoutAmount, timeoutUnit)
    if (proc.exitValue() != 0) {
        error("Non-zero exit code received: ${proc.exitValue()}")
    }
    return if (returnOutputStream) {
        proc.inputStream.bufferedReader()
    } else {
        null
    }
}.onFailure { it.printStackTrace() }.getOrNull()

data class ProfileResult(
    val tag: String,
    private val taskTimes: List<Map<String, Float>>
) {
    val numOfRuns = taskTimes.size
    val tasks = taskTimes.first().keys

    fun minTotal(): Float = taskTimes.minOf { it.values.sum() }

    fun avgTotal(): Float = taskTimes.map { it.values.sum() }.sum() / taskTimes.size

    fun maxTotal(): Float = taskTimes.maxOf { it.values.sum() }

    fun minTask(name: String): Float = taskTimes.minOf { it.getValue(name) }

    fun avgTask(name: String): Float = taskTimes.map { it.getValue(name) }.sum() / taskTimes.size

    fun maxTask(name: String): Float = taskTimes.maxOf { it.getValue(name) }
}