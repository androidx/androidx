/*
 * Copyright (C) 2014 The Android Open Source Project
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

package android.support.v4.view;

import android.graphics.Rect;
import android.view.WindowInsets;

class WindowInsetsCompatApi21 {
    public static Object consumeStableInsets(Object insets) {
        return ((WindowInsets) insets).consumeStableInsets();
    }

    public static int getStableInsetBottom(Object insets) {
        return ((WindowInsets) insets).getStableInsetBottom();
    }

    public static int getStableInsetLeft(Object insets) {
        return ((WindowInsets) insets).getStableInsetLeft();
    }

    public static int getStableInsetRight(Object insets) {
        return ((WindowInsets) insets).getStableInsetRight();
    }

    public static int getStableInsetTop(Object insets) {
        return ((WindowInsets) insets).getStableInsetTop();
    }

    public static boolean hasStableInsets(Object insets) {
        return ((WindowInsets) insets).hasStableInsets();
    }

    public static boolean isConsumed(Object insets) {
        return ((WindowInsets) insets).isConsumed();
    }

    public static Object replaceSystemWindowInsets(Object insets, Rect systemWindowInsets) {
        return ((WindowInsets) insets).replaceSystemWindowInsets(systemWindowInsets);
    }
}
