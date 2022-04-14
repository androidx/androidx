/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.room.compiler.processing.javac.kotlin

/**
 * Helper methods to match ABI rules for Kotlin java bytecode generation.
 *
 * https://github.com/JetBrains/kotlin/blob/master/core/compiler.common.jvm/src/org/jetbrains/kotlin/load/java/JvmAbi.kt#L89
 * and
 * https://github.com/JetBrains/kotlin/blob/master/core/util.runtime/src/org/jetbrains/kotlin/utils/capitalizeDecapitalize.kt
 *
 * See: https://kotlinlang.org/docs/java-to-kotlin-interop.html#properties
 */
internal object JvmAbi {
    private const val GET_PREFIX = "get"
    private const val IS_PREFIX = "is"
    private const val SET_PREFIX = "set"

    fun computeGetterName(propertyName: String): String {
        return if (propertyName.startsWithIsPrefix()) {
            propertyName
        } else {
            GET_PREFIX + propertyName.capitalizeAsciiOnly()
        }
    }

    fun computeSetterName(propertyName: String): String {
        return SET_PREFIX +
            if (propertyName.startsWithIsPrefix()) {
                propertyName.substring(IS_PREFIX.length)
            } else {
                propertyName.capitalizeAsciiOnly()
            }
    }

    private fun String.startsWithIsPrefix(): Boolean {
        if (!startsWith(IS_PREFIX)) return false
        if (length == IS_PREFIX.length) return false
        val c = this[IS_PREFIX.length]
        return !('a' <= c && c <= 'z')
    }

    private fun String.capitalizeAsciiOnly(): String {
        if (isEmpty()) return this
        val c = this[0]
        return if (c in 'a'..'z') {
            c.uppercaseChar() + substring(1)
        } else {
            this
        }
    }
}