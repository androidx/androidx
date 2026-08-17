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

import androidx.compose.runtime.Stable
import kotlinx.coroutines.CoroutineDispatcher

/**
 * Interface providing access to [CoroutineDispatcher]s used for task execution inside a Compose
 * hierarchy.
 *
 * @sample androidx.compose.ui.samples.TaskDispatchersSample
 */
@Stable
public interface TaskDispatchers {
    /**
     * A [CoroutineDispatcher] optimized for performing CPU-intensive work outside of the UI thread.
     */
    public val Default: CoroutineDispatcher

    /**
     * A [CoroutineDispatcher] designed for offloading blocking I/O tasks to a shared pool of
     * threads.
     */
    @get:Suppress("AcronymName") public val IO: CoroutineDispatcher
}
