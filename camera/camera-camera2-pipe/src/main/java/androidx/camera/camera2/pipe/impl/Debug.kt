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

import android.os.Trace
import android.os.Build

/**
 * Internal debug utilities, constants, and checks.
 */
object Debug {
    const val ENABLE_LOGGING = true
    const val ENABLE_TRACING = true

    /**
     * Wrap the specified [block] in calls to [Trace.beginSection] (with the supplied [label])
     * and [Trace.endSection].
     *
     * @param label A name of the code section to appear in the trace.
     * @param block A block of code which is being traced.
     */
    inline fun <T> trace(label: String, crossinline block: () -> T): T {
        try {
            if (ENABLE_TRACING) {
                Trace.beginSection(label)
            }
            return block()
        } finally {
            if (ENABLE_TRACING) {
                Trace.endSection()
            }
        }
    }

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

/**
 * Asserts that the method was invoked on a specific API version or higher.
 *
 * Example: checkApi(Build.VERSION_CODES.LOLLIPOP, "createCameraDevice")
 */
inline fun checkApi(requiredApi: Int, methodName: String) {
    check(Build.VERSION.SDK_INT >= requiredApi) {
        "$methodName is not supported on API ${Build.VERSION.SDK_INT} (requires API $requiredApi)"
    }
}

/** Asserts that this method was invoked on Android L (API 21) or higher. */
inline fun checkLOrHigher(methodName: String) = checkApi(
    Build.VERSION_CODES.LOLLIPOP, methodName
)

/** Asserts that this method was invoked on Android M (API 23) or higher. */
inline fun checkMOrHigher(methodName: String) = checkApi(
    Build.VERSION_CODES.M, methodName
)

/** Asserts that this method was invoked on Android N (API 24) or higher. */
inline fun checkNOrHigher(methodName: String) = checkApi(
    Build.VERSION_CODES.N, methodName
)

/** Asserts that this method was invoked on Android O (API 26) or higher. */
inline fun checkOOrHigher(methodName: String) = checkApi(
    Build.VERSION_CODES.O, methodName
)

/** Asserts that this method was invoked on Android P (API 28) or higher. */
inline fun checkPOrHigher(methodName: String) = checkApi(
    Build.VERSION_CODES.P, methodName
)

/** Asserts that this method was invoked on Android Q (API 29) or higher. */
inline fun checkQOrHigher(methodName: String) = checkApi(
    Build.VERSION_CODES.Q, methodName
)
