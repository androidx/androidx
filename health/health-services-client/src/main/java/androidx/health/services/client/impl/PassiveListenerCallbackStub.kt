/*
 * Copyright (C) 2022 The Android Open Source Project
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
import androidx.health.services.client.PassiveListenerCallback
import androidx.health.services.client.impl.event.PassiveListenerEvent
import androidx.health.services.client.impl.ipc.internal.ListenerKey
import androidx.health.services.client.impl.response.HealthEventResponse
import androidx.health.services.client.impl.response.PassiveMonitoringGoalResponse
import androidx.health.services.client.impl.response.PassiveMonitoringUpdateResponse
import androidx.health.services.client.proto.EventsProto
import androidx.health.services.client.proto.EventsProto.PassiveListenerEvent.EventCase.EVENT_NOT_SET
import androidx.health.services.client.proto.EventsProto.PassiveListenerEvent.EventCase.HEALTH_EVENT_RESPONSE
import androidx.health.services.client.proto.EventsProto.PassiveListenerEvent.EventCase.PASSIVE_GOAL_RESPONSE
import androidx.health.services.client.proto.EventsProto.PassiveListenerEvent.EventCase.PASSIVE_UPDATE_RESPONSE
import androidx.health.services.client.proto.EventsProto.PassiveListenerEvent.EventCase.PERMISSION_LOST_RESPONSE
import java.util.concurrent.Executor

/** A stub implementation for IPassiveListenerCallback. */
internal class PassiveListenerCallbackStub(
    private val packageName: String,
    private val executor: Executor,
    private val callback: PassiveListenerCallback
) : IPassiveListenerCallback.Stub() {

    public val listenerKey: ListenerKey = ListenerKey(packageName)

    override fun onPassiveListenerEvent(event: PassiveListenerEvent) {
        executor.execute { triggerListener(event.proto) }
    }

    private fun triggerListener(proto: EventsProto.PassiveListenerEvent) {
        when (proto.eventCase) {
            PASSIVE_UPDATE_RESPONSE -> {
                val response = PassiveMonitoringUpdateResponse(proto.passiveUpdateResponse)
                if (response.passiveMonitoringUpdate.dataPoints.dataPoints.isNotEmpty()) {
                    callback.onNewDataPointsReceived(response.passiveMonitoringUpdate.dataPoints)
                }
                for (userActivityInfo in response.passiveMonitoringUpdate.userActivityInfoUpdates) {
                    callback.onUserActivityInfoReceived(userActivityInfo)
                }
            }
            PASSIVE_GOAL_RESPONSE -> {
                val response = PassiveMonitoringGoalResponse(proto.passiveGoalResponse)
                callback.onGoalCompleted(response.passiveGoal)
            }
            HEALTH_EVENT_RESPONSE -> {
                val response = HealthEventResponse(proto.healthEventResponse)
                callback.onHealthEventReceived(response.healthEvent)
            }
            PERMISSION_LOST_RESPONSE -> {
                callback.onPermissionLost()
            }
            null, EVENT_NOT_SET -> Log.w(TAG, "Received unknown event ${proto.eventCase}")
        }
    }

    /**
     * Its important to use the same stub for registration and un-registration, to ensure same
     * binder object is passed by framework to service side of the IPC.
     */
    internal class PassiveListenerCallbackCache private constructor() {
        private val listenerLock = Any()

        @GuardedBy("listenerLock")
        private val listeners: MutableMap<String, PassiveListenerCallbackStub> = HashMap()

        public fun getOrCreate(
            packageName: String,
            executor: Executor,
            callback: PassiveListenerCallback
        ): PassiveListenerCallbackStub {
            synchronized(listenerLock) {
                return listeners.getOrPut(packageName) {
                    PassiveListenerCallbackStub(packageName, executor, callback)
                }
            }
        }

        public fun remove(packageName: String): PassiveListenerCallbackStub? {
            synchronized(listenerLock) {
                return listeners.remove(packageName)
            }
        }

        public companion object {
            @JvmField
            public val INSTANCE: PassiveListenerCallbackCache = PassiveListenerCallbackCache()
        }
    }

    private companion object {
        const val TAG = "PassiveListenerCallbackStub"
    }
}
