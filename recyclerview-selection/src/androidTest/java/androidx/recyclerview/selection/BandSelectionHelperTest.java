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

import android.graphics.Rect;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;
import android.view.MotionEvent;

import androidx.recyclerview.selection.testing.TestAdapter;
import androidx.recyclerview.selection.testing.TestAutoScroller;
import androidx.recyclerview.selection.testing.TestBandPredicate;
import androidx.recyclerview.selection.testing.TestData;
import androidx.recyclerview.selection.testing.TestEvents.Builder;
import androidx.recyclerview.selection.testing.TestItemKeyProvider;
import androidx.recyclerview.widget.RecyclerView.OnScrollListener;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Collections;
import java.util.List;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class BandSelectionHelperTest {

    private List<String> mItems;
    private BandSelectionHelper<String> mBandController;
    private boolean mIsActive;
    private Builder mStartBuilder;
    private Builder mStopBuilder;
    private MotionEvent mStartEvent;
    private MotionEvent mStopEvent;
    private TestBandHost mBandHost;
    private TestBandPredicate mBandPredicate;
    private TestAdapter<String> mAdapter;

    @Before
    public void setup() throws Exception {
        mItems = TestData.createStringData(10);
        mIsActive = false;
        mAdapter = new TestAdapter<String>();
        mAdapter.updateTestModelIds(mItems);
        mBandHost = new TestBandHost();
        mBandPredicate = new TestBandPredicate();
        ItemKeyProvider<String> keyProvider =
                new TestItemKeyProvider<>(ItemKeyProvider.SCOPE_MAPPED, mAdapter);
        OperationMonitor operationMonitor = new OperationMonitor();

        SelectionTracker<String> helper = new DefaultSelectionTracker<String>(
                "band-selection-test",
                keyProvider,
                SelectionPredicates.createSelectAnything(),
                StorageStrategy.createStringStorage());

        EventBridge.install(mAdapter, helper, keyProvider);
        FocusDelegate<String> focusDelegate = FocusDelegate.dummy();

        mBandController = new BandSelectionHelper<String>(
                mBandHost,
                new TestAutoScroller(),
                keyProvider,
                helper,
                mBandPredicate,
                focusDelegate,
                operationMonitor) {
                    @Override
                    public boolean isActive() {
                        return mIsActive;
                    }
                };

        mStartBuilder = new Builder().mouse().primary().action(MotionEvent.ACTION_MOVE);
        mStopBuilder = new Builder().mouse().action(MotionEvent.ACTION_UP);
        mStartEvent = mStartBuilder.build();
        mStopEvent = mStopBuilder.build();
    }

    @Test
    public void testStartsBand() {
        assertTrue(mBandController.shouldStart(mStartEvent));
    }

    @Test
    public void testStartsBand_NoItems() {
        mAdapter.updateTestModelIds(Collections.EMPTY_LIST);
        assertTrue(mBandController.shouldStart(mStartEvent));
    }

    @Test
    public void testBadStart_NoButtons() {
        assertFalse(mBandController.shouldStart(
                mStartBuilder.releaseButton(MotionEvent.BUTTON_PRIMARY).build()));
    }

    @Test
    public void testBadStart_SecondaryButton() {
        assertFalse(
                mBandController.shouldStart(mStartBuilder.secondary().build()));
    }

    @Test
    public void testBadStart_TertiaryButton() {
        assertFalse(
                mBandController.shouldStart(mStartBuilder.tertiary().build()));
    }

    @Test
    public void testBadStart_Touch() {
        assertFalse(mBandController.shouldStart(
                mStartBuilder.touch().releaseButton(MotionEvent.BUTTON_PRIMARY).build()));
    }

    @Test
    public void testBadStart_RespectsCanInitiateBand() {
        mBandPredicate.setCanInitiate(false);
        assertFalse(mBandController.shouldStart(mStartEvent));
    }

    @Test
    public void testBadStart_ActionDown() {
        assertFalse(mBandController
                .shouldStart(mStartBuilder.action(MotionEvent.ACTION_DOWN).build()));
    }

    @Test
    public void testBadStart_ActionUp() {
        assertFalse(mBandController
                .shouldStart(mStartBuilder.action(MotionEvent.ACTION_UP).build()));
    }

    @Test
    public void testBadStart_ActionPointerDown() {
        assertFalse(mBandController.shouldStart(
                mStartBuilder.action(MotionEvent.ACTION_POINTER_DOWN).build()));
    }

    @Test
    public void testBadStart_ActionPointerUp() {
        assertFalse(mBandController.shouldStart(
                mStartBuilder.action(MotionEvent.ACTION_POINTER_UP).build()));
    }

    @Test
    public void testBadStart_alreadyActive() {
        mIsActive = true;
        assertFalse(mBandController.shouldStart(mStartEvent));
    }

    @Test
    public void testGoodStop() {
        mIsActive = true;
        assertTrue(mBandController.shouldStop(mStopEvent));
    }

    @Test
    public void testGoodStop_PointerUp() {
        mIsActive = true;
        assertTrue(mBandController
                .shouldStop(mStopBuilder.action(MotionEvent.ACTION_POINTER_UP).build()));
    }

    @Test
    public void testGoodStop_Cancel() {
        mIsActive = true;
        assertTrue(mBandController
                .shouldStop(mStopBuilder.action(MotionEvent.ACTION_CANCEL).build()));
    }

    @Test
    public void testBadStop_NotActive() {
        assertFalse(mBandController.shouldStop(mStopEvent));
    }

    @Test
    public void testBadStop_Move() {
        mIsActive = true;
        assertFalse(mBandController.shouldStop(
                mStopBuilder.action(MotionEvent.ACTION_MOVE).touch().build()));
    }

    @Test
    public void testBadStop_Down() {
        mIsActive = true;
        assertFalse(mBandController.shouldStop(
                mStopBuilder.action(MotionEvent.ACTION_DOWN).touch().build()));
    }

    private final class TestBandHost extends BandSelectionHelper.BandHost<String> {
        @Override
        GridModel<String> createGridModel() {
            throw new UnsupportedOperationException();
        }

        @Override
        void showBand(Rect rect) {
            throw new UnsupportedOperationException();
        }

        @Override
        void hideBand() {
            throw new UnsupportedOperationException();
        }

        @Override
        void addOnScrollListener(OnScrollListener listener) {
        }
    }
}
