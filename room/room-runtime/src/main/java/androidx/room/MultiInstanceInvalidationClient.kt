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
import java.util.concurrent.Executor
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Handles all the communication from [RoomDatabase] and [InvalidationTracker] to
 * [MultiInstanceInvalidationService].
 *
 * @param context             The Context to be used for binding
 * [IMultiInstanceInvalidationService].
 * @param name                The name of the database file.
 * @param serviceIntent       The [Intent] used for binding
 * [IMultiInstanceInvalidationService].
 * @param invalidationTracker The [InvalidationTracker]
 * @param executor            The background executor.
 */
internal class MultiInstanceInvalidationClient(
    context: Context,
    val name: String,
    serviceIntent: Intent,
    val invalidationTracker: InvalidationTracker,
    val executor: Executor
) {
    private val appContext = context.applicationContext

    /**
     * The client ID assigned by [MultiInstanceInvalidationService].
     */
    var clientId = 0
    lateinit var observer: InvalidationTracker.Observer
    var service: IMultiInstanceInvalidationService? = null

    val callback: IMultiInstanceInvalidationCallback =
        object : IMultiInstanceInvalidationCallback.Stub() {
            override fun onInvalidation(tables: Array<out String>) {
                executor.execute { invalidationTracker.notifyObserversByTableNames(*tables) }
            }
        }

    val stopped = AtomicBoolean(false)

    val serviceConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            this@MultiInstanceInvalidationClient.service =
                IMultiInstanceInvalidationService.Stub.asInterface(service)
            executor.execute(setUpRunnable)
        }

        override fun onServiceDisconnected(name: ComponentName) {
            executor.execute(removeObserverRunnable)
            service = null
        }
    }

    val setUpRunnable = Runnable {
        try {
            service?.let {
                clientId = it.registerCallback(callback, name)
                invalidationTracker.addObserver(observer)
            }
        } catch (e: RemoteException) {
            Log.w(LOG_TAG, "Cannot register multi-instance invalidation callback", e)
        }
    }

    val removeObserverRunnable = Runnable { invalidationTracker.removeObserver(observer) }

    init {
        // Use all tables names for observer.
        val tableNames: Set<String> = invalidationTracker.tableIdLookup.keys
        observer = object : InvalidationTracker.Observer(tableNames.toTypedArray()) {
            override fun onInvalidated(tables: Set<String>) {
                if (stopped.get()) {
                    return
                }

                try {
                    service?.broadcastInvalidation(clientId, tables.toTypedArray())
                } catch (e: RemoteException) {
                    Log.w(LOG_TAG, "Cannot broadcast invalidation", e)
                }
            }

            override val isRemote: Boolean
                get() = true
        }
        appContext.bindService(
            serviceIntent,
            serviceConnection,
            Context.BIND_AUTO_CREATE
        )
    }

    fun stop() {
        if (stopped.compareAndSet(false, true)) {
            invalidationTracker.removeObserver(observer)
            try {
                service?.unregisterCallback(callback, clientId)
            } catch (e: RemoteException) {
                Log.w(LOG_TAG, "Cannot unregister multi-instance invalidation callback", e)
            }
            appContext.unbindService(serviceConnection)
        }
    }
}
