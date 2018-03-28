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

package com.android.tools.build.jetifier.core.type

/**
 * Wrapper for Java package name declaration.
 */
data class PackageName(val fullName: String) {

    init {
        if (fullName.contains('.')) {
            throw IllegalArgumentException("The type does not support '.' as a package separator!")
        }
    }

    companion object {
        /** Creates the package from notation where packages are separated using '.' */
        fun fromDotVersion(fullName: String): PackageName {
            return PackageName(fullName.replace('.', '/'))
        }
    }

    /** Returns the package as a string where packages are separated using '.' */
    fun toDotNotation(): String {
        return fullName.replace('/', '.')
    }

    override fun toString() = fullName
}