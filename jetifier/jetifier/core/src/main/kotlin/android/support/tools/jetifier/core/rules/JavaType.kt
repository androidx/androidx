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

package android.support.tools.jetifier.core.rules

/**
 * Wrapper for Java type declaration.
 */
data class JavaType(val fullName: String) {

    init {
        if (fullName.contains('.')) {
            throw IllegalArgumentException("The type does not support '.' as package separator!")
        }
    }

    companion object {
        /** Creates the type from notation where packages are separated using '.' */
        fun fromDotVersion(fullName: String) : JavaType {
            return JavaType(fullName.replace('.', '/'))
        }
    }

    /** Returns the type as a string where packages are separated using '.' */
    fun toDotNotation() : String {
        return fullName.replace('/', '.')
    }


    override fun toString() = fullName

}