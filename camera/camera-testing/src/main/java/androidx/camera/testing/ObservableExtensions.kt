/*
 * Copyright 2021 The Android Open Source Project
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

@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@file:RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java

package androidx.camera.testing

import androidx.annotation.RequiresApi
import androidx.camera.core.impl.Observable
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlin.coroutines.ContinuationInterceptor

public fun <T> Observable<T>.asFlow(): Flow<T?> = callbackFlow {
    val observer = object : Observable.Observer<T> {
        override fun onNewData(value: T?) {
            launch(start = CoroutineStart.UNDISPATCHED) {
                send(value)
            }
        }

        override fun onError(t: Throwable) {
            // Close the channel with the error
            close(t)
        }
    }

    val producerDispatcher = coroutineContext[ContinuationInterceptor] as CoroutineDispatcher
    addObserver(producerDispatcher.asExecutor(), observer)

    awaitClose { removeObserver(observer) }
}