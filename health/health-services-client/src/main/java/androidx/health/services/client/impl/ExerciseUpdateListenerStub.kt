/*
 * Copyright (C) 2021 The Android Open Source Project
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

package androidx.health.services.client.impl

import androidx.annotation.GuardedBy
import androidx.health.services.client.ExerciseUpdateListener
import androidx.health.services.client.impl.ipc.internal.ListenerKey
import androidx.health.services.client.impl.response.ExerciseLapSummaryResponse
import androidx.health.services.client.impl.response.ExerciseUpdateResponse
import java.util.HashMap
import java.util.concurrent.Executor

/**
 * A stub implementation for IExerciseUpdateListener.
 *
 * @hide
 */
public class ExerciseUpdateListenerStub
private constructor(private val listener: ExerciseUpdateListener, private val executor: Executor) :
    IExerciseUpdateListener.Stub() {

    public val listenerKey: ListenerKey = ListenerKey(listener)

    override fun onExerciseUpdate(response: ExerciseUpdateResponse) {
        executor.execute { listener.onExerciseUpdate(response.exerciseUpdate) }
    }

    override fun onLapSummary(response: ExerciseLapSummaryResponse) {
        executor.execute { listener.onLapSummary(response.exerciseLapSummary) }
    }

    /**
     * A class that stores unique active instances of [ExerciseUpdateListener] to ensure same binder
     * object is passed by framework to service side of the IPC.
     */
    public class ExerciseUpdateListenerCache private constructor() {
        @GuardedBy("this")
        private val listeners: MutableMap<ExerciseUpdateListener, ExerciseUpdateListenerStub> =
            HashMap()

        @Synchronized
        public fun getOrCreate(
            listener: ExerciseUpdateListener,
            executor: Executor
        ): ExerciseUpdateListenerStub {
            return listeners.getOrPut(listener) { ExerciseUpdateListenerStub(listener, executor) }
        }

        @Synchronized
        public fun remove(
            exerciseUpdateListener: ExerciseUpdateListener
        ): ExerciseUpdateListenerStub? {
            return listeners.remove(exerciseUpdateListener)
        }

        public companion object {
            @JvmField
            public val INSTANCE: ExerciseUpdateListenerCache = ExerciseUpdateListenerCache()
        }
    }
}
