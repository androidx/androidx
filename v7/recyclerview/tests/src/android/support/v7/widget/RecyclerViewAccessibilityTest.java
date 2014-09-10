/*
 * Copyright (C) 2014 The Android Open Source Project
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

package android.support.v7.widget;

import android.support.v4.view.AccessibilityDelegateCompat;
import android.support.v4.view.accessibility.AccessibilityEventCompat;
import android.support.v4.view.accessibility.AccessibilityNodeInfoCompat;
import android.support.v4.view.accessibility.AccessibilityRecordCompat;
import android.view.View;
import android.view.accessibility.AccessibilityEvent;

import java.util.concurrent.atomic.AtomicBoolean;

public class RecyclerViewAccessibilityTest extends BaseRecyclerViewInstrumentationTest {
    public void testOnInitializeAccessibilityNodeInfo() throws Throwable {
        for (boolean vBefore : new boolean[]{true, false}) {
            for (boolean vAfter : new boolean[]{true, false}) {
                for (boolean hBefore : new boolean[]{true, false}) {
                    for (boolean hAfter : new boolean[]{true, false}) {
                        onInitializeAccessibilityNodeInfoTest(vBefore, hBefore,
                                vAfter, hAfter);
                        removeRecyclerView();
                    }
                }
            }
        }
    }
    public void onInitializeAccessibilityNodeInfoTest(final boolean verticalScrollBefore,
            final boolean horizontalScrollBefore, final boolean verticalScrollAfter,
            final boolean horizontalScrollAfter) throws Throwable {
        final RecyclerView recyclerView = new RecyclerView(getActivity()) {
            //@Override
            public boolean canScrollHorizontally(int direction) {
                return direction < 0 && horizontalScrollBefore ||
                        direction > 0 && horizontalScrollAfter;
            }

            //@Override
            public boolean canScrollVertically(int direction) {
                return direction < 0 && verticalScrollBefore ||
                        direction > 0 && verticalScrollAfter;
            }
        };
        final TestAdapter adapter = new TestAdapter(10);
        final AtomicBoolean hScrolledBack = new AtomicBoolean(false);
        final AtomicBoolean vScrolledBack = new AtomicBoolean(false);
        final AtomicBoolean hScrolledFwd = new AtomicBoolean(false);
        final AtomicBoolean vScrolledFwd = new AtomicBoolean(false);
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new TestLayoutManager() {

            @Override
            public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
                layoutRange(recycler, 0, 5);
            }

            @Override
            public RecyclerView.LayoutParams generateDefaultLayoutParams() {
                return new RecyclerView.LayoutParams(-1, -1);
            }

            @Override
            public boolean canScrollVertically() {
                return verticalScrollAfter || verticalScrollBefore;
            }

            @Override
            public int scrollHorizontallyBy(int dx, RecyclerView.Recycler recycler,
                    RecyclerView.State state) {
                if (dx > 0) {
                    hScrolledFwd.set(true);
                } else if (dx < 0) {
                    hScrolledBack.set(true);
                }
                return 0;
            }

            @Override
            public int scrollVerticallyBy(int dy, RecyclerView.Recycler recycler,
                    RecyclerView.State state) {
                if (dy > 0) {
                    vScrolledFwd.set(true);
                } else if (dy < 0) {
                    vScrolledBack.set(true);
                }
                return 0;
            }

            @Override
            public boolean canScrollHorizontally() {
                return horizontalScrollAfter || horizontalScrollBefore;
            }
        });
        setRecyclerView(recyclerView);
        final RecyclerViewAccessibilityDelegate delegateCompat = recyclerView
                .getCompatAccessibilityDelegate();
        final AccessibilityNodeInfoCompat info = AccessibilityNodeInfoCompat.obtain();
        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                delegateCompat.onInitializeAccessibilityNodeInfo(recyclerView, info);
            }
        });
        assertEquals(horizontalScrollAfter || horizontalScrollBefore
                || verticalScrollAfter || verticalScrollBefore, info.isScrollable());
        assertEquals(horizontalScrollBefore || verticalScrollBefore,
                (info.getActions() & AccessibilityNodeInfoCompat.ACTION_SCROLL_BACKWARD) != 0);
        assertEquals(horizontalScrollAfter || verticalScrollAfter,
                (info.getActions() & AccessibilityNodeInfoCompat.ACTION_SCROLL_FORWARD) != 0);
        final AccessibilityNodeInfoCompat.CollectionInfoCompat collectionInfo = info
                .getCollectionInfo();
        assertNotNull(collectionInfo);
        if (recyclerView.getLayoutManager().canScrollVertically()) {
            assertEquals(adapter.getItemCount(), collectionInfo.getRowCount());
        }
        if (recyclerView.getLayoutManager().canScrollHorizontally()) {
            assertEquals(adapter.getItemCount(), collectionInfo.getColumnCount());
        }

        final AccessibilityEvent event = AccessibilityEvent.obtain();
        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                delegateCompat.onInitializeAccessibilityEvent(recyclerView, event);
            }
        });
        final AccessibilityRecordCompat record = AccessibilityEventCompat
                .asRecord(event);
        assertEquals(record.isScrollable(), verticalScrollAfter || horizontalScrollAfter ||
        verticalScrollBefore || horizontalScrollBefore);
        assertEquals(record.getItemCount(), adapter.getItemCount());

        getInstrumentation().waitForIdleSync();
        for (int i = 0; i < mRecyclerView.getChildCount(); i ++) {
            final View view = mRecyclerView.getChildAt(i);
            final AccessibilityNodeInfoCompat childInfo = AccessibilityNodeInfoCompat.obtain();
            runTestOnUiThread(new Runnable() {
                @Override
                public void run() {
                    delegateCompat.getItemDelegate().
                            onInitializeAccessibilityNodeInfo(view, childInfo);
                }
            });
            final AccessibilityNodeInfoCompat.CollectionItemInfoCompat collectionItemInfo
                    = childInfo.getCollectionItemInfo();
            assertNotNull(collectionItemInfo);
            if (recyclerView.getLayoutManager().canScrollHorizontally()) {
                assertEquals(i, collectionItemInfo.getColumnIndex());
            } else {
                assertEquals(0, collectionItemInfo.getColumnIndex());
            }

            if (recyclerView.getLayoutManager().canScrollVertically()) {
                assertEquals(i, collectionItemInfo.getRowIndex());
            } else {
                assertEquals(0, collectionItemInfo.getRowIndex());
            }
        }

        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {

            }
        });
        hScrolledBack.set(false);
        vScrolledBack.set(false);
        hScrolledFwd.set(false);
        vScrolledBack.set(false);
        performAccessibilityAction(delegateCompat, recyclerView,
                AccessibilityNodeInfoCompat.ACTION_SCROLL_BACKWARD);
        assertEquals(horizontalScrollBefore, hScrolledBack.get());
        assertEquals(verticalScrollBefore, vScrolledBack.get());
        assertEquals(false, hScrolledFwd.get());
        assertEquals(false, vScrolledFwd.get());

        hScrolledBack.set(false);
        vScrolledBack.set(false);
        hScrolledFwd.set(false);
        vScrolledBack.set(false);
        performAccessibilityAction(delegateCompat, recyclerView,
                AccessibilityNodeInfoCompat.ACTION_SCROLL_FORWARD);
        assertEquals(false, hScrolledBack.get());
        assertEquals(false, vScrolledBack.get());
        assertEquals(horizontalScrollAfter, hScrolledFwd.get());
        assertEquals(verticalScrollAfter, vScrolledFwd.get());
    }

    void performAccessibilityAction(final AccessibilityDelegateCompat delegate,
            final RecyclerView recyclerView,  final int action) throws Throwable {
        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                delegate.performAccessibilityAction(recyclerView, action, null);
            }
        });
        getInstrumentation().waitForIdleSync();
        Thread.sleep(250);
    }
}
