/*
 * Copyright 2020 The Android Open Source Project
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
import android.content.res.Resources;
import android.content.res.TypedArray;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;


/**
 * Class containing helpers for querying wearable navigation pattern
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public final class WearableNavigationHelper {

    private static final String ITEM_NAME = "config_windowSwipeToDismiss";
    private static final String ITEM_TYPE = "bool";
    private static final String PACKAGE_NAME = "android";

    private WearableNavigationHelper() {
    }

    /**
     * Query whether swipe-to-dismiss is enabled on current device.
     */
    public static boolean isSwipeToDismissEnabled() {
        final Resources res = Resources.getSystem(); // system resources
        final int identifier = res.getIdentifier(ITEM_NAME, ITEM_TYPE, PACKAGE_NAME);
        if (identifier != 0) {
            return res.getBoolean(identifier);
        }
        return false;
    }

    /**
     * Query whether swipe-to-dismiss is enabled in current context.
     */
    @SuppressWarnings("deprecation")
    public static boolean isSwipeToDismissEnabled(@NonNull Context context) {
        TypedArray windowAttr =
                context.obtainStyledAttributes(new int[]{android.R.attr.windowSwipeToDismiss});
        boolean enabled = false;
        if (windowAttr.getIndexCount() > 0) {
            enabled = windowAttr.getBoolean(0, true);
        }
        windowAttr.recycle();
        return enabled;
    }
}
