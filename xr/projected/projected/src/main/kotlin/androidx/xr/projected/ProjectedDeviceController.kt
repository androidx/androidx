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

// ConcurrentHashMap is safe for our minSdk 24. The lint check guards against a Lollipop bug.
@file:Suppress("BanConcurrentHashMap")

package androidx.xr.projected

import android.content.Context
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import android.os.RemoteException
import androidx.annotation.RequiresApi
import androidx.xr.projected.ProjectedDeviceController.Companion.create
import androidx.xr.projected.binding.ProjectedServiceConnection
import androidx.xr.projected.binding.ProjectedServiceConnection.ProjectedIntentAction.Companion.ACTION_BIND
import androidx.xr.projected.experimental.ExperimentalProjectedApi
import androidx.xr.projected.platform.BatteryState as AidlBatteryState
import androidx.xr.projected.platform.IBatteryStateListener
import androidx.xr.projected.platform.IProjectedService
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.ContinuationInterceptor
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Controller for the Projected device.
 *
 * Use [create] to create an instance of this class. Call [close] when finished with this instance
 * to release resources.
 */
@ExperimentalProjectedApi
public class ProjectedDeviceController
private constructor(
    private val context: Context,
    private val connection: ProjectedServiceConnection,
    private val projectedService: IProjectedService,
    capabilitiesParam: Set<Capability>,
    private val coroutineContext: CoroutineContext,
) : AutoCloseable {

    /**
     * Represents an intrinsic piece of functionality of a Projected device, i.e., what it is
     * capable of.
     */
    public class Capability private constructor(private val id: Int) {
        override fun toString(): String =
            when (id) {
                0 -> "CAPABILITY_VISUAL_UI"
                else -> "UNKNOWN ($id)"
            }

        override fun equals(other: Any?): Boolean = (other is Capability) && this.id == other.id

        override fun hashCode(): Int = id.hashCode()

        public companion object {
            /**
             * Indicates that the Projected device is capable of showing a visual user interface,
             * i.e., that it has a screen.
             */
            @JvmField public val CAPABILITY_VISUAL_UI: Capability = Capability(0)
        }
    }

    /**
     * The capabilities of the Projected device.
     *
     * These capabilities represent intrinsic functionality of the device that will not change over
     * the lifetime of the device, for example, whether the device is capable of showing a visual
     * user interface at all, regardless of whether the screen is currently on or off.
     */
    public val capabilities: Set<Capability> = capabilitiesParam

    /** Returns the list of [AudioDeviceInfo] objects associated with the projected device. */
    public val audioDevices: List<AudioDeviceInfo>
        get() = getAudioDevicesInternal()

    private val batteryStateListeners =
        ConcurrentHashMap<(BatteryState) -> Unit, IBatteryStateListener>()

    // Runs listeners in the context passed to create. Its SupervisorJob replaces any Job passed
    // in, so listeners stop only when they're removed or close() is called.
    private val coroutineScope = CoroutineScope(coroutineContext + SupervisorJob())

    // Without a dispatcher, listeners run directly on the thread that delivers each update.
    private val listenerStart =
        if (coroutineContext[ContinuationInterceptor] == null) CoroutineStart.UNDISPATCHED
        else CoroutineStart.DEFAULT

    // TODO(b/578116054): Remove once all clients have been migrated.
    /**
     * Adds a listener for battery state changes.
     *
     * The [listener] lambda will be executed in a new coroutine launched within the provided
     * [context]. The listener will be automatically unregistered when the [context]'s [Job] is
     * canceled.
     */
    @Deprecated("Use addBatteryStateChangedListener(listener) and pass the context to create().")
    public fun addBatteryStateChangedListener(
        context: CoroutineContext,
        listener: (BatteryState) -> Unit,
    ) {
        val aidlListener =
            object : IBatteryStateListener.Stub() {
                override fun onBatteryStateChanged(batteryState: AidlBatteryState) {
                    // Launch in the provided CoroutineContext
                    CoroutineScope(context).launch {
                        listener(BatteryState(batteryState.isCharging, batteryState.batteryLevel))
                    }
                }

                override fun getInterfaceVersion() = VERSION
            }

        projectedService.registerBatteryStateListener(aidlListener)
        // Add to map only after successful registration
        batteryStateListeners[listener] = aidlListener
        // Unregister when the scope is canceled
        context[Job]?.invokeOnCompletion { removeBatteryStateChangedListener(listener) }
    }

    /**
     * Adds a listener for battery state changes of the Projected device.
     *
     * The [listener] is invoked with the current battery state right after it's added, and then
     * whenever the battery state changes. It runs on the dispatcher of the `coroutineContext`
     * passed to [create], or on the thread that delivers the update if that context has no
     * dispatcher. Adding a listener that is already registered has no effect.
     *
     * @param listener listener that receives [BatteryState] updates
     * @throws IllegalStateException if the Projected service is not running
     */
    public fun addBatteryStateChangedListener(listener: (BatteryState) -> Unit) {
        val aidlListener =
            object : IBatteryStateListener.Stub() {
                override fun onBatteryStateChanged(batteryState: AidlBatteryState) {
                    val state = BatteryState(batteryState.isCharging, batteryState.batteryLevel)
                    val stub = this
                    coroutineScope.launch(start = listenerStart) {
                        // Skip updates that were still queued when the listener was removed.
                        if (batteryStateListeners[listener] === stub) listener(state)
                    }
                }

                override fun getInterfaceVersion() = VERSION
            }

        // Add the listener before registering, since the service sends the current state during
        // registration and the listener may remove itself when it receives it.
        if (batteryStateListeners.putIfAbsent(listener, aidlListener) != null) return
        try {
            projectedService.registerBatteryStateListener(aidlListener)
        } catch (e: RemoteException) {
            batteryStateListeners.remove(listener, aidlListener)
            throw IllegalStateException("Projected service is not running.", e)
        } catch (e: Throwable) {
            batteryStateListeners.remove(listener, aidlListener)
            throw e
        }
        // If the listener was removed during registration, make sure the service drops it too.
        if (batteryStateListeners[listener] !== aidlListener) unregister(aidlListener)
    }

    /**
     * Removes a listener previously added with [addBatteryStateChangedListener]. If the listener
     * has already been removed or was never added, then this is a no-op.
     *
     * @param listener listener previously passed to [addBatteryStateChangedListener]
     */
    public fun removeBatteryStateChangedListener(listener: (BatteryState) -> Unit) {
        batteryStateListeners.remove(listener)?.let { aidlListener -> unregister(aidlListener) }
    }

    /**
     * Releases resources, unregistering any active listeners. This instance should not be used
     * after calling close.
     */
    override fun close() {
        batteryStateListeners.keys.forEach { removeBatteryStateChangedListener(it) }
        coroutineScope.cancel()

        connection.disconnect()
    }

    private fun unregister(aidlListener: IBatteryStateListener) {
        try {
            projectedService.unregisterBatteryStateListener(aidlListener)
        } catch (_: RemoteException) {
            // Ignore errors during unregistration
        }
    }

    private fun getAudioDevicesInternal(): List<AudioDeviceInfo> {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

        val allAudioDevices: Array<AudioDeviceInfo> =
            audioManager.getDevices(
                AudioManager.GET_DEVICES_OUTPUTS or AudioManager.GET_DEVICES_INPUTS
            )

        val projectedAudioDeviceIds = projectedService.audioDeviceIds

        return allAudioDevices
            .filter { deviceInfo -> deviceInfo.id in projectedAudioDeviceIds }
            .toList()
    }

    public companion object {
        /**
         * Connects to the service providing features for Projected devices and returns the
         * [ProjectedDeviceController] when the connection is established. The caller is responsible
         * for calling [close] on the returned instance when it's no longer needed to release all
         * internal resources.
         *
         * @param context The context to use for binding to the service.
         * @param coroutineContext context whose dispatcher runs the listeners of the returned
         *   instance (any [Job] in this context is ignored and does not unregister listeners);
         *   without a dispatcher, listeners run on the thread that delivers each update
         * @throws IllegalStateException if the projected service is not found or binding is not
         *   permitted
         */
        @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
        @JvmOverloads
        @JvmStatic
        public suspend fun create(
            context: Context,
            coroutineContext: CoroutineContext = EmptyCoroutineContext,
        ): ProjectedDeviceController {
            val serviceConnection = ProjectedServiceConnection(context, ACTION_BIND)
            val projectedService = serviceConnection.connect()
            val capabilities =
                try {
                    if (projectedService.isDisplayCapable()) setOf(Capability.CAPABILITY_VISUAL_UI)
                    else setOf()
                } catch (e: Exception) {
                    serviceConnection.disconnect()
                    throw e
                }

            return ProjectedDeviceController(
                context,
                serviceConnection,
                projectedService,
                capabilities,
                coroutineContext,
            )
        }
    }
}
