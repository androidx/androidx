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

package androidx.health.services.client

import androidx.health.services.client.data.Availability
import androidx.health.services.client.data.DataPoint
import androidx.health.services.client.data.DataPointContainer
import androidx.health.services.client.data.DataType
import androidx.health.services.client.data.DeltaDataType

/** Callback for [MeasureClient.registerMeasureCallback]. */
public interface MeasureCallback {

    /** Called when this callback has been successfully registered with Health Services. */
    public fun onRegistered() {}

    /**
     * Called when Health Services reports a failure with the registration of this callback. Common
     * causes include: the calling app lacks the necessary permission, or the device does not
     * support the requested [DataType].
     *
     * @param throwable a throwable sent by Health Services with information about the failure
     */
    public fun onRegistrationFailed(throwable: Throwable) {}

    /**
     * Called when the availability of a [DataType] changes.
     *
     * @param dataType the [DeltaDataType] that experienced a change in availability
     * @param availability the new [Availability] status for this [dataType]
     */
    public fun onAvailabilityChanged(dataType: DeltaDataType<*, *>, availability: Availability)

    /**
     * Called when new data is available. Data may be batched.
     *
     * @param data the (potentially batched) set of measured [DataPoint]s corresponding to one or
     * more of the requested [DeltaDataType]s
     */
    public fun onDataReceived(data: DataPointContainer)
}
