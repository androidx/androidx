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

package androidx.compose.remote.creation.compose.text

import android.text.format.DateFormat
import androidx.compose.remote.core.RemoteContext.FLOAT_TIME_IN_HR
import androidx.compose.remote.core.RemoteContext.FLOAT_TIME_IN_MIN
import androidx.compose.remote.core.operations.TextFromFloat
import androidx.compose.remote.creation.compose.state.RemoteBoolean
import androidx.compose.remote.creation.compose.state.RemoteFloat
import androidx.compose.remote.creation.compose.state.RemoteString
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.remote.creation.compose.state.rs
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

/**
 * Contains default values and helper methods for displaying time-related text in a remote context.
 */
public object RemoteTimeDefaults {

    /**
     * Returns a [RemoteBoolean] indicating whether the time should be displayed in 24-hour format.
     * Currently captured at recording time.
     */
    @Composable
    public fun is24HourFormat(): RemoteBoolean =
        RemoteBoolean(DateFormat.is24HourFormat(LocalContext.current))

    /**
     * Creates a [RemoteString] representing the current time in either 12-hour or 24-hour format.
     *
     * The 24-hour format is represented as "HH:mm", while the 12-hour format is represented as
     * "hh:mm AM/PM".
     *
     * @param is24HourFormat A [RemoteBoolean] indicating whether to use 24-hour format. Defaults to
     *   the system's current setting at the time of recording.
     * @return A [RemoteString] that evaluates to the formatted time string.
     */
    @Composable
    public fun defaultTimeString(is24HourFormat: RemoteBoolean = is24HourFormat()): RemoteString {
        val mins =
            (RemoteFloat(FLOAT_TIME_IN_MIN) % 60f).toRemoteString(2, 0, TextFromFloat.PAD_PRE_ZERO)
        val hours24String: RemoteString =
            RemoteFloat(FLOAT_TIME_IN_HR).toRemoteString(2, 0, TextFromFloat.PAD_PRE_ZERO)
        val currentHour = RemoteFloat(FLOAT_TIME_IN_HR)
        val hour12: RemoteFloat =
            ((currentHour % 12f).eq(0.rf)).select(RemoteFloat(12f), currentHour % 12f)
        val hours12String: RemoteString = hour12.toRemoteString(2, 0, TextFromFloat.PAD_PRE_ZERO)
        val amPm: RemoteString = (currentHour.lt(12.rf)).select(" AM".rs, " PM".rs)

        val time24 = hours24String + ":" + mins
        val time12 = hours12String + ":" + mins + amPm
        return is24HourFormat.select(time24, time12)
    }
}
