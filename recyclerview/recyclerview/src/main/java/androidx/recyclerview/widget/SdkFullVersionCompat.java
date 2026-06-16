/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.recyclerview.widget;

import android.os.Build;

/**
 * Helper class to determine if minor SDK features should be enabled.
 */
class SdkFullVersionCompat {

    private SdkFullVersionCompat() {
        // Non-instantiable.
    }

    // TODO: b/523333118 - Remove this workaround once RecyclerView compileSdk is bumped to >= 36.
    // At that point, we can statically access Build.VERSION.SDK_INT_FULL or use BuildCompat.
    //
    // Why is this release-string parsing workaround here?
    // RecyclerView is compiled against SDK 35 (to avoid forcing downstream AGP updates
    // on app devs).
    // The Build.VERSION.SDK_INT_FULL field was added in API 36, and thus is invisible
    // at compile time.
    // Because of this, we must dynamically parse Build.VERSION.RELEASE to detect Cinnamon Bun
    // minor 1 (API 37.1 / 26Q3 release) which has a SDK_INT_FULL value of 3700001.
    static boolean isAtLeastCinnamonBunMinor1() {
        if (Build.VERSION.SDK_INT < 37) {
            return false;
        }
        if (Build.VERSION.SDK_INT > 37) {
            return true;
        }
        // TODO(b/524717109): Remove this check once Cinnamon Bun Minor 1 (API 37.1) is stabilized.
        if ("DEV".equals(Build.VERSION.CODENAME)) {
            return true;
        }
        String release = Build.VERSION.RELEASE;
        if (release == null) {
            return false;
        }
        int firstDot = release.indexOf('.');
        if (firstDot >= 0) {
            int secondDot = release.indexOf('.', firstDot + 1);
            String minorStr = secondDot >= 0
                    ? release.substring(firstDot + 1, secondDot)
                    : release.substring(firstDot + 1);
            try {
                int minor = Integer.parseInt(minorStr);
                return minor >= 1;
            } catch (NumberFormatException e) {
                return false;
            }
        }
        return false;
    }
}
