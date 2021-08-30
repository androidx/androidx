/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.benchmark.macro

public enum class StartupMode {
    /**
     * Startup from scratch - app's process is not alive, and must be started in addition to
     * Activity creation.
     *
     * See
     * [Cold startup documentation](https://developer.android.com/topic/performance/vitals/launch-time#cold)
     */
    COLD,

    /**
     * Create and display a new Activity in a currently running app process.
     *
     * See
     * [Warm startup documentation](https://developer.android.com/topic/performance/vitals/launch-time#warm)
     */
    WARM,

    /**
     * Bring existing activity to the foreground, process and Activity still exist from previous
     * launch.
     *
     * See
     * [Hot startup documentation](https://developer.android.com/topic/performance/vitals/launch-time#hot)
     */
    HOT
}