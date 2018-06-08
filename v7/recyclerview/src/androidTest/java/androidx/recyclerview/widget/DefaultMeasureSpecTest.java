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

package androidx.recyclerview.widget;

import static android.view.View.MeasureSpec.AT_MOST;
import static android.view.View.MeasureSpec.EXACTLY;
import static android.view.View.MeasureSpec.UNSPECIFIED;
import static android.view.View.MeasureSpec.makeMeasureSpec;

import android.graphics.Rect;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.SmallTest;
import android.view.View;

import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.List;

@RunWith(Parameterized.class)
@SmallTest
public class DefaultMeasureSpecTest {
    private final int mWSpec;
    private final int mHSpec;
    private int mExpectedW;
    private int mExpectedH;
    private final Rect mPadding;
    RecyclerView mRecyclerView;

    @Before
    public void setUp() throws Exception {
        mRecyclerView = new RecyclerView(InstrumentationRegistry.getContext());
        if (mPadding != null) {
            mRecyclerView.setPadding(mPadding.left, mPadding.top, mPadding.right, mPadding.bottom);
        }
    }

    @Parameterized.Parameters(name = "{0}")
    public static List<Object[]> param() {
        List<Object[]> params = Arrays.asList(
                new Object[]{null, makeMeasureSpec(10, EXACTLY), makeMeasureSpec(20, EXACTLY), 10,
                        20, new Rect(0, 0, 0, 0)},
                new Object[]{null, makeMeasureSpec(10, EXACTLY), makeMeasureSpec(100, AT_MOST), 10,
                        0, new Rect(0, 0, 0, 0)},
                new Object[]{null, makeMeasureSpec(10, AT_MOST), makeMeasureSpec(100, EXACTLY), 0,
                        100, new Rect(0, 0, 0, 0)},
                new Object[]{null, makeMeasureSpec(10, EXACTLY), makeMeasureSpec(100, UNSPECIFIED),
                        10, 0, new Rect(0, 0, 0, 0)},
                new Object[]{null, makeMeasureSpec(10, UNSPECIFIED), makeMeasureSpec(100, EXACTLY),
                        0, 100, new Rect(0, 0, 0, 0)},
                new Object[]{null, makeMeasureSpec(10, EXACTLY), makeMeasureSpec(20, EXACTLY), 10,
                        20, new Rect(39, 50, 34, 23)},
                new Object[]{null, makeMeasureSpec(10, EXACTLY), makeMeasureSpec(100, AT_MOST), 10,
                        50, new Rect(3, 35, 3, 15)},
                new Object[]{null, makeMeasureSpec(10, EXACTLY), makeMeasureSpec(100, AT_MOST), 10,
                        100, new Rect(3, 350, 3, 15)},

                new Object[]{null, makeMeasureSpec(10, AT_MOST), makeMeasureSpec(100, EXACTLY), 10,
                        100, new Rect(15, 500, 5, 30)},
                new Object[]{null, makeMeasureSpec(10, AT_MOST), makeMeasureSpec(100, EXACTLY), 5,
                        100, new Rect(3, 500, 2, 30)},
                new Object[]{null, makeMeasureSpec(10, EXACTLY), makeMeasureSpec(100, UNSPECIFIED),
                        10, 20, new Rect(500, 15, 30, 5)},
                new Object[]{null, makeMeasureSpec(10, UNSPECIFIED), makeMeasureSpec(100, EXACTLY),
                        45, 100, new Rect(15, 400, 30, 5)}
        );
        for (Object[] param : params) {
            param[0] = "width: " + log((Integer) param[1]) + ", height:" + log((Integer) param[2]);
            param[0] = param[0] + ", padding:" + param[5];
        }
        return params;
    }

    public DefaultMeasureSpecTest(@SuppressWarnings("UnusedParameters") String ignored,
            int wSpec, int hSpec, int expectedW, int expectedH, Rect padding) {
        mWSpec = wSpec;
        mHSpec = hSpec;
        mExpectedW = expectedW;
        mExpectedH = expectedH;
        this.mPadding = padding;
    }

    @Test
    public void testWithSmallerMinWidth() {
        mRecyclerView.setMinimumWidth(Math.max(0, mExpectedW - 5));
        runTest();
    }

    @Test
    public void testWithSmallerMinHeight() {
        mRecyclerView.setMinimumHeight(Math.max(0, mExpectedH - 5));
        runTest();
    }

    @Test
    public void testWithLargerMinHeight() {
        mRecyclerView.setMinimumHeight(mExpectedH + 5);
        int mode = View.MeasureSpec.getMode(mHSpec);
        switch (mode) {
            case UNSPECIFIED:
                mExpectedH += 5;
                break;
            case AT_MOST:
                mExpectedH = Math.min(View.MeasureSpec.getSize(mHSpec), mExpectedH + 5);
                break;
        }
        runTest();
    }

    @Test
    public void testWithLargerMinWidth() {
        mRecyclerView.setMinimumWidth(mExpectedW + 5);
        int mode = View.MeasureSpec.getMode(mWSpec);
        switch (mode) {
            case UNSPECIFIED:
                mExpectedW += 5;
                break;
            case AT_MOST:
                mExpectedW = Math.min(View.MeasureSpec.getSize(mWSpec), mExpectedW + 5);
                break;
        }
        runTest();
    }

    @Test
    public void runTest() {
        mRecyclerView.defaultOnMeasure(mWSpec, mHSpec);
        MatcherAssert.assertThat("measured width", mRecyclerView.getMeasuredWidth(),
                CoreMatchers.is(mExpectedW));
        MatcherAssert.assertThat("measured height", mRecyclerView.getMeasuredHeight(),
                CoreMatchers.is(mExpectedH));
    }

    private static String log(int spec) {
        final int size = View.MeasureSpec.getSize(spec);
        int mode = View.MeasureSpec.getMode(spec);
        if (mode == View.MeasureSpec.AT_MOST) {
            return "at most " + size;
        }
        if (mode == View.MeasureSpec.UNSPECIFIED) {
            return "unspecified " + size;
        }
        if (mode == View.MeasureSpec.EXACTLY) {
            return "exactly " + size;
        }
        return "?? " + size;
    }
}
