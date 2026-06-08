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
import androidx.compose.ui.node.OutOfFrameExecutor

/**
 * Platform-specific scheduler for work that should be deferred out of the current
 * composition/layout/rendering stack.
 *
 * @see OutOfFrameExecutor
 */
@InternalComposeUiApi
interface PlatformOutOfFrameExecutor {
    /**
     * `true` when there is scheduled work that has not been executed yet.
     */
    val hasWorkScheduled: Boolean
        get() = false

    /**
     * Schedules [block] to run out of the current call stack.
     */
    fun schedule(block: () -> Unit)

    /**
     * Runs pending work scheduled by [schedule] immediately for tests.
     */
    fun drainScheduledWorkForTest()
}
