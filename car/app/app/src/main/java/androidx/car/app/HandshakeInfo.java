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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;

/**
 * A container for the information conveyed by the host after the handshake with the app is
 * completed.
 *
 * @hide
 */
@RestrictTo(Scope.LIBRARY)
public class HandshakeInfo {
    @Nullable
    private final String mHostPackageName;

    public HandshakeInfo(@NonNull String hostPackageName) {
        this.mHostPackageName = hostPackageName;
    }

    // Used for serialization
    public HandshakeInfo() {
        mHostPackageName = null;
    }

    @NonNull
    public String getHostPackageName() {
        return requireNonNull(mHostPackageName);
    }
}
