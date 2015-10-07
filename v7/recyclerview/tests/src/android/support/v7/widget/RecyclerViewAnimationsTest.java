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

import android.graphics.Rect;
import android.os.Debug;
import android.support.v4.view.ViewCompat;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Tests for {@link SimpleItemAnimator} API.
 */
public class RecyclerViewAnimationsTest extends BaseRecyclerViewAnimationsTest {

    final List<TestViewHolder> recycledVHs = new ArrayList<>();

    public void testDontLayoutReusedViewWithoutPredictive() throws Throwable {
        reuseHiddenViewTest(new ReuseTestCallback() {
            @Override
            public void postSetup(List<TestViewHolder> recycledList,
                    final TestViewHolder target) throws Throwable {
                LoggingItemAnimator itemAnimator = (LoggingItemAnimator) mRecyclerView
                        .getItemAnimator();
                itemAnimator.reset();
                mLayoutManager.mOnLayoutCallbacks = new OnLayoutCallbacks() {
                    @Override
                    void beforePreLayout(RecyclerView.Recycler recycler,
                            AnimationLayoutManager lm, RecyclerView.State state) {
                        fail("pre layout is not expected");
                    }

                    @Override
                    void beforePostLayout(RecyclerView.Recycler recycler,
                            AnimationLayoutManager layoutManager,
                            RecyclerView.State state) {
                        mLayoutItemCount = 7;
                        View targetView = recycler
                                .getViewForPosition(target.getAdapterPosition());
                        assertSame(targetView, target.itemView);
                        super.beforePostLayout(recycler, layoutManager, state);
                    }

                    @Override
                    void afterPostLayout(RecyclerView.Recycler recycler,
                            AnimationLayoutManager layoutManager,
                            RecyclerView.State state) {
                        super.afterPostLayout(recycler, layoutManager, state);
                        assertNull("test sanity. this view should not be re-laid out in post "
                                + "layout", target.itemView.getParent());
                    }
                };
                mLayoutManager.expectLayouts(1);
                mLayoutManager.requestSimpleAnimationsInNextLayout();
                requestLayoutOnUIThread(mRecyclerView);
                mLayoutManager.waitForLayout(2);
                checkForMainThreadException();
                assertFalse(itemAnimator.contains(target, itemAnimator.mAnimatePersistenceList));
                assertFalse(itemAnimator.contains(target, itemAnimator.mAnimateChangeList));
                assertFalse(itemAnimator.contains(target, itemAnimator.mAnimateAppearanceList));
                // This is a LayoutManager problem if it asked for the view but didn't properly
                // lay it out. It will move to disappearance
                assertTrue(itemAnimator.contains(target, itemAnimator.mAnimateDisappearanceList));
                waitForAnimations(5);
                assertTrue(recycledVHs.contains(target));
            }
        });
    }

    public void testDontLayoutReusedViewWithPredictive() throws Throwable {
        reuseHiddenViewTest(new ReuseTestCallback() {
            @Override
            public void postSetup(List<TestViewHolder> recycledList,
                    final TestViewHolder target) throws Throwable {
                LoggingItemAnimator itemAnimator = (LoggingItemAnimator) mRecyclerView
                        .getItemAnimator();
                itemAnimator.reset();
                mLayoutManager.mOnLayoutCallbacks = new OnLayoutCallbacks() {
                    @Override
                    void beforePreLayout(RecyclerView.Recycler recycler,
                            AnimationLayoutManager lm, RecyclerView.State state) {
                        mLayoutItemCount = 9;
                        super.beforePreLayout(recycler, lm, state);
                    }

                    @Override
                    void beforePostLayout(RecyclerView.Recycler recycler,
                            AnimationLayoutManager layoutManager,
                            RecyclerView.State state) {
                        mLayoutItemCount = 7;
                        super.beforePostLayout(recycler, layoutManager, state);
                    }

                    @Override
                    void afterPostLayout(RecyclerView.Recycler recycler,
                            AnimationLayoutManager layoutManager,
                            RecyclerView.State state) {
                        super.afterPostLayout(recycler, layoutManager, state);
                        assertNull("test sanity. this view should not be re-laid out in post "
                                + "layout", target.itemView.getParent());
                    }
                };
                mLayoutManager.expectLayouts(2);
                mTestAdapter.deleteAndNotify(1, 1);
                mLayoutManager.waitForLayout(2);
                checkForMainThreadException();
                assertFalse(itemAnimator.contains(target, itemAnimator.mAnimatePersistenceList));
                assertFalse(itemAnimator.contains(target, itemAnimator.mAnimateChangeList));
                assertFalse(itemAnimator.contains(target, itemAnimator.mAnimateAppearanceList));
                // This is a LayoutManager problem if it asked for the view but didn't properly
                // lay it out. It will move to disappearance.
                assertTrue(itemAnimator.contains(target, itemAnimator.mAnimateDisappearanceList));
                waitForAnimations(5);
                assertTrue(recycledVHs.contains(target));
            }
        });
    }

    public void testReuseHiddenViewWithoutPredictive() throws Throwable {
        reuseHiddenViewTest(new ReuseTestCallback() {
            @Override
            public void postSetup(List<TestViewHolder> recycledList,
                    TestViewHolder target) throws Throwable {
                LoggingItemAnimator itemAnimator = (LoggingItemAnimator) mRecyclerView
                        .getItemAnimator();
                itemAnimator.reset();
                mLayoutManager.expectLayouts(1);
                mLayoutManager.requestSimpleAnimationsInNextLayout();
                mLayoutManager.mOnLayoutCallbacks.mLayoutItemCount = 9;
                requestLayoutOnUIThread(mRecyclerView);
                mLayoutManager.waitForLayout(2);
                waitForAnimations(5);
                assertTrue(itemAnimator.contains(target, itemAnimator.mAnimatePersistenceList));
                assertFalse(itemAnimator.contains(target, itemAnimator.mAnimateChangeList));
                assertFalse(itemAnimator.contains(target, itemAnimator.mAnimateAppearanceList));
                assertFalse(itemAnimator.contains(target, itemAnimator.mAnimateDisappearanceList));
                assertFalse(recycledVHs.contains(target));
            }
        });
    }

    public void testReuseHiddenViewWithoutAnimations() throws Throwable {
        reuseHiddenViewTest(new ReuseTestCallback() {
            @Override
            public void postSetup(List<TestViewHolder> recycledList,
                    TestViewHolder target) throws Throwable {
                LoggingItemAnimator itemAnimator = (LoggingItemAnimator) mRecyclerView
                        .getItemAnimator();
                itemAnimator.reset();
                mLayoutManager.expectLayouts(1);
                mLayoutManager.mOnLayoutCallbacks.mLayoutItemCount = 9;
                requestLayoutOnUIThread(mRecyclerView);
                mLayoutManager.waitForLayout(2);
                waitForAnimations(5);
                assertFalse(itemAnimator.contains(target, itemAnimator.mAnimatePersistenceList));
                assertFalse(itemAnimator.contains(target, itemAnimator.mAnimateChangeList));
                assertFalse(itemAnimator.contains(target, itemAnimator.mAnimateAppearanceList));
                assertFalse(itemAnimator.contains(target, itemAnimator.mAnimateDisappearanceList));
                assertFalse(recycledVHs.contains(target));
            }
        });
    }

    public void testReuseHiddenViewWithPredictive() throws Throwable {
        reuseHiddenViewTest(new ReuseTestCallback() {
            @Override
            public void postSetup(List<TestViewHolder> recycledList,
                    TestViewHolder target) throws Throwable {
                // it should move to change scrap and then show up from there
                LoggingItemAnimator itemAnimator = (LoggingItemAnimator) mRecyclerView
                        .getItemAnimator();
                itemAnimator.reset();
                mLayoutManager.expectLayouts(2);
                mTestAdapter.deleteAndNotify(2, 1);
                mLayoutManager.waitForLayout(2);
                waitForAnimations(5);
                // This LM does not layout the additional item so it does predictive wrong.
                // We should still handle it and animate persistence for this item
                assertTrue(itemAnimator.contains(target, itemAnimator.mAnimatePersistenceList));
                assertFalse(itemAnimator.contains(target, itemAnimator.mAnimateChangeList));
                assertFalse(itemAnimator.contains(target, itemAnimator.mAnimateAppearanceList));
                assertFalse(itemAnimator.contains(target, itemAnimator.mAnimateDisappearanceList));
                assertTrue(itemAnimator.mMoveVHs.contains(target));
                assertFalse(recycledVHs.contains(target));
            }
        });
    }

    public void testReuseHiddenViewWithProperPredictive() throws Throwable {
        reuseHiddenViewTest(new ReuseTestCallback() {
            @Override
            public void postSetup(List<TestViewHolder> recycledList,
                    TestViewHolder target) throws Throwable {
                // it should move to change scrap and then show up from there
                LoggingItemAnimator itemAnimator = (LoggingItemAnimator) mRecyclerView
                        .getItemAnimator();
                itemAnimator.reset();
                mLayoutManager.mOnLayoutCallbacks = new OnLayoutCallbacks() {
                    @Override
                    void beforePreLayout(RecyclerView.Recycler recycler,
                            AnimationLayoutManager lm, RecyclerView.State state) {
                        mLayoutItemCount = 9;
                        super.beforePreLayout(recycler, lm, state);
                    }

                    @Override
                    void afterPreLayout(RecyclerView.Recycler recycler,
                            AnimationLayoutManager layoutManager,
                            RecyclerView.State state) {
                        mLayoutItemCount = 8;
                        super.afterPreLayout(recycler, layoutManager, state);
                    }
                };

                mLayoutManager.expectLayouts(2);
                mTestAdapter.deleteAndNotify(2, 1);
                mLayoutManager.waitForLayout(2);
                waitForAnimations(5);
                // This LM implements predictive animations properly by requesting target view
                // in pre-layout.
                assertTrue(itemAnimator.contains(target, itemAnimator.mAnimatePersistenceList));
                assertFalse(itemAnimator.contains(target, itemAnimator.mAnimateChangeList));
                assertFalse(itemAnimator.contains(target, itemAnimator.mAnimateAppearanceList));
                assertFalse(itemAnimator.contains(target, itemAnimator.mAnimateDisappearanceList));
                assertTrue(itemAnimator.mMoveVHs.contains(target));
                assertFalse(recycledVHs.contains(target));
            }
        });
    }

    public void testDontReuseHiddenViewOnInvalidate() throws Throwable {
        reuseHiddenViewTest(new ReuseTestCallback() {
            @Override
            public void postSetup(List<TestViewHolder> recycledList,
                    TestViewHolder target) throws Throwable {
                // it should move to change scrap and then show up from there
                LoggingItemAnimator itemAnimator = (LoggingItemAnimator) mRecyclerView
                        .getItemAnimator();
                itemAnimator.reset();
                mLayoutManager.expectLayouts(1);
                mTestAdapter.dispatchDataSetChanged();
                mLayoutManager.waitForLayout(2);
                waitForAnimations(5);
                assertFalse(mRecyclerView.getItemAnimator().isRunning());
                assertFalse(itemAnimator.contains(target, itemAnimator.mAnimatePersistenceList));
                assertFalse(itemAnimator.contains(target, itemAnimator.mAnimateChangeList));
                assertFalse(itemAnimator.contains(target, itemAnimator.mAnimateAppearanceList));
                assertFalse(itemAnimator.contains(target, itemAnimator.mAnimateDisappearanceList));
                assertTrue(recycledVHs.contains(target));
            }
        });
    }

    public void testDontReuseOnTypeChange() throws Throwable {
        reuseHiddenViewTest(new ReuseTestCallback() {
            @Override
            public void postSetup(List<TestViewHolder> recycledList,
                    TestViewHolder target) throws Throwable {
                // it should move to change scrap and then show up from there
                LoggingItemAnimator itemAnimator = (LoggingItemAnimator) mRecyclerView
                        .getItemAnimator();
                itemAnimator.reset();
                mLayoutManager.expectLayouts(1);
                target.mBoundItem.mType += 2;
                mLayoutManager.mOnLayoutCallbacks.mLayoutItemCount = 9;
                mTestAdapter.changeAndNotify(target.getAdapterPosition(), 1);
                requestLayoutOnUIThread(mRecyclerView);
                mLayoutManager.waitForLayout(2);

                assertTrue(itemAnimator.mChangeOldVHs.contains(target));
                assertFalse(itemAnimator.contains(target, itemAnimator.mAnimatePersistenceList));
                assertFalse(itemAnimator.contains(target, itemAnimator.mAnimateAppearanceList));
                assertFalse(itemAnimator.contains(target, itemAnimator.mAnimateDisappearanceList));
                assertTrue(mRecyclerView.mChildHelper.isHidden(target.itemView));
                assertFalse(recycledVHs.contains(target));
                waitForAnimations(5);
                assertTrue(recycledVHs.contains(target));
            }
        });
    }

    interface ReuseTestCallback {

        void postSetup(List<TestViewHolder> recycledList, TestViewHolder target) throws Throwable;
    }

    @Override
    protected RecyclerView.ItemAnimator createItemAnimator() {
        return new LoggingItemAnimator();
    }

    public void reuseHiddenViewTest(ReuseTestCallback callback) throws Throwable {
        TestAdapter adapter = new TestAdapter(10) {
            @Override
            public void onViewRecycled(TestViewHolder holder) {
                super.onViewRecycled(holder);
                recycledVHs.add(holder);
            }
        };
        setupBasic(10, 0, 10, adapter);
        mRecyclerView.setItemViewCacheSize(0);
        TestViewHolder target = (TestViewHolder) mRecyclerView.findViewHolderForAdapterPosition(9);
        mRecyclerView.getItemAnimator().setAddDuration(1000);
        mRecyclerView.getItemAnimator().setRemoveDuration(1000);
        mRecyclerView.getItemAnimator().setChangeDuration(1000);
        mRecyclerView.getItemAnimator().setMoveDuration(1000);
        mLayoutManager.mOnLayoutCallbacks.mLayoutItemCount = 8;
        mLayoutManager.expectLayouts(2);
        adapter.deleteAndNotify(2, 1);
        mLayoutManager.waitForLayout(2);
        // test sanity, make sure target is hidden now
        assertTrue("test sanity", mRecyclerView.mChildHelper.isHidden(target.itemView));
        callback.postSetup(recycledVHs, target);
        // TODO TEST ITEM INVALIDATION OR TYPE CHANGE IN BETWEEN
        // TODO TEST ITEM IS RECEIVED FROM RECYCLER BUT NOT RE-ADDED
        // TODO TEST ITEM ANIMATOR IS CALLED TO GET NEW INFORMATION ABOUT LOCATION

    }

    public void testDetachBeforeAnimations() throws Throwable {
        setupBasic(10, 0, 5);
        final RecyclerView rv = mRecyclerView;
        waitForAnimations(2);
        final DefaultItemAnimator animator = new DefaultItemAnimator() {
            @Override
            public void runPendingAnimations() {
                super.runPendingAnimations();
            }
        };
        rv.setItemAnimator(animator);
        mLayoutManager.expectLayouts(2);
        mTestAdapter.deleteAndNotify(3, 4);
        mLayoutManager.waitForLayout(2);
        removeRecyclerView();
        assertNull("test sanity check RV should be removed", rv.getParent());
        assertEquals("no views should be hidden", 0, rv.mChildHelper.mHiddenViews.size());
        assertFalse("there should not be any animations running", animator.isRunning());
    }

    public void testMoveDeleted() throws Throwable {
        setupBasic(4, 0, 3);
        waitForAnimations(2);
        final View[] targetChild = new View[1];
        final LoggingItemAnimator animator = new LoggingItemAnimator();
        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                mRecyclerView.setItemAnimator(animator);
                targetChild[0] = mRecyclerView.getChildAt(1);
            }
        });

        assertNotNull("test sanity", targetChild);
        mLayoutManager.expectLayouts(1);
        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                mRecyclerView.addItemDecoration(new RecyclerView.ItemDecoration() {
                    @Override
                    public void getItemOffsets(Rect outRect, View view, RecyclerView parent,
                            RecyclerView.State state) {
                        if (view == targetChild[0]) {
                            outRect.set(10, 20, 30, 40);
                        } else {
                            outRect.set(0, 0, 0, 0);
                        }
                    }
                });
            }
        });
        mLayoutManager.waitForLayout(1);

        // now delete that item.
        mLayoutManager.expectLayouts(2);
        RecyclerView.ViewHolder targetVH = mRecyclerView.getChildViewHolder(targetChild[0]);
        targetChild[0] = null;
        mTestAdapter.deleteAndNotify(1, 1);
        mLayoutManager.waitForLayout(2);
        assertFalse("if deleted view moves, it should not be in move animations",
                animator.mMoveVHs.contains(targetVH));
        assertEquals("only 1 item is deleted", 1, animator.mRemoveVHs.size());
        assertTrue("the target view is removed", animator.mRemoveVHs.contains(targetVH
        ));
    }

    private void runTestImportantForAccessibilityWhileDeteling(
            final int boundImportantForAccessibility,
            final int expectedImportantForAccessibility) throws Throwable {
        // Adapter binding the item to the initial accessibility option.
        // RecyclerView is expected to change it to 'expectedImportantForAccessibility'.
        TestAdapter adapter = new TestAdapter(1) {
            @Override
            public void onBindViewHolder(TestViewHolder holder, int position) {
                super.onBindViewHolder(holder, position);
                ViewCompat.setImportantForAccessibility(
                        holder.itemView, boundImportantForAccessibility);
            }
        };

        // Set up with 1 item.
        setupBasic(1, 0, 1, adapter);
        waitForAnimations(2);
        final View[] targetChild = new View[1];
        final LoggingItemAnimator animator = new LoggingItemAnimator();
        animator.setRemoveDuration(500);
        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                mRecyclerView.setItemAnimator(animator);
                targetChild[0] = mRecyclerView.getChildAt(0);
                assertEquals(
                        expectedImportantForAccessibility,
                        ViewCompat.getImportantForAccessibility(targetChild[0]));
            }
        });

        assertNotNull("test sanity", targetChild[0]);

        // now delete that item.
        mLayoutManager.expectLayouts(2);
        mTestAdapter.deleteAndNotify(0, 1);

        mLayoutManager.waitForLayout(2);

        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                // The view is still a child of mRecyclerView, and is invisible for accessibility.
                assertTrue(targetChild[0].getParent() == mRecyclerView);
                assertEquals(
                        ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS,
                        ViewCompat.getImportantForAccessibility(targetChild[0]));
            }
        });

        waitForAnimations(2);

        // Delete animation is now complete.
        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                // The view is in recycled state, and back to the expected accessibility.
                assertTrue(targetChild[0].getParent() == null);
                assertEquals(
                        expectedImportantForAccessibility,
                        ViewCompat.getImportantForAccessibility(targetChild[0]));
            }
        });

        // Add 1 element, which should use same view.
        mLayoutManager.expectLayouts(2);
        mTestAdapter.addAndNotify(1);
        mLayoutManager.waitForLayout(2);

        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                // The view should be reused, and have the expected accessibility.
                assertTrue(
                        "the item must be reused", targetChild[0] == mRecyclerView.getChildAt(0));
                assertEquals(
                        expectedImportantForAccessibility,
                        ViewCompat.getImportantForAccessibility(targetChild[0]));
            }
        });
    }

    public void testImportantForAccessibilityWhileDetelingAuto() throws Throwable {
        runTestImportantForAccessibilityWhileDeteling(
                ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_AUTO,
                ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_YES);
    }

    public void testImportantForAccessibilityWhileDetelingNo() throws Throwable {
        runTestImportantForAccessibilityWhileDeteling(
                ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_NO,
                ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_NO);
    }

    public void testImportantForAccessibilityWhileDetelingNoHideDescandants() throws Throwable {
        runTestImportantForAccessibilityWhileDeteling(
                ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS,
                ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);
    }

    public void testImportantForAccessibilityWhileDetelingYes() throws Throwable {
        runTestImportantForAccessibilityWhileDeteling(
                ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_YES,
                ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_YES);
    }

    public void testPreLayoutPositionCleanup() throws Throwable {
        setupBasic(4, 0, 4);
        mLayoutManager.expectLayouts(2);
        mLayoutManager.mOnLayoutCallbacks = new OnLayoutCallbacks() {
            @Override
            void beforePreLayout(RecyclerView.Recycler recycler,
                    AnimationLayoutManager lm, RecyclerView.State state) {
                mLayoutMin = 0;
                mLayoutItemCount = 3;
            }

            @Override
            void beforePostLayout(RecyclerView.Recycler recycler,
                    AnimationLayoutManager layoutManager,
                    RecyclerView.State state) {
                mLayoutMin = 0;
                mLayoutItemCount = 4;
            }
        };
        mTestAdapter.addAndNotify(0, 1);
        mLayoutManager.waitForLayout(2);


    }

    public void testAddRemoveSamePass() throws Throwable {
        final List<RecyclerView.ViewHolder> mRecycledViews
                = new ArrayList<RecyclerView.ViewHolder>();
        TestAdapter adapter = new TestAdapter(50) {
            @Override
            public void onViewRecycled(TestViewHolder holder) {
                super.onViewRecycled(holder);
                mRecycledViews.add(holder);
            }
        };
        adapter.setHasStableIds(true);
        setupBasic(50, 3, 5, adapter);
        mRecyclerView.setItemViewCacheSize(0);
        final ArrayList<RecyclerView.ViewHolder> addVH
                = new ArrayList<RecyclerView.ViewHolder>();
        final ArrayList<RecyclerView.ViewHolder> removeVH
                = new ArrayList<RecyclerView.ViewHolder>();

        final ArrayList<RecyclerView.ViewHolder> moveVH
                = new ArrayList<RecyclerView.ViewHolder>();

        final View[] testView = new View[1];
        mRecyclerView.setItemAnimator(new DefaultItemAnimator() {
            @Override
            public boolean animateAdd(RecyclerView.ViewHolder holder) {
                addVH.add(holder);
                return true;
            }

            @Override
            public boolean animateRemove(RecyclerView.ViewHolder holder) {
                removeVH.add(holder);
                return true;
            }

            @Override
            public boolean animateMove(RecyclerView.ViewHolder holder, int fromX, int fromY,
                    int toX, int toY) {
                moveVH.add(holder);
                return true;
            }
        });
        mLayoutManager.mOnLayoutCallbacks = new OnLayoutCallbacks() {
            @Override
            void afterPreLayout(RecyclerView.Recycler recycler,
                    AnimationLayoutManager layoutManager,
                    RecyclerView.State state) {
                super.afterPreLayout(recycler, layoutManager, state);
                testView[0] = recycler.getViewForPosition(45);
                testView[0].measure(View.MeasureSpec.makeMeasureSpec(100, View.MeasureSpec.AT_MOST),
                        View.MeasureSpec.makeMeasureSpec(100, View.MeasureSpec.AT_MOST));
                testView[0].layout(10, 10, 10 + testView[0].getMeasuredWidth(),
                        10 + testView[0].getMeasuredHeight());
                layoutManager.addView(testView[0], 4);
            }

            @Override
            void afterPostLayout(RecyclerView.Recycler recycler,
                    AnimationLayoutManager layoutManager,
                    RecyclerView.State state) {
                super.afterPostLayout(recycler, layoutManager, state);
                testView[0].layout(50, 50, 50 + testView[0].getMeasuredWidth(),
                        50 + testView[0].getMeasuredHeight());
                layoutManager.addDisappearingView(testView[0], 4);
            }
        };
        mLayoutManager.mOnLayoutCallbacks.mLayoutMin = 3;
        mLayoutManager.mOnLayoutCallbacks.mLayoutItemCount = 5;
        mRecycledViews.clear();
        mLayoutManager.expectLayouts(2);
        mTestAdapter.deleteAndNotify(3, 1);
        mLayoutManager.waitForLayout(2);

        for (RecyclerView.ViewHolder vh : addVH) {
            assertNotSame("add-remove item should not animate add", testView[0], vh.itemView);
        }
        for (RecyclerView.ViewHolder vh : moveVH) {
            assertNotSame("add-remove item should not animate move", testView[0], vh.itemView);
        }
        for (RecyclerView.ViewHolder vh : removeVH) {
            assertNotSame("add-remove item should not animate remove", testView[0], vh.itemView);
        }
        boolean found = false;
        for (RecyclerView.ViewHolder vh : mRecycledViews) {
            found |= vh.itemView == testView[0];
        }
        assertTrue("added-removed view should be recycled", found);
    }

    public void testTmpRemoveMe() throws Throwable {
        changeAnimTest(false, false, true, false);
    }

    public void testChangeAnimations() throws Throwable {
        final boolean[] booleans = {true, false};
        for (boolean supportsChange : booleans) {
            for (boolean changeType : booleans) {
                for (boolean hasStableIds : booleans) {
                    for (boolean deleteSomeItems : booleans) {
                        changeAnimTest(supportsChange, changeType, hasStableIds, deleteSomeItems);
                    }
                    removeRecyclerView();
                }
            }
        }
    }

    public void changeAnimTest(final boolean supportsChangeAnim, final boolean changeType,
            final boolean hasStableIds, final boolean deleteSomeItems) throws Throwable {
        final int changedIndex = 3;
        final int defaultType = 1;
        final AtomicInteger changedIndexNewType = new AtomicInteger(defaultType);
        final String logPrefix = "supportsChangeAnim:" + supportsChangeAnim +
                ", change view type:" + changeType +
                ", has stable ids:" + hasStableIds +
                ", delete some items:" + deleteSomeItems;
        TestAdapter testAdapter = new TestAdapter(10) {
            @Override
            public int getItemViewType(int position) {
                return position == changedIndex ? changedIndexNewType.get() : defaultType;
            }

            @Override
            public TestViewHolder onCreateViewHolder(ViewGroup parent,
                    int viewType) {
                TestViewHolder vh = super.onCreateViewHolder(parent, viewType);
                if (DEBUG) {
                    Log.d(TAG, logPrefix + " onCreateVH" + vh.toString());
                }
                return vh;
            }

            @Override
            public void onBindViewHolder(TestViewHolder holder,
                    int position) {
                super.onBindViewHolder(holder, position);
                if (DEBUG) {
                    Log.d(TAG, logPrefix + " onBind to " + position + "" + holder.toString());
                }
            }
        };
        testAdapter.setHasStableIds(hasStableIds);
        setupBasic(testAdapter.getItemCount(), 0, 10, testAdapter);
        ((SimpleItemAnimator) mRecyclerView.getItemAnimator()).setSupportsChangeAnimations(
                supportsChangeAnim);

        final RecyclerView.ViewHolder toBeChangedVH =
                mRecyclerView.findViewHolderForLayoutPosition(changedIndex);
        mLayoutManager.mOnLayoutCallbacks = new OnLayoutCallbacks() {
            @Override
            void afterPreLayout(RecyclerView.Recycler recycler,
                    AnimationLayoutManager layoutManager,
                    RecyclerView.State state) {
                RecyclerView.ViewHolder vh = mRecyclerView.findViewHolderForLayoutPosition(
                        changedIndex);
                assertTrue(logPrefix + " changed view holder should have correct flag"
                        , vh.isUpdated());
            }

            @Override
            void afterPostLayout(RecyclerView.Recycler recycler,
                    AnimationLayoutManager layoutManager, RecyclerView.State state) {
                RecyclerView.ViewHolder vh = mRecyclerView.findViewHolderForLayoutPosition(
                        changedIndex);
                if (supportsChangeAnim) {
                    assertNotSame(logPrefix + "a new VH should be given if change is supported",
                            toBeChangedVH, vh);
                } else if (!changeType && hasStableIds) {
                    assertSame(logPrefix + "if change animations are not supported but we have "
                            + "stable ids, same view holder should be returned", toBeChangedVH, vh);
                }
                super.beforePostLayout(recycler, layoutManager, state);
            }
        };
        mLayoutManager.expectLayouts(1);
        if (changeType) {
            changedIndexNewType.set(defaultType + 1);
        }
        if (deleteSomeItems) {
            runTestOnUiThread(new Runnable() {
                @Override
                public void run() {
                    try {
                        mTestAdapter.deleteAndNotify(changedIndex + 2, 1);
                        mTestAdapter.notifyItemChanged(3);
                    } catch (Throwable throwable) {
                        throwable.printStackTrace();
                    }

                }
            });
        } else {
            mTestAdapter.changeAndNotify(3, 1);
        }

        mLayoutManager.waitForLayout(2);
    }

    private static boolean listEquals(List list1, List list2) {
        if (list1.size() != list2.size()) {
            return false;
        }
        for (int i = 0; i < list1.size(); i++) {
            if (!list1.get(i).equals(list2.get(i))) {
                return false;
            }
        }
        return true;
    }

    private void testChangeWithPayload(final boolean supportsChangeAnim,
            Object[][] notifyPayloads, Object[][] expectedPayloadsInOnBind)
            throws Throwable {
        final List<Object> expectedPayloads = new ArrayList<Object>();
        final int changedIndex = 3;
        TestAdapter testAdapter = new TestAdapter(10) {
            @Override
            public int getItemViewType(int position) {
                return 1;
            }

            @Override
            public TestViewHolder onCreateViewHolder(ViewGroup parent,
                    int viewType) {
                TestViewHolder vh = super.onCreateViewHolder(parent, viewType);
                if (DEBUG) {
                    Log.d(TAG, " onCreateVH" + vh.toString());
                }
                return vh;
            }

            @Override
            public void onBindViewHolder(TestViewHolder holder,
                    int position, List<Object> payloads) {
                super.onBindViewHolder(holder, position);
                if (DEBUG) {
                    Log.d(TAG, " onBind to " + position + "" + holder.toString());
                }
                assertTrue(listEquals(payloads, expectedPayloads));
            }
        };
        testAdapter.setHasStableIds(false);
        setupBasic(testAdapter.getItemCount(), 0, 10, testAdapter);
        ((SimpleItemAnimator) mRecyclerView.getItemAnimator()).setSupportsChangeAnimations(
                supportsChangeAnim);

        int numTests = notifyPayloads.length;
        for (int i = 0; i < numTests; i++) {
            mLayoutManager.expectLayouts(1);
            expectedPayloads.clear();
            for (int j = 0; j < expectedPayloadsInOnBind[i].length; j++) {
                expectedPayloads.add(expectedPayloadsInOnBind[i][j]);
            }
            final Object[] payloadsToSend = notifyPayloads[i];
            runTestOnUiThread(new Runnable() {
                @Override
                public void run() {
                    for (int j = 0; j < payloadsToSend.length; j++) {
                        mTestAdapter.notifyItemChanged(changedIndex, payloadsToSend[j]);
                    }
                }
            });
            mLayoutManager.waitForLayout(2);
            checkForMainThreadException();
        }
    }

    public void testCrossFadingChangeAnimationWithPayload() throws Throwable {
        // for crossfading change animation,  will receive EMPTY payload in onBindViewHolder
        testChangeWithPayload(true,
                new Object[][]{
                        new Object[]{"abc"},
                        new Object[]{"abc", null, "cdf"},
                        new Object[]{"abc", null},
                        new Object[]{null, "abc"},
                        new Object[]{"abc", "cdf"}
                },
                new Object[][]{
                        new Object[0],
                        new Object[0],
                        new Object[0],
                        new Object[0],
                        new Object[0]
                });
    }

    public void testNoChangeAnimationWithPayload() throws Throwable {
        // for Change Animation disabled, payload should match the payloads unless
        // null payload is fired.
        testChangeWithPayload(false,
                new Object[][]{
                        new Object[]{"abc"},
                        new Object[]{"abc", null, "cdf"},
                        new Object[]{"abc", null},
                        new Object[]{null, "abc"},
                        new Object[]{"abc", "cdf"}
                },
                new Object[][]{
                        new Object[]{"abc"},
                        new Object[0],
                        new Object[0],
                        new Object[0],
                        new Object[]{"abc", "cdf"}
                });
    }

    public void testRecycleDuringAnimations() throws Throwable {
        final AtomicInteger childCount = new AtomicInteger(0);
        final TestAdapter adapter = new TestAdapter(1000) {
            @Override
            public TestViewHolder onCreateViewHolder(ViewGroup parent,
                    int viewType) {
                childCount.incrementAndGet();
                return super.onCreateViewHolder(parent, viewType);
            }
        };
        setupBasic(1000, 10, 20, adapter);
        mLayoutManager.mOnLayoutCallbacks.mLayoutMin = 10;
        mLayoutManager.mOnLayoutCallbacks.mLayoutItemCount = 20;

        mRecyclerView.setRecycledViewPool(new RecyclerView.RecycledViewPool() {
            @Override
            public void putRecycledView(RecyclerView.ViewHolder scrap) {
                super.putRecycledView(scrap);
                childCount.decrementAndGet();
            }

            @Override
            public RecyclerView.ViewHolder getRecycledView(int viewType) {
                final RecyclerView.ViewHolder recycledView = super.getRecycledView(viewType);
                if (recycledView != null) {
                    childCount.incrementAndGet();
                }
                return recycledView;
            }
        });

        // now keep adding children to trigger more children being created etc.
        for (int i = 0; i < 100; i++) {
            adapter.addAndNotify(15, 1);
            Thread.sleep(50);
        }
        getInstrumentation().waitForIdleSync();
        waitForAnimations(2);
        assertEquals("Children count should add up", childCount.get(),
                mRecyclerView.getChildCount() + mRecyclerView.mRecycler.mCachedViews.size());
    }

    public void testNotifyDataSetChanged() throws Throwable {
        setupBasic(10, 3, 4);
        int layoutCount = mLayoutManager.mTotalLayoutCount;
        mLayoutManager.expectLayouts(1);
        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    mTestAdapter.deleteAndNotify(4, 1);
                    mTestAdapter.dispatchDataSetChanged();
                } catch (Throwable throwable) {
                    throwable.printStackTrace();
                }

            }
        });
        mLayoutManager.waitForLayout(2);
        getInstrumentation().waitForIdleSync();
        assertEquals("on notify data set changed, predictive animations should not run",
                layoutCount + 1, mLayoutManager.mTotalLayoutCount);
        mLayoutManager.expectLayouts(2);
        mTestAdapter.addAndNotify(4, 2);
        // make sure animations recover
        mLayoutManager.waitForLayout(2);
    }

    public void testStableIdNotifyDataSetChanged() throws Throwable {
        final int itemCount = 20;
        List<Item> initialSet = new ArrayList<Item>();
        final TestAdapter adapter = new TestAdapter(itemCount) {
            @Override
            public long getItemId(int position) {
                return mItems.get(position).mId;
            }
        };
        adapter.setHasStableIds(true);
        initialSet.addAll(adapter.mItems);
        positionStatesTest(itemCount, 5, 5, adapter, new AdapterOps() {
                    @Override
                    void onRun(TestAdapter testAdapter) throws Throwable {
                        Item item5 = adapter.mItems.get(5);
                        Item item6 = adapter.mItems.get(6);
                        item5.mAdapterIndex = 6;
                        item6.mAdapterIndex = 5;
                        adapter.mItems.remove(5);
                        adapter.mItems.add(6, item5);
                        adapter.dispatchDataSetChanged();
                        //hacky, we support only 1 layout pass
                        mLayoutManager.layoutLatch.countDown();
                    }
                }, PositionConstraint.scrap(6, -1, 5), PositionConstraint.scrap(5, -1, 6),
                PositionConstraint.scrap(7, -1, 7), PositionConstraint.scrap(8, -1, 8),
                PositionConstraint.scrap(9, -1, 9));
        // now mix items.
    }


    public void testGetItemForDeletedView() throws Throwable {
        getItemForDeletedViewTest(false);
        getItemForDeletedViewTest(true);
    }

    public void getItemForDeletedViewTest(boolean stableIds) throws Throwable {
        final Set<Integer> itemViewTypeQueries = new HashSet<Integer>();
        final Set<Integer> itemIdQueries = new HashSet<Integer>();
        TestAdapter adapter = new TestAdapter(10) {
            @Override
            public int getItemViewType(int position) {
                itemViewTypeQueries.add(position);
                return super.getItemViewType(position);
            }

            @Override
            public long getItemId(int position) {
                itemIdQueries.add(position);
                return mItems.get(position).mId;
            }
        };
        adapter.setHasStableIds(stableIds);
        setupBasic(10, 0, 10, adapter);
        assertEquals("getItemViewType for all items should be called", 10,
                itemViewTypeQueries.size());
        if (adapter.hasStableIds()) {
            assertEquals("getItemId should be called when adapter has stable ids", 10,
                    itemIdQueries.size());
        } else {
            assertEquals("getItemId should not be called when adapter does not have stable ids", 0,
                    itemIdQueries.size());
        }
        itemViewTypeQueries.clear();
        itemIdQueries.clear();
        mLayoutManager.expectLayouts(2);
        // delete last two
        final int deleteStart = 8;
        final int deleteCount = adapter.getItemCount() - deleteStart;
        adapter.deleteAndNotify(deleteStart, deleteCount);
        mLayoutManager.waitForLayout(2);
        for (int i = 0; i < deleteStart; i++) {
            assertTrue("getItemViewType for existing item " + i + " should be called",
                    itemViewTypeQueries.contains(i));
            if (adapter.hasStableIds()) {
                assertTrue("getItemId for existing item " + i
                                + " should be called when adapter has stable ids",
                        itemIdQueries.contains(i));
            }
        }
        for (int i = deleteStart; i < deleteStart + deleteCount; i++) {
            assertFalse("getItemViewType for deleted item " + i + " SHOULD NOT be called",
                    itemViewTypeQueries.contains(i));
            if (adapter.hasStableIds()) {
                assertFalse("getItemId for deleted item " + i + " SHOULD NOT be called",
                        itemIdQueries.contains(i));
            }
        }
    }

    public void testDeleteInvisibleMultiStep() throws Throwable {
        setupBasic(1000, 1, 7);
        mLayoutManager.mOnLayoutCallbacks.mLayoutMin = 1;
        mLayoutManager.mOnLayoutCallbacks.mLayoutItemCount = 7;
        mLayoutManager.expectLayouts(1);
        // try to trigger race conditions
        int targetItemCount = mTestAdapter.getItemCount();
        for (int i = 0; i < 100; i++) {
            mTestAdapter.deleteAndNotify(new int[]{0, 1}, new int[]{7, 1});
            checkForMainThreadException();
            targetItemCount -= 2;
        }
        // wait until main thread runnables are consumed
        while (targetItemCount != mTestAdapter.getItemCount()) {
            Thread.sleep(100);
        }
        mLayoutManager.waitForLayout(2);
    }

    public void testAddManyMultiStep() throws Throwable {
        setupBasic(10, 1, 7);
        mLayoutManager.mOnLayoutCallbacks.mLayoutMin = 1;
        mLayoutManager.mOnLayoutCallbacks.mLayoutItemCount = 7;
        mLayoutManager.expectLayouts(1);
        // try to trigger race conditions
        int targetItemCount = mTestAdapter.getItemCount();
        for (int i = 0; i < 100; i++) {
            checkForMainThreadException();
            mTestAdapter.addAndNotify(0, 1);
            checkForMainThreadException();
            mTestAdapter.addAndNotify(7, 1);
            targetItemCount += 2;
        }
        checkForMainThreadException();
        // wait until main thread runnables are consumed
        while (targetItemCount != mTestAdapter.getItemCount()) {
            Thread.sleep(100);
            checkForMainThreadException();
        }
        mLayoutManager.waitForLayout(2);
    }

    public void testBasicDelete() throws Throwable {
        setupBasic(10);
        final OnLayoutCallbacks callbacks = new OnLayoutCallbacks() {
            @Override
            public void postDispatchLayout() {
                // verify this only in first layout
                assertEquals("deleted views should still be children of RV",
                        mLayoutManager.getChildCount() + mDeletedViewCount
                        , mRecyclerView.getChildCount());
            }

            @Override
            void afterPreLayout(RecyclerView.Recycler recycler,
                    AnimationLayoutManager layoutManager,
                    RecyclerView.State state) {
                super.afterPreLayout(recycler, layoutManager, state);
                mLayoutItemCount = 3;
                mLayoutMin = 0;
            }
        };
        callbacks.mLayoutItemCount = 10;
        callbacks.setExpectedItemCounts(10, 3);
        mLayoutManager.setOnLayoutCallbacks(callbacks);

        mLayoutManager.expectLayouts(2);
        mTestAdapter.deleteAndNotify(0, 7);
        mLayoutManager.waitForLayout(2);
        callbacks.reset();// when animations end another layout will happen
    }


    public void testAdapterChangeDuringScrolling() throws Throwable {
        setupBasic(10);
        final AtomicInteger onLayoutItemCount = new AtomicInteger(0);
        final AtomicInteger onScrollItemCount = new AtomicInteger(0);

        mLayoutManager.setOnLayoutCallbacks(new OnLayoutCallbacks() {
            @Override
            void onLayoutChildren(RecyclerView.Recycler recycler,
                    AnimationLayoutManager lm, RecyclerView.State state) {
                onLayoutItemCount.set(state.getItemCount());
                super.onLayoutChildren(recycler, lm, state);
            }

            @Override
            public void onScroll(int dx, RecyclerView.Recycler recycler, RecyclerView.State state) {
                onScrollItemCount.set(state.getItemCount());
                super.onScroll(dx, recycler, state);
            }
        });
        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                mTestAdapter.mItems.remove(5);
                mTestAdapter.notifyItemRangeRemoved(5, 1);
                mRecyclerView.scrollBy(0, 100);
                assertTrue("scrolling while there are pending adapter updates should "
                        + "trigger a layout", mLayoutManager.mOnLayoutCallbacks.mLayoutCount > 0);
                assertEquals("scroll by should be called w/ updated adapter count",
                        mTestAdapter.mItems.size(), onScrollItemCount.get());

            }
        });
    }

    public void testNotifyDataSetChangedDuringScroll() throws Throwable {
        setupBasic(10);
        final AtomicInteger onLayoutItemCount = new AtomicInteger(0);
        final AtomicInteger onScrollItemCount = new AtomicInteger(0);

        mLayoutManager.setOnLayoutCallbacks(new OnLayoutCallbacks() {
            @Override
            void onLayoutChildren(RecyclerView.Recycler recycler,
                    AnimationLayoutManager lm, RecyclerView.State state) {
                onLayoutItemCount.set(state.getItemCount());
                super.onLayoutChildren(recycler, lm, state);
            }

            @Override
            public void onScroll(int dx, RecyclerView.Recycler recycler, RecyclerView.State state) {
                onScrollItemCount.set(state.getItemCount());
                super.onScroll(dx, recycler, state);
            }
        });
        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                mTestAdapter.mItems.remove(5);
                mTestAdapter.notifyDataSetChanged();
                mRecyclerView.scrollBy(0, 100);
                assertTrue("scrolling while there are pending adapter updates should "
                        + "trigger a layout", mLayoutManager.mOnLayoutCallbacks.mLayoutCount > 0);
                assertEquals("scroll by should be called w/ updated adapter count",
                        mTestAdapter.mItems.size(), onScrollItemCount.get());

            }
        });
    }

    public void testAddInvisibleAndVisible() throws Throwable {
        setupBasic(10, 1, 7);
        mLayoutManager.expectLayouts(2);
        mLayoutManager.mOnLayoutCallbacks.setExpectedItemCounts(10, 12);
        mTestAdapter.addAndNotify(new int[]{0, 1}, new int[]{7, 1});// add a new item 0 // invisible
        mLayoutManager.waitForLayout(2);
    }

    public void testAddInvisible() throws Throwable {
        setupBasic(10, 1, 7);
        mLayoutManager.expectLayouts(1);
        mLayoutManager.mOnLayoutCallbacks.setExpectedItemCounts(10, 12);
        mTestAdapter.addAndNotify(new int[]{0, 1}, new int[]{8, 1});// add a new item 0
        mLayoutManager.waitForLayout(2);
    }

    public void testBasicAdd() throws Throwable {
        setupBasic(10);
        mLayoutManager.expectLayouts(2);
        setExpectedItemCounts(10, 13);
        mTestAdapter.addAndNotify(2, 3);
        mLayoutManager.waitForLayout(2);
    }

    public void testAppCancelAnimationInDetach() throws Throwable {
        final View[] addedView = new View[2];
        TestAdapter adapter = new TestAdapter(1) {
            @Override
            public void onViewDetachedFromWindow(TestViewHolder holder) {
                if ((addedView[0] == holder.itemView || addedView[1] == holder.itemView)
                        && ViewCompat.hasTransientState(holder.itemView)) {
                    ViewCompat.animate(holder.itemView).cancel();
                }
                super.onViewDetachedFromWindow(holder);
            }
        };
        // original 1 item
        setupBasic(1, 0, 1, adapter);
        mRecyclerView.getItemAnimator().setAddDuration(10000);
        mLayoutManager.expectLayouts(2);
        // add 2 items
        setExpectedItemCounts(1, 3);
        mTestAdapter.addAndNotify(0, 2);
        mLayoutManager.waitForLayout(2, false);
        checkForMainThreadException();
        // wait till "add animation" starts
        int limit = 200;
        while (addedView[0] == null || addedView[1] == null) {
            Thread.sleep(100);
            runTestOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (mRecyclerView.getChildCount() == 3) {
                        View view = mRecyclerView.getChildAt(0);
                        if (ViewCompat.hasTransientState(view)) {
                            addedView[0] = view;
                        }
                        view = mRecyclerView.getChildAt(1);
                        if (ViewCompat.hasTransientState(view)) {
                            addedView[1] = view;
                        }
                    }
                }
            });
            assertTrue("add should start on time", --limit > 0);
        }

        // Layout from item2, exclude the current adding items
        mLayoutManager.expectLayouts(1);
        mLayoutManager.mOnLayoutCallbacks = new OnLayoutCallbacks() {
            @Override
            void beforePostLayout(RecyclerView.Recycler recycler,
                    AnimationLayoutManager layoutManager,
                    RecyclerView.State state) {
                mLayoutMin = 2;
                mLayoutItemCount = 1;
            }
        };
        requestLayoutOnUIThread(mRecyclerView);
        mLayoutManager.waitForLayout(2);
    }

    public void testAdapterChangeFrozen() throws Throwable {
        setupBasic(10, 1, 7);
        assertTrue(mRecyclerView.getChildCount() == 7);

        mLayoutManager.expectLayouts(2);
        mLayoutManager.mOnLayoutCallbacks.mLayoutMin = 1;
        mLayoutManager.mOnLayoutCallbacks.mLayoutItemCount = 8;
        freezeLayout(true);
        mTestAdapter.addAndNotify(0, 1);

        mLayoutManager.assertNoLayout("RV should keep old child during frozen", 2);
        assertEquals(7, mRecyclerView.getChildCount());

        freezeLayout(false);
        mLayoutManager.waitForLayout(2);
        assertEquals("RV should get updated after waken from frozen",
                8, mRecyclerView.getChildCount());
    }

    public void testRemoveScrapInvalidate() throws Throwable {
        setupBasic(10);
        TestRecyclerView testRecyclerView = getTestRecyclerView();
        mLayoutManager.expectLayouts(1);
        testRecyclerView.expectDraw(1);
        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                mTestAdapter.mItems.clear();
                mTestAdapter.notifyDataSetChanged();
            }
        });
        mLayoutManager.waitForLayout(2);
        testRecyclerView.waitForDraw(2);
    }

    public void testDeleteVisibleAndInvisible() throws Throwable {
        setupBasic(11, 3, 5); //layout items  3 4 5 6 7
        mLayoutManager.expectLayouts(2);
        setLayoutRange(3, 5); //layout previously invisible child 10 from end of the list
        setExpectedItemCounts(9, 8);
        mTestAdapter.deleteAndNotify(new int[]{4, 1}, new int[]{7, 2});// delete items 4, 8, 9
        mLayoutManager.waitForLayout(2);
    }

    public void testFindPositionOffset() throws Throwable {
        setupBasic(10);
        mLayoutManager.mOnLayoutCallbacks = new OnLayoutCallbacks() {
            @Override
            void beforePreLayout(RecyclerView.Recycler recycler,
                    AnimationLayoutManager lm, RecyclerView.State state) {
                super.beforePreLayout(recycler, lm, state);
                // [0,2,4]
                assertEquals("offset check", 0, mAdapterHelper.findPositionOffset(0));
                assertEquals("offset check", 1, mAdapterHelper.findPositionOffset(2));
                assertEquals("offset check", 2, mAdapterHelper.findPositionOffset(4));
            }
        };
        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    // delete 1
                    mTestAdapter.deleteAndNotify(1, 1);
                    // delete 3
                    mTestAdapter.deleteAndNotify(2, 1);
                } catch (Throwable throwable) {
                    throwable.printStackTrace();
                }
            }
        });
        mLayoutManager.waitForLayout(2);
    }

    private void setLayoutRange(int start, int count) {
        mLayoutManager.mOnLayoutCallbacks.mLayoutMin = start;
        mLayoutManager.mOnLayoutCallbacks.mLayoutItemCount = count;
    }

    private void setExpectedItemCounts(int preLayout, int postLayout) {
        mLayoutManager.mOnLayoutCallbacks.setExpectedItemCounts(preLayout, postLayout);
    }

    public void testDeleteInvisible() throws Throwable {
        setupBasic(10, 1, 7);
        mLayoutManager.mOnLayoutCallbacks.mLayoutMin = 1;
        mLayoutManager.mOnLayoutCallbacks.mLayoutItemCount = 7;
        mLayoutManager.expectLayouts(1);
        mLayoutManager.mOnLayoutCallbacks.setExpectedItemCounts(8, 8);
        mTestAdapter.deleteAndNotify(new int[]{0, 1}, new int[]{7, 1});// delete item id 0,8
        mLayoutManager.waitForLayout(2);
    }

    private CollectPositionResult findByPos(RecyclerView recyclerView,
            RecyclerView.Recycler recycler, RecyclerView.State state, int position) {
        View view = recycler.getViewForPosition(position, true);
        RecyclerView.ViewHolder vh = recyclerView.getChildViewHolder(view);
        if (vh.wasReturnedFromScrap()) {
            vh.clearReturnedFromScrapFlag(); //keep data consistent.
            return CollectPositionResult.fromScrap(vh);
        } else {
            return CollectPositionResult.fromAdapter(vh);
        }
    }

    public Map<Integer, CollectPositionResult> collectPositions(RecyclerView recyclerView,
            RecyclerView.Recycler recycler, RecyclerView.State state, int... positions) {
        Map<Integer, CollectPositionResult> positionToAdapterMapping
                = new HashMap<Integer, CollectPositionResult>();
        for (int position : positions) {
            if (position < 0) {
                continue;
            }
            positionToAdapterMapping.put(position,
                    findByPos(recyclerView, recycler, state, position));
        }
        return positionToAdapterMapping;
    }

    public void testAddDelete2() throws Throwable {
        positionStatesTest(5, 0, 5, new AdapterOps() {
                    // 0 1 2 3 4
                    // 0 1 2 a b 3 4
                    // 0 1 b 3 4
                    // pre: 0 1 2 3 4
                    // pre w/ adap: 0 1 2 b 3 4
                    @Override
                    void onRun(TestAdapter adapter) throws Throwable {
                        adapter.addDeleteAndNotify(new int[]{3, 2}, new int[]{2, -2});
                    }
                }, PositionConstraint.scrap(2, 2, -1), PositionConstraint.scrap(1, 1, 1),
                PositionConstraint.scrap(3, 3, 3)
        );
    }

    public void testAddDelete1() throws Throwable {
        positionStatesTest(5, 0, 5, new AdapterOps() {
                    // 0 1 2 3 4
                    // 0 1 2 a b 3 4
                    // 0 2 a b 3 4
                    // 0 c d 2 a b 3 4
                    // 0 c d 2 a 4
                    // c d 2 a 4
                    // pre: 0 1 2 3 4
                    @Override
                    void onRun(TestAdapter adapter) throws Throwable {
                        adapter.addDeleteAndNotify(new int[]{3, 2}, new int[]{1, -1},
                                new int[]{1, 2}, new int[]{5, -2}, new int[]{0, -1});
                    }
                }, PositionConstraint.scrap(0, 0, -1), PositionConstraint.scrap(1, 1, -1),
                PositionConstraint.scrap(2, 2, 2), PositionConstraint.scrap(3, 3, -1),
                PositionConstraint.scrap(4, 4, 4), PositionConstraint.adapter(0),
                PositionConstraint.adapter(1), PositionConstraint.adapter(3)
        );
    }

    public void testAddSameIndexTwice() throws Throwable {
        positionStatesTest(12, 2, 7, new AdapterOps() {
                    @Override
                    void onRun(TestAdapter adapter) throws Throwable {
                        adapter.addAndNotify(new int[]{1, 2}, new int[]{5, 1}, new int[]{5, 1},
                                new int[]{11, 1});
                    }
                }, PositionConstraint.adapterScrap(0, 0), PositionConstraint.adapterScrap(1, 3),
                PositionConstraint.scrap(2, 2, 4), PositionConstraint.scrap(3, 3, 7),
                PositionConstraint.scrap(4, 4, 8), PositionConstraint.scrap(7, 7, 12),
                PositionConstraint.scrap(8, 8, 13)
        );
    }

    public void testDeleteTwice() throws Throwable {
        positionStatesTest(12, 2, 7, new AdapterOps() {
                    @Override
                    void onRun(TestAdapter adapter) throws Throwable {
                        adapter.deleteAndNotify(new int[]{0, 1}, new int[]{1, 1}, new int[]{7, 1},
                                new int[]{0, 1});// delete item ids 0,2,9,1
                    }
                }, PositionConstraint.scrap(2, 0, -1), PositionConstraint.scrap(3, 1, 0),
                PositionConstraint.scrap(4, 2, 1), PositionConstraint.scrap(5, 3, 2),
                PositionConstraint.scrap(6, 4, 3), PositionConstraint.scrap(8, 6, 5),
                PositionConstraint.adapterScrap(7, 6), PositionConstraint.adapterScrap(8, 7)
        );
    }


    public void positionStatesTest(int itemCount, int firstLayoutStartIndex,
            int firstLayoutItemCount, AdapterOps adapterChanges,
            final PositionConstraint... constraints) throws Throwable {
        positionStatesTest(itemCount, firstLayoutStartIndex, firstLayoutItemCount, null,
                adapterChanges, constraints);
    }

    public void positionStatesTest(int itemCount, int firstLayoutStartIndex,
            int firstLayoutItemCount, TestAdapter adapter, AdapterOps adapterChanges,
            final PositionConstraint... constraints) throws Throwable {
        setupBasic(itemCount, firstLayoutStartIndex, firstLayoutItemCount, adapter);
        mLayoutManager.expectLayouts(2);
        mLayoutManager.mOnLayoutCallbacks = new OnLayoutCallbacks() {
            @Override
            void beforePreLayout(RecyclerView.Recycler recycler, AnimationLayoutManager lm,
                    RecyclerView.State state) {
                super.beforePreLayout(recycler, lm, state);
                //harmless
                lm.detachAndScrapAttachedViews(recycler);
                final int[] ids = new int[constraints.length];
                for (int i = 0; i < constraints.length; i++) {
                    ids[i] = constraints[i].mPreLayoutPos;
                }
                Map<Integer, CollectPositionResult> positions
                        = collectPositions(lm.mRecyclerView, recycler, state, ids);
                StringBuilder positionLog = new StringBuilder("\nPosition logs:\n");
                for (Map.Entry<Integer, CollectPositionResult> entry : positions.entrySet()) {
                    positionLog.append(entry.getKey()).append(":").append(entry.getValue())
                            .append("\n");
                }
                for (PositionConstraint constraint : constraints) {
                    if (constraint.mPreLayoutPos != -1) {
                        constraint.validate(state, positions.get(constraint.mPreLayoutPos),
                                lm.getLog() + positionLog);
                    }
                }
            }

            @Override
            void beforePostLayout(RecyclerView.Recycler recycler, AnimationLayoutManager lm,
                    RecyclerView.State state) {
                super.beforePostLayout(recycler, lm, state);
                lm.detachAndScrapAttachedViews(recycler);
                final int[] ids = new int[constraints.length];
                for (int i = 0; i < constraints.length; i++) {
                    ids[i] = constraints[i].mPostLayoutPos;
                }
                Map<Integer, CollectPositionResult> positions
                        = collectPositions(lm.mRecyclerView, recycler, state, ids);
                StringBuilder positionLog = new StringBuilder("\nPosition logs:\n");
                for (Map.Entry<Integer, CollectPositionResult> entry : positions.entrySet()) {
                    positionLog.append(entry.getKey()).append(":")
                            .append(entry.getValue()).append("\n");
                }
                for (PositionConstraint constraint : constraints) {
                    if (constraint.mPostLayoutPos >= 0) {
                        constraint.validate(state, positions.get(constraint.mPostLayoutPos),
                                lm.getLog() + positionLog);
                    }
                }
            }
        };
        adapterChanges.run(mTestAdapter);
        mLayoutManager.waitForLayout(2);
        checkForMainThreadException();
        for (PositionConstraint constraint : constraints) {
            constraint.assertValidate();
        }
    }

    public void testAddThenRecycleRemovedView() throws Throwable {
        setupBasic(10);
        final AtomicInteger step = new AtomicInteger(0);
        final List<RecyclerView.ViewHolder> animateRemoveList
                = new ArrayList<RecyclerView.ViewHolder>();
        DefaultItemAnimator animator = new DefaultItemAnimator() {
            @Override
            public boolean animateRemove(RecyclerView.ViewHolder holder) {
                animateRemoveList.add(holder);
                return super.animateRemove(holder);
            }
        };
        mRecyclerView.setItemAnimator(animator);
        final List<RecyclerView.ViewHolder> pooledViews = new ArrayList<RecyclerView.ViewHolder>();
        mRecyclerView.setRecycledViewPool(new RecyclerView.RecycledViewPool() {
            @Override
            public void putRecycledView(RecyclerView.ViewHolder scrap) {
                pooledViews.add(scrap);
                super.putRecycledView(scrap);
            }
        });
        final RecyclerView.ViewHolder[] targetVh = new RecyclerView.ViewHolder[1];
        mLayoutManager.mOnLayoutCallbacks = new OnLayoutCallbacks() {
            @Override
            void doLayout(RecyclerView.Recycler recycler,
                    AnimationLayoutManager lm, RecyclerView.State state) {
                switch (step.get()) {
                    case 1:
                        super.doLayout(recycler, lm, state);
                        if (state.isPreLayout()) {
                            View view = mLayoutManager.getChildAt(1);
                            RecyclerView.ViewHolder holder =
                                    mRecyclerView.getChildViewHolderInt(view);
                            targetVh[0] = holder;
                            assertTrue("test sanity", holder.isRemoved());
                            mLayoutManager.removeAndRecycleView(view, recycler);
                        }
                        break;
                }
            }
        };
        step.set(1);
        animateRemoveList.clear();
        mLayoutManager.expectLayouts(2);
        mTestAdapter.deleteAndNotify(1, 1);
        mLayoutManager.waitForLayout(2);
        assertTrue("test sanity, view should be recycled", pooledViews.contains(targetVh[0]));
        assertTrue("since LM force recycled a view, animate disappearance should not be called",
                animateRemoveList.isEmpty());
    }
}
