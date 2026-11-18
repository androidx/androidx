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

package androidx.compose.markuptext.parser.block

/**
 * Renders [block] and its descendants with every field that affects behavior, so that two trees
 * compare equal only if they would parse and render the same. A container whose children's line
 * counts do not add up to its own line count gets a `!!` line.
 */
internal fun dumpParseTree(block: ParseBlock): String =
    StringBuilder().apply { appendParseTree(block, indent = "") }.toString()

private fun StringBuilder.appendParseTree(block: ParseBlock, indent: String) {
    append(indent).append(block::class.simpleName).append(" lineCount=").append(block.lineCount)
    when (block) {
        is ParagraphParseBlock ->
            append(" heading=")
                .append(block.headingLevel)
                .append(" '")
                .append(block.content)
                .append("'")
        is IndentedCodeBlockParseBlock -> append(" '").append(block.getCodeContent()).append("'")
        is FencedCodeBlockParseBlock ->
            append(" closed=")
                .append(block.isClosed)
                .append(" language=")
                .append(block.language)
                .append(" '")
                .append(block.getCodeContent())
                .append("'")
        is ListParseBlock ->
            append(" bulletOrDelimiter=")
                .append(block.marker.bulletOrDelimiter)
                .append(" start=")
                .append(block.marker.startNumber)
                .append(" tight=")
                .append(block.computeIsTight())
        is ListItemParseBlock ->
            append(" bulletOrDelimiter=")
                .append(block.marker.bulletOrDelimiter)
                .append(" start=")
                .append(block.marker.startNumber)
                .append(" contentIndent=")
                .append(block.marker.contentIndentColumn)
        else -> {}
    }
    append('\n')
    if (block is ContainerParseBlock) {
        var childLineCount = 0
        for (child in block.children) {
            childLineCount += child.lineCount
            appendParseTree(child, "$indent  ")
        }
        if (childLineCount != block.lineCount) {
            append(indent).append("!! children lineCount sum=").append(childLineCount).append('\n')
        }
    }
}
