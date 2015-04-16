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

package android.support.v7.widget.test;

import android.app.Activity;
import android.os.Debug;
import android.support.v7.widget.GridLayout;
import android.test.ActivityInstrumentationTestCase2;
import android.support.v7.gridlayout.R;
import android.test.UiThreadTest;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

/**
 * @hide
 */
public class GridLayoutTest extends ActivityInstrumentationTestCase2 {

    public GridLayoutTest() {
        super("android.support.v7.widget.test", GridLayoutTestActivity.class);
    }

    private void setContentView(final int layoutId) throws Throwable {
        final Activity activity = getActivity();
        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                activity.setContentView(layoutId);
            }
        });
    }

    public void testUseDefaultMargin() throws Throwable {
        setContentView(R.layout.use_default_margin_test);
        getInstrumentation().waitForIdleSync();
        int left = getActivity().findViewById(R.id.leftView).getWidth();
        int right = getActivity().findViewById(R.id.rightView).getWidth();
        int total = getActivity().findViewById(R.id.gridView).getWidth();
        assertTrue("left item should get some width", left > 0);
        assertTrue("right item should get some width", right > 0);
        assertTrue("test sanity", total > 0);
        assertTrue("left view should be almost two times right view " + left + " vs " + right,
                Math.abs(right * 2 - left) < 2);
    }

    public void testImplicitFillHorizontal() throws Throwable {
        setContentView(R.layout.fill_horizontal_test);
        getInstrumentation().waitForIdleSync();
        int left = getActivity().findViewById(R.id.leftView).getWidth();
        int right = getActivity().findViewById(R.id.rightView).getWidth();
        int total = getActivity().findViewById(R.id.gridView).getWidth();
        assertTrue("left item should get some width", left > 0);
        assertTrue("right item should get some width", right > 0);
        assertTrue("test sanity", total > 0);
        assertTrue("left view should be almost two times right view " + left + " vs " + right,
                Math.abs(right * 2 - left) < 2);
    }

    public void testMakeViewGone() throws Throwable {
        setContentView(R.layout.make_view_gone_test);
        getInstrumentation().waitForIdleSync();
        int left = getActivity().findViewById(R.id.leftView).getWidth();
        final int right = getActivity().findViewById(R.id.rightView).getWidth();
        int total = getActivity().findViewById(R.id.gridView).getWidth();
        assertTrue("left item should get some width", left > 0);
        assertTrue("right item should get some width", right > 0);
        assertTrue("test sanity", total > 0);
        // set second view to gone
        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                final View rightView = getActivity().findViewById(R.id.rightView);
                GridLayout.LayoutParams lp = (GridLayout.LayoutParams) rightView.getLayoutParams();
                lp.setGravity(Gravity.NO_GRAVITY);
                rightView.setVisibility(View.GONE);
            }
        });
        getInstrumentation().waitForIdleSync();
        left = getActivity().findViewById(R.id.leftView).getWidth();
        assertEquals(total, left);
    }
    public void testWrapContentInOtherDirection() throws Throwable {
        setContentView(R.layout.height_wrap_content_test);
        getInstrumentation().waitForIdleSync();
        int left = getActivity().findViewById(R.id.leftView).getHeight();
        final int right = getActivity().findViewById(R.id.rightView).getHeight();
        final View gridView = getActivity().findViewById(R.id.gridView);
        int total = gridView.getHeight();
        assertTrue("test sanity", left > 0);
        assertTrue("test sanity", right > 0);
        assertTrue("test sanity", total > 0);
        assertTrue("right should be taller than left", right > left);
        assertTrue("total height should be smaller than what it could be",
                total < ((ViewGroup)gridView.getParent()).getHeight());

    }
}
