/*
 * Copyright (C) 2022 The Android Open Source Project
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

package androidx.core.uwb

import android.content.Context
import androidx.core.uwb.impl.UwbManagerImpl

@JvmDefaultWithCompatibility
public
/**
 * Interface for getting UWB capabilities and interacting with nearby UWB devices to perform
 * ranging.
 */
interface UwbManager {
    public companion object {

        /** Creates a new UwbManager that is used for creating UWB client sessions. */
        @JvmStatic
        public fun createInstance(context: Context): UwbManager {
            return UwbManagerImpl(context)
        }
    }

    /**
     * @return a new [UwbClientSessionScope] that tracks the lifecycle of a UWB connection.
     * @throws [androidx.core.uwb.exceptions.UwbServiceNotAvailableException] if the UWB is turned
     *   off.
     * @throws [androidx.core.uwb.exceptions.UwbHardwareNotAvailableException] if the hardware is
     *   not available on the device.
     */
    @Deprecated("Renamed to controleeSessionScope")
    public suspend fun clientSessionScope(): UwbClientSessionScope

    /**
     * @return a new [UwbControleeSessionScope] that tracks the lifecycle of a UWB connection.
     * @throws [androidx.core.uwb.exceptions.UwbServiceNotAvailableException] if the UWB is turned
     *   off.
     * @throws [androidx.core.uwb.exceptions.UwbHardwareNotAvailableException] if the hardware is
     *   not available on the device.
     */
    public suspend fun controleeSessionScope(): UwbControleeSessionScope

    /**
     * @return a new [UwbControllerSessionScope] that tracks the lifecycle of a UWB connection.
     * @throws [androidx.core.uwb.exceptions.UwbServiceNotAvailableException] if the UWB is turned
     *   off.
     * @throws [androidx.core.uwb.exceptions.UwbHardwareNotAvailableException] if the hardware is
     *   not available on the device.
     */
    public suspend fun controllerSessionScope(): UwbControllerSessionScope
}
