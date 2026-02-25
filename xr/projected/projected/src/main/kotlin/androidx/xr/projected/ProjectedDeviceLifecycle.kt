/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.xr.projected

import android.content.Context
import android.os.Bundle
import android.os.RemoteException
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.xr.projected.binding.ProjectedServiceConnection
import androidx.xr.projected.platform.IProjectedDeviceStateListener
import androidx.xr.projected.platform.IProjectedService
import androidx.xr.projected.platform.ProjectedDeviceState
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** Class representing lifecycle of a Projected device. */
internal class ProjectedDeviceLifecycle(
    provider: LifecycleOwner,
    private val context: Context,
    private val serviceConnectionCoroutineContext: CoroutineContext,
) : Lifecycle() {

    private val registry = LifecycleRegistry(provider)
    private val mainDispatcherCoroutineScope = CoroutineScope(Dispatchers.Main)
    private val serviceConnectionCoroutineScope = CoroutineScope(serviceConnectionCoroutineContext)
    private var serviceConnection: ProjectedServiceConnection? = null
    private var projectedService: IProjectedService? = null

    private val projectedDeviceStateListener =
        object : IProjectedDeviceStateListener.Stub() {
            override fun onProjectedDeviceStateChanged(deviceState: Int, data: Bundle?) {
                when (deviceState) {
                    ProjectedDeviceState.ACTIVE -> handleLifecycleEventOnMainThread(Event.ON_START)
                    ProjectedDeviceState.INACTIVE -> handleLifecycleEventOnMainThread(Event.ON_STOP)
                    ProjectedDeviceState.DESTROYED -> {
                        if (registry.currentState.isAtLeast(State.CREATED)) {
                            handleLifecycleEventOnMainThread(Event.ON_DESTROY)
                        }
                        projectedService?.unregisterProjectedDeviceStateListener(this)
                        serviceConnection?.disconnect()
                    }
                }
            }

            override fun getInterfaceVersion(): Int = VERSION
        }

    /**
     * Currently the following states are supported:
     * * [Lifecycle.State.INITIALIZED] - This state indicates the Projected device lifecycle object
     *   was created but the lifecycle is not observed.
     * * [Lifecycle.State.CREATED] - This state indicates the connection to the Projected service
     *   was established after a [LifecycleObserver] was added. This state indicates the Projected
     *   device is inactive.
     * * [Lifecycle.State.STARTED] - This state indicates the Projected device is active.
     * * [Lifecycle.State.DESTROYED] - This state indicates the Projected service disconnected
     *   unexpectedly. To retry connection and start listening to the Projected device lifecycle
     *   again, a new [LifecycleObserver] needs to be added.
     */
    override val currentState: State
        get() = registry.currentState

    override fun addObserver(observer: LifecycleObserver) {
        mainDispatcherCoroutineScope.launch {
            val notInitialized = registry.observerCount == 0
            registry.addObserver(observer)
            if (notInitialized) {
                launch(serviceConnectionCoroutineContext) { initialize() }
            }
        }
    }

    override fun removeObserver(observer: LifecycleObserver) {
        mainDispatcherCoroutineScope.launch {
            registry.removeObserver(observer)
            if (registry.observerCount == 0) {
                cleanup()
            }
        }
    }

    private suspend fun initialize() {
        serviceConnection =
            ProjectedServiceConnection(
                context,
                ProjectedServiceConnection.ProjectedIntentAction.ACTION_BIND,
            )
        serviceConnectionCoroutineScope.launch {
            serviceConnection?.isServiceConnected?.collect { isConnected ->
                if (isConnected) {
                    handleLifecycleEventOnMainThread(Event.ON_CREATE)
                } else {
                    if (registry.currentState.isAtLeast(State.CREATED)) {
                        handleLifecycleEventOnMainThread(Event.ON_DESTROY)
                    }
                }
            }
        }

        try {
            projectedService = serviceConnection?.connect()
            projectedService?.registerProjectedDeviceStateListener(projectedDeviceStateListener)
        } catch (_: IllegalStateException) {
            // TODO(b/467064995) - Implement a proper error handling mechanism.
        }
    }

    private fun cleanup() {
        try {
            projectedService?.unregisterProjectedDeviceStateListener(projectedDeviceStateListener)
            serviceConnection?.disconnect()
            registry.currentState = State.INITIALIZED
        } catch (_: RemoteException) {
            // TODO(b/467064995) - Implement a proper error handling mechanism.
        }
    }

    private fun handleLifecycleEventOnMainThread(event: Event) {
        mainDispatcherCoroutineScope.launch { registry.handleLifecycleEvent(event) }
    }
}
