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
import android.widget.PopupWindow;

/**
 * Helper for accessing features in PopupWindow introduced after API level 4
 * in a backwards compatible fashion.
 */
public class PopupWindowCompat {
    /**
     * Interface for the full API.
     */
    interface PopupWindowImpl {
        public void showAsDropDown(PopupWindow popup, View anchor, int xoff, int yoff,
                int gravity);
    }

    /**
     * Interface implementation that doesn't use anything above v4 APIs.
     */
    static class BasePopupWindowImpl implements PopupWindowImpl {
        @Override
        public void showAsDropDown(PopupWindow popup, View anchor, int xoff, int yoff,
                int gravity) {
            popup.showAsDropDown(anchor, xoff, yoff);
        }
    }

    /**
     * Interface implementation for devices with at least KitKat APIs.
     */
    static class KitKatPopupWindowImpl extends BasePopupWindowImpl {
        @Override
        public void showAsDropDown(PopupWindow popup, View anchor, int xoff, int yoff,
                int gravity) {
            PopupWindowCompatKitKat.showAsDropDown(popup, anchor, xoff, yoff, gravity);
        }
    }

    /**
     * Select the correct implementation to use for the current platform.
     */
    static final PopupWindowImpl IMPL;
    static {
        final int version = android.os.Build.VERSION.SDK_INT;
        if (version >= 19) {
            IMPL = new KitKatPopupWindowImpl();
        } else {
            IMPL = new BasePopupWindowImpl();
        }
    }

    private PopupWindowCompat() {
        // This class is not publicly instantiable.
    }

    /**
     * <p>Display the content view in a popup window anchored to the bottom-left
     * corner of the anchor view offset by the specified x and y coordinates.
     * If there is not enough room on screen to show
     * the popup in its entirety, this method tries to find a parent scroll
     * view to scroll. If no parent scroll view can be scrolled, the bottom-left
     * corner of the popup is pinned at the top left corner of the anchor view.</p>
     * <p>If the view later scrolls to move <code>anchor</code> to a different
     * location, the popup will be moved correspondingly.</p>
     *
     * @param popup the PopupWindow to show
     * @param anchor the view on which to pin the popup window
     * @param xoff A horizontal offset from the anchor in pixels
     * @param yoff A vertical offset from the anchor in pixels
     * @param gravity Alignment of the popup relative to the anchor
     */
    public static void showAsDropDown(PopupWindow popup, View anchor, int xoff, int yoff,
            int gravity) {
        IMPL.showAsDropDown(popup, anchor, xoff, yoff, gravity);
    }
}
