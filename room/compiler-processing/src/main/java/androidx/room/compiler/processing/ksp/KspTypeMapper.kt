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

    init {
        // https://kotlinlang.org/docs/reference/java-interop.html#mapped-types
        kotlinTypeToJavaPrimitiveMapping.forEach {
            mapping[it.value.toString()] = it.key
        }
        // TODO Add non primitives after TypeNames move to the java type realm.
    }

    fun swapWithKotlinType(javaType: String): String = mapping[javaType] ?: javaType

    fun getPrimitiveJavaTypeName(kotlinType: String) = kotlinTypeToJavaPrimitiveMapping[kotlinType]

    fun isJavaPrimitiveType(qName: String) = mapping[qName]?.let {
        kotlinTypeToJavaPrimitiveMapping[it]
    } != null
}
