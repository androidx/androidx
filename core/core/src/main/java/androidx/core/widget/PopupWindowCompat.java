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

package androidx.core.widget;

import android.view.View;
import android.widget.PopupWindow;

import org.jspecify.annotations.NonNull;

/**
 * Helper for accessing features in {@link PopupWindow}.
 */
public final class PopupWindowCompat {

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
     * @deprecated Call {@link PopupWindow#showAsDropDown()} directly.
     */
    @Deprecated
    @androidx.annotation.ReplaceWith(expression = "popup.showAsDropDown(anchor, xoff, yoff, gravity)")
    public static void showAsDropDown(@NonNull PopupWindow popup, @NonNull View anchor,
            int xoff, int yoff, int gravity) {
        popup.showAsDropDown(anchor, xoff, yoff, gravity);
    }

    /**
     * Sets whether the popup window should overlap its anchor view when
     * displayed as a drop-down.
     *
     * @param popupWindow popup window for which to set the anchor.
     * @param overlapAnchor Whether the popup should overlap its anchor.
     */
    public static void setOverlapAnchor(@NonNull PopupWindow popupWindow, boolean overlapAnchor) {
        popupWindow.setOverlapAnchor(overlapAnchor);
    }

    /**
     * Returns whether the popup window should overlap its anchor view when
     * displayed as a drop-down.
     *
     * @return Whether the popup should overlap its anchor.
     */
    public static boolean getOverlapAnchor(@NonNull PopupWindow popupWindow) {
        return popupWindow.getOverlapAnchor();
    }

    /**
     * Set the layout type for this window. This value will be passed through to
     * {@link android.view.WindowManager.LayoutParams#type} therefore the value should match any
     * value {@link android.view.WindowManager.LayoutParams#type} accepts.
     *
     * @param popupWindow popup window for which to set the layout type.
     * @param layoutType Layout type for this window.
     *
     * @see android.view.WindowManager.LayoutParams#type
     */
    public static void setWindowLayoutType(@NonNull PopupWindow popupWindow, int layoutType) {
        popupWindow.setWindowLayoutType(layoutType);
    }

    /**
     * Returns the layout type for this window.
     *
     * @see #setWindowLayoutType(PopupWindow popupWindow, int)
     */
    public static int getWindowLayoutType(@NonNull PopupWindow popupWindow) {
        return popupWindow.getWindowLayoutType();
    }
}
