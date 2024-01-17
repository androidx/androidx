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
        var libraryType: String? = null
        var publish: String? = null
        var specifiesVersion = false
        fileLines.forEach { line ->
            if (libraryType == null) libraryType = line.extractVariableValue(" type = LibraryType.")
            if (publish == null) publish = line.extractVariableValue(" publish = Publish.")
            if (line.contains("mavenVersion =")) specifiesVersion = true
        }
        val libraryTypeEnum = libraryType?.let { LibraryType.valueOf(it) } ?: LibraryType.UNSET
        val publishEnum = publish?.let { Publish.valueOf(it) } ?: Publish.UNSET
        return ParsedProject(
            libraryType = libraryTypeEnum,
            publish = publishEnum,
            specifiesVersion = specifiesVersion
        )
    }

    data class ParsedProject(
        val libraryType: LibraryType,
        val publish: Publish,
        val specifiesVersion: Boolean
    ) {
        fun shouldPublish(): Boolean =
            if (publish != Publish.UNSET) {
                publish.shouldPublish()
            } else if (libraryType != LibraryType.UNSET) {
                libraryType.publish.shouldPublish()
            } else {
                false
            }

        fun shouldRelease(): Boolean =
            if (publish != Publish.UNSET) {
                publish.shouldRelease()
            } else if (libraryType != LibraryType.UNSET) {
                libraryType.publish.shouldRelease()
            } else {
                false
            }
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
    if (buildFile.path.contains("compose/material/material-icons-extended-")) {
        // These projects all read from this Gradle script
        return parseBuildFile(
            File(buildFile.parentFile.parentFile, "material-icons-extended/generate.gradle")
        )
    }
    val parserProvider =
        project.rootProject.gradle.sharedServices.registerIfAbsent(
            "ProjectParser",
            ProjectParser::class.java
        ) {}
    val parser = parserProvider.get()
    return parser.get(buildFile)
}
