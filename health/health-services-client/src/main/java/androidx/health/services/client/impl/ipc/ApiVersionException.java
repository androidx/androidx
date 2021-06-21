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

import java.util.concurrent.ExecutionException;

/**
 * Exception that is thrown when API version requirements are not met.
 *
 * @hide
 */
@RestrictTo(Scope.LIBRARY)
public class ApiVersionException extends ExecutionException {

    private final int mRemoteVersion;
    private final int mMinVersion;

    public ApiVersionException(int remoteVersion, int minVersion) {
        super(
                "Version requirements for calling the method was not met, remoteVersion: "
                        + remoteVersion
                        + ", minVersion: "
                        + minVersion);
        this.mRemoteVersion = remoteVersion;
        this.mMinVersion = minVersion;
    }

    public int getRemoteVersion() {
        return mRemoteVersion;
    }

    public int getMinVersion() {
        return mMinVersion;
    }
}
