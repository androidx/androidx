/*
 * Copyright 2024 The Android Open Source Project
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

// Impl classes from kotlin.library.abi.impl are necessary to instantiate parsed declarations
@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
@file:OptIn(ExperimentalLibraryAbiReader::class)

package androidx.binarycompatibilityvalidator

import org.jetbrains.kotlin.library.abi.AbiClassKind
import org.jetbrains.kotlin.library.abi.AbiCompoundName
import org.jetbrains.kotlin.library.abi.AbiModality
import org.jetbrains.kotlin.library.abi.AbiPropertyKind
import org.jetbrains.kotlin.library.abi.AbiQualifiedName
import org.jetbrains.kotlin.library.abi.AbiType
import org.jetbrains.kotlin.library.abi.AbiTypeArgument
import org.jetbrains.kotlin.library.abi.AbiTypeNullability
import org.jetbrains.kotlin.library.abi.AbiTypeParameter
import org.jetbrains.kotlin.library.abi.AbiValueParameter
import org.jetbrains.kotlin.library.abi.AbiVariance
import org.jetbrains.kotlin.library.abi.ExperimentalLibraryAbiReader
import org.jetbrains.kotlin.library.abi.impl.AbiTypeParameterImpl
import org.jetbrains.kotlin.library.abi.impl.AbiValueParameterImpl
import org.jetbrains.kotlin.library.abi.impl.ClassReferenceImpl
import org.jetbrains.kotlin.library.abi.impl.SimpleTypeImpl
import org.jetbrains.kotlin.library.abi.impl.StarProjectionImpl
import org.jetbrains.kotlin.library.abi.impl.TypeParameterReferenceImpl
import org.jetbrains.kotlin.library.abi.impl.TypeProjectionImpl

// This file contains Cursor methods specific to parsing klib dump files

internal fun Cursor.parseAbiModality(): AbiModality? {
    val parsed = parseAbiModalityString(peek = true)?.let { AbiModality.valueOf(it) }
    if (parsed != null) {
        parseAbiModalityString()
    }
    return parsed
}

internal fun Cursor.parseClassKind(peek: Boolean = false): AbiClassKind? {
    val parsed = parseClassKindString(peek = true)?.let { AbiClassKind.valueOf(it) }
    if (parsed != null && !peek) {
        parseClassKindString()
    }
    return parsed
}

internal fun Cursor.parsePropertyKind(peek: Boolean = false): AbiPropertyKind? {
    val parsed = parsePropertyKindString(peek = true)?.let { AbiPropertyKind.valueOf(it) }
    if (parsed != null && !peek) {
        parsePropertyKindString()
    }
    return parsed
}

internal fun Cursor.hasClassKind(): Boolean {
    val subCursor = copy()
    subCursor.skipInlineWhitespace()
    subCursor.parseAbiModality()
    subCursor.parseClassModifiers()
    return subCursor.parseClassKind() != null
}

internal fun Cursor.hasFunctionKind(): Boolean {
    val subCursor = copy()
    subCursor.skipInlineWhitespace()
    subCursor.parseAbiModality()
    subCursor.parseFunctionModifiers()
    return subCursor.parseFunctionKind() != null
}

internal fun Cursor.hasPropertyKind(): Boolean {
    val subCursor = copy()
    subCursor.skipInlineWhitespace()
    subCursor.parseAbiModality()
    return subCursor.parsePropertyKind() != null
}

internal fun Cursor.hasEnumEntry(): Boolean = parseEnumEntryKind(peek = true) != null

internal fun Cursor.hasGetter() = hasPropertyAccessor(GetterOrSetter.GETTER)

internal fun Cursor.hasSetter() = hasPropertyAccessor(GetterOrSetter.SETTER)

internal fun Cursor.hasGetterOrSetter() = hasGetter() || hasSetter()

internal fun Cursor.parseGetterName(peek: Boolean = false): String? {
    val cursor = subCursor(peek)
    cursor.parseSymbol("^<get\\-") ?: return null
    val name = cursor.parseValidIdentifier() ?: return null
    cursor.parseSymbol("^>") ?: return null
    return "<get-$name>"
}

internal fun Cursor.parseSetterName(peek: Boolean = false): String? {
    val cursor = subCursor(peek)
    cursor.parseSymbol("^<set\\-") ?: return null
    val name = cursor.parseValidIdentifier() ?: return null
    cursor.parseSymbol("^>") ?: return null
    return "<set-$name>"
}

internal fun Cursor.parseGetterOrSetterName(peek: Boolean = false) =
    parseGetterName(peek) ?: parseSetterName(peek)

internal fun Cursor.parseClassModifier(peek: Boolean = false): String? =
    parseSymbol("^(inner|value|fun|open)", peek)

internal fun Cursor.parseClassModifiers(): Set<String> {
    val modifiers = mutableSetOf<String>()
    while (parseClassModifier(peek = true) != null) {
        modifiers.add(parseClassModifier()!!)
    }
    return modifiers
}

internal fun Cursor.parseFunctionKind(peek: Boolean = false) =
    parseSymbol("^(constructor|fun)", peek)

internal fun Cursor.parseFunctionModifier(peek: Boolean = false): String? =
    parseSymbol("^(inline|suspend)", peek)

internal fun Cursor.parseFunctionModifiers(): Set<String> {
    val modifiers = mutableSetOf<String>()
    while (parseFunctionModifier(peek = true) != null) {
        modifiers.add(parseFunctionModifier()!!)
    }
    return modifiers
}

internal fun Cursor.parseAbiQualifiedName(peek: Boolean = false): AbiQualifiedName? {
    val symbol =
        parseSymbol("^[a-zA-Z0-9\\.]+\\/[a-zA-Z0-9]+(\\.[a-zA-Z0-9]+)?", peek) ?: return null
    val (packageName, relativeName) = symbol.split("/")
    return AbiQualifiedName(AbiCompoundName(packageName), AbiCompoundName(relativeName))
}

internal fun Cursor.parseAbiType(peek: Boolean = false): AbiType? {
    val cursor = subCursor(peek)
    // A type will either be a qualified name (kotlin/Array) or a type reference (#A)
    // try to parse a qualified name and a type reference if it doesn't exist
    val abiQualifiedName = cursor.parseAbiQualifiedName() ?: return cursor.parseTypeReference()
    val typeArgs = cursor.parseTypeArgs() ?: emptyList()
    val nullability = cursor.parseNullability(assumeNotNull = true)
    return SimpleTypeImpl(
        ClassReferenceImpl(abiQualifiedName),
        arguments = typeArgs,
        nullability = nullability
    )
}

internal fun Cursor.parseTypeArgs(): List<AbiTypeArgument>? {
    val typeArgsString = parseTypeParamsString() ?: return null
    val subCursor = Cursor(typeArgsString)
    subCursor.parseSymbol("<") ?: return null
    val typeArgs = mutableListOf<AbiTypeArgument>()
    while (subCursor.parseTypeArg(peek = true) != null) {
        typeArgs.add(subCursor.parseTypeArg()!!)
        subCursor.parseSymbol(",")
    }
    return typeArgs
}

internal fun Cursor.parseTypeArg(peek: Boolean = false): AbiTypeArgument? {
    val cursor = subCursor(peek)
    val variance = cursor.parseAbiVariance()
    cursor.parseSymbol("\\*")?.let {
        return StarProjectionImpl
    }
    val type = cursor.parseAbiType(peek) ?: return null
    return TypeProjectionImpl(type = type, variance = variance)
}

internal fun Cursor.parseAbiVariance(): AbiVariance {
    val variance = parseSymbol("^(out|in)") ?: return AbiVariance.INVARIANT
    return AbiVariance.valueOf(variance.uppercase())
}

internal fun Cursor.parseTypeReference(): AbiType? {
    val typeParamReference = parseTag() ?: return null
    val typeArgs = parseTypeArgs() ?: emptyList()
    val nullability = parseNullability()
    return SimpleTypeImpl(
        TypeParameterReferenceImpl(typeParamReference),
        arguments = typeArgs,
        nullability = nullability
    )
}

internal fun Cursor.parseTag() = parseSymbol("^#[a-zA-Z0-9]+")?.removePrefix("#")

internal fun Cursor.parseNullability(assumeNotNull: Boolean = false): AbiTypeNullability {
    val nullable = parseSymbol("^\\?") != null
    val definitelyNotNull = parseSymbol("^\\!\\!") != null
    return when {
        nullable -> AbiTypeNullability.MARKED_NULLABLE
        definitelyNotNull -> AbiTypeNullability.DEFINITELY_NOT_NULL
        else ->
            if (assumeNotNull) {
                AbiTypeNullability.DEFINITELY_NOT_NULL
            } else {
                AbiTypeNullability.NOT_SPECIFIED
            }
    }
}

internal fun Cursor.parseSuperTypes(): MutableSet<AbiType> {
    parseSymbol(":")
    val superTypes = mutableSetOf<AbiType>()
    while (parseAbiQualifiedName(peek = true) != null) {
        superTypes.add(parseAbiType()!!)
        parseSymbol(",")
    }
    return superTypes
}

fun Cursor.parseTypeParams(peek: Boolean = false): List<AbiTypeParameter>? {
    val typeParamsString = parseTypeParamsString(peek) ?: return null
    val subCursor = Cursor(typeParamsString)
    subCursor.parseSymbol("^<")
    val typeParams = mutableListOf<AbiTypeParameter>()
    while (subCursor.parseTypeParam(peek = true) != null) {
        typeParams.add(subCursor.parseTypeParam()!!)
        subCursor.parseSymbol("^,")
    }
    return typeParams
}

internal fun Cursor.parseTypeParam(peek: Boolean = false): AbiTypeParameter? {
    val cursor = subCursor(peek)
    val tag = cursor.parseTag() ?: return null
    cursor.parseSymbol("^:")
    val variance = cursor.parseAbiVariance()
    val isReified = cursor.parseSymbol("reified") != null
    val upperBounds = mutableListOf<AbiType>()
    if (null != cursor.parseAbiType(peek = true)) {
        upperBounds.add(cursor.parseAbiType()!!)
    }

    return AbiTypeParameterImpl(
        tag = tag,
        variance = variance,
        isReified = isReified,
        upperBounds = upperBounds
    )
}

internal fun Cursor.parseValueParameters(): List<AbiValueParameter>? {
    val valueParamString = parseValueParametersString() ?: return null
    val subCursor = Cursor(valueParamString)
    val valueParams = mutableListOf<AbiValueParameter>()
    subCursor.parseSymbol("\\(")
    while (null != subCursor.parseValueParameter(peek = true)) {
        valueParams.add(subCursor.parseValueParameter()!!)
        subCursor.parseSymbol("^,")
    }
    return valueParams
}

internal fun Cursor.parseValueParameter(peek: Boolean = false): AbiValueParameter? {
    val cursor = subCursor(peek)
    val modifiers = cursor.parseValueParameterModifiers()
    val isNoInline = modifiers.contains("noinline")
    val isCrossinline = modifiers.contains("crossinline")
    val type = cursor.parseAbiType() ?: return null
    val isVararg = cursor.parseVarargSymbol() != null
    val hasDefaultArg = cursor.parseDefaultArg() != null
    return AbiValueParameterImpl(
        type = type,
        isVararg = isVararg,
        hasDefaultArg = hasDefaultArg,
        isNoinline = isNoInline,
        isCrossinline = isCrossinline
    )
}

internal fun Cursor.parseValueParameterModifiers(): Set<String> {
    val modifiers = mutableSetOf<String>()
    while (parseValueParameterModifier(peek = true) != null) {
        modifiers.add(parseValueParameterModifier()!!)
    }
    return modifiers
}

internal fun Cursor.parseValueParameterModifier(peek: Boolean = false): String? =
    parseSymbol("^(crossinline|noinline)", peek)

internal fun Cursor.parseVarargSymbol() = parseSymbol("^\\.\\.\\.")

internal fun Cursor.parseDefaultArg() = parseSymbol("^=\\.\\.\\.")

internal fun Cursor.parseFunctionReceiver(): AbiType? {
    val string = parseFunctionReceiverString() ?: return null
    val subCursor = Cursor(string)
    subCursor.parseSymbol("\\(")
    return subCursor.parseAbiType()
}

internal fun Cursor.parseReturnType(): AbiType? {
    parseSymbol("^:\\s")
    return parseAbiType()
}

internal fun Cursor.parseTargets(): List<String> {
    parseSymbol("^Targets:")
    parseSymbol("^\\[")
    val targets = mutableListOf<String>()
    while (parseValidIdentifier(peek = true) != null) {
        targets.add(parseValidIdentifier()!!)
        parseSymbol("^,")
    }
    parseSymbol("^\\]")
    return targets
}

internal fun Cursor.parseEnumEntryKind(peek: Boolean = false) = parseSymbol("enum\\sentry", peek)

internal fun Cursor.parseEnumName() = parseSymbol("^[A-Z_]+")

/**
 * Used to check if declarations after a property are getter / setter methods which should be
 * attached to that property.
 */
private fun Cursor.hasPropertyAccessor(type: GetterOrSetter): Boolean {
    val subCursor = copy()
    subCursor.parseAbiModality()
    subCursor.parseFunctionModifiers()
    subCursor.parseFunctionKind() ?: return false // if it's not a function it's not a getter/setter
    val mightHaveTypeParams = subCursor.parseGetterOrSetterName(peek = true) == null
    if (mightHaveTypeParams) {
        subCursor.parseTypeParams()
    }
    subCursor.parseFunctionReceiver()
    return when (type) {
        GetterOrSetter.GETTER -> subCursor.parseGetterName() != null
        GetterOrSetter.SETTER -> subCursor.parseSetterName() != null
    }
}

private fun Cursor.subCursor(peek: Boolean) =
    if (peek) {
        copy()
    } else {
        this
    }

private fun Cursor.parseTypeParamsString(peek: Boolean = false): String? {
    if (parseSymbol("^<(get|set)\\-", peek = true) != null) {
        return null
    }
    val cursor = subCursor(peek)
    val result = StringBuilder()
    cursor.parseSymbol("^<")?.let { result.append(it) } ?: return null
    var openBracketCount = 1
    while (openBracketCount > 0) {
        val nextSymbol =
            cursor.parseSymbol(".", skipInlineWhitespace = false).also { result.append(it) }
        when (nextSymbol) {
            "<" -> openBracketCount++
            ">" -> openBracketCount--
        }
    }
    cursor.skipInlineWhitespace()
    return result.toString()
}

private fun Cursor.parseFunctionReceiverString() =
    parseSymbol("^\\([a-zA-Z0-9,\\/<>,#\\.\\s]+?\\)\\.")

private fun Cursor.parseValueParametersString() =
    parseSymbol("^\\(([a-zA-Z0-9,\\/<>,#\\.\\s\\?=]+)?\\)")

private fun Cursor.parseAbiModalityString(peek: Boolean = false) =
    parseSymbol("^(final|open|abstract|sealed)", peek)?.uppercase()

private fun Cursor.parsePropertyKindString(peek: Boolean = false) =
    parseSymbol("^(const\\sval|val|var)", peek)?.uppercase()?.replace(" ", "_")

private fun Cursor.parseClassKindString(peek: Boolean = false) =
    parseSymbol("^(class|interface|object|enum\\sclass|annotation\\sclass)", peek)
        ?.uppercase()
        ?.replace(" ", "_")

private enum class GetterOrSetter() {
    GETTER,
    SETTER
}
