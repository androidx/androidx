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

@file:JvmName("Coroutines")

package androidx.xr.runtime.java

import androidx.xr.runtime.Session
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.guava.future

/**
 * Converts a coroutine created within the [session] to a [ListenableFuture].
 *
 * The returned [ListenableFuture] will be automatically cancelled when the [session] is destroyed.
 *
 * @param session the [Session] that originated the [coroutine].
 * @param coroutine the coroutine to convert to a [ListenableFuture].
 */
@Suppress("AsyncSuffixFuture")
public fun <T> toFuture(
    session: Session,
    coroutine: suspend CoroutineScope.() -> T,
): ListenableFuture<T> =
    session.coroutineScope.future(
        session.coroutineScope.coroutineContext,
        CoroutineStart.DEFAULT,
        coroutine,
    )
