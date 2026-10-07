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

package androidx.compose.remote.creation.compose.state

import androidx.annotation.RestrictTo
import androidx.compose.remote.core.RemoteContext

/**
 * Provides access to remote time and animation variables evaluated by the host player.
 *
 * Continuous animation and sub-second clock values are represented as [RemoteFloat], while discrete
 * wall-clock and calendar fields are represented as [RemoteInt].
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public object RemoteTimeVariables {
    /** The elapsed animation time in seconds since document load as a [RemoteFloat]. */
    public val animationTime: RemoteFloat
        get() = RemoteFloat(RemoteContext.FLOAT_ANIMATION_TIME)

    /** The delta time in seconds since the previous animation frame as a [RemoteFloat]. */
    public val animationDeltaTime: RemoteFloat
        get() = RemoteFloat(RemoteContext.FLOAT_ANIMATION_DELTA_TIME)

    /** The continuous seconds `[0.0, 3600.0)` in the current hour as a [RemoteFloat]. */
    public val continuousSeconds: RemoteFloat
        get() = RemoteFloat(RemoteContext.FLOAT_CONTINUOUS_SEC)

    /** The second of the current hour `[0, 3599]` as a [RemoteInt]. */
    public val secondOfHour: RemoteInt
        get() = RemoteInt.createForContextId(RemoteContext.ID_TIME_IN_SEC)

    /** The minute of the current day `[0, 1439]` as a [RemoteInt]. */
    public val minuteOfDay: RemoteInt
        get() = RemoteInt.createForContextId(RemoteContext.ID_TIME_IN_MIN)

    /** The hour of the current day `[0, 23]` as a [RemoteInt]. */
    public val hourOfDay: RemoteInt
        get() = RemoteInt.createForContextId(RemoteContext.ID_TIME_IN_HR)

    /** The month of the year `[1, 12]` (January to December) as a [RemoteInt]. */
    public val month: RemoteInt
        get() = RemoteInt.createForContextId(RemoteContext.ID_CALENDAR_MONTH)

    /** The day of the month `[1, 31]` as a [RemoteInt]. */
    public val dayOfMonth: RemoteInt
        get() = RemoteInt.createForContextId(RemoteContext.ID_DAY_OF_MONTH)

    /** The day of the week `[1, 7]` (Monday to Sunday) as a [RemoteInt]. */
    public val dayOfWeek: RemoteInt
        get() = RemoteInt.createForContextId(RemoteContext.ID_WEEK_DAY)

    /** The day of the year `[1, 366]` as a [RemoteInt]. */
    public val dayOfYear: RemoteInt
        get() = RemoteInt.createForContextId(RemoteContext.ID_DAY_OF_YEAR)

    /** The calendar year (e.g., `2026`) as a [RemoteInt]. */
    public val year: RemoteInt
        get() = RemoteInt.createForContextId(RemoteContext.ID_YEAR)

    /** The local time zone offset from UTC in seconds as a [RemoteInt]. */
    public val utcOffsetSeconds: RemoteInt
        get() = RemoteInt.createForContextId(RemoteContext.ID_OFFSET_TO_UTC)

    /** The elapsed seconds since the Unix epoch as a [RemoteInt]. */
    public val epochSecond: RemoteInt
        get() = RemoteInt.createForContextId(RemoteContext.ID_EPOCH_SECOND)
}
