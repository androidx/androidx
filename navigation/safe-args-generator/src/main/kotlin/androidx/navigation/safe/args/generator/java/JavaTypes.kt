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

package androidx.navigation.safe.args.generator.java

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
import androidx.navigation.safe.args.generator.StringArrayType
import androidx.navigation.safe.args.generator.StringType
import androidx.navigation.safe.args.generator.models.Argument
import androidx.navigation.safe.args.generator.ReferenceValue
import androidx.navigation.safe.args.generator.StringValue
import androidx.navigation.safe.args.generator.WritableValue
import androidx.navigation.safe.args.generator.ext.toClassNameParts
import androidx.navigation.safe.args.generator.models.ResReference
import com.squareup.javapoet.ArrayTypeName
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.CodeBlock
import com.squareup.javapoet.MethodSpec
import com.squareup.javapoet.TypeName

internal val NAV_DIRECTION_CLASSNAME: ClassName =
    ClassName.get("androidx.navigation", "NavDirections")
internal val ACTION_ONLY_NAV_DIRECTION_CLASSNAME: ClassName =
    ClassName.get("androidx.navigation", "ActionOnlyNavDirections")
internal val NAV_ARGS_CLASSNAME: ClassName = ClassName.get("androidx.navigation", "NavArgs")
internal val HASHMAP_CLASSNAME: ClassName = ClassName.get("java.util", "HashMap")
internal val BUNDLE_CLASSNAME: ClassName = ClassName.get("android.os", "Bundle")
internal val PARCELABLE_CLASSNAME = ClassName.get("android.os", "Parcelable")
internal val SERIALIZABLE_CLASSNAME = ClassName.get("java.io", "Serializable")
internal val SYSTEM_CLASSNAME = ClassName.get("java.lang", "System")

internal abstract class Annotations {
    abstract val NULLABLE_CLASSNAME: ClassName
    abstract val NONNULL_CLASSNAME: ClassName

    private object AndroidAnnotations : Annotations() {
        override val NULLABLE_CLASSNAME = ClassName.get("android.support.annotation", "Nullable")
        override val NONNULL_CLASSNAME = ClassName.get("android.support.annotation", "NonNull")
    }

    private object AndroidXAnnotations : Annotations() {
        override val NULLABLE_CLASSNAME = ClassName.get("androidx.annotation", "Nullable")
        override val NONNULL_CLASSNAME = ClassName.get("androidx.annotation", "NonNull")
    }

    companion object {
        fun getInstance(useAndroidX: Boolean) = if (useAndroidX) {
            AndroidXAnnotations
        } else {
            AndroidAnnotations
        }
    }
}

internal fun NavType.addBundleGetStatement(
    builder: MethodSpec.Builder,
    arg: Argument,
    lValue: String,
    bundle: String
): MethodSpec.Builder = when (this) {
    is ObjectType -> builder.apply {
        beginControlFlow(
            "if ($T.class.isAssignableFrom($T.class) " +
                    "|| $T.class.isAssignableFrom($T.class))",
            PARCELABLE_CLASSNAME, arg.type.typeName(),
            SERIALIZABLE_CLASSNAME, arg.type.typeName()
        ).apply {
            addStatement(
                "$N = ($T) $N.$N($S)",
                lValue, arg.type.typeName(), bundle, "get", arg.name
            )
        }.nextControlFlow("else").apply {
            addStatement(
                "throw new UnsupportedOperationException($T.class.getName() + " +
                        "\" must implement Parcelable or Serializable " +
                        "or must be an Enum.\")",
                arg.type.typeName()
            )
        }.endControlFlow()
    }
    is ObjectArrayType -> builder.apply {
        val arrayName = "__array"
        val baseType = (arg.type.typeName() as ArrayTypeName).componentType
        addStatement("$T[] $N = $N.$N($S)",
            PARCELABLE_CLASSNAME, arrayName, bundle, bundleGetMethod(), arg.name)
        beginControlFlow("if ($N != null)", arrayName).apply {
            addStatement("$N = new $T[$N.length]", lValue, baseType, arrayName)
            addStatement("$T.arraycopy($N, 0, $N, 0, $N.length)",
                SYSTEM_CLASSNAME, arrayName, lValue, arrayName
            )
        }
        nextControlFlow("else").apply {
            addStatement("$N = null", lValue)
        }
        endControlFlow()
    }
    else -> builder.addStatement(
        "$N = $N.$N($S)",
        lValue,
        bundle,
        bundleGetMethod(),
        arg.name
    )
}

internal fun NavType.addBundlePutStatement(
    builder: MethodSpec.Builder,
    arg: Argument,
    bundle: String,
    argValue: String
): MethodSpec.Builder = when (this) {
    is ObjectType -> builder.apply {
        beginControlFlow(
            "if ($T.class.isAssignableFrom($T.class) || $N == null)",
            PARCELABLE_CLASSNAME, arg.type.typeName(), argValue
        ).apply {
            addStatement(
                "$N.$N($S, $T.class.cast($N))",
                bundle, "putParcelable", arg.name, PARCELABLE_CLASSNAME, argValue
            )
        }.nextControlFlow(
            "else if ($T.class.isAssignableFrom($T.class))",
            SERIALIZABLE_CLASSNAME, arg.type.typeName()
        ).apply {
            addStatement(
                "$N.$N($S, $T.class.cast($N))",
                bundle, "putSerializable", arg.name, SERIALIZABLE_CLASSNAME, argValue
            )
        }.nextControlFlow("else").apply {
            addStatement(
                "throw new UnsupportedOperationException($T.class.getName() + " +
                        "\" must implement Parcelable or Serializable or must be an Enum.\")",
                arg.type.typeName()
            )
        }.endControlFlow()
    }
    else -> builder.addStatement(
        "$N.$N($S, $N)",
        bundle,
        bundlePutMethod(),
        arg.name,
        argValue
    )
}

internal fun NavType.typeName(): TypeName = when (this) {
    IntType -> TypeName.INT
    IntArrayType -> ArrayTypeName.of(TypeName.INT)
    LongType -> TypeName.LONG
    LongArrayType -> ArrayTypeName.of(TypeName.LONG)
    FloatType -> TypeName.FLOAT
    FloatArrayType -> ArrayTypeName.of(TypeName.FLOAT)
    StringType -> ClassName.get(String::class.java)
    StringArrayType -> ArrayTypeName.of(ClassName.get(String::class.java))
    BoolType -> TypeName.BOOLEAN
    BoolArrayType -> ArrayTypeName.of(TypeName.BOOLEAN)
    ReferenceType -> TypeName.INT
    ReferenceArrayType -> ArrayTypeName.of(TypeName.INT)
    is ObjectType -> canonicalName.toClassNameParts().let { (packageName, simpleName, innerNames) ->
        ClassName.get(packageName, simpleName, *innerNames)
    }
    is ObjectArrayType -> ArrayTypeName.of(
        canonicalName.toClassNameParts().let { (packageName, simpleName, innerNames) ->
            ClassName.get(packageName, simpleName, *innerNames)
        })
    else -> throw IllegalStateException("Unknown type: $this")
}

internal fun WritableValue.write(): CodeBlock {
    return when (this) {
        is ReferenceValue -> resReference.accessor()
        is StringValue -> CodeBlock.of(S, value)
        is IntValue -> CodeBlock.of(value)
        is LongValue -> CodeBlock.of(value)
        is FloatValue -> CodeBlock.of("${value}F")
        is BooleanValue -> CodeBlock.of(value)
        is NullValue -> CodeBlock.of("null")
        is EnumValue -> CodeBlock.of("$T.$N", type.typeName(), value)
        else -> throw IllegalStateException("Unknown value: $this")
    }
}

internal fun ResReference?.accessor() = this?.let {
    CodeBlock.of("$T.$N", ClassName.get(packageName, "R", resType), javaIdentifier)
} ?: CodeBlock.of("0")