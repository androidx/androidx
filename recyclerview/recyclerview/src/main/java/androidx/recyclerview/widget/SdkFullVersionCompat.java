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

import androidx.annotation.DoNotInline;
import androidx.annotation.RequiresApi;

/** Helper class to determine if minor SDK features should be enabled. */
class SdkFullVersionCompat {

    private SdkFullVersionCompat() {
        // Non-instantiable.
    }

    static boolean isAtLeastCinnamonBunMinor1() {
        // Build.VERSION_CODES_FULL.CINNAMON_BUN_1 is not available in the SDK 36 compileSdk.
        // We use the literal integer 3700001 to represent Cinnamon Bun Minor 1 checks.
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA
                && Api36Impl.getSdkIntFull() >= 3700001;
    }

    @RequiresApi(36)
    static class Api36Impl {
        private Api36Impl() {
            // Non-instantiable.
        }

        @DoNotInline
        static int getSdkIntFull() {
            return Build.VERSION.SDK_INT_FULL;
        }
    }
}
