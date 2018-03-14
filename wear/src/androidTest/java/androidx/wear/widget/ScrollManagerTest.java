/*
 * Copyright (C) 2017 The Android Open Source Project
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

package androidx.wear.widget;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import static java.lang.Math.cos;
import static java.lang.Math.sin;

import android.os.SystemClock;
import android.support.test.filters.MediumTest;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.view.MotionEvent;

import androidx.wear.widget.util.WakeLockRule;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@MediumTest
@RunWith(AndroidJUnit4.class)
public class ScrollManagerTest {
    private static final int TEST_WIDTH = 400;
    private static final int TEST_HEIGHT = 400;
    private static final int STEP_COUNT = 300;

    private static final int EXPECTED_SCROLLS_FOR_STRAIGHT_GESTURE = 36;
    private static final int EXPECTED_SCROLLS_FOR_CIRCULAR_GESTURE = 199;

    @Rule
    public final WakeLockRule wakeLock = new WakeLockRule();

    @Rule
    public final ActivityTestRule<WearableRecyclerViewTestActivity> mActivityRule =
            new ActivityTestRule<>(WearableRecyclerViewTestActivity.class, true, true);

    @Mock
    WearableRecyclerView mMockWearableRecyclerView;

    ScrollManager mScrollManagerUnderTest;

    @Before
    public void setUp() throws Throwable {
        MockitoAnnotations.initMocks(this);
        mScrollManagerUnderTest = new ScrollManager();
        mScrollManagerUnderTest.setRecyclerView(mMockWearableRecyclerView, TEST_WIDTH, TEST_HEIGHT);
    }

    @Test
    public void testStraightUpScrollingGestureLeft() throws Throwable {
        // Pretend to scroll in a straight line from center left to upper left
        scroll(mScrollManagerUnderTest, 30, 30, 200, 150);
        // The scroll manager should require the recycler view to scroll up and only up
        verify(mMockWearableRecyclerView, times(EXPECTED_SCROLLS_FOR_STRAIGHT_GESTURE))
                .scrollBy(0, 1);
    }

    @Test
    public void testStraightDownScrollingGestureLeft() throws Throwable {
        // Pretend to scroll in a straight line upper left to center left
        scroll(mScrollManagerUnderTest, 30, 30, 150, 200);
        // The scroll manager should require the recycler view to scroll down and only down
        verify(mMockWearableRecyclerView, times(EXPECTED_SCROLLS_FOR_STRAIGHT_GESTURE))
                .scrollBy(0, -1);
    }

    @Test
    public void testStraightUpScrollingGestureRight() throws Throwable {
        // Pretend to scroll in a straight line from center right to upper right
        scroll(mScrollManagerUnderTest, 370, 370, 200, 150);
        // The scroll manager should require the recycler view to scroll down and only down
        verify(mMockWearableRecyclerView, times(EXPECTED_SCROLLS_FOR_STRAIGHT_GESTURE))
                .scrollBy(0, -1);
    }

    @Test
    public void testStraightDownScrollingGestureRight() throws Throwable {
        // Pretend to scroll in a straight line upper right to center right
        scroll(mScrollManagerUnderTest, 370, 370, 150, 200);
        // The scroll manager should require the recycler view to scroll up and only up
        verify(mMockWearableRecyclerView, times(EXPECTED_SCROLLS_FOR_STRAIGHT_GESTURE))
                .scrollBy(0, 1);
    }

    @Test
    public void testCircularScrollingGestureLeft() throws Throwable {
        // Pretend to scroll in an arch from center left to center right
        scrollOnArch(mScrollManagerUnderTest, 30, 200, 180.0f);
        // The scroll manager should never reverse the scroll direction and scroll up
        verify(mMockWearableRecyclerView, times(EXPECTED_SCROLLS_FOR_CIRCULAR_GESTURE))
                .scrollBy(0, 1);
    }

    @Test
    public void testCircularScrollingGestureRight() throws Throwable {
        // Pretend to scroll in an arch from center left to center right
        scrollOnArch(mScrollManagerUnderTest, 370, 200, -180.0f);
        // The scroll manager should never reverse the scroll direction and scroll down.
        verify(mMockWearableRecyclerView, times(EXPECTED_SCROLLS_FOR_CIRCULAR_GESTURE))
                .scrollBy(0, -1);
    }

    private static void scroll(ScrollManager scrollManager, float fromX, float toX, float fromY,
            float toY) {
        long downTime = SystemClock.uptimeMillis();
        long eventTime = SystemClock.uptimeMillis();

        float y = fromY;
        float x = fromX;

        float yStep = (toY - fromY) / STEP_COUNT;
        float xStep = (toX - fromX) / STEP_COUNT;

        MotionEvent event = MotionEvent.obtain(downTime, eventTime,
                MotionEvent.ACTION_DOWN, x, y, 0);
        scrollManager.onTouchEvent(event);
        for (int i = 0; i < STEP_COUNT; ++i) {
            y += yStep;
            x += xStep;
            eventTime = SystemClock.uptimeMillis();
            event = MotionEvent.obtain(downTime, eventTime, MotionEvent.ACTION_MOVE, x, y, 0);
            scrollManager.onTouchEvent(event);
        }

        eventTime = SystemClock.uptimeMillis();
        event = MotionEvent.obtain(downTime, eventTime, MotionEvent.ACTION_UP, x, y, 0);
        scrollManager.onTouchEvent(event);
    }

    private static void scrollOnArch(ScrollManager scrollManager, float fromX, float fromY,
            float deltaAngle) {
        long downTime = SystemClock.uptimeMillis();
        long eventTime = SystemClock.uptimeMillis();

        float stepAngle = deltaAngle / STEP_COUNT;
        double relativeX = fromX - (TEST_WIDTH / 2);
        double relativeY = fromY - (TEST_HEIGHT / 2);
        float radius = (float) Math.sqrt(relativeX * relativeX + relativeY * relativeY);
        float angle = getAngle(fromX, fromY, TEST_WIDTH, TEST_HEIGHT);

        float y = fromY;
        float x = fromX;

        MotionEvent event = MotionEvent.obtain(downTime, eventTime,
                MotionEvent.ACTION_DOWN, x, y, 0);
        scrollManager.onTouchEvent(event);
        for (int i = 0; i < STEP_COUNT; ++i) {
            angle += stepAngle;
            x = getX(angle, radius, TEST_WIDTH);
            y = getY(angle, radius, TEST_HEIGHT);
            eventTime = SystemClock.uptimeMillis();
            event = MotionEvent.obtain(downTime, eventTime, MotionEvent.ACTION_MOVE, x, y, 0);
            scrollManager.onTouchEvent(event);
        }

        eventTime = SystemClock.uptimeMillis();
        event = MotionEvent.obtain(downTime, eventTime, MotionEvent.ACTION_UP, x, y, 0);
        scrollManager.onTouchEvent(event);
    }

    private static float getX(double angle, double radius, double viewWidth) {
        double radianAngle = Math.toRadians(angle - 90);
        double relativeX = cos(radianAngle) * radius;
        return (float) (relativeX + (viewWidth / 2));
    }

    private static float getY(double angle, double radius, double viewHeight) {
        double radianAngle = Math.toRadians(angle - 90);
        double relativeY = sin(radianAngle) * radius;
        return (float) (relativeY + (viewHeight / 2));
    }

    private static float getAngle(double x, double y, double viewWidth, double viewHeight) {
        double relativeX = x - (viewWidth / 2);
        double relativeY = y - (viewHeight / 2);
        double rowAngle = Math.atan2(relativeX, relativeY);
        double angle = -Math.toDegrees(rowAngle) - 180;
        if (angle < 0) {
            angle += 360;
        }
        return (float) angle;
    }
}
