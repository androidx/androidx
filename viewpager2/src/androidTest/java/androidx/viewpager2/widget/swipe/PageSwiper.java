/*
 * Copyright (C) 2018 The Android Open Source Project
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

package androidx.viewpager2.widget.swipe;

import static androidx.core.util.Preconditions.checkArgumentNonnegative;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

import static org.hamcrest.CoreMatchers.allOf;

import androidx.test.espresso.ViewAction;
import androidx.test.espresso.action.ViewActions;
import androidx.viewpager2.test.R;
import androidx.viewpager2.widget.ViewPager2;

public class PageSwiper {
    private final int mLastPageIx;
    private final ViewAction mActionPrevious;
    private final ViewAction mActionNext;

    public PageSwiper(int totalPages, @ViewPager2.Orientation int orientation) {
        mLastPageIx = checkArgumentNonnegative(totalPages - 1);

        // TODO: handle RTL
        mActionPrevious = orientation == ViewPager2.Orientation.HORIZONTAL
                ? ViewActions.swipeRight() : ViewActions.swipeDown();
        mActionNext = orientation == ViewPager2.Orientation.HORIZONTAL
                ? ViewActions.swipeLeft() : ViewActions.swipeUp();
    }

    public void swipe(int currentPageIx, int nextPageIx) {
        if (nextPageIx > mLastPageIx) {
            throw new IllegalArgumentException("Invalid next page: beyond last page.");
        }

        if (currentPageIx == nextPageIx) { // dedicated for testing edge behaviour
            if (nextPageIx == 0) {
                swipePrevious(); // bounce off the "left" edge
                return;
            }
            if (nextPageIx == mLastPageIx) { // bounce off the "right" edge
                swipeNext();
                return;
            }
            throw new IllegalArgumentException(
                    "Invalid sequence. Not on an edge, and current page = next page.");
        }

        if (Math.abs(nextPageIx - currentPageIx) > 1) {
            throw new IllegalArgumentException(
                    "Specified next page not adjacent to the current page.");
        }

        if (nextPageIx > currentPageIx) {
            swipeNext();
        } else {
            swipePrevious();
        }
    }

    private void swipeNext() {
        swipe(mActionNext);
    }

    private void swipePrevious() {
        swipe(mActionPrevious);
    }

    private void swipe(ViewAction swipeAction) {
        onView(allOf(isDisplayed(), withId(R.id.text_view))).perform(swipeAction);
    }
}
