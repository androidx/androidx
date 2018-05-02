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

package com.android.tools.build.jetifier.core.proguard

import com.android.tools.build.jetifier.core.type.JavaType
import java.util.regex.Pattern

/**
 * Represents a type reference in ProGuard file. This type is similar to the regular java type but
 * can also contain wildcards (*,**,?).
 *
 * ProGuard can also contain token {any}. This comes from the configuration and is simply used as
 * a shortcut for multiple different wildcards (such as. "*", "**", "***", "*.*", "**.*").
 */
data class ProGuardType(val value: String) {

    companion object {
        val EXPANSION_TOKENS = listOf("*", "**", "***", "*/*", "**/*")

        val TRIVIAL_SELECTOR_MATCHER: Pattern = Pattern.compile("^[/?*]*$")

        /** Creates the type reference from notation where packages are separated using '.' */
        fun fromDotNotation(type: String): ProGuardType {
            return ProGuardType(type.replace('.', '/'))
        }
    }

    init {
        if (value.contains('.')) {
            throw IllegalArgumentException("The type does not support '.' as package separator!")
        }
    }

    /**
     * Whether the type reference is trivial such as "*".
     */
    fun isTrivial() = TRIVIAL_SELECTOR_MATCHER.matcher(value).matches()

    fun toJavaType(): JavaType? {
        if (value.contains('*') || value.contains('?')) {
            return null
        }
        return JavaType(value)
    }

    fun needsExpansion(): Boolean {
        return value.contains("{any}")
    }

    fun expandWith(token: String): ProGuardType {
        return ProGuardType(value.replace("{any}", token))
    }

    /** Returns the type reference as a string where packages are separated using '.' */
    fun toDotNotation(): String {
        return value.replace('/', '.')
    }
}