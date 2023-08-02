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

package androidx.room.compiler.processing.util

import com.squareup.kotlinpoet.MUTABLE_COLLECTION
import com.squareup.kotlinpoet.MUTABLE_ITERABLE
import com.squareup.kotlinpoet.MUTABLE_LIST
import com.squareup.kotlinpoet.MUTABLE_MAP
import com.squareup.kotlinpoet.MUTABLE_MAP_ENTRY
import com.squareup.kotlinpoet.MUTABLE_SET
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.asTypeName
import com.squareup.kotlinpoet.javapoet.JClassName
import com.squareup.kotlinpoet.javapoet.JParameterizedTypeName
import com.squareup.kotlinpoet.javapoet.JTypeName
import com.squareup.kotlinpoet.javapoet.JTypeVariableName
import com.squareup.kotlinpoet.javapoet.KClassName
import com.squareup.kotlinpoet.javapoet.KParameterizedTypeName
import com.squareup.kotlinpoet.javapoet.KTypeName
import com.squareup.kotlinpoet.javapoet.KTypeVariableName
import kotlin.reflect.KClass

val UNIT_JCLASS_NAME = JClassName.get("kotlin", "Unit")
val CONTINUATION_JCLASS_NAME = JClassName.get("kotlin.coroutines", "Continuation")

// TODO(b/247242378): Migrate usages to asJTypeName() and asJClassName()
// @Deprecated(
//     message = "Use asJTypeName() to be clear it's a JavaPoet converter",
//     replaceWith = ReplaceWith("asJTypeName()")
// )
fun KClass<*>.typeName() = JTypeName.get(this.java)

// TODO(b/247242378): Migrate usages to asJTypeName() and asJClassName()
// @Deprecated(
//     message = "Use asJClassName() to be clear it's a JavaPoet converter",
//     replaceWith = ReplaceWith("asJClassName()")
// )
fun KClass<*>.className() = JClassName.get(this.java)

fun KClass<*>.asJTypeName() = JTypeName.get(this.java)
fun KClass<*>.asJClassName() = JClassName.get(this.java)

fun KClass<*>.asKTypeName() = this.asTypeName()
fun KClass<*>.asKClassName() = this.asClassName()

/**
 * Workaround for:
 * https://github.com/square/kotlinpoet/issues/279
 * https://youtrack.jetbrains.com/issue/KT-11754
 */
fun KClass<*>.asMutableKClassName(): KClassName = when (this) {
    Iterator::class -> MUTABLE_ITERABLE
    Collection::class -> MUTABLE_COLLECTION
    List::class -> MUTABLE_LIST
    Set::class -> MUTABLE_SET
    Map::class -> MUTABLE_MAP
    Map.Entry::class -> MUTABLE_MAP_ENTRY
    else -> this.asKClassName()
}

/**
 * Dumps the typename with its bounds in a given depth, making tests more readable.
 */
fun JTypeName.dumpToString(depth: Int): String {
    return dump(this, depth).toString()
}

/**
 * Dumps the typename with its bounds in a given depth, making tests more readable.
 */
fun KTypeName.dumpToString(depth: Int): String {
    return dump(this, depth).toString()
}

private fun dump(typeName: Any, depth: Int): TypeNameNode? {
    if (depth < 0) return null
    return when (typeName) {
        is JParameterizedTypeName -> TypeNameNode(
            text = typeName.toString(),
            typeArgs = typeName.typeArguments.mapNotNull { dump(it, depth - 1) }
        )
        is KParameterizedTypeName -> TypeNameNode(
            text = typeName.toString(),
            typeArgs = typeName.typeArguments.mapNotNull { dump(it, depth - 1) }
        )
        is JTypeVariableName -> TypeNameNode(
            text = typeName.toString(),
            bounds = typeName.bounds.mapNotNull { dump(it, depth - 1) }
        )
        is KTypeVariableName -> TypeNameNode(
            text = typeName.toString(),
            bounds = typeName.bounds.mapNotNull { dump(it, depth - 1) }
        )
        else -> TypeNameNode(text = typeName.toString())
    }
}

private data class TypeNameNode(
    val text: String,
    val bounds: List<TypeNameNode> = emptyList(),
    val typeArgs: List<TypeNameNode> = emptyList()
) {
    override fun toString(): String {
        return buildString {
            appendLine(text)
            bounds.forEach {
                appendLine(it.toString().prependIndent("> "))
            }
            typeArgs.forEach {
                appendLine(it.toString().prependIndent("| "))
            }
        }.trim()
    }
}
