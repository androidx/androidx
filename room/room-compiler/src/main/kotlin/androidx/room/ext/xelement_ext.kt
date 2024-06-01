/*
 * Copyright (C) 2020 The Android Open Source Project
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

package androidx.room.ext

import androidx.room.compiler.processing.XElement
import androidx.room.compiler.processing.XExecutableParameterElement
import androidx.room.compiler.processing.XTypeElement
import kotlin.contracts.contract

fun XElement.isEntityElement(): Boolean {
    contract { returns(true) implies (this@isEntityElement is XTypeElement) }
    return this.hasAnnotation(androidx.room.Entity::class)
}

fun XTypeElement.getValueClassUnderlyingElement(): XExecutableParameterElement {
    check(this.isValueClass()) {
        "Can't get value class property, type element '$this' is not a value class"
    }
    // Kotlin states:
    // * Primary constructor is required for value class
    // * Value class must have exactly one primary constructor parameter
    // * Value class primary constructor must only have final read-only (val) property parameter
    return checkNotNull(this.findPrimaryConstructor()) {
            "Couldn't find primary constructor for value class."
        }
        .parameters
        .single()
}

/** Suffix of the Kotlin synthetic class created interface method implementations. */
const val DEFAULT_IMPLS_CLASS_NAME = "DefaultImpls"
