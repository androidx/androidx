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

import androidx.work.Logger;

import java.util.WeakHashMap;

/**
 * A common class for creating WakeLocks.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class WakeLocks {

    private static final String TAG = Logger.tagWithPrefix("WakeLocks");

    private static final WeakHashMap<PowerManager.WakeLock, String> sWakeLocks =
            new WeakHashMap<>();

    /**
     * Creates and returns a new WakeLock.
     *
     * @param context The context from which to get the PowerManager
     * @param tag     A descriptive tag for the WakeLock; this method will prefix "WorkManager: "
     *                to it
     * @return A new {@link android.os.PowerManager.WakeLock}
     */
    public static PowerManager.WakeLock newWakeLock(
            @NonNull Context context,
            @NonNull String tag) {
        PowerManager powerManager = (PowerManager) context.getApplicationContext()
                .getSystemService(Context.POWER_SERVICE);

        String tagWithPrefix = "WorkManager: " + tag;
        PowerManager.WakeLock wakeLock = powerManager.newWakeLock(PARTIAL_WAKE_LOCK, tagWithPrefix);
        // All wakelocks are only created on the command handler thread. No need to synchronize.
        sWakeLocks.put(wakeLock, tagWithPrefix);
        return wakeLock;
    }

    /**
     * Checks to see if there are any {@link PowerManager.WakeLock}s that
     * {@link androidx.work.impl.background.systemalarm.SystemAlarmService} holds when all the
     * pending commands have been drained in the command queue.
     */
    public static void checkWakeLocks() {
        for (PowerManager.WakeLock wakeLock : sWakeLocks.keySet()) {
            if (wakeLock.isHeld()) {
                String message = String.format("WakeLock held for %s", sWakeLocks.get(wakeLock));
                Logger.get().warning(TAG, message);
            }
        }
    }

    private WakeLocks() {
    }
}
