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
import androidx.annotation.VisibleForTesting;

/**
 * The client version of the Extensions-Interface that CameraX extension library uses.
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public class ClientVersion {
    // Current version of vendor library implementation that the CameraX extension supports. This
    // needs to be increased along with the version of vendor library interface.
    private static ClientVersion sCurrent = new ClientVersion("1.3.0");

    @NonNull
    public static ClientVersion getCurrentVersion() {
        return sCurrent;
    }

    /**
     * Overrides the client version for testing.
     */
    @VisibleForTesting
    public static void setCurrentVersion(@NonNull ClientVersion clientVersion) {
        sCurrent = clientVersion;
    }

    private final Version mVersion;

    @NonNull
    public Version getVersion() {
        return mVersion;
    }

    public ClientVersion(@NonNull String versionString) {
        mVersion = Version.parse(versionString);
    }

    /**
     * Check if the client version meets the minimum compatible version requirement. This implies
     * that the client version is equal to or newer than the version.
     *
     * <p> The compatible version is comprised of the major and minor version numbers. The patch
     * number is ignored.
     *
     * @param version The minimum compatible version required
     * @return True if the client version meets the minimum version requirement and False
     * otherwise.
     */
    public static boolean isMinimumCompatibleVersion(@NonNull Version version) {
        return ClientVersion.getCurrentVersion().mVersion
                .compareTo(version.getMajor(), version.getMinor()) >= 0;
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
