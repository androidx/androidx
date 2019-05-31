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

import android.os.Build;
import android.view.View.OnTouchListener;
import android.widget.PopupMenu;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Helper for accessing features in {@link PopupMenu}.
 */
public final class PopupMenuCompat {
    private PopupMenuCompat() {
        // This class is not publicly instantiable.
    }

    /**
     * On API {@link android.os.Build.VERSION_CODES#KITKAT} and higher, returns
     * an {@link OnTouchListener} that can be added to the anchor view to
     * implement drag-to-open behavior.
     * <p>
     * When the listener is set on a view, touching that view and dragging
     * outside of its bounds will open the popup window. Lifting will select the
     * currently touched list item.
     * <p>
     * Example usage:
     * <pre>
     * PopupMenu myPopup = new PopupMenu(context, myAnchor);
     * myAnchor.setOnTouchListener(PopupMenuCompat.getDragToOpenListener(myPopup));
     * </pre>
     *
     * @param popupMenu the PopupMenu against which to invoke the method
     * @return a touch listener that controls drag-to-open behavior, or {@code null} on
     *         unsupported APIs
     */
    @Nullable
    public static OnTouchListener getDragToOpenListener(@NonNull Object popupMenu) {
        if (Build.VERSION.SDK_INT >= 19) {
            return ((PopupMenu) popupMenu).getDragToOpenListener();
        } else {
            return null;
        }
    }
}
