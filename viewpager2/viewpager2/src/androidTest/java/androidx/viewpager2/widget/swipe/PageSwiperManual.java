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

import static androidx.test.espresso.action.GeneralLocation.CENTER;
import static androidx.viewpager2.widget.BaseTestKt.isHorizontal;
import static androidx.viewpager2.widget.BaseTestKt.isRtl;

import android.view.animation.Interpolator;

import androidx.annotation.GuardedBy;
import androidx.test.espresso.action.CoordinatesProvider;
import androidx.viewpager2.widget.ViewPager2;

public class PageSwiperManual implements PageSwiper {
    private final ViewPager2 mViewPager;
    private final SwipeAction mActionPrevious;
    private final SwipeAction mActionNext;
    // Factor to apply to an x-delta to generate a forward swipe by that delta.
    // Use -mXForwardFactor for a backwards swipe.
    private final int mXForwardFactor;
    // Factor to apply to a y-delta to generate a forward swipe by that delta
    // Use -mYForwardFactor for a backwards swipe.
    private final int mYForwardFactor;

    // The actual injector. Should probably be merged with this class.
    @GuardedBy("mLock")
    private ManualSwipeInjector mInjector;
    private final Object mLock = new Object();

    // If the swipe has been cancelled; in case this PageSwiper is cancelled before it was started
    private boolean mCancelled;

    public PageSwiperManual(ViewPager2 viewPager) {
        mViewPager = viewPager;
        boolean isRtl = isRtl(mViewPager);
        boolean isHorizontal = isHorizontal(mViewPager);
        mActionPrevious = isHorizontal ? (isRtl ? SWIPE_LEFT : SWIPE_RIGHT) : SWIPE_DOWN;
        mActionNext = isHorizontal ? (isRtl ? SWIPE_RIGHT : SWIPE_LEFT) : SWIPE_UP;
        mXForwardFactor = isHorizontal ? (isRtl ? 1 : -1) : 0;
        mYForwardFactor = isHorizontal ? 0 : -1;
    }

    @Override
    public void swipeNext() {
        setSwipeInjector(mActionNext.createInjector());
        if (!mCancelled) {
            getSwipeInjector().perform(mViewPager);
        }
        setSwipeInjector(null);
    }

    @Override
    public void swipePrevious() {
        setSwipeInjector(mActionPrevious.createInjector());
        if (!mCancelled) {
            getSwipeInjector().perform(mViewPager);
        }
        setSwipeInjector(null);
    }

    public void swipeForward(float px, Interpolator interpolator) {
        setSwipeInjector(createInjector(px * mXForwardFactor, px * mYForwardFactor));
        if (!mCancelled) {
            getSwipeInjector().perform(mViewPager, interpolator);
        }
        setSwipeInjector(null);
    }

    public void swipeBackward(float px, Interpolator interpolator) {
        setSwipeInjector(createInjector(px * -mXForwardFactor, px * -mYForwardFactor));
        if (!mCancelled) {
            getSwipeInjector().perform(mViewPager, interpolator);
        }
        setSwipeInjector(null);
    }

    public void cancel() {
        mCancelled = true;
        synchronized (mLock) {
            if (mInjector != null) {
                mInjector.cancel();
            }
        }
    }

    private ManualSwipeInjector getSwipeInjector() {
        synchronized (mLock) {
            return mInjector;
        }
    }

    private void setSwipeInjector(ManualSwipeInjector swipeInjector) {
        synchronized (mLock) {
            mInjector = swipeInjector;
        }
    }

    private ManualSwipeInjector createInjector(float xOffset, float yOffset) {
        return new ManualSwipeInjector(
                offsetCenter(-xOffset / 2, -yOffset / 2),
                offsetCenter(xOffset / 2, yOffset / 2),
                150, 20
        );
    }

    private static CoordinatesProvider offsetCenter(final float dx, final float dy) {
        return new TranslatedCoordinatesProvider(CENTER, dx, dy);
    }

    private interface SwipeAction {
        ManualSwipeInjector createInjector();
    }

    private static final SwipeAction SWIPE_LEFT = new SwipeAction() {
        @Override
        public ManualSwipeInjector createInjector() {
            return ManualSwipeInjector.swipeLeft();
        }
    };

    private static final SwipeAction SWIPE_RIGHT = new SwipeAction() {
        @Override
        public ManualSwipeInjector createInjector() {
            return ManualSwipeInjector.swipeRight();
        }
    };

    private static final SwipeAction SWIPE_UP = new SwipeAction() {
        @Override
        public ManualSwipeInjector createInjector() {
            return ManualSwipeInjector.swipeUp();
        }
    };

    private static final SwipeAction SWIPE_DOWN = new SwipeAction() {
        @Override
        public ManualSwipeInjector createInjector() {
            return ManualSwipeInjector.swipeDown();
        }
    };
}
