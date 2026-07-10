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

import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import kotlin.collections.joinToString

private const val PARAM_TAG_REGEX_PATTERN = """^@param\s+(\w+)\s*(.*)"""
private const val PROPERTY_TAG_REGEX_PATTERN = """^@property\s+(\w+)\s*(.*)"""
private const val RESPONSE_TAG_REGEX_PATTERN = """^@return\s+(.*)"""
private const val ANY_TAG_REGEX_PATTERN = """^@\w+.*"""
private const val KOTLIN_SUPPORTED_TAGS_PATTERN =
    """^@(param|return|constructor|receiver|property|throws|exception|sample|see|author|since|suppress)\b.*"""
private val PARAM_TAG_REGEX = Regex(PARAM_TAG_REGEX_PATTERN)
private val PROPERTY_TAG_REGEX = Regex(PROPERTY_TAG_REGEX_PATTERN)
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
    return getTagContentsFromKDoc(docString, PARAM_TAG_REGEX)
}

/**
 * Returns a mapping of property name to property description, where the property's description is
 * extracted from `@property` declarations in the KDoc.
 *
 * The input docString is expected to be stripped from any "/**", "*/" or "*".
 */
internal fun getPropertyDescriptionsFromKDoc(docString: String): Map<String, String> {
    return getTagContentsFromKDoc(docString, PROPERTY_TAG_REGEX)
}

private fun getTagContentsFromKDoc(docString: String, tagRegex: Regex): Map<String, String> {
    val contentsMap = mutableMapOf<String, String>()

    val currentContentBuilder = StringBuilder()
    var currentTagName: String? = null

    for (line in docString.lines()) {
        val trimmedLine = line.trim()
        val propertyMatch = tagRegex.find(trimmedLine)

        when {
            propertyMatch != null -> {
                if (currentTagName != null) {
                    contentsMap[currentTagName] = currentContentBuilder.toString().trim()
                    currentContentBuilder.clear()
                }
                currentTagName = propertyMatch.groupValues[1]
                currentContentBuilder.append(propertyMatch.groupValues[2])
            }

            ANY_TAG_REGEX.matches(trimmedLine) -> {
                if (currentTagName != null) {
                    contentsMap[currentTagName] = currentContentBuilder.toString().trim()
                    currentContentBuilder.clear()
                    currentTagName = null
                }
            }

            currentTagName != null -> {
                if (trimmedLine.isNotBlank()) {
                    if (currentContentBuilder.isNotEmpty()) {
                        currentContentBuilder.append(" ")
                    }
                    currentContentBuilder.append(trimmedLine)
                }
            }
        }
    }

    if (currentTagName != null) {
        contentsMap[currentTagName] = currentContentBuilder.toString().trim()
    }
    return contentsMap
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
                responseDescriptionBuilder.append(responseMatch.groupValues[1])
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

/**
 * Returns the function's description.
 *
 * If the function is annotated with `@AppFunctionInstruction`, its `instruction` property value is
 * returned. Otherwise, the sanitized KDoc is returned (obtained by stripping Kotlin supported tags
 * from [rawKDoc]).
 *
 * @param rawKDoc The raw KDoc string of the function.
 * @return The description of the function.
 */
internal fun KSFunctionDeclaration.getFunctionDescription(rawKDoc: String): String {
    val instruction =
        this.annotations
            .findAnnotation(IntrospectionHelper.AppFunctionInstructionAnnotation.CLASS_NAME)
            ?.requirePropertyValueOfType(
                IntrospectionHelper.AppFunctionInstructionAnnotation.PROPERTY_INSTRUCTION,
                String::class,
            )
    if (instruction != null) {
        return instruction
    }
    return sanitizeKDoc(rawKDoc)
}

/**
 * Returns the function's response description.
 *
 * If the function's return type is annotated with `@AppFunctionInstruction`, its `instruction`
 * property value is returned. Otherwise, the response description is extracted from the `@return`
 * tag of the [rawKDoc] string.
 *
 * @param rawKDoc The raw KDoc string of the function.
 * @return The description of the function's response.
 */
internal fun KSFunctionDeclaration.getResponseDescription(rawKDoc: String): String {
    val returnInstruction =
        this.returnType
            ?.annotations
            ?.findAnnotation(IntrospectionHelper.AppFunctionInstructionAnnotation.CLASS_NAME)
            ?.requirePropertyValueOfType(
                IntrospectionHelper.AppFunctionInstructionAnnotation.PROPERTY_INSTRUCTION,
                String::class,
            )
    if (returnInstruction != null) {
        return returnInstruction
    }
    return getResponseDescriptionFromKDoc(rawKDoc)
}
