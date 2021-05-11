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

import android.app.PendingIntent;
import androidx.health.services.client.impl.IPassiveMonitoringCallback;
import androidx.health.services.client.impl.internal.IStatusCallback;
import androidx.health.services.client.impl.request.BackgroundRegistrationRequest;
import androidx.health.services.client.impl.request.CapabilitiesRequest;
import androidx.health.services.client.impl.request.EventRequest;
import androidx.health.services.client.impl.response.PassiveMonitoringCapabilitiesResponse;

/** @hide */
interface IPassiveMonitoringApiService {
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
     * Method to subscribe to an event with corresponding callback intent.
     */
    void registerEventCallback(in EventRequest request, in PendingIntent intent, in IStatusCallback statusCallback) = 1;

    /**
     * Method to subscribe to a set of data types with corresponding callback
     * intent and an optional callback.
     *
     * <p>If a callback is present and is active, updates are provided via the callback. Otherwise,
     * the provided PendingIntent gets the updates.
     */
    void registerDataCallback(in BackgroundRegistrationRequest request, in PendingIntent fallbackIntent, in IPassiveMonitoringCallback callback, in IStatusCallback statusCallback) = 2;

    /**
     * Method to subscribe to a set of data types with corresponding callback intent.
     */
    void unregisterDataCallback(in String packageName, in IStatusCallback statusCallback) = 3;

    /**
     * Method to subscribe to a set of data types with corresponding callback intent.
     */
    void unregisterEventCallback(in EventRequest request, in IStatusCallback statusCallback) = 4;

    /** Method to get capabilities. */
    PassiveMonitoringCapabilitiesResponse getCapabilities(in CapabilitiesRequest request) = 5;
}
