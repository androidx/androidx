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

import static androidx.recyclerview.selection.testing.TestEvents.Mouse.ALT_CLICK;
import static androidx.recyclerview.selection.testing.TestEvents.Mouse.CLICK;
import static androidx.recyclerview.selection.testing.TestEvents.Mouse.CTRL_CLICK;
import static androidx.recyclerview.selection.testing.TestEvents.Mouse.SECONDARY_CLICK;
import static androidx.recyclerview.selection.testing.TestEvents.Mouse.SHIFT_CLICK;
import static androidx.recyclerview.selection.testing.TestEvents.Mouse.TERTIARY_CLICK;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.view.MotionEvent;

import androidx.recyclerview.selection.ItemDetailsLookup.ItemDetails;
import androidx.recyclerview.selection.testing.SelectionProbe;
import androidx.recyclerview.selection.testing.SelectionTrackers;
import androidx.recyclerview.selection.testing.TestAdapter;
import androidx.recyclerview.selection.testing.TestData;
import androidx.recyclerview.selection.testing.TestEvents;
import androidx.recyclerview.selection.testing.TestFocusDelegate;
import androidx.recyclerview.selection.testing.TestItemDetails;
import androidx.recyclerview.selection.testing.TestItemDetailsLookup;
import androidx.recyclerview.selection.testing.TestItemKeyProvider;
import androidx.recyclerview.selection.testing.TestOnContextClickListener;
import androidx.recyclerview.selection.testing.TestOnItemActivatedListener;
import androidx.recyclerview.widget.RecyclerView;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
@SmallTest
public final class MouseInputHandlerTest {

    private MouseInputHandler<String> mInputDelegate;

    private TestOnContextClickListener mMouseCallbacks;
    private TestOnItemActivatedListener<String> mActivationCallbacks;
    private TestFocusDelegate<String> mFocusCallbacks;

    private TestItemDetailsLookup mDetailsLookup;
    private SelectionProbe mSelection;
    private SelectionTracker<String> mSelectionMgr;

    private TestEvents.Builder mEvent;

    @Before
    public void setUp() {

        mSelectionMgr = SelectionTrackers.createStringTracker("mouse-input-test", 100);
        mDetailsLookup = new TestItemDetailsLookup();
        mSelection = new SelectionProbe(mSelectionMgr);

        mMouseCallbacks = new TestOnContextClickListener();
        mActivationCallbacks = new TestOnItemActivatedListener<>();
        mFocusCallbacks = new TestFocusDelegate<>();

        mInputDelegate = new MouseInputHandler<>(
                mSelectionMgr,
                new TestItemKeyProvider<>(
                        ItemKeyProvider.SCOPE_MAPPED,
                        new TestAdapter<>(TestData.createStringData(100))),
                mDetailsLookup,
                mMouseCallbacks,
                mActivationCallbacks,
                mFocusCallbacks);

        mEvent = TestEvents.builder().mouse();
        mDetailsLookup.initAt(RecyclerView.NO_POSITION);
    }

    private boolean callTapHandlers(MotionEvent e, boolean isDoubleTap) {
        MotionEvent downEvent = e;
        MotionEvent secondDownEvent = e;
        // Strictly speaking, it would be more realistic if the upEvent's getAction() was
        // MotionEvent.ACTION_UP, in contrast to e.getAction(), which is MotionEvent.ACTION_DOWN.
        // But the code under test doesn't care about the action. It's simpler to just re-use e.
        MotionEvent upEvent = e;

        boolean handled = mInputDelegate.onDown(downEvent)
                || mInputDelegate.onSingleTapUp(upEvent);

        if (handled) {
            // No-op.
        } else if (isDoubleTap) {
            // As can be seen in the GestureDetector.onTouchEvent code, for the ACTION_DOWN case,
            // onDoubleTap will be called first (with the first down event) and then onDown will be
            // called (with the second down event), regardless of whether onDoubleTap returned true
            // or false. When the GestureDetector recognizes this as a double tap, it also won't
            // call onSingleTapEtc methods.
            handled |= mInputDelegate.onDoubleTap(downEvent);
            handled |= mInputDelegate.onDown(secondDownEvent);
        } else {
            handled |= mInputDelegate.onSingleTapConfirmed(downEvent);
        }

        return handled;
    }

    private boolean singleTap(MotionEvent e) {
        return callTapHandlers(e, false);
    }

    private boolean doubleTap(MotionEvent e) {
        return callTapHandlers(e, true);
    }

    @Test
    public void testConfirmedClick_StartsSelection() {
        mDetailsLookup.initAt(11).setInItemSelectRegion(true);
        mInputDelegate.onSingleTapConfirmed(CLICK);

        mSelection.assertSelection(11);
    }

    @Test
    public void testClickOnSelectionHotspot_Outside() {
        // When nothing is selected, clicking outside the hotspot does nothing.

        mSelection.assertNoSelection();

        mDetailsLookup.initAt(5).setClassifySelectionHotspot(
                ItemDetails.SELECTION_HOTSPOT_OUTSIDE);
        assertTrue(singleTap(CLICK));
        mSelection.assertNoSelection();

        mDetailsLookup.initAt(5).setClassifySelectionHotspot(
                ItemDetails.SELECTION_HOTSPOT_OUTSIDE);
        assertTrue(singleTap(CLICK));
        mSelection.assertNoSelection();

        // When something is selected, clicking outside the hotspot does something conditional. If
        // clicking on a selected item, it deselects it. Otherwise, it clears the entire selection.

        mSelectionMgr.clearSelection();
        mSelectionMgr.select("8");
        mSelectionMgr.select("9");
        mSelectionMgr.select("10");
        mSelectionMgr.select("11");
        mSelection.assertSelection(8, 9, 10, 11);

        mDetailsLookup.initAt(9).setClassifySelectionHotspot(
                ItemDetails.SELECTION_HOTSPOT_OUTSIDE);
        assertTrue(singleTap(CLICK));
        mSelection.assertSelection(8, 10, 11);

        mDetailsLookup.initAt(9).setClassifySelectionHotspot(
                ItemDetails.SELECTION_HOTSPOT_OUTSIDE);
        assertTrue(singleTap(CLICK));
        mSelection.assertNoSelection();

        mDetailsLookup.initAt(20).setClassifySelectionHotspot(
                ItemDetails.SELECTION_HOTSPOT_OUTSIDE);
        assertTrue(singleTap(CLICK));
        mSelection.assertNoSelection();
    }

    @Test
    public void testClickOnSelectionHotspot_InsideToggleMulti() {
        // Clicking inside the hotspot toggles selection, with multiple items selectable.

        mSelection.assertNoSelection();

        mDetailsLookup.initAt(5).setClassifySelectionHotspot(
                ItemDetails.SELECTION_HOTSPOT_INSIDE_TOGGLE_MULTI);
        assertTrue(singleTap(CLICK));
        mSelection.assertSelection(5);

        mDetailsLookup.initAt(5).setClassifySelectionHotspot(
                ItemDetails.SELECTION_HOTSPOT_INSIDE_TOGGLE_MULTI);
        assertTrue(singleTap(CLICK));
        mSelection.assertNoSelection();

        mSelectionMgr.clearSelection();
        mSelectionMgr.select("8");
        mSelectionMgr.select("9");
        mSelectionMgr.select("10");
        mSelectionMgr.select("11");
        mSelection.assertSelection(8, 9, 10, 11);

        mDetailsLookup.initAt(9).setClassifySelectionHotspot(
                ItemDetails.SELECTION_HOTSPOT_INSIDE_TOGGLE_MULTI);
        assertTrue(singleTap(CLICK));
        mSelection.assertSelection(8, 10, 11);

        mDetailsLookup.initAt(9).setClassifySelectionHotspot(
                ItemDetails.SELECTION_HOTSPOT_INSIDE_TOGGLE_MULTI);
        assertTrue(singleTap(CLICK));
        mSelection.assertSelection(8, 9, 10, 11);

        mDetailsLookup.initAt(20).setClassifySelectionHotspot(
                ItemDetails.SELECTION_HOTSPOT_INSIDE_TOGGLE_MULTI);
        assertTrue(singleTap(CLICK));
        mSelection.assertSelection(8, 9, 10, 11, 20);
    }

    @Test
    public void testClickOnSelectionHotspot_InsideToggleSolo() {
        // Clicking inside the hotspot toggles selection, with just one item selectable.

        mSelection.assertNoSelection();

        mDetailsLookup.initAt(5).setClassifySelectionHotspot(
                ItemDetails.SELECTION_HOTSPOT_INSIDE_TOGGLE_SOLO);
        assertTrue(singleTap(CLICK));
        mSelection.assertSelection(5);

        mDetailsLookup.initAt(5).setClassifySelectionHotspot(
                ItemDetails.SELECTION_HOTSPOT_INSIDE_TOGGLE_SOLO);
        assertTrue(singleTap(CLICK));
        mSelection.assertNoSelection();

        mSelectionMgr.clearSelection();
        mSelectionMgr.select("8");
        mSelectionMgr.select("9");
        mSelectionMgr.select("10");
        mSelectionMgr.select("11");
        mSelection.assertSelection(8, 9, 10, 11);

        mDetailsLookup.initAt(9).setClassifySelectionHotspot(
                ItemDetails.SELECTION_HOTSPOT_INSIDE_TOGGLE_SOLO);
        assertTrue(singleTap(CLICK));
        mSelection.assertSelection(9);

        mDetailsLookup.initAt(9).setClassifySelectionHotspot(
                ItemDetails.SELECTION_HOTSPOT_INSIDE_TOGGLE_SOLO);
        assertTrue(singleTap(CLICK));
        mSelection.assertNoSelection();

        mDetailsLookup.initAt(20).setClassifySelectionHotspot(
                ItemDetails.SELECTION_HOTSPOT_INSIDE_TOGGLE_SOLO);
        assertTrue(singleTap(CLICK));
        mSelection.assertSelection(20);
    }

    @Test
    public void testClickOnSelectionHotspot_InsideClearAndThenSet() {
        // Clicking inside the hotspot clears and then sets selection.

        mSelection.assertNoSelection();

        mDetailsLookup.initAt(5).setClassifySelectionHotspot(
                ItemDetails.SELECTION_HOTSPOT_INSIDE_CLEAR_AND_THEN_SET);
        assertTrue(singleTap(CLICK));
        mSelection.assertSelection(5);

        mDetailsLookup.initAt(5).setClassifySelectionHotspot(
                ItemDetails.SELECTION_HOTSPOT_INSIDE_CLEAR_AND_THEN_SET);
        assertTrue(singleTap(CLICK));
        mSelection.assertSelection(5);

        mSelectionMgr.clearSelection();
        mSelectionMgr.select("8");
        mSelectionMgr.select("9");
        mSelectionMgr.select("10");
        mSelectionMgr.select("11");
        mSelection.assertSelection(8, 9, 10, 11);

        mDetailsLookup.initAt(9).setClassifySelectionHotspot(
                ItemDetails.SELECTION_HOTSPOT_INSIDE_CLEAR_AND_THEN_SET);
        assertTrue(singleTap(CLICK));
        mSelection.assertSelection(9);

        mDetailsLookup.initAt(9).setClassifySelectionHotspot(
                ItemDetails.SELECTION_HOTSPOT_INSIDE_CLEAR_AND_THEN_SET);
        assertTrue(singleTap(CLICK));
        mSelection.assertSelection(9);

        mDetailsLookup.initAt(20).setClassifySelectionHotspot(
                ItemDetails.SELECTION_HOTSPOT_INSIDE_CLEAR_AND_THEN_SET);
        assertTrue(singleTap(CLICK));
        mSelection.assertSelection(20);
    }

    @Test
    public void testClickOnSelectRegion_AddsToSelection() {
        mDetailsLookup.initAt(11).setInItemSelectRegion(true);
        mInputDelegate.onSingleTapConfirmed(CLICK);

        mDetailsLookup.initAt(10).setInItemSelectRegion(true);
        mInputDelegate.onSingleTapConfirmed(CLICK);

        mSelection.assertSelected(10, 11);
    }

    @Test
    public void testClickOnIconOfSelectedItem_RemovesFromSelection() {
        mDetailsLookup.initAt(8).setInItemSelectRegion(true);
        mInputDelegate.onSingleTapConfirmed(CLICK);

        mDetailsLookup.initAt(11);
        mInputDelegate.onSingleTapConfirmed(SHIFT_CLICK);
        mSelection.assertSelected(8, 9, 10, 11);

        mDetailsLookup.initAt(9);
        mInputDelegate.onSingleTapConfirmed(CLICK);
        mSelection.assertSelected(8, 10, 11);
    }

    @Test
    public void testRightClickDown_StartsContextMenu() {
        mInputDelegate.onDown(SECONDARY_CLICK);

        mMouseCallbacks.assertLastEvent(SECONDARY_CLICK);
    }

    @Test
    public void testAltClickDown_StartsContextMenu() {
        mInputDelegate.onDown(ALT_CLICK);

        mMouseCallbacks.assertLastEvent(ALT_CLICK);
    }

    @Test
    public void testScroll_shouldTrap() {
        mDetailsLookup.initAt(0);
        assertTrue(mInputDelegate.onScroll(
                null,
                mEvent.action(MotionEvent.ACTION_MOVE).primary().build(),
                -1,
                -1));
    }

    @Test
    public void testScroll_NoTrapForTwoFinger() {
        mDetailsLookup.initAt(0);
        assertFalse(mInputDelegate.onScroll(
                null,
                mEvent.action(MotionEvent.ACTION_MOVE).build(),
                -1,
                -1));
    }

    @Test
    public void testUnconfirmedCtrlClick_AddsToExistingSelection() {
        mDetailsLookup.initAt(7).setInItemSelectRegion(true);
        mInputDelegate.onSingleTapConfirmed(CLICK);

        mDetailsLookup.initAt(11);
        mInputDelegate.onSingleTapConfirmed(CTRL_CLICK);

        mSelection.assertSelection(7, 11);
    }

    @Test
    public void testUnconfirmedShiftClick_ExtendsSelection() {
        mDetailsLookup.initAt(7).setInItemSelectRegion(true);
        mInputDelegate.onSingleTapConfirmed(CLICK);

        mDetailsLookup.initAt(11);
        mInputDelegate.onSingleTapConfirmed(SHIFT_CLICK);

        mSelection.assertSelection(7, 8, 9, 10, 11);
    }

    @Test
    public void testConfirmedShiftClick_ExtendsSelectionFromFocus() {
        TestItemDetails item = mDetailsLookup.initAt(7);
        mFocusCallbacks.focusItem(item);

        // There should be no selected item at this point, just focus on "7".
        mDetailsLookup.initAt(11);
        mInputDelegate.onSingleTapConfirmed(SHIFT_CLICK);
        mSelection.assertSelection(7, 8, 9, 10, 11);
    }

    @Test
    public void testUnconfirmedShiftClick_RotatesAroundOrigin() {
        mDetailsLookup.initAt(7).setInItemSelectRegion(true);
        mInputDelegate.onSingleTapConfirmed(CLICK);

        mDetailsLookup.initAt(11);
        mInputDelegate.onSingleTapConfirmed(SHIFT_CLICK);
        mSelection.assertSelection(7, 8, 9, 10, 11);

        mDetailsLookup.initAt(5);
        mInputDelegate.onSingleTapConfirmed(SHIFT_CLICK);

        mSelection.assertSelection(5, 6, 7);
        mSelection.assertNotSelected(8, 9, 10, 11);
    }

    @Test
    public void testUnconfirmedShiftCtrlClick_Combination() {
        mDetailsLookup.initAt(7).setInItemSelectRegion(true);
        mInputDelegate.onSingleTapConfirmed(CLICK);

        mDetailsLookup.initAt(11);
        mInputDelegate.onSingleTapConfirmed(SHIFT_CLICK);
        mSelection.assertSelection(7, 8, 9, 10, 11);

        mDetailsLookup.initAt(5);
        mInputDelegate.onSingleTapConfirmed(CTRL_CLICK);

        mSelection.assertSelection(5, 7, 8, 9, 10, 11);
    }

    @Test
    public void testUnconfirmedShiftCtrlClick_ShiftTakesPriority() {
        mDetailsLookup.initAt(7).setInItemSelectRegion(true);
        mInputDelegate.onSingleTapConfirmed(CLICK);

        mDetailsLookup.initAt(11);
        mInputDelegate.onSingleTapConfirmed(mEvent.primary().ctrl().shift().build());

        mSelection.assertSelection(7, 8, 9, 10, 11);
    }

    // TODO: Add testSpaceBar_Previews, but we need to set a system property
    // to have a deterministic state.

    @Test
    public void testDoubleClick_Opens() {
        // Double-click on an unselected file should open it.
        TestItemDetails doc1 = mDetailsLookup.initAt(1);
        assertTrue(doubleTap(CLICK));
        mActivationCallbacks.assertActivated(doc1);

        // Double-click on a selected file should also open it.
        mSelectionMgr.select("2");
        TestItemDetails doc2 = mDetailsLookup.initAt(2);
        assertTrue(doubleTap(CLICK));
        mActivationCallbacks.assertActivated(doc2);
    }

    @Test
    public void testMiddleClick_DoesNothing() {
        mDetailsLookup.initAt(11).setInItemSelectRegion(true);
        mInputDelegate.onSingleTapConfirmed(TERTIARY_CLICK);

        mSelection.assertNoSelection();
    }

    @Test
    public void testClickOff_ClearsSelection() {
        mDetailsLookup.initAt(11).setInItemSelectRegion(true);
        mInputDelegate.onSingleTapConfirmed(CLICK);

        mDetailsLookup.initAt(RecyclerView.NO_POSITION);
        mInputDelegate.onSingleTapConfirmed(CLICK);

        mSelection.assertNoSelection();
    }

    @Test
    public void testClick_Focuses() {
        mDetailsLookup.initAt(11).setInItemSelectRegion(false);
        mInputDelegate.onSingleTapConfirmed(CLICK);

        mFocusCallbacks.assertHasFocus(true);
        mFocusCallbacks.assertFocused("11");
    }

    @Test
    public void testClickOff_ClearsFocus() {
        mDetailsLookup.initAt(11).setInItemSelectRegion(false);
        mInputDelegate.onSingleTapConfirmed(CLICK);
        mFocusCallbacks.assertHasFocus(true);

        mDetailsLookup.initAt(RecyclerView.NO_POSITION);
        mInputDelegate.onSingleTapConfirmed(CLICK);
        mFocusCallbacks.assertHasFocus(false);
    }

    @Test
    public void testClickOffSelection_RemovesSelectionAndFocuses() {
        mDetailsLookup.initAt(1).setInItemSelectRegion(true);
        mInputDelegate.onSingleTapConfirmed(CLICK);

        mDetailsLookup.initAt(5);
        mInputDelegate.onSingleTapConfirmed(SHIFT_CLICK);

        mSelection.assertSelection(1, 2, 3, 4, 5);

        mDetailsLookup.initAt(11);
        mInputDelegate.onSingleTapConfirmed(CLICK);

        mFocusCallbacks.assertFocused("11");
        mSelection.assertNoSelection();
    }
}
