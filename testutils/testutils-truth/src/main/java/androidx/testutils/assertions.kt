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

package androidx.testutils

import com.google.common.truth.ExpectFailure
import com.google.common.truth.ThrowableSubject
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.TruthFailureSubject

inline fun <reified T : Throwable> assertThrows(body: () -> Unit): ThrowableSubject {
    try {
        body()
    } catch (e: Throwable) {
        if (e is T) {
            return assertThat(e)
        }
        throw e
    }
    throw AssertionError("Body completed successfully. Expected ${T::class.java.simpleName}.")
}

inline fun assertThrows(body: () -> Unit): TruthFailureSubject {
    try {
        body()
    } catch (e: Throwable) {
        if (e is AssertionError) {
            return ExpectFailure.assertThat(e)
        }
        throw e
    }
    throw AssertionError("Body completed successfully. Expected AssertionError")
}

fun fail(message: String? = null): Nothing = throw AssertionError(message)

// The assertThrows above cannot be used from Java.
@Suppress("UNCHECKED_CAST")
fun <T : Throwable?> assertThrows(
    expectedType: Class<T>,
    runnable: Runnable
): ThrowableSubject {
    try {
        runnable.run()
    } catch (t: Throwable) {
        if (expectedType.isInstance(t)) {
            return assertThat(t)
        }
        throw t
    }
    throw AssertionError(
        "Body completed successfully. Expected ${expectedType.simpleName}"
    )
}
