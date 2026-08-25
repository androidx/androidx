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

package androidx.credentials.registry.digitalcredentials.openid4vci

/**
 * Internal parser to compute the rendered length of explainer text containing markdown links.
 *
 * Mirrors the GMS Core markdown link parsing logic in a flattened, allocation-efficient manner.
 */
internal object ExplainerTextParser {
    private const val MAX_RECURSION_DEPTH = 10

    private class LinkCandidate(
        val openBracket: Int,
        val closeBracket: Int,
        val openParen: Int,
        val closeParen: Int,
        val linkText: String,
        val url: String,
    )

    /**
     * Calculates the length of [text] when rendered with markdown link formatting applied.
     *
     * Valid markdown links in the format `[linkText](url)` where `url` has an `http://` or
     * `https://` scheme contribute only `linkText.length` to the total character count. Malformed
     * or unsupported markdown syntax is treated as literal plain text and counted in full.
     *
     * @param text the raw markdown explainer string
     * @return the total number of characters in the rendered text
     */
    fun computeRenderedLength(text: String): Int {
        val bracketMap = precomputeBracketPairs(text)
        return computeRenderedLengthRecursive(
            text = text,
            bracketMap = bracketMap,
            offset = 0,
            depth = 0,
            maxDepth = MAX_RECURSION_DEPTH,
        )
    }

    private fun isWebUrl(url: String): Boolean {
        return url.startsWith("http://", ignoreCase = true) ||
            url.startsWith("https://", ignoreCase = true)
    }

    private fun isEscaped(text: String, index: Int): Boolean {
        if (index == 0) return false
        var backslashes = 0
        for (k in index - 1 downTo 0) {
            if (text[k] == '\\') {
                backslashes++
            } else {
                break
            }
        }
        return backslashes % 2 == 1
    }

    private fun findMatchingBracket(
        text: String,
        startIndex: Int,
        openChar: Char,
        closeChar: Char,
    ): Int {
        var balance = 1
        for (j in startIndex + 1 until text.length) {
            val char = text[j]
            if (char == openChar && !isEscaped(text, j)) {
                balance++
            } else if (char == closeChar && !isEscaped(text, j)) {
                balance--
            }
            if (balance == 0) {
                return j
            }
        }
        return -1
    }

    private fun precomputeBracketPairs(text: String): Map<Int, Int> {
        val bracketMap = mutableMapOf<Int, Int>()
        val squareStack = IntArray(text.length)
        var squareTop = 0
        val parenStack = IntArray(text.length)
        var parenTop = 0
        var consecutiveBackslashes = 0

        for (i in text.indices) {
            val c = text[i]
            if (c == '\\') {
                consecutiveBackslashes++
                continue
            }
            val isEscaped = (consecutiveBackslashes % 2 == 1)
            consecutiveBackslashes = 0
            if (isEscaped) continue

            when (c) {
                '[' -> squareStack[squareTop++] = i
                ']' -> if (squareTop > 0) bracketMap[squareStack[--squareTop]] = i
                '(' -> parenStack[parenTop++] = i
                ')' -> if (parenTop > 0) bracketMap[parenStack[--parenTop]] = i
            }
        }
        return bracketMap
    }

    private fun extractLinkCandidate(
        text: String,
        openBracketIndex: Int,
        bracketMap: Map<Int, Int>?,
        offset: Int,
    ): LinkCandidate? {
        val closeBracketIndex =
            if (bracketMap != null) {
                bracketMap[openBracketIndex + offset]?.let { it - offset } ?: -1
            } else {
                findMatchingBracket(text, openBracketIndex, '[', ']')
            }
        if (closeBracketIndex == -1 || closeBracketIndex >= text.length) return null

        val openParenIndex = closeBracketIndex + 1
        if (openParenIndex >= text.length || text[openParenIndex] != '(') return null

        val closeParenIndex =
            if (bracketMap != null) {
                bracketMap[openParenIndex + offset]?.let { it - offset } ?: -1
            } else {
                findMatchingBracket(text, openParenIndex, '(', ')')
            }
        if (closeParenIndex == -1 || closeParenIndex >= text.length) return null

        val linkText = text.substring(openBracketIndex + 1, closeBracketIndex)
        val url = text.substring(openParenIndex + 1, closeParenIndex)
        if (linkText.isEmpty() || url.isEmpty()) return null

        return LinkCandidate(
            openBracket = openBracketIndex,
            closeBracket = closeBracketIndex,
            openParen = openParenIndex,
            closeParen = closeParenIndex,
            linkText = linkText,
            url = url,
        )
    }

    private fun computeRenderedLengthRecursive(
        text: String,
        bracketMap: Map<Int, Int>?,
        offset: Int,
        depth: Int,
        maxDepth: Int,
    ): Int {
        if (depth >= maxDepth) return text.length

        var length = 0
        var i = 0
        while (i < text.length) {
            val openBracketIndex = text.indexOf('[', i)
            if (openBracketIndex == -1) {
                length += text.length - i
                break
            }

            length += openBracketIndex - i

            val link = extractLinkCandidate(text, openBracketIndex, bracketMap, offset)
            if (link == null) {
                length += 1 // Treat '[' as literal
                i = openBracketIndex + 1
                continue
            }

            val hasNested =
                hasValidNestedLink(
                    text = link.linkText,
                    bracketMap = bracketMap,
                    offset = offset + link.openBracket + 1,
                    depth = depth + 1,
                    maxDepth = maxDepth,
                )

            if (hasNested) {
                val innerLength =
                    computeRenderedLengthRecursive(
                        text = link.linkText,
                        bracketMap = bracketMap,
                        offset = offset + link.openBracket + 1,
                        depth = depth + 1,
                        maxDepth = maxDepth,
                    )
                length += 1 + innerLength + 1 + (link.closeParen - link.openParen + 1)
            } else if (isWebUrl(link.url)) {
                length += link.linkText.length
            } else {
                length += (link.closeParen - link.openBracket + 1)
            }
            i = link.closeParen + 1
        }
        return length
    }

    private fun hasValidNestedLink(
        text: String,
        bracketMap: Map<Int, Int>?,
        offset: Int,
        depth: Int,
        maxDepth: Int,
    ): Boolean {
        if (depth >= maxDepth) return false
        var i = 0
        while (i < text.length) {
            val openBracketIndex = text.indexOf('[', i)
            if (openBracketIndex == -1) break

            val link = extractLinkCandidate(text, openBracketIndex, bracketMap, offset)
            if (link != null) {
                if (isWebUrl(link.url)) return true
                i = link.closeParen + 1
            } else {
                i = openBracketIndex + 1
            }
        }
        return false
    }
}
