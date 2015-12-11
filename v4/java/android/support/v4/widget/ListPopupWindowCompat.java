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

/**
 * Helper for accessing features in ListPopupWindow introduced after API level 4
 * in a backwards compatible fashion.
 */
public final class ListPopupWindowCompat {
    /**
     * Interface for the full API.
     */
    interface ListPopupWindowImpl {
        public OnTouchListener createDragToOpenListener(Object listPopupWindow, View src);
    }

    /**
     * Interface implementation that doesn't use anything above v4 APIs.
     */
    static class BaseListPopupWindowImpl implements ListPopupWindowImpl {
        @Override
        public OnTouchListener createDragToOpenListener(Object listPopupWindow, View src) {
            return null;
        }
    }

    /**
     * Interface implementation for devices with at least KitKat APIs.
     */
    static class KitKatListPopupWindowImpl extends BaseListPopupWindowImpl {
        @Override
        public OnTouchListener createDragToOpenListener(Object listPopupWindow, View src) {
            return ListPopupWindowCompatKitKat.createDragToOpenListener(listPopupWindow, src);
        }
    }

    /**
     * Select the correct implementation to use for the current platform.
     */
    static final ListPopupWindowImpl IMPL;
    static {
        final int version = android.os.Build.VERSION.SDK_INT;
        if (version >= 19) {
            IMPL = new KitKatListPopupWindowImpl();
        } else {
            IMPL = new BaseListPopupWindowImpl();
        }
    }

    private ListPopupWindowCompat() {
        // This class is not publicly instantiable.
    }

    /**
     * On API {@link android.os.Build.VERSION_CODES#KITKAT} and higher, returns
     * an {@link OnTouchListener} that can be added to the source view to
     * implement drag-to-open behavior. Generally, the source view should be the
     * same view that was passed to ListPopupWindow.setAnchorView(View).
     * <p>
     * When the listener is set on a view, touching that view and dragging
     * outside of its bounds will open the popup window. Lifting will select the
     * currently touched list item.
     * <p>
     * Example usage:
     * 
     * <pre>
     * ListPopupWindow myPopup = new ListPopupWindow(context);
     * myPopup.setAnchor(myAnchor);
     * OnTouchListener dragListener = myPopup.createDragToOpenListener(myAnchor);
     * myAnchor.setOnTouchListener(dragListener);
     * </pre>
     *
     * @param listPopupWindow the ListPopupWindow against which to invoke the
     *            method
     * @param src the view on which the resulting listener will be set
     * @return a touch listener that controls drag-to-open behavior, or null on
     *         unsupported APIs
     */
    public static OnTouchListener createDragToOpenListener(Object listPopupWindow, View src) {
        return IMPL.createDragToOpenListener(listPopupWindow, src);
    }
}
