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

/**
 * Describes the system state of the watch face ui.
 *
 * @param inAmbientMode Whether the device is is ambient mode or not.
 * @param interruptionFilter The interruption filter defines which notifications are allowed to
 * interrupt the user. For watch faces this value is one of:
 * [NotificationManager.INTERRUPTION_FILTER_ALARMS],
 * [NotificationManager.INTERRUPTION_FILTER_ALL],
 * [NotificationManager.INTERRUPTION_FILTER_NONE],
 * [NotificationManager.INTERRUPTION_FILTER_PRIORITY],
 * [NotificationManager.INTERRUPTION_FILTER_UNKNOWN]. @see [NotificationManager] for more details.
 */
public class WatchUiState(
    @get:JvmName("inAmbientMode")
    public val inAmbientMode: Boolean,

    @InterruptionFilter
    public val interruptionFilter: Int
)