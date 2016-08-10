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

package android.support.v7.widget.helper;

import static android.support.v7.widget.helper.ItemTouchHelper.*;

import static org.junit.Assert.*;

import android.os.Build;
import android.support.test.filters.SdkSuppress;
import android.support.test.runner.AndroidJUnit4;
import android.support.v4.view.ViewCompat;
import android.support.v7.util.TouchUtils;
import android.support.v7.widget.BaseRecyclerViewInstrumentationTest;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.WrappedRecyclerView;
import android.test.suitebuilder.annotation.MediumTest;
import android.view.Gravity;
import android.view.View;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;

@MediumTest
@RunWith(AndroidJUnit4.class)
public class ItemTouchHelperTest extends BaseRecyclerViewInstrumentationTest {

    TestAdapter mAdapter;

    TestLayoutManager mLayoutManager;

    private LoggingCalback mCalback;

    private LoggingItemTouchHelper mItemTouchHelper;

    private WrappedRecyclerView mWrappedRecyclerView;

    private Boolean mSetupRTL;

    public ItemTouchHelperTest() {
        super(false);
    }

    private RecyclerView setup(int dragDirs, int swipeDirs) throws Throwable {
        mWrappedRecyclerView = inflateWrappedRV();
        mAdapter = new TestAdapter(10);
        mLayoutManager = new TestLayoutManager() {
            @Override
            public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
                detachAndScrapAttachedViews(recycler);
                layoutRange(recycler, 0, Math.min(5, state.getItemCount()));
                layoutLatch.countDown();
            }

            @Override
            public boolean canScrollHorizontally() {
                return false;
            }

            @Override
            public boolean supportsPredictiveItemAnimations() {
                return false;
            }
        };
        mWrappedRecyclerView.setFakeRTL(mSetupRTL);
        mWrappedRecyclerView.setAdapter(mAdapter);
        mWrappedRecyclerView.setLayoutManager(mLayoutManager);
        mCalback = new LoggingCalback(dragDirs, swipeDirs);
        mItemTouchHelper = new LoggingItemTouchHelper(mCalback);
        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                mItemTouchHelper.attachToRecyclerView(mWrappedRecyclerView);
            }
        });

        return mWrappedRecyclerView;
    }

    @Test
    public void swipeLeft() throws Throwable {
        basicSwipeTest(LEFT, LEFT | RIGHT, -getActivity().getWindow().getDecorView().getWidth());
    }

    @Test
    public void swipeRight() throws Throwable {
        basicSwipeTest(RIGHT, LEFT | RIGHT, getActivity().getWindow().getDecorView().getWidth());
    }

    @Test
    public void swipeStart() throws Throwable {
        basicSwipeTest(START, START | END, -getActivity().getWindow().getDecorView().getWidth());
    }

    @Test
    public void swipeEnd() throws Throwable {
        basicSwipeTest(END, START | END, getActivity().getWindow().getDecorView().getWidth());
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.JELLY_BEAN_MR1)
    @Test
    public void swipeStartInRTL() throws Throwable {
        mSetupRTL = true;
        basicSwipeTest(START, START | END, getActivity().getWindow().getDecorView().getWidth());
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.JELLY_BEAN_MR1)
    @Test
    public void swipeEndInRTL() throws Throwable {
        mSetupRTL = true;
        basicSwipeTest(END, START | END, -getActivity().getWindow().getDecorView().getWidth());
    }

    private void setLayoutDirection(final View view, final int layoutDir) throws Throwable {
        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                ViewCompat.setLayoutDirection(view, layoutDir);
            }
        });
    }

    public void basicSwipeTest(int dir, int swipeDirs, int targetX) throws Throwable {
        final RecyclerView recyclerView = setup(0, swipeDirs);
        mLayoutManager.expectLayouts(1);
        setRecyclerView(recyclerView);
        mLayoutManager.waitForLayout(1);

        final RecyclerView.ViewHolder target = mRecyclerView
                .findViewHolderForAdapterPosition(1);
        TouchUtils.dragViewToX(getInstrumentation(), target.itemView, Gravity.CENTER, targetX);
        Thread.sleep(100); //wait for animation end
        final SwipeRecord swipe = mCalback.getSwipe(target);
        assertNotNull(swipe);
        assertEquals(dir, swipe.dir);
        assertEquals(1, mItemTouchHelper.mRecoverAnimations.size());
        assertEquals(1, mItemTouchHelper.mPendingCleanup.size());
        // get rid of the view
        mLayoutManager.expectLayouts(1);
        mAdapter.deleteAndNotify(1, 1);
        mLayoutManager.waitForLayout(1);
        waitForAnimations();
        assertEquals(0, mItemTouchHelper.mRecoverAnimations.size());
        assertEquals(0, mItemTouchHelper.mPendingCleanup.size());
        assertTrue(mCalback.isCleared(target));
    }

    private void waitForAnimations() throws InterruptedException {
        while (mRecyclerView.getItemAnimator().isRunning()) {
            Thread.sleep(100);
        }
    }

    private static class LoggingCalback extends SimpleCallback {

        private List<MoveRecord> mMoveRecordList = new ArrayList<MoveRecord>();

        private List<SwipeRecord> mSwipeRecords = new ArrayList<SwipeRecord>();

        private List<RecyclerView.ViewHolder> mCleared = new ArrayList<RecyclerView.ViewHolder>();

        LoggingCalback(int dragDirs, int swipeDirs) {
            super(dragDirs, swipeDirs);
        }

        @Override
        public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder,
                RecyclerView.ViewHolder target) {
            mMoveRecordList.add(new MoveRecord(viewHolder, target));
            return true;
        }

        @Override
        public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
            mSwipeRecords.add(new SwipeRecord(viewHolder, direction));
        }

        public MoveRecord getMove(RecyclerView.ViewHolder vh) {
            for (MoveRecord move : mMoveRecordList) {
                if (move.from == vh) {
                    return move;
                }
            }
            return null;
        }

        @Override
        public void clearView(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
            super.clearView(recyclerView, viewHolder);
            mCleared.add(viewHolder);
        }

        public SwipeRecord getSwipe(RecyclerView.ViewHolder vh) {
            for (SwipeRecord swipe : mSwipeRecords) {
                if (swipe.viewHolder == vh) {
                    return swipe;
                }
            }
            return null;
        }

        public boolean isCleared(RecyclerView.ViewHolder vh) {
            return mCleared.contains(vh);
        }
    }

    private static class LoggingItemTouchHelper extends ItemTouchHelper {

        public LoggingItemTouchHelper(Callback callback) {
            super(callback);
        }
    }

    private static class SwipeRecord {

        RecyclerView.ViewHolder viewHolder;

        int dir;

        public SwipeRecord(RecyclerView.ViewHolder viewHolder, int dir) {
            this.viewHolder = viewHolder;
            this.dir = dir;
        }
    }

    private static class MoveRecord {

        final int fromPos, toPos;

        RecyclerView.ViewHolder from, to;

        MoveRecord(RecyclerView.ViewHolder from, RecyclerView.ViewHolder to) {
            this.from = from;
            this.to = to;
            fromPos = from.getAdapterPosition();
            toPos = to.getAdapterPosition();
        }
    }
}
