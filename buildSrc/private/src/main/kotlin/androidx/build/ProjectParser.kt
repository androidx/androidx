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

import com.autonomousapps.grammar.gradle.GradleScriptLexer
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.Token
import org.gradle.api.Project
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters

abstract class ProjectParser : BuildService<BuildServiceParameters.None> {
    @Transient val cache: MutableMap<File, ParsedProject> = ConcurrentHashMap()

    fun get(buildFile: File): ParsedProject {
        return cache.getOrPut(key = buildFile) { parseProject(buildFile.readText()) }
    }

    data class ParsedProject(
        val softwareType: SoftwareType = SoftwareType.UNSET,
        val specifiesVersion: Boolean = false,
        val singleQuoteViolations: List<String> = emptyList(),
    ) {
        fun shouldPublish(): Boolean = softwareType.publish.shouldPublish()

        fun shouldRelease(): Boolean = softwareType.publish.shouldRelease()
    }

    companion object {
        fun parseProject(text: String, fileName: String = "build.gradle"): ParsedProject {
            val lexer = GradleScriptLexer(CharStreams.fromString(text, fileName))
            val violations = mutableListOf<String>()
            var inQuotes = false
            var prevType = 0
            var softwareType: SoftwareType = SoftwareType.UNSET
            var specifiesVersion = false

            for (token in lexer.tokens()) {
                when (token.type) {
                    GradleScriptLexer.QUOTE_DOUBLE -> {
                        if (prevType != GradleScriptLexer.BACKSLASH) inQuotes = !inQuotes
                    }
                    GradleScriptLexer.QUOTE_SINGLE -> {
                        if (!inQuotes) {
                            violations += "line ${token.line}:${token.charPositionInLine + 1}"
                        }
                    }
                    else ->
                        if (!inQuotes) {
                            if (token.text.startsWith("SoftwareType.")) {
                                softwareType =
                                    SoftwareType.valueOf(token.text.removePrefix("SoftwareType."))
                            } else if (token.text == "mavenVersion") {
                                specifiesVersion = true
                            }
                        }
                }
                prevType = token.type
            }

            return ParsedProject(softwareType, specifiesVersion, violations)
        }

        private fun GradleScriptLexer.tokens(): Sequence<Token> = generateSequence {
            nextToken().takeUnless { it.type == GradleScriptLexer.EOF }
        }
    }
}

fun Project.parse(): ProjectParser.ParsedProject {
    return parseBuildFile(project.buildFile)
}

fun Project.parseBuildFile(buildFile: File): ProjectParser.ParsedProject {
    val parserProvider =
        project.gradle.sharedServices.registerIfAbsent(
            "ProjectParser",
            ProjectParser::class.java,
        ) {}
    val parser = parserProvider.get()
    return parser.get(buildFile)
}
