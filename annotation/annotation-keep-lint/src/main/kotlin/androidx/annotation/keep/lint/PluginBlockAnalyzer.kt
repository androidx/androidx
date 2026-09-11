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

/** Result of analyzing a build script's `plugins { ... }` block. */
internal data class PluginBlockAnalysis(
    val indent: String,
    val quote: String,
    /**
     * The line to anchor quickfix insertions after (the last plugin statement, or the opening
     * `plugins {` line if empty), or `null` if the block is absent.
     */
    val anchorLine: String?,
    /** Whether the build script contains a `plugins { ... }` block. */
    val hasPluginsBlock: Boolean,
)

/**
 * Analyzes a Gradle build script to extract formatting conventions and anchor lines from its
 * `plugins { ... }` block.
 */
internal object PluginBlockAnalyzer {
    const val DEFAULT_INDENT = "    "
    const val GROOVY_SINGLE_QUOTE = "'"
    const val DOUBLE_QUOTE = "\""

    /**
     * Analyzes the given build script [contents] (or isolated plugin block) to determine formatting
     * and insertion anchor points.
     */
    fun analyze(contents: String?, isKts: Boolean): PluginBlockAnalysis {
        if (contents == null) {
            return PluginBlockAnalysis(
                indent = DEFAULT_INDENT,
                quote = if (isKts) DOUBLE_QUOTE else GROOVY_SINGLE_QUOTE,
                anchorLine = null,
                hasPluginsBlock = false,
            )
        }

        val pluginsMatch = Regex("""plugins\s*\{[\s\S]*?\}""").find(contents)
        val blockText =
            pluginsMatch?.value ?: if (contents.trim().startsWith("plugins")) contents else null
        val hasPluginsBlock = blockText != null

        val indent = detectIndent(blockText)
        val quote = detectQuote(blockText, isKts)
        val anchorLine = findAnchorLine(blockText)

        return PluginBlockAnalysis(
            indent = indent,
            quote = quote,
            anchorLine = anchorLine,
            hasPluginsBlock = hasPluginsBlock,
        )
    }

    private fun findAnchorLine(blockText: String?): String? {
        if (blockText == null) return null
        val lines = blockText.lines()
        val lastPluginLine = lines.lastOrNull { line ->
            val trimmed = line.trim()
            trimmed.isNotBlank() && trimmed != "}" && !trimmed.startsWith("plugins")
        }
        return lastPluginLine ?: lines.firstOrNull { it.trim().startsWith("plugins") }
    }

    private fun detectIndent(blockText: String?): String {
        if (blockText != null) {
            for (line in blockText.lines()) {
                if (line.isNotBlank()) {
                    val indent = line.takeWhile { it == ' ' || it == '\t' }
                    if (indent.isNotEmpty()) {
                        return indent
                    }
                }
            }
        }
        return DEFAULT_INDENT
    }

    private fun detectQuote(blockText: String?, isKts: Boolean): String {
        if (isKts) {
            return DOUBLE_QUOTE
        }
        if (blockText != null) {
            val hasDouble = blockText.contains('"')
            val hasSingle = blockText.contains('\'')
            if (hasDouble && !hasSingle) return DOUBLE_QUOTE
        }
        return GROOVY_SINGLE_QUOTE
    }
}
