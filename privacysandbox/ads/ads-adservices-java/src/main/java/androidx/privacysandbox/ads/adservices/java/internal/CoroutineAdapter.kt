/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.privacysandbox.ads.adservices.java.internal

import androidx.concurrent.futures.CallbackToFutureAdapter
import com.google.common.util.concurrent.ListenableFuture
import java.util.concurrent.CancellationException
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.ExperimentalCoroutinesApi

@SuppressWarnings("AsyncSuffixFuture")
@OptIn(ExperimentalCoroutinesApi::class)
internal fun <T> Deferred<T>.asListenableFuture(
    tag: Any? = "Deferred.asListenableFuture"
): ListenableFuture<T> =
    CallbackToFutureAdapter.getFuture { completer ->
        this.invokeOnCompletion {
            if (it != null) {
                if (it is CancellationException) {
                    completer.setCancelled()
                } else {
                    completer.setException(it)
                }
            } else {
                // Ignore exceptions - This should never throw in this situation.
                completer.set(this.getCompleted())
            }
        }
        tag
    }
