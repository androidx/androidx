/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.compose.ui.node

/**
 * Executes the scheduled work on the main thread after the frame completion. It allows to execute
 * some work which is not blocking the current frame drawing. The execution should have priority
 * over the work scheduled by PrefetchScheduler from the lazy layouts, and the execution should
 * happen within the same frame even if our deadline calculation says we are out of time already, as
 * the current contract is that this work is executed before the next frame starts.
 */
internal interface OutOfFrameExecutor {
    /** Schedules the [block] execution out of frame. */
    fun schedule(block: () -> Unit)
}
