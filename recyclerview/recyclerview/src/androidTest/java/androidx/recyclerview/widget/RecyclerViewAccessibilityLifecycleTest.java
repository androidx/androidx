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

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.core.view.AccessibilityDelegateCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;
import androidx.core.view.accessibility.AccessibilityNodeProviderCompat;
import androidx.recyclerview.test.R;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.filters.MediumTest;

import org.jspecify.annotations.NonNull;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@MediumTest
@RunWith(AndroidJUnit4.class)
public class RecyclerViewAccessibilityLifecycleTest extends BaseRecyclerViewInstrumentationTest {
    @Test
    public void dontDispatchChangeDuringLayout() throws Throwable {
        LayoutAllLayoutManager lm = new LayoutAllLayoutManager();
        final AtomicBoolean calledA11DuringLayout = new AtomicBoolean(false);
        final List<Integer> invocations = new ArrayList<>();

        final WrappedRecyclerView recyclerView = new WrappedRecyclerView(getActivity()) {
            @Override
            boolean isAccessibilityEnabled() {
                return true;
            }

            @Override
            public boolean setChildImportantForAccessibilityInternal(ViewHolder viewHolder,
                    int importantForAccessibilityBeforeHidden) {
                invocations.add(importantForAccessibilityBeforeHidden);
                boolean notified = super.setChildImportantForAccessibilityInternal(viewHolder,
                        importantForAccessibilityBeforeHidden);
                if (notified && mRecyclerView.isComputingLayout()) {
                    calledA11DuringLayout.set(true);
                }
                return notified;
            }
        };
        TestAdapter adapter = new TestAdapter(10) {
            @Override
            public TestViewHolder onCreateViewHolder(@NonNull ViewGroup parent,
                    int viewType) {
                TestViewHolder vh = super.onCreateViewHolder(parent, viewType);
                vh.itemView.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);
                return vh;
            }
        };
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(lm);
        lm.expectLayouts(1);
        setRecyclerView(recyclerView);
        lm.waitForLayout(1);
        assertThat(calledA11DuringLayout.get(), is(false));
        lm.expectLayouts(1);
        adapter.deleteAndNotify(2, 2);
        lm.waitForLayout(2);
        recyclerView.waitUntilAnimations();
        assertThat(invocations, is(Arrays.asList(
                View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS,
                View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS,
                View.IMPORTANT_FOR_ACCESSIBILITY_YES,
                View.IMPORTANT_FOR_ACCESSIBILITY_YES)));

        assertThat(calledA11DuringLayout.get(), is(false));
    }

    @LargeTest
    @Test
    public void processAllViewHolders() {
        RecyclerView rv = new RecyclerView(getActivity());
        rv.setLayoutManager(new LinearLayoutManager(getActivity()));
        View itemView1 = spy(new View(getActivity()));
        View itemView2 = spy(new View(getActivity()));
        View itemView3 = spy(new View(getActivity()));

        rv.addView(itemView1);
        // do not add 2
        rv.addView(itemView3);

        RecyclerView.ViewHolder vh1 = new RecyclerView.ViewHolder(itemView1) {};
        vh1.mPendingAccessibilityState = View.IMPORTANT_FOR_ACCESSIBILITY_YES;
        RecyclerView.ViewHolder vh2 = new RecyclerView.ViewHolder(itemView2) {};
        vh2.mPendingAccessibilityState = View.IMPORTANT_FOR_ACCESSIBILITY_YES;
        RecyclerView.ViewHolder vh3 = new RecyclerView.ViewHolder(itemView3) {};
        vh3.mPendingAccessibilityState = View.IMPORTANT_FOR_ACCESSIBILITY_AUTO;

        rv.mPendingAccessibilityImportanceChange.add(vh1);
        rv.mPendingAccessibilityImportanceChange.add(vh2);
        rv.mPendingAccessibilityImportanceChange.add(vh3);
        rv.dispatchPendingImportantForAccessibilityChanges();

        verify(itemView1).setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);
        //noinspection WrongConstant
        verify(itemView2, never()).setImportantForAccessibility(anyInt());
        verify(itemView3).setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_AUTO);
        assertThat(rv.mPendingAccessibilityImportanceChange.size(), is(0));
    }

    public class LayoutAllLayoutManager extends TestLayoutManager {
        private final boolean mAllowNullLayoutLatch;

        public LayoutAllLayoutManager() {
            // by default, we don't allow unexpected layouts.
            this(false);
        }
        LayoutAllLayoutManager(boolean allowNullLayoutLatch) {
            mAllowNullLayoutLatch = allowNullLayoutLatch;
        }

        @Override
        public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
            detachAndScrapAttachedViews(recycler);
            layoutRange(recycler, 0, state.getItemCount());
            if (!mAllowNullLayoutLatch || layoutLatch != null) {
                layoutLatch.countDown();
            }
        }
    }

    @Test
    public void notClearCustomViewDelegateAndMaintainItemDelegate() throws Throwable {
        final RecyclerView recyclerView = new RecyclerView(getActivity()) {
            @Override
            boolean isAccessibilityEnabled() {
                return true;
            }
        };
        final int[] layoutStart = new int[] {0};
        final int layoutCount = 5;
        final TestLayoutManager layoutManager = new TestLayoutManager() {
            @Override
            public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
                detachAndScrapAttachedViews(recycler);
                removeAndRecycleScrapInt(recycler);
                layoutRange(recycler, layoutStart[0], layoutStart[0] + layoutCount);
                if (layoutLatch != null) {
                    layoutLatch.countDown();
                }
            }
        };
        final AccessibilityDelegateCompat delegateCompat = new AccessibilityDelegateCompat() {
            @Override
            public void onInitializeAccessibilityNodeInfo(View host,
                    AccessibilityNodeInfoCompat info) {
                super.onInitializeAccessibilityNodeInfo(host, info);
                info.setChecked(true);
            }
        };
        final TestAdapter adapter = new TestAdapter(100) {
            @Override
            public TestViewHolder onCreateViewHolder(@NonNull ViewGroup parent,
                    int viewType) {
                TestViewHolder vh = super.onCreateViewHolder(parent, viewType);
                ViewCompat.setAccessibilityDelegate(vh.itemView, delegateCompat);
                return vh;
            }
        };
        layoutManager.expectLayouts(1);
        recyclerView.getRecycledViewPool().setMaxRecycledViews(0, 100);
        recyclerView.setItemViewCacheSize(0); // no cache, directly goes to pool
        recyclerView.setLayoutManager(layoutManager);
        setRecyclerView(recyclerView);
        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                recyclerView.setAdapter(adapter);
            }
        });
        layoutManager.waitForLayout(1);

        assertEquals(layoutCount, recyclerView.getChildCount());
        final ArrayList<View> children = new ArrayList<>();
        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < recyclerView.getChildCount(); i++) {
                    View view = recyclerView.getChildAt(i);
                    assertEquals(layoutStart[0] + i,
                            recyclerView.getChildAdapterPosition(view));
                    AccessibilityNodeInfo info = recyclerView.getChildAt(i)
                            .createAccessibilityNodeInfo();
                    assertTrue("custom delegate sets isChecked", info.isChecked());
                    assertNotNull(info.getCollectionItemInfo());
                    children.add(view);
                }
            }
        });

        // invalidate and start layout at 50, all existing views will goes to recycler and
        // being reused.
        layoutStart[0] = 50;
        layoutManager.expectLayouts(1);
        adapter.dispatchDataSetChanged();
        layoutManager.waitForLayout(1);
        assertEquals(layoutCount, recyclerView.getChildCount());
        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < recyclerView.getChildCount(); i++) {
                    View view = recyclerView.getChildAt(i);
                    assertEquals(layoutStart[0] + i,
                            recyclerView.getChildAdapterPosition(view));
                    assertTrue(children.contains(view));
                    AccessibilityNodeInfo info = view.createAccessibilityNodeInfo();
                    assertTrue("custom delegate sets isChecked", info.isChecked());
                    assertNotNull(info.getCollectionItemInfo());
                }
            }
        });
    }

    @Test
    public void clearItemDelegateWhenGoesToPool() throws Throwable {
        final RecyclerView recyclerView = new RecyclerView(getActivity()) {
            @Override
            boolean isAccessibilityEnabled() {
                return true;
            }
        };
        final int firstPassLayoutCount = 5;
        final int[] layoutCount = new int[]{firstPassLayoutCount};
        final TestLayoutManager layoutManager = new TestLayoutManager() {
            @Override
            public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
                detachAndScrapAttachedViews(recycler);
                removeAndRecycleScrapInt(recycler);
                layoutRange(recycler, 0, layoutCount[0]);
                if (layoutLatch != null) {
                    layoutLatch.countDown();
                }
            }
        };
        final TestAdapter adapter = new TestAdapter(100);
        layoutManager.expectLayouts(1);
        recyclerView.getRecycledViewPool().setMaxRecycledViews(0, 100);
        recyclerView.setItemViewCacheSize(0); // no cache, directly goes to pool
        recyclerView.setLayoutManager(layoutManager);
        setRecyclerView(recyclerView);
        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                recyclerView.setAdapter(adapter);

            }
        });
        layoutManager.waitForLayout(1);

        assertEquals(firstPassLayoutCount, recyclerView.getChildCount());
        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < recyclerView.getChildCount(); i++) {
                    View view = recyclerView.getChildAt(i);
                    assertEquals(i, recyclerView.getChildAdapterPosition(view));
                    assertTrue(accessibiltyDelegateIsItemDelegate(recyclerView, view));
                    AccessibilityNodeInfo info = view.createAccessibilityNodeInfo();
                    assertNotNull(info.getCollectionItemInfo());
                }
            }
        });

        // let all items go to recycler pool
        layoutManager.expectLayouts(1);
        layoutCount[0] = 0;
        adapter.resetItemsTo(new ArrayList<Item>());
        layoutManager.waitForLayout(1);
        assertEquals(0, recyclerView.getChildCount());
        assertEquals(firstPassLayoutCount, recyclerView.getRecycledViewPool()
                .getRecycledViewCount(0));
        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < firstPassLayoutCount; i++) {
                    RecyclerView.ViewHolder vh = recyclerView.getRecycledViewPool()
                            .getRecycledView(0);
                    View view = vh.itemView;
                    assertEquals(RecyclerView.NO_POSITION,
                            recyclerView.getChildAdapterPosition(view));
                    assertFalse(accessibiltyDelegateIsItemDelegate(recyclerView, view));
                }
            }
        });
    }

    @Test
    public void onInitNodeInfoWithNestedDelegateDoesntAddChildrenTwice() throws Throwable {
        final AccessibilityDelegateCompat delegateCompat = new AccessibilityDelegateCompat() {
            @Override
            public void onInitializeAccessibilityNodeInfo(View host,
                    AccessibilityNodeInfoCompat info) {
                super.onInitializeAccessibilityNodeInfo(host, info);
            }};
        testCustomAccessibilityDelegateWithAdapter(
                new Runnable() {
                    @Override
                    public void run() {
                        ViewGroup itemView = (ViewGroup) mRecyclerView.getChildAt(0);
                        AccessibilityNodeInfoCompat info =
                                AccessibilityNodeInfoCompat.wrap(
                                        itemView.createAccessibilityNodeInfo());
                        assertEquals(1, info.getChildCount());
                    }
                },
                new TestAdapter(100) {
                    @Override
                    public TestViewHolder onCreateViewHolder(@NonNull ViewGroup parent,
                            int viewType) {
                        FrameLayout fl = new FrameLayout(parent.getContext());
                        TextView textView = new TextView(parent.getContext());
                        textView.setFocusableInTouchMode(true);
                        textView.setFocusable(true);
                        fl.addView(textView);
                        ViewCompat.setAccessibilityDelegate(fl, delegateCompat);
                        return new TestViewHolder(fl);
                    }

                    @Override
                    protected TextView getTextViewInHolder(TestViewHolder holder) {
                        return (TextView) ((ViewGroup) holder.itemView).getChildAt(0);
                    }
                });
    }

    @Test
    public void onInitNodeInfoWithNestedDelegateReturnsNodeProvider() throws Throwable {
        final AccessibilityNodeProviderCompat expectedNodeProvider =
                new AccessibilityNodeProviderCompat();
        testCustomAccessibilityDelegate(new AccessibilityDelegateCompat() {
                @Override
                public AccessibilityNodeProviderCompat getAccessibilityNodeProvider(View host) {
                    return expectedNodeProvider;
                }},
                new Runnable() {
                    @Override
                    public void run() {
                        View itemView = mRecyclerView.getChildAt(0);
                        AccessibilityNodeProviderCompat actualNodeProvider =
                                ViewCompat.getAccessibilityNodeProvider(itemView);
                        assertEquals(actualNodeProvider.getProvider(),
                                expectedNodeProvider.getProvider());
                    }
                });
    }

    @Test
    public void onInitNodeInfoWithnestedDelegateEveryoneGetsToPopulate() throws Throwable {
        testCustomAccessibilityDelegate(new AccessibilityDelegateCompat() {
                @Override
                public void onInitializeAccessibilityNodeInfo(View host,
                        AccessibilityNodeInfoCompat info) {
                    super.onInitializeAccessibilityNodeInfo(host, info);
                    info.setCheckable(true);
                }},
                new Runnable() {
                    @Override
                    public void run() {
                        View itemView = mRecyclerView.getChildAt(0);

                        AccessibilityNodeInfoCompat info =
                                AccessibilityNodeInfoCompat.wrap(
                                        itemView.createAccessibilityNodeInfo());
                        assertTrue(info.isCheckable());
                        assertTrue(info.isPassword());
                    }
                },
                new TextViewCreator() {
                    public TextView createView(Context context) {
                        return new TextView(context) {
                            public void onInitializeAccessibilityNodeInfo(
                                    AccessibilityNodeInfo info) {
                                super.onInitializeAccessibilityNodeInfo(info);
                                info.setPassword(true);
                            }
                        };
                    }
                });
    }

    @Test
    public void performActionWithhNestedDelegate() throws Throwable {
        final int expectedActionId = 42;
        testCustomAccessibilityDelegate(new AccessibilityDelegateCompat() {
                @Override
                public boolean performAccessibilityAction(View host, int action, Bundle args) {
                    return action == expectedActionId;
                }},
                new Runnable() {
                    @Override
                    public void run() {
                        View itemView = mRecyclerView.getChildAt(0);
                        assertTrue(itemView.performAccessibilityAction(42, null));
                    }
                });
    }

    @Test
    public void performActionWithhNestedDelegateCallsView() throws Throwable {
        final int expectedActionId = 42;
        testCustomAccessibilityDelegate(new AccessibilityDelegateCompat() {
                @Override
                public boolean performAccessibilityAction(View host, int action, Bundle args) {
                    return action < expectedActionId;
                }},
                new Runnable() {
                    @Override
                    public void run() {
                        View itemView = mRecyclerView.getChildAt(0);
                        assertTrue(itemView.performAccessibilityAction(42, null));
                    }
                },
                new TextViewCreator() {
                    public TextView createView(Context context) {
                        return new TextView(context) {
                            public boolean performAccessibilityAction(int action,
                                    Bundle arguments) {
                                return expectedActionId == action;
                            }
                        };
                    }
                });
    }

    @Test
    public void customItemDelegate() throws Throwable {
        final RecyclerView recyclerView = new RecyclerView(getActivity()) {
            @Override
            boolean isAccessibilityEnabled() {
                return true;
            }
        };
        recyclerView.setAccessibilityDelegateCompat(
                    new RecyclerViewAccessibilityDelegate(recyclerView) {
                @Override
                public @NonNull AccessibilityDelegateCompat getItemDelegate() {
                    return new RecyclerViewAccessibilityDelegate.ItemDelegate(this) {
                        @Override
                        public void onInitializeAccessibilityNodeInfo(@NonNull View host,
                                @NonNull AccessibilityNodeInfoCompat info) {
                            super.onInitializeAccessibilityNodeInfo(host, info);
                            info.setChecked(true);
                        }
                    };
                }
            }
        );
        testRecyclerViewWithAdapter(
                new Runnable() {
                    @Override
                    public void run() {
                        View itemView = mRecyclerView.getChildAt(0);
                        assertTrue(itemView.createAccessibilityNodeInfo().isChecked());
                    }
                }, new TestAdapter(100), recyclerView);
    }

    private void testCustomAccessibilityDelegate(final AccessibilityDelegateCompat delegateCompat,
            Runnable runnable) throws Throwable {
        testCustomAccessibilityDelegate(delegateCompat, runnable, null);
    }

    private void testCustomAccessibilityDelegate(final AccessibilityDelegateCompat delegateCompat,
            Runnable runnable, final TextViewCreator viewCreator) throws Throwable {
        testCustomAccessibilityDelegateWithAdapter(runnable, new TestAdapter(100) {
                @Override
                public TestViewHolder onCreateViewHolder(@NonNull ViewGroup parent,
                        int viewType) {
                    TestViewHolder vh;
                    if (viewCreator == null) {
                        vh = super.onCreateViewHolder(parent, viewType);
                    } else {
                        TextView textView = viewCreator.createView(parent.getContext());
                        textView.setLayoutParams(new ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.WRAP_CONTENT));
                        textView.setFocusable(true);
                        textView.setBackgroundResource(R.drawable.item_bg);
                        vh = new TestViewHolder(textView);
                    }
                    ViewCompat.setAccessibilityDelegate(vh.itemView, delegateCompat);
                    return vh;
                }
            });
    }

    private void testCustomAccessibilityDelegateWithAdapter(Runnable runnable,
            final TestAdapter adapter) throws Throwable {
        testRecyclerViewWithAdapter(runnable, adapter, new RecyclerView(getActivity()) {
            @Override
            boolean isAccessibilityEnabled() {
                return true;
            }
        });
    }

    private void testRecyclerViewWithAdapter(Runnable runnable,
            final TestAdapter adapter, final RecyclerView recyclerView)
            throws Throwable {
        final int[] layoutStart = new int[] {0};
        final int layoutCount = 5;
        final TestLayoutManager layoutManager = new TestLayoutManager() {
            @Override
            public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
                detachAndScrapAttachedViews(recycler);
                removeAndRecycleScrapInt(recycler);
                layoutRange(recycler, layoutStart[0], layoutStart[0] + layoutCount);
                if (layoutLatch != null) {
                    layoutLatch.countDown();
                }
            }
        };
        layoutManager.expectLayouts(1);
        recyclerView.getRecycledViewPool().setMaxRecycledViews(0, 100);
        recyclerView.setItemViewCacheSize(0); // no cache, directly goes to pool
        recyclerView.setLayoutManager(layoutManager);
        setRecyclerView(recyclerView);
        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                recyclerView.setAdapter(adapter);
            }
        });
        layoutManager.waitForLayout(1);
        mActivityRule.runOnUiThread(runnable);
    }

    private interface TextViewCreator {
        TextView createView(Context context);
    }

    private boolean accessibiltyDelegateIsItemDelegate(RecyclerView rc, View item) {
        return rc.getCompatAccessibilityDelegate().getItemDelegate()
                .equals(ViewCompat.getAccessibilityDelegate(item));
    }
}
