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
package androidx.room3

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.os.RemoteException
import android.util.Log
import androidx.room3.Room.LOG_TAG
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onStart
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

    /** The set of tables ids invalidated from another instance. */
    private val invalidatedTables =
        MutableSharedFlow<Set<String>>(
            replay = 0,
            extraBufferCapacity = 0,
            onBufferOverflow = BufferOverflow.SUSPEND,
        )

    /** All tables invalidation flow collecting job to notify service of changes. */
    private var job: Job? = null

    private val invalidationCallback: IMultiInstanceInvalidationCallback =
        object : IMultiInstanceInvalidationCallback.Stub() {
            override fun onInvalidation(tables: Array<out String>) {
                coroutineScope.launch {
                    val invalidatedTablesSet = setOf(*tables)
                    invalidatedTables.emit(invalidatedTablesSet)
                }
            }

            override fun getInterfaceVersion(): Int = VERSION
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
            val latch = CountDownLatch(1)
            job =
                coroutineScope.launch {
                    invalidationTracker
                        .createAllTablesFlow()
                        .onStart { latch.countDown() }
                        .collect { tables ->
                            if (stopped.get()) {
                                throw CancellationException()
                            }

                            try {
                                invalidationService?.broadcastInvalidation(
                                    clientId,
                                    tables.toTypedArray(),
                                )
                            } catch (e: RemoteException) {
                                Log.w(LOG_TAG, "Cannot broadcast invalidation", e)
                            }
                        }
                }
            // Await initialization of the all tables invalidation flow, if we do not wait, then
            // invalidation can be lost if the first interaction with the database is a write.
            if (!latch.await(200, TimeUnit.MILLISECONDS)) {
                Log.w(
                    LOG_TAG,
                    "Timed out waiting for multi-instance invalidation start. " +
                        "It will continue to start but invalidation might be missed on the other " +
                        "instance.",
                )
            }
        }
    }

    fun stop() {
        if (stopped.compareAndSet(false, true)) {
            job?.cancel()
            try {
                invalidationService?.unregisterCallback(invalidationCallback, clientId)
            } catch (e: RemoteException) {
                Log.w(LOG_TAG, "Cannot unregister multi-instance invalidation callback", e)
            }
            appContext.unbindService(serviceConnection)
        }
    }

    fun createFlow(resolvedTableNames: Array<out String>): Flow<Set<String>> =
        invalidatedTables.mapNotNull { invalidatedTableNames ->
            resolvedTableNames.intersect(invalidatedTableNames).ifEmpty { null }
        }
}
