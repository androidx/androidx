/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.navigation.safe.args.generator.kotlin

import androidx.navigation.safe.args.generator.BoolArrayType
import androidx.navigation.safe.args.generator.BoolType
import androidx.navigation.safe.args.generator.BooleanValue
import androidx.navigation.safe.args.generator.EnumValue
import androidx.navigation.safe.args.generator.FloatArrayType
import androidx.navigation.safe.args.generator.FloatType
import androidx.navigation.safe.args.generator.FloatValue
import androidx.navigation.safe.args.generator.IntArrayType
import androidx.navigation.safe.args.generator.IntType
import androidx.navigation.safe.args.generator.IntValue
import androidx.navigation.safe.args.generator.LongArrayType
import androidx.navigation.safe.args.generator.LongType
import androidx.navigation.safe.args.generator.LongValue
import androidx.navigation.safe.args.generator.NavType
import androidx.navigation.safe.args.generator.NullValue
import androidx.navigation.safe.args.generator.ObjectArrayType
import androidx.navigation.safe.args.generator.ObjectType
import androidx.navigation.safe.args.generator.ReferenceArrayType
import androidx.navigation.safe.args.generator.ReferenceType
import androidx.navigation.safe.args.generator.ReferenceValue
import androidx.navigation.safe.args.generator.StringArrayType
import androidx.navigation.safe.args.generator.StringType
import androidx.navigation.safe.args.generator.StringValue
import androidx.navigation.safe.args.generator.WritableValue
import androidx.navigation.safe.args.generator.ext.toClassNameParts
import androidx.navigation.safe.args.generator.models.Argument
import androidx.navigation.safe.args.generator.models.ResReference
import com.squareup.kotlinpoet.ARRAY
import com.squareup.kotlinpoet.BOOLEAN
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FLOAT
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.INT
import com.squareup.kotlinpoet.LONG
import com.squareup.kotlinpoet.ParameterizedTypeName
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.asTypeName
import java.lang.UnsupportedOperationException

internal val NAV_DIRECTION_CLASSNAME: ClassName = ClassName("androidx.navigation", "NavDirections")
internal val ACTION_ONLY_NAV_DIRECTION_CLASSNAME: ClassName =
    ClassName("androidx.navigation", "ActionOnlyNavDirections")
internal val NAV_ARGS_CLASSNAME: ClassName = ClassName("androidx.navigation", "NavArgs")
internal val BUNDLE_CLASSNAME: ClassName = ClassName("android.os", "Bundle")
internal val SAVED_STATE_HANDLE_CLASSNAME: ClassName =
    ClassName("androidx.lifecycle", "SavedStateHandle")

internal val PARCELABLE_CLASSNAME = ClassName("android.os", "Parcelable")
internal val SERIALIZABLE_CLASSNAME = ClassName("java.io", "Serializable")

internal fun NavType.addBundleGetStatement(
    builder: FunSpec.Builder,
    arg: Argument,
    lValue: String,
    bundle: String
): FunSpec.Builder = when (this) {
    is ObjectType -> builder.apply {
        beginControlFlow(
            "if (%T::class.java.isAssignableFrom(%T::class.java) " +
                "|| %T::class.java.isAssignableFrom(%T::class.java))",
            PARCELABLE_CLASSNAME, arg.type.typeName(),
            SERIALIZABLE_CLASSNAME, arg.type.typeName()
        )
        addStatement(
            "%L = %L.%L(%S)·as·%T",
            lValue, bundle, "get", arg.name, arg.type.typeName().copy(nullable = true)
        )
        nextControlFlow("else")
        addStatement(
            "throw·%T(%T::class.java.name + %S)",
            UnsupportedOperationException::class.asTypeName(),
            arg.type.typeName(),
            " must implement Parcelable or Serializable or must be an Enum."
        )
        endControlFlow()
    }
    is ObjectArrayType -> builder.apply {
        val baseType = (arg.type.typeName() as ParameterizedTypeName).typeArguments.first()
        addStatement(
            "%L = %L.%L(%S)?.map { it as %T }?.toTypedArray()",
            lValue, bundle, bundleGetMethod(), arg.name, baseType
        )
    }
    else -> builder.addStatement(
        "%L = %L.%L(%S)",
        lValue,
        bundle,
        bundleGetMethod(),
        arg.name
    )
}

internal fun NavType.addBundlePutStatement(
    builder: FunSpec.Builder,
    arg: Argument,
    bundle: String,
    argValue: String
): FunSpec.Builder = when (this) {
    is ObjectType -> builder.apply {
        beginControlFlow(
            "if (%T::class.java.isAssignableFrom(%T::class.java))",
            PARCELABLE_CLASSNAME, arg.type.typeName()
        )
        addStatement(
            "%L.%L(%S, %L as %T)",
            bundle, "putParcelable", arg.name, argValue,
            PARCELABLE_CLASSNAME.copy(nullable = arg.isNullable)
        )
        nextControlFlow(
            "else if (%T::class.java.isAssignableFrom(%T::class.java))",
            SERIALIZABLE_CLASSNAME, arg.type.typeName()
        )
        addStatement(
            "%L.%L(%S, %L as %T)",
            bundle, "putSerializable", arg.name, argValue,
            SERIALIZABLE_CLASSNAME.copy(nullable = arg.isNullable)
        )
        if (!arg.isOptional()) {
            nextControlFlow("else")
            addStatement(
                "throw·%T(%T::class.java.name + %S)",
                UnsupportedOperationException::class.asTypeName(),
                arg.type.typeName(),
                " must implement Parcelable or Serializable or must be an Enum."
            )
        }
        endControlFlow()
    }
    else -> builder.addStatement(
        "%L.%L(%S, %L)",
        bundle,
        bundlePutMethod(),
        arg.name,
        argValue
    )
}

internal fun NavType.addSavedStateSetStatement(
    builder: FunSpec.Builder,
    arg: Argument,
    savedStateHandle: String,
    argValue: String
): FunSpec.Builder = when (this) {
    is ObjectType -> builder.apply {
        beginControlFlow(
            "if (%T::class.java.isAssignableFrom(%T::class.java))",
            PARCELABLE_CLASSNAME, arg.type.typeName()
        )
        addStatement(
            "%L.set(%S, %L as %T)",
            savedStateHandle, arg.name, argValue,
            PARCELABLE_CLASSNAME.copy(nullable = arg.isNullable)
        )
        nextControlFlow(
            "else if (%T::class.java.isAssignableFrom(%T::class.java))",
            SERIALIZABLE_CLASSNAME, arg.type.typeName()
        )
        addStatement(
            "%L.set(%S, %L as %T)",
            savedStateHandle, arg.name, argValue,
            SERIALIZABLE_CLASSNAME.copy(nullable = arg.isNullable)
        )
        if (!arg.isOptional()) {
            nextControlFlow("else")
            addStatement(
                "throw·%T(%T::class.java.name + %S)",
                UnsupportedOperationException::class.asTypeName(),
                arg.type.typeName(),
                " must implement Parcelable or Serializable or must be an Enum."
            )
        }
        endControlFlow()
    }
    else -> builder.addStatement(
        "%L.set(%S, %L)",
        savedStateHandle,
        arg.name,
        argValue
    )
}

internal fun NavType.typeName(): TypeName = when (this) {
    IntType -> INT
    IntArrayType -> IntArray::class.asTypeName()
    LongType -> LONG
    LongArrayType -> LongArray::class.asTypeName()
    FloatType -> FLOAT
    FloatArrayType -> FloatArray::class.asTypeName()
    StringType -> String::class.asTypeName()
    StringArrayType -> ARRAY.parameterizedBy(String::class.asTypeName())
    BoolType -> BOOLEAN
    BoolArrayType -> BooleanArray::class.asTypeName()
    ReferenceType -> INT
    ReferenceArrayType -> IntArray::class.asTypeName()
    is ObjectType ->
        canonicalName.toClassNameParts().let { (packageName, simpleName, innerNames) ->
            ClassName(packageName, simpleName, *innerNames)
        }
    is ObjectArrayType -> ARRAY.parameterizedBy(
        canonicalName.toClassNameParts().let { (packageName, simpleName, innerNames) ->
            ClassName(packageName, simpleName, *innerNames)
        }
    )
    else -> throw IllegalStateException("Unknown type: $this")
}

internal fun WritableValue.write(): CodeBlock {
    return when (this) {
        is ReferenceValue -> resReference.accessor()
        is StringValue -> CodeBlock.of("%S", value)
        is IntValue -> CodeBlock.of(value)
        is LongValue -> CodeBlock.of(value)
        is FloatValue -> CodeBlock.of("${value}F")
        is BooleanValue -> CodeBlock.of(value)
        is NullValue -> CodeBlock.of("null")
        is EnumValue -> CodeBlock.of("%T.%N", type.typeName(), value)
        else -> throw IllegalStateException("Unknown value: $this")
    }
}

internal fun ResReference?.accessor() = this?.let {
    CodeBlock.of("%T.%N", ClassName(packageName, "R", resType), javaIdentifier)
} ?: CodeBlock.of("0")