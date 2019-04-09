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

package androidx.leanback.widget;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.filters.SmallTest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;

@SmallTest
@RunWith(JUnit4.class)
public class ItemBridgeAdapterTest {
    private static final int sViewWidth = 100;
    private static final int sViewHeight = 100;

    public static class BasePresenter extends Presenter {

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent) {
            View view = new View(parent.getContext());
            view.setLayoutParams(new ViewGroup.LayoutParams(sViewWidth, sViewHeight));
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ViewHolder viewHolder, Object item) {
        }

        @Override
        public void onUnbindViewHolder(ViewHolder viewHolder) {

        }
    }

    private ItemBridgeAdapter mItemBridgeAdapter;

    private ArrayObjectAdapter mAdapter;

    private RecyclerView mRecyclerView;

    private Presenter mPresenter;

    @Before
    public void setup() {
        mPresenter = spy(BasePresenter.class);

        mItemBridgeAdapter = new ItemBridgeAdapter();
        mAdapter = new ArrayObjectAdapter(mPresenter);
        mAdapter.setItems(populateData(), null);
        mItemBridgeAdapter.setAdapter(mAdapter);

        Context context = ApplicationProvider.getApplicationContext();
        mRecyclerView = new RecyclerView(context);
        mRecyclerView.setItemViewCacheSize(0);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(context,
                LinearLayoutManager.VERTICAL, false));
        mRecyclerView.setHasFixedSize(false); // force layout items in layout pass
    }

    List populateData() {
        List data = new ArrayList();
        for (int i = 0; i < 10000; i++) {
            data.add(i);
        }
        return data;
    }

    static void measureAndLayoutRecycleView(RecyclerView recyclerView) {
        measureAndLayout(recyclerView, 1920, 1080);
    }

    static void measureAndLayout(View view, int expectedWidth, int expectedHeight) {
        view.measure(View.MeasureSpec.makeMeasureSpec(expectedWidth, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(expectedHeight, View.MeasureSpec.EXACTLY));
        view.layout(0, 0, expectedWidth, expectedHeight);
    }

    @Test
    public void onCreateAndOnBind() {
        mRecyclerView.setAdapter(mItemBridgeAdapter);
        measureAndLayoutRecycleView(mRecyclerView);
        int childCount = mRecyclerView.getChildCount();
        assertTrue(childCount > 0);
        // Assert number of invokes of onCreateViewHolder and onBindViewHolder.
        Mockito.verify(mPresenter, times(childCount)).onCreateViewHolder(any(ViewGroup.class));
        Mockito.verify(mPresenter, times(childCount))
                .onBindViewHolder(any(Presenter.ViewHolder.class), any());
    }
    @Test
    public void onUnbind() {
        mRecyclerView.setAdapter(mItemBridgeAdapter);
        measureAndLayoutRecycleView(mRecyclerView);
        assertTrue(mRecyclerView.getChildCount() > 0);
        // When scroll one item offscreen, assert onUnbindViewHolder called once.
        mRecyclerView.scrollBy(0, mRecyclerView.getChildAt(0).getHeight());
        Mockito.verify(mPresenter, times(1))
                .onUnbindViewHolder(any(Presenter.ViewHolder.class));
    }

    @Test
    public void onUnbindWithTransientState() {
        mRecyclerView.setAdapter(mItemBridgeAdapter);
        measureAndLayoutRecycleView(mRecyclerView);
        assertTrue(mRecyclerView.getChildCount() > 0);
        // Set TransientState to true to simulate the view is running custom ViewPropertyAnimation.
        mRecyclerView.getChildAt(0).setHasTransientState(true);
        // When scroll one item with custom animation offscreen, assert onUnbindViewHolder
        // was called once when ViewHolder is still having transient state.
        mRecyclerView.scrollBy(0, mRecyclerView.getChildAt(0).getHeight());
        Mockito.verify(mPresenter, times(1))
                .onUnbindViewHolder(any(Presenter.ViewHolder.class));
    }

    static class PresenterUsingPool extends Presenter {
        final ArrayList<View> mViews = new ArrayList<>();

        PresenterUsingPool(RecyclerView.RecycledViewPool pool, int viewType) {
            while (pool.getRecycledViewCount(viewType) > 0) {
                mViews.add(pool.getRecycledView(viewType).itemView);
            }
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent) {
            View view = mViews.get(mViews.size() - 1);
            mViews.remove(mViews.size() - 1);
            view.setLayoutParams(new ViewGroup.LayoutParams(sViewWidth, sViewHeight));
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ViewHolder viewHolder, Object item) {
        }

        @Override
        public void onUnbindViewHolder(ViewHolder viewHolder) {

        }
    }

    @Test
    public void focusHighlightChanging() {
        RecyclerView.RecycledViewPool pool = new RecyclerView.RecycledViewPool();
        pool.setMaxRecycledViews(0, 1000);
        FocusHighlightHandler focusHighlightHandler1 = Mockito.spy(FocusHighlightHandler.class);
        mItemBridgeAdapter.setFocusHighlight(focusHighlightHandler1);
        mRecyclerView.setRecycledViewPool(pool);
        mRecyclerView.setAdapter(mItemBridgeAdapter);
        measureAndLayoutRecycleView(mRecyclerView);
        assertSame(focusHighlightHandler1,
                ((ItemBridgeAdapter.ChainingFocusChangeListener) mRecyclerView.getChildAt(0)
                        .getOnFocusChangeListener()).mFocusHighlight);
        Mockito.verify(focusHighlightHandler1).onInitializeView(mRecyclerView.getChildAt(0));

        mItemBridgeAdapter.clear();
        measureAndLayoutRecycleView(mRecyclerView);
        assertTrue(pool.getRecycledViewCount(0) > 0);
        assertTrue(mRecyclerView.getChildCount() == 0);

        // re-create adapter, change focus highlight from non-null to null
        mItemBridgeAdapter = new ItemBridgeAdapter();
        mAdapter = new ArrayObjectAdapter(new PresenterUsingPool(pool, 0));
        mAdapter.setItems(populateData(), null);
        mItemBridgeAdapter.setAdapter(mAdapter);
        mItemBridgeAdapter.setFocusHighlight(null);
        mRecyclerView.swapAdapter(mItemBridgeAdapter, false);
        measureAndLayoutRecycleView(mRecyclerView);
        assertNull(mRecyclerView.getChildAt(0).getOnFocusChangeListener());

        mItemBridgeAdapter.clear();
        measureAndLayoutRecycleView(mRecyclerView);
        assertTrue(pool.getRecycledViewCount(0) > 0);
        assertTrue(mRecyclerView.getChildCount() == 0);

        // re-create adapter, change focus highlight from null to non-null
        mItemBridgeAdapter = new ItemBridgeAdapter();
        mAdapter = new ArrayObjectAdapter(new PresenterUsingPool(pool, 0));
        mAdapter.setItems(populateData(), null);
        mItemBridgeAdapter.setAdapter(mAdapter);
        FocusHighlightHandler focusHighlightHandler2 = Mockito.spy(FocusHighlightHandler.class);
        mItemBridgeAdapter.setFocusHighlight(focusHighlightHandler2);
        mRecyclerView.swapAdapter(mItemBridgeAdapter, false);
        measureAndLayoutRecycleView(mRecyclerView);
        assertSame(focusHighlightHandler2,
                ((ItemBridgeAdapter.ChainingFocusChangeListener) mRecyclerView.getChildAt(0)
                        .getOnFocusChangeListener()).mFocusHighlight);
        Mockito.verify(focusHighlightHandler2).onInitializeView(mRecyclerView.getChildAt(0));
    }
}
