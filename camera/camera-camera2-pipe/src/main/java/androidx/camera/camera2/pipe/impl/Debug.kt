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

@file:Suppress("NOTHING_TO_INLINE")

package androidx.camera.camera2.pipe.impl

/**
 * Internal debug utilities, constants, and checks.
 */
object Debug {
    const val ENABLE_LOGGING = true

    /**
     * Asserts that the provided value *is* null.
     */
    inline fun checkNull(value: Any?) {
        if (value != null) {
            throw IllegalArgumentException("Expected null, but got $value!")
        }
    }

    /**
     * Asserts that the provided value *is* null with an optional message.
     *
     * Syntax: checkNull(nullableValue) { "nullableValue should be null, but is $nullableValue }
     */
    inline fun checkNull(value: Any?, crossinline msg: () -> String) {
        if (value != null) {
            throw IllegalArgumentException(msg())
        }
    }
}
