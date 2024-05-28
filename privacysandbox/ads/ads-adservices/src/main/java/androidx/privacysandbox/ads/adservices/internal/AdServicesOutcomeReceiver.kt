/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.privacysandbox.ads.adservices.internal

import android.adservices.common.AdServicesOutcomeReceiver
import android.annotation.SuppressLint
import android.os.Build
import androidx.annotation.RequiresExtension
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/*
 This file is a modified version OutcomeReceiver.kt in androidx.core.os, designed to provide the same
 functionality with the AdServicesOutcomeReceiver, to keep the implementation of the backward compatible
 classes as close to identical as possible.
*/

@RequiresExtension(extension = Build.VERSION_CODES.R, version = 11)
fun <R, E : Throwable> Continuation<R>.asAdServicesOutcomeReceiver():
    AdServicesOutcomeReceiver<R, E> = ContinuationOutcomeReceiver(this)

@SuppressLint("NewApi")
@RequiresExtension(extension = Build.VERSION_CODES.R, version = 11)
private class ContinuationOutcomeReceiver<R, E : Throwable>(
    private val continuation: Continuation<R>
) : AdServicesOutcomeReceiver<R, E>, AtomicBoolean(false) {
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
