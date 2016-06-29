/*
 * Copyright (C) 2016 The Android Open Source Project
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

import android.view.WindowInsets;

class WindowInsetsCompatApi20 {
    public static Object consumeSystemWindowInsets(Object insets) {
        return ((WindowInsets) insets).consumeSystemWindowInsets();
    }

    public static int getSystemWindowInsetBottom(Object insets) {
        return ((WindowInsets) insets).getSystemWindowInsetBottom();
    }

    public static int getSystemWindowInsetLeft(Object insets) {
        return ((WindowInsets) insets).getSystemWindowInsetLeft();
    }

    public static int getSystemWindowInsetRight(Object insets) {
        return ((WindowInsets) insets).getSystemWindowInsetRight();
    }

    public static int getSystemWindowInsetTop(Object insets) {
        return ((WindowInsets) insets).getSystemWindowInsetTop();
    }

    public static boolean hasInsets(Object insets) {
        return ((WindowInsets) insets).hasInsets();
    }

    public static boolean hasSystemWindowInsets(Object insets) {
        return ((WindowInsets) insets).hasSystemWindowInsets();
    }

    public static boolean isRound(Object insets) {
        return ((WindowInsets) insets).isRound();
    }

    public static Object replaceSystemWindowInsets(Object insets, int left, int top, int right,
            int bottom) {
        return ((WindowInsets) insets).replaceSystemWindowInsets(left, top, right, bottom);
    }

    public static Object getSourceWindowInsets(Object src) {
        return new WindowInsets((WindowInsets) src);
    }
}
