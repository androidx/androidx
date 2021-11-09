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

import android.util.Log
import androidx.annotation.GuardedBy
import androidx.health.services.client.ExerciseUpdateListener
import androidx.health.services.client.data.Availability
import androidx.health.services.client.data.DataType
import androidx.health.services.client.data.ExerciseLapSummary
import androidx.health.services.client.data.ExerciseUpdate
import androidx.health.services.client.impl.event.ExerciseUpdateListenerEvent
import androidx.health.services.client.impl.ipc.internal.ListenerKey
import androidx.health.services.client.proto.EventsProto
import androidx.health.services.client.proto.EventsProto.ExerciseUpdateListenerEvent.EventCase
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

    override fun onExerciseUpdateListenerEvent(event: ExerciseUpdateListenerEvent) {
        executor.execute { triggerListener(event.proto) }
    }

    private fun triggerListener(proto: EventsProto.ExerciseUpdateListenerEvent) {
        when (proto.eventCase) {
            EventCase.EXERCISE_UPDATE_RESPONSE ->
                listener.onExerciseUpdate(
                    ExerciseUpdate(proto.exerciseUpdateResponse.exerciseUpdate)
                )
            EventCase.LAP_SUMMARY_RESPONSE ->
                listener.onLapSummary(ExerciseLapSummary(proto.lapSummaryResponse.lapSummary))
            EventCase.AVAILABILITY_RESPONSE ->
                listener.onAvailabilityChanged(
                    DataType(proto.availabilityResponse.dataType),
                    Availability.fromProto(proto.availabilityResponse.availability)
                )
            null, EventCase.EVENT_NOT_SET -> Log.w(TAG, "Received unknown event ${proto.eventCase}")
        }
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

    private companion object {
        val TAG = "ExerciseUpdateListener"
    }
}
