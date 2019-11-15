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

import static androidx.recyclerview.selection.testing.TestEvents.Mouse;
import static androidx.recyclerview.selection.testing.TestEvents.Touch;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.graphics.Rect;

import androidx.recyclerview.selection.testing.TestAdapter;
import androidx.recyclerview.selection.testing.TestAutoScroller;
import androidx.recyclerview.selection.testing.TestBandPredicate;
import androidx.recyclerview.selection.testing.TestData;
import androidx.recyclerview.selection.testing.TestItemKeyProvider;
import androidx.recyclerview.widget.RecyclerView.OnScrollListener;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

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
    private TestBandHost mBandHost;
    private TestBandPredicate mBandPredicate;
    private TestAdapter<String> mAdapter;
    private SelectionTracker<String> mTracker;

    @Before
    public void setup() throws Exception {
        mItems = TestData.createStringData(10);
        mIsActive = false;
        mAdapter = new TestAdapter<>();
        mAdapter.updateTestModelIds(mItems);
        mBandHost = new TestBandHost();
        mBandPredicate = new TestBandPredicate();
        ItemKeyProvider<String> keyProvider =
                new TestItemKeyProvider<>(ItemKeyProvider.SCOPE_MAPPED, mAdapter);
        OperationMonitor operationMonitor = new OperationMonitor();

        mTracker = new DefaultSelectionTracker<>(
                "band-selection-test",
                keyProvider,
                SelectionPredicates.createSelectAnything(),
                StorageStrategy.createStringStorage());

        EventBridge.install(mAdapter, mTracker, keyProvider);
        FocusDelegate<String> focusDelegate = FocusDelegate.dummy();

        mBandController = new BandSelectionHelper<String>(
                mBandHost,
                new TestAutoScroller(),
                keyProvider,
                mTracker,
                mBandPredicate,
                focusDelegate,
                operationMonitor) {
            @Override
            public boolean isActive() {
                return mIsActive;
            }
        };
    }

    @Test
    public void testReset_HidesBand() {
        mIsActive = true;
        mBandController.reset();
        mBandHost.assertBandHidden();
    }

    @Test
    public void testStart() {
        assertTrue(mBandController.shouldStart(Mouse.PRIMARY_DRAG));
    }

    @Test
    public void testStart_NoItems() {
        // Band selection can happen in a view without any items.
        mAdapter.updateTestModelIds(Collections.<String>emptyList());
        assertTrue(mBandController.shouldStart(Mouse.PRIMARY_DRAG));
    }

    @Test
    public void testNoStart_NoButtons() {
        assertFalse(mBandController.shouldStart(Mouse.MOVE));
    }

    @Test
    public void testNoStart_SecondaryButton() {
        assertFalse(mBandController.shouldStart(Mouse.SECONDARY_DRAG));
    }

    @Test
    public void testNoStart_TertiaryButton() {
        assertFalse(mBandController.shouldStart(Mouse.TERTIARY_DRAG));
    }

    @Test
    public void testNoStart_Touch() {
        assertFalse(mBandController.shouldStart(Touch.MOVE));
    }

    @Test
    public void testNoStart_RejectedByPredicate() {
        mBandPredicate.setCanInitiate(false);
        assertFalse(mBandController.shouldStart(Mouse.PRIMARY_DRAG));
    }

    @Test
    public void testNoStart() {
        // only starts on
        assertFalse(mBandController.shouldStart(Mouse.DOWN));
        assertFalse(mBandController.shouldStart(Mouse.UP));
        assertFalse(mBandController.shouldStart(Mouse.POINTER_UP));
        assertFalse(mBandController.shouldStart(Mouse.POINTER_DOWN));
    }


    @Test
    public void testNoStart_AlreadyStarted() {
        mIsActive = true;
        assertFalse(mBandController.shouldStart(Mouse.PRIMARY_DRAG));
    }

    @Test
    public void testStops() {
        mIsActive = true;
        assertTrue(mBandController.shouldStop(Mouse.UP));
    }

    @Test
    public void testNoStop_Down() {
        mIsActive = true;
        // Not really sure when this would, happen, maybe a secondary
        // or tertiary button press while dragging?
        assertFalse(mBandController.shouldStop(Touch.DOWN));  // :) Touch Down!
    }

    @Test
    public void testNoStop_NotActive() {
        assertFalse(mBandController.shouldStop(Mouse.UP));
        assertFalse(mBandController.shouldStop(Touch.MOVE));
    }

    private final class TestBandHost extends BandSelectionHelper.BandHost<String> {
        private boolean mBandHidden;

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
            mBandHidden = true;
        }

        // Asserts that a call was made to hide the band.
        void assertBandHidden() {
            assertTrue(mBandHidden);
        }

        @Override
        void addOnScrollListener(OnScrollListener listener) {
        }
    }
}
