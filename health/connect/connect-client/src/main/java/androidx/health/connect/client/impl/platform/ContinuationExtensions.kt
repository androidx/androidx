/*
 * Copyright 2023 The Android Open Source Project
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

// TODO(b/269468056): Remove this file and use androidx.core.os implementation

@file:RestrictTo(RestrictTo.Scope.LIBRARY)
@file:RequiresApi(api = 34)

package androidx.health.connect.client.impl.platform

import android.os.OutcomeReceiver
import androidx.annotation.RequiresApi
import androidx.annotation.RestrictTo
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

internal fun <R, E : Throwable> Continuation<R>.asOutcomeReceiver(): OutcomeReceiver<R, E> =
    ContinuationOutcomeReceiver(this)

private class ContinuationOutcomeReceiver<R, E : Throwable>(
    private val continuation: Continuation<R>
) : OutcomeReceiver<R, E>, AtomicBoolean(false) {
    @Suppress("WRONG_NULLABILITY_FOR_JAVA_OVERRIDE")
    override fun onResult(result: R) {
        // Do not attempt to resume more than once, even if the caller of the returned
        // OutcomeReceiver is buggy and tries anyway.
        if (compareAndSet(false, true)) {
            continuation.resume(result)
        }
    }

    override fun onError(error: E) {
        // Do not attempt to resume more than once, even if the caller of the returned
        // OutcomeReceiver is buggy and tries anyway.
        if (compareAndSet(false, true)) {
            continuation.resumeWithException(error)
        }
    }

    override fun toString() = "ContinuationOutcomeReceiver(outcomeReceived = ${get()})"
}
