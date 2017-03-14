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

package android.support.wearable.view;

import static junit.framework.Assert.assertEquals;

import android.support.test.InstrumentationRegistry;
import android.support.test.filters.MediumTest;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.support.wearable.test.R;
import android.support.wearable.view.util.WakeLockRule;
import android.view.View;
import android.widget.FrameLayout;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;

import java.util.HashMap;
import java.util.Map;

@MediumTest
@RunWith(AndroidJUnit4.class)
public class CurvedOffsettingLayoutManagerTest {

    @Rule
    public final WakeLockRule wakeLock = new WakeLockRule();

    @Rule
    public final ActivityTestRule<WearableRecyclerViewTestActivity> mActivityRule =
            new ActivityTestRule<>(WearableRecyclerViewTestActivity.class, true, true);

    CurvedOffsettingLayoutManager mCurvedOffsettingLayoutManagerUnderTest;

    @Before
    public void setUp() throws Throwable {
        MockitoAnnotations.initMocks(this);
        mCurvedOffsettingLayoutManagerUnderTest =
                new CurvedOffsettingLayoutManager(mActivityRule.getActivity());
    }

    @Test
    public void testOffsetting() throws Throwable {
        ViewFetchingRunnable customRunnable = new ViewFetchingRunnable(){
            @Override
            public void run() {
                WearableRecyclerView wrv =
                        (WearableRecyclerView) mActivityRule.getActivity().findViewById(R.id.wrv);
                wrv.setLayoutParams(new FrameLayout.LayoutParams(390, 390));
                wrv.invalidate();
                mIdViewMap.put(R.id.wrv, wrv);
            }
        };
        mActivityRule.runOnUiThread(customRunnable);
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
        WearableRecyclerView wrv = (WearableRecyclerView) customRunnable.mIdViewMap.get(R.id.wrv);
        int offset = wrv.getResources().getDimensionPixelSize(R.dimen.wrv_curve_default_x_offset);
        View child1 = wrv.getChildAt(0);
        View child2 = wrv.getChildAt(1);
        View child3 = wrv.getChildAt(2);
        View child4 = wrv.getChildAt(3);
        View child5 = wrv.getChildAt(4);

        // When the child is updated by the curved offsetting helper
        if (child1 != null) {
            mCurvedOffsettingLayoutManagerUnderTest.updateChild(child1, wrv);
        }
        if (child2 != null) {
            mCurvedOffsettingLayoutManagerUnderTest.updateChild(child2, wrv);
        }
        if (child3 != null) {
            mCurvedOffsettingLayoutManagerUnderTest.updateChild(child3, wrv);
        }
        if (child4 != null) {
            mCurvedOffsettingLayoutManagerUnderTest.updateChild(child4, wrv);
        }
        if (child5 != null) {
            mCurvedOffsettingLayoutManagerUnderTest.updateChild(child5, wrv);
        }
        if (wrv.getResources().getConfiguration().isScreenRound()) {
            // Then the left position and the translation of the child is modified if the screen is
            // round
            if (child1 != null) {
                assertEquals(162 - offset, child1.getLeft(), 1);
                assertEquals(-9.5, child1.getTranslationY(), 1);
            }
            if (child2 != null) {
                assertEquals(129 - offset, child2.getLeft(), 1);
                assertEquals(-16.7, child2.getTranslationY(), 1);
            }
            if (child3 != null) {
                assertEquals(99 - offset, child3.getLeft(), 1);
                assertEquals(-19.9, child3.getTranslationY(), 1);
            }
            if (child4 != null) {
                assertEquals(76 - offset, child4.getLeft(), 1);
                assertEquals(-17.9, child4.getTranslationY(), 1);
            }
            if (child5 != null) {
                assertEquals(59 - offset, child5.getLeft(), 1);
                assertEquals(-13, child5.getTranslationY(), 1);
            }
        } else {
            // Then the child is not modified if the screen is not round.
            if (child1 != null) {
                assertEquals(0, child1.getLeft());
                assertEquals(0.0f, child1.getTranslationY());
            }
            if (child2 != null) {
                assertEquals(0, child2.getLeft());
                assertEquals(0.0f, child2.getTranslationY());
            }
            if (child3 != null) {
                assertEquals(0, child3.getLeft());
                assertEquals(0.0f, child3.getTranslationY());
            }
            if (child4 != null) {
                assertEquals(0, child4.getLeft());
                assertEquals(0.0f, child4.getTranslationY());
            }
            if (child5 != null) {
                assertEquals(0, child5.getLeft());
                assertEquals(0.0f, child5.getTranslationY());
            }
        }
    }

    private abstract class ViewFetchingRunnable implements Runnable {
        Map<Integer, View> mIdViewMap = new HashMap();
    }
}
