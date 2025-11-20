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
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.xr.projected.experimental.ExperimentalProjectedApi

/**
 * Controller for the Projected device.
 *
 * Use [create] to create an instance of this class.
 */
@ExperimentalProjectedApi
public class ProjectedDeviceController private constructor(capabilitiesParam: Set<Capability>) {

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

    public companion object {
        /**
         * Connects to the service providing features for Projected devices and returns the
         * [ProjectedDisplayController] when the connection is established.
         *
         * @param context The context to use for binding to the service.
         * @throws IllegalStateException if the projected service is not found or binding is not
         *   permitted
         */
        @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
        @JvmStatic
        public suspend fun create(context: Context): ProjectedDeviceController {
            val serviceConnection = ProjectedServiceConnection(context)
            val projectedService = serviceConnection.connect()
            val capabilities =
                if (projectedService.isDisplayCapable()) setOf(Capability.CAPABILITY_VISUAL_UI)
                else setOf()
            serviceConnection.disconnect()

            return ProjectedDeviceController(capabilities)
        }
    }
}
