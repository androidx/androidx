/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.room.compiler.processing.ksp

import com.squareup.javapoet.TypeName

/**
 * Maps java specific types to their kotlin counterparts.
 * see: https://github.com/google/ksp/issues/126
 * see: https://github.com/google/ksp/issues/125
 *
 * `Resolver.getClassDeclarationByName` returns the java representation of a class even when a
 * kotlin version of the same class exists. e.g. It returns a KSClassDeclaration representing
 * `java.lang.String` if queried with `java.lang.String`. Even though this makes sense by itself,
 * it is inconsistent with the kotlin compiler which will resolve all instances of
 * `java.lang.String` to `kotlin.String` (even if it is in Java source code).
 *
 * Until KSP provides compiler consistent behavior, this helper utility does the mapping for us.
 *
 * This list is built from https://kotlinlang.org/docs/reference/java-interop.html#mapped-types.
 * Hopefully, it will be temporary until KSP provides a utility to do the same conversion.
 */
object KspTypeMapper {
    private val mapping = mutableMapOf<String, String>()
    private val kotlinTypeToJavaPrimitiveMapping = mapOf(
        "kotlin.Byte" to TypeName.BYTE,
        "kotlin.Short" to TypeName.SHORT,
        "kotlin.Int" to TypeName.INT,
        "kotlin.Long" to TypeName.LONG,
        "kotlin.Char" to TypeName.CHAR,
        "kotlin.Float" to TypeName.FLOAT,
        "kotlin.Double" to TypeName.DOUBLE,
        "kotlin.Boolean" to TypeName.BOOLEAN
    )
    private val javaPrimitiveQNames = kotlinTypeToJavaPrimitiveMapping
        .values.mapTo(mutableSetOf()) {
            it.toString()
        }

    init {
        // https://kotlinlang.org/docs/reference/java-interop.html#mapped-types
        kotlinTypeToJavaPrimitiveMapping.forEach {
            mapping[it.value.toString()] = it.key
        }
        mapping["java.lang.Object"] = "kotlin.Any"
        mapping["java.lang.Cloneable"] = "kotlin.Cloneable"
        mapping["java.lang.Comparable"] = "kotlin.Comparable"
        mapping["java.lang.Enum"] = "kotlin.Enum"
        mapping["java.lang.Annotation"] = "kotlin.Annotation"
        mapping["java.lang.CharSequence"] = "kotlin.CharSequence"
        mapping["java.lang.String"] = "kotlin.String"
        mapping["java.lang.Number"] = "kotlin.Number"
        mapping["java.lang.Throwable"] = "kotlin.Throwable"
        mapping["java.lang.Byte"] = "kotlin.Byte"
        mapping["java.lang.Short"] = "kotlin.Short"
        mapping["java.lang.Integer"] = "kotlin.Int"
        mapping["java.lang.Long"] = "kotlin.Long"
        mapping["java.lang.Character"] = "kotlin.Char"
        mapping["java.lang.Float"] = "kotlin.Float"
        mapping["java.lang.Double"] = "kotlin.Double"
        mapping["java.lang.Boolean"] = "kotlin.Boolean"
        // collections. default to mutable ones since java types are always mutable
        mapping["java.util.Iterator"] = "kotlin.collections.MutableIterator"
        mapping["java.lang.Iterable"] = "kotlin.collections.Iterable"
        mapping["java.util.Collection"] = "kotlin.collections.MutableCollection"
        mapping["java.util.Set"] = "kotlin.collections.MutableSet"
        mapping["java.util.List"] = "kotlin.collections.MutableList"
        mapping["java.util.ListIterator"] = "kotlin.collections.ListIterator"
        mapping["java.util.Map"] = "kotlin.collections.MutableMap"
        mapping["java.util.Map.Entry"] = "Map.kotlin.collections.MutableEntry"
    }

    fun swapWithKotlinType(javaType: String): String = mapping[javaType] ?: javaType

    fun getPrimitiveJavaTypeName(kotlinType: String) = kotlinTypeToJavaPrimitiveMapping[kotlinType]

    fun isJavaPrimitiveType(qName: String) = javaPrimitiveQNames.contains(qName)
}
