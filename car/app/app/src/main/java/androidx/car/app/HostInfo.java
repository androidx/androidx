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

import org.jspecify.annotations.NonNull;

/**
 * Container class for information about the host the app is connected to.
 *
 * <p>Apps can use this information to determine how they will respond to the host. For example, a
 * host which is not recognized could receive a message screen while an authorized host could
 * receive full access to the app's capabilities.
 *
 * <p>The package name and uid can used to query the system package manager for a signature or to
 * determine if the host has a system signature.
 *
 * <p>The host API level can be used to adjust the models exchanged with the host to those valid
 * for the specific host version the app is connected to.
 */
public final class HostInfo {
    private final @NonNull String mPackageName;
    private final int mUid;

    /**
     * Constructs an instance of the HostInfo from the required package name, UID and API level.
     */
    public HostInfo(@NonNull String packageName, int uid) {
        mPackageName = requireNonNull(packageName);
        mUid = uid;
    }

    /** Returns the package name of the host. */
    public @NonNull String getPackageName() {
        return mPackageName;
    }

    /** Returns the UID of the host. */
    public int getUid() {
        return mUid;
    }

    @Override
    public @NonNull String toString() {
        return mPackageName + ", uid: " + mUid;
    }
}
