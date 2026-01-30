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

@file:OptIn(ExperimentalLibraryAbiReader::class)

package androidx.binarycompatibilityvalidator

import com.google.common.truth.Truth.assertThat
import org.jetbrains.kotlin.library.abi.AbiClassKind
import org.jetbrains.kotlin.library.abi.AbiModality
import org.jetbrains.kotlin.library.abi.AbiPropertyKind
import org.jetbrains.kotlin.library.abi.AbiTypeArgument
import org.jetbrains.kotlin.library.abi.AbiTypeNullability
import org.jetbrains.kotlin.library.abi.AbiValueParameter
import org.jetbrains.kotlin.library.abi.AbiValueParameterKind
import org.jetbrains.kotlin.library.abi.AbiVariance
import org.jetbrains.kotlin.library.abi.ExperimentalLibraryAbiReader
import org.junit.Test

class KlibParsingCursorExtensionsTest {

    @Test
    fun parseModalityFailure() {
        val input = "something else"
        val cursor = Cursor(input)
        val modality = cursor.parseAbiModality()
        assertThat(modality).isNull()
        assertThat(cursor.currentLine).isEqualTo("something else")
    }

    @Test
    fun parseModalitySuccess() {
        val input = "final whatever"
        val cursor = Cursor(input)
        val modality = cursor.parseAbiModality()
        assertThat(modality).isEqualTo(AbiModality.FINAL)
        assertThat(cursor.currentLine).isEqualTo("whatever")
    }

    @Test
    fun parseClassModifier() {
        val input = "inner whatever"
        val cursor = Cursor(input)
        val modifier = cursor.parseClassModifier()
        assertThat(modifier).isEqualTo("inner")
        assertThat(cursor.currentLine).isEqualTo("whatever")
    }

    @Test
    fun parseClassModifiers() {
        val input = "inner value fun whatever"
        val cursor = Cursor(input)
        val modifiers = cursor.parseClassModifiers()
        assertThat(modifiers).containsExactly("inner", "fun", "value")
        assertThat(cursor.currentLine).isEqualTo("whatever")
    }

    @Test
    fun parseFunctionModifiers() {
        val input = "final inline suspend fun component1(): kotlin/Long"
        val cursor = Cursor(input)
        cursor.parseAbiModality()
        val modifiers = cursor.parseFunctionModifiers()
        assertThat(modifiers).containsExactly("inline", "suspend")
        assertThat(cursor.currentLine).isEqualTo("fun component1(): kotlin/Long")
    }

    @Test
    fun parseClassKindSimple() {
        val input = "class"
        val cursor = Cursor(input)
        val kind = cursor.parseClassKind()
        assertThat(kind).isEqualTo(AbiClassKind.CLASS)
    }

    @Test
    fun parseClassKindFalsePositive() {
        val input = "androidx.collection/objectFloatMap"
        val cursor = Cursor(input)
        val kind = cursor.parseClassKind()
        assertThat(kind).isNull()
    }

    @Test
    fun parseClassKindMultiPart() {
        val input = "annotation class"
        val cursor = Cursor(input)
        val kind = cursor.parseClassKind()
        assertThat(kind).isEqualTo(AbiClassKind.ANNOTATION_CLASS)
    }

    @Test
    fun hasClassKind() {
        val input = "final class my.lib/MyClass"
        val cursor = Cursor(input)
        assertThat(cursor.hasClassKind()).isTrue()
        assertThat(cursor.currentLine).isEqualTo(input)
    }

    @Test
    fun parseFunctionKindSimple() {
        val input = "fun hello"
        val cursor = Cursor(input)
        val kind = cursor.parseFunctionKind()
        assertThat(kind).isEqualTo("fun")
        assertThat(cursor.currentLine).isEqualTo(cursor.currentLine)
    }

    @Test
    fun hasFunctionKind() {
        val input = "    final fun myFun(): kotlin/String "
        val cursor = Cursor(input)
        assertThat(cursor.hasFunctionKind()).isTrue()
        assertThat(cursor.currentLine).isEqualTo(input)
    }

    @Test
    fun hasFunctionKindConstructor() {
        val input = "    constructor <init>(kotlin/Int =...)"
        val cursor = Cursor(input)
        assertThat(cursor.hasFunctionKind()).isTrue()
        assertThat(cursor.currentLine).isEqualTo(input)
    }

    @Test
    fun parseGetterOrSetterName() {
        val input = "<get-indices>()"
        val cursor = Cursor(input)
        val name = cursor.parseGetterOrSetterName()
        assertThat(name).isEqualTo("<get-indices>")
        assertThat(cursor.currentLine).isEqualTo("()")
    }

    @Test
    fun hasGetter() {
        val input = "final inline fun <get-indices>(): kotlin.ranges/IntRange"
        val cursor = Cursor(input)
        assertThat(cursor.hasGetter()).isTrue()
        assertThat(cursor.currentLine).isEqualTo(input)
    }

    @Test
    fun hasSetter() {
        val input = "final inline fun <set-indices>(): kotlin.ranges/IntRange"
        val cursor = Cursor(input)
        assertThat(cursor.hasSetter()).isTrue()
        assertThat(cursor.currentLine).isEqualTo(input)
    }

    @Test
    fun hasGetterOrSetter() {
        val inputs =
            listOf(
                "final inline fun <set-indices>(): kotlin.ranges/IntRange",
                "final inline fun <get-indices>(): kotlin.ranges/IntRange",
            )
        inputs.forEach { input -> assertThat(Cursor(input).hasGetterOrSetter()).isTrue() }
    }

    @Test
    fun hasPropertyKind() {
        val input = "final const val my.lib/myProp"
        val cursor = Cursor(input)
        assertThat(cursor.hasPropertyKind()).isTrue()
        assertThat(cursor.currentLine).isEqualTo(input)
    }

    @Test
    fun parsePropertyKindConstVal() {
        val input = "const val something"
        val cursor = Cursor(input)
        val kind = cursor.parsePropertyKind()
        assertThat(kind).isEqualTo(AbiPropertyKind.CONST_VAL)
        assertThat(cursor.currentLine).isEqualTo("something")
    }

    @Test
    fun parsePropertyKindVal() {
        val input = "val something"
        val cursor = Cursor(input)
        val kind = cursor.parsePropertyKind()
        assertThat(kind).isEqualTo(AbiPropertyKind.VAL)
        assertThat(cursor.currentLine).isEqualTo("something")
    }

    @Test
    fun parseNullability() {
        val nullable = Cursor("?").parseNullability()
        val notNull = Cursor("!!").parseNullability()
        val unspecified = Cursor("another symbol").parseNullability()
        assertThat(nullable).isEqualTo(AbiTypeNullability.MARKED_NULLABLE)
        assertThat(notNull).isEqualTo(AbiTypeNullability.DEFINITELY_NOT_NULL)
        assertThat(unspecified).isEqualTo(AbiTypeNullability.NOT_SPECIFIED)
    }

    @Test
    fun parseNullabilityWhenAssumingNotNullable() {
        val unspecified = Cursor("").parseNullability(assumeNotNull = true)
        assertThat(unspecified).isEqualTo(AbiTypeNullability.DEFINITELY_NOT_NULL)
    }

    @Test
    fun parseQualifiedName() {
        val input = "androidx.collection/MutableScatterMap { something"
        val cursor = Cursor(input)
        val qName = cursor.parseAbiQualifiedName()
        assertThat(qName.toString()).isEqualTo("androidx.collection/MutableScatterMap")
        assertThat(cursor.currentLine).isEqualTo("{ something")
    }

    @Test
    fun parseQualifiedNameKotlin() {
        val input = "kotlin/Function2<#A1, #A, #A1>"
        val cursor = Cursor(input)
        val qName = cursor.parseAbiQualifiedName()
        assertThat(qName.toString()).isEqualTo("kotlin/Function2")
        assertThat(cursor.currentLine).isEqualTo("<#A1, #A, #A1>")
    }

    @Test
    fun parseQualifie0dNameDoesNotGrabNullable() {
        val input = "androidx.collection/MutableScatterMap? something"
        val cursor = Cursor(input)
        val qName = cursor.parseAbiQualifiedName()
        assertThat(qName.toString()).isEqualTo("androidx.collection/MutableScatterMap")
        assertThat(cursor.currentLine).isEqualTo("? something")
    }

    @Test
    fun parseQualifiedNameWithQualifiedReceiver() {
        val input = "androidx.compose.ui.text/AnnotatedString.Builder.BulletScope {"
        val cursor = Cursor(input)
        val qName = cursor.parseAbiQualifiedName()
        assertThat(qName.toString())
            .isEqualTo("androidx.compose.ui.text/AnnotatedString.Builder.BulletScope")
    }

    @Test
    fun parseQualifiedNameBeforeDefaultParameterSymbol() {
        val input = "androidx.collection/MutableScatterMap =..."
        val cursor = Cursor(input)
        val qName = cursor.parseAbiQualifiedName()
        assertThat(qName.toString()).isEqualTo("androidx.collection/MutableScatterMap")
    }

    @Test
    fun parseAbiType() {
        val input = "androidx.collection/ScatterMap<#A, #B> something"
        val cursor = Cursor(input)
        val type = cursor.parseAbiType()
        assertThat(type?.className?.toString()).isEqualTo("androidx.collection/ScatterMap")
        assertThat(cursor.currentLine).isEqualTo("something")
    }

    @Test
    fun parseAbiTypeWithAnotherType() {
        val input =
            "androidx.collection/ScatterMap<#A, #B>, androidx.collection/Other<#A, #B> " +
                "something"
        val cursor = Cursor(input)
        val type = cursor.parseAbiType()
        assertThat(type?.className?.toString()).isEqualTo("androidx.collection/ScatterMap")
        assertThat(cursor.currentLine).isEqualTo(", androidx.collection/Other<#A, #B> something")
    }

    @Test
    fun parseAbiTypeWithThreeParams() {
        val input = "kotlin/Function2<#A1, #A, #A1>"
        val cursor = Cursor(input)
        val type = cursor.parseAbiType()
        assertThat(type?.className?.toString()).isEqualTo("kotlin/Function2")
    }

    @Test
    fun parseSuperTypes() {
        val input =
            ": androidx.collection/ScatterMap<#A, #B>, androidx.collection/Other<#A, #B> " +
                "something"
        val cursor = Cursor(input)
        val superTypes = cursor.parseSuperTypes().toList()
        assertThat(superTypes).hasSize(2)
        assertThat(superTypes.first().className?.toString())
            .isEqualTo("androidx.collection/ScatterMap")
        assertThat(superTypes.last().className?.toString()).isEqualTo("androidx.collection/Other")
        assertThat(cursor.currentLine).isEqualTo("something")
    }

    @Test
    fun parseReturnType() {
        val input = ": androidx.collection/ScatterMap<#A, #B> stuff"
        val cursor = Cursor(input)
        val returnType = cursor.parseReturnType()
        assertThat(returnType?.className?.toString()).isEqualTo("androidx.collection/ScatterMap")
        assertThat(cursor.currentLine).isEqualTo("stuff")
    }

    @Test
    fun parseReturnTypeNullableWithTypeParamsNullable() {
        val input = ": #B? stuff"
        val cursor = Cursor(input)
        val returnType = cursor.parseReturnType()
        assertThat(returnType?.tag).isEqualTo("B")
        assertThat(returnType?.nullability).isEqualTo(AbiTypeNullability.MARKED_NULLABLE)
        assertThat(cursor.currentLine).isEqualTo("stuff")
    }

    @Test
    fun parseReturnTypeNullableWithTypeParamsNotSpecified() {
        val input = ": #B stuff"
        val cursor = Cursor(input)
        val returnType = cursor.parseReturnType()
        assertThat(returnType?.tag).isEqualTo("B")
        assertThat(returnType?.nullability).isEqualTo(AbiTypeNullability.NOT_SPECIFIED)
        assertThat(cursor.currentLine).isEqualTo("stuff")
    }

    @Test
    fun parseFunctionReceiver() {
        val input = "(androidx.collection/LongSparseArray<#A>).androidx.collection/keyIterator()"
        val cursor = Cursor(input)
        val receiver = cursor.parseFunctionReceiver()
        assertThat(receiver?.className.toString()).isEqualTo("androidx.collection/LongSparseArray")
        assertThat(cursor.currentLine).isEqualTo("androidx.collection/keyIterator()")
    }

    @Test
    fun parseFunctionReceiver2() {
        val input = "(androidx.collection/LongSparseArray<#A1>).<get-size>(): kotlin/Int"
        val cursor = Cursor(input)
        val receiver = cursor.parseFunctionReceiver()
        assertThat(receiver?.className.toString()).isEqualTo("androidx.collection/LongSparseArray")
        assertThat(cursor.currentLine).isEqualTo("<get-size>(): kotlin/Int")
    }

    @Test
    fun parseFunctionReceiverWithStarParams() {
        val input = "(androidx.compose.animation.core/AnimationState<*, *>).<get-isFinished>()"
        val cursor = Cursor(input)
        val receiver = cursor.parseFunctionReceiver()
        assertThat(receiver?.className.toString())
            .isEqualTo("androidx.compose.animation.core/AnimationState")
        assertThat(cursor.currentLine).isEqualTo("<get-isFinished>()")
    }

    @Test
    fun parseFunctionReceiverWithNullableParam() {
        val input = "(kotlin.collections/List<#A?>)."
        val cursor = Cursor(input)
        val receiver = cursor.parseFunctionReceiver()
        assertThat(receiver?.className.toString()).isEqualTo("kotlin.collections/List")
        val typeArg = receiver?.arguments?.single()?.type
        assertThat(typeArg?.nullability).isEqualTo(AbiTypeNullability.MARKED_NULLABLE)
        assertThat(typeArg?.tag).isEqualTo("A")
    }

    @Test
    fun parseSimpleContextParams() {
        val input = "context(kotlin/Int)"
        val cursor = Cursor(input)
        val contextParams = cursor.parseContextParams()
        assertThat(contextParams).hasSize(1)
        assertThat(contextParams?.single()?.type?.className.toString()).isEqualTo("kotlin/Int")
    }

    @Test
    fun parseContextWithMultipleParams() {
        val input = "context(kotlin/Int, kotlin.collections/List<kotlin/String>)"
        val cursor = Cursor(input)
        val contextParams = cursor.parseContextParams()
        assertThat(contextParams).hasSize(2)
        assertThat(contextParams?.first()?.type?.className.toString()).isEqualTo("kotlin/Int")
        assertThat(contextParams?.last()?.type?.className.toString())
            .isEqualTo("kotlin.collections/List")
        assertThat(contextParams?.last()?.type?.arguments?.first()?.type?.className.toString())
            .isEqualTo("kotlin/String")
    }

    @Test
    fun parseValueParamCrossinlineDefault() {
        val input = "crossinline kotlin/Function2<#A, #B, kotlin/Int> =..."
        val cursor = Cursor(input)
        val valueParam = cursor.parseValueParameter()!!
        assertThat(valueParam.type.className.toString()).isEqualTo("kotlin/Function2")
        assertThat(valueParam.hasDefaultArg).isTrue()
        assertThat(valueParam.isCrossinline).isTrue()
        assertThat(valueParam.isVararg).isFalse()
    }

    @Test
    fun parseValueParamVararg() {
        val input = "kotlin/Array<out kotlin/Pair<#A, #B>>..."
        val cursor = Cursor(input)
        val valueParam = cursor.parseValueParameter()
        assertThat(valueParam?.type?.className?.toString()).isEqualTo("kotlin/Array")
        assertThat(valueParam?.hasDefaultArg).isFalse()
        assertThat(valueParam?.isCrossinline).isFalse()
        assertThat(valueParam?.isVararg).isTrue()
    }

    @Test
    fun parseValueParametersWithTypeArgs() {
        val input = "kotlin/Array<out #A>..."
        val cursor = Cursor(input)
        val valueParam = cursor.parseValueParameter()
        assertThat(valueParam?.type?.arguments).hasSize(1)
    }

    @Test
    fun parseValueParametersWithTwoTypeArgs() {
        val input = "kotlin/Function1<kotlin/Double, kotlin/Boolean>)"
        val cursor = Cursor(input)
        val valueParam = cursor.parseValueParameter()
        assertThat(valueParam?.type?.arguments).hasSize(2)
    }

    @Test
    fun parseValueParametersEmpty() {
        val input = "() thing"
        val cursor = Cursor(input)
        val params = cursor.parseValueParameters(AbiValueParameterKind.CONTEXT)
        assertThat(params).isEqualTo(emptyList<AbiValueParameter>())
        assertThat(cursor.currentLine).isEqualTo("thing")
    }

    @Test
    fun parseValueParamsSimple() {
        val input = "(kotlin/Function1<#A, kotlin/Boolean>)"
        val cursor = Cursor(input)
        val valueParams = cursor.parseValueParameters(AbiValueParameterKind.CONTEXT)
        assertThat(valueParams).hasSize(1)
    }

    @Test
    fun parseValueParamsTwoArgs() {
        val input = "(#A1, kotlin/Function2<#A1, #A, #A1>)"
        val cursor = Cursor(input)
        val valueParams = cursor.parseValueParameters(AbiValueParameterKind.CONTEXT)
        assertThat(valueParams).hasSize(2)
        assertThat(valueParams?.first()?.type?.tag).isEqualTo("A1")
    }

    @Test
    fun parseValueParamsWithHasDefaultArg() {
        val input = "(kotlin/Int =...)"
        val cursor = Cursor(input)
        val valueParams = cursor.parseValueParameters(AbiValueParameterKind.CONTEXT)
        assertThat(valueParams).hasSize(1)
        assertThat(valueParams?.single()?.hasDefaultArg).isTrue()
    }

    @Test
    fun parseValueParamsComplex2() {
        val input =
            "(kotlin/Int, crossinline kotlin/Function2<#A, #B, kotlin/Int> =..., " +
                "crossinline kotlin/Function1<#A, #B?> =..., " +
                "crossinline kotlin/Function4<kotlin/Boolean, #A, #B, #B?, kotlin/Unit> =...)"
        val cursor = Cursor(input)
        val valueParams = cursor.parseValueParameters(AbiValueParameterKind.CONTEXT)!!
        assertThat(valueParams).hasSize(4)
        assertThat(valueParams.first().type.className?.toString()).isEqualTo("kotlin/Int")
        val rest = valueParams.subList(1, valueParams.size)
        assertThat(rest).hasSize(3)
        assertThat(rest.all { it.hasDefaultArg }).isTrue()
        assertThat(rest.all { it.isCrossinline }).isTrue()
    }

    @Test
    fun parseValueParamsComplex3() {
        val input = "(kotlin/Array<out kotlin/Pair<#A, #B>>...)"
        val cursor = Cursor(input)
        val valueParams = cursor.parseValueParameters(AbiValueParameterKind.CONTEXT)!!
        assertThat(valueParams).hasSize(1)

        assertThat(valueParams.single().isVararg).isTrue()
        val type = valueParams.single().type
        assertThat(type.className.toString()).isEqualTo("kotlin/Array")
    }

    @Test
    fun parseValueParamsWithStarTypeParam() {
        val input = "(androidx.datastore.preferences.core/Preferences.Key<*>)"
        val cursor = Cursor(input)
        val valueParams = cursor.parseValueParameters(AbiValueParameterKind.CONTEXT)!!
        assertThat(valueParams).hasSize(1)
        val valueParam = valueParams.single()
        val type = valueParam.type
        assertThat(type.className.toString())
            .isEqualTo("androidx.datastore.preferences.core/Preferences.Key")
        assertThat(type.arguments).hasSize(1)
        assertThat(type.arguments?.single())
            .isInstanceOf(AbiTypeArgument.StarProjection::class.java)
    }

    @Test
    fun parseTypeParams() {
        val input = "<#A1: kotlin/Any?>"
        val cursor = Cursor(input)
        val typeParams = cursor.parseTypeParams()
        assertThat(typeParams).hasSize(1)
        val type = typeParams?.single()?.upperBounds?.single()
        assertThat(typeParams?.single()?.tag).isEqualTo("A1")
        assertThat(type?.className?.toString()).isEqualTo("kotlin/Any")
        assertThat(type?.nullability).isEqualTo(AbiTypeNullability.MARKED_NULLABLE)
        assertThat(typeParams?.single()?.variance).isEqualTo(AbiVariance.INVARIANT)
    }

    @Test
    fun parseTypeParamsWithVariance() {
        val input = "<#A1: out kotlin/Any?>"
        val cursor = Cursor(input)
        val typeParams = cursor.parseTypeParams()
        assertThat(typeParams).hasSize(1)
        val type = typeParams?.single()?.upperBounds?.single()
        assertThat(typeParams?.single()?.tag).isEqualTo("A1")
        assertThat(type?.className?.toString()).isEqualTo("kotlin/Any")
        assertThat(type?.nullability).isEqualTo(AbiTypeNullability.MARKED_NULLABLE)
        assertThat(typeParams?.single()?.variance).isEqualTo(AbiVariance.OUT)
    }

    @Test
    fun parseTypeParamsWithTwo() {
        val input = "<#A: kotlin/Any?, #B: kotlin/Any?>"
        val cursor = Cursor(input)
        val typeParams = cursor.parseTypeParams()
        assertThat(typeParams).hasSize(2)
        val type1 = typeParams?.first()?.upperBounds?.single()
        val type2 = typeParams?.first()?.upperBounds?.single()
        assertThat(typeParams?.first()?.tag).isEqualTo("A")
        assertThat(typeParams?.last()?.tag).isEqualTo("B")
        assertThat(type1?.className?.toString()).isEqualTo("kotlin/Any")
        assertThat(type1?.nullability).isEqualTo(AbiTypeNullability.MARKED_NULLABLE)
        assertThat(type2?.className?.toString()).isEqualTo("kotlin/Any")
        assertThat(type2?.nullability).isEqualTo(AbiTypeNullability.MARKED_NULLABLE)
    }

    @Test
    fun parseTypeParamsWithMultipleUpperBounds() {
        val input = "<#A: my.lib/A & my.lib/B>"
        val cursor = Cursor(input)
        val typeParams = cursor.parseTypeParams()
        assertThat(typeParams).hasSize(1)
        val upperBounds = typeParams?.single()!!.upperBounds
        assertThat(upperBounds).hasSize(2)
        assertThat(upperBounds.first().className.toString()).isEqualTo("my.lib/A")
        assertThat(upperBounds.last().className.toString()).isEqualTo("my.lib/B")
    }

    @Test
    fun parseTypeParamsReifed() {
        val input = "<#A1: reified kotlin/Any?>"
        val cursor = Cursor(input)
        val typeParam = cursor.parseTypeParams()?.single()
        assertThat(typeParam).isNotNull()
        assertThat(typeParam?.isReified).isTrue()
    }

    @Test
    fun parseTypeParamsDoesNotMatchGetter() {
        val input = "<get-size>"
        val cursor = Cursor(input)
        val typeParams = cursor.parseTypeParams()
        assertThat(typeParams).isNull()
    }

    @Test
    fun parseTypeArgs() {
        val input = "<out #A>"
        val cursor = Cursor(input)
        val typeArgs = cursor.parseTypeArgs()
        assertThat(typeArgs).hasSize(1)
        val typeArg = typeArgs?.single()
        assertThat(typeArg?.type?.tag).isEqualTo("A")
        assertThat(typeArg?.variance).isEqualTo(AbiVariance.OUT)
    }

    @Test
    fun parseTwoTypeArgs() {
        val input = "<kotlin/Double, kotlin/Boolean>"
        val cursor = Cursor(input)
        val typeArgs = cursor.parseTypeArgs()
        assertThat(typeArgs).hasSize(2)
        assertThat(typeArgs?.first()?.type?.className?.toString()).isEqualTo("kotlin/Double")
        assertThat(typeArgs?.last()?.type?.className?.toString()).isEqualTo("kotlin/Boolean")
    }

    @Test
    fun parseTypeArgsWithNestedBrackets() {
        val input =
            "<androidx.collection/ScatterMap<#A, #B>, androidx.collection/Other<#A, #B>>," +
                " something else"
        val cursor = Cursor(input)
        val typeArgs = cursor.parseTypeArgs()
        assertThat(typeArgs).hasSize(2)
        assertThat(cursor.currentLine).isEqualTo(", something else")
    }

    @Test
    fun parseVarargSymbol() {
        val input = "..."
        val cursor = Cursor(input)
        val vararg = cursor.parseVarargSymbol()
        assertThat(vararg).isNotNull()
    }

    @Test
    fun parseTargets() {
        val input = "Targets: [iosX64, linuxX64]"
        val cursor = Cursor(input)
        val targets = cursor.parseTargets()
        assertThat(targets).containsExactly("linuxX64", "iosX64")
    }

    @Test
    fun hasSignatureVersion() {
        val input = "// - Signature version: 2"
        val cursor = Cursor(input)
        assertThat(cursor.hasSignatureVersion()).isTrue()
        assertThat(cursor.currentLine).isEqualTo(input)
    }

    @Test
    fun hasSignatureVersionFalsePositive() {
        val input = "// - Show manifest properties: true"
        val cursor = Cursor(input)
        assertThat(cursor.hasSignatureVersion()).isFalse()
    }

    @Test
    fun parseSignatureVersion() {
        val input = "// - Signature version: 2"
        val cursor = Cursor(input)
        val signatureVersion = cursor.parseSignatureVersion()
        assertThat(signatureVersion.toString()).isEqualTo("V2")
    }

    @Test
    fun parseSignatureVersionFromTheFuture() {
        val input = "// - Signature version: 101"
        val cursor = Cursor(input)
        val signatureVersion = cursor.parseSignatureVersion()
        assertThat(signatureVersion.toString()).isEqualTo("Unsupported(versionNumber=101)")
    }

    @Test
    fun parseEnumEntryName() {
        val input = "SOME_ENUM something else"
        val cursor = Cursor(input)
        val enumName = cursor.parseEnumName()
        assertThat(enumName).isEqualTo("SOME_ENUM")
        assertThat(cursor.currentLine).isEqualTo("something else")
    }

    @Test
    fun parseEnumEntryKind() {
        val input = "enum entry SOME_ENUM"
        val cursor = Cursor(input)
        val enumName = cursor.parseEnumEntryKind()
        assertThat(enumName).isEqualTo("enum entry")
        assertThat(cursor.currentLine).isEqualTo("SOME_ENUM")
    }

    @Test
    fun hasEnumEntry() {
        val input = "enum entry SOME_ENUM"
        val cursor = Cursor(input)
        assertThat(cursor.hasEnumEntry()).isTrue()
        assertThat(cursor.currentLine).isEqualTo(input)
    }

    @Test
    fun parseValidIdentifierAndMaybeTrimForFunctionName() {
        val input = "myFun ()"
        val cursor = Cursor(input)
        assertThat(cursor.parseValidIdentifierAndMaybeTrim()).isEqualTo("myFun ")
    }

    @Test
    fun parseValidIdentifierAndMaybeTrimForClassName() {
        val input = "MyClass  {"
        val cursor = Cursor(input)
        assertThat(cursor.parseValidIdentifierAndMaybeTrim()).isEqualTo("MyClass ")
    }

    @Test
    fun parseValidIdentifierAndMaybeTrimForClassWithSuper() {
        val input = "MyClass = : my.lib/MyClass {"
        val cursor = Cursor(input)
        assertThat(cursor.parseValidIdentifierAndMaybeTrim()).isEqualTo("MyClass =")
    }

    @Test
    fun parseValidIdentifierAndMaybeTrimForForValueParameter() {
        val input = "MyClass ,"
        val cursor = Cursor(input)
        assertThat(cursor.parseValidIdentifierAndMaybeTrim()).isEqualTo("MyClass ")
    }
}
