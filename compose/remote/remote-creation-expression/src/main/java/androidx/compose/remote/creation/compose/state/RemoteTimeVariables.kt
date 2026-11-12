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

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public object RemoteTimeVariables {
    public val animationTime: RemoteFloat
        get() = RemoteFloat(RemoteContext.FLOAT_ANIMATION_TIME)

    public val animationDeltaTime: RemoteFloat
        get() = RemoteFloat(RemoteContext.FLOAT_ANIMATION_DELTA_TIME)

    public val continuousSeconds: RemoteFloat
        get() = RemoteFloat(RemoteContext.FLOAT_CONTINUOUS_SEC)

    public val secondOfHour: RemoteInt
        get() = RemoteInt.createForContextId(RemoteContext.ID_TIME_IN_SEC)

    public val minuteOfDay: RemoteInt
        get() = RemoteInt.createForContextId(RemoteContext.ID_TIME_IN_MIN)

    public val hourOfDay: RemoteInt
        get() = RemoteInt.createForContextId(RemoteContext.ID_TIME_IN_HR)

    public val month: RemoteInt
        get() = RemoteInt.createForContextId(RemoteContext.ID_CALENDAR_MONTH)

    public val dayOfMonth: RemoteInt
        get() = RemoteInt.createForContextId(RemoteContext.ID_DAY_OF_MONTH)

    public val dayOfWeek: RemoteInt
        get() = RemoteInt.createForContextId(RemoteContext.ID_WEEK_DAY)

    public val dayOfYear: RemoteInt
        get() = RemoteInt.createForContextId(RemoteContext.ID_DAY_OF_YEAR)

    public val year: RemoteInt
        get() = RemoteInt.createForContextId(RemoteContext.ID_YEAR)

    public val utcOffsetSeconds: RemoteInt
        get() = RemoteInt.createForContextId(RemoteContext.ID_OFFSET_TO_UTC)

    public val epochSecond: RemoteInt
        get() = RemoteInt.createForContextId(RemoteContext.ID_EPOCH_SECOND)
}
