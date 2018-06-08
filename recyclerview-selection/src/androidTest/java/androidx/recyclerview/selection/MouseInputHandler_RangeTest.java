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

import static androidx.recyclerview.selection.testing.TestEvents.Mouse.CLICK;
import static androidx.recyclerview.selection.testing.TestEvents.Mouse.SECONDARY_CLICK;
import static androidx.recyclerview.selection.testing.TestEvents.Mouse.SHIFT_CLICK;

import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import androidx.recyclerview.selection.testing.SelectionProbe;
import androidx.recyclerview.selection.testing.SelectionTrackers;
import androidx.recyclerview.selection.testing.TestAdapter;
import androidx.recyclerview.selection.testing.TestData;
import androidx.recyclerview.selection.testing.TestFocusDelegate;
import androidx.recyclerview.selection.testing.TestItemDetails;
import androidx.recyclerview.selection.testing.TestItemDetailsLookup;
import androidx.recyclerview.selection.testing.TestItemKeyProvider;
import androidx.recyclerview.selection.testing.TestOnContextClickListener;
import androidx.recyclerview.selection.testing.TestOnItemActivatedListener;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * MouseInputDelegate / SelectHelper integration test covering the shared
 * responsibility of range selection.
 */
@RunWith(AndroidJUnit4.class)
@SmallTest
public final class MouseInputHandler_RangeTest {

    private MouseInputHandler<String> mInputDelegate;
    private SelectionProbe mSelection;
    private TestFocusDelegate<String> mFocusCallbacks;
    private TestItemDetailsLookup mDetailsLookup;

    @Before
    public void setUp() {
        SelectionTracker<String> selectionMgr =
                SelectionTrackers.createStringTracker("mouse-input-test", 100);
        TestOnContextClickListener mouseCallbacks = new TestOnContextClickListener();
        TestOnItemActivatedListener<String> activationCallbacks =
                new TestOnItemActivatedListener<>();

        mDetailsLookup = new TestItemDetailsLookup();
        mSelection = new SelectionProbe(selectionMgr);
        mFocusCallbacks = new TestFocusDelegate<>();

        mInputDelegate = new MouseInputHandler<>(
                selectionMgr,
                new TestItemKeyProvider(
                        ItemKeyProvider.SCOPE_MAPPED,
                        new TestAdapter(TestData.createStringData(100))),
                mDetailsLookup,
                mouseCallbacks,
                activationCallbacks,
                mFocusCallbacks);
    }

    @Test
    public void testExtendRange() {
        // uni-click just focuses.
        mDetailsLookup.initAt(7).setInItemSelectRegion(true);
        mInputDelegate.onSingleTapConfirmed(CLICK);

        mDetailsLookup.initAt(11);
        mInputDelegate.onSingleTapUp(SHIFT_CLICK);

        mSelection.assertRangeSelection(7, 11);
    }

    @Test
    public void testExtendRangeContinues() {
        mDetailsLookup.initAt(7).setInItemSelectRegion(true);
        mInputDelegate.onSingleTapConfirmed(CLICK);

        mDetailsLookup.initAt(11);
        mInputDelegate.onSingleTapUp(SHIFT_CLICK);

        mDetailsLookup.initAt(21);
        mInputDelegate.onSingleTapUp(SHIFT_CLICK);

        mSelection.assertRangeSelection(7, 21);
    }

    @Test
    public void testMultipleContiguousRanges() {
        mDetailsLookup.initAt(7).setInItemSelectRegion(true);
        mInputDelegate.onSingleTapConfirmed(CLICK);

        mDetailsLookup.initAt(11);
        mInputDelegate.onSingleTapUp(SHIFT_CLICK);

        // click without shift sets a new range start point.
        TestItemDetails item = mDetailsLookup.initAt(20);
        mInputDelegate.onSingleTapUp(CLICK);
        mInputDelegate.onSingleTapConfirmed(CLICK);

        mFocusCallbacks.focusItem(item);

        mDetailsLookup.initAt(25);
        mInputDelegate.onSingleTapUp(SHIFT_CLICK);
        mInputDelegate.onSingleTapConfirmed(SHIFT_CLICK);

        mSelection.assertRangeNotSelected(7, 11);
        mSelection.assertRangeSelected(20, 25);
        mSelection.assertSelectionSize(6);
    }

    @Test
    public void testReducesSelectionRange() {
        mDetailsLookup.initAt(7).setInItemSelectRegion(true);
        mInputDelegate.onSingleTapConfirmed(CLICK);

        mDetailsLookup.initAt(17);
        mInputDelegate.onSingleTapUp(SHIFT_CLICK);

        mDetailsLookup.initAt(10);
        mInputDelegate.onSingleTapUp(SHIFT_CLICK);

        mSelection.assertRangeSelection(7, 10);
    }

    @Test
    public void testReducesSelectionRange_Reverse() {
        mDetailsLookup.initAt(17).setInItemSelectRegion(true);
        mInputDelegate.onSingleTapConfirmed(CLICK);

        mDetailsLookup.initAt(7);
        mInputDelegate.onSingleTapUp(SHIFT_CLICK);

        mDetailsLookup.initAt(14);
        mInputDelegate.onSingleTapUp(SHIFT_CLICK);

        mSelection.assertRangeSelection(14, 17);
    }

    @Test
    public void testExtendsRange_Reverse() {
        mDetailsLookup.initAt(12).setInItemSelectRegion(true);
        mInputDelegate.onSingleTapConfirmed(CLICK);

        mDetailsLookup.initAt(5);
        mInputDelegate.onSingleTapUp(SHIFT_CLICK);

        mSelection.assertRangeSelection(5, 12);
    }

    @Test
    public void testExtendsRange_ReversesAfterForwardClick() {
        mDetailsLookup.initAt(7).setInItemSelectRegion(true);
        mInputDelegate.onSingleTapConfirmed(CLICK);

        mDetailsLookup.initAt(11);
        mInputDelegate.onSingleTapUp(SHIFT_CLICK);

        mDetailsLookup.initAt(0);
        mInputDelegate.onSingleTapUp(SHIFT_CLICK);

        mSelection.assertRangeSelection(0, 7);
    }

    @Test
    public void testRightClickEstablishesRange() {

        mDetailsLookup.initAt(7).setInItemSelectRegion(true);
        mInputDelegate.onDown(SECONDARY_CLICK);
        // This next method call simulates the behavior of the system event dispatch code.
        // UserInputHandler depends on a specific sequence of events for internal
        // state to remain valid. It's not an awesome arrangement, but it is currently
        // necessary.
        //
        // See: UserInputHandler.MouseDelegate#mHandledOnDown;
        mInputDelegate.onSingleTapUp(SECONDARY_CLICK);

        mDetailsLookup.initAt(11);
        // Now we can send a subsequent event that should extend selection.
        mInputDelegate.onDown(SHIFT_CLICK);
        mInputDelegate.onSingleTapUp(SHIFT_CLICK);

        mSelection.assertRangeSelection(7, 11);
    }
}
