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

package androidx.build

import java.io.File
import java.util.concurrent.ConcurrentHashMap
import org.gradle.api.Project
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters

abstract class ProjectParser : BuildService<BuildServiceParameters.None> {
    @Transient val cache: MutableMap<File, ParsedProject> = ConcurrentHashMap()

    fun get(buildFile: File): ParsedProject {
        return cache.getOrPut(key = buildFile) {
            val text = buildFile.readLines()
            parseProject(text)
        }
    }

    private fun parseProject(fileLines: List<String>): ParsedProject {
        var softwareType: String? = null
        var publish: String? = null
        var specifiesVersion = false
        fileLines.forEach { line ->
            if (softwareType == null)
                softwareType = line.extractVariableValue(" type = SoftwareType.")
            if (publish == null) publish = line.extractVariableValue(" publish = Publish.")
            if (line.contains("mavenVersion =")) specifiesVersion = true
        }
        val softwareTypeEnum = softwareType?.let { SoftwareType.valueOf(it) } ?: SoftwareType.UNSET
        return ParsedProject(softwareType = softwareTypeEnum, specifiesVersion = specifiesVersion)
    }

    data class ParsedProject(val softwareType: SoftwareType, val specifiesVersion: Boolean) {
        fun shouldPublish(): Boolean = softwareType.publish.shouldPublish()

        fun shouldRelease(): Boolean = softwareType.publish.shouldRelease()
    }
}

private fun String.extractVariableValue(prefix: String): String? {
    val declarationIndex = this.indexOf(prefix)
    if (declarationIndex >= 0) {
        val suffix = this.substring(declarationIndex + prefix.length)
        val spaceIndex = suffix.indexOf(" ")
        if (spaceIndex > 0) return suffix.substring(0, spaceIndex)
        return suffix
    }
    return null
}

fun Project.parse(): ProjectParser.ParsedProject {
    return parseBuildFile(project.buildFile)
}

fun Project.parseBuildFile(buildFile: File): ProjectParser.ParsedProject {
    val parserProvider =
        project.gradle.sharedServices.registerIfAbsent(
            "ProjectParser",
            ProjectParser::class.java
        ) {}
    val parser = parserProvider.get()
    return parser.get(buildFile)
}
