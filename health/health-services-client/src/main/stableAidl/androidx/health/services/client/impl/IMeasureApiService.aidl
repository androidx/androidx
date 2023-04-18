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

package androidx.health.services.client.impl;

import androidx.health.services.client.impl.IMeasureCallback;
import androidx.health.services.client.impl.internal.IStatusCallback;
import androidx.health.services.client.impl.request.CapabilitiesRequest;
import androidx.health.services.client.impl.request.MeasureRegistrationRequest;
import androidx.health.services.client.impl.request.MeasureUnregistrationRequest;
import androidx.health.services.client.impl.response.MeasureCapabilitiesResponse;

/**
 * Interface to make ipc calls for health services api.
 *
 * @hide
 */
interface IMeasureApiService {
    /**
     * API version of the AIDL interface. Should be incremented every time a new
     * method is added.
     */
    const int API_VERSION = 1;

    /**
     * Returns version of this AIDL interface.
     *
     * <p> Can be used by client to detect version of the API on the service
     * side. Returned version should be always > 0.
     */
    int getApiVersion() = 0;

    /**
     * Method to register measure listener.
     */
    void registerCallback(in MeasureRegistrationRequest request, in IMeasureCallback callback, in IStatusCallback statusCallback) = 1;

    /**
     * Method to unregister measure listener.
     */
    void unregisterCallback(in MeasureUnregistrationRequest request, in IMeasureCallback callback, in IStatusCallback statusCallback) = 2;

   /** Method to get capabilities. */
    MeasureCapabilitiesResponse getCapabilities(in CapabilitiesRequest request) = 3;
}
