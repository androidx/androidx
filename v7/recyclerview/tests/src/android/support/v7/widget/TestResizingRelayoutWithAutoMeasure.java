/*
 * Copyright (C) 2016 The Android Open Source Project
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

import static android.view.View.MeasureSpec.AT_MOST;
import static android.view.View.MeasureSpec.EXACTLY;
import static android.view.View.MeasureSpec.makeMeasureSpec;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import android.graphics.Rect;
import android.test.suitebuilder.annotation.MediumTest;
import android.view.View;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Tests whether the layout manager can keep its children positions properly after it is re-laid
 * out with larger/smaller intermediate size but the same final size.
 */
@MediumTest
@RunWith(Parameterized.class)
public class TestResizingRelayoutWithAutoMeasure extends BaseRecyclerViewInstrumentationTest {
    private final RecyclerView.LayoutManager mLayoutManager;
    private final float mWidthMultiplier;
    private final float mHeightMultiplier;

    public TestResizingRelayoutWithAutoMeasure(@SuppressWarnings("UnusedParameters") String name,
            RecyclerView.LayoutManager layoutManager, float widthMultiplier,
            float heightMultiplier) {
        mLayoutManager = layoutManager;
        mWidthMultiplier = widthMultiplier;
        mHeightMultiplier = heightMultiplier;
    }

    @Parameterized.Parameters(name = "{0} w:{2} h:{3}")
    public static List<Object[]> getParams() {
        List<Object[]> params = new ArrayList<>();
        for (float w : new float[]{.5f, 1f, 2f}) {
            for (float h : new float[]{.5f, 1f, 2f}) {
                params.add(
                        new Object[]{"linear layout", new LinearLayoutManager(null), w, h}
                );
                params.add(
                        new Object[]{"grid layout", new GridLayoutManager(null, 3), w, h}
                );
                params.add(
                        new Object[]{"staggered", new StaggeredGridLayoutManager(3,
                                StaggeredGridLayoutManager.VERTICAL), w, h}
                );
            }
        }
        return params;
    }

    @Test
    public void testResizeDuringMeasurements() throws Throwable {
        final RecyclerView recyclerView = new RecyclerView(getActivity());
        recyclerView.setLayoutManager(mLayoutManager);
        recyclerView.setAdapter(new TestAdapter(500));
        setRecyclerView(recyclerView);
        getInstrumentation().waitForIdleSync();
        assertThat("Test sanity", recyclerView.getChildCount() > 0, is(true));
        final int lastPosition = recyclerView.getAdapter().getItemCount() - 1;
        smoothScrollToPosition(lastPosition);
        assertThat("test sanity", recyclerView.findViewHolderForAdapterPosition(lastPosition),
                notNullValue());
        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                int startHeight = recyclerView.getMeasuredHeight();
                int startWidth = recyclerView.getMeasuredWidth();
                Map<Integer, Rect> startPositions = capturePositions(recyclerView);
                recyclerView.measure(
                        makeMeasureSpec((int) (startWidth * mWidthMultiplier),
                                mWidthMultiplier == 1f ? EXACTLY : AT_MOST),
                        makeMeasureSpec((int) (startHeight * mHeightMultiplier),
                                mHeightMultiplier == 1f ? EXACTLY : AT_MOST));

                recyclerView.measure(
                        makeMeasureSpec(startWidth, EXACTLY),
                        makeMeasureSpec(startHeight, EXACTLY));
                recyclerView.dispatchLayout();
                Map<Integer, Rect> endPositions = capturePositions(recyclerView);
                assertStartItemPositions(startPositions, endPositions);
            }
        });
    }

    private void assertStartItemPositions(Map<Integer, Rect> startPositions,
            Map<Integer, Rect> endPositions) {
        for (Map.Entry<Integer, Rect> entry : startPositions.entrySet()) {
            Rect rect = endPositions.get(entry.getKey());
            assertThat("view for position " + entry.getKey() + " at" + entry.getValue(), rect,
                    notNullValue());
            assertThat("rect for position " + entry.getKey(), entry.getValue(), is(rect));
        }
    }

    private Map<Integer, Rect> capturePositions(RecyclerView recyclerView) {
        Map<Integer, Rect> positions = new HashMap<>();
        for (int i = 0; i < mLayoutManager.getChildCount(); i++) {
            View view = mLayoutManager.getChildAt(i);
            int childAdapterPosition = recyclerView.getChildAdapterPosition(view);
            Rect outRect = new Rect();
            mLayoutManager.getDecoratedBoundsWithMargins(view, outRect);
            // only record if outRect is visible
            if (outRect.left >= mRecyclerView.getWidth() ||
                    outRect.top >= mRecyclerView.getHeight() ||
                    outRect.right < 0 ||
                    outRect.bottom < 0) {
                continue;
            }
            positions.put(childAdapterPosition, outRect);
        }
        return positions;
    }
}