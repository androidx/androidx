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
import static android.view.View.MeasureSpec.makeMeasureSpec;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import android.graphics.Rect;
import android.support.test.filters.MediumTest;
import android.support.test.runner.AndroidJUnit4;
import android.view.View;

import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests scroll to position with wrap content to make sure that LayoutManagers can keep track of
 * the position if layout is called multiple times.
 */
@MediumTest
@RunWith(AndroidJUnit4.class)
public class ScrollToPositionWithAutoMeasure extends BaseRecyclerViewInstrumentationTest {
    @Test
    public void testLinearLayoutManager() throws Throwable {
        LinearLayoutManager llm = new LinearLayoutManager(getActivity());
        llm.ensureLayoutState();
        test(llm, llm.mOrientationHelper);
    }

    @Test
    public void testGridLayoutManager() throws Throwable {
        GridLayoutManager glm = new GridLayoutManager(getActivity(), 3);
        glm.ensureLayoutState();
        test(glm, glm.mOrientationHelper);
    }

    @Test
    public void testStaggeredGridLayoutManager() throws Throwable {
        StaggeredGridLayoutManager sglm = new StaggeredGridLayoutManager(3,
                StaggeredGridLayoutManager.VERTICAL);
        test(sglm, sglm.mPrimaryOrientation);
    }

    public void test(final RecyclerView.LayoutManager llm,
            final OrientationHelper orientationHelper) throws Throwable {
        final RecyclerView recyclerView = new RecyclerView(getActivity());
        recyclerView.setLayoutManager(llm);
        recyclerView.setAdapter(new TestAdapter(1000));
        setRecyclerView(recyclerView);
        getInstrumentation().waitForIdleSync();
        assertThat("Test sanity", recyclerView.getChildCount() > 0, is(true));
        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                View lastChild = llm.getChildAt(llm.getChildCount() - 1);
                int lastChildPos = recyclerView.getChildAdapterPosition(lastChild);
                int targetPos = lastChildPos * 2;
                llm.scrollToPosition(targetPos);
                recyclerView.measure(
                        makeMeasureSpec(recyclerView.getWidth(), EXACTLY),
                        makeMeasureSpec(recyclerView.getHeight(), AT_MOST));
                assertThat(recyclerView.findViewHolderForAdapterPosition(targetPos),
                        notNullValue());
                // make sure it is still visible from top at least
                int size = orientationHelper.getDecoratedMeasurement(
                        recyclerView.findViewHolderForAdapterPosition(targetPos).itemView);
                recyclerView.measure(
                        makeMeasureSpec(recyclerView.getWidth(), EXACTLY),
                        makeMeasureSpec(size + 1, EXACTLY));
                recyclerView.layout(0, 0, recyclerView.getMeasuredWidth(),
                        recyclerView.getMeasuredHeight());
                assertThat(recyclerView.findViewHolderForAdapterPosition(targetPos),
                        notNullValue());
                RecyclerView.ViewHolder viewHolder =
                        recyclerView.findViewHolderForAdapterPosition(targetPos);
                assertThat(viewHolder, notNullValue());
                Rect viewBounds = new Rect();
                llm.getDecoratedBoundsWithMargins(viewHolder.itemView, viewBounds);
                Rect rvBounds = new Rect(0, 0, llm.getWidth(), llm.getHeight());
                assertThat(rvBounds + " vs " + viewBounds, rvBounds.contains(viewBounds), is(true));
            }
        });
    }
}