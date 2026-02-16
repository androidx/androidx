/*
 * Copyright (C) 2023 The Android Open Source Project
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
package androidx.compose.remote.core;

import androidx.annotation.RestrictTo;

import org.jspecify.annotations.NonNull;

/** This generates the standard system variables for time. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class TimeVariables {
    private @NonNull RemoteClock mClock;

    public TimeVariables(@NonNull RemoteClock clock) {
        this.mClock = clock;
    }

    /** Returns the current clock. */
    public @NonNull RemoteClock getClock() {
        return mClock;
    }

    /**
     * This class populates all time variables in the system
     */
    public void updateTime(@NonNull RemoteContext context) {
        RemoteClock.TimeSnapshot snapshot = mClock.snapshot(null);

        context.loadFloat(RemoteContext.ID_OFFSET_TO_UTC, snapshot.getOffsetSeconds());
        context.loadFloat(RemoteContext.ID_CONTINUOUS_SEC, snapshot.getContinuousSeconds());
        context.loadInteger(RemoteContext.ID_EPOCH_SECOND, snapshot.getEpochSeconds());
        context.loadFloat(RemoteContext.ID_TIME_IN_SEC, snapshot.getTimeInSec());
        context.loadFloat(RemoteContext.ID_TIME_IN_MIN, snapshot.getTimeInMin());
        context.loadFloat(RemoteContext.ID_TIME_IN_HR, snapshot.getHour());
        context.loadFloat(RemoteContext.ID_CALENDAR_MONTH, snapshot.getMonth());
        context.loadFloat(RemoteContext.ID_DAY_OF_MONTH, snapshot.getDayOfMonth());
        context.loadFloat(RemoteContext.ID_WEEK_DAY, snapshot.getDayOfWeek());
        context.loadFloat(RemoteContext.ID_DAY_OF_YEAR, snapshot.getDayOfYear());
        context.loadFloat(RemoteContext.ID_YEAR, snapshot.getYear());

        context.loadFloat(RemoteContext.ID_API_LEVEL,
                CoreDocument.getDocumentApiLevel() + CoreDocument.BUILD);
    }
}
