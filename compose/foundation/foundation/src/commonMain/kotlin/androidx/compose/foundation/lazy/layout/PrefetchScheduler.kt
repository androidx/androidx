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

package androidx.compose.foundation.lazy.layout

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.runtime.Composable

/**
 * Remembers the platform-specific implementation for scheduling lazy layout item prefetch
 * (pre-composing next items in advance during the scrolling).
 */
@ExperimentalFoundationApi
@Composable
internal expect fun rememberDefaultPrefetchScheduler(): PrefetchScheduler

/**
 * Implementations of this interface accept prefetch requests via [schedulePrefetch] and decide when
 * to execute them in a way that will have minimal impact on user experience, e.g. during frame idle
 * time.
 *
 * Requests should be executed by invoking [PrefetchRequest.execute]. The implementation of
 * [PrefetchRequest.execute] will return `false` when all work for that request is done, or `true`
 * when it still has more to do but doesn't think it can complete it within
 * [PrefetchRequestScope.availableTimeNanos].
 */
@ExperimentalFoundationApi
interface PrefetchScheduler {

    /**
     * Accepts a prefetch request. Implementations should find a time to execute them which will
     * have minimal impact on user experience.
     */
    fun schedulePrefetch(prefetchRequest: PrefetchRequest)
}

/**
 * A request for prefetch which can be submitted to a [PrefetchScheduler] to execute during idle
 * time.
 */
@ExperimentalFoundationApi
sealed interface PrefetchRequest {

    /**
     * Gives this request a chance to execute work. It should only do work if it thinks it can
     * finish it within [PrefetchRequestScope.availableTimeNanos].
     *
     * @return whether this request has more work it wants to do, but ran out of time. `true`
     *   indicates this request wants to have [execute] called again to do more work, while `false`
     *   indicates its work is complete.
     */
    fun PrefetchRequestScope.execute(): Boolean
}

/**
 * Scope for [PrefetchRequest.execute], supplying info about how much time it has to execute
 * requests.
 */
@ExperimentalFoundationApi
interface PrefetchRequestScope {

    /**
     * How much time is available to do prefetch work. Implementations of [PrefetchRequest] should
     * do their best to fit their work into this time without going over.
     */
    fun availableTimeNanos(): Long
}
