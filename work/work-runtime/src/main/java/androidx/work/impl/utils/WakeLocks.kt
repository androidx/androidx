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
@file:JvmName("WakeLocks")

package androidx.work.impl.utils

import android.content.Context
import android.os.PowerManager
import androidx.work.Logger
import java.util.WeakHashMap

private val TAG = Logger.tagWithPrefix("WakeLocks")

/**
 * Creates and returns a new WakeLock.
 *
 * @param context The context from which to get the PowerManager
 * @param tag A descriptive tag for the WakeLock; this method will prefix "WorkManager: " to it
 * @return A new [android.os.PowerManager.WakeLock]
 */
internal fun newWakeLock(context: Context, tag: String): PowerManager.WakeLock {
    val powerManager = context.applicationContext
        .getSystemService(Context.POWER_SERVICE) as PowerManager
    val tagWithPrefix = "WorkManager: $tag"
    val wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, tagWithPrefix)
    // Wakelocks are created on the command processor thread, but we check if they are still
    // being held on the main thread.
    synchronized(WakeLocksHolder) {
        WakeLocksHolder.wakeLocks.put(wakeLock, tagWithPrefix)
    }
    return wakeLock
}

/**
 * Checks to see if there are any [PowerManager.WakeLock]s that
 * [androidx.work.impl.background.systemalarm.SystemAlarmService] holds when all the
 * pending commands have been drained in the command queue.
 */
fun checkWakeLocks() {
    // There is a small chance that while we are checking if all the commands in the queue are
    // drained and wake locks are no longer being held, a new command comes along and we end up
    // with a ConcurrentModificationException. The addition of commands happens on the command
    // processor thread and this check is done on the main thread.
    val wakeLocksCopy = mutableMapOf<PowerManager.WakeLock?, String>()
    synchronized(WakeLocksHolder) {
        // Copy the WakeLocks - otherwise we can get a ConcurrentModificationException if the
        // garbage collector kicks in and ends up removing something from the master copy while
        // we are iterating over it.
        wakeLocksCopy.putAll(WakeLocksHolder.wakeLocks)
    }

    wakeLocksCopy.forEach { (wakeLock, tag) ->
        if (wakeLock?.isHeld == true) Logger.get().warning(TAG, "WakeLock held for $tag")
    }
}

private object WakeLocksHolder {
    val wakeLocks = WeakHashMap<PowerManager.WakeLock, String>()
}
