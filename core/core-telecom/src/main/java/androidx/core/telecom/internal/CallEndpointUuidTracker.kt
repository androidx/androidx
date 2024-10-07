/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.core.telecom.internal

import android.os.Build
import android.os.ParcelUuid
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.telecom.CallEndpointCompat
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger

/**
 * CallEndpointUuidTracker is responsible for providing a Uuid for emitted [CallEndpointCompat]s.
 * Furthermore, all audio device endpoints (e.g. bluetooth device) should be represented by the same
 * Uuid across sessions.
 *
 * A session can be either a flow created via
 * [androidx.core.telecom.CallsManager.getAvailableStartingCallEndpoints] or a [CallSession] object
 * created via [androidx.core.telecom.CallsManager.addCall]
 *
 * Question: Why is there a need to track the Session that is fetching the UUID ? Answer: ALL
 * Session's need to reference the same uuid for a given [CallEndpointCompat]. Doing so means that
 * all audio output devices are represented by the same [CallEndpointCompat]. When all the Session's
 * are destroyed, the cached bluetooth uuid SHOULD be cleaned up!
 */
@RequiresApi(Build.VERSION_CODES.O)
internal object CallEndpointUuidTracker {
    private val TAG: String = CallEndpointUuidTracker::class.java.simpleName
    private val mNextSessionId = AtomicInteger(0)
    val EARPIECE_UUID: ParcelUuid = ParcelUuid.fromString("e93d936b-3082-450e-9891-1d300c2198e6")
    val SPEAKER_UUID: ParcelUuid = ParcelUuid.fromString("a1b8c6d4-e5f7-48a9-b0c1-d2e3f4a5b6c7")
    val WIRED_HEADSET_UUID: ParcelUuid =
        ParcelUuid.fromString("7c35e196-6e23-461a-8a27-7e45d6a299cf")
    val UNKNOWN_ENDPOINT_UUID: ParcelUuid =
        ParcelUuid.fromString("6b48e033-988f-406a-a3c9-1ab0880e485d")

    // This map is useful to return the same [ParcelUuid] for a given bluetooth device across all
    // sessions.
    // key=[bluetooth device name],
    // value.first = [CallEndpointCompats ParcelUuid] , value.second = [sessions emitting bt device]
    private val mBtDevicesToSessionsMap: HashMap<String, Pair<ParcelUuid, MutableSet<Int>>> =
        HashMap()

    // This map is useful for cleaning up the uuids when all the sessions are destroyed
    // key=[session emitting bt device uuids],
    //    value = [set of bluetooth device names]
    private val mSessionToBtDevicesMap: HashMap<Int, MutableSet<String>> = HashMap()

    internal fun getBluetoothMapping(): HashMap<String, Pair<ParcelUuid, MutableSet<Int>>> {
        return mBtDevicesToSessionsMap
    }

    internal fun getSessionMapping(): HashMap<Int, MutableSet<String>> {
        return mSessionToBtDevicesMap
    }

    internal fun getUuid(sessionId: Int, type: Int, deviceName: String = ""): ParcelUuid {
        if (type != CallEndpointCompat.TYPE_BLUETOOTH) {
            return getNonBtUuid(type)
        }
        trackDeviceForSession(sessionId, deviceName)
        // Either return the existing jetpack uuid for the given device or create a new one.
        // Also, track what sessions are referencing the device uuid for cleanup purposes!
        if (mBtDevicesToSessionsMap.containsKey(deviceName)) {
            val uuidAndTrackedSessions = mBtDevicesToSessionsMap[deviceName]
            uuidAndTrackedSessions?.second?.add(sessionId)
            return uuidAndTrackedSessions!!.first
        } else {
            val jetpackEndpointUuid = ParcelUuid(UUID.randomUUID())
            mBtDevicesToSessionsMap[deviceName] = Pair(jetpackEndpointUuid, mutableSetOf(sessionId))
            return jetpackEndpointUuid
        }
    }

    private fun getNonBtUuid(type: Int): ParcelUuid {
        return when (type) {
            CallEndpointCompat.TYPE_EARPIECE -> {
                EARPIECE_UUID
            }
            CallEndpointCompat.TYPE_SPEAKER -> {
                SPEAKER_UUID
            }
            CallEndpointCompat.TYPE_WIRED_HEADSET -> {
                WIRED_HEADSET_UUID
            }
            else -> {
                UNKNOWN_ENDPOINT_UUID
            }
        }
    }

    /**
     * Start tracking the Bluetooth Device for the given Session. This is helpful in the cleanup
     * process for individual Sessions so there isn't any memory leak.
     */
    private fun trackDeviceForSession(sessionId: Int, deviceName: String) {
        Log.i(TAG, "sessionId=[$sessionId], btName=[$deviceName]")
        val btDevices = mSessionToBtDevicesMap.computeIfAbsent(sessionId) { mutableSetOf() }
        btDevices.add(deviceName)
    }

    internal fun startSession(): Int {
        val newSessionId = mNextSessionId.getAndIncrement()
        Log.i(TAG, "startSession: sessionId=[$newSessionId]")
        return newSessionId
    }

    internal fun endSession(session: Int) {
        Log.i(TAG, "endSession: sessionId=[$session]")
        if (!mSessionToBtDevicesMap.containsKey(session)) {
            return
        }
        val btDevices = mSessionToBtDevicesMap[session]?.toList()
        if (btDevices != null) {
            for (deviceName in btDevices) {
                val uuidAndTrackedSessions = mBtDevicesToSessionsMap[deviceName]
                if (uuidAndTrackedSessions != null) {
                    val sessions = uuidAndTrackedSessions.second
                    sessions.remove(session)
                    if (sessions.size == 0) {
                        // Once all the sessions that are tracking a bt device have been
                        // destroyed, we can cleanup the references to the bt device uuid.
                        mBtDevicesToSessionsMap.remove(deviceName)
                    }
                }
            }
        }
        // The CallSession is either being destroyed or a Callback Flow is being cancelled
        mSessionToBtDevicesMap.remove(session)
        mNextSessionId.decrementAndGet()
    }
}
