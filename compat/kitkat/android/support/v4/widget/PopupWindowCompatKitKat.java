/*
 * Copyright (C) 2013 The Android Open Source Project
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

package android.support.v4.widget;

import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.ListPopupWindow;
import android.widget.PopupWindow;

/**
 * Implementation of PopupWindow compatibility that can call KitKat APIs.
 */
class PopupWindowCompatKitKat {
    public static void showAsDropDown(PopupWindow popup, View anchor, int xoff, int yoff,
            int gravity) {
        popup.showAsDropDown(anchor, xoff, yoff, gravity);
    }
}
