/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.compose.ui.platform

import androidx.compose.ui.InternalComposeUiApi

/**
 * Implementations of this interface accept prefetch requests and decide when to execute them in a
 * way that will have minimal impact on user experience, e.g. during frame idle time.
 *
 * Requests should be executed by invoking [PlatformPrefetchRequest.execute]. The implementation of
 * [PlatformPrefetchRequest.execute] will return `false` when all work for that request is done,
 * or `true` when it still has more to do but doesn't think it can complete it within
 * [PlatformPrefetchRequestScope.availableTimeNanos].
 */
@InternalComposeUiApi
interface PlatformPrefetchScheduler {
    /**
     * Accepts a high-priority prefetch request. Implementations should find time to execute it
     * before lower-priority work, with minimal impact on user experience.
     */
    fun scheduleHighPriorityPrefetch(request: PlatformPrefetchRequest)

    /**
     * Accepts a low-priority prefetch request. Implementations should find time to execute it with
     * minimal impact on user experience.
     */
    fun scheduleLowPriorityPrefetch(request: PlatformPrefetchRequest)
}

/**
 * A request for prefetch which can be submitted to a [PlatformPrefetchScheduler] to execute during
 * idle time.
 */
@InternalComposeUiApi
interface PlatformPrefetchRequest {
    /**
     * Gives this request a chance to execute work. It should only do work if it thinks it can
     * finish it within [PlatformPrefetchRequestScope.availableTimeNanos].
     *
     * @return whether this request has more work it wants to do, but ran out of time. `true`
     *   indicates this request wants to have [execute] called again to do more work, while `false`
     *   indicates its work is complete.
     */
    fun PlatformPrefetchRequestScope.execute(): Boolean
}

/**
 * Scope for [PlatformPrefetchRequest.execute], supplying info about how much time it has to execute
 * requests and the type of execution mode.
 */
@InternalComposeUiApi
interface PlatformPrefetchRequestScope {
    /**
     * How much time is available to do prefetch work. Implementations of [PlatformPrefetchRequest] should
     * do their best to fit their work into this time without going over.
     */
    fun availableTimeNanos(): Long
}
