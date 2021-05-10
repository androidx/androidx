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

package androidx.health.services.client.impl.ipc;

import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;

/**
 * An interface that provides basic information about the IPC service. This is required for building
 * the service in {@link Client}.
 *
 * @hide
 */
@RestrictTo(Scope.LIBRARY)
public class ClientConfiguration {
    private final String mServicePackageName;
    private final String mBindAction;
    private final String mApiClientName;

    public ClientConfiguration(String apiClientName, String servicePackageName, String bindAction) {
        this.mServicePackageName = servicePackageName;
        this.mBindAction = bindAction;
        this.mApiClientName = apiClientName;
    }

    /** Returns the application package of the remote service. */
    public String getServicePackageName() {
        return mServicePackageName;
    }

    /** Returns the action used to bind to the remote service. */
    public String getBindAction() {
        return mBindAction;
    }

    /** Returns name of the service, use for logging and debugging only. */
    public String getApiClientName() {
        return mApiClientName;
    }
}
