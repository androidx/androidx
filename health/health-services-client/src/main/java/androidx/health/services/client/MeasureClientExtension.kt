/*
 * Copyright 2022 The Android Open Source Project
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

import androidx.health.services.client.data.DeltaDataType
import androidx.health.services.client.data.MeasureCapabilities

/**
 * Unregisters the given [MeasureCallback] for updates of the given [DeltaDataType].
 *
 * @param dataType the [DeltaDataType] that needs to be unregistered
 * @param callback the [MeasureCallback] which was used in registration
 * @throws HealthServicesException if Health Service fails to process the call
 */
@Suppress("PairedRegistration")
@kotlin.jvm.Throws(HealthServicesException::class)
public suspend fun MeasureClient.unregisterMeasureCallback(
    dataType: DeltaDataType<*, *>,
    callback: MeasureCallback
) = unregisterMeasureCallbackAsync(dataType, callback).awaitWithException()

/**
 * Returns the [MeasureCapabilities] of this client for the device.
 *
 * This can be used to determine what [DeltaDataType]s this device supports for live measurement.
 * Clients should use the capabilities to inform their requests since Health Services will typically
 * reject requests made for [DeltaDataType]s which are not enabled for measurement.
 *
 * @return a [MeasureCapabilities] for this device
 * @throws HealthServicesException if Health Service fails to process the call
 */
@kotlin.jvm.Throws(HealthServicesException::class)
public suspend fun MeasureClient.getCapabilities() = getCapabilitiesAsync().awaitWithException()
