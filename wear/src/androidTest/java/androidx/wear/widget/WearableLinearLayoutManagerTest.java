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

import static org.junit.Assert.assertEquals;

import android.app.Activity;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.MediumTest;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.view.View;
import android.widget.FrameLayout;

import androidx.wear.test.R;
import androidx.wear.widget.util.WakeLockRule;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.atomic.AtomicReference;

@MediumTest
@RunWith(AndroidJUnit4.class)
public class WearableLinearLayoutManagerTest {

    @Rule
    public final WakeLockRule wakeLock = new WakeLockRule();

    @Rule
    public final ActivityTestRule<WearableRecyclerViewTestActivity> mActivityRule =
            new ActivityTestRule<>(WearableRecyclerViewTestActivity.class, true, true);

    WearableLinearLayoutManager mWearableLinearLayoutManagerUnderTest;

    @Before
    public void setUp() throws Throwable {
        Activity activity = mActivityRule.getActivity();
        CurvingLayoutCallback mCurvingCallback = new CurvingLayoutCallback(activity);
        mCurvingCallback.setOffset(10);
        mWearableLinearLayoutManagerUnderTest =
                new WearableLinearLayoutManager(mActivityRule.getActivity(), mCurvingCallback);
    }

    @Test
    public void testRoundOffsetting() throws Throwable {
        ((CurvingLayoutCallback) mWearableLinearLayoutManagerUnderTest.getLayoutCallback())
                .setRound(true);
        final AtomicReference<WearableRecyclerView> wrvReference = new AtomicReference<>();
        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                WearableRecyclerView wrv =
                        (WearableRecyclerView) mActivityRule.getActivity().findViewById(R.id.wrv);
                // Set a fixed layout so that the test adapts to different device screens.
                wrv.setLayoutParams(new FrameLayout.LayoutParams(390, 390));
            }
        });

        InstrumentationRegistry.getInstrumentation().waitForIdleSync();

        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                WearableRecyclerView wrv =
                        (WearableRecyclerView) mActivityRule.getActivity().findViewById(R.id.wrv);
                wrv.setLayoutManager(mWearableLinearLayoutManagerUnderTest);
                wrvReference.set(wrv);
            }
        });

        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
        WearableRecyclerView wrv = wrvReference.get();

        View child1 = wrv.getChildAt(0);
        View child2 = wrv.getChildAt(1);
        View child3 = wrv.getChildAt(2);
        View child4 = wrv.getChildAt(3);
        View child5 = wrv.getChildAt(4);

        // The left position and the translation of the child is modified if the screen is round.
        // Check if the 5th child is not null as some devices will not be able to display 5 views.
        assertEquals(136, child1.getLeft());
        assertEquals(-6.3, child1.getTranslationY(), 0.1);

        assertEquals(91, child2.getLeft(), 1);
        assertEquals(-15.21, child2.getTranslationY(), 0.1);

        assertEquals(58, child3.getLeft(), 1);
        assertEquals(-13.5, child3.getTranslationY(), 0.1);

        assertEquals(42, child4.getLeft(), 1);
        assertEquals(-4.5, child4.getTranslationY(), 0.1);

        if (child5 != null) {
            assertEquals(43, child5.getLeft(), 1);
            assertEquals(6.7, child5.getTranslationY(), 0.1);
        }
    }

    @Test
    public void testStraightOffsetting() throws Throwable {
        ((CurvingLayoutCallback) mWearableLinearLayoutManagerUnderTest.getLayoutCallback())
                .setRound(
                false);
        final AtomicReference<WearableRecyclerView> wrvReference = new AtomicReference<>();
        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                WearableRecyclerView wrv =
                        (WearableRecyclerView) mActivityRule.getActivity().findViewById(R.id.wrv);
                wrv.setLayoutManager(mWearableLinearLayoutManagerUnderTest);
                wrvReference.set(wrv);
            }
        });
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
        WearableRecyclerView wrv = wrvReference.get();

        View child1 = wrv.getChildAt(0);
        View child2 = wrv.getChildAt(1);
        View child3 = wrv.getChildAt(2);
        View child4 = wrv.getChildAt(3);
        View child5 = wrv.getChildAt(4);

        // The left position and the translation of the child is not modified if the screen is
        // straight. Check if the 5th child is not null as some devices will not be able to display
        // 5 views.
        assertEquals(0, child1.getLeft());
        assertEquals(0.0f, child1.getTranslationY(), 0);

        assertEquals(0, child2.getLeft());
        assertEquals(0.0f, child2.getTranslationY(), 0);

        assertEquals(0, child3.getLeft());
        assertEquals(0.0f, child3.getTranslationY(), 0);

        assertEquals(0, child4.getLeft());
        assertEquals(0.0f, child4.getTranslationY(), 0);

        if (child5 != null) {
            assertEquals(0, child5.getLeft());
            assertEquals(0.0f, child5.getTranslationY(), 0);
        }
    }
}
