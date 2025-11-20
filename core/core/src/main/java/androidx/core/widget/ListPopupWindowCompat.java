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
import android.view.View.OnTouchListener;
import android.widget.ListPopupWindow;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Helper for accessing features in {@link ListPopupWindow}.
 */
public final class ListPopupWindowCompat {
    private ListPopupWindowCompat() {
        // This class is not publicly instantiable.
    }

    /**
     * Returns an {@link OnTouchListener} that can be added to the source view to
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
     * @return a touch listener that controls drag-to-open behavior, or {@code null} on
     *         unsupported APIs
     *
     * @deprecated Use {@link #createDragToOpenListener(ListPopupWindow, View)} that takes in
     * {@link ListPopupWindow} instead of {@link Object}.
     */
    @Deprecated
    public static OnTouchListener createDragToOpenListener(Object listPopupWindow, View src) {
        return ListPopupWindowCompat.createDragToOpenListener(
                (ListPopupWindow) listPopupWindow, src);
    }

    /**
     * Returns an {@link OnTouchListener} that can be added to the source view to
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
     * @return a touch listener that controls drag-to-open behavior, or {@code null} on
     *         unsupported APIs
     * @deprecated Call {@link ListPopupWindow#createDragToOpenListener()} directly.
     */
    @Deprecated
    @androidx.annotation.ReplaceWith(expression = "listPopupWindow.createDragToOpenListener(src)")
    public static @Nullable OnTouchListener createDragToOpenListener(
            @NonNull ListPopupWindow listPopupWindow, @NonNull View src) {
        return listPopupWindow.createDragToOpenListener(src);
    }
}
