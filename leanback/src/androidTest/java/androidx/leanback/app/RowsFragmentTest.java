// CHECKSTYLE:OFF Generated code
/* This file is auto-generated from RowsSupportFragmentTest.java.  DO NOT MODIFY. */

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
package androidx.leanback.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import android.app.Fragment;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.LargeTest;
import android.support.test.filters.SdkSuppress;
import android.support.test.runner.AndroidJUnit4;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.leanback.test.R;
import androidx.leanback.testutils.PollingCheck;
import androidx.leanback.widget.ArrayObjectAdapter;
import androidx.leanback.widget.HeaderItem;
import androidx.leanback.widget.HorizontalGridView;
import androidx.leanback.widget.ItemBridgeAdapter;
import androidx.leanback.widget.ListRow;
import androidx.leanback.widget.ListRowPresenter;
import androidx.leanback.widget.ObjectAdapter;
import androidx.leanback.widget.OnItemViewClickedListener;
import androidx.leanback.widget.PageRow;
import androidx.leanback.widget.Presenter;
import androidx.leanback.widget.Row;
import androidx.leanback.widget.RowPresenter;
import androidx.leanback.widget.SinglePresenterSelector;
import androidx.leanback.widget.VerticalGridView;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

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

    static Bundle saveActivityState(final SingleFragmentTestActivity activity) {
        final Bundle[] savedState = new Bundle[1];
        // save activity state
        InstrumentationRegistry.getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                savedState[0] = activity.performSaveInstanceState();
            }
        });
        return savedState[0];
    }

    static void waitForHeaderTransition(final F_Base fragment) {
        // Wait header transition finishes
        SystemClock.sleep(100);
        PollingCheck.waitFor(new PollingCheck.PollingCheckCondition() {
            @Override
            public boolean canProceed() {
                return !fragment.isInHeadersTransition();
            }
        });
    }

    static void selectAndWaitFragmentAnimation(final F_Base fragment, final int row,
            final int item) {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                fragment.setSelectedPosition(row, true,
                        new ListRowPresenter.SelectItemViewHolderTask(item));
            }
        });
        // Wait header transition finishes and scrolling stops
        SystemClock.sleep(100);
        PollingCheck.waitFor(new PollingCheck.PollingCheckCondition() {
            @Override
            public boolean canProceed() {
                return !fragment.isInHeadersTransition()
                        && !fragment.getHeadersFragment().isScrolling();
            }
        });
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

    public static class F_Base extends BrowseFragment {

        List<Long> mEntranceTransitionStartTS = new ArrayList();
        List<Long> mEntranceTransitionEndTS = new ArrayList();

        @Override
        protected void onEntranceTransitionStart() {
            super.onEntranceTransitionStart();
            mEntranceTransitionStartTS.add(SystemClock.uptimeMillis());
        }

        @Override
        protected void onEntranceTransitionEnd() {
            super.onEntranceTransitionEnd();
            mEntranceTransitionEndTS.add(SystemClock.uptimeMillis());
        }

        public void assertExecutedEntranceTransition() {
            assertEquals(1, mEntranceTransitionStartTS.size());
            assertEquals(1, mEntranceTransitionEndTS.size());
            assertTrue(mEntranceTransitionEndTS.get(0) - mEntranceTransitionStartTS.get(0) > 100);
        }

        public void assertNoEntranceTransition() {
            assertEquals(0, mEntranceTransitionStartTS.size());
            assertEquals(0, mEntranceTransitionEndTS.size());
        }

        /**
         * Util to wait PageFragment swapped.
         */
        Fragment waitPageFragment(final Class pageFragmentClass) {
            PollingCheck.waitFor(new PollingCheck.PollingCheckCondition() {
                @Override
                public boolean canProceed() {
                    return pageFragmentClass.isInstance(getMainFragment())
                            && getMainFragment().getView() != null;
                }
            });
            return getMainFragment();
        }

        /**
         * Wait until a fragment for non-page Row is created. Does not apply to the case a
         * RowsFragment is created on a PageRow.
         */
        RowsFragment waitRowsFragment() {
            PollingCheck.waitFor(new PollingCheck.PollingCheckCondition() {
                @Override
                public boolean canProceed() {
                    return mMainFragmentListRowDataAdapter != null
                            && getMainFragment() instanceof RowsFragment
                            && !(getMainFragment() instanceof SampleRowsFragment);
                }
            });
            return (RowsFragment) getMainFragment();
        }
    }

    static ObjectAdapter createListRowAdapter() {
        StableIdAdapter listRowAdapter = new StableIdAdapter();
        listRowAdapter.setHasStableIds(false);
        listRowAdapter.setPresenterSelector(
                new SinglePresenterSelector(sCardPresenter));
        int index = 0;
        listRowAdapter.mList.add(index++);
        listRowAdapter.mList.add(index++);
        listRowAdapter.mList.add(index++);
        return listRowAdapter;
    }

    /**
     * Create BrowseFragmentAdapter with 3 ListRows
     */
    static ArrayObjectAdapter createListRowsAdapter() {
        ListRowPresenter lrp = new ListRowPresenter();
        final ArrayObjectAdapter adapter = new ArrayObjectAdapter(lrp);
        for (int i = 0; i < 3; i++) {
            ObjectAdapter listRowAdapter = createListRowAdapter();
            HeaderItem header = new HeaderItem(i, "Row " + i);
            adapter.add(new ListRow(header, listRowAdapter));
        }
        return adapter;
    }

    /**
     * A typical BrowseFragment with multiple rows that start entrance transition
     */
    public static class F_standard extends F_Base {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            if (savedInstanceState == null) {
                prepareEntranceTransition();
            }
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    setAdapter(createListRowsAdapter());
                    startEntranceTransition();
                }
            }, 100);
        }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.LOLLIPOP)
    @Test
    public void browseFragmentSetNullAdapter() throws InterruptedException {
        final SingleFragmentTestActivity activity = launchAndWaitActivity(
                RowsFragmentTest.F_standard.class, 2000);
        final F_standard fragment = ((F_standard) activity.getTestFragment());
        fragment.assertExecutedEntranceTransition();

        final ObjectAdapter adapter1 = fragment.getAdapter();
        ListRowDataAdapter wrappedAdapter = fragment.mMainFragmentListRowDataAdapter;
        assertTrue(adapter1.hasObserver());
        assertTrue(wrappedAdapter.hasObserver());
        InstrumentationRegistry.getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                fragment.setAdapter(null);
            }
        });
        // adapter should no longer has observer and there is no reference to adapter from
        // BrowseFragment.
        assertFalse(adapter1.hasObserver());
        assertFalse(wrappedAdapter.hasObserver());
        assertNull(fragment.getAdapter());
        assertNull(fragment.mMainFragmentListRowDataAdapter);
        // RowsFragment is still there
        assertTrue(fragment.mMainFragment instanceof RowsFragment);
        assertNotNull(fragment.mMainFragmentRowsAdapter);
        assertNotNull(fragment.mMainFragmentAdapter);

        // initialize to same adapter
        InstrumentationRegistry.getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                fragment.setAdapter(adapter1);
            }
        });
        assertTrue(adapter1.hasObserver());
        assertNotSame(wrappedAdapter, fragment.mMainFragmentListRowDataAdapter);
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.LOLLIPOP)
    @Test
    public void browseFragmentChangeAdapter() throws InterruptedException {
        final SingleFragmentTestActivity activity = launchAndWaitActivity(
                RowsFragmentTest.F_standard.class, 2000);
        final F_standard fragment = ((F_standard) activity.getTestFragment());
        fragment.assertExecutedEntranceTransition();

        final ObjectAdapter adapter1 = fragment.getAdapter();
        ListRowDataAdapter wrappedAdapter = fragment.mMainFragmentListRowDataAdapter;
        assertTrue(adapter1.hasObserver());
        assertTrue(wrappedAdapter.hasObserver());
        final ObjectAdapter adapter2 = createListRowsAdapter();
        InstrumentationRegistry.getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                fragment.setAdapter(adapter2);
            }
        });
        // adapter1 should no longer has observer and adapter2 will have observer
        assertFalse(adapter1.hasObserver());
        assertFalse(wrappedAdapter.hasObserver());
        assertSame(adapter2, fragment.getAdapter());
        assertNotSame(wrappedAdapter, fragment.mMainFragmentListRowDataAdapter);
        assertTrue(adapter2.hasObserver());
        assertTrue(fragment.mMainFragmentListRowDataAdapter.hasObserver());
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.LOLLIPOP)
    @Test
    public void browseFragmentChangeAdapterToPage() throws InterruptedException {
        final SingleFragmentTestActivity activity = launchAndWaitActivity(
                RowsFragmentTest.F_standard.class, 2000);
        final F_standard fragment = ((F_standard) activity.getTestFragment());
        fragment.assertExecutedEntranceTransition();

        final ObjectAdapter adapter1 = fragment.getAdapter();
        ListRowDataAdapter wrappedAdapter = fragment.mMainFragmentListRowDataAdapter;
        assertTrue(adapter1.hasObserver());
        assertTrue(wrappedAdapter.hasObserver());
        final ObjectAdapter adapter2 = create2PageRow3ListRow();
        fragment.getMainFragmentRegistry().registerFragment(MyPageRow.class,
                new MyFragmentFactory());
        InstrumentationRegistry.getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                fragment.setAdapter(adapter2);
            }
        });
        fragment.waitPageFragment(SampleRowsFragment.class);
        // adapter1 should no longer has observer and adapter2 will have observer
        assertFalse(adapter1.hasObserver());
        assertFalse(wrappedAdapter.hasObserver());
        assertSame(adapter2, fragment.getAdapter());
        assertNull(fragment.mMainFragmentListRowDataAdapter);
        assertTrue(adapter2.hasObserver());
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.LOLLIPOP)
    @Test
    public void browseFragmentNotifyDataChangeListRowToPage() throws InterruptedException {
        final SingleFragmentTestActivity activity = launchAndWaitActivity(
                RowsFragmentTest.F_standard.class, 2000);
        final F_standard fragment = ((F_standard) activity.getTestFragment());
        fragment.assertExecutedEntranceTransition();

        final ArrayObjectAdapter adapter1 = (ArrayObjectAdapter) fragment.getAdapter();
        ListRowDataAdapter wrappedAdapter = fragment.mMainFragmentListRowDataAdapter;
        assertTrue(adapter1.hasObserver());
        assertTrue(wrappedAdapter.hasObserver());

        fragment.getMainFragmentRegistry().registerFragment(MyPageRow.class,
                new MyFragmentFactory());
        InstrumentationRegistry.getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                adapter1.removeItems(0, 1);
                adapter1.add(0, new MyPageRow(0));
            }
        });
        fragment.waitPageFragment(SampleRowsFragment.class);
        assertTrue(adapter1.hasObserver());
        assertNull(fragment.mMainFragmentListRowDataAdapter);
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.LOLLIPOP)
    @Test
    public void browseFragmentNotifyItemChangeListRowToPage() throws InterruptedException {
        final SingleFragmentTestActivity activity = launchAndWaitActivity(
                RowsFragmentTest.F_standard.class, 2000);
        final F_standard fragment = ((F_standard) activity.getTestFragment());
        fragment.assertExecutedEntranceTransition();

        final ArrayObjectAdapter adapter1 = (ArrayObjectAdapter) fragment.getAdapter();
        ListRowDataAdapter wrappedAdapter = fragment.mMainFragmentListRowDataAdapter;
        assertTrue(adapter1.hasObserver());
        assertTrue(wrappedAdapter.hasObserver());

        fragment.getMainFragmentRegistry().registerFragment(MyPageRow.class,
                new MyFragmentFactory());
        InstrumentationRegistry.getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                adapter1.replace(0, new MyPageRow(0));
            }
        });
        fragment.waitPageFragment(SampleRowsFragment.class);
        assertTrue(adapter1.hasObserver());
        assertNull(fragment.mMainFragmentListRowDataAdapter);
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.LOLLIPOP)
    @Test
    public void browseFragmentNotifyDataChangeListRowToListRow() throws InterruptedException {
        final SingleFragmentTestActivity activity = launchAndWaitActivity(
                RowsFragmentTest.F_standard.class, 2000);
        final F_standard fragment = ((F_standard) activity.getTestFragment());
        fragment.assertExecutedEntranceTransition();

        final ArrayObjectAdapter adapter1 = (ArrayObjectAdapter) fragment.getAdapter();
        ListRowDataAdapter wrappedAdapter = fragment.mMainFragmentListRowDataAdapter;
        assertTrue(adapter1.hasObserver());
        assertTrue(wrappedAdapter.hasObserver());

        fragment.getMainFragmentRegistry().registerFragment(MyPageRow.class,
                new MyFragmentFactory());
        InstrumentationRegistry.getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                ObjectAdapter listRowAdapter = createListRowAdapter();
                HeaderItem header = new HeaderItem(0, "Row 0 changed");
                adapter1.removeItems(0, 1);
                adapter1.add(0, new ListRow(header, listRowAdapter));
            }
        });
        assertTrue(adapter1.hasObserver());
        assertTrue(wrappedAdapter.hasObserver());
        assertSame(wrappedAdapter, fragment.mMainFragmentListRowDataAdapter);
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.LOLLIPOP)
    @Test
    public void browseFragmentNotifyItemChangeListRowToListRow() throws InterruptedException {
        final SingleFragmentTestActivity activity = launchAndWaitActivity(
                RowsFragmentTest.F_standard.class, 2000);
        final F_standard fragment = ((F_standard) activity.getTestFragment());
        fragment.assertExecutedEntranceTransition();

        final ArrayObjectAdapter adapter1 = (ArrayObjectAdapter) fragment.getAdapter();
        ListRowDataAdapter wrappedAdapter = fragment.mMainFragmentListRowDataAdapter;
        assertTrue(adapter1.hasObserver());
        assertTrue(wrappedAdapter.hasObserver());

        fragment.getMainFragmentRegistry().registerFragment(MyPageRow.class,
                new MyFragmentFactory());
        InstrumentationRegistry.getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                ObjectAdapter listRowAdapter = createListRowAdapter();
                HeaderItem header = new HeaderItem(0, "Row 0 changed");
                adapter1.replace(0, new ListRow(header, listRowAdapter));
            }
        });
        assertTrue(adapter1.hasObserver());
        assertTrue(wrappedAdapter.hasObserver());
        assertSame(wrappedAdapter, fragment.mMainFragmentListRowDataAdapter);
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.LOLLIPOP)
    @Test
    public void browseFragmentChangeAdapterPageToPage() throws InterruptedException {
        final SingleFragmentTestActivity activity = launchAndWaitActivity(
                RowsFragmentTest.F_2PageRow3ListRow.class, 2000);
        final F_2PageRow3ListRow fragment = ((F_2PageRow3ListRow) activity.getTestFragment());
        fragment.assertExecutedEntranceTransition();

        final ObjectAdapter adapter1 = fragment.getAdapter();
        assertNull(fragment.mMainFragmentListRowDataAdapter);
        assertTrue(adapter1.hasObserver());
        final ObjectAdapter adapter2 = create2PageRow3ListRow();
        InstrumentationRegistry.getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                fragment.setAdapter(adapter2);
            }
        });
        fragment.waitPageFragment(SampleRowsFragment.class);
        // adapter1 should no longer has observer and adapter2 will have observer
        assertFalse(adapter1.hasObserver());
        assertSame(adapter2, fragment.getAdapter());
        assertNull(fragment.mMainFragmentListRowDataAdapter);
        assertTrue(adapter2.hasObserver());
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.LOLLIPOP)
    @Test
    public void browseFragmentNotifyChangePageToPage() throws InterruptedException {
        final SingleFragmentTestActivity activity = launchAndWaitActivity(
                RowsFragmentTest.F_2PageRow3ListRow.class, 2000);
        final F_2PageRow3ListRow fragment = ((F_2PageRow3ListRow) activity.getTestFragment());
        fragment.assertExecutedEntranceTransition();

        final ArrayObjectAdapter adapter1 = (ArrayObjectAdapter) fragment.getAdapter();
        assertNull(fragment.mMainFragmentListRowDataAdapter);
        assertTrue(adapter1.hasObserver());
        InstrumentationRegistry.getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                adapter1.removeItems(0, 1);
                adapter1.add(0, new MyPageRow(1));
            }
        });
        fragment.waitPageFragment(SampleFragment.class);
        // adapter1 should no longer has observer and adapter2 will have observer
        assertTrue(adapter1.hasObserver());
        assertNull(fragment.mMainFragmentListRowDataAdapter);
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.LOLLIPOP)
    @Test
    public void browseFragmentNotifyItemChangePageToPage() throws InterruptedException {
        final SingleFragmentTestActivity activity = launchAndWaitActivity(
                RowsFragmentTest.F_2PageRow3ListRow.class, 2000);
        final F_2PageRow3ListRow fragment = ((F_2PageRow3ListRow) activity.getTestFragment());
        fragment.assertExecutedEntranceTransition();

        final ArrayObjectAdapter adapter1 = (ArrayObjectAdapter) fragment.getAdapter();
        assertNull(fragment.mMainFragmentListRowDataAdapter);
        assertTrue(adapter1.hasObserver());
        InstrumentationRegistry.getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                adapter1.replace(0, new MyPageRow(1));
            }
        });
        fragment.waitPageFragment(SampleFragment.class);
        // adapter1 should no longer has observer and adapter2 will have observer
        assertTrue(adapter1.hasObserver());
        assertNull(fragment.mMainFragmentListRowDataAdapter);
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.LOLLIPOP)
    @Test
    public void browseFragmentChangeAdapterPageToListRow() throws InterruptedException {
        final SingleFragmentTestActivity activity = launchAndWaitActivity(
                RowsFragmentTest.F_2PageRow3ListRow.class, 2000);
        final F_2PageRow3ListRow fragment = ((F_2PageRow3ListRow) activity.getTestFragment());
        fragment.assertExecutedEntranceTransition();

        final ObjectAdapter adapter1 = fragment.getAdapter();
        assertNull(fragment.mMainFragmentListRowDataAdapter);
        assertTrue(adapter1.hasObserver());
        final ObjectAdapter adapter2 = createListRowsAdapter();
        InstrumentationRegistry.getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                fragment.setAdapter(adapter2);
            }
        });
        fragment.waitRowsFragment();
        // adapter1 should no longer has observer and adapter2 will have observer
        assertFalse(adapter1.hasObserver());
        assertSame(adapter2, fragment.getAdapter());
        assertTrue(adapter2.hasObserver());
        assertTrue(fragment.mMainFragmentListRowDataAdapter.hasObserver());
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.LOLLIPOP)
    @Test
    public void browseFragmentNotifyDataChangePageToListRow() throws InterruptedException {
        final SingleFragmentTestActivity activity = launchAndWaitActivity(
                RowsFragmentTest.F_2PageRow3ListRow.class, 2000);
        final F_2PageRow3ListRow fragment = ((F_2PageRow3ListRow) activity.getTestFragment());
        fragment.assertExecutedEntranceTransition();

        final ArrayObjectAdapter adapter1 = (ArrayObjectAdapter) fragment.getAdapter();
        assertNull(fragment.mMainFragmentListRowDataAdapter);
        assertTrue(adapter1.hasObserver());
        InstrumentationRegistry.getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                ObjectAdapter listRowAdapter = createListRowAdapter();
                HeaderItem header = new HeaderItem(0, "Row 0 changed");
                adapter1.removeItems(0, 1);
                adapter1.add(0, new ListRow(header, listRowAdapter));
            }
        });
        fragment.waitRowsFragment();
        assertTrue(adapter1.hasObserver());
        assertTrue(fragment.mMainFragmentListRowDataAdapter.hasObserver());
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.LOLLIPOP)
    @Test
    public void browseFragmentNotifyItemChangePageToListRow() throws InterruptedException {
        final SingleFragmentTestActivity activity = launchAndWaitActivity(
                RowsFragmentTest.F_2PageRow3ListRow.class, 2000);
        final F_2PageRow3ListRow fragment = ((F_2PageRow3ListRow) activity.getTestFragment());
        fragment.assertExecutedEntranceTransition();

        final ArrayObjectAdapter adapter1 = (ArrayObjectAdapter) fragment.getAdapter();
        assertNull(fragment.mMainFragmentListRowDataAdapter);
        assertTrue(adapter1.hasObserver());
        InstrumentationRegistry.getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                ObjectAdapter listRowAdapter = createListRowAdapter();
                HeaderItem header = new HeaderItem(0, "Row 0 changed");
                adapter1.replace(0, new ListRow(header, listRowAdapter));
            }
        });
        fragment.waitRowsFragment();
        assertTrue(adapter1.hasObserver());
        assertTrue(fragment.mMainFragmentListRowDataAdapter.hasObserver());
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.LOLLIPOP)
    @Test
    public void browseFragmentRestore() throws InterruptedException {
        final SingleFragmentTestActivity activity = launchAndWaitActivity(
                RowsFragmentTest.F_standard.class, 2000);
        final F_standard fragment = ((F_standard) activity.getTestFragment());
        fragment.assertExecutedEntranceTransition();

        // select item 2 on row 1
        selectAndWaitFragmentAnimation(fragment, 1, 2);
        // save activity to state
        Bundle savedState = saveActivityState(activity);
        activity.finish();

        // recreate activity with saved state
        SingleFragmentTestActivity activity2 = launchAndWaitActivity(
                RowsFragmentTest.F_standard.class,
                new Options().savedInstance(savedState), 2000);
        final F_standard fragment2 = ((F_standard) activity2.getTestFragment());
        // validate restored activity selected row and selected item
        fragment2.assertNoEntranceTransition();
        assertEquals(1, fragment2.getSelectedPosition());
        assertEquals(2, ((ListRowPresenter.ViewHolder) fragment2.getSelectedRowViewHolder())
                .getSelectedPosition());
        activity2.finish();
    }

    public static class MyPageRow extends PageRow {
        public int type;
        public MyPageRow(int type) {
            super(new HeaderItem(100 + type, "page type " + type));
            this.type = type;
        }
    }

    /**
     * A RowsFragment that is a separate page in BrowseFragment.
     */
    public static class SampleRowsFragment extends RowsFragment {
        public SampleRowsFragment() {
            // simulates late data loading:
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    setAdapter(createListRowsAdapter());
                    if (getMainFragmentAdapter() != null) {
                        getMainFragmentAdapter().getFragmentHost()
                                .notifyDataReady(getMainFragmentAdapter());
                    }
                }
            }, 500);
        }
    }

    /**
     * A custom Fragment that is a separate page in BrowseFragment.
     */
    public static class SampleFragment extends Fragment implements
            BrowseFragment.MainFragmentAdapterProvider {

        public static class PageFragmentAdapterImpl extends
                BrowseFragment.MainFragmentAdapter<SampleFragment> {

            public PageFragmentAdapterImpl(SampleFragment fragment) {
                super(fragment);
                setScalingEnabled(true);
            }

            @Override
            public void setEntranceTransitionState(boolean state) {
                getFragment().setEntranceTransitionState(state);
            }
        }

        final PageFragmentAdapterImpl mMainFragmentAdapter = new PageFragmentAdapterImpl(this);

        void setEntranceTransitionState(boolean state) {
            final View view = getView();
            int visibility = state ? View.VISIBLE : View.INVISIBLE;
            view.findViewById(R.id.tv1).setVisibility(visibility);
            view.findViewById(R.id.tv2).setVisibility(visibility);
            view.findViewById(R.id.tv3).setVisibility(visibility);
        }

        @Override
        public View onCreateView(
                final LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            View view = inflater.inflate(R.layout.page_fragment, container, false);
            return view;
        }

        @Override
        public void onViewCreated(View view, Bundle savedInstanceState) {
            // static layout has view and data ready immediately
            mMainFragmentAdapter.getFragmentHost().notifyViewCreated(mMainFragmentAdapter);
            mMainFragmentAdapter.getFragmentHost().notifyDataReady(mMainFragmentAdapter);
        }

        @Override
        public BrowseFragment.MainFragmentAdapter getMainFragmentAdapter() {
            return mMainFragmentAdapter;
        }
    }

    /**
     * Create BrowseFragmentAdapter with 3 ListRows and 2 PageRows
     */
    private static ArrayObjectAdapter create3ListRow2PageRowAdapter() {
        ListRowPresenter lrp = new ListRowPresenter();
        final ArrayObjectAdapter adapter = new ArrayObjectAdapter(lrp);
        for (int i = 0; i < 3; i++) {
            StableIdAdapter listRowAdapter = new StableIdAdapter();
            listRowAdapter.setHasStableIds(false);
            listRowAdapter.setPresenterSelector(
                    new SinglePresenterSelector(sCardPresenter));
            int index = 0;
            listRowAdapter.mList.add(index++);
            listRowAdapter.mList.add(index++);
            listRowAdapter.mList.add(index++);
            HeaderItem header = new HeaderItem(i, "Row " + i);
            adapter.add(new ListRow(header, listRowAdapter));
        }
        adapter.add(new MyPageRow(0));
        adapter.add(new MyPageRow(1));
        return adapter;
    }

    /**
     * Create BrowseFragmentAdapter with 2 PageRows then 3 ListRow
     */
    private static ArrayObjectAdapter create2PageRow3ListRow() {
        ListRowPresenter lrp = new ListRowPresenter();
        final ArrayObjectAdapter adapter = new ArrayObjectAdapter(lrp);
        adapter.add(new MyPageRow(0));
        adapter.add(new MyPageRow(1));
        for (int i = 0; i < 3; i++) {
            StableIdAdapter listRowAdapter = new StableIdAdapter();
            listRowAdapter.setHasStableIds(false);
            listRowAdapter.setPresenterSelector(
                    new SinglePresenterSelector(sCardPresenter));
            int index = 0;
            listRowAdapter.mList.add(index++);
            listRowAdapter.mList.add(index++);
            listRowAdapter.mList.add(index++);
            HeaderItem header = new HeaderItem(i, "Row " + i);
            adapter.add(new ListRow(header, listRowAdapter));
        }
        return adapter;
    }

    static class MyFragmentFactory extends BrowseFragment.FragmentFactory {
        @Override
        public Fragment createFragment(Object rowObj) {
            MyPageRow row = (MyPageRow) rowObj;
            if (row.type == 0) {
                return new SampleRowsFragment();
            } else if (row.type == 1) {
                return new SampleFragment();
            }
            return null;
        }
    }

    /**
     * A BrowseFragment with three ListRows, one SampleRowsFragment and one SampleFragment.
     */
    public static class F_3ListRow2PageRow extends F_Base {
        public F_3ListRow2PageRow() {
            getMainFragmentRegistry().registerFragment(MyPageRow.class, new MyFragmentFactory());
        }
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            if (savedInstanceState == null) {
                prepareEntranceTransition();
            }
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    setAdapter(create3ListRow2PageRowAdapter());
                    startEntranceTransition();
                }
            }, 100);
        }
    }

    /**
     * A BrowseFragment with three ListRows, one SampleRowsFragment and one SampleFragment.
     */
    public static class F_2PageRow3ListRow extends F_Base {
        public F_2PageRow3ListRow() {
            getMainFragmentRegistry().registerFragment(MyPageRow.class, new MyFragmentFactory());
        }
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            if (savedInstanceState == null) {
                prepareEntranceTransition();
            }
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    setAdapter(create2PageRow3ListRow());
                    startEntranceTransition();
                }
            }, 100);
        }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.LOLLIPOP)
    @Test
    public void mixedBrowseFragmentRestoreToListRow() throws Throwable {
        final SingleFragmentTestActivity activity = launchAndWaitActivity(
                RowsFragmentTest.F_3ListRow2PageRow.class, 2000);
        final F_3ListRow2PageRow fragment = ((F_3ListRow2PageRow) activity.getTestFragment());
        fragment.assertExecutedEntranceTransition();

        // select item 2 on row 1.
        selectAndWaitFragmentAnimation(fragment, 1, 2);
        Bundle savedState = saveActivityState(activity);
        activity.finish();

        // start a new activity with the state
        SingleFragmentTestActivity activity2 = launchAndWaitActivity(
                RowsFragmentTest.F_standard.class,
                new Options().savedInstance(savedState), 2000);
        final F_3ListRow2PageRow fragment2 = ((F_3ListRow2PageRow) activity2.getTestFragment());
        assertFalse(fragment2.isShowingHeaders());
        fragment2.assertNoEntranceTransition();
        assertEquals(1, fragment2.getSelectedPosition());
        assertEquals(2, ((ListRowPresenter.ViewHolder) fragment2.getSelectedRowViewHolder())
                .getSelectedPosition());
        activity2.finish();
    }

    void mixedBrowseFragmentRestoreToSampleRowsFragment(final boolean hideFastLane)
            throws Throwable {
        final SingleFragmentTestActivity activity = launchAndWaitActivity(
                RowsFragmentTest.F_3ListRow2PageRow.class, 2000);
        final F_3ListRow2PageRow fragment = ((F_3ListRow2PageRow) activity.getTestFragment());
        fragment.assertExecutedEntranceTransition();

        // select row 3 which is mapped to SampleRowsFragment.
        InstrumentationRegistry.getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                fragment.setSelectedPosition(3, true);
            }
        });
        // Wait SampleRowsFragment being created
        final SampleRowsFragment mainFragment = (SampleRowsFragment) fragment.waitPageFragment(
                SampleRowsFragment.class);
        InstrumentationRegistry.getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                if (hideFastLane) {
                    fragment.startHeadersTransition(false);
                }
            }
        });
        // Wait header transition finishes
        waitForHeaderTransition(fragment);
        // Select item 1 on row 1 in SampleRowsFragment
        InstrumentationRegistry.getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                mainFragment.setSelectedPosition(1, true,
                        new ListRowPresenter.SelectItemViewHolderTask(1));
            }
        });
        // Save activity state
        Bundle savedState = saveActivityState(activity);
        activity.finish();

        SingleFragmentTestActivity activity2 = launchAndWaitActivity(
                RowsFragmentTest.F_3ListRow2PageRow.class,
                new Options().savedInstance(savedState), 2000);
        final F_3ListRow2PageRow fragment2 = ((F_3ListRow2PageRow) activity2.getTestFragment());
        final SampleRowsFragment mainFragment2 = (SampleRowsFragment) fragment2.waitPageFragment(
                SampleRowsFragment.class);
        assertEquals(!hideFastLane, fragment2.isShowingHeaders());
        fragment2.assertNoEntranceTransition();
        // Validate BrowseFragment selected row 3 (mapped to SampleRowsFragment)
        assertEquals(3, fragment2.getSelectedPosition());
        // Validate SampleRowsFragment's selected row and selected item
        assertEquals(1, mainFragment2.getSelectedPosition());
        assertEquals(1, ((ListRowPresenter.ViewHolder) mainFragment2
                .findRowViewHolderByPosition(1)).getSelectedPosition());
        activity2.finish();
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.LOLLIPOP)
    @Test
    public void mixedBrowseFragmentRestoreToSampleRowsFragmentHideFastLane() throws Throwable {
        mixedBrowseFragmentRestoreToSampleRowsFragment(true);

    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.LOLLIPOP)
    @Test
    public void mixedBrowseFragmentRestoreToSampleRowsFragmentShowFastLane() throws Throwable {
        mixedBrowseFragmentRestoreToSampleRowsFragment(false);
    }

    void mixedBrowseFragmentRestoreToSampleFragment(final boolean hideFastLane)
            throws Throwable {
        final SingleFragmentTestActivity activity = launchAndWaitActivity(
                RowsFragmentTest.F_3ListRow2PageRow.class, 2000);
        final F_3ListRow2PageRow fragment = ((F_3ListRow2PageRow) activity.getTestFragment());
        fragment.assertExecutedEntranceTransition();

        // select row 3 which is mapped to SampleFragment.
        InstrumentationRegistry.getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                fragment.setSelectedPosition(4, true);
            }
        });
        // Wait SampleFragment to be created
        final SampleFragment mainFragment = (SampleFragment) fragment.waitPageFragment(
                SampleFragment.class);
        InstrumentationRegistry.getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                if (hideFastLane) {
                    fragment.startHeadersTransition(false);
                }
            }
        });
        waitForHeaderTransition(fragment);
        InstrumentationRegistry.getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                // change TextView content which should be saved in states.
                TextView t = mainFragment.getView().findViewById(R.id.tv2);
                t.setText("changed text");
            }
        });
        // Save activity state
        Bundle savedState = saveActivityState(activity);
        activity.finish();

        SingleFragmentTestActivity activity2 = launchAndWaitActivity(
                RowsFragmentTest.F_3ListRow2PageRow.class,
                new Options().savedInstance(savedState), 2000);
        final F_3ListRow2PageRow fragment2 = ((F_3ListRow2PageRow) activity2.getTestFragment());
        final SampleFragment mainFragment2 = (SampleFragment) fragment2.waitPageFragment(
                SampleFragment.class);
        assertEquals(!hideFastLane, fragment2.isShowingHeaders());
        fragment2.assertNoEntranceTransition();
        // Validate BrowseFragment selected row 3 (mapped to SampleFragment)
        assertEquals(4, fragment2.getSelectedPosition());
        // Validate SampleFragment's view states are restored
        TextView t = mainFragment2.getView().findViewById(R.id.tv2);
        assertEquals("changed text", t.getText().toString());
        activity2.finish();
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.LOLLIPOP)
    @Test
    public void mixedBrowseFragmentRestoreToSampleFragmentHideFastLane() throws Throwable {
        mixedBrowseFragmentRestoreToSampleFragment(true);

    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.LOLLIPOP)
    @Test
    public void mixedBrowseFragmentRestoreToSampleFragmentShowFastLane() throws Throwable {
        mixedBrowseFragmentRestoreToSampleFragment(false);
    }


}
