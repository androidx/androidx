/*
 * Copyright (C) 2021 The Android Open Source Project
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

package androidx.health.services.client.data

import android.os.Parcel
import android.os.Parcelable

/**
 * A place holder class that represents the capabilities of the
 * [androidx.health.services.client.PassiveMonitoringClient] on the device.
 */
public data class PassiveMonitoringCapabilities(

    /**
     * Set of supported [DataType] s for background capture on this device.
     *
     * Some data types are only available during exercise (e.g. location) or for measurements.
     */
    val supportedDataTypesPassiveMonitoring: Set<DataType>,

    /** Set of supported [DataType] s for event callbacks on this device. */
    val supportedDataTypesEvents: Set<DataType>,
) : Parcelable {
    override fun describeContents(): Int = 0

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeTypedList(supportedDataTypesPassiveMonitoring.toList())
        dest.writeTypedList(supportedDataTypesEvents.toList())
    }

    public companion object {
        @JvmField
        public val CREATOR: Parcelable.Creator<PassiveMonitoringCapabilities> =
            object : Parcelable.Creator<PassiveMonitoringCapabilities> {
                override fun createFromParcel(source: Parcel): PassiveMonitoringCapabilities? {
                    val passiveMonitoringDataTypes = ArrayList<DataType>()
                    source.readTypedList(passiveMonitoringDataTypes, DataType.CREATOR)
                    val eventDataTypes = ArrayList<DataType>()
                    source.readTypedList(eventDataTypes, DataType.CREATOR)
                    return PassiveMonitoringCapabilities(
                        passiveMonitoringDataTypes.toSet(),
                        eventDataTypes.toSet()
                    )
                }

                override fun newArray(size: Int): Array<PassiveMonitoringCapabilities?> {
                    return arrayOfNulls(size)
                }
            }
    }
}
