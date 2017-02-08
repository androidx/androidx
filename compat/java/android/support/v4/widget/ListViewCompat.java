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

package android.support.v4.widget;

import android.os.Build;
import android.support.annotation.NonNull;
import android.view.View;
import android.widget.ListView;

/**
 * Helper for accessing features in {@link ListView} introduced after API level
 * 4 in a backwards compatible fashion.
 */
public final class ListViewCompat {

    /**
     * Scrolls the list items within the view by a specified number of pixels.
     *
     * @param listView the list to scroll
     * @param y the amount of pixels to scroll by vertically
     */
    public static void scrollListBy(@NonNull ListView listView, int y) {
        if (Build.VERSION.SDK_INT >= 19) {
            listView.scrollListBy(y);
        } else {
            final int firstPosition = listView.getFirstVisiblePosition();
            if (firstPosition == ListView.INVALID_POSITION) {
                return;
            }

            final View firstView = listView.getChildAt(0);
            if (firstView == null) {
                return;
            }

            final int newTop = firstView.getTop() - y;
            listView.setSelectionFromTop(firstPosition, newTop);
        }
    }

    private ListViewCompat() {}
}
