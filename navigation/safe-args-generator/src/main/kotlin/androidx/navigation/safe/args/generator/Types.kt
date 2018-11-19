/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.navigation.safe.args.generator

import androidx.navigation.safe.args.generator.ext.N
import androidx.navigation.safe.args.generator.ext.S
import androidx.navigation.safe.args.generator.ext.T
import androidx.navigation.safe.args.generator.models.Argument
import androidx.navigation.safe.args.generator.models.ResReference
import androidx.navigation.safe.args.generator.models.accessor
import com.squareup.javapoet.ArrayTypeName
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.CodeBlock
import com.squareup.javapoet.MethodSpec
import com.squareup.javapoet.TypeName
import javax.naming.OperationNotSupportedException

sealed class NavType {
    open fun addBundleGetStatement(
        builder: MethodSpec.Builder,
        arg: Argument,
        lValue: String,
        bundle: String
    ): MethodSpec.Builder {
        return builder.addStatement("$N = $N.$N($S)", lValue, bundle, bundleGetMethod(), arg.name)
    }

    open fun addBundlePutStatement(
        builder: MethodSpec.Builder,
        arg: Argument,
        bundle: String,
        argValue: String
    ): MethodSpec.Builder {
        return builder.addStatement(
                "$N.$N($S, $N)",
                bundle,
                bundlePutMethod(),
                arg.name,
                argValue
        )
    }

    abstract fun typeName(): TypeName
    abstract fun bundlePutMethod(): String
    abstract fun bundleGetMethod(): String
    abstract fun allowsNullable(): Boolean

    companion object {
        fun from(name: String?, rFilePackage: String? = null) = when (name) {
            "integer" -> IntType
            "integer[]" -> IntArrayType
            "long" -> LongType
            "long[]" -> LongArrayType
            "float" -> FloatType
            "float[]" -> FloatArrayType
            "boolean" -> BoolType
            "boolean[]" -> BoolArrayType
            "reference" -> ReferenceType
            "reference[]" -> ReferenceArrayType
            "string" -> StringType
            "string[]" -> StringArrayType
            null -> StringType
            else -> {
                val prependPackageName = if (name.startsWith(".") && rFilePackage != null) {
                    rFilePackage
                } else {
                    ""
                }
                if (name.endsWith("[]")) {
                    name.substringBeforeLast("[]").let { clsName ->
                        ObjectArrayType(ClassName.get(
                                prependPackageName + clsName.substringBeforeLast('.', ""),
                                clsName.substringAfterLast('.')
                        ))
                    }
                } else {
                    ObjectType(ClassName.get(
                            prependPackageName + name.substringBeforeLast('.', ""),
                            name.substringAfterLast('.')
                    ))
                }
            }
        }
    }
}

object IntType : NavType() {
    override fun typeName(): TypeName = TypeName.INT
    override fun bundlePutMethod() = "putInt"
    override fun bundleGetMethod() = "getInt"
    override fun toString() = "integer"
    override fun allowsNullable() = false
}

object IntArrayType : NavType() {
    override fun typeName(): TypeName = ArrayTypeName.of(TypeName.INT)
    override fun bundlePutMethod() = "putIntArray"
    override fun bundleGetMethod() = "getIntArray"
    override fun toString() = "integer[]"
    override fun allowsNullable() = true
}

object LongType : NavType() {
    override fun typeName(): TypeName = TypeName.LONG
    override fun bundlePutMethod() = "putLong"
    override fun bundleGetMethod() = "getLong"
    override fun toString() = "long"
    override fun allowsNullable() = false
}

object LongArrayType : NavType() {
    override fun typeName(): TypeName = ArrayTypeName.of(TypeName.LONG)
    override fun bundlePutMethod() = "putLongArray"
    override fun bundleGetMethod() = "getLongArray"
    override fun toString() = "long[]"
    override fun allowsNullable() = true
}

object FloatType : NavType() {
    override fun typeName(): TypeName = TypeName.FLOAT
    override fun bundlePutMethod() = "putFloat"
    override fun bundleGetMethod() = "getFloat"
    override fun toString() = "float"
    override fun allowsNullable() = false
}

object FloatArrayType : NavType() {
    override fun typeName(): TypeName = ArrayTypeName.of(TypeName.FLOAT)
    override fun bundlePutMethod() = "putFloatArray"
    override fun bundleGetMethod() = "getFloatArray"
    override fun toString() = "float[]"
    override fun allowsNullable() = true
}

object StringType : NavType() {
    override fun typeName(): TypeName = ClassName.get(String::class.java)
    override fun bundlePutMethod() = "putString"
    override fun bundleGetMethod() = "getString"
    override fun toString() = "string"
    override fun allowsNullable() = true
}

object StringArrayType : NavType() {
    override fun typeName(): TypeName = ArrayTypeName.of(ClassName.get(String::class.java))
    override fun bundlePutMethod() = "putStringArray"
    override fun bundleGetMethod() = "getStringArray"
    override fun toString() = "string[]"
    override fun allowsNullable() = true
}

object BoolType : NavType() {
    override fun typeName(): TypeName = TypeName.BOOLEAN
    override fun bundlePutMethod() = "putBoolean"
    override fun bundleGetMethod() = "getBoolean"
    override fun toString() = "boolean"
    override fun allowsNullable() = false
}

object BoolArrayType : NavType() {
    override fun typeName(): TypeName = ArrayTypeName.of(TypeName.BOOLEAN)
    override fun bundlePutMethod() = "putBooleanArray"
    override fun bundleGetMethod() = "getBooleanArray"
    override fun toString() = "boolean"
    override fun allowsNullable() = false
}

object ReferenceType : NavType() {
    // it is internally the same as INT, but we don't want to allow to
    // assignment between int and reference args
    override fun typeName(): TypeName = TypeName.INT

    override fun bundlePutMethod() = "putInt"
    override fun bundleGetMethod() = "getInt"
    override fun toString() = "reference"
    override fun allowsNullable() = false
}

object ReferenceArrayType : NavType() {
    // it is internally the same as INT, but we don't want to allow to
    // assignment between int and reference args
    override fun typeName(): TypeName = ArrayTypeName.of(TypeName.INT)

    override fun bundlePutMethod() = "putIntArray"
    override fun bundleGetMethod() = "getIntArray"
    override fun toString() = "reference[]"
    override fun allowsNullable() = true
}

data class ObjectType(private val typeName: TypeName) : NavType() {
    override fun typeName(): TypeName = typeName
    override fun bundlePutMethod() =
            throw OperationNotSupportedException("Use addBundlePutStatement instead.")

    override fun bundleGetMethod() =
            throw OperationNotSupportedException("Use addBundleGetStatement instead.")

    override fun toString() = "parcelable or serializable"
    override fun allowsNullable() = true
    override fun addBundleGetStatement(
        builder: MethodSpec.Builder,
        arg: Argument,
        lValue: String,
        bundle: String
    ): MethodSpec.Builder {
        return builder.apply {
            beginControlFlow("if ($T.class.isAssignableFrom($T.class) " +
                    "|| $T.class.isAssignableFrom($T.class))",
                    parcelableType, arg.type.typeName(),
                    serializableType, arg.type.typeName())
                    .apply {
                        addStatement(
                                "$N = ($T) $N.$N($S)",
                                lValue, arg.type.typeName(), bundle, "get", arg.name
                        )
                    }.nextControlFlow("else").apply {
                        addStatement(
                                "throw new UnsupportedOperationException($T.class.getName() + " +
                                        "\" must implement Parcelable or Serializable " +
                                        "or must be an Enum.\")",
                                arg.type.typeName())
                    }.endControlFlow()
        }
    }

    override fun addBundlePutStatement(
        builder: MethodSpec.Builder,
        arg: Argument,
        bundle: String,
        argValue: String
    ): MethodSpec.Builder {
        return builder.apply {
            beginControlFlow("if ($T.class.isAssignableFrom($T.class) || $N == null)",
                    parcelableType, arg.type.typeName(), argValue).apply {
                addStatement(
                        "$N.$N($S, $T.class.cast($N))",
                        bundle, "putParcelable", arg.name, parcelableType, argValue
                )
            }.nextControlFlow("else if ($T.class.isAssignableFrom($T.class))",
                    serializableType, arg.type.typeName()).apply {
                addStatement(
                        "$N.$N($S, $T.class.cast($N))",
                        bundle, "putSerializable", arg.name, serializableType, argValue
                )
            }.nextControlFlow("else").apply {
                addStatement("throw new UnsupportedOperationException($T.class.getName() + " +
                        "\" must implement Parcelable or Serializable or must be an Enum.\")",
                        arg.type.typeName())
            }.endControlFlow()
        }
    }

    companion object {
        private val parcelableType = ClassName.get("android.os", "Parcelable")
        private val serializableType = ClassName.get("java.io", "Serializable")
    }
}

data class ObjectArrayType(private val typeName: TypeName) : NavType() {
    override fun typeName(): TypeName = ArrayTypeName.of(typeName)
    override fun bundlePutMethod() = "putParcelableArray"
    override fun bundleGetMethod() = "getParcelableArray"
    override fun toString() = "parcelable array"
    override fun allowsNullable() = true

    override fun addBundleGetStatement(
        builder: MethodSpec.Builder,
        arg: Argument,
        lValue: String,
        bundle: String
    ): MethodSpec.Builder {
        return builder.addStatement("$N = ($T) $N.$N($S)",
            lValue, typeName(), bundle, bundleGetMethod(), arg.name)
    }
}

sealed class WriteableValue {
    abstract fun write(): CodeBlock
}

data class ReferenceValue(private val resReference: ResReference) : WriteableValue() {
    override fun write(): CodeBlock = CodeBlock.of(resReference.accessor())
}

data class StringValue(private val value: String) : WriteableValue() {
    override fun write(): CodeBlock = CodeBlock.of(S, value)
}

// keeping value as String, it will help to preserve client format of it: hex, dec
data class IntValue(private val value: String) : WriteableValue() {
    override fun write(): CodeBlock = CodeBlock.of(value)
}

// keeping value as String, it will help to preserve client format of it: hex, dec
data class LongValue(private val value: String) : WriteableValue() {
    override fun write(): CodeBlock = CodeBlock.of(value)
}

// keeping value as String, it will help to preserve client format of it: scientific, dot
data class FloatValue(private val value: String) : WriteableValue() {
    override fun write(): CodeBlock = CodeBlock.of("${value}F")
}

data class BooleanValue(private val value: String) : WriteableValue() {
    override fun write(): CodeBlock = CodeBlock.of(value)
}

object NullValue : WriteableValue() {
    override fun write(): CodeBlock = CodeBlock.of("null")
}

data class EnumValue(private val type: TypeName, private val value: String) : WriteableValue() {
    override fun write(): CodeBlock = CodeBlock.of("$T.$N", type, value)
}