// CHECKSTYLE:OFF Generated code
/* This file is auto-generated from BrowseSupportFragmentTest.java.  DO NOT MODIFY. */

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
package androidx.leanback.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.app.Fragment;
import androidx.leanback.test.R;
import androidx.leanback.testutils.LeakDetector;
import androidx.leanback.testutils.PollingCheck;
import androidx.leanback.widget.ArrayObjectAdapter;
import androidx.leanback.widget.ItemBridgeAdapter;
import androidx.leanback.widget.ListRowPresenter;
import androidx.leanback.widget.Presenter;
import androidx.leanback.widget.Row;
import androidx.leanback.widget.RowPresenter;
import androidx.leanback.widget.VerticalGridView;
import androidx.recyclerview.widget.RecyclerView;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.filters.SdkSuppress;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.ActivityTestRule;

import org.junit.After;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;

@LargeTest
@RunWith(AndroidJUnit4.class)
public class BrowseFragmentTest {

    static final String TAG = "BrowseFragmentTest";
    static final long WAIT_TRANSIITON_TIMEOUT = 10000;

    @Rule
    public ActivityTestRule<BrowseFragmentTestActivity> activityTestRule =
            new ActivityTestRule<>(BrowseFragmentTestActivity.class, false, false);
    private BrowseFragmentTestActivity mActivity;

    @After
    public void afterTest() throws Throwable {
        activityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (mActivity != null) {
                    mActivity.finish();
                    mActivity = null;
                }
            }
        });
    }

    void waitForEntranceTransitionFinished() {
        PollingCheck.waitFor(WAIT_TRANSIITON_TIMEOUT, new PollingCheck.PollingCheckCondition() {
            @Override
            public boolean canProceed() {
                if (Build.VERSION.SDK_INT >= 21) {
                    return mActivity.getBrowseTestFragment() != null
                            && mActivity.getBrowseTestFragment().mEntranceTransitionEnded;
                } else {
                    // when entrance transition not supported, wait main fragment loaded.
                    return mActivity.getBrowseTestFragment() != null
                            && mActivity.getBrowseTestFragment().getMainFragment() != null;
                }
            }
        });
    }

    void waitForHeaderTransitionFinished() {
        View row = mActivity.getBrowseTestFragment().getRowsFragment().getRowViewHolder(
                mActivity.getBrowseTestFragment().getSelectedPosition()).view;
        PollingCheck.waitFor(WAIT_TRANSIITON_TIMEOUT, new PollingCheck.ViewStableOnScreen(row));
        PollingCheck.waitFor(WAIT_TRANSIITON_TIMEOUT, new PollingCheck.PollingCheckCondition() {
            public boolean canProceed() {
                return !mActivity.getBrowseTestFragment().isInHeadersTransition();
            }
        });
    }

    void waitForShowingHeaders() {
        PollingCheck.waitFor(WAIT_TRANSIITON_TIMEOUT, new PollingCheck.PollingCheckCondition() {
            public boolean canProceed() {
                return mActivity.getBrowseTestFragment().isShowingHeaders();
            }
        });
    }

    void waitForHidingHeaders() {
        PollingCheck.waitFor(WAIT_TRANSIITON_TIMEOUT, new PollingCheck.PollingCheckCondition() {
            public boolean canProceed() {
                return !mActivity.getBrowseTestFragment().isShowingHeaders();
            }
        });
    }

    @Test
    public void testTouchMode() throws Throwable {
        Intent intent = new Intent();
        intent.putExtra(BrowseFragmentTestActivity.EXTRA_ADD_TO_BACKSTACK , true);
        intent.putExtra(BrowseFragmentTestActivity.EXTRA_LOAD_DATA_DELAY , 0L);
        mActivity = activityTestRule.launchActivity(intent);

        waitForEntranceTransitionFinished();

        ListRowPresenter.ViewHolder rowVh = (ListRowPresenter.ViewHolder) mActivity
                .getBrowseTestFragment().getRowsFragment().getRowViewHolder(0);
        View card = rowVh.getGridView().getChildAt(0);
        tapView(card);
        waitForHidingHeaders();
        waitForHeaderTransitionFinished();
        assertTrue(card.hasFocus());
        assertTrue(card.isInTouchMode());
        sendKeys(KeyEvent.KEYCODE_BACK);
        waitForShowingHeaders();
        waitForHeaderTransitionFinished();
        assertTrue((mActivity.getBrowseTestFragment().getHeadersFragment()
                .getVerticalGridView().getChildAt(0)).hasFocus());
    }

    @Test
    public void testTwoBackKeysWithBackStack() throws Throwable {
        final long dataLoadingDelay = 0L;
        Intent intent = new Intent();
        intent.putExtra(BrowseFragmentTestActivity.EXTRA_LOAD_DATA_DELAY, dataLoadingDelay);
        intent.putExtra(BrowseFragmentTestActivity.EXTRA_ADD_TO_BACKSTACK , true);
        mActivity = activityTestRule.launchActivity(intent);

        waitForEntranceTransitionFinished();

        assertNotNull(mActivity.getBrowseTestFragment().getMainFragment());
        sendKeys(KeyEvent.KEYCODE_DPAD_RIGHT);
        waitForHidingHeaders();
        waitForHeaderTransitionFinished();
        sendKeys(KeyEvent.KEYCODE_BACK, KeyEvent.KEYCODE_BACK);
    }

    @Test
    public void testTwoBackKeysWithoutBackStack() throws Throwable {
        final long dataLoadingDelay = 0L;
        Intent intent = new Intent();
        intent.putExtra(BrowseFragmentTestActivity.EXTRA_LOAD_DATA_DELAY, dataLoadingDelay);
        intent.putExtra(BrowseFragmentTestActivity.EXTRA_ADD_TO_BACKSTACK , false);
        mActivity = activityTestRule.launchActivity(intent);

        waitForEntranceTransitionFinished();

        assertNotNull(mActivity.getBrowseTestFragment().getMainFragment());
        sendKeys(KeyEvent.KEYCODE_DPAD_RIGHT);
        waitForHeaderTransitionFinished();
        sendKeys(KeyEvent.KEYCODE_BACK, KeyEvent.KEYCODE_BACK);
    }

    @Test
    @Ignore("b/281082608")
    public void testPressRightBeforeMainFragmentCreated() throws Throwable {
        final long dataLoadingDelay = 1000;
        Intent intent = new Intent();
        intent.putExtra(BrowseFragmentTestActivity.EXTRA_LOAD_DATA_DELAY, dataLoadingDelay);
        intent.putExtra(BrowseFragmentTestActivity.EXTRA_ADD_TO_BACKSTACK , false);
        mActivity = activityTestRule.launchActivity(intent);

        assertNull(mActivity.getBrowseTestFragment().getMainFragment());
        sendKeys(KeyEvent.KEYCODE_DPAD_RIGHT);
    }

    public static class MyRow extends Row {
    }

    public static class MyFragment extends Fragment implements
            BrowseFragment.MainFragmentAdapterProvider {
        BrowseFragment.MainFragmentAdapter<MyFragment> mMainFragmentAdapter =
                new BrowseFragment.MainFragmentAdapter<>(this);

        @Nullable
        @Override
        public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                @Nullable Bundle savedInstanceState) {
            return new FrameLayout(container.getContext());
        }

        @Override
        public BrowseFragment.MainFragmentAdapter<MyFragment> getMainFragmentAdapter() {
            return mMainFragmentAdapter;
        }
    }

    public static class MyFragmentFactory extends
            BrowseFragment.FragmentFactory<MyFragment> {
        public MyFragment createFragment(Object row) {
            return new MyFragment();
        }
    }

    @Test
    public void testPressCenterBeforeMainFragmentCreated() throws Throwable {
        Intent intent = new Intent();
        intent.putExtra(BrowseFragmentTestActivity.EXTRA_LOAD_DATA_DELAY, 0L);
        mActivity = activityTestRule.launchActivity(intent);

        final BrowseFragment fragment = mActivity.getBrowseTestFragment();
        fragment.getMainFragmentRegistry().registerFragment(MyRow.class, new MyFragmentFactory());

        final ArrayObjectAdapter adapter = new ArrayObjectAdapter(new RowPresenter() {
            protected ViewHolder createRowViewHolder(ViewGroup parent) {
                View view = new FrameLayout(parent.getContext());
                return new RowPresenter.ViewHolder(view);
            }
        });
        adapter.add(new MyRow());
        activityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                fragment.setAdapter(adapter);
            }
        });
        activityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                KeyEvent kv;
                kv = new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_CENTER);
                fragment.getView().dispatchKeyEvent(kv);
                kv = new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DPAD_CENTER);
                fragment.getView().dispatchKeyEvent(kv);
            }
        });
    }

    @Test
    public void testSelectCardOnARow() throws Throwable {
        final int selectRow = 10;
        final int selectItem = 20;
        Intent intent = new Intent();
        final long dataLoadingDelay = 0L;
        intent.putExtra(BrowseFragmentTestActivity.EXTRA_LOAD_DATA_DELAY, dataLoadingDelay);
        intent.putExtra(BrowseFragmentTestActivity.EXTRA_ADD_TO_BACKSTACK , true);
        mActivity = activityTestRule.launchActivity(intent);

        waitForEntranceTransitionFinished();

        Presenter.ViewHolderTask itemTask = Mockito.spy(
                new ItemSelectionTask(mActivity, selectRow));

        final ListRowPresenter.SelectItemViewHolderTask task =
                new ListRowPresenter.SelectItemViewHolderTask(selectItem);
        task.setItemTask(itemTask);

        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mActivity.getBrowseTestFragment().setSelectedPosition(selectRow, true, task);
            }
        });

        PollingCheck.waitFor(5000, new PollingCheck.PollingCheckCondition() {
            @Override
            public boolean canProceed() {
                return mActivity.getBrowseTestFragment().getSelectedPosition() != 0
                        && mActivity.getBrowseTestFragment().getGridView().getScrollState()
                                == RecyclerView.SCROLL_STATE_IDLE;
            }
        });
        activityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ListRowPresenter.ViewHolder row = (ListRowPresenter.ViewHolder) mActivity
                        .getBrowseTestFragment().getRowsFragment().getRowViewHolder(selectRow);
                assertNotNull(dumpRecyclerView(mActivity.getBrowseTestFragment().getGridView()), row);
                assertNotNull(row.getGridView());
                assertEquals(selectItem, row.getGridView().getSelectedPosition());
            }
        });
    }

    @Test
    public void activityRecreate_notCrash() throws Throwable {
        final long dataLoadingDelay = 0L;
        Intent intent = new Intent();
        intent.putExtra(BrowseFragmentTestActivity.EXTRA_LOAD_DATA_DELAY, dataLoadingDelay);
        intent.putExtra(BrowseFragmentTestActivity.EXTRA_ADD_TO_BACKSTACK , false);
        intent.putExtra(BrowseFragmentTestActivity.EXTRA_SET_ADAPTER_AFTER_DATA_LOAD, true);
        mActivity = activityTestRule.launchActivity(intent);

        waitForEntranceTransitionFinished();

        InstrumentationRegistry.getInstrumentation().callActivityOnRestart(mActivity);
        activityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mActivity.recreate();
            }
        });
    }


    @Test
    public void lateLoadingHeaderDisabled() throws Throwable {
        final long dataLoadingDelay = 0L;
        Intent intent = new Intent();
        intent.putExtra(BrowseFragmentTestActivity.EXTRA_LOAD_DATA_DELAY, dataLoadingDelay);
        intent.putExtra(BrowseFragmentTestActivity.EXTRA_HEADERS_STATE,
                BrowseFragment.HEADERS_DISABLED);
        mActivity = activityTestRule.launchActivity(intent);
        waitForEntranceTransitionFinished();
        PollingCheck.waitFor(new PollingCheck.PollingCheckCondition() {
            @Override
            public boolean canProceed() {
                return mActivity.getBrowseTestFragment().getGridView() != null
                        && mActivity.getBrowseTestFragment().getGridView().getChildCount() > 0;
            }
        });
    }

    public static final class EmptyFragment extends Fragment {
        EditText mEditText;

        @Override
        public View onCreateView(
                final LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            return mEditText = new EditText(container.getContext());
        }

        @Override
        public void onStart() {
            super.onStart();
            // focus IME on the new fragment because there is a memory leak that IME remembers
            // last editable view, which will cause a false reporting of leaking View.
            InputMethodManager imm =
                    (InputMethodManager) getActivity()
                            .getSystemService(Context.INPUT_METHOD_SERVICE);
            mEditText.requestFocus();
            imm.showSoftInput(mEditText, 0);
        }

        @Override
        public void onDestroyView() {
            mEditText = null;
            super.onDestroyView();
        }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.LOLLIPOP) // API 17 retains local Variable
    @Test
    public void viewLeakTest() throws Throwable {
        Intent intent = new Intent();
        intent.putExtra(BrowseFragmentTestActivity.EXTRA_HEADERS_STATE,
                BrowseFragment.HEADERS_DISABLED);
        mActivity = activityTestRule.launchActivity(intent);
        waitForEntranceTransitionFinished();

        VerticalGridView gridView = mActivity.getBrowseTestFragment().getGridView();
        LeakDetector leakDetector = new LeakDetector();
        leakDetector.observeObject(gridView);
        leakDetector.observeObject(gridView.getRecycledViewPool());
        for (int i = 0; i < gridView.getChildCount(); i++) {
            leakDetector.observeObject(gridView.getChildAt(i));
        }
        gridView = null;
        EmptyFragment emptyFragment = new EmptyFragment();
        mActivity.getFragmentManager().beginTransaction()
                .replace(R.id.main_frame, emptyFragment)
                .addToBackStack("BK")
                .commit();

        PollingCheck.waitFor(1000, new PollingCheck.PollingCheckCondition() {
            @Override
            public boolean canProceed() {
                return emptyFragment.isResumed();
            }
        });
        leakDetector.assertNoLeak();
    }

    static void tapView(View v) {
        Instrumentation inst = InstrumentationRegistry.getInstrumentation();
        int[] xy = new int[2];
        v.getLocationOnScreen(xy);

        final int viewWidth = v.getWidth();
        final int viewHeight = v.getHeight();

        final float x = xy[0] + (viewWidth / 2.0f);
        float y = xy[1] + (viewHeight / 2.0f);

        long downTime = SystemClock.uptimeMillis();
        long eventTime = SystemClock.uptimeMillis();

        MotionEvent event = MotionEvent.obtain(downTime, eventTime,
                MotionEvent.ACTION_DOWN, x, y, 0);
        inst.sendPointerSync(event);
        inst.waitForIdleSync();

        eventTime = SystemClock.uptimeMillis();
        event = MotionEvent.obtain(downTime, eventTime, MotionEvent.ACTION_UP, x, y, 0);
        inst.sendPointerSync(event);
        inst.waitForIdleSync();
    }

    private void sendKeys(int ...keys) {
        for (int i = 0; i < keys.length; i++) {
            InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(keys[i]);
        }
    }

    public static class ItemSelectionTask extends Presenter.ViewHolderTask {

        private final BrowseFragmentTestActivity activity;
        private final int expectedRow;

        public ItemSelectionTask(BrowseFragmentTestActivity activity, int expectedRow) {
            this.activity = activity;
            this.expectedRow = expectedRow;
        }

        @Override
        public void run(Presenter.ViewHolder holder) {
            android.util.Log.d(TAG, dumpRecyclerView(activity.getBrowseTestFragment()
                    .getGridView()));
            android.util.Log.d(TAG, "Row " + expectedRow + " " + activity.getBrowseTestFragment()
                    .getRowsFragment().getRowViewHolder(expectedRow), new Exception());
        }
    }

    static String dumpRecyclerView(RecyclerView recyclerView) {
        StringBuffer b = new StringBuffer();
        for (int i = 0; i < recyclerView.getChildCount(); i++) {
            View child = recyclerView.getChildAt(i);
            ItemBridgeAdapter.ViewHolder vh = (ItemBridgeAdapter.ViewHolder)
                    recyclerView.getChildViewHolder(child);
            b.append("child").append(i).append(":").append(vh);
            if (vh != null) {
                b.append(",").append(vh.getViewHolder());
            }
            b.append(";");
        }
        return b.toString();
    }
}
