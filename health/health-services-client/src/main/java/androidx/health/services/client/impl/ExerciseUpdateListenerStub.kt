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
import androidx.health.services.client.ExerciseUpdateCallback
import androidx.health.services.client.data.Availability
import androidx.health.services.client.data.DataType
import androidx.health.services.client.data.ExerciseEvent
import androidx.health.services.client.data.ExerciseLapSummary
import androidx.health.services.client.data.ExerciseUpdate
import androidx.health.services.client.impl.event.ExerciseUpdateListenerEvent
import androidx.health.services.client.impl.ipc.internal.ListenerKey
import androidx.health.services.client.proto.EventsProto
import androidx.health.services.client.proto.EventsProto.ExerciseUpdateListenerEvent.EventCase
import java.util.HashMap
import java.util.concurrent.Executor

/** A stub implementation for IExerciseUpdateListener. */
internal class ExerciseUpdateListenerStub internal constructor(
    private val listener: ExerciseUpdateCallback,
    private val executor: Executor,
    private val requestedDataTypesProvider: () -> Set<DataType<*, *>>
) : IExerciseUpdateListener.Stub() {

    public val listenerKey: ListenerKey = ListenerKey(listener)

    override fun onExerciseUpdateListenerEvent(event: ExerciseUpdateListenerEvent) {
        executor.execute { triggerListener(event.proto) }
    }

    private fun triggerListener(proto: EventsProto.ExerciseUpdateListenerEvent) {
        when (proto.eventCase) {
            EventCase.EXERCISE_UPDATE_RESPONSE ->
                listener.onExerciseUpdateReceived(
                    ExerciseUpdate(proto.exerciseUpdateResponse.exerciseUpdate)
                )
            EventCase.LAP_SUMMARY_RESPONSE ->
                listener.onLapSummaryReceived(
                    ExerciseLapSummary(proto.lapSummaryResponse.lapSummary)
                )
            EventCase.AVAILABILITY_RESPONSE -> {
                val requestedDataTypes = requestedDataTypesProvider.invoke()
                if (requestedDataTypes.isEmpty()) {
                    Log.w(TAG, "Availability received without any requested DataTypes")
                    return
                }
                // The DataType in the Availability Response could be a delta or aggregate; there's
                // not enough information to distinguish. For example, the developer might have
                // requested ether or both of HEART_RATE_BPM / HEART_RATE_BPM_STATS. We should
                // trigger onAvailabilityChanged for all matching data types.
                val matchingDataTypes = requestedDataTypes.filter {
                    it.name == proto.availabilityResponse.dataType.name
                }
                val availability = Availability.fromProto(proto.availabilityResponse.availability)
                matchingDataTypes.forEach { listener.onAvailabilityChanged(it, availability) }
            }
            EventCase.EXERCISE_EVENT_RESPONSE ->
                listener
                    .onExerciseEventReceived(
                        ExerciseEvent.fromProto(
                            proto.exerciseEventResponse.exerciseEvent))
            null,
            EventCase.EVENT_NOT_SET -> Log.w(TAG, "Received unknown event ${proto.eventCase}")
        }
    }

    /**
     * A class that stores unique active instances of [ExerciseUpdateCallback] to ensure same binder
     * object is passed by framework to service side of the IPC.
     */
    public class ExerciseUpdateListenerCache private constructor() {
        private val listenerLock = Any()

        @GuardedBy("listenerLock")
        private val listeners: MutableMap<ExerciseUpdateCallback, ExerciseUpdateListenerStub> =
            HashMap()

        public fun getOrCreate(
            listener: ExerciseUpdateCallback,
            executor: Executor,
            requestedDataTypesProvider: () -> Set<DataType<*, *>>
        ): ExerciseUpdateListenerStub {
            synchronized(listenerLock) {
                return listeners.getOrPut(listener) {
                    ExerciseUpdateListenerStub(listener, executor, requestedDataTypesProvider)
                }
            }
        }

        public fun remove(
            exerciseUpdateCallback: ExerciseUpdateCallback
        ): ExerciseUpdateListenerStub? {
            synchronized(listenerLock) {
                return listeners.remove(exerciseUpdateCallback)
            }
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
