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
import android.widget.ListView;

/**
 * An implementation of {@link AutoScrollHelper} that knows how to scroll
 * through a {@link ListView}.
 */
public class ListViewAutoScrollHelper extends AutoScrollHelper {
    private final ListView mTarget;

    public ListViewAutoScrollHelper(ListView target) {
        super(target);

        mTarget = target;
    }

    @Override
    public boolean onScrollBy(int deltaX, int deltaY) {
        final ListView target = mTarget;
        final int itemCount = target.getCount();
        final int childCount = target.getChildCount();
        final int firstPosition = target.getFirstVisiblePosition();
        final int lastPosition = firstPosition + childCount;

        if (deltaY > 0) {
            // Are we already showing the entire last item?
            if (lastPosition >= itemCount) {
                final View lastView = target.getChildAt(childCount - 1);
                if (lastView.getBottom() <= target.getHeight()) {
                    return false;
                }
            }
        } else if (deltaY < 0) {
            // Are we already showing the entire first item?
            if (firstPosition <= 0) {
                final View firstView = target.getChildAt(0);
                if (firstView.getTop() >= 0) {
                    return false;
                }
            }
        } else {
            // We're not scrolling anywhere.
            return false;
        }

        final View firstView = target.getChildAt(0);
        target.setSelectionFromTop(firstPosition, firstView.getTop() - deltaY);
        return true;
    }
}
