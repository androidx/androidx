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

package androidx.ui.test.util

import com.google.common.truth.Truth.assertWithMessage
import java.io.PrintWriter
import java.io.StringWriter

/**
 * Runs the [block] and asserts that an [AssertionError] is thrown if [expectError] is `true`, or
 * that it is not thrown if [expectError] is `false`.
 */
fun expectAssertionError(expectError: Boolean, block: () -> Unit) {
    expectError<AssertionError>(expectError, block)
}

/**
 * Runs the [block] and asserts that a [T] is thrown if [expectError] is `true`, or that it is
 * not thrown if [expectError] is `false`.
 */
inline fun <reified T : Throwable> expectError(expectError: Boolean = true, block: () -> Unit) {
    var thrown = false
    val errorClassName = T::class.java.simpleName
    var errorMessage = "Expected a $errorClassName, got nothing"
    try {
        block()
    } catch (t: Throwable) {
        if (t !is T) {
            throw t
        }
        thrown = true
        StringWriter().use { sw ->
            PrintWriter(sw).use { pw ->
                t.printStackTrace(pw)
            }
            errorMessage = "Expected no $errorClassName, got:\n==============\n$sw=============="
        }
    }
    assertWithMessage(errorMessage).that(thrown).isEqualTo(expectError)
}
