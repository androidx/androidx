/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.wear.tiles.connection

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.wear.tiles.TileProvider
import androidx.wear.tiles.TileProviderService
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.util.concurrent.TimeUnit.SECONDS
import kotlin.coroutines.resume

/**
 * Connection binder for Tiles. This will connect to a {@link TileProvider} (with a timeout), and
 * run the given coroutine, passsing the tile provider to it. After all coroutines passed to
 * runWithTilesConnection have exited, this class will then schedule a timer. When this timer
 * elapses, it will unbind from the tile provider.
 */
internal class TilesConnectionBinder(
    private val context: Context,
    private val componentName: ComponentName,
    private val coroutineScope: CoroutineScope,
    private val backgroundCoroutineDispatcher: CoroutineDispatcher
) {
    companion object {
        internal val BIND_TIMEOUT_MILLIS = SECONDS.toMillis(10)
        internal val INACTIVITY_TIMEOUT_MILLIS = SECONDS.toMillis(1) // 1s
    }

    // Reference counter. When this drops to zero, a job is scheduled to unbind from the tile
    // provider. If this becomes non-zero in that timeout, the cleanup job is aborted.
    private var activeCoroutines = 0

    // Job for connecting to TileProvider. If a bind is in progress, this will be non-null and can
    // be awaited by tasks passed to runWithTilesConnection.
    private var connectBinderJob: Deferred<TileProvider>? = null

    // Job to unbind from the tile provider.
    private var releaseBinderJob: Job? = null

    // Instance of a tile provider. Note that presence of this alone is not enough to know that
    // this class is bound to the tile provider; you must also check whether or not the binder is
    // dead.
    private var tileProvider: TileProvider? = null
    private var connection: ServiceConnection? = null

    /**
     * Bind to the tile provider, if needed, then call the given coroutine, passing the tile
     * provider to it.
     */
    suspend fun <T> runWithTilesConnection(
        fn: suspend CoroutineScope.(TileProvider) -> T
    ): T = coroutineScope {
        // Cache the original context, then use withContext to dispatch fn on it. The alternative is
        // to update mRefCnt in a withContext(mDispatcher), then putting the rest of the function
        // in a try/finally, which itself uses withContext to call getBinder/releaseBinder and
        // decrement mRefCnt again.
        val originalContext = coroutineContext
        withContext(backgroundCoroutineDispatcher) {
            activeCoroutines++

            try {
                if (activeCoroutines == 1) {
                    cancelBinderRelease()
                }

                val binder = getBinder()

                withContext(originalContext) {
                    fn(binder)
                }
            } finally {
                activeCoroutines--
                if (activeCoroutines == 0) {
                    scheduleBinderRelease()
                }
            }
        }
    }

    /**
     * Run cleanup tasks. This should be called when the given coroutine scope is being
     * destroyed, as that will also cancel the cleanup task.
     */
    suspend fun cleanUp() {
        withContext(backgroundCoroutineDispatcher) {
            disconnectFromService()
        }
    }

    /**
     * Get an instance of TileProvider's binder. Note that this **must** be called from a context
     * which uses mDispatcher as its dispatcher.
     */
    private suspend fun getBinder(): TileProvider {
        val cachedTileProvider = tileProvider
        val cachedConnectBinderJob = connectBinderJob

        return when {
            cachedTileProvider?.asBinder()?.isBinderAlive == true -> cachedTileProvider
            cachedConnectBinderJob != null -> cachedConnectBinderJob.await()
            else -> connectToService()
        }
    }

    private suspend fun connectToService(): TileProvider = coroutineScope {
        val bindJob = async(backgroundCoroutineDispatcher) {
            suspendCancellableCoroutine<TileProvider> { continuation ->
                val bindIntent = Intent(TileProviderService.ACTION_BIND_TILE_PROVIDER)
                bindIntent.component = componentName

                val myConnection = object : ServiceConnection {
                    override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                        val boundTileProvider = TileProvider.Stub.asInterface(service)
                        continuation.resume(boundTileProvider)
                    }

                    override fun onServiceDisconnected(name: ComponentName?) {
                        // This is called when the remote side hangs up, but will be dispatched
                        // from an unknown thread. Ignore it for now.
                    }
                }

                connection = myConnection
                context.bindService(bindIntent, myConnection, Context.BIND_AUTO_CREATE)

                continuation.invokeOnCancellation {
                    disconnectFromService()
                }
            }
        }

        connectBinderJob = bindJob

        withTimeout(BIND_TIMEOUT_MILLIS) {
            val tp = bindJob.await()

            connectBinderJob = null
            tileProvider = tp

            tp
        }
    }

    private fun disconnectFromService() {
        connection?.let(context::unbindService)
        connection = null
        connectBinderJob = null
        tileProvider = null
    }

    private fun scheduleBinderRelease() {
        releaseBinderJob = coroutineScope.launch(backgroundCoroutineDispatcher) {
            delay(INACTIVITY_TIMEOUT_MILLIS)

            // Only run the unbind if we didn't get cancelled.
            if (isActive) {
                disconnectFromService()
            }
        }
    }

    private suspend fun cancelBinderRelease() {
        releaseBinderJob?.cancelAndJoin()
    }
}