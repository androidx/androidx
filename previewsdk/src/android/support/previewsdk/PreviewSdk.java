/*
 * Copyright (C) 2015 The Android Open Source Project
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

package android.support.previewsdk;

import android.os.Build;

/**
 * Utility class for performing version checks on Android platform preview SDKs.
 *
 * <p>Apps must be very careful when targeting preview builds because binary compatibility
 * is not guaranteed. APIs can be renamed or drastically changed before they are finalized
 * into a new API level. The new SDK constant <code>Build.VERSION.PREVIEW_SDK_INT</code>
 * marks a precise snapshot version of prerelease API.</p>
 *
 * <p>{@link #isKnownPreviewDevice()} will return <code>true</code> if the current device
 * is running a preview build with the same SDK snapshot this support lib was built with.
 * If it returns <code>true</code> it is safe to call prerelease APIs. If not, the app
 * should fall back to only assuming the presence of the latest public, final API level.</p>
 */
public class PreviewSdk {
    /**
     * Check if the current device is running a prerelease platform preview build matching
     * the SDK this library was built for. If it returns true, it is safe to call prerelease
     * APIs known to this SDK.
     *
     * @return true if the device is running a preview build that matches the SDK.
     */
    public static boolean isKnownPreviewDevice() {
        return "MNC".equals(Build.VERSION.CODENAME)
                && Build.VERSION.PREVIEW_SDK_INT == PreviewConstants.PREVIEW_SDK_VERSION;
    }
}
