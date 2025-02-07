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

@file:JvmName("Flows")

package androidx.xr.runtime.java

import androidx.xr.runtime.Session
import io.reactivex.rxjava3.core.Observable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.rx3.asObservable

/**
 * Converts a [flow] created within the [session] to an [Observable].
 *
 * The returned [Observable] will be given the session's [CoroutineContext].
 *
 * @param session the [Session] that originated the [flow].
 * @param flow the [Flow] to convert to an [Observable].
 */
public fun <T : Any> toObservable(session: Session, flow: Flow<T>): Observable<T> =
    flow.asObservable(session.coroutineScope.coroutineContext)
