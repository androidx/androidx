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
import android.view.WindowManager;
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
        void showAsDropDown(PopupWindow popup, View anchor, int xoff, int yoff, int gravity);
        void setOverlapAnchor(PopupWindow popupWindow, boolean overlapAnchor);
        boolean getOverlapAnchor(PopupWindow popupWindow);
        void setWindowLayoutType(PopupWindow popupWindow, int layoutType);
        int getWindowLayoutType(PopupWindow popupWindow);
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

        @Override
        public void setOverlapAnchor(PopupWindow popupWindow, boolean overlapAnchor) {
            // noop
        }

        @Override
        public boolean getOverlapAnchor(PopupWindow popupWindow) {
            return false;
        }

        @Override
        public void setWindowLayoutType(PopupWindow popupWindow, int layoutType) {
            // no-op
        }

        @Override
        public int getWindowLayoutType(PopupWindow popupWindow) {
            return 0;
        }
    }

    /**
     * Interface implementation that doesn't use anything above v4 APIs.
     */
    static class GingerbreadPopupWindowImpl extends BasePopupWindowImpl {
        @Override
        public void setWindowLayoutType(PopupWindow popupWindow, int layoutType) {
            PopupWindowCompatGingerbread.setWindowLayoutType(popupWindow, layoutType);
        }

        @Override
        public int getWindowLayoutType(PopupWindow popupWindow) {
            return PopupWindowCompatGingerbread.getWindowLayoutType(popupWindow);
        }
    }

    /**
     * Interface implementation for devices with at least KitKat APIs.
     */
    static class KitKatPopupWindowImpl extends GingerbreadPopupWindowImpl {
        @Override
        public void showAsDropDown(PopupWindow popup, View anchor, int xoff, int yoff,
                int gravity) {
            PopupWindowCompatKitKat.showAsDropDown(popup, anchor, xoff, yoff, gravity);
        }
    }

    static class Api21PopupWindowImpl extends KitKatPopupWindowImpl {
        @Override
        public void setOverlapAnchor(PopupWindow popupWindow, boolean overlapAnchor) {
            PopupWindowCompatApi21.setOverlapAnchor(popupWindow, overlapAnchor);
        }

        @Override
        public boolean getOverlapAnchor(PopupWindow popupWindow) {
            return PopupWindowCompatApi21.getOverlapAnchor(popupWindow);
        }
    }

    static class Api23PopupWindowImpl extends Api21PopupWindowImpl {
        @Override
        public void setOverlapAnchor(PopupWindow popupWindow, boolean overlapAnchor) {
            PopupWindowCompatApi23.setOverlapAnchor(popupWindow, overlapAnchor);
        }

        @Override
        public boolean getOverlapAnchor(PopupWindow popupWindow) {
            return PopupWindowCompatApi23.getOverlapAnchor(popupWindow);
        }

        @Override
        public void setWindowLayoutType(PopupWindow popupWindow, int layoutType) {
            PopupWindowCompatApi23.setWindowLayoutType(popupWindow, layoutType);
        }

        @Override
        public int getWindowLayoutType(PopupWindow popupWindow) {
            return PopupWindowCompatApi23.getWindowLayoutType(popupWindow);
        }
    }

    /**
     * Select the correct implementation to use for the current platform.
     */
    static final PopupWindowImpl IMPL;
    static {
        final int version = android.os.Build.VERSION.SDK_INT;
        if (version >= 23) {
            IMPL = new Api23PopupWindowImpl();
        } else if (version >= 21) {
            IMPL = new Api21PopupWindowImpl();
        } else if (version >= 19) {
            IMPL = new KitKatPopupWindowImpl();
        } else if (version >= 9) {
            IMPL = new GingerbreadPopupWindowImpl();
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

    /**
     * Sets whether the popup window should overlap its anchor view when
     * displayed as a drop-down.
     *
     * @param overlapAnchor Whether the popup should overlap its anchor.
     */
    public static void setOverlapAnchor(PopupWindow popupWindow, boolean overlapAnchor) {
        IMPL.setOverlapAnchor(popupWindow, overlapAnchor);
    }

    /**
     * Returns whether the popup window should overlap its anchor view when
     * displayed as a drop-down.
     *
     * @return Whether the popup should overlap its anchor.
     */
    public static boolean getOverlapAnchor(PopupWindow popupWindow) {
        return IMPL.getOverlapAnchor(popupWindow);
    }

    /**
     * Set the layout type for this window. This value will be passed through to
     * {@link WindowManager.LayoutParams#type} therefore the value should match any value
     * {@link WindowManager.LayoutParams#type} accepts.
     *
     * @param layoutType Layout type for this window.
     *
     * @see WindowManager.LayoutParams#type
     */
    public static void setWindowLayoutType(PopupWindow popupWindow, int layoutType) {
        IMPL.setWindowLayoutType(popupWindow, layoutType);
    }

    /**
     * Returns the layout type for this window.
     *
     * @see #setWindowLayoutType(PopupWindow popupWindow, int)
     */
    public static int getWindowLayoutType(PopupWindow popupWindow) {
        return IMPL.getWindowLayoutType(popupWindow);
    }
}
