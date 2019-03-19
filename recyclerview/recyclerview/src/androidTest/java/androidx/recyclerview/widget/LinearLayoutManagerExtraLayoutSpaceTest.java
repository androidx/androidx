/*
 * Copyright 2019 The Android Open Source Project
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

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;

import static androidx.recyclerview.widget.LinearLayoutManagerExtraLayoutSpaceTest.ScrollDirection.TOWARDS_END;
import static androidx.recyclerview.widget.LinearLayoutManagerExtraLayoutSpaceTest.ScrollDirection.TOWARDS_START;
import static androidx.recyclerview.widget.RecyclerView.HORIZONTAL;
import static androidx.recyclerview.widget.RecyclerView.VERTICAL;

import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.junit.Assert.assertThat;

import android.content.Context;
import android.view.View;
import android.view.ViewTreeObserver;

import androidx.annotation.NonNull;
import androidx.test.filters.MediumTest;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

@RunWith(Parameterized.class)
public class LinearLayoutManagerExtraLayoutSpaceTest extends BaseLinearLayoutManagerTest {

    private static final int RECYCLERVIEW_SIZE = 400;
    private static final int CHILD_SIZE = 140;

    enum ScrollDirection {
        TOWARDS_START, // towards left/top
        TOWARDS_END;   // towards right/bottom

        public ScrollDirection invert() {
            return this == TOWARDS_END ? TOWARDS_START : TOWARDS_END;
        }
    }

    private final Config mConfig;
    private final int mExtraLayoutSpaceLegacy;
    private final int[] mExtraLayoutSpace;
    private ExtraLayoutSpaceLayoutManager mLayoutManager;

    private int mCurrPosition = 0;
    private ScrollDirection mLastScrollDirection = TOWARDS_END;

    public LinearLayoutManagerExtraLayoutSpaceTest(Config config, int extraLayoutSpaceLegacy,
            int extraLayoutSpace) {
        mConfig = config;
        mExtraLayoutSpaceLegacy = extraLayoutSpaceLegacy;
        if (extraLayoutSpace == -1) {
            mExtraLayoutSpace = null;
        } else {
            mExtraLayoutSpace = new int[]{extraLayoutSpace, extraLayoutSpace};
        }
    }

    @Parameterized.Parameters(name = "config:{0},extraLegacySpace:{1},extraLayoutSpace:{2}")
    public static List<Object[]> getParams() {
        List<Object[]> result = new ArrayList<>();
        List<Config> configs = createBaseVariations();
        for (Config config : configs) {
            // Ignore mWrap because we have our own RV layout params
            if (!config.mWrap) {
                // Either the legacy, or the new mechanism is used,
                // so test at most one of them at a time
                result.add(new Object[]{config, -1, -1});
                for (int extraLegacySpace : new int[]{0, 200}) {
                    result.add(new Object[]{config, extraLegacySpace, -1});
                }
                for (int extraLayoutSpace : new int[]{100, 400}) {
                    result.add(new Object[]{config, -1, extraLayoutSpace});
                }
            }
        }
        return result;
    }

    private boolean isReversed() {
        return mConfig.mReverseLayout ^ mConfig.mStackFromEnd;
    }

    @Test
    @MediumTest
    public void test() throws Throwable {
        // Setup
        mConfig.mTestLayoutManager = new ExtraLayoutSpaceLayoutManager(getActivity(),
                mConfig.mOrientation, mConfig.mReverseLayout);
        setupByConfig(mConfig, false, getChildLayoutParams(), getParentLayoutParams());
        mLayoutManager = (ExtraLayoutSpaceLayoutManager) super.mLayoutManager;
        mLayoutManager.mExtraLayoutSpaceLegacy = mExtraLayoutSpaceLegacy;
        mLayoutManager.mExtraLayoutSpace = mExtraLayoutSpace;

        // Verify start position
        verifyStartPosition();
        // Jump directly to a position, moving forward
        scrollToPositionAndVerify(90, false);
        // Smooth scroll to a position, moving forward
        scrollToPositionAndVerify(100, true);
        // Jump directly to a position, moving backward
        scrollToPositionAndVerify(60, false);
        // Smooth scroll to a position, moving backward
        scrollToPositionAndVerify(50, true);
    }

    private void verifyStartPosition() throws Throwable {
        mLayoutManager.recordNextExtraLayoutSpace();
        waitForFirstLayout();
        waitForDraw(2);
        verify(getExpectedExtraSpace(false), getAvailableSpace(TOWARDS_START));
    }

    private void scrollToPositionAndVerify(int target, boolean smoothScroll) throws Throwable {
        int prevPosition = mCurrPosition;
        mCurrPosition = target;

        // Perform the scroll
        scrollToPosition(mCurrPosition, smoothScroll);

        // Update expected results
        // Alignment means the side of the viewport to which mCurrPosition is aligned
        // This is necessary to calculate the available space before and after the viewport
        ScrollDirection alignment = mCurrPosition > prevPosition ? TOWARDS_END : TOWARDS_START;
        if (smoothScroll) {
            mLastScrollDirection = isReversed() ? alignment.invert() : alignment;
        }

        // Verify actual results
        verify(getExpectedExtraSpace(smoothScroll), getAvailableSpace(alignment));
    }

    private void scrollToPosition(final int position, final boolean smoothScroll) throws Throwable {
        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // When mStackFromEnd, RV starts at the last item, so mirror positions in that case
                int resolvedTarget = mConfig.mStackFromEnd
                        ? mConfig.mItemCount - position - 1
                        : position;
                mLayoutManager.recordNextExtraLayoutSpace();
                if (smoothScroll) {
                    mRecyclerView.smoothScrollToPosition(resolvedTarget);
                } else {
                    mRecyclerView.scrollToPosition(resolvedTarget);
                }
            }
        });
        if (smoothScroll) {
            mLayoutManager.expectIdleState(1);
            mLayoutManager.waitForSnap(10);
        } else {
            mLayoutManager.waitForLayout(2);
            waitForDraw(2);
        }
    }

    private int[] getExpectedExtraSpace(boolean didScroll) {
        int[] space = new int[2];

        if (mExtraLayoutSpace != null) {
            // If calculateExtraLayoutSpace is overridden, expect those values
            space[0] = mExtraLayoutSpace[0];
            space[1] = mExtraLayoutSpace[1];
        } else {
            // Otherwise, expect mExtraLayoutSpaceLegacy or the default
            // of one page when scrolling in the scroll direction
            int defaultScrollSpace = didScroll ? RECYCLERVIEW_SIZE : 0;
            int potentialScrollSpace = mExtraLayoutSpaceLegacy == -1
                    ? defaultScrollSpace : mExtraLayoutSpaceLegacy;
            if (mLastScrollDirection == TOWARDS_START) {
                space[0] = potentialScrollSpace;
            } else {
                space[1] = potentialScrollSpace;
            }
        }

        return space;
    }

    private int[] getAvailableSpace(ScrollDirection alignment) {
        int[] availableSpace = new int[2];
        int fullListSize = mConfig.mItemCount * CHILD_SIZE;
        int positionFromStart = mCurrPosition;
        if (isReversed()) {
            // If RV starts at the end, mirror both the alignment and
            // position to make the available space calculation uniform
            alignment = alignment.invert();
            positionFromStart = mConfig.mItemCount - mCurrPosition - 1;
        }
        if (alignment == TOWARDS_START) {
            availableSpace[0] = positionFromStart * CHILD_SIZE;
        } else {
            availableSpace[0] = (positionFromStart + 1) * CHILD_SIZE - RECYCLERVIEW_SIZE;
        }
        availableSpace[1] = fullListSize - availableSpace[0] - RECYCLERVIEW_SIZE;
        return availableSpace;
    }

    private void verify(int[] expectedExtraSpace, int[] availableSpace) {
        // Verify that the recorded extra layout space matches what we expected
        assertThat(mLayoutManager.mRecordedExtraLayoutSpace, equalTo(expectedExtraSpace));
        // Verify that the extra layout space was actually used
        expectedExtraSpace[0] = Math.min(availableSpace[0], expectedExtraSpace[0]);
        expectedExtraSpace[1] = Math.min(availableSpace[1], expectedExtraSpace[1]);
        OrientationHelper orientationHelper = mLayoutManager.mOrientationHelper;
        int[] startBandwidth = new int[] {
                orientationHelper.getStartAfterPadding() - expectedExtraSpace[0] - CHILD_SIZE,
                orientationHelper.getStartAfterPadding() - expectedExtraSpace[0]
        };
        int[] endBandwidth = new int[] {
                orientationHelper.getEndAfterPadding() + expectedExtraSpace[1],
                orientationHelper.getEndAfterPadding() + expectedExtraSpace[1] + CHILD_SIZE
        };
        assertThat(mLayoutManager.mLayoutRecorder.mIsContiguous,
                equalTo(true));
        assertThat(mLayoutManager.mLayoutRecorder.mObservedStart,
                allOf(
                        greaterThan(startBandwidth[0]),
                        lessThanOrEqualTo(startBandwidth[1])
                ));
        assertThat(mLayoutManager.mLayoutRecorder.mObservedEnd,
                allOf(
                        greaterThanOrEqualTo(endBandwidth[0]),
                        lessThan(endBandwidth[1])
                ));
    }

    private ViewTreeObserver getViewTreeObserver() {
        return mRecyclerView.getViewTreeObserver();
    }

    private RecyclerView.LayoutParams getParentLayoutParams() {
        return new RecyclerView.LayoutParams(
                mConfig.mOrientation == HORIZONTAL ? RECYCLERVIEW_SIZE : CHILD_SIZE,
                mConfig.mOrientation == VERTICAL ? RECYCLERVIEW_SIZE : CHILD_SIZE
        );
    }

    private RecyclerView.LayoutParams getChildLayoutParams() {
        return new RecyclerView.LayoutParams(
                mConfig.mOrientation == HORIZONTAL ? CHILD_SIZE : MATCH_PARENT,
                mConfig.mOrientation == VERTICAL ? CHILD_SIZE : MATCH_PARENT
        );
    }

    class ExtraLayoutSpaceLayoutManager extends WrappedLinearLayoutManager {
        int mExtraLayoutSpaceLegacy = -1;
        int[] mExtraLayoutSpace = null;

        private boolean mRecordExtraLayoutSpace = false;
        int[] mRecordedExtraLayoutSpace = new int[2];
        LayoutBoundsRecorder mLayoutRecorder;

        ExtraLayoutSpaceLayoutManager(Context context, int orientation, boolean reverseLayout) {
            super(context, orientation, reverseLayout);
        }

        @SuppressWarnings("deprecation")
        @Override
        protected int getExtraLayoutSpace(RecyclerView.State state) {
            if (mExtraLayoutSpaceLegacy == -1) {
                return super.getExtraLayoutSpace(state);
            } else {
                return mExtraLayoutSpaceLegacy;
            }
        }

        @Override
        protected void calculateExtraLayoutSpace(@NonNull RecyclerView.State state,
                @NonNull int[] extraLayoutSpace) {
            if (mExtraLayoutSpace == null) {
                super.calculateExtraLayoutSpace(state, extraLayoutSpace);
            } else {
                extraLayoutSpace[0] = mExtraLayoutSpace[0];
                extraLayoutSpace[1] = mExtraLayoutSpace[1];
            }
            if (mRecordExtraLayoutSpace) {
                mRecordExtraLayoutSpace = false;
                mRecordedExtraLayoutSpace[0] = extraLayoutSpace[0];
                mRecordedExtraLayoutSpace[1] = extraLayoutSpace[1];
                getViewTreeObserver().addOnDrawListener(mLayoutRecorder);
            }
        }

        public void recordNextExtraLayoutSpace() {
            mRecordedExtraLayoutSpace[0] = -1;
            mRecordedExtraLayoutSpace[1] = -1;
            mRecordExtraLayoutSpace = true;
            mLayoutRecorder = new LayoutBoundsRecorder();
        }
    }

    class LayoutBoundsRecorder implements ViewTreeObserver.OnDrawListener {
        private final OrientationHelper mHelper;
        private final int[][] mBounds;

        private boolean mHasRecorded = false;
        public boolean mIsContiguous;
        public int mObservedStart;
        public int mObservedEnd;

        LayoutBoundsRecorder() {
            mHelper = mLayoutManager.mOrientationHelper;
            mBounds = new int[mTestAdapter.getItemCount()][2];
        }

        @Override
        public void onDraw() {
            if (!mHasRecorded) {
                recordBounds();
                mRecyclerView.post(new Runnable() {
                    @Override
                    public void run() {
                        getViewTreeObserver().removeOnDrawListener(LayoutBoundsRecorder.this);
                    }
                });
            }
        }

        private void recordBounds() {
            int childCount = mLayoutManager.getChildCount();
            for (int i = 0; i < childCount; i++) {
                View child = mLayoutManager.getChildAt(i);
                mBounds[i][0] = mHelper.getDecoratedStart(child);
                mBounds[i][1] = mHelper.getDecoratedEnd(child);
            }
            Arrays.sort(mBounds, 0, childCount, BOUNDS_COMPARATOR);
            mObservedStart = childCount == 0 ? 0 : mBounds[0][0];
            mObservedEnd = childCount == 0 ? 0 : mBounds[childCount - 1][1];
            mIsContiguous = true;
            for (int i = 1; mIsContiguous && i < childCount; i++) {
                mIsContiguous = mBounds[i - 1][1] == mBounds[i][0];
            }
            mHasRecorded = true;
        }
    }

    private static final Comparator<int[]> BOUNDS_COMPARATOR = new Comparator<int[]>() {
        @Override
        public int compare(int[] lhs, int[] rhs) {
            return lhs[0] - rhs[0];
        }
    };
}
