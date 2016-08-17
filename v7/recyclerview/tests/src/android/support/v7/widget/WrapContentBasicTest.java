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

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.content.Context;
import android.support.annotation.Nullable;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import android.test.AndroidTestCase;
import android.test.suitebuilder.annotation.MediumTest;
import android.view.Display;
import android.view.View;
import android.view.ViewGroup;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@MediumTest
@RunWith(AndroidJUnit4.class)
public class WrapContentBasicTest extends AndroidTestCase {

    private WrapContentLayoutManager mLayoutManager;
    private RecyclerView mRecyclerView;
    private WrapAdapter mAdapter;
    private static int WRAP = View.MeasureSpec.makeMeasureSpec(10, View.MeasureSpec.AT_MOST);
    private static int EXACT = View.MeasureSpec.makeMeasureSpec(10, View.MeasureSpec.EXACTLY);
    private static int UNSPECIFIED = View.MeasureSpec
            .makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        setContext(InstrumentationRegistry.getContext());
        RecyclerView rv = new RecyclerView(getContext());
        mRecyclerView = spy(rv);
        mLayoutManager = spy(new WrapContentLayoutManager());
        // working around a mockito issue
        rv.mLayout = mLayoutManager;
        mAdapter = spy(new WrapAdapter());
        mRecyclerView.setLayoutManager(mLayoutManager);
        mRecyclerView.setAdapter(mAdapter);
    }

    @After
    @Override
    public void tearDown() throws Exception {
        super.tearDown();
    }

    @Test
    public void testLayoutInOnMeasureWithoutPredictive() {
        mLayoutManager.setAutoMeasureEnabled(true);
        when(mLayoutManager.supportsPredictiveItemAnimations()).thenReturn(false);
        mRecyclerView.onMeasure(WRAP, WRAP);
        mRecyclerView.onMeasure(WRAP, WRAP);
        mRecyclerView.onLayout(true, 0, 10, 10, 10);
        verify(mLayoutManager, times(3))
                .onLayoutChildren(mRecyclerView.mRecycler, mRecyclerView.mState);
    }

    @Test
    public void dataChangeAfterMeasure() {
        mLayoutManager.setAutoMeasureEnabled(true);
        mRecyclerView.onMeasure(WRAP, WRAP);
        mRecyclerView.onMeasure(WRAP, WRAP);
        mAdapter.notifyItemChanged(1);
        mRecyclerView.onLayout(true, 0, 10, 10, 10);
        verify(mLayoutManager, times(3))
                .onLayoutChildren(mRecyclerView.mRecycler, mRecyclerView.mState);
    }

    @Test
    public void setDimensionsFromChildren() {
        mLayoutManager.setAutoMeasureEnabled(true);
        View[] children = createMockChildren(3);
        mLayoutManager.setMeasuredDimensionFromChildren(WRAP, WRAP);
        verify(mLayoutManager).setMeasuredDimension(children[0].getWidth(),
                children[0].getHeight());
    }

    @Test
    public void setDimensionsFromChildrenAnsSpec1() {
        mLayoutManager.setAutoMeasureEnabled(true);
        View[] children = createMockChildren(3);
        int hSpec = View.MeasureSpec.makeMeasureSpec(111, View.MeasureSpec.EXACTLY);
        mLayoutManager.setMeasuredDimensionFromChildren(WRAP, hSpec);
        verify(mLayoutManager).setMeasuredDimension(children[0].getWidth(), 111);
    }

    @Test
    public void setDimensionsFromChildrenAnsSpec2() {
        mLayoutManager.setAutoMeasureEnabled(true);
        View[] children = createMockChildren(3);
        int wSpec = View.MeasureSpec.makeMeasureSpec(111, View.MeasureSpec.EXACTLY);
        mLayoutManager.setMeasuredDimensionFromChildren(wSpec, WRAP);
        verify(mLayoutManager).setMeasuredDimension(111, children[0].getHeight());
    }

    @Test
    public void setDimensionsFromChildrenAnsSpec3() {
        mLayoutManager.setAutoMeasureEnabled(true);
        View[] children = createMockChildren(3);
        children[0].layout(0, 0, 100, 100);
        children[1].layout(-5, 0, 100, 100);
        children[2].layout(-5, -10, 100, 100);
        mLayoutManager.setMeasuredDimensionFromChildren(UNSPECIFIED, UNSPECIFIED);
        verify(mLayoutManager).setMeasuredDimension(105, 110);
    }

    @Test
    public void setDimensionsFromChildrenAnsSpec4() {
        mLayoutManager.setAutoMeasureEnabled(true);
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
        mLayoutManager.setAutoMeasureEnabled(true);
        View[] children = createMockChildren(3);
        children[0].layout(0, 0, 100, 100);
        children[1].layout(-5, 0, 100, 100);
        children[2].layout(-5, -10, 100, 100);
        when(mRecyclerView.getMinimumWidth()).thenReturn(250);
        mLayoutManager.setMeasuredDimensionFromChildren(UNSPECIFIED, UNSPECIFIED);
        verify(mLayoutManager).setMeasuredDimension(250, 110);

        when(mRecyclerView.getMinimumWidth()).thenReturn(5);
        mLayoutManager.setMeasuredDimensionFromChildren(UNSPECIFIED, UNSPECIFIED);
        verify(mLayoutManager).setMeasuredDimension(105, 110);
    }

    @Test
    public void setDimensionsFromChildrenAnsSpec6() {
        mLayoutManager.setAutoMeasureEnabled(true);
        View[] children = createMockChildren(3);
        children[0].layout(0, 0, 100, 100);
        children[1].layout(-5, 0, 100, 100);
        children[2].layout(-5, -10, 100, 100);
        when(mRecyclerView.getMinimumHeight()).thenReturn(250);
        mLayoutManager.setMeasuredDimensionFromChildren(UNSPECIFIED, UNSPECIFIED);
        verify(mLayoutManager).setMeasuredDimension(105, 250);

        when(mRecyclerView.getMinimumHeight()).thenReturn(50);
        mLayoutManager.setMeasuredDimensionFromChildren(UNSPECIFIED, UNSPECIFIED);
        verify(mLayoutManager).setMeasuredDimension(105, 110);
    }

    private View[] createMockChildren(int count) {
        View[] views = new View[count];
        for (int i = 0; i < count; i++) {
            View v = new View(getContext());
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
    }

    public class WrapAdapter extends RecyclerView.Adapter {

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return null;
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {

        }

        @Override
        public int getItemCount() {
            return 10;
        }
    }
}
