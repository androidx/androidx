/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.build

import java.io.File
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault

// This task prints the coordinates (group/artifact/version) of a project
@DisableCachingByDefault(because = "The purpose of this task is to print information")
abstract class PrintProjectCoordinatesTask : DefaultTask() {

    fun configureWithAndroidXExtension(androidXExtension: AndroidXExtension) {
        projectGroup = androidXExtension.mavenGroup
        groupExplanation = androidXExtension.explainMavenGroup()
        projectName = project.name
        version = project.version.toString()
        projectDir = project.projectDir.relativeTo(project.rootDir)
        projectPath = project.path
    }

    @Internal // Task is always out-of-date: no need to track inputs
    var projectGroup: LibraryGroup? = null

    @Internal // Task is always out-of-date: no need to track inputs
    var groupExplanation: List<String>? = null

    @Internal // Task is always out-of-date: no need to track inputs
    var projectName: String? = null

    @Internal // Task is always out-of-date: no need to track inputs
    var version: String? = null

    @Internal // Task is always out-of-date: no need to track inputs
    var projectDir: File? = null

    @Internal // Task is always out-of-date: no need to track inputs
    var projectPath: String? = null

    @TaskAction
    fun printInformation() {
        val projectGroup = projectGroup
        val versionFrom =
            if (projectGroup?.atomicGroupVersion == null) {
                "build.gradle: mavenVersion"
            } else {
                "group.atomicGroupVersion"
            }

        val groupExplanation = groupExplanation!!
        val lines =
            mutableListOf(listOf("filepath: $projectDir/build.gradle ", "(from settings.gradle)"))
        // put each component of the explanation on its own line
        groupExplanation.forEachIndexed { i, component ->
            if (i == 0) lines.add(listOf("group   : ${projectGroup?.group} ", component))
            else lines.add(listOf("", component))
        }
        lines.add(listOf("artifact: $projectName ", "(from project name)"))
        lines.add(listOf("version : $version ", "(from $versionFrom)"))
        printTable(lines)
    }

    private fun printTable(lines: List<List<String>>) {
        val columnSizes = getColumnSizes(lines)
        for (line in lines) {
            println(formatRow(line, columnSizes))
        }
    }

    private fun formatRow(line: List<String>, columnSizes: List<Int>): String {
        var result = ""
        for (i in line.indices) {
            val word = line[i]
            val columnSize = columnSizes[i]
            // only have to pad columns before the last column
            result += if (i != line.size - 1) word.padEnd(columnSize) else word
        }
        return result
    }

    private fun getColumnSizes(lines: List<List<String>>): List<Int> {
        val maxLengths = mutableListOf<Int>()
        for (line in lines) {
            for (i in line.indices) {
                val word = line[i]
                if (maxLengths.size <= i) maxLengths.add(0)
                if (maxLengths[i] < word.length) maxLengths[i] = word.length
            }
        }
        return maxLengths
    }

    private fun formatTableLine(prefix: String, suffix: String): String {
        return prefix.padEnd(10) + suffix
    }
}
