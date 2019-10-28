/*
 * Copyright 2017 The Android Open Source Project
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

package androidx.recyclerview.selection;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import android.view.MotionEvent;

import androidx.recyclerview.selection.testing.SelectionProbe;
import androidx.recyclerview.selection.testing.SelectionTrackers;
import androidx.recyclerview.selection.testing.TestAutoScroller;
import androidx.recyclerview.selection.testing.TestEvents;
import androidx.recyclerview.selection.testing.TestSelectionPredicate;
import androidx.recyclerview.widget.RecyclerView;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class GestureSelectionHelperTest {

    private static final MotionEvent DOWN = TestEvents.builder()
            .action(MotionEvent.ACTION_DOWN)
            .location(1, 1)
            .build();

    private static final MotionEvent MOVE = TestEvents.builder()
            .action(MotionEvent.ACTION_MOVE)
            .location(1, 1)
            .build();

    private static final MotionEvent UP = TestEvents.builder()
            .action(MotionEvent.ACTION_UP)
            .location(1, 1)
            .build();

    private GestureSelectionHelper mHelper;
    private SelectionTracker<String> mSelectionTracker;
    private TestSelectionPredicate<String> mSelectionPredicate;
    private SelectionProbe mSelection;
    private OperationMonitor mMonitor;
    private TestViewDelegate mView;

    @Before
    public void setUp() {
        mSelectionTracker = SelectionTrackers.createStringTracker("gesture-selection-test", 100);
        mSelectionPredicate = new TestSelectionPredicate<>();
        mSelection = new SelectionProbe(mSelectionTracker);
        mMonitor = new OperationMonitor();
        mView = new TestViewDelegate();
        mHelper = new GestureSelectionHelper(
                mSelectionTracker, mSelectionPredicate, mView, new TestAutoScroller(), mMonitor);

        mSelectionPredicate.setReturnValue(true);
    }

    @Test
    public void testIgnoresDown_NoPosition() {
        mView.mNextPosition = RecyclerView.NO_POSITION;
        assertFalse(mHelper.onInterceptTouchEvent(null, DOWN));
    }

    @Test
    public void testIgnoresDown_NoItemDetails() {
        assertFalse(mHelper.onInterceptTouchEvent(null, DOWN));
    }

    @Test
    public void testThrowsWhenStartingWithoutRange() {
        mView.mNextPosition = 0;

        // Normally, this is controller by the TouchSelectionHelper via a a long press gesture.
        mSelectionTracker.select("1");
        // If mSelectionTracker.anchorRange(1) didn't initialize the range, then start
        // should throw an exception.
        try {
            mHelper.start();
            fail("Should have thrown IllegalStateException.");
        } catch (IllegalStateException expected) { }
    }

    @Test
    public void testIgnoresEventsWhenNotStarted() {
        assertFalse(mHelper.onInterceptTouchEvent(null, MOVE));
    }

    @Test
    public void testDoesNotClaimDownOnItem() {
        mView.mNextPosition = 0;
        assertFalse(mHelper.onInterceptTouchEvent(null, DOWN));
    }

    @Test
    public void testClaimsMoveIfStarted() {
        mView.mNextPosition = 0;

        // Normally, this is controller by the TouchSelectionHelper via a a long press gesture.
        mSelectionTracker.select("1");
        mSelectionTracker.anchorRange(1);
        mHelper.start();

        assertTrue(mHelper.onInterceptTouchEvent(null, MOVE));
    }

    @Test
    public void testCreatesRangeSelection() {
        mView.mNextPosition = 1;

        mSelectionTracker.select("1");
        mSelectionTracker.anchorRange(1);

        mHelper.start();

        mHelper.onTouchEvent(null, MOVE);

        mView.mNextPosition = 9;
        mHelper.onTouchEvent(null, MOVE);
        mHelper.onTouchEvent(null, UP);

        mSelection.assertRangeSelected(1, 9);
    }

    // Verify b/78615740 is fixed.
    @Test
    public void testEndsSelectionOnInterceptUp() {
        mView.mNextPosition = 1;

        mSelectionTracker.select("1");
        mSelectionTracker.anchorRange(1);

        mHelper.start();
        mHelper.onInterceptTouchEvent(null, UP);

        // If the helper didn't stop after onInterceptTouchEvent UP
        // this would fail.
        mHelper.start();
    }

    private static final class TestViewDelegate extends GestureSelectionHelper.ViewDelegate {

        private int mNextPosition = RecyclerView.NO_POSITION;

        @Override
        int getHeight() {
            return 1000;
        }

        @Override
        int getItemUnder(MotionEvent e) {
            return mNextPosition;
        }

        @Override
        int getLastGlidedItemPosition(MotionEvent e) {
            return mNextPosition;
        }
    }
}
