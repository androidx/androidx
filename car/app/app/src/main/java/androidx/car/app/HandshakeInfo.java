/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.car.app;

import static java.util.Objects.requireNonNull;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.car.app.annotations.CarProtocol;

/**
 * A container for the information conveyed by the host after the handshake with the app is
 * completed.
 */
@CarProtocol
public final class HandshakeInfo {
    @Keep
    @Nullable
    private final String mHostPackageName;
    @Keep
    private final int mHostCarAppApiLevel;

    /**
     * Creates an instance of {@link HandshakeInfo}.
     *
     * @param hostPackageName    the host package name
     * @param hostCarAppApiLevel the API level that should be used to communicate with the host
     */
    public HandshakeInfo(@NonNull String hostPackageName, int hostCarAppApiLevel) {
        mHostPackageName = hostPackageName;
        mHostCarAppApiLevel = hostCarAppApiLevel;
    }

    // Used for serialization
    private HandshakeInfo() {
        mHostPackageName = null;
        mHostCarAppApiLevel = 0;
    }

    /**
     * Returns the host package name.
     */
    @NonNull
    public String getHostPackageName() {
        return requireNonNull(mHostPackageName);
    }

    /**
     * Returns the negotiated API level that should be used to communicate with the host.
     */
    public int getHostCarAppApiLevel() {
        return mHostCarAppApiLevel;
    }
}
