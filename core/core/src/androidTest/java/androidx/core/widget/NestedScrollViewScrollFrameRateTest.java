/*
 * Copyright 2024 The Android Open Source Project
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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;

import android.content.Context;
import android.os.Build;
import android.support.v4.BaseInstrumentationTestCase;
import android.view.View;
import android.view.ViewGroup;

import androidx.core.test.R;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.MediumTest;
import androidx.test.filters.SdkSuppress;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@MediumTest
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.VANILLA_ICE_CREAM)
@RunWith(AndroidJUnit4.class)
public class NestedScrollViewScrollFrameRateTest extends
        BaseInstrumentationTestCase<TestContentViewActivity>  {

    private static final int CHILD_HEIGHT = 800;
    private static final int NSV_HEIGHT = 400;
    private static final int WIDTH = 400;
    private static final int TOTAL_SCROLL_DISTANCE = CHILD_HEIGHT - NSV_HEIGHT;

    private NestedScrollView mNestedScrollView;
    private View mChild;

    public NestedScrollViewScrollFrameRateTest() {
        super(TestContentViewActivity.class);
    }

    @Before
    public void setup() {
        Context context = mActivityTestRule.getActivity();

        mChild = new View(context);
        mChild.setMinimumWidth(WIDTH);
        mChild.setMinimumHeight(CHILD_HEIGHT);
        mChild.setLayoutParams(new ViewGroup.LayoutParams(WIDTH, CHILD_HEIGHT));

        mNestedScrollView = new NestedScrollView(context);
        mNestedScrollView.setLayoutParams(new ViewGroup.LayoutParams(WIDTH, NSV_HEIGHT));
        mNestedScrollView.addView(mChild);
    }

    @Test
    public void smoothScrollByFrameRateBoost() throws Throwable {
        setChildMargins(20, 30);
        attachToActivity();

        final int scrollDistance = TOTAL_SCROLL_DISTANCE + 20 + 30;
        mActivityTestRule.runOnUiThread(() -> {
            mNestedScrollView.setOnScrollChangeListener(
                    (NestedScrollView.OnScrollChangeListener) (v, scrollX, scrollY,
                            oldScrollX, oldScrollY) -> {
                        if (scrollY >= oldScrollX + 10) {
                            assertThat(mNestedScrollView.getFrameContentVelocity(),
                                    greaterThan(0f));
                        }
                    });
            mNestedScrollView.smoothScrollBy(0, scrollDistance);
        });
    }

    @Test
    public void flingFrameRateBoost() throws Throwable {
        setChildMargins(20, 30);
        attachToActivity();

        mActivityTestRule.runOnUiThread(() -> {
            mNestedScrollView.setOnScrollChangeListener(
                    (NestedScrollView.OnScrollChangeListener) (v, scrollX, scrollY,
                            oldScrollX, oldScrollY) -> {
                        if (scrollY >= oldScrollX + 10) {
                            assertThat(mNestedScrollView.getFrameContentVelocity(),
                                    greaterThan(0f));
                        }
                    });
            mNestedScrollView.fling(1000);
        });
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
        mActivityTestRule.runOnUiThread(() -> testContentView.addView(mNestedScrollView));
        testContentView.awaitLayouts(2);
    }
}
