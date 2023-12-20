/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.appsearch.utils;

import static androidx.annotation.RestrictTo.Scope.LIBRARY;

import android.content.Context;
import android.os.Build;
import android.provider.Settings;

import androidx.annotation.DoNotInline;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;

/**
 * Helper class for device boot count.
 *
 * @exportToFramework:hide
 */
@RestrictTo(LIBRARY)
public class BootCountUtil {
    private BootCountUtil() {}

    /**
     * Returns the current boot count of the device if available. Otherwise return -1.
     */
    public static int getCurrentBootCount(@NonNull Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            return Api17Impl.getCurrentBootCount(context);
        }

        return -1;
    }

    @RequiresApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    private static final class Api17Impl {
        @DoNotInline
        static int getCurrentBootCount(@NonNull Context context) {
            return Settings.Global.getInt(context.getContentResolver(),
                    Settings.Global.BOOT_COUNT, -1);
        }

        private Api17Impl() {}
    }
}
