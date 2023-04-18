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

/**
 * Interface to make ipc calls for health services api.
 *
 * @hide
 */
interface IHealthServicesApiService {
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
}
