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
package androidx.room

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.os.RemoteCallbackList
import android.os.RemoteException
import android.util.Log
import androidx.room.Room.LOG_TAG

/**
 * A [Service] for remote invalidation among multiple [InvalidationTracker] instances.
 * This service runs in the main app process. All the instances of [InvalidationTracker]
 * (potentially in other processes) has to connect to this service.
 *
 * The intent to launch it can be specified by
 * [RoomDatabase.Builder.setMultiInstanceInvalidationServiceIntent], although the service is
 * defined in the manifest by default so there should be no need to override it in a normal
 * situation.
 */
@ExperimentalRoomApi
class MultiInstanceInvalidationService : Service() {
    internal var maxClientId = 0
    internal val clientNames = mutableMapOf<Int, String>()

    internal val callbackList: RemoteCallbackList<IMultiInstanceInvalidationCallback> =
        object : RemoteCallbackList<IMultiInstanceInvalidationCallback>() {
            override fun onCallbackDied(
                callback: IMultiInstanceInvalidationCallback,
                cookie: Any
            ) {
                clientNames.remove(cookie as Int)
            }
        }

    private val binder: IMultiInstanceInvalidationService.Stub =
        object : IMultiInstanceInvalidationService.Stub() {
            // Assigns a client ID to the client.
            override fun registerCallback(
                callback: IMultiInstanceInvalidationCallback,
                name: String?
            ): Int {
                if (name == null) {
                    return 0
                }
                synchronized(callbackList) {
                    val clientId = ++maxClientId
                    // Use the client ID as the RemoteCallbackList cookie.
                    return if (callbackList.register(callback, clientId)) {
                        clientNames[clientId] = name
                        clientId
                    } else {
                        --maxClientId
                        0
                    }
                }
            }

            // Explicitly removes the client.
            // The client can die without calling this. In that case, callbackList
            // .onCallbackDied() can take care of removal.
            override fun unregisterCallback(
                callback: IMultiInstanceInvalidationCallback,
                clientId: Int
            ) {
                synchronized(callbackList) {
                    callbackList.unregister(callback)
                    clientNames.remove(clientId)
                }
            }

            // Broadcasts table invalidation to other instances of the same database file.
            // The broadcast is not sent to the caller itself.
            override fun broadcastInvalidation(clientId: Int, tables: Array<out String>) {
                synchronized(callbackList) {
                    val name = clientNames[clientId]
                    if (name == null) {
                        Log.w(LOG_TAG, "Remote invalidation client ID not registered")
                        return
                    }
                    val count = callbackList.beginBroadcast()
                    try {
                        for (i in 0 until count) {
                            val targetClientId = callbackList.getBroadcastCookie(i) as Int
                            val targetName = clientNames[targetClientId]
                            if (clientId == targetClientId || name != targetName) {
                                // Skip if this is the caller itself or broadcast is for another
                                // database.
                                continue
                            }
                            try {
                                callbackList.getBroadcastItem(i).onInvalidation(tables)
                            } catch (e: RemoteException) {
                                Log.w(LOG_TAG, "Error invoking a remote callback", e)
                            }
                        }
                    } finally {
                        callbackList.finishBroadcast()
                    }
                }
            }
        }

    override fun onBind(intent: Intent): IBinder {
        return binder
    }
}
