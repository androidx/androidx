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
package android.support.v17.leanback.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.LargeTest;
import android.support.test.filters.SdkSuppress;
import android.support.test.runner.AndroidJUnit4;
import android.support.v17.leanback.R;
import android.support.v17.leanback.testutils.PollingCheck;
import android.support.v17.leanback.widget.ArrayObjectAdapter;
import android.support.v17.leanback.widget.HeaderItem;
import android.support.v17.leanback.widget.HorizontalGridView;
import android.support.v17.leanback.widget.ItemBridgeAdapter;
import android.support.v17.leanback.widget.ListRow;
import android.support.v17.leanback.widget.ListRowPresenter;
import android.support.v17.leanback.widget.ObjectAdapter;
import android.support.v17.leanback.widget.OnItemViewClickedListener;
import android.support.v17.leanback.widget.Presenter;
import android.support.v17.leanback.widget.Row;
import android.support.v17.leanback.widget.RowPresenter;
import android.support.v17.leanback.widget.SinglePresenterSelector;
import android.support.v17.leanback.widget.VerticalGridView;
import android.view.KeyEvent;
import android.view.View;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

@LargeTest
@RunWith(AndroidJUnit4.class)
public class RowsFragmentTest extends SingleFragmentTestBase {

    static final StringPresenter sCardPresenter = new StringPresenter();

    static void loadData(ArrayObjectAdapter adapter, int numRows, int repeatPerRow) {
        for (int i = 0; i < numRows; ++i) {
            ArrayObjectAdapter listRowAdapter = new ArrayObjectAdapter(sCardPresenter);
            int index = 0;
            for (int j = 0; j < repeatPerRow; ++j) {
                listRowAdapter.add("Hello world-" + (index++));
                listRowAdapter.add("This is a test-" + (index++));
                listRowAdapter.add("Android TV-" + (index++));
                listRowAdapter.add("Leanback-" + (index++));
                listRowAdapter.add("Hello world-" + (index++));
                listRowAdapter.add("Android TV-" + (index++));
                listRowAdapter.add("Leanback-" + (index++));
                listRowAdapter.add("GuidedStepFragment-" + (index++));
            }
            HeaderItem header = new HeaderItem(i, "Row " + i);
            adapter.add(new ListRow(header, listRowAdapter));
        }
    }

    public static class F_defaultAlignment extends RowsFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            ListRowPresenter lrp = new ListRowPresenter();
            ArrayObjectAdapter adapter = new ArrayObjectAdapter(lrp);
            setAdapter(adapter);
            loadData(adapter, 10, 1);
        }
    }

    @Test
    public void defaultAlignment() throws Throwable {
        SingleFragmentTestActivity activity = launchAndWaitActivity(F_defaultAlignment.class, 1000);

        final Rect rect = new Rect();

        final VerticalGridView gridView = ((RowsFragment) activity.getTestFragment())
                .getVerticalGridView();
        View row0 = gridView.findViewHolderForAdapterPosition(0).itemView;
        rect.set(0, 0, row0.getWidth(), row0.getHeight());
        gridView.offsetDescendantRectToMyCoords(row0, rect);
        assertEquals("First row is initially aligned to top of screen", 0, rect.top);

        sendKeys(KeyEvent.KEYCODE_DPAD_DOWN);
        waitForScrollIdle(gridView);
        View row1 = gridView.findViewHolderForAdapterPosition(1).itemView;
        PollingCheck.waitFor(new PollingCheck.ViewStableOnScreen(row1));

        rect.set(0, 0, row1.getWidth(), row1.getHeight());
        gridView.offsetDescendantRectToMyCoords(row1, rect);
        assertTrue("Second row should not be aligned to top of screen", rect.top > 0);
    }

    public static class F_selectBeforeSetAdapter extends RowsFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setSelectedPosition(7, false);
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    getVerticalGridView().requestLayout();
                }
            }, 100);
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    ListRowPresenter lrp = new ListRowPresenter();
                    ArrayObjectAdapter adapter = new ArrayObjectAdapter(lrp);
                    setAdapter(adapter);
                    loadData(adapter, 10, 1);
                }
            }, 1000);
        }
    }

    @Test
    public void selectBeforeSetAdapter() throws InterruptedException {
        SingleFragmentTestActivity activity =
                launchAndWaitActivity(F_selectBeforeSetAdapter.class, 2000);

        final VerticalGridView gridView = ((RowsFragment) activity.getTestFragment())
                .getVerticalGridView();
        assertEquals(7, gridView.getSelectedPosition());
        assertNotNull(gridView.findViewHolderForAdapterPosition(7));
    }

    public static class F_selectBeforeAddData extends RowsFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            ListRowPresenter lrp = new ListRowPresenter();
            final ArrayObjectAdapter adapter = new ArrayObjectAdapter(lrp);
            setAdapter(adapter);
            setSelectedPosition(7, false);
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    getVerticalGridView().requestLayout();
                }
            }, 100);
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    loadData(adapter, 10, 1);
                }
            }, 1000);
        }
    }

    @Test
    public void selectBeforeAddData() throws InterruptedException {
        SingleFragmentTestActivity activity =
                launchAndWaitActivity(F_selectBeforeAddData.class, 2000);

        final VerticalGridView gridView = ((RowsFragment) activity.getTestFragment())
                .getVerticalGridView();
        assertEquals(7, gridView.getSelectedPosition());
        assertNotNull(gridView.findViewHolderForAdapterPosition(7));
    }

    public static class F_selectAfterAddData extends RowsFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            ListRowPresenter lrp = new ListRowPresenter();
            final ArrayObjectAdapter adapter = new ArrayObjectAdapter(lrp);
            setAdapter(adapter);
            loadData(adapter, 10, 1);
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    setSelectedPosition(7, false);
                }
            }, 1000);
        }
    }

    @Test
    public void selectAfterAddData() throws InterruptedException {
        SingleFragmentTestActivity activity =
                launchAndWaitActivity(F_selectAfterAddData.class, 2000);

        final VerticalGridView gridView = ((RowsFragment) activity.getTestFragment())
                .getVerticalGridView();
        assertEquals(7, gridView.getSelectedPosition());
        assertNotNull(gridView.findViewHolderForAdapterPosition(7));
    }

    static WeakReference<F_restoreSelection> sLastF_restoreSelection;

    public static class F_restoreSelection extends RowsFragment {
        public F_restoreSelection() {
            sLastF_restoreSelection = new WeakReference<F_restoreSelection>(this);
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            ListRowPresenter lrp = new ListRowPresenter();
            final ArrayObjectAdapter adapter = new ArrayObjectAdapter(lrp);
            setAdapter(adapter);
            loadData(adapter, 10, 1);
            if (savedInstanceState == null) {
                setSelectedPosition(7, false);
            }
        }
    }

    @Test
    public void restoreSelection() {
        final SingleFragmentTestActivity activity =
                launchAndWaitActivity(F_restoreSelection.class, 1000);

        InstrumentationRegistry.getInstrumentation().runOnMainSync(
                new Runnable() {
                    @Override
                    public void run() {
                        activity.recreate();
                    }
                }
        );
        SystemClock.sleep(1000);

        // mActivity is invalid after recreate(), a new Activity instance is created
        // but we could get Fragment from static variable.
        RowsFragment fragment = sLastF_restoreSelection.get();
        final VerticalGridView gridView = fragment.getVerticalGridView();
        assertEquals(7, gridView.getSelectedPosition());
        assertNotNull(gridView.findViewHolderForAdapterPosition(7));

    }

    public static class F_ListRowWithOnClick extends RowsFragment {
        Presenter.ViewHolder mLastClickedItemViewHolder;
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setOnItemViewClickedListener(new OnItemViewClickedListener() {
                @Override
                public void onItemClicked(Presenter.ViewHolder itemViewHolder, Object item,
                        RowPresenter.ViewHolder rowViewHolder, Row row) {
                    mLastClickedItemViewHolder = itemViewHolder;
                }
            });
            ListRowPresenter lrp = new ListRowPresenter();
            ArrayObjectAdapter adapter = new ArrayObjectAdapter(lrp);
            setAdapter(adapter);
            loadData(adapter, 10, 1);
        }
    }

    @Test
    public void prefetchChildItemsBeforeAttach() throws Throwable {
        SingleFragmentTestActivity activity =
                launchAndWaitActivity(F_ListRowWithOnClick.class, 1000);

        F_ListRowWithOnClick fragment = (F_ListRowWithOnClick) activity.getTestFragment();
        final VerticalGridView gridView = fragment.getVerticalGridView();
        View lastRow = gridView.getChildAt(gridView.getChildCount() - 1);
        final int lastRowPos = gridView.getChildAdapterPosition(lastRow);
        InstrumentationRegistry.getInstrumentation().runOnMainSync(
                new Runnable() {
                    public void run() {
                        gridView.setSelectedPositionSmooth(lastRowPos);
                    }
                }
        );
        waitForScrollIdle(gridView);
        ItemBridgeAdapter.ViewHolder prefetchedBridgeVh = (ItemBridgeAdapter.ViewHolder)
                gridView.findViewHolderForAdapterPosition(lastRowPos + 1);
        RowPresenter prefetchedRowPresenter = (RowPresenter) prefetchedBridgeVh.getPresenter();
        final ListRowPresenter.ViewHolder prefetchedListRowVh = (ListRowPresenter.ViewHolder)
                prefetchedRowPresenter.getRowViewHolder(prefetchedBridgeVh.getViewHolder());

        fragment.mLastClickedItemViewHolder = null;
        InstrumentationRegistry.getInstrumentation().runOnMainSync(
                new Runnable() {
                    public void run() {
                        prefetchedListRowVh.getItemViewHolder(0).view.performClick();
                    }
                }
        );
        assertSame(prefetchedListRowVh.getItemViewHolder(0), fragment.mLastClickedItemViewHolder);
    }

    @Test
    public void changeHasStableIdToTrueAfterViewCreated() throws InterruptedException {
        SingleFragmentTestActivity activity =
                launchAndWaitActivity(RowsFragment.class, 2000);
        final RowsFragment fragment = (RowsFragment) activity.getTestFragment();

        InstrumentationRegistry.getInstrumentation().runOnMainSync(
                new Runnable() {
                    public void run() {
                        ObjectAdapter adapter = new ObjectAdapter() {
                            @Override
                            public int size() {
                                return 0;
                            }

                            @Override
                            public Object get(int position) {
                                return null;
                            }

                            @Override
                            public long getId(int position) {
                                return 1;
                            }
                        };
                        adapter.setHasStableIds(true);
                        fragment.setAdapter(adapter);
                    }
                }
        );
    }

    static class StableIdAdapter extends ObjectAdapter {
        ArrayList<Integer> mList = new ArrayList();

        @Override
        public long getId(int position) {
            return mList.get(position).longValue();
        }

        @Override
        public Object get(int position) {
            return mList.get(position);
        }

        @Override
        public int size() {
            return mList.size();
        }
    }

    public static class F_rowNotifyItemRangeChange extends BrowseFragment {

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            ListRowPresenter lrp = new ListRowPresenter();
            final ArrayObjectAdapter adapter = new ArrayObjectAdapter(lrp);
            for (int i = 0; i < 2; i++) {
                StableIdAdapter listRowAdapter = new StableIdAdapter();
                listRowAdapter.setHasStableIds(true);
                listRowAdapter.setPresenterSelector(
                        new SinglePresenterSelector(sCardPresenter));
                int index = 0;
                listRowAdapter.mList.add(index++);
                listRowAdapter.mList.add(index++);
                listRowAdapter.mList.add(index++);
                HeaderItem header = new HeaderItem(i, "Row " + i);
                adapter.add(new ListRow(header, listRowAdapter));
            }
            setAdapter(adapter);
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    StableIdAdapter rowAdapter = (StableIdAdapter)
                            ((ListRow) adapter.get(1)).getAdapter();
                    rowAdapter.notifyItemRangeChanged(0, 3);
                }
            }, 500);
        }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.LOLLIPOP)
    @Test
    public void rowNotifyItemRangeChange() throws InterruptedException {
        SingleFragmentTestActivity activity = launchAndWaitActivity(
                RowsFragmentTest.F_rowNotifyItemRangeChange.class, 2000);

        VerticalGridView verticalGridView = ((BrowseFragment) activity.getTestFragment())
                .getRowsFragment().getVerticalGridView();
        for (int i = 0; i < verticalGridView.getChildCount(); i++) {
            HorizontalGridView horizontalGridView = verticalGridView.getChildAt(i)
                    .findViewById(R.id.row_content);
            for (int j = 0; j < horizontalGridView.getChildCount(); j++) {
                assertEquals(horizontalGridView.getPaddingTop(),
                        horizontalGridView.getChildAt(j).getTop());
            }
        }
    }

    public static class F_rowNotifyItemRangeChangeWithTransition extends BrowseFragment {

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            ListRowPresenter lrp = new ListRowPresenter();
            prepareEntranceTransition();
            final ArrayObjectAdapter adapter = new ArrayObjectAdapter(lrp);
            for (int i = 0; i < 2; i++) {
                StableIdAdapter listRowAdapter = new StableIdAdapter();
                listRowAdapter.setHasStableIds(true);
                listRowAdapter.setPresenterSelector(
                        new SinglePresenterSelector(sCardPresenter));
                int index = 0;
                listRowAdapter.mList.add(index++);
                listRowAdapter.mList.add(index++);
                listRowAdapter.mList.add(index++);
                HeaderItem header = new HeaderItem(i, "Row " + i);
                adapter.add(new ListRow(header, listRowAdapter));
            }
            setAdapter(adapter);
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    StableIdAdapter rowAdapter = (StableIdAdapter)
                            ((ListRow) adapter.get(1)).getAdapter();
                    rowAdapter.notifyItemRangeChanged(0, 3);
                }
            }, 500);
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    startEntranceTransition();
                }
            }, 520);
        }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.LOLLIPOP)
    @Test
    public void rowNotifyItemRangeChangeWithTransition() throws InterruptedException {
        SingleFragmentTestActivity activity = launchAndWaitActivity(
                        RowsFragmentTest.F_rowNotifyItemRangeChangeWithTransition.class, 3000);

        VerticalGridView verticalGridView = ((BrowseFragment) activity.getTestFragment())
                .getRowsFragment().getVerticalGridView();
        for (int i = 0; i < verticalGridView.getChildCount(); i++) {
            HorizontalGridView horizontalGridView = verticalGridView.getChildAt(i)
                    .findViewById(R.id.row_content);
            for (int j = 0; j < horizontalGridView.getChildCount(); j++) {
                assertEquals(horizontalGridView.getPaddingTop(),
                        horizontalGridView.getChildAt(j).getTop());
                assertEquals(0, horizontalGridView.getChildAt(j).getTranslationY(), 0.1f);
            }
        }
    }
}
