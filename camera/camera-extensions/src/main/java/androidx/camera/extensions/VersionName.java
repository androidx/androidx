/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.camera.extensions;

import androidx.annotation.RestrictTo;

/**
 * The version of CameraX extension releases.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public enum VersionName {

    /* The current version of the CameraX extension. */
    CURRENT(BuildConfig.CAMERA_VERSION);

    private final Version mVersion;

    public Version getVersion() {
        return mVersion;
    }

    VersionName(String versionString) {
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
    public String toVersionString() {
        return mVersion.toString();
    }
}
