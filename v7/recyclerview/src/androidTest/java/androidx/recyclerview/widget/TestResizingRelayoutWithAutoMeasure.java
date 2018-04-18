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
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;

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
    private final int mRvWidth;
    private final int mRvHeight;
    private final RecyclerView.LayoutManager mLayoutManager;
    private final float mWidthMultiplier;
    private final float mHeightMultiplier;

    public TestResizingRelayoutWithAutoMeasure(@SuppressWarnings("UnusedParameters") String name,
            int rvWidth, int rvHeight,
            RecyclerView.LayoutManager layoutManager, float widthMultiplier,
            float heightMultiplier) {
        mRvWidth = rvWidth;
        mRvHeight = rvHeight;
        mLayoutManager = layoutManager;
        mWidthMultiplier = widthMultiplier;
        mHeightMultiplier = heightMultiplier;
    }

    @Parameterized.Parameters(name = "{0} rv w/h:{1}/{2} changed w/h:{4}/{5}")
    public static List<Object[]> getParams() {
        List<Object[]> params = new ArrayList<>();
        for(int[] rvSize : new int[][]{new int[]{200, 200}, new int[]{200, 100},
                new int[]{100, 200}}) {
            for (float w : new float[]{.5f, 1f, 2f}) {
                for (float h : new float[]{.5f, 1f, 2f}) {
                    params.add(
                            new Object[]{"linear layout", rvSize[0], rvSize[1],
                                    new LinearLayoutManager(null), w, h}
                    );
                    params.add(
                            new Object[]{"grid layout", rvSize[0], rvSize[1],
                                    new GridLayoutManager(null, 3), w, h}
                    );
                    params.add(
                            new Object[]{"staggered", rvSize[0], rvSize[1],
                                    new StaggeredGridLayoutManager(3,
                                    StaggeredGridLayoutManager.VERTICAL), w, h}
                    );
                }
            }
        }
        return params;
    }

    @Test
    public void testResizeDuringMeasurements() throws Throwable {
        final WrappedRecyclerView recyclerView = new WrappedRecyclerView(getActivity());
        recyclerView.setLayoutManager(mLayoutManager);
        StaticAdapter adapter = new StaticAdapter(50, ViewGroup.LayoutParams.MATCH_PARENT,
                mRvHeight / 5);
        recyclerView.setLayoutParams(new FrameLayout.LayoutParams(mRvWidth, mRvHeight));
        recyclerView.setAdapter(adapter);
        setRecyclerView(recyclerView);
        getInstrumentation().waitForIdleSync();
        assertThat("Test sanity", recyclerView.getChildCount() > 0, is(true));
        final int lastPosition = recyclerView.getAdapter().getItemCount() - 1;
        smoothScrollToPosition(lastPosition);
        assertThat("test sanity", recyclerView.findViewHolderForAdapterPosition(lastPosition),
                notNullValue());
        assertThat("test sanity", mRvWidth, is(recyclerView.getWidth()));
        assertThat("test sanity", mRvHeight, is(recyclerView.getHeight()));
        recyclerView.waitUntilLayout();
        recyclerView.waitUntilAnimations();
        final Map<Integer, Rect> startPositions = capturePositions(recyclerView);
        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                recyclerView.measure(
                        makeMeasureSpec((int) (mRvWidth * mWidthMultiplier),
                                mWidthMultiplier == 1f ? EXACTLY : AT_MOST),
                        makeMeasureSpec((int) (mRvHeight * mHeightMultiplier),
                                mHeightMultiplier == 1f ? EXACTLY : AT_MOST));

                recyclerView.measure(
                        makeMeasureSpec(mRvWidth, EXACTLY),
                        makeMeasureSpec(mRvHeight, EXACTLY));
                recyclerView.dispatchLayout();
                Map<Integer, Rect> endPositions = capturePositions(recyclerView);
                assertStartItemPositions(startPositions, endPositions);
            }
        });
        recyclerView.waitUntilLayout();
        recyclerView.waitUntilAnimations();
        checkForMainThreadException();
    }

    private void assertStartItemPositions(Map<Integer, Rect> startPositions,
            Map<Integer, Rect> endPositions) {
        String log = log(startPositions, endPositions);
        for (Map.Entry<Integer, Rect> entry : startPositions.entrySet()) {
            Rect rect = endPositions.get(entry.getKey());
            assertThat(log + "view for position " + entry.getKey() + " at" + entry.getValue(), rect,
                    notNullValue());
            assertThat(log + "rect for position " + entry.getKey(), entry.getValue(), is(rect));
        }
    }

    @NonNull
    private String log(Map<Integer, Rect> startPositions, Map<Integer, Rect> endPositions) {
        StringBuilder logBuilder = new StringBuilder();
        for (Map.Entry<Integer, Rect> entry : startPositions.entrySet()) {
            logBuilder.append(entry.getKey()).append(":").append(entry.getValue()).append("\n");
        }
        logBuilder.append("------\n");
        for (Map.Entry<Integer, Rect> entry : endPositions.entrySet()) {
            logBuilder.append(entry.getKey()).append(":").append(entry.getValue()).append("\n");
        }
        return logBuilder.toString();
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

    private class StaticAdapter extends RecyclerView.Adapter<TestViewHolder> {
        final int mSize;
        // is passed to the layout params of the item
        final int mMinItemWidth;
        final int mMinItemHeight;

        public StaticAdapter(int size, int minItemWidth, int minItemHeight) {
            mSize = size;
            mMinItemWidth = minItemWidth;
            mMinItemHeight = minItemHeight;
        }

        @Override
        public TestViewHolder onCreateViewHolder(@NonNull ViewGroup parent,
                int viewType) {
            return new TestViewHolder(new View(parent.getContext()));
        }

        @Override
        public void onBindViewHolder(@NonNull TestViewHolder holder, int position) {
            holder.mBoundItem = new Item(position, "none");
            if (mMinItemHeight < 1 && mMinItemWidth < 1) {
                return;
            }
            ViewGroup.LayoutParams lp = holder.itemView.getLayoutParams();
            if (lp == null) {
                lp = new ViewGroup.LayoutParams(0, 0);
            }
            if (mMinItemWidth > 0) {
                lp.width = (int) (mMinItemWidth + (position % 10) * mMinItemWidth / 7f);
            } else {
                lp.width = mMinItemWidth;
            }

            if (mMinItemHeight > 0) {
                lp.height = (int) (mMinItemHeight + (position % 10) * mMinItemHeight / 7f);
            } else {
                lp.height = mMinItemHeight;
            }
            holder.itemView.setLayoutParams(lp);
        }

        @Override
        public int getItemCount() {
            return mSize;
        }
    }
}
