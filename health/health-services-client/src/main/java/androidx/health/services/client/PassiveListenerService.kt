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

package androidx.health.services.client

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.health.services.client.impl.IPassiveListenerService
import androidx.health.services.client.impl.event.PassiveListenerEvent
import androidx.health.services.client.impl.response.PassiveMonitoringGoalResponse
import androidx.health.services.client.impl.response.PassiveMonitoringUpdateResponse
import androidx.health.services.client.proto.EventsProto.PassiveListenerEvent.EventCase.EVENT_NOT_SET
import androidx.health.services.client.proto.EventsProto.PassiveListenerEvent.EventCase.HEALTH_EVENT_RESPONSE
import androidx.health.services.client.proto.EventsProto.PassiveListenerEvent.EventCase.PASSIVE_GOAL_RESPONSE
import androidx.health.services.client.proto.EventsProto.PassiveListenerEvent.EventCase.PASSIVE_UPDATE_RESPONSE
import androidx.health.services.client.proto.EventsProto.PassiveListenerEvent.EventCase.PERMISSION_LOST_RESPONSE

/**
 * Service that enables receiving passive monitoring updates throughout the day when the app may not
 * be running.
 *
 * Health Services will bind to the [PassiveListenerService] to deliver passive monitoring updates
 * such as data or goal updates. Clients should extend this service and override those methods of
 * the [PassiveListenerCallback] that they care about. They can then pass in their service to
 * [PassiveMonitoringClient.registerPassiveListenerServiceAsync] to receive data updates.
 */
// TODO(b/227475943): open up visibility
internal abstract class PassiveListenerService : PassiveListenerCallback, Service() {

    private var wrapper: IPassiveListenerServiceWrapper? = null

    final override fun onBind(intent: Intent): IBinder? {
        wrapper = IPassiveListenerServiceWrapper()
        return wrapper
    }

    private inner class IPassiveListenerServiceWrapper : IPassiveListenerService.Stub() {

        override fun onPassiveListenerEvent(event: PassiveListenerEvent) {
            val proto = event.proto

            when (proto.eventCase) {
                PASSIVE_UPDATE_RESPONSE -> {
                    val response = PassiveMonitoringUpdateResponse(proto.passiveUpdateResponse)
                    if (!response.passiveMonitoringUpdate.dataPoints.isEmpty()) {
                        this@PassiveListenerService.onNewDataPoints(
                            response.passiveMonitoringUpdate.dataPoints
                        )
                    }
                    for (userActivityInfo in
                        response.passiveMonitoringUpdate.userActivityInfoUpdates) {
                        this@PassiveListenerService.onUserActivityInfo(userActivityInfo)
                    }
                }
                PASSIVE_GOAL_RESPONSE -> {
                    val response = PassiveMonitoringGoalResponse(proto.passiveGoalResponse)
                    this@PassiveListenerService.onGoalCompleted(response.passiveGoal)
                }
                HEALTH_EVENT_RESPONSE -> {
                    // TODO(b/227475943): fill in when health events are added.
                }
                PERMISSION_LOST_RESPONSE -> {
                    this@PassiveListenerService.onPermissionLost()
                }
                null, EVENT_NOT_SET -> Log.w(TAG, "Received unknown event ${proto.eventCase}")
            }
        }

        override fun getApiVersion(): Int {
            return API_VERSION
        }
    }

    private companion object {
        const val TAG = "PassiveListenerService"
    }
}
