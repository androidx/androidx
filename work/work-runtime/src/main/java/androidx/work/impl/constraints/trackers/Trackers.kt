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

import android.content.Context
import androidx.annotation.RestrictTo
import androidx.work.impl.constraints.NetworkState
import androidx.work.impl.utils.taskexecutor.TaskExecutor

/**
 * A singleton class to hold an instance of each [ConstraintTracker].
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class Trackers
@JvmOverloads
constructor(
    context: Context,
    taskExecutor: TaskExecutor,
    /**
     * The tracker used to track the battery charging status.
     */
    val batteryChargingTracker: ConstraintTracker<Boolean> =
        BatteryChargingTracker(context.applicationContext, taskExecutor),

    /**
     * The tracker used to track if the battery is okay or low.
     */
    val batteryNotLowTracker: BatteryNotLowTracker =
        BatteryNotLowTracker(context.applicationContext, taskExecutor),

    /**
     * The tracker used to track network state changes.
     */
    val networkStateTracker: ConstraintTracker<NetworkState> =
        NetworkStateTracker(context.applicationContext, taskExecutor),

    /**
     * The tracker used to track if device storage is okay or low.
     */
    val storageNotLowTracker: ConstraintTracker<Boolean> =
        StorageNotLowTracker(context.applicationContext, taskExecutor),
)
