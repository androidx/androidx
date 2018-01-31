/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License
 */

package android.support.tools.jetifier.core.transform.proguard

import android.support.tools.jetifier.core.rules.JavaType

/**
 * Represents a type reference in ProGuard file. This type is similar to the regular java type but
 * can also contain wildcards (*,**,?).
 */
data class ProGuardType(val value: String) {

    init {
        if (value.contains('.')) {
            throw IllegalArgumentException("The type does not support '.' as package separator!")
        }
    }

    companion object {
        /** Creates the type reference from notation where packages are separated using '.' */
        fun fromDotNotation(type: String) : ProGuardType {
            return ProGuardType(type.replace('.', '/'))
        }
    }

    /**
     * Whether the type reference is trivial such as "*".
     */
    fun isTrivial() = value == "*" || value == "**" || value == "***" || value == "%"

    fun toJavaType() : JavaType? {
        if (value.contains('*') || value.contains('?')) {
            return null
        }
        return JavaType(value)
    }

    /** Returns the type reference as a string where packages are separated using '.' */
    fun toDotNotation() : String {
        return value.replace('/', '.')
    }
}