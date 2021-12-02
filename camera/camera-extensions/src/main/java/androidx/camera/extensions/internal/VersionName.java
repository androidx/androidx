/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.camera.extensions.internal;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

/**
 * The version of CameraX extension releases.
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public class VersionName {
    // Current version of vendor library implementation that the CameraX extension supports. This
    // needs to be increased along with the version of vendor library interface.
    private static final VersionName CURRENT = new VersionName("1.1.0");

    @NonNull
    public static VersionName getCurrentVersion() {
        return CURRENT;
    }

    private final Version mVersion;

    @NonNull
    public Version getVersion() {
        return mVersion;
    }

    public VersionName(@NonNull String versionString) {
        mVersion = Version.parse(versionString);
    }

    VersionName(int major, int minor, int patch, String description) {
        mVersion = Version.create(major, minor, patch, description);
    }

    /**
     * Gets this version number as string.
     *
     * @return the string of the version in a form of MAJOR.MINOR.PATCH-description.
     */
    @NonNull
    public String toVersionString() {
        return mVersion.toString();
    }
}
