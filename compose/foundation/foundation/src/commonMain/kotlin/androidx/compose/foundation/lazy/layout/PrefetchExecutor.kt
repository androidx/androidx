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
internal expect fun rememberDefaultPrefetchExecutor(): PrefetchExecutor

/**
 * Implementations of this interface accept prefetch requests via [requestPrefetch] and decide when
 * to execute them in a way that will have minimal impact on user experience, e.g. during frame idle
 * time. Executing a request involves invoking [Request.performComposition] and
 * [Request.performMeasure].
 */
@ExperimentalFoundationApi
interface PrefetchExecutor {

    /**
     * Accepts a prefetch request. Implementations should find a time to execute them which will
     * have minimal impact on user experience.
     */
    fun requestPrefetch(request: Request)

    sealed interface Request {

        /**
         * Whether this is still a valid request (wasn't canceled, within list bounds). If it's
         * not valid, it should be dropped and not executed.
         */
        val isValid: Boolean

        /**
         * Whether this request has been composed via [performComposition].
         */
        val isComposed: Boolean

        /**
         * Composes the content belonging to this request.
         */
        fun performComposition()

        /**
         * Measures the Composition belonging to this request. Must be called after
         * [performComposition].
         */
        fun performMeasure()
    }
}
