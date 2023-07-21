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

package androidx.wear.utils;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;

/**
 * Helper class for determining whether the given Wear OS device is for China or rest of the world.
 */
public final class WearTypeHelper {
    @VisibleForTesting
    static final String CHINA_SYSTEM_FEATURE = "cn.google";

    /**
     * Returns whether the given device is running a China build.
     *
     * This can be used together with
     * {@code androidx.wear.phone.interactions.PhoneTypeHelper} to
     * decide what Uri should be used when opening Play Store on connected phone.
     *
     * @return True if device is running a China build and false if it is running the rest of the
     * world build.
     */
    public static boolean isChinaBuild(@NonNull Context context) {
        return context.getPackageManager().hasSystemFeature(CHINA_SYSTEM_FEATURE);
    }

    private WearTypeHelper() {}
}
