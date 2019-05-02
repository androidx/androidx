/*
 * Copyright 2018 The Android Open Source Project
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

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.swipeDown;
import static androidx.test.espresso.action.ViewActions.swipeLeft;
import static androidx.test.espresso.action.ViewActions.swipeRight;
import static androidx.test.espresso.action.ViewActions.swipeUp;
import static androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.viewpager2.widget.BaseTestKt.isHorizontal;
import static androidx.viewpager2.widget.BaseTestKt.isRtl;

import static org.hamcrest.CoreMatchers.allOf;

import androidx.test.espresso.ViewAction;
import androidx.viewpager2.widget.ViewPager2;

public class PageSwiperEspresso implements PageSwiper {
    private final ViewAction mActionPrevious;
    private final ViewAction mActionNext;

    public PageSwiperEspresso(ViewPager2 viewPager) {
        boolean isHorizontal = isHorizontal(viewPager);
        boolean isRtl = isRtl(viewPager);
        mActionPrevious = isHorizontal ? (isRtl ? swipeLeft() : swipeRight()) : swipeDown();
        mActionNext = isHorizontal ? (isRtl ? swipeRight() : swipeLeft()) : swipeUp();
    }

    @Override
    public void swipeNext() {
        swipe(mActionNext);
    }

    @Override
    public void swipePrevious() {
        swipe(mActionPrevious);
    }

    private void swipe(ViewAction swipeAction) {
        onView(allOf(isDisplayed(), isAssignableFrom(ViewPager2.class))).perform(swipeAction);
    }
}
