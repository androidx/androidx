/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.core.widget;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.support.v4.BaseInstrumentationTestCase;
import android.view.View;
import android.view.ViewGroup;

import androidx.core.test.R;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Large integration tests that verify correct NestedScrollView scrolling behavior,
 * including interaction with nested scrolling.
*/
@RunWith(AndroidJUnit4.class)
@LargeTest
public class NestedScrollViewSmoothScrollByTest extends
        BaseInstrumentationTestCase<TestContentViewActivity> {

    private static final int CHILD_HEIGHT = 800;
    private static final int NSV_HEIGHT = 400;
    private static final int WIDTH = 400;
    private static final int TOTAL_SCROLL_DISTANCE = CHILD_HEIGHT - NSV_HEIGHT;

    private NestedScrollView mNestedScrollView;
    private View mChild;

    public NestedScrollViewSmoothScrollByTest() {
        super(TestContentViewActivity.class);
    }

    @Before
    public void setup() {
        Context context = mActivityTestRule.getActivity();

        mChild = new View(context);
        mChild.setMinimumWidth(WIDTH);
        mChild.setMinimumHeight(CHILD_HEIGHT);
        mChild.setLayoutParams(new ViewGroup.LayoutParams(WIDTH, CHILD_HEIGHT));
        mChild.setBackgroundDrawable(
                new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                        new int[]{0xFFFF0000, 0xFF00FF00}));

        mNestedScrollView = new NestedScrollView(context);
        mNestedScrollView.setLayoutParams(new ViewGroup.LayoutParams(WIDTH, NSV_HEIGHT));
        mNestedScrollView.setBackgroundColor(0xFF0000FF);
        mNestedScrollView.addView(mChild);
    }

    @Test
    public void smoothScrollBy_scrollsEntireDistanceIncludingMargins() throws Throwable {
        setChildMargins(20, 30);
        attachToActivity();

        final CountDownLatch countDownLatch = new CountDownLatch(1);
        final int expectedTarget = TOTAL_SCROLL_DISTANCE + 20 + 30;
        final int scrollDistance = TOTAL_SCROLL_DISTANCE + 20 + 30;
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mNestedScrollView.setOnScrollChangeListener(
                        new NestedScrollView.OnScrollChangeListener() {
                            @Override
                            public void onScrollChange(NestedScrollView v, int scrollX, int scrollY,
                                    int oldScrollX, int oldScrollY) {
                                if (scrollY == expectedTarget) {
                                    countDownLatch.countDown();
                                }
                            }
                        });
                mNestedScrollView.smoothScrollBy(0, scrollDistance);
            }
        });
        assertThat(countDownLatch.await(2, TimeUnit.SECONDS), is(true));

        assertThat(mNestedScrollView.getScrollY(), is(expectedTarget));
    }

    @SuppressWarnings("SameParameterValue")
    private void setChildMargins(int top, int bottom) {
        ViewGroup.LayoutParams currentLayoutParams = mChild.getLayoutParams();
        NestedScrollView.LayoutParams childLayoutParams = new NestedScrollView.LayoutParams(
                currentLayoutParams.width, currentLayoutParams.height);
        childLayoutParams.topMargin = top;
        childLayoutParams.bottomMargin = bottom;
        mChild.setLayoutParams(childLayoutParams);
    }

    private void attachToActivity() throws Throwable {
        final TestContentView testContentView =
                mActivityTestRule.getActivity().findViewById(R.id.testContentView);
        testContentView.expectLayouts(1);
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                testContentView.addView(mNestedScrollView);
            }
        });
        testContentView.awaitLayouts(2);
    }
}
