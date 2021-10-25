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
import androidx.health.services.client.PassiveMonitoringCallback
import androidx.health.services.client.impl.event.PassiveCallbackEvent
import androidx.health.services.client.impl.ipc.internal.ListenerKey
import androidx.health.services.client.impl.response.PassiveMonitoringUpdateResponse
import androidx.health.services.client.proto.EventsProto.PassiveCallbackEvent.EventCase.EVENT_NOT_SET
import androidx.health.services.client.proto.EventsProto.PassiveCallbackEvent.EventCase.PASSIVE_UPDATE_RESPONSE

/**
 * A stub implementation for IPassiveMonitoringCallback.
 *
 * @hide
 */
public class PassiveMonitoringCallbackStub
private constructor(
    private val packageName: String,
    private val callback: PassiveMonitoringCallback
) : IPassiveMonitoringCallback.Stub() {

    public val listenerKey: ListenerKey = ListenerKey(packageName)

    override fun onPassiveCallbackEvent(event: PassiveCallbackEvent) {
        val proto = event.proto

        when (proto.eventCase) {
            PASSIVE_UPDATE_RESPONSE -> {
                val response = PassiveMonitoringUpdateResponse(proto.passiveUpdateResponse)
                callback.onPassiveMonitoringUpdate(response.passiveMonitoringUpdate)
            }
            null, EVENT_NOT_SET -> Log.w(TAG, "Received unknown event ${proto.eventCase}")
        }
    }

    /**
     * Its important to use the same stub for registration and un-registration, to ensure same
     * binder object is passed by framework to service side of the IPC.
     */
    public class PassiveMonitoringCallbackCache private constructor() {
        @GuardedBy("this")
        private val listeners: MutableMap<String, PassiveMonitoringCallbackStub> = HashMap()

        @Synchronized
        public fun getOrCreate(
            packageName: String,
            callback: PassiveMonitoringCallback
        ): PassiveMonitoringCallbackStub {
            return listeners.getOrPut(packageName) {
                PassiveMonitoringCallbackStub(packageName, callback)
            }
        }

        @Synchronized
        public fun remove(packageName: String): PassiveMonitoringCallbackStub? {
            return listeners.remove(packageName)
        }

        public companion object {
            @JvmField
            public val INSTANCE: PassiveMonitoringCallbackCache = PassiveMonitoringCallbackCache()
        }
    }

    private companion object {
        const val TAG = "PassiveCallbackStub"
    }
}
