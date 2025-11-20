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

package androidx.appfunctions.compiler.core

import kotlin.collections.joinToString

private const val PARAM_TAG_REGEX_PATTERN = """^@param\s+(\w+)\s*(.*)"""
private const val RESPONSE_TAG_REGEX_PATTERN = """^@return\s+(.*)"""
private const val ANY_TAG_REGEX_PATTERN = """^@\w+.*"""
private const val KOTLIN_SUPPORTED_TAGS_PATTERN =
    """^@(param|return|constructor|receiver|property|throws|exception|sample|see|author|since|suppress)\b.*"""
private val PARAM_TAG_REGEX = Regex(PARAM_TAG_REGEX_PATTERN)
private val RESPONSE_TAG_REGEX = Regex(RESPONSE_TAG_REGEX_PATTERN)
private val ANY_TAG_REGEX = Regex(ANY_TAG_REGEX_PATTERN)
private val KOTLIN_SUPPORTED_TAGS = Regex(KOTLIN_SUPPORTED_TAGS_PATTERN)

/**
 * Returns a mapping of parameter name to parameter description, where the parameter's description
 * is extracted from `@param` declarations in the KDoc.
 *
 * The input docString is expected to be stripped from any "/**", "*/" or "*".
 */
internal fun getParamDescriptionsFromKDoc(docString: String): Map<String, String> {
    val descriptionMap = mutableMapOf<String, String>()

    val currentDescriptionBuilder = StringBuilder()
    var currentParamName: String? = null

    for (line in docString.lines()) {
        val trimmedLine = line.trim()
        val paramMatch = PARAM_TAG_REGEX.find(trimmedLine)

        when {
            paramMatch != null -> {
                if (currentParamName != null) {
                    descriptionMap[currentParamName] = currentDescriptionBuilder.toString().trim()
                    currentDescriptionBuilder.clear()
                }
                currentParamName = checkNotNull(paramMatch.groupValues[1])
                currentDescriptionBuilder.append(checkNotNull(paramMatch.groupValues[2]))
            }

            ANY_TAG_REGEX.matches(trimmedLine) -> {
                if (currentParamName != null) {
                    descriptionMap[currentParamName] = currentDescriptionBuilder.toString().trim()
                    currentDescriptionBuilder.clear()
                    currentParamName = null
                }
            }

            currentParamName != null -> {
                if (trimmedLine.isNotBlank()) {
                    if (currentDescriptionBuilder.isNotEmpty()) {
                        currentDescriptionBuilder.append(" ")
                    }
                    currentDescriptionBuilder.append(trimmedLine)
                }
            }
        }
    }

    if (currentParamName != null) {
        descriptionMap[currentParamName] = currentDescriptionBuilder.toString().trim()
    }
    return descriptionMap
}

/**
 * Returns the function's response description, extracted from the `@return` tag of the function's
 * KDoc.
 *
 * The input docString is expected to be stripped from any "/**", "*/" or "*".
 */
internal fun getResponseDescriptionFromKDoc(docString: String): String {
    val responseDescriptionBuilder = StringBuilder()

    for (line in docString.lines()) {
        val trimmedLine = line.trim()
        val responseMatch = RESPONSE_TAG_REGEX.find(trimmedLine)

        when {
            responseMatch != null -> {
                responseDescriptionBuilder.append(checkNotNull(responseMatch.groupValues[1]))
            }
            responseDescriptionBuilder.isNotEmpty() -> {
                if (ANY_TAG_REGEX.matches(trimmedLine)) {
                    return responseDescriptionBuilder.toString()
                } else {
                    responseDescriptionBuilder.append(" $trimmedLine")
                }
            }
        }
    }
    return responseDescriptionBuilder.toString().trim()
}

/**
 * Returns the function's docstring with all of the kotlin supported tags stripped out.
 *
 * The input docString is expected to be stripped from any "/**", "*/" or "*". Any content preceding
 * block tags is considered part of the previous tag's content and is stripped in case of kotlin
 * supported tags.
 */
internal fun sanitizeKDoc(docString: String): String {
    val resultLines = mutableListOf<String>()
    var skippingTagDescription = false

    for (line in docString.lines()) {
        val trimmedLine = line.trim()

        when {
            KOTLIN_SUPPORTED_TAGS.matches(trimmedLine) -> {
                skippingTagDescription = true
            }

            skippingTagDescription &&
                ANY_TAG_REGEX.matches(trimmedLine) &&
                !KOTLIN_SUPPORTED_TAGS.matches(trimmedLine) -> {
                skippingTagDescription = false
                resultLines.add(line)
            }

            !skippingTagDescription -> {
                resultLines.add(line)
            }
        }
    }

    return resultLines.joinToString("\n").trim()
}
