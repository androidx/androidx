/*
 * Copyright 2017 The Android Open Source Project
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

package androidx.recyclerview.selection;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import android.graphics.Point;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import androidx.annotation.Nullable;
import androidx.recyclerview.selection.ViewAutoScroller.ScrollHost;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
@SmallTest
public final class ViewAutoScrollerTest {

    private static final float SCROLL_THRESHOLD_RATIO = 0.125f;
    private static final int VIEW_HEIGHT = 100;
    private static final int TOP_Y_POINT = (int) (VIEW_HEIGHT * SCROLL_THRESHOLD_RATIO) - 1;
    private static final int BOTTOM_Y_POINT =
            VIEW_HEIGHT - (int) (VIEW_HEIGHT * SCROLL_THRESHOLD_RATIO) + 1;

    private ViewAutoScroller mScroller;
    private TestHost mHost;

    @Before
    public void setUp() {
        mHost = new TestHost();
        mScroller = new ViewAutoScroller(mHost, SCROLL_THRESHOLD_RATIO);
    }

    @Test
    public void testNoScrollWhenOutOfScrollZone() {
        mScroller.scroll(new Point(0, VIEW_HEIGHT / 2));
        mHost.run();
        mHost.assertNotScrolled();
    }

//    @Test
//    public void testNoScrollWhenDisabled() {
//        mScroller.reset();
//        mScroller.scroll(mEvent.location(0, TOP_Y_POINT).build());
//        mHost.assertNotScrolled();
//    }

    @Test
    public void testMotionThreshold() {
        mScroller.scroll(new Point(0, TOP_Y_POINT));
        mHost.run();

        mScroller.scroll(new Point(0, TOP_Y_POINT - 1));
        mHost.run();

        mHost.assertNotScrolled();
    }

    @Test
    public void testMotionThreshold_Resets() {
        int expectedScrollDistance = mScroller.computeScrollDistance(-21);
        mScroller.scroll(new Point(0, TOP_Y_POINT));
        mHost.run();
        // We need enough y motion to overcome motion threshold
        mScroller.scroll(new Point(0, TOP_Y_POINT - 20));
        mHost.run();

        mHost.reset();
        // After resetting events should be required to cross the motion threshold
        // before auto-scrolling again.
        mScroller.reset();

        mScroller.scroll(new Point(0, TOP_Y_POINT));
        mHost.run();

        mHost.assertNotScrolled();
    }

    @Test
    public void testAutoScrolls_Top() {
        int expectedScrollDistance = mScroller.computeScrollDistance(-21);
        mScroller.scroll(new Point(0, TOP_Y_POINT));
        mHost.run();
        // We need enough y motion to overcome motion threshold
        mScroller.scroll(new Point(0, TOP_Y_POINT - 20));
        mHost.run();

        mHost.assertScrolledBy(expectedScrollDistance);
    }

    @Test
    public void testAutoScrolls_Bottom() {
        int expectedScrollDistance = mScroller.computeScrollDistance(21);
        mScroller.scroll(new Point(0, BOTTOM_Y_POINT));
        mHost.run();
        // We need enough y motion to overcome motion threshold
        mScroller.scroll(new Point(0, BOTTOM_Y_POINT + 20));
        mHost.run();

        mHost.assertScrolledBy(expectedScrollDistance);
    }

    private final class TestHost extends ScrollHost {

        private @Nullable Integer mScrollDistance;
        private @Nullable Runnable mRunnable;

        @Override
        int getViewHeight() {
            return VIEW_HEIGHT;
        }

        @Override
        void scrollBy(int distance) {
            mScrollDistance = distance;
        }

        @Override
        void runAtNextFrame(Runnable r) {
            mRunnable = r;
        }

        @Override
        void removeCallback(Runnable r) {
        }

        private void reset() {
            mScrollDistance = null;
            mRunnable = null;
        }

        private void run() {
            mRunnable.run();
        }

        private void assertNotScrolled() {
            assertNull(mScrollDistance);
        }

        private void assertScrolledBy(int expectedDistance) {
            assertNotNull(mScrollDistance);
            assertEquals(expectedDistance, mScrollDistance.intValue());
        }
    }
}
