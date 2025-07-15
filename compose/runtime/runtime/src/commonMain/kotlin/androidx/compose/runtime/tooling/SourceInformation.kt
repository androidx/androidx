/*
 * Copyright 2025 The Android Open Source Project
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

@file:OptIn(ComposeToolingApi::class)

package androidx.compose.runtime.tooling

import androidx.compose.runtime.internal.logError
import androidx.compose.runtime.snapshots.fastAny
import kotlin.math.min

/**
 * Source information of a [CompositionGroup].
 *
 * This source information can represent a function call or a control flow group.
 */
@ComposeToolingApi
public class SourceInformation(
    public val isCall: Boolean,
    public val isInline: Boolean,
    public val functionName: String?,
    public val sourceFile: String?,
    public val parameters: List<ParameterSourceInformation>,
    public val packageHash: String?,
    public val locations: List<LocationSourceInformation>,
    public val rawData: String,
)

/** Source information about parameters of a function group. */
@ComposeToolingApi
public class ParameterSourceInformation(
    public val sortedIndex: Int,
    public val name: String? = null,
    public val inlineClass: String? = null,
)

/** Source information about composable function call locations inside parent group. */
@ComposeToolingApi
public class LocationSourceInformation(
    public val lineNumber: Int,
    public val offset: Int,
    public val length: Int,
    public val isRepeatable: Boolean,
)

private class ParseException(override val message: String) : Exception(message)

private class SourceInfoParserState(val data: String) {
    var i = 0

    fun expect(char: Char) {
        if (!matches(char)) {
            throwParseError("expected $char")
        }
    }

    fun throwParseError(message: String): Nothing {
        val end = min(i, data.length)
        throw ParseException(
            "Error while parsing source information: $message at " +
                "${data.substring(0, end)}|${data.substring(startIndex = end)}"
        )
    }

    fun matches(char: Char) = i < data.length && data[i] == char

    fun takeIntUntil(separator: String): Int {
        val int = takeUntil(separator).toIntOrNull()
        return int ?: throwParseError("expected int")
    }

    fun takeUntil(separator: String): String {
        val start = i
        skipUntil(separator)
        return if (i > start) data.substring(start, i) else ""
    }

    fun takeUntilEnd(): String {
        return data.substring(i, data.length)
    }

    fun skipUntil(separator: String) {
        while (i < data.length && data[i] !in separator) i++
    }

    fun advance(count: Int = 1) {
        i += count
    }

    fun current(): Char = data[i]

    fun atEnd(): Boolean = i >= data.length
}

/**
 * Parses source information from a string produced by Compose compiler. The source information
 * string is usually obtained from [CompositionGroup.sourceInfo].
 *
 * @return parsed source information or `null` if the string is not a valid source information
 */
@ComposeToolingApi
public fun parseSourceInformation(data: String): SourceInformation? {
    if (data.isEmpty()) {
        return null
    }

    return try {
        parseSourceInformationInternal(data)
    } catch (e: ParseException) {
        logError(e.message, e)
        null
    }
}

internal fun parseSourceInformationInternal(data: String): SourceInformation {
    var isCall = false
    var isInline = false
    var functionName: String? = null

    val p = SourceInfoParserState(data)
    // call section: "C" [ "C" ] [ "(" <name> ")" ] ]
    if (p.matches('C')) { // call
        isCall = true
        p.advance()

        if (p.matches('C')) { // inline call
            isInline = true
            p.advance()
        }
        if (p.matches('(')) { // function name
            p.advance()
            functionName = p.takeUntil(")")
            p.expect(')')
            p.advance()
        }
    }

    var parameters: List<ParameterSourceInformation> = emptyList()
    // Parameter sections
    while (p.hasSection()) {
        val sectionType = p.current()
        when (sectionType) {
            'P' -> {
                parameters = p.parseParameterIndex()
            }
            'N' -> {
                parameters = p.parseParameterNames()
            }
            else -> {
                // unknown section, skip to the end
                // we allow nested parens for future proofing
                var count = 0
                p.advance(2) // Skip section header
                while (count > 0 || !p.matches(')')) {
                    if (p.atEnd()) {
                        p.throwParseError("unexpected end")
                    }
                    if (p.matches('(')) {
                        count++
                    } else if (p.matches(')')) {
                        count--
                    }
                    p.advance()
                }
                p.expect(')')
                p.advance()
            }
        }
    }

    // Locations
    var locations: List<LocationSourceInformation> = emptyList()
    if (!p.matches(':')) {
        locations = p.parseLocations()
    } else {
        p.advance()
    }

    // File name and package hash
    // <name> [ "#" <package-hash>]

    val fileName = p.takeUntil("#").takeIf { it.isNotEmpty() }

    var packageHash: String? = null
    if (p.matches('#')) {
        p.advance()
        packageHash = p.takeUntilEnd()
    }

    return SourceInformation(
        isCall = isCall,
        isInline = isInline,
        functionName = functionName,
        parameters = parameters,
        locations = locations,
        sourceFile = fileName,
        packageHash = packageHash,
        rawData = data,
    )
}

private fun SourceInfoParserState.hasSection() =
    i < data.length - 1 && data[i].isLetter() && data[i + 1] == '('

private fun SourceInfoParserState.parseParameterIndex(): List<ParameterSourceInformation> {
    // "P(" (<parameter-index> | <run>) [ [ "," ] (<parameter-index> | <run>) ]* ")"
    // parameter-index: <index> [ ":" <inline-class-fqname> ]
    // run: "!" [ <number> ]

    advance(2) //  skip "P("
    val parameters = mutableListOf<ParameterSourceInformation>()
    var pendingRun = false
    while (!atEnd() && !matches(')')) {
        if (matches('!')) {
            // run
            advance()
            val countString = takeUntil("!,)")
            if (countString.isEmpty()) {
                pendingRun = true
            } else {
                var count = countString.toInt()
                var nextIndex = 0

                while (count > 0) {
                    // find next unsorted index
                    if (parameters.fastAny { it.sortedIndex == nextIndex }) {
                        nextIndex++
                        continue
                    }

                    parameters.add(ParameterSourceInformation(sortedIndex = nextIndex))
                    count--
                }
            }
        } else {
            // parameter-index
            val index = takeIntUntil("!:,)")
            var inlineClass: String? = null
            if (matches(':')) {
                advance()
                inlineClass = takeUntil("!,)").replaceComposePrefix()
            }

            if (pendingRun) {
                var nextIndex = 0
                val maxIndex = index
                while (nextIndex < maxIndex) {
                    if (parameters.fastAny { it.sortedIndex == nextIndex }) {
                        nextIndex++
                        continue
                    }
                    parameters.add(ParameterSourceInformation(sortedIndex = nextIndex))
                }
                pendingRun = false
            }

            parameters.add(
                ParameterSourceInformation(sortedIndex = index, inlineClass = inlineClass)
            )
        }

        if (matches(',')) {
            advance()
        }
    }
    expect(')')
    advance() // Advance past ')'
    return parameters
}

private fun SourceInfoParserState.parseParameterNames(): List<ParameterSourceInformation> {
    // "N(" <parameter-name> [ "," <parameter-name> ]* ")"
    // parameter-name: <name-string> [ ":" <inline-class-fqname> ]

    advance(2) //  skip "N("
    val parameters = mutableListOf<ParameterSourceInformation>()
    while (!atEnd() && !matches(')')) {
        // parameter-index
        val name = takeUntil(":,)")
        var inlineClass: String? = null
        if (matches(':')) {
            advance()
            inlineClass = takeUntil(",)").replaceComposePrefix()
        }
        parameters.add(
            ParameterSourceInformation(
                sortedIndex = parameters.size,
                inlineClass = inlineClass,
                name = name,
            )
        )

        if (matches(',')) {
            advance()
        }
    }
    expect(')')
    advance() // Advance past ')'
    return parameters
}

private fun SourceInfoParserState.parseLocations(): List<LocationSourceInformation> {
    // locations: [ <location> [ "," <location> ] ":" ]
    // location: [ "*" ] [ <line-number> ] "@" <offset>  [ "L" <length> ]
    val locations = mutableListOf<LocationSourceInformation>()
    while (!atEnd() && !matches(':')) {
        var repeatable = false
        if (matches('*')) {
            repeatable = true
            advance()
        }
        var lineNumber: Int? = null
        if (!matches('@')) {
            lineNumber = takeIntUntil("@") + 1
        }
        advance()
        val offset = takeIntUntil("L,:")
        var length: Int? = null
        if (matches('L')) {
            advance()
            length = takeIntUntil(",:")
        }
        locations.add(
            LocationSourceInformation(
                lineNumber = lineNumber ?: -1,
                offset = offset,
                length = length ?: -1,
                isRepeatable = repeatable,
            )
        )

        if (matches(',')) {
            advance()
        }
    }
    advance() // Advance past ':'
    return locations
}

private fun String.replaceComposePrefix() = replaceFirst("c#", "androidx.compose.")
