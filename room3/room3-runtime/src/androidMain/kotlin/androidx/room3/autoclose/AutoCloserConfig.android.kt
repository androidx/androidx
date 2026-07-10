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

package androidx.room3.autoclose

import java.util.concurrent.TimeUnit

/**
 * The auto-close configuration used to automatically close the database if not used after a certain
 * amount time.
 */
internal class AutoCloserConfig(val timeout: Long, val timeUnit: TimeUnit) {
    internal val timeoutInMs = timeUnit.toMillis(timeout)
}
