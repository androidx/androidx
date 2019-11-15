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

import android.graphics.Point;
import android.graphics.Rect;
import android.view.MotionEvent;

import androidx.annotation.NonNull;
import androidx.recyclerview.selection.GridModel.GridHost;
import androidx.recyclerview.selection.testing.TestAdapter;
import androidx.recyclerview.selection.testing.TestAutoScroller;
import androidx.recyclerview.selection.testing.TestBandPredicate;
import androidx.recyclerview.selection.testing.TestData;
import androidx.recyclerview.selection.testing.TestEvents;
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
    private TestHostEnvironment mHostEnv;
    private TestBandPredicate mBandPredicate;
    private TestAdapter<String> mAdapter;
    private SelectionTracker<String> mTracker;
    TestItemKeyProvider<String> mKeyProvider;

    // Builder for all drag events. Twiddle the button's and other stuff as needed.
    TestEvents.Builder mBaseEvent;

    @Before
    public void setup() throws Exception {
        mItems = TestData.createStringData(10);
        mAdapter = new TestAdapter<>();
        mAdapter.updateTestModelIds(mItems);
        mKeyProvider = new TestItemKeyProvider<>(ItemKeyProvider.SCOPE_MAPPED, mAdapter);
        mHostEnv = new TestHostEnvironment();
        mBandPredicate = new TestBandPredicate();

        OperationMonitor operationMonitor = new OperationMonitor();

        mTracker = new DefaultSelectionTracker<>(
                "band-selection-test",
                mKeyProvider,
                SelectionPredicates.createSelectAnything(),
                StorageStrategy.createStringStorage());

        EventBridge.install(mAdapter, mTracker, mKeyProvider);
        FocusDelegate<String> focusDelegate = FocusDelegate.dummy();

        mBandController = new BandSelectionHelper<String>(
                mHostEnv,
                new TestAutoScroller(),
                mKeyProvider,
                mTracker,
                mBandPredicate,
                focusDelegate,
                operationMonitor);

        // No buttons pressed. Tests can fiddle with this as needed.
        mBaseEvent = TestEvents.builder()
                .location(1, 1)
                .mouse()
                .move();
    }

    private boolean startBandSelect() {
        return startBandSelect(mBaseEvent.primary().build());
    }

    // Allows tests to pass an event that might not start band selection.
    private boolean startBandSelect(MotionEvent e) {
        mBandController.onInterceptTouchEvent(null, e);
        return mBandController.isResetRequired();
    }

    private boolean stopBandSelect() {
        mBandController.onInterceptTouchEvent(null, mBaseEvent.up().build());
        return !mBandController.isResetRequired();
    }

    @Test
    public void testStart() {
        assertTrue(startBandSelect());
    }

    @Test
    public void testStart_RejectedByPredicate() {
        mBandPredicate.setCanInitiate(false);
        assertFalse(startBandSelect());
    }

    @Test
    public void testRequiresReset() {
        assertFalse(mBandController.isResetRequired());
        startBandSelect();
        assertTrue(mBandController.isResetRequired());
    }

    @Test
    public void testReset() {
        startBandSelect();
        mBandController.reset();
        assertFalse(mBandController.isResetRequired());
    }

    @Test
    public void testReset_HidesBand() {
        startBandSelect();
        mBandController.reset();
        mHostEnv.assertBandHidden();
    }

    @Test
    public void testStart_IgnoresNonPrimaryDragEvents() {
        // W/ no buttons pressed.
        assertFalse(startBandSelect(mBaseEvent.build()));

        // With wrong buttons pressed.
        assertFalse(startBandSelect(mBaseEvent.secondary().build()));
        assertFalse(startBandSelect(mBaseEvent.tertiary().build()));
    }

    @Test
    public void testStart_IgnoresNonMoveEvents() {
        // Primary button has to be pressed for controller to pay attention to an event.
        mBaseEvent.primary();

        // Override MOVE action with UP and DOWN to verify they are ignored.
        assertFalse(startBandSelect(mBaseEvent.up().build()));
        assertFalse(startBandSelect(mBaseEvent.down().build()));
    }

    @Test
    public void testStarts_NoItems() {
        // Band selection can happen in a view without any items.
        mAdapter.updateTestModelIds(Collections.<String>emptyList());

        assertTrue(startBandSelect());
    }

    @Test
    public void testStops() {
        startBandSelect();
        assertTrue(stopBandSelect());
    }

    @Test
    public void testStop_IgnoresUpWhenNotStarted() {
        assertTrue(stopBandSelect());
    }

    // GridHost extends BandHost. We satisfy both by implementing GridHost.
    private final class TestHostEnvironment extends GridHost<String> {

        private boolean mBandHidden;

        @Override
        GridModel<String> createGridModel() {
            return new GridModel<String>(
                    this,
                    mKeyProvider,
                    SelectionPredicates.createSelectAnything());
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
            // ignored for testing
        }

        @Override
        void removeOnScrollListener(@NonNull OnScrollListener listener) {
            // ignored for testing
        }

        @Override
        Point createAbsolutePoint(@NonNull Point relativePoint) {
            throw new UnsupportedOperationException();
        }

        @Override
        Rect getAbsoluteRectForChildViewAt(int index) {
            throw new UnsupportedOperationException();
        }

        @Override
        int getAdapterPositionAt(int index) {
            throw new UnsupportedOperationException();
        }

        @Override
        int getColumnCount() {
            throw new UnsupportedOperationException();
        }

        @Override
        int getVisibleChildCount() {
            return 0;
        }

        @Override
        boolean hasView(int adapterPosition) {
            throw new UnsupportedOperationException();
        }
    }
}
