/*
 * Copyright (C) 2025 The Android Open Source Project
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

package androidx.ink.authoring.latency.aggregators.internal

import kotlin.time.Duration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Runs [function] at `now + interval`, `now + 2*interval`, ... indefinitely. Returns immediately.
 * The returned [Job] may be canceled to stop the loop.
 */
internal fun CoroutineScope.runEvery(interval: Duration, function: suspend () -> Unit): Job {
    return launch {
        while (true) {
            delay(interval)
            // Run the function in a separate coroutine to avoid throwing off the loop timing.
            launch { function() }
        }
    }
}
