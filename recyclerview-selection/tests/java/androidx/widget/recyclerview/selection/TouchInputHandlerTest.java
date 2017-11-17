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

package androidx.widget.recyclerview.selection;

import static org.junit.Assert.assertFalse;

import static androidx.widget.recyclerview.selection.testing.TestEvents.Touch.TAP;

import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;
import android.support.v7.widget.RecyclerView;
import android.view.MotionEvent;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

import androidx.widget.recyclerview.selection.ItemDetailsLookup.ItemDetails;
import androidx.widget.recyclerview.selection.testing.SelectionProbe;
import androidx.widget.recyclerview.selection.testing.SelectionTrackers;
import androidx.widget.recyclerview.selection.testing.TestAdapter;
import androidx.widget.recyclerview.selection.testing.TestData;
import androidx.widget.recyclerview.selection.testing.TestFocusDelegate;
import androidx.widget.recyclerview.selection.testing.TestItemDetailsLookup;
import androidx.widget.recyclerview.selection.testing.TestItemKeyProvider;
import androidx.widget.recyclerview.selection.testing.TestOnItemActivatedListener;
import androidx.widget.recyclerview.selection.testing.TestRunnable;
import androidx.widget.recyclerview.selection.testing.TestSelectionPredicate;

@RunWith(AndroidJUnit4.class)
@SmallTest
public final class TouchInputHandlerTest {

    private static final List<String> ITEMS = TestData.createStringData(100);

    private TouchInputHandler mInputDelegate;
    private SelectionTracker mSelectionMgr;
    private TestSelectionPredicate mSelectionPredicate;
    private TestRunnable mGestureStarted;
    private TestRunnable mHapticPerformer;
    private TestDragListener mOnItemDragListener;
    private TestOnItemActivatedListener mActivationCallbacks;
    private TestFocusDelegate mFocusCallbacks;
    private TestItemDetailsLookup mDetailsLookup;
    private SelectionProbe mSelection;

    @Before
    public void setUp() {
        mSelectionMgr = SelectionTrackers.createStringTracker("input-handler-test", 100);
        mDetailsLookup = new TestItemDetailsLookup();
        mSelectionPredicate = new TestSelectionPredicate();
        mSelection = new SelectionProbe(mSelectionMgr);
        mGestureStarted = new TestRunnable();
        mHapticPerformer = new TestRunnable();
        mOnItemDragListener = new TestDragListener();
        mActivationCallbacks = new TestOnItemActivatedListener();
        mFocusCallbacks = new TestFocusDelegate();

        mInputDelegate = new TouchInputHandler(
                mSelectionMgr,
                new TestItemKeyProvider(
                        ItemKeyProvider.SCOPE_MAPPED,
                        new TestAdapter(TestData.createStringData(100))),
                mDetailsLookup,
                mSelectionPredicate,
                mGestureStarted,
                mOnItemDragListener,
                mActivationCallbacks,
                mFocusCallbacks,
                mHapticPerformer);
    }

    @Test
    public void testTap_ActivatesWhenNoExistingSelection() {
        ItemDetails doc = mDetailsLookup.initAt(11);
        mInputDelegate.onSingleTapUp(TAP);

        mActivationCallbacks.assertActivated(doc);
    }

    @Test
    public void testScroll_shouldNotBeTrapped() {
        assertFalse(mInputDelegate.onScroll(null, TAP, -1, -1));
    }

    @Test
    public void testLongPress_SelectsItem() {
        mSelectionPredicate.setReturnValue(true);

        mDetailsLookup.initAt(7);
        mInputDelegate.onLongPress(TAP);

        mSelection.assertSelection(7);
    }

    @Test
    public void testLongPress_StartsGestureSelection() {
        mSelectionPredicate.setReturnValue(true);

        mDetailsLookup.initAt(7);
        mInputDelegate.onLongPress(TAP);
        mGestureStarted.assertRan();
    }

    @Test
    public void testSelectHotspot_StartsSelectionMode() {
        mSelectionPredicate.setReturnValue(true);

        mDetailsLookup.initAt(7).setInItemSelectRegion(true);
        mInputDelegate.onSingleTapUp(TAP);

        mSelection.assertSelection(7);
    }

    @Test
    public void testSelectionHotspot_UnselectsSelectedItem() {
        mSelectionMgr.select("11");

        mDetailsLookup.initAt(11).setInItemSelectRegion(true);
        mInputDelegate.onSingleTapUp(TAP);

        mSelection.assertNoSelection();
    }

    @Test
    public void testStartsSelection_PerformsHapticFeedback() {
        mSelectionPredicate.setReturnValue(true);

        mDetailsLookup.initAt(7);
        mInputDelegate.onLongPress(TAP);

        mHapticPerformer.assertRan();
    }

    @Test
    public void testLongPress_AddsToSelection() {
        mSelectionPredicate.setReturnValue(true);

        mDetailsLookup.initAt(7);
        mInputDelegate.onLongPress(TAP);

        mDetailsLookup.initAt(99);
        mInputDelegate.onLongPress(TAP);

        mDetailsLookup.initAt(13);
        mInputDelegate.onLongPress(TAP);

        mSelection.assertSelection(7, 13, 99);
    }

    @Test
    public void testTap_UnselectsSelectedItem() {
        mSelectionMgr.select("11");

        mDetailsLookup.initAt(11);
        mInputDelegate.onSingleTapUp(TAP);

        mSelection.assertNoSelection();
    }

    @Test
    public void testTapOff_ClearsSelection() {
        mSelectionMgr.select("7");
        mDetailsLookup.initAt(7);

        mInputDelegate.onLongPress(TAP);

        mSelectionMgr.select("11");
        mDetailsLookup.initAt(11);
        mInputDelegate.onSingleTapUp(TAP);

        mDetailsLookup.initAt(RecyclerView.NO_POSITION).setInItemSelectRegion(false);
        mInputDelegate.onSingleTapUp(TAP);

        mSelection.assertNoSelection();
    }

    private static final class TestDragListener implements OnDragInitiatedListener {
        @Override
        public boolean onDragInitiated(MotionEvent e) {
            return false;
        }
    }
}
