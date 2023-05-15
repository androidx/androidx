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

package androidx.health.services.client.impl;

import androidx.health.services.client.impl.IMeasureCallback;
import androidx.health.services.client.impl.internal.IStatusCallback;
import androidx.health.services.client.impl.request.CapabilitiesRequest;
import androidx.health.services.client.impl.request.MeasureRegistrationRequest;
import androidx.health.services.client.impl.request.MeasureUnregistrationRequest;
import androidx.health.services.client.impl.response.MeasureCapabilitiesResponse;

/**
 * Interface to make ipc calls to query version information.
 *
 * @hide
 */
interface IVersionApiService {
    /**
     * API version of _this_ AIDL interface. Should be incremented every time a
     * new method is added.
     */
    const int VERSION_API_SERVICE_VERSION = 1;

    /**
     * Version of the SDK as a whole. Should be incremented on each release,
     * regardless of whether the API surface has changed.
     */
    const int CANONICAL_SDK_VERSION = 27;

    /**
     * Returns the version of _this_ AIDL interface.
     *
     * <p> Can be used by client to detect version of the API on the service
     * side. Returned version should be always > 0.
     */
    int getVersionApiServiceVersion() = 0;

    /**
     * Returns the version of the SDK as a whole.
     *
     * <p> Can be used by client to detect version of the SDK on the service
     * side. Returned version should be always > 0.
     */
    int getSdkVersion() = 1;
}
