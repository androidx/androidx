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

import android.view.View;
import android.widget.AbsListView;
import android.widget.ListView;

class ListViewCompatDonut {
    static void scrollListBy(final ListView listView, int y) {
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
