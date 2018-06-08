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

package androidx.appcompat.app;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.matcher.ViewMatchers.isAssignableFrom;

import static androidx.appcompat.testutils.DrawerLayoutActions.openDrawer;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import android.app.Activity;
import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.LargeTest;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.view.Gravity;
import android.view.InputDevice;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@RunWith(AndroidJUnit4.class)
public class DrawerInteractionTest {

    @Rule
    public final ActivityTestRule<DrawerInteractionActivity> mActivityTestRule =
            new ActivityTestRule<>(DrawerInteractionActivity.class);

    private TestDrawerLayout mDrawerLayout;
    private MockView mStartDrawer;
    private MockView mContentView;

    @Before
    public void setUp() throws Throwable {
        final Activity activity = mActivityTestRule.getActivity();

        Context context = InstrumentationRegistry.getContext();
        mDrawerLayout = new TestDrawerLayout(InstrumentationRegistry.getContext());
        mContentView = new MockView(context);
        mStartDrawer = new MockView(context);

        mDrawerLayout.setLayoutParams(new ViewGroup.LayoutParams(100, 100));
        DrawerLayout.LayoutParams contentViewLayoutParams = new DrawerLayout.LayoutParams(100, 100);
        DrawerLayout.LayoutParams drawerViewLayoutParams = new DrawerLayout.LayoutParams(50, 100);

        contentViewLayoutParams.gravity = Gravity.NO_GRAVITY;
        drawerViewLayoutParams.gravity = Gravity.START;

        mContentView.setLayoutParams(contentViewLayoutParams);
        mStartDrawer.setLayoutParams(drawerViewLayoutParams);
        mStartDrawer.setBackgroundColor(0xFF00FF00);

        mDrawerLayout.addView(mContentView);
        mDrawerLayout.addView(mStartDrawer);
        mDrawerLayout.setBackgroundColor(0xFFFF0000);

        mDrawerLayout.expectLayouts(1);
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                activity.setContentView(mDrawerLayout);
            }
        });
        assertThat(mDrawerLayout.waitForLayouts(2), is(true));
    }

    // Verification that DrawerLayout blocks certain pointer motion events from getting to the
    // content view when the drawer is open.

    @Test
    @LargeTest
    public void ui_pointerEvent_overDrawer_receivedByDrawer() throws Throwable {
        onView(isAssignableFrom(DrawerLayout.class)).perform(openDrawer(
                GravityCompat.START));
        MotionEvent motionEvent = MotionEvent.obtain(0, 0, MotionEvent.ACTION_SCROLL, 25, 50, 0);
        motionEvent.setSource(InputDevice.SOURCE_CLASS_POINTER);

        mDrawerLayout.dispatchGenericMotionEvent(motionEvent);

        assertThat(mStartDrawer.mMotionEventPassedToDispatchGenericMotionEvent,
                is(equalTo(motionEvent)));
    }

    @Test
    @LargeTest
    public void ui_pointerEvent_overDrawer_notReceivedByContent() throws Throwable {
        onView(isAssignableFrom(DrawerLayout.class)).perform(openDrawer(
                GravityCompat.START));
        MotionEvent motionEvent = MotionEvent.obtain(0, 0, MotionEvent.ACTION_SCROLL, 25, 50, 0);
        motionEvent.setSource(InputDevice.SOURCE_CLASS_POINTER);

        mDrawerLayout.dispatchGenericMotionEvent(motionEvent);

        assertThat(mContentView.mDispatchGenericMotionEventCalled, is(false));
    }

    @Test
    @LargeTest
    public void ui_pointerEvent_overContentDrawerOpen_notReceivedByContent() throws Throwable {
        onView(isAssignableFrom(DrawerLayout.class)).perform(openDrawer(
                GravityCompat.START));
        MotionEvent motionEvent = MotionEvent.obtain(0, 0, MotionEvent.ACTION_SCROLL, 75, 50, 0);
        motionEvent.setSource(InputDevice.SOURCE_CLASS_POINTER);

        mDrawerLayout.dispatchGenericMotionEvent(motionEvent);

        assertThat(mContentView.mDispatchGenericMotionEventCalled, is(false));
    }

    @Test
    @LargeTest
    public void ui_pointerEvent_overContentDrawerClosed_receivedByContent() {
        MotionEvent motionEvent = MotionEvent.obtain(0, 0, MotionEvent.ACTION_SCROLL, 75, 50, 0);
        motionEvent.setSource(InputDevice.SOURCE_CLASS_POINTER);

        mDrawerLayout.dispatchGenericMotionEvent(motionEvent);

        assertThat(mContentView.mMotionEventPassedToDispatchGenericMotionEvent,
                is(equalTo(motionEvent)));
    }

    private class TestDrawerLayout extends DrawerLayout {

        CountDownLatch mLayoutCountDownLatch;

        TestDrawerLayout(Context context) {
            super(context);
        }

        public void expectLayouts(int count) {
            mLayoutCountDownLatch = new CountDownLatch(count);
        }

        public boolean waitForLayouts(int seconds) throws InterruptedException {
            boolean result = mLayoutCountDownLatch.await(seconds, TimeUnit.SECONDS);
            mLayoutCountDownLatch = null;
            return result;
        }

        @Override
        protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
            super.onLayout(changed, left, top, right, bottom);
            if (mLayoutCountDownLatch != null) {
                mLayoutCountDownLatch.countDown();
            }
        }

    }

    private class MockView extends View {

        MotionEvent mMotionEventPassedToDispatchGenericMotionEvent;
        boolean mDispatchGenericMotionEventCalled = false;

        MockView(Context context) {
            super(context);
        }

        @Override
        public boolean dispatchGenericMotionEvent(MotionEvent event) {
            mMotionEventPassedToDispatchGenericMotionEvent = event;
            mDispatchGenericMotionEventCalled = true;
            return super.dispatchGenericMotionEvent(event);
        }
    }


}
