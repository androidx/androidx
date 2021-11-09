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

import static androidx.recyclerview.widget.ItemTouchHelper.END;
import static androidx.recyclerview.widget.ItemTouchHelper.LEFT;
import static androidx.recyclerview.widget.ItemTouchHelper.RIGHT;
import static androidx.recyclerview.widget.ItemTouchHelper.START;
import static androidx.recyclerview.widget.ItemTouchHelper.SimpleCallback;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.animation.ValueAnimator;
import android.os.Build;
import android.view.Gravity;
import android.view.ViewConfiguration;

import androidx.annotation.NonNull;
import androidx.core.util.Pair;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.FlakyTest;
import androidx.test.filters.LargeTest;
import androidx.test.filters.SdkSuppress;
import androidx.test.filters.Suppress;
import androidx.testutils.AnimationDurationScaleRule;
import androidx.testutils.PollingCheck;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;

@LargeTest
@RunWith(AndroidJUnit4.class)
public class ItemTouchHelperTest extends BaseRecyclerViewInstrumentationTest {

    private static class RecyclerViewState {
        public TestAdapter mAdapter;
        public TestLayoutManager mLayoutManager;
        public WrappedRecyclerView mWrappedRecyclerView;
    }

    private LoggingCallback mCallback;

    private LoggingItemTouchHelper mItemTouchHelper;

    private Boolean mSetupRTL;

    @Rule
    public final AnimationDurationScaleRule mAnimationDurationScaleRule =
            AnimationDurationScaleRule.create();


    public ItemTouchHelperTest() {
        super(false);
    }

    private RecyclerViewState setupRecyclerView() throws Throwable {
        RecyclerViewState rvs = new RecyclerViewState();
        rvs.mWrappedRecyclerView = inflateWrappedRV();
        rvs.mAdapter = new TestAdapter(10);
        rvs.mLayoutManager = new TestLayoutManager() {
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
        rvs.mWrappedRecyclerView.setFakeRTL(mSetupRTL);
        rvs.mWrappedRecyclerView.setAdapter(rvs.mAdapter);
        rvs.mWrappedRecyclerView.setLayoutManager(rvs.mLayoutManager);
        return rvs;
    }

    private RecyclerViewState setupItemTouchHelper(final RecyclerViewState rvs, int dragDirs,
            int swipeDirs) throws Throwable {
        mCallback = new LoggingCallback(dragDirs, swipeDirs);
        mItemTouchHelper = new LoggingItemTouchHelper(mCallback);
        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mItemTouchHelper.attachToRecyclerView(rvs.mWrappedRecyclerView);
            }
        });

        return rvs;
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

    // Test is disabled as it is flaky.
    @Suppress
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.JELLY_BEAN_MR1)
    @Test
    public void swipeStartInRTL() throws Throwable {
        mSetupRTL = true;
        basicSwipeTest(START, START | END, getActivity().getWindow().getDecorView().getWidth());
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.JELLY_BEAN_MR1)
    @Test
    @FlakyTest(bugId = 190192628)
    public void swipeEndInRTL() throws Throwable {
        mSetupRTL = true;
        basicSwipeTest(END, START | END, -getActivity().getWindow().getDecorView().getWidth());
    }

    @Test
    public void attachToRecyclerView_duringLongPressWithNullRV_doesNotCrash() throws Throwable {

        // Arrange
        final RecyclerViewState rvs = setupItemTouchHelper(setupRecyclerView(), END, 0);
        rvs.mLayoutManager.expectLayouts(1);
        setRecyclerView(rvs.mWrappedRecyclerView);
        rvs.mLayoutManager.waitForLayout(1);
        final RecyclerView.ViewHolder target = mRecyclerView
                .findViewHolderForAdapterPosition(1);

        float longPressTime = ViewConfiguration.getLongPressTimeout() * 1.5f;

        // Act: Long press the item, and half way through the long press, attach the
        // ItemTouchHelper to null, then wait for the long press to finish.
        TouchUtils.longClickView(getInstrumentation(), target.itemView, longPressTime,
                new Runnable() {
                    @Override
                    public void run() {
                        try {
                            mActivityRule.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    mItemTouchHelper.attachToRecyclerView(null);
                                }
                            });
                        } catch (Throwable throwable) {
                            postExceptionToInstrumentation(throwable);
                        }
                    }
                });

        // Assert: Make sure we didn't get an exception on the main thread.
        checkForMainThreadException();
    }

    @Test
    public void attachToRecyclerView_calledDuringLongPressWithAnRV_longPressListenerDoesNotFire()
            throws Throwable {

        // Arrange

        // Add the main RV, already attached to the ItemTouchHelper.
        final RecyclerViewState rvs = setupItemTouchHelper(setupRecyclerView(), END, 0);
        rvs.mLayoutManager.expectLayouts(1);
        setRecyclerView(rvs.mWrappedRecyclerView);
        rvs.mLayoutManager.waitForLayout(1);

        // Add a second RV that we will attach to.
        final RecyclerViewState rvs2 = setupRecyclerView();
        rvs2.mLayoutManager.expectLayouts(1);
        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                getActivity().getContainer().addView(rvs2.mWrappedRecyclerView);
            }
        });
        rvs2.mLayoutManager.waitForLayout(1);

        float longPressTime = ViewConfiguration.getLongPressTimeout() * 1.5f;

        // The RV item we will long press.
        final RecyclerView.ViewHolder target = mRecyclerView
                .findViewHolderForAdapterPosition(1);

        // Act

        // Long press the item, and half way through the long press, attach the ItemTouchHelper
        // to the other RV, then wait for the long press to finish.
        TouchUtils.longClickView(getInstrumentation(), target.itemView, longPressTime,
                new Runnable() {
                    @Override
                    public void run() {
                        try {
                            mActivityRule.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    mItemTouchHelper.attachToRecyclerView(
                                            rvs2.mWrappedRecyclerView);
                                }
                            });
                        } catch (Throwable throwable) {
                            postExceptionToInstrumentation(throwable);
                        }
                    }
                });

        // Assert

        // The long press on the original item should not have triggered a long press on the
        // ItemTouchHelper.
        //
        // This check is a stand in for a better way to see if the ItemTouchHelper's internal
        // long press callback method was called.  mCallback.mHasDragFlag will only be 0 if the
        // ItemTouchHelper's long press call back method was never called.
        assertEquals(0, mCallback.mHasDragFlag.size());

        // Make sure we didn't get an exception the main thread.
        checkForMainThreadException();
    }

    public void basicSwipeTest(int dir, int swipeDirs, int targetX) throws Throwable {
        final RecyclerViewState rvs = setupItemTouchHelper(setupRecyclerView(), 0, swipeDirs);
        rvs.mLayoutManager.expectLayouts(1);
        setRecyclerView(rvs.mWrappedRecyclerView);
        rvs.mLayoutManager.waitForLayout(1);

        final RecyclerView.ViewHolder target = mRecyclerView
                .findViewHolderForAdapterPosition(1);
        TouchUtils.dragViewToX(getInstrumentation(), target.itemView, Gravity.CENTER, targetX);

        PollingCheck.waitFor(1000, new PollingCheck.PollingCheckCondition() {
            @Override
            public boolean canProceed() {
                return mCallback.getSwipe(target) != null;
            }
        });
        final SwipeRecord swipe = mCallback.getSwipe(target);
        assertNotNull(swipe);
        assertEquals(dir, swipe.dir);
        assertEquals(1, mItemTouchHelper.mRecoverAnimations.size());
        assertEquals(1, mItemTouchHelper.mPendingCleanup.size());
        // get rid of the view
        rvs.mLayoutManager.expectLayouts(1);
        rvs.mAdapter.deleteAndNotify(1, 1);
        rvs.mLayoutManager.waitForLayout(1);
        waitForAnimations();
        assertEquals(0, mItemTouchHelper.mRecoverAnimations.size());
        assertEquals(0, mItemTouchHelper.mPendingCleanup.size());
        assertTrue(mCallback.isCleared(target));
    }

    @Test
    public void attachToRecyclerView_recoveryAnimRunningNullRv_recoveryAnimStopped()
            throws Throwable {
        // enable animations for this test
        mAnimationDurationScaleRule.setAnimationDurationScale(1f);
        // Arrange
        final RecyclerViewState rvs = setupItemTouchHelper(setupRecyclerView(), 0,
                LEFT | RIGHT);
        rvs.mLayoutManager.expectLayouts(1);
        setRecyclerView(rvs.mWrappedRecyclerView);
        rvs.mLayoutManager.waitForLayout(1);

        // Drag the child view to the right by as many pixels as the child is wide, then release
        // without waiting for idle sync.
        int targetX = mRecyclerView.getChildAt(0).getWidth();
        final RecyclerView.ViewHolder target = mRecyclerView.findViewHolderForAdapterPosition(
                1);
        TouchUtils.dragViewToX(getInstrumentation(), target.itemView, Gravity.CENTER, targetX,
                false);

        // Wait for there to be a recovery animation which means that the item is in the process
        // of animating.
        PollingCheck.waitFor(1000, new PollingCheck.PollingCheckCondition() {
            @Override
            public boolean canProceed() {
                return mItemTouchHelper.mRecoverAnimations.size() > 0;
            }
        });

        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // Verify the assumption that at this time there should be one, and only one,
                // recovery animation running.
                assertEquals(1, mItemTouchHelper.mRecoverAnimations.size());
                ValueAnimator valueAnimator =
                        mItemTouchHelper.mRecoverAnimations.get(0).mValueAnimator;
                // Verify the assumption that the animator should be currently running.
                assertTrue(valueAnimator.isRunning());

                // Act

                mItemTouchHelper.attachToRecyclerView(null);

                // Assert

                // After we attach to a null RecyclerView, the animation should no longer be
                // running.
                assertFalse(valueAnimator.isRunning());
            }
        });
    }

    private void waitForAnimations() throws InterruptedException {
        while (mRecyclerView.getItemAnimator().isRunning()) {
            Thread.sleep(100);
        }
    }

    private static class LoggingCallback extends SimpleCallback {

        private List<MoveRecord> mMoveRecordList = new ArrayList<MoveRecord>();

        private List<SwipeRecord> mSwipeRecords = new ArrayList<SwipeRecord>();

        private List<RecyclerView.ViewHolder> mCleared = new ArrayList<RecyclerView.ViewHolder>();

        private List<Pair<RecyclerView, RecyclerView.ViewHolder>> mHasDragFlag = new ArrayList<>();

        LoggingCallback(int dragDirs, int swipeDirs) {
            super(dragDirs, swipeDirs);
        }

        @Override
        public boolean onMove(@NonNull RecyclerView recyclerView,
                @NonNull RecyclerView.ViewHolder viewHolder,
                @NonNull RecyclerView.ViewHolder target) {
            mMoveRecordList.add(new MoveRecord(viewHolder, target));
            return true;
        }

        @Override
        public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
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
        public void clearView(@NonNull RecyclerView recyclerView,
                @NonNull RecyclerView.ViewHolder viewHolder) {
            super.clearView(recyclerView, viewHolder);
            mCleared.add(viewHolder);
        }

        @Override
        boolean hasDragFlag(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
            mHasDragFlag.add(new Pair<>(recyclerView, viewHolder));
            return super.hasDragFlag(recyclerView, viewHolder);
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
            fromPos = from.getAbsoluteAdapterPosition();
            toPos = to.getAbsoluteAdapterPosition();
        }
    }
}
