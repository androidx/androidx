/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.wear.watchface.client

import android.app.NotificationManager
import androidx.annotation.IntDef

/**
 * The InterruptionFilter.
 * @hide
 */
@IntDef(
    value = [
        NotificationManager.INTERRUPTION_FILTER_ALARMS,
        NotificationManager.INTERRUPTION_FILTER_ALL,
        NotificationManager.INTERRUPTION_FILTER_NONE,
        NotificationManager.INTERRUPTION_FILTER_PRIORITY,
        NotificationManager.INTERRUPTION_FILTER_UNKNOWN
    ]
)
public annotation class InterruptionFilter

/** Describes the system state of the watch face. */
public class SystemState(
    /** Whether the device is is ambient mode or not. */
    @get:JvmName("inAmbientMode")
    public val inAmbientMode: Boolean,

    /**
     * The current user interruption settings.
     *
     * @see [NotificationManager] for details.
     */
    @InterruptionFilter
    public val interruptionFilter: Int
)