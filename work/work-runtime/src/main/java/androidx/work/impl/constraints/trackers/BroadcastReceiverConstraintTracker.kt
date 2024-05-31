/*
 * Copyright (C) 2017 The Android Open Source Project
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
package androidx.work.impl.constraints.trackers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.annotation.RestrictTo
import androidx.work.Logger
import androidx.work.impl.utils.taskexecutor.TaskExecutor

/**
 * A [ConstraintTracker] with a [BroadcastReceiver] for monitoring constraint changes.
 *
 * @param T the constraint data type observed by this tracker
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
abstract class BroadcastReceiverConstraintTracker<T>(context: Context, taskExecutor: TaskExecutor) :
    ConstraintTracker<T>(context, taskExecutor) {
    private val broadcastReceiver: BroadcastReceiver =
        object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                onBroadcastReceive(intent)
            }
        }

    /**
     * Called when the [BroadcastReceiver] is receiving an [Intent] broadcast and should handle the
     * received [Intent].
     *
     * @param intent The [Intent] being received.
     */
    abstract fun onBroadcastReceive(intent: Intent)

    /** @return The [IntentFilter] associated with this tracker. */
    abstract val intentFilter: IntentFilter

    override fun startTracking() {
        Logger.get().debug(TAG, "${javaClass.simpleName}: registering receiver")
        appContext.registerReceiver(broadcastReceiver, intentFilter)
    }

    override fun stopTracking() {
        Logger.get().debug(TAG, "${javaClass.simpleName}: unregistering receiver")
        appContext.unregisterReceiver(broadcastReceiver)
    }
}

private val TAG = Logger.tagWithPrefix("BrdcstRcvrCnstrntTrckr")
