/*
 * Copyright (C) 2015 The Android Open Source Project
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

package android.support.v7.widget;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.support.v7.gridlayout.test.R;
import android.test.suitebuilder.annotation.SmallTest;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class GridLayoutTest {
    @Rule public final ActivityTestRule<GridLayoutTestActivity> mActivityTestRule;

    private View mLeftView;
    private View mRightView;
    private View mGridView;

    public GridLayoutTest() {
        mActivityTestRule = new ActivityTestRule<>(GridLayoutTestActivity.class);
    }

    private void setContentView(final int layoutId) {
        final Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        instrumentation.runOnMainSync(new Runnable() {
            @Override
            public void run() {
                final Activity activity = mActivityTestRule.getActivity();
                activity.setContentView(layoutId);
                // Now that we've set the content view, find the views we'll be testing
                mLeftView = activity.findViewById(R.id.leftView);
                mRightView = activity.findViewById(R.id.rightView);
                mGridView = activity.findViewById(R.id.gridView);
            }
        });
        instrumentation.waitForIdleSync();
    }

    @Test
    public void testUseDefaultMargin() {
        setContentView(R.layout.use_default_margin_test);
        int left = mLeftView.getWidth();
        int right = mRightView.getWidth();
        int total = mGridView.getWidth();
        assertTrue("left item should get some width", left > 0);
        assertTrue("right item should get some width", right > 0);
        assertTrue("test sanity", total > 0);
        assertTrue("left view should be almost two times right view " + left + " vs " + right,
                Math.abs(right * 2 - left) < 2);
    }

    @Test
    public void testImplicitFillHorizontal() {
        setContentView(R.layout.fill_horizontal_test);
        int left = mLeftView.getWidth();
        int right = mRightView.getWidth();
        int total = mGridView.getWidth();
        assertTrue("left item should get some width", left > 0);
        assertTrue("right item should get some width", right > 0);
        assertTrue("test sanity", total > 0);
        assertTrue("left view should be almost two times right view " + left + " vs " + right,
                Math.abs(right * 2 - left) < 2);
    }

    @Test
    public void testMakeViewGone() {
        setContentView(R.layout.make_view_gone_test);
        int left = mLeftView.getWidth();
        int right = mRightView.getWidth();
        int total = mGridView.getWidth();
        assertTrue("left item should get some width", left > 0);
        assertTrue("right item should get some width", right > 0);
        assertTrue("test sanity", total > 0);
        // set second view to gone
        final Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        instrumentation.runOnMainSync(new Runnable() {
            @Override
            public void run() {
                final View rightView = mActivityTestRule.getActivity().findViewById(R.id.rightView);
                GridLayout.LayoutParams lp = (GridLayout.LayoutParams) rightView.getLayoutParams();
                lp.setGravity(Gravity.NO_GRAVITY);
                rightView.setVisibility(View.GONE);
            }
        });
        instrumentation.waitForIdleSync();
        left = mActivityTestRule.getActivity().findViewById(R.id.leftView).getWidth();
        assertEquals(total, left);
    }

    @Test
    public void testWrapContentInOtherDirection() {
        setContentView(R.layout.height_wrap_content_test);
        int left = mLeftView.getHeight();
        int right = mRightView.getHeight();
        int total = mGridView.getHeight();
        assertTrue("test sanity", left > 0);
        assertTrue("test sanity", right > 0);
        assertTrue("test sanity", total > 0);
        assertTrue("right should be taller than left", right >= left);
        assertTrue("total height should be smaller than what it could be",
                total < ((ViewGroup) mGridView.getParent()).getHeight());
    }

    @Test
    public void testGenerateLayoutParamsFromMarginParams() {
        MyGridLayout gridLayout = new MyGridLayout(mActivityTestRule.getActivity());
        ViewGroup.MarginLayoutParams lp = new ViewGroup.MarginLayoutParams(3, 5);
        lp.leftMargin = 1;
        lp.topMargin = 2;
        lp.rightMargin = 3;
        lp.bottomMargin = 4;
        GridLayout.LayoutParams generated = gridLayout.generateLayoutParams(lp);
        assertEquals(3, generated.width);
        assertEquals(5, generated.height);

        assertEquals(1, generated.leftMargin);
        assertEquals(2, generated.topMargin);
        assertEquals(3, generated.rightMargin);
        assertEquals(4, generated.bottomMargin);
    }

    private static class MyGridLayout extends GridLayout {

        public MyGridLayout(Context context) {
            super(context);
        }

        public MyGridLayout(Context context, AttributeSet attrs) {
            super(context, attrs);
        }

        public MyGridLayout(Context context, AttributeSet attrs, int defStyleAttr) {
            super(context, attrs, defStyleAttr);
        }

        @Override
        protected LayoutParams generateLayoutParams(ViewGroup.LayoutParams p) {
            return super.generateLayoutParams(p);
        }
    }
}
