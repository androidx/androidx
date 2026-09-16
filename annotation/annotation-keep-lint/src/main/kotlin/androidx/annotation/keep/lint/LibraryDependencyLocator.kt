/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.annotation.keep.lint

import androidx.annotation.keep.lint.KeepAnnotationPluginDetector.Companion.KEEP_LIBRARY_COORDINATE
import androidx.annotation.keep.lint.KeepAnnotationPluginDetector.Companion.KEEP_PROJECT_PATH

/** Result of locating a keep library dependency in build script contents. */
internal data class DependencyMatch(
    val range: IntRange,
    val text: String,
)

/**
 * Locates keep library dependencies within build script text without relying on regexes.
 *
 * Handles:
 * - Inter-project dependencies (e.g. `project(":annotation:annotation-keep")`)
 * - Maven coordinates (e.g. `"androidx.annotation:annotation-keep:1.0.0"`)
 * - Version catalog accessors (e.g. `deps.annotation.keep` or `libs.annotation.keep`)
 */
internal object LibraryDependencyLocator {

    /**
     * Locates a dependency on `androidx.annotation:annotation-keep` within [contents].
     *
     * @param contents the full text of the build script
     * @param libraryAliases normalized version catalog aliases for the library (e.g.
     *   `setOf("annotation.keep")`)
     * @return the [DependencyMatch] containing the range and matched text, or `null` if not found
     */
    fun locate(contents: String, libraryAliases: Set<String> = emptySet()): DependencyMatch? {
        return findMavenCoordinate(contents)
            ?: findCatalogAccessor(contents, libraryAliases)
            ?: findProjectDependency(contents)
    }

    private fun findProjectDependency(contents: String): DependencyMatch? {
        // Project dependencies don't have version suffixes, so find the exact quoted path directly
        var startQuote = contents.indexOf("'$KEEP_PROJECT_PATH'")
        if (startQuote == -1) {
            startQuote = contents.indexOf("\"$KEEP_PROJECT_PATH\"")
        }
        if (startQuote == -1) return null
        val endQuote = startQuote + KEEP_PROJECT_PATH.length + 1

        // Attempt to do a decent job of detecting project prefix - not robust though,
        // won't handle comments for example
        val before = contents.substring(0, startQuote).trimEnd()
        val hasProject = before.endsWith("project(") || before.endsWith("project")
        val start = if (hasProject) before.lastIndexOf("project") else startQuote

        val after = contents.substring(endQuote + 1)
        val end =
            if (hasProject && after.trimStart().startsWith(")")) {
                endQuote + 1 + after.indexOf(')') + 1
            } else {
                endQuote + 1
            }

        return DependencyMatch(start until end, contents.substring(start, end))
    }

    private fun findMavenCoordinate(contents: String): DependencyMatch? {
        // Maven coordinates have a dynamic version suffix inside their quotes, so find the start
        // and
        // scan forward for the closing quote.
        for (quote in charArrayOf('\'', '"')) {
            val startQuote = contents.indexOf("$quote$KEEP_LIBRARY_COORDINATE")
            if (startQuote != -1) {
                val endQuote =
                    contents.indexOf(quote, startQuote + 1 + KEEP_LIBRARY_COORDINATE.length)
                if (endQuote != -1) {
                    return DependencyMatch(
                        startQuote until (endQuote + 1),
                        contents.substring(startQuote, endQuote + 1),
                    )
                }
            }
        }
        return null
    }

    private fun findCatalogAccessor(
        contents: String,
        libraryAliases: Set<String>,
    ): DependencyMatch? {
        for (alias in libraryAliases) {
            val aliasIndex = contents.indexOf(alias)
            if (aliasIndex == -1) continue

            val before = contents.substring(0, aliasIndex)
            val prefix = before.takeLastWhile { it.isJavaIdentifierPart() || it == '.' }
            val start = aliasIndex - prefix.length
            val end = aliasIndex + alias.length
            return DependencyMatch(start until end, contents.substring(start, end))
        }
        return null
    }
}
