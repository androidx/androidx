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

import androidx.navigation.safe.args.generator.models.ResReference

interface NavType {
    fun bundlePutMethod(): String
    fun bundleGetMethod(): String
    fun allowsNullable(): Boolean

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
                    ObjectArrayType(prependPackageName + name.substringBeforeLast("[]"))
                } else {
                    ObjectType(prependPackageName + name)
                }
            }
        }
    }
}

object IntType : NavType {
    override fun bundlePutMethod() = "putInt"
    override fun bundleGetMethod() = "getInt"
    override fun toString() = "integer"
    override fun allowsNullable() = false
}

object IntArrayType : NavType {
    override fun bundlePutMethod() = "putIntArray"
    override fun bundleGetMethod() = "getIntArray"
    override fun toString() = "integer[]"
    override fun allowsNullable() = true
}

object LongType : NavType {
    override fun bundlePutMethod() = "putLong"
    override fun bundleGetMethod() = "getLong"
    override fun toString() = "long"
    override fun allowsNullable() = false
}

object LongArrayType : NavType {
    override fun bundlePutMethod() = "putLongArray"
    override fun bundleGetMethod() = "getLongArray"
    override fun toString() = "long[]"
    override fun allowsNullable() = true
}

object FloatType : NavType {
    override fun bundlePutMethod() = "putFloat"
    override fun bundleGetMethod() = "getFloat"
    override fun toString() = "float"
    override fun allowsNullable() = false
}

object FloatArrayType : NavType {
    override fun bundlePutMethod() = "putFloatArray"
    override fun bundleGetMethod() = "getFloatArray"
    override fun toString() = "float[]"
    override fun allowsNullable() = true
}

object StringType : NavType {
    override fun bundlePutMethod() = "putString"
    override fun bundleGetMethod() = "getString"
    override fun toString() = "string"
    override fun allowsNullable() = true
}

object StringArrayType : NavType {
    override fun bundlePutMethod() = "putStringArray"
    override fun bundleGetMethod() = "getStringArray"
    override fun toString() = "string[]"
    override fun allowsNullable() = true
}

object BoolType : NavType {
    override fun bundlePutMethod() = "putBoolean"
    override fun bundleGetMethod() = "getBoolean"
    override fun toString() = "boolean"
    override fun allowsNullable() = false
}

object BoolArrayType : NavType {
    override fun bundlePutMethod() = "putBooleanArray"
    override fun bundleGetMethod() = "getBooleanArray"
    override fun toString() = "boolean"
    override fun allowsNullable() = true
}

object ReferenceType : NavType {
    override fun bundlePutMethod() = "putInt"
    override fun bundleGetMethod() = "getInt"
    override fun toString() = "reference"
    override fun allowsNullable() = false
}

object ReferenceArrayType : NavType {
    override fun bundlePutMethod() = "putIntArray"
    override fun bundleGetMethod() = "getIntArray"
    override fun toString() = "reference[]"
    override fun allowsNullable() = true
}

data class ObjectType(val canonicalName: String) : NavType {
    override fun bundlePutMethod() =
        throw UnsupportedOperationException("Use addBundlePutStatement instead.")

    override fun bundleGetMethod() =
        throw UnsupportedOperationException("Use addBundleGetStatement instead.")

    override fun toString() = "parcelable or serializable"
    override fun allowsNullable() = true
}

data class ObjectArrayType(val canonicalName: String) : NavType {
    override fun bundlePutMethod() = "putParcelableArray"
    override fun bundleGetMethod() = "getParcelableArray"
    override fun toString() = "parcelable array"
    override fun allowsNullable() = true
}

interface WritableValue

data class ReferenceValue(val resReference: ResReference) : WritableValue

data class StringValue(val value: String) : WritableValue

// keeping value as String, it will help to preserve client format of it: hex, dec
data class IntValue(val value: String) : WritableValue

// keeping value as String, it will help to preserve client format of it: hex, dec
data class LongValue(val value: String) : WritableValue

// keeping value as String, it will help to preserve client format of it: scientific, dot
data class FloatValue(val value: String) : WritableValue

data class BooleanValue(val value: String) : WritableValue

object NullValue : WritableValue

data class EnumValue(val type: ObjectType, val value: String) : WritableValue
