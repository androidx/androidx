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

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.os.RemoteException
import android.util.Log
import androidx.room.Room.LOG_TAG
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.launch

/**
 * Handles all the communication from [RoomDatabase] and [InvalidationTracker] to
 * [MultiInstanceInvalidationService].
 *
 * @param context The Context to be used for binding [IMultiInstanceInvalidationService].
 * @param name The name of the database file.
 * @param invalidationTracker The [InvalidationTracker]
 */
internal class MultiInstanceInvalidationClient(
    context: Context,
    val name: String,
    val invalidationTracker: InvalidationTracker,
) {
    private val appContext = context.applicationContext
    private val coroutineScope = invalidationTracker.database.getCoroutineScope()

    private val stopped = AtomicBoolean(true)

    /** The client ID assigned by [MultiInstanceInvalidationService]. */
    private var clientId = 0
    private var invalidationService: IMultiInstanceInvalidationService? = null

    /** All table observer to notify service of changes. */
    private val observer =
        object : InvalidationTracker.Observer(invalidationTracker.tableNames) {
            override fun onInvalidated(tables: Set<String>) {
                if (stopped.get()) {
                    return
                }

                try {
                    invalidationService?.broadcastInvalidation(clientId, tables.toTypedArray())
                } catch (e: RemoteException) {
                    Log.w(LOG_TAG, "Cannot broadcast invalidation", e)
                }
            }

            override val isRemote: Boolean
                get() = true
        }

    private val invalidationCallback: IMultiInstanceInvalidationCallback =
        object : IMultiInstanceInvalidationCallback.Stub() {
            override fun onInvalidation(tables: Array<out String>) {
                coroutineScope.launch { invalidationTracker.notifyObserversByTableNames(*tables) }
            }
        }

    private val serviceConnection: ServiceConnection =
        object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName, service: IBinder) {
                invalidationService = IMultiInstanceInvalidationService.Stub.asInterface(service)
                registerCallback()
            }

            override fun onServiceDisconnected(name: ComponentName) {
                invalidationService = null
            }
        }

    private fun registerCallback() {
        try {
            invalidationService?.let { clientId = it.registerCallback(invalidationCallback, name) }
        } catch (e: RemoteException) {
            Log.w(LOG_TAG, "Cannot register multi-instance invalidation callback", e)
        }
    }

    fun start(serviceIntent: Intent) {
        if (stopped.compareAndSet(true, false)) {
            appContext.bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE)
            invalidationTracker.addRemoteObserver(observer)
        }
    }

    fun stop() {
        if (stopped.compareAndSet(false, true)) {
            invalidationTracker.removeObserver(observer)
            try {
                invalidationService?.unregisterCallback(invalidationCallback, clientId)
            } catch (e: RemoteException) {
                Log.w(LOG_TAG, "Cannot unregister multi-instance invalidation callback", e)
            }
            appContext.unbindService(serviceConnection)
        }
    }
}
