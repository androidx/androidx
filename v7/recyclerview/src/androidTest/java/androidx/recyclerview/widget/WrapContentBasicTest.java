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

import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.MediumTest;
import android.support.test.runner.AndroidJUnit4;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@MediumTest
@RunWith(AndroidJUnit4.class)
public class WrapContentBasicTest {
    private Context mContext;
    private WrapContentLayoutManager mLayoutManager;
    private RecyclerView mRecyclerView;
    private WrapAdapter mAdapter;
    private static int WRAP = View.MeasureSpec.makeMeasureSpec(10, View.MeasureSpec.AT_MOST);
    private static int EXACT = View.MeasureSpec.makeMeasureSpec(10, View.MeasureSpec.EXACTLY);
    private static int UNSPECIFIED = View.MeasureSpec
            .makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);

    @Before
    public void setup() throws Exception {
        mContext = InstrumentationRegistry.getContext();
        mRecyclerView = new RecyclerView(mContext);
        mLayoutManager = spy(new WrapContentLayoutManager());
        // working around a mockito issue
        mRecyclerView.setLayoutManager(mLayoutManager);
        mAdapter = spy(new WrapAdapter());
        mRecyclerView.setAdapter(mAdapter);
    }

    @Test
    public void testLayoutInOnMeasureWithoutPredictive() {
        when(mLayoutManager.supportsPredictiveItemAnimations()).thenReturn(false);
        mRecyclerView.onMeasure(WRAP, WRAP);
        mRecyclerView.onMeasure(WRAP, WRAP);
        mRecyclerView.onLayout(true, 0, 10, 10, 10);
        verify(mLayoutManager, times(3))
                .onLayoutChildren(mRecyclerView.mRecycler, mRecyclerView.mState);
    }

    @Test
    public void dataChangeAfterMeasure() {
        mRecyclerView.onMeasure(WRAP, WRAP);
        mRecyclerView.onMeasure(WRAP, WRAP);
        mAdapter.notifyItemChanged(1);
        mRecyclerView.onLayout(true, 0, 10, 10, 10);
        verify(mLayoutManager, times(3))
                .onLayoutChildren(mRecyclerView.mRecycler, mRecyclerView.mState);
    }

    @Test
    public void setDimensionsFromChildren() {
        View[] children = createMockChildren(3);
        mLayoutManager.setMeasuredDimensionFromChildren(WRAP, WRAP);
        verify(mLayoutManager).setMeasuredDimension(children[0].getWidth(),
                children[0].getHeight());
    }

    @Test
    public void setDimensionsFromChildrenAnsSpec1() {
        View[] children = createMockChildren(3);
        int hSpec = View.MeasureSpec.makeMeasureSpec(111, View.MeasureSpec.EXACTLY);
        mLayoutManager.setMeasuredDimensionFromChildren(WRAP, hSpec);
        verify(mLayoutManager).setMeasuredDimension(children[0].getWidth(), 111);
    }

    @Test
    public void setDimensionsFromChildrenAnsSpec2() {
        View[] children = createMockChildren(3);
        int wSpec = View.MeasureSpec.makeMeasureSpec(111, View.MeasureSpec.EXACTLY);
        mLayoutManager.setMeasuredDimensionFromChildren(wSpec, WRAP);
        verify(mLayoutManager).setMeasuredDimension(111, children[0].getHeight());
    }

    @Test
    public void setDimensionsFromChildrenAnsSpec3() {
        View[] children = createMockChildren(3);
        children[0].layout(0, 0, 100, 100);
        children[1].layout(-5, 0, 100, 100);
        children[2].layout(-5, -10, 100, 100);
        mLayoutManager.setMeasuredDimensionFromChildren(UNSPECIFIED, UNSPECIFIED);
        verify(mLayoutManager).setMeasuredDimension(105, 110);
    }

    @Test
    public void setDimensionsFromChildrenAnsSpec4() {
        View[] children = createMockChildren(3);
        children[0].layout(0, 0, 100, 100);
        children[1].layout(-5, 0, 100, 100);
        children[2].layout(-5, -10, 100, 100);
        int atMost = View.MeasureSpec.makeMeasureSpec(95, View.MeasureSpec.AT_MOST);
        mLayoutManager.setMeasuredDimensionFromChildren(atMost, atMost);
        verify(mLayoutManager).setMeasuredDimension(95, 95);
    }

    @Test
    public void setDimensionsFromChildrenAnsSpec5() {
        View[] children = createMockChildren(3);
        children[0].layout(0, 0, 100, 100);
        children[1].layout(-5, 0, 100, 100);
        children[2].layout(-5, -10, 100, 100);
        mRecyclerView.setMinimumWidth(250);
        mLayoutManager.setMeasuredDimensionFromChildren(UNSPECIFIED, UNSPECIFIED);
        verify(mLayoutManager).setMeasuredDimension(250, 110);

        mRecyclerView.setMinimumWidth(5);
        mLayoutManager.setMeasuredDimensionFromChildren(UNSPECIFIED, UNSPECIFIED);
        verify(mLayoutManager).setMeasuredDimension(105, 110);
    }

    @Test
    public void setDimensionsFromChildrenAnsSpec6() {
        View[] children = createMockChildren(3);
        children[0].layout(0, 0, 100, 100);
        children[1].layout(-5, 0, 100, 100);
        children[2].layout(-5, -10, 100, 100);
        mRecyclerView.setMinimumHeight(250);
        mLayoutManager.setMeasuredDimensionFromChildren(UNSPECIFIED, UNSPECIFIED);
        verify(mLayoutManager).setMeasuredDimension(105, 250);

        mRecyclerView.setMinimumHeight(50);
        mLayoutManager.setMeasuredDimensionFromChildren(UNSPECIFIED, UNSPECIFIED);
        verify(mLayoutManager).setMeasuredDimension(105, 110);
    }

    private View[] createMockChildren(int count) {
        View[] views = new View[count];
        for (int i = 0; i < count; i++) {
            View v = new View(mContext);
            v.setLayoutParams(new RecyclerView.LayoutParams(1, 1));
            views[i] = v;
            when(mLayoutManager.getChildAt(i)).thenReturn(v);
        }
        when(mLayoutManager.getChildCount()).thenReturn(3);
        return views;
    }

    public class WrapContentLayoutManager extends RecyclerView.LayoutManager {

        @Override
        public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {

        }

        @Override
        public RecyclerView.LayoutParams generateDefaultLayoutParams() {
            return new RecyclerView.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        @Override
        public boolean isAutoMeasureEnabled() {
            return true;
        }

        // START MOCKITO OVERRIDES
        // We override package protected methods to make them public. This is necessary to run
        // mockito on Kitkat
        @Override
        public void setRecyclerView(RecyclerView recyclerView) {
            super.setRecyclerView(recyclerView);
        }

        @Override
        public void dispatchAttachedToWindow(RecyclerView view) {
            super.dispatchAttachedToWindow(view);
        }

        @Override
        public void dispatchDetachedFromWindow(RecyclerView view, RecyclerView.Recycler recycler) {
            super.dispatchDetachedFromWindow(view, recycler);
        }

        @Override
        public void setExactMeasureSpecsFrom(RecyclerView recyclerView) {
            super.setExactMeasureSpecsFrom(recyclerView);
        }

        @Override
        public void setMeasureSpecs(int wSpec, int hSpec) {
            super.setMeasureSpecs(wSpec, hSpec);
        }

        @Override
        public void setMeasuredDimensionFromChildren(int widthSpec, int heightSpec) {
            super.setMeasuredDimensionFromChildren(widthSpec, heightSpec);
        }

        @Override
        public boolean shouldReMeasureChild(View child, int widthSpec, int heightSpec,
                RecyclerView.LayoutParams lp) {
            return super.shouldReMeasureChild(child, widthSpec, heightSpec, lp);
        }

        @Override
        public boolean shouldMeasureChild(View child, int widthSpec, int heightSpec,
                RecyclerView.LayoutParams lp) {
            return super.shouldMeasureChild(child, widthSpec, heightSpec, lp);
        }

        @Override
        public void removeAndRecycleScrapInt(RecyclerView.Recycler recycler) {
            super.removeAndRecycleScrapInt(recycler);
        }

        @Override
        public void stopSmoothScroller() {
            super.stopSmoothScroller();
        }

        @Override
        public boolean shouldMeasureTwice() {
            return super.shouldMeasureTwice();
        }

        // END MOCKITO OVERRIDES
    }

    public class WrapAdapter extends RecyclerView.Adapter {

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return null;
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {

        }

        @Override
        public int getItemCount() {
            return 10;
        }
    }
}
