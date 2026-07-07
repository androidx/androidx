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

package androidx.compose.remote.creation.compose.layout

import androidx.annotation.RestrictTo
import androidx.compose.remote.core.RemoteContext.FLOAT_CONTINUOUS_SEC
import androidx.compose.remote.core.RemoteContext.FLOAT_DAY_OF_MONTH
import androidx.compose.remote.core.RemoteContext.FLOAT_OFFSET_TO_UTC
import androidx.compose.remote.core.RemoteContext.FLOAT_TIME_IN_HR
import androidx.compose.remote.core.RemoteContext.FLOAT_TIME_IN_MIN
import androidx.compose.remote.core.RemoteContext.FLOAT_TIME_IN_SEC
import androidx.compose.remote.core.RemoteContext.FLOAT_WEEK_DAY
import androidx.compose.remote.creation.compose.state.RemoteFloat

/** A class that provides access to remote time information. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class RemoteTime {
    /** Returns the current hour as a [RemoteFloat]. */
    public fun Hour(): RemoteFloat = RemoteFloat(FLOAT_TIME_IN_HR)

    /** Returns the current minute as a [RemoteFloat]. */
    public fun Minutes(): RemoteFloat = RemoteFloat(FLOAT_TIME_IN_MIN)

    /** Returns the current second as a [RemoteFloat]. */
    public fun Seconds(): RemoteFloat = RemoteFloat(FLOAT_TIME_IN_SEC)

    /** Returns the continuous elapsed time in seconds as a [RemoteFloat]. */
    public fun ContinuousSec(): RemoteFloat = RemoteFloat(FLOAT_CONTINUOUS_SEC)

    /** Returns the offset to UTC time in seconds as a [RemoteFloat]. */
    public fun UtcOffset(): RemoteFloat = RemoteFloat(FLOAT_OFFSET_TO_UTC)

    /** Returns the current day of the week as a [RemoteFloat]. */
    public fun DayOfWeek(): RemoteFloat = RemoteFloat(FLOAT_WEEK_DAY)

    /** Returns the current day of the month as a [RemoteFloat]. */
    public fun DayOfMonth(): RemoteFloat = RemoteFloat(FLOAT_DAY_OF_MONTH)
}
