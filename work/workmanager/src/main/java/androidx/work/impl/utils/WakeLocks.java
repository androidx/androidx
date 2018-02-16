/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.work.impl.utils;

import static android.os.PowerManager.PARTIAL_WAKE_LOCK;

import android.content.Context;
import android.os.PowerManager;
import android.support.annotation.NonNull;
import android.support.annotation.RestrictTo;

/**
 * A common class for creating WakeLocks.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class WakeLocks {

    /**
     * Creates and returns a new WakeLock.
     *
     * @param context The context from which to get the PowerManager
     * @param tag A descriptive tag for the WakeLock; this method will prefix "WorkManager: " to it
     * @return A new WakeLock
     */
    public static PowerManager.WakeLock newWakeLock(
            @NonNull Context context,
            @NonNull String tag) {
        PowerManager powerManager = (PowerManager) context.getApplicationContext()
                .getSystemService(Context.POWER_SERVICE);
        return powerManager.newWakeLock(PARTIAL_WAKE_LOCK, "WorkManager: " + tag);
    }

    private WakeLocks() {
    }
}
