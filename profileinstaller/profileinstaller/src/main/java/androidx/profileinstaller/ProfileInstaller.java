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

package androidx.profileinstaller;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;

/**
 * Install ahead of time tracing profiles to configure ART to precompile bundled libraries.
 *
 * This will automatically be called by {@link ProfileInstallerInitializer} and you should never
 * call this unless you have disabled the initializer in your manifest.
 *
 * This reads profiles from the assets directory, where they must be embedded during the build
 * process. This will have no effect if there is not a profile embedded in the current APK.
 */
public class ProfileInstaller {
    // cannot construct
    private ProfileInstaller() {}

    /**
     * Try to install the profile from assets into the ART aot profile directory.
     *
     * This should always be called after the first screen is shown to the user, to avoid
     * delaying application startup to install AOT profiles.
     *
     * @param context context to read assets from
     */
    @WorkerThread
    public static void tryInstallProfile(@NonNull Context context) {
    }
}
