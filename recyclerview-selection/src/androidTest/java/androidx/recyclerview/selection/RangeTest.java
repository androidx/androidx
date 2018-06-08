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

import static androidx.recyclerview.selection.Range.TYPE_PRIMARY;
import static androidx.recyclerview.selection.Range.TYPE_PROVISIONAL;

import static org.junit.Assert.assertEquals;

import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import androidx.recyclerview.selection.testing.TestData;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;
import java.util.Stack;

/**
 * MouseInputDelegate / SelectHelper integration test covering the shared
 * responsibility of range selection.
 */
@RunWith(AndroidJUnit4.class)
@SmallTest
public final class RangeTest {

    private static final List<String> ITEMS = TestData.createStringData(100);

    private RangeSpy mSpy;
    private Stack<Capture> mOperations;
    private Range mRange;

    @Before
    public void setUp() {
        mOperations = new Stack<>();
        mSpy = new RangeSpy(mOperations);
    }

    @Test
    public void testEstablishRange() {
        mRange = new Range(0, mSpy);
        mRange.extendRange(5, TYPE_PRIMARY);

        // Origin is expected to have already been selected.
        mOperations.pop().assertChanged(1, 5, true);
    }

    @Test
    public void testExpandRange() {
        mRange = new Range(0, mSpy);
        mRange.extendRange(5, TYPE_PRIMARY);
        mRange.extendRange(10, TYPE_PRIMARY);

        mOperations.pop().assertChanged(6, 10, true);
    }

    @Test
    public void testContractRange() {
        mRange = new Range(0, mSpy);
        mRange.extendRange(10, TYPE_PRIMARY);
        mRange.extendRange(5, TYPE_PRIMARY);
        mOperations.pop().assertChanged(6, 10, false);
    }


    @Test
    public void testFlipRange_InitiallyDescending() {
        mRange = new Range(10, mSpy);
        mRange.extendRange(20, TYPE_PRIMARY);
        mRange.extendRange(5, TYPE_PRIMARY);

        // When a revision results in a flip two changes
        // are sent to the callback. 1 to unselect the old items
        // and one to select the new items.
        mOperations.pop().assertChanged(5, 9, true);
        // note that range never modifies the anchor.
        mOperations.pop().assertChanged(11, 20, false);
    }

    @Test
    public void testFlipRange_InitiallyAscending() {
        mRange = new Range(10, mSpy);
        mRange.extendRange(5, TYPE_PRIMARY);
        mRange.extendRange(20, TYPE_PRIMARY);

        // When a revision results in a flip two changes
        // are sent to the callback. 1 to unselect the old items
        // and one to select the new items.
        mOperations.pop().assertChanged(11, 20, true);
        // note that range never modifies the anchor.
        mOperations.pop().assertChanged(5, 9, false);
    }

    // NOTE: The operation type is conveyed among methods, then
    // returned to the caller. It's more of something we coury
    // for the caller. So we won't verify courying the value
    // with all behaviors. Just this once.
    @Test
    public void testCouriesRangeType() {
        mRange = new Range(0, mSpy);

        mRange.extendRange(5, TYPE_PRIMARY);
        mOperations.pop().assertType(TYPE_PRIMARY);

        mRange.extendRange(10, TYPE_PROVISIONAL);
        mOperations.pop().assertType(TYPE_PROVISIONAL);
    }

    private static class Capture {

        private int mBegin;
        private int mEnd;
        private boolean mSelected;
        private int mType;

        private Capture(int begin, int end, boolean selected, int type) {
            mBegin = begin;
            mEnd = end;
            mSelected = selected;
            mType = type;
        }

        private void assertType(int expected) {
            assertEquals(expected, mType);
        }

        private void assertChanged(int begin, int end, boolean selected) {
            assertEquals(begin, mBegin);
            assertEquals(end, mEnd);
            assertEquals(selected, mSelected);
        }
    }

    private static final class RangeSpy extends Range.Callbacks {

        private final Stack<Capture> mOperations;

        RangeSpy(Stack<Capture> operations) {
            mOperations = operations;
        }

        @Override
        void updateForRange(int begin, int end, boolean selected, int type) {
            mOperations.push(new Capture(begin, end, selected, type));
        }

        Capture popOp() {
            return mOperations.pop();
        }
    }
}
