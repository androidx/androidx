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

import static androidx.recyclerview.selection.testing.TestEvents.Touch.DOWN;
import static androidx.recyclerview.selection.testing.TestEvents.Touch.MOVE;
import static androidx.recyclerview.selection.testing.TestEvents.Touch.UP;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import androidx.recyclerview.selection.ItemDetailsLookup.ItemDetails;
import androidx.recyclerview.selection.testing.SelectionProbe;
import androidx.recyclerview.selection.testing.SelectionTrackers;
import androidx.recyclerview.selection.testing.TestAdapter;
import androidx.recyclerview.selection.testing.TestData;
import androidx.recyclerview.selection.testing.TestDragListener;
import androidx.recyclerview.selection.testing.TestFocusDelegate;
import androidx.recyclerview.selection.testing.TestItemDetailsLookup;
import androidx.recyclerview.selection.testing.TestItemKeyProvider;
import androidx.recyclerview.selection.testing.TestOnItemActivatedListener;
import androidx.recyclerview.selection.testing.TestRunnable;
import androidx.recyclerview.selection.testing.TestSelectionPredicate;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
@SmallTest
public final class TouchInputHandlerTest {

    private TouchInputHandler<String> mInputDelegate;
    private SelectionTracker<String> mSelectionMgr;
    private TestSelectionPredicate<String> mSelectionPredicate;
    private TestRunnable mGestureStarted;
    private TestRunnable mHapticPerformer;
    private TestOnItemActivatedListener<String> mActivationCallbacks;
    private TestItemDetailsLookup mDetailsLookup;
    private SelectionProbe mSelection;
    private TestDragListener mDragInitiatedListener;
    private TestRunnable mLongPressCallback;

    @Before
    public void setUp() {
        mSelectionMgr = SelectionTrackers.createStringTracker("input-handler-test", 100);
        mDetailsLookup = new TestItemDetailsLookup();
        mSelectionPredicate = new TestSelectionPredicate<>();
        mSelection = new SelectionProbe(mSelectionMgr);
        mGestureStarted = new TestRunnable();
        mHapticPerformer = new TestRunnable();
        mActivationCallbacks = new TestOnItemActivatedListener<>();
        mDragInitiatedListener = new TestDragListener();
        mLongPressCallback = new TestRunnable();

        mInputDelegate = new TouchInputHandler<>(
                mSelectionMgr,
                new TestItemKeyProvider<>(
                        ItemKeyProvider.SCOPE_MAPPED,
                        new TestAdapter<>(TestData.createStringData(100))),
                mDetailsLookup,
                mSelectionPredicate,
                mGestureStarted,
                mDragInitiatedListener,
                mActivationCallbacks,
                new TestFocusDelegate<>(),
                mHapticPerformer,
                mLongPressCallback::run);
    }

    @Test
    public void testTap_ActivatesWhenNoExistingSelection() {
        ItemDetails<String> doc = mDetailsLookup.initAt(11);

        assertTrue(mInputDelegate.onSingleTapUp(UP));
        mActivationCallbacks.assertActivated(doc);

        mActivationCallbacks.setReturnValue(false);
        assertFalse(mInputDelegate.onSingleTapUp(UP));
    }

    @Test
    public void testScroll_shouldNotBeTrapped() {
        assertFalse(mInputDelegate.onScroll(null, MOVE, -1, -1));
    }

    @Test
    public void testLongPress_UnselectedItem_Selects() {
        mSelectionPredicate.setReturnValue(true);
        mDetailsLookup.initAt(7);

        mInputDelegate.onLongPress(DOWN);

        mSelection.assertSelection(7);
    }


    @Test
    public void testLongPress_NotifiesLongPressCallback() {
        mSelectionPredicate.setReturnValue(true);
        mDetailsLookup.initAt(7);

        mInputDelegate.onLongPress(DOWN);
        mLongPressCallback.assertRun();
    }

    @Test
    public void testLongPress_UnselectedItem_PerformsHapticFeedback() {
        mSelectionPredicate.setReturnValue(true);
        mDetailsLookup.initAt(7);

        mInputDelegate.onLongPress(DOWN);

        mHapticPerformer.assertRun();
    }

    @Test
    public void testLongPress_UnselectedItem_StartsGestureSelection() {
        mSelectionPredicate.setReturnValue(true);
        mDetailsLookup.initAt(7);

        mInputDelegate.onLongPress(DOWN);

        mGestureStarted.assertRun();
    }

    @Test
    public void testLongPress_UnselectedItem_DoesNotInitiateDrag() {
        mSelectionPredicate.setReturnValue(true);
        mDetailsLookup.initAt(7);

        mInputDelegate.onLongPress(DOWN);

        mDragInitiatedListener.assertDragInitiated(false);
    }

    @Test
    public void testLongPress_SelectedItem_InitiatesDrag() {
        mSelectionPredicate.setReturnValue(true);
        mSelectionMgr.select("7");
        mDetailsLookup.initAt(7);

        mDragInitiatedListener.setReturnValue(true);
        mInputDelegate.onLongPress(DOWN);

        mDragInitiatedListener.assertDragInitiated(true);
        mHapticPerformer.assertRun();
    }

    @Test
    public void testLongPress_SelectedItem_NoFeedbackIfHandlerPasses() {
        mSelectionMgr.select("7");
        mDetailsLookup.initAt(7);
        mDragInitiatedListener.setReturnValue(false);
        mInputDelegate.onLongPress(DOWN);
        mHapticPerformer.assertNotRun();
    }

    @Test
    public void testLongPress_SelectedItem_PerformsHapticFeedback() {
        mSelectionPredicate.setReturnValue(true);
        mSelectionMgr.select("7");
        mDetailsLookup.initAt(7);

        mInputDelegate.onLongPress(DOWN);

        mHapticPerformer.assertRun();
    }

    @Test
    public void testUpAfterLongPress_Consumed() {
        mSelectionPredicate.setReturnValue(true);
        mDetailsLookup.initAt(7);

        mInputDelegate.onLongPress(DOWN);

        mDetailsLookup.reset();  // Any lookup should yield unselectable item.
        // After a long press we basically don't want to take
        // any additional action, but we want to consume
        // the event so other code doesn't think the event
        // was unhandled.
        assertTrue(mInputDelegate.onSingleTapUp(UP));
    }

    @Test
    public void testTapSelectHotspot_InitiatesSelection() {
        mSelectionPredicate.setReturnValue(true);

        mDetailsLookup.initAt(7).setInItemSelectRegion(true);
        assertTrue(mInputDelegate.onSingleTapUp(UP));

        mSelection.assertSelection(7);
    }

    @Test
    public void testTapSelectionHotspot_SelectedItem_Unselected() {
        mSelectionMgr.select("11");

        mDetailsLookup.initAt(11).setInItemSelectRegion(true);
        assertTrue(mInputDelegate.onSingleTapUp(UP));

        mSelection.assertNoSelection();
    }

    @Test
    public void testLongPress_AddsToSelection() {
        mSelectionPredicate.setReturnValue(true);

        mDetailsLookup.initAt(7);
        mInputDelegate.onLongPress(DOWN);

        mDetailsLookup.initAt(99);
        mInputDelegate.onLongPress(DOWN);

        mDetailsLookup.initAt(13);
        mInputDelegate.onLongPress(DOWN);

        mSelection.assertSelection(7, 13, 99);
    }

    @Test
    public void testTap_UnselectsSelectedItem() {
        mSelectionMgr.select("11");

        mDetailsLookup.initAt(11);
        assertTrue(mInputDelegate.onSingleTapUp(UP));

        mSelection.assertNoSelection();
    }

    @Test
    public void testTap_AdaptsDoubleTapUpToSingleTap() {
        mSelectionMgr.select("11");

        mDetailsLookup.initAt(12);
        assertTrue(mInputDelegate.onDoubleTapEvent(UP));

        mSelection.assertSelection(11, 12);
    }

    @Test
    public void testTapOff_ClearsSelection() {
        mSelectionMgr.select("11");

        assertTrue(mInputDelegate.onSingleTapUp(UP));

        mSelection.assertNoSelection();
    }
}
