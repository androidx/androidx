/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.core.content.pm;

import android.content.pm.PackageInfo;

import androidx.annotation.NonNull;
import androidx.core.os.BuildCompat;

/** Helper for accessing features in {@link PackageInfo}. */
public final class PackageInfoCompat {
    /**
     * Return {@link android.R.attr#versionCode} and {@link android.R.attr#versionCodeMajor}
     * combined together as a single long value. The {@code versionCodeMajor} is placed in the
     * upper 32 bits on Android P or newer, otherwise these bits are all set to 0.
     *
     * @see PackageInfo#getLongVersionCode()
     */
    public static long getLongVersionCode(@NonNull PackageInfo info) {
        if (BuildCompat.isAtLeastP()) {
            return info.getLongVersionCode();
        }

        return info.versionCode;
    }

    private PackageInfoCompat() {
    }
}
