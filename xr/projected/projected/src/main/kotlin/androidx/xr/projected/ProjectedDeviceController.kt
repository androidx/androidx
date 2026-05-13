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
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.xr.projected.ProjectedDeviceController.Companion.create
import androidx.xr.projected.binding.ProjectedServiceConnection
import androidx.xr.projected.binding.ProjectedServiceConnection.ProjectedIntentAction.Companion.ACTION_BIND
import androidx.xr.projected.experimental.ExperimentalProjectedApi
import androidx.xr.projected.platform.BatteryState as AidlBatteryState
import androidx.xr.projected.platform.IBatteryStateListener
import androidx.xr.projected.platform.IProjectedService
import java.util.Collections
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
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
        Collections.synchronizedMap(mutableMapOf<(BatteryState) -> Unit, IBatteryStateListener>())

    /**
     * Adds a listener for battery state changes.
     *
     * The [listener] lambda will be executed in a new coroutine launched within the provided
     * [context]. The listener will be automatically unregistered when the [context]'s [Job] is
     * canceled.
     */
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
     * Removes a previously added listener. Note: Listeners are also automatically removed when
     * their associated CoroutineScope is canceled.
     */
    public fun removeBatteryStateChangedListener(listener: (BatteryState) -> Unit) {
        batteryStateListeners.remove(listener)?.let { aidlListener ->
            try {
                projectedService.unregisterBatteryStateListener(aidlListener)
            } catch (_: Exception) {
                // Ignore errors during unregistration
            }
        }
    }

    /**
     * Releases resources, unregistering any active listeners. This instance should not be used
     * after calling close.
     */
    override fun close() {
        batteryStateListeners.keys.toList().forEach { removeBatteryStateChangedListener(it) }

        connection.disconnect()
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
         * @throws IllegalStateException if the projected service is not found or binding is not
         *   permitted
         */
        @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
        @JvmStatic
        public suspend fun create(context: Context): ProjectedDeviceController {
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
            )
        }
    }
}
