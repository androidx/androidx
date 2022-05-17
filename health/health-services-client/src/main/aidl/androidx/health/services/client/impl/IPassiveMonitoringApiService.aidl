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

import androidx.health.services.client.impl.IPassiveListenerCallback;
import androidx.health.services.client.impl.internal.IStatusCallback;
import androidx.health.services.client.impl.request.CapabilitiesRequest;
import androidx.health.services.client.impl.request.FlushRequest;
import androidx.health.services.client.impl.request.PassiveListenerCallbackRegistrationRequest;
import androidx.health.services.client.impl.request.PassiveListenerServiceRegistrationRequest;
import androidx.health.services.client.impl.response.PassiveMonitoringCapabilitiesResponse;

/** @hide */
interface IPassiveMonitoringApiService {
    /**
     * API version of the AIDL interface. Should be incremented every time a new
     * method is added.
     */
    const int API_VERSION = 4;

    /**
     * Returns version of this AIDL interface.
     *
     * <p> Can be used by client to detect version of the API on the service
     * side. Returned version should be always > 0.
     */
    int getApiVersion() = 0;

    /** Method to get capabilities. */
    PassiveMonitoringCapabilitiesResponse getCapabilities(in CapabilitiesRequest request) = 5;

    /** Method to flush data metrics. */
    void flush(in FlushRequest request, in IStatusCallback statusCallback) = 6;

    /**
     * Method to subscribe to updates via the passive listener service.
     *
     * This call was added in API_VERSION = 4.
     */
    void registerPassiveListenerService(in PassiveListenerServiceRegistrationRequest request, in IStatusCallback statusCallback) = 10;

    /**
     * Method to subscribe to updates via the passive listener callback.
     *
     * This call was added in API_VERSION = 4.
     */
    void registerPassiveListenerCallback(in PassiveListenerCallbackRegistrationRequest request, in IPassiveListenerCallback callback, in IStatusCallback statusCallback) = 11;

    /**
     * Method to unsubscribe from data updates via the passive listener service.
     *
     * This call was added in API_VERSION = 4.
     */
    void unregisterPassiveListenerService(in String packageName, in IStatusCallback statusCallback) = 12;

    /**
     * Method to unsubscribe from data updates via the passive listener callback.
     *
     * This call was added in API_VERSION = 4.
     */
    void unregisterPassiveListenerCallback(in String packageName, in IStatusCallback statusCallback) = 13;
}
