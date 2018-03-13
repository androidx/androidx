/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.recyclerview.widget;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.os.Build;
import android.support.test.filters.MediumTest;
import android.view.View;
import android.view.accessibility.AccessibilityEvent;

import androidx.core.view.AccessibilityDelegateCompat;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@MediumTest
@RunWith(Parameterized.class)
public class RecyclerViewAccessibilityTest extends BaseRecyclerViewInstrumentationTest {
    private static final boolean SUPPORTS_COLLECTION_INFO =
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;
    private final boolean mVerticalScrollBefore;
    private final boolean mHorizontalScrollBefore;
    private final boolean mVerticalScrollAfter;
    private final boolean mHorizontalScrollAfter;

    public RecyclerViewAccessibilityTest(boolean verticalScrollBefore,
            boolean horizontalScrollBefore, boolean verticalScrollAfter,
            boolean horizontalScrollAfter) {
        mVerticalScrollBefore = verticalScrollBefore;
        mHorizontalScrollBefore = horizontalScrollBefore;
        mVerticalScrollAfter = verticalScrollAfter;
        mHorizontalScrollAfter = horizontalScrollAfter;
    }

    @Parameterized.Parameters(name = "vBefore={0},vAfter={1},hBefore={2},hAfter={3}")
    public static List<Object[]> getParams() {
        List<Object[]> params = new ArrayList<>();
        for (boolean vBefore : new boolean[]{true, false}) {
            for (boolean vAfter : new boolean[]{true, false}) {
                for (boolean hBefore : new boolean[]{true, false}) {
                    for (boolean hAfter : new boolean[]{true, false}) {
                        params.add(new Object[]{vBefore, hBefore, vAfter, hAfter});
                    }
                }
            }
        }
        return params;
    }

    @Test
    public void onInitializeAccessibilityNodeInfoTest() throws Throwable {
        final RecyclerView recyclerView = new RecyclerView(getActivity()) {
            @Override
            public boolean canScrollHorizontally(int direction) {
                return direction < 0 && mHorizontalScrollBefore ||
                        direction > 0 && mHorizontalScrollAfter;
            }

            @Override
            public boolean canScrollVertically(int direction) {
                return direction < 0 && mVerticalScrollBefore ||
                        direction > 0 && mVerticalScrollAfter;
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
                return mVerticalScrollAfter || mVerticalScrollBefore;
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
                return mHorizontalScrollAfter || mHorizontalScrollBefore;
            }
        });
        setRecyclerView(recyclerView);
        final RecyclerViewAccessibilityDelegate delegateCompat = recyclerView
                .getCompatAccessibilityDelegate();
        final AccessibilityNodeInfoCompat info = AccessibilityNodeInfoCompat.obtain();
        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                delegateCompat.onInitializeAccessibilityNodeInfo(recyclerView, info);
            }
        });
        assertEquals(mHorizontalScrollAfter || mHorizontalScrollBefore
                || mVerticalScrollAfter || mVerticalScrollBefore, info.isScrollable());
        assertEquals(mHorizontalScrollBefore || mVerticalScrollBefore,
                (info.getActions() & AccessibilityNodeInfoCompat.ACTION_SCROLL_BACKWARD) != 0);
        assertEquals(mHorizontalScrollAfter || mVerticalScrollAfter,
                (info.getActions() & AccessibilityNodeInfoCompat.ACTION_SCROLL_FORWARD) != 0);
        if (SUPPORTS_COLLECTION_INFO) {
            final AccessibilityNodeInfoCompat.CollectionInfoCompat collectionInfo = info
                    .getCollectionInfo();
            assertNotNull(collectionInfo);
            if (recyclerView.getLayoutManager().canScrollVertically()) {
                assertEquals(adapter.getItemCount(), collectionInfo.getRowCount());
            }
            if (recyclerView.getLayoutManager().canScrollHorizontally()) {
                assertEquals(adapter.getItemCount(), collectionInfo.getColumnCount());
            }
        }

        final AccessibilityEvent event = AccessibilityEvent.obtain();
        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                delegateCompat.onInitializeAccessibilityEvent(recyclerView, event);
            }
        });
        assertEquals(event.isScrollable(), mVerticalScrollAfter || mHorizontalScrollAfter
                || mVerticalScrollBefore || mHorizontalScrollBefore);
        assertEquals(event.getItemCount(), adapter.getItemCount());

        getInstrumentation().waitForIdleSync();
        if (SUPPORTS_COLLECTION_INFO) {
            for (int i = 0; i < mRecyclerView.getChildCount(); i++) {
                final View view = mRecyclerView.getChildAt(i);
                final AccessibilityNodeInfoCompat childInfo = AccessibilityNodeInfoCompat.obtain();
                mActivityRule.runOnUiThread(new Runnable() {
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
        }

        mActivityRule.runOnUiThread(new Runnable() {
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
        assertEquals(mHorizontalScrollBefore, hScrolledBack.get());
        assertEquals(mVerticalScrollBefore, vScrolledBack.get());
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
        assertEquals(mHorizontalScrollAfter, hScrolledFwd.get());
        assertEquals(mVerticalScrollAfter, vScrolledFwd.get());
    }

    @Test
    public void ignoreAccessibilityIfAdapterHasChanged() throws Throwable {
        final RecyclerView recyclerView = new RecyclerView(getActivity()) {
            //@Override
            @Override
            public boolean canScrollHorizontally(int direction) {
                return true;
            }

            //@Override
            @Override
            public boolean canScrollVertically(int direction) {
                return true;
            }
        };
        final DumbLayoutManager layoutManager = new DumbLayoutManager();
        final TestAdapter adapter = new TestAdapter(10);
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(layoutManager);
        layoutManager.expectLayouts(1);
        setRecyclerView(recyclerView);
        layoutManager.waitForLayout(1);

        final RecyclerViewAccessibilityDelegate delegateCompat = recyclerView
                .getCompatAccessibilityDelegate();
        final AccessibilityNodeInfoCompat info = AccessibilityNodeInfoCompat.obtain();
        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                delegateCompat.onInitializeAccessibilityNodeInfo(recyclerView, info);
            }
        });
        assertTrue("test sanity", info.isScrollable());
        final AccessibilityNodeInfoCompat info2 = AccessibilityNodeInfoCompat.obtain();
        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    adapter.deleteAndNotify(1, 1);
                } catch (Throwable throwable) {
                    postExceptionToInstrumentation(throwable);
                }
                delegateCompat.onInitializeAccessibilityNodeInfo(recyclerView, info2);
                assertFalse("info should not be filled if data is out of date",
                        info2.isScrollable());
            }
        });
        checkForMainThreadException();
    }

    boolean performAccessibilityAction(final AccessibilityDelegateCompat delegate,
            final RecyclerView recyclerView, final int action) throws Throwable {
        final boolean[] result = new boolean[1];
        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                result[0] = delegate.performAccessibilityAction(recyclerView, action, null);
            }
        });
        getInstrumentation().waitForIdleSync();
        Thread.sleep(250);
        return result[0];
    }
}
