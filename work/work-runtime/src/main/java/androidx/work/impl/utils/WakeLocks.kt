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

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.work.Logger;

import java.util.HashMap;
import java.util.Map;
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
        // Wakelocks are created on the command processor thread, but we check if they are still
        // being held on the main thread.
        synchronized (sWakeLocks) {
            sWakeLocks.put(wakeLock, tagWithPrefix);
        }
        return wakeLock;
    }

    /**
     * Checks to see if there are any {@link PowerManager.WakeLock}s that
     * {@link androidx.work.impl.background.systemalarm.SystemAlarmService} holds when all the
     * pending commands have been drained in the command queue.
     */
    public static void checkWakeLocks() {
        // There is a small chance that while we are checking if all the commands in the queue are
        // drained and wake locks are no longer being held, a new command comes along and we end up
        // with a ConcurrentModificationException. The addition of commands happens on the command
        // processor thread and this check is done on the main thread.

        Map<PowerManager.WakeLock, String> wakeLocksCopy = new HashMap<>();
        synchronized (sWakeLocks) {
            // Copy the WakeLocks - otherwise we can get a ConcurrentModificationException if the
            // garbage collector kicks in and ends up removing something from the master copy while
            // we are iterating over it.
            wakeLocksCopy.putAll(sWakeLocks);
        }

        for (PowerManager.WakeLock wakeLock : wakeLocksCopy.keySet()) {
            if (wakeLock != null && wakeLock.isHeld()) {
                String message = "WakeLock held for " + wakeLocksCopy.get(wakeLock);
                Logger.get().warning(TAG, message);
            }
        }
    }

    private WakeLocks() {
    }
}
