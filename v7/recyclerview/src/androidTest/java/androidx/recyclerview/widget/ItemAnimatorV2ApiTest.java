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

import static androidx.recyclerview.widget.RecyclerView.ItemAnimator.FLAG_CHANGED;
import static androidx.recyclerview.widget.RecyclerView.ItemAnimator.FLAG_MOVED;
import static androidx.recyclerview.widget.RecyclerView.ItemAnimator.FLAG_REMOVED;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import android.support.test.filters.MediumTest;
import android.support.test.runner.AndroidJUnit4;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.hamcrest.CoreMatchers;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Includes tests for the new RecyclerView animations API (v2).
 */
@MediumTest
@RunWith(AndroidJUnit4.class)
public class ItemAnimatorV2ApiTest extends BaseRecyclerViewAnimationsTest {
    @Override
    protected RecyclerView.ItemAnimator createItemAnimator() {
        return mAnimator;
    }

    @Test
    public void changeMovedOutside() throws Throwable {
        setupBasic(10);
        final RecyclerView.ViewHolder target = mRecyclerView.findViewHolderForAdapterPosition(9);
        mLayoutManager.expectLayouts(2);
        mLayoutManager.mOnLayoutCallbacks.mLayoutItemCount = 9;
        mTestAdapter.changeAndNotify(9, 1);
        mLayoutManager.waitForLayout(2);
        // changed item should not be laid out and should just receive disappear
        LoggingInfo pre = mAnimator.preLayoutInfoMap.get(target);
        assertNotNull("test sanity", pre);
        assertNull("test sanity", mAnimator.postLayoutInfoMap.get(target));
        assertTrue(mAnimator.animateChangeList.isEmpty());
        assertEquals(1, mAnimator.animateDisappearanceList.size());
        assertEquals(new AnimateDisappearance(target, pre, null),
                mAnimator.animateDisappearanceList.get(0));
        // This is kind of problematic because layout manager will never layout the updated
        // version of this view since it went out of bounds and it won't show up in scrap.
        // I don't think we can do much better since other option is to bind a fresh view
    }

    @Test
    public void changeMovedOutsideWithPredictiveAndTwoViewHolders() throws Throwable {
        final RecyclerView.ViewHolder[] targets = new RecyclerView.ViewHolder[2];

        setupBasic(10, 0, 10, new TestAdapter(10) {
            @Override
            public void onBindViewHolder(@NonNull TestViewHolder holder,
                    int position) {
                super.onBindViewHolder(holder, position);
                if (position == 0) {
                    if (targets[0] == null) {
                        targets[0] = holder;
                    } else {
                        assertThat(targets[1], CoreMatchers.nullValue());
                        targets[1] = holder;
                    }
                }
            }
        });
        final RecyclerView.ViewHolder singleItemTarget =
                mRecyclerView.findViewHolderForAdapterPosition(1);
        mAnimator.canReUseCallback = new CanReUseCallback() {
            @Override
            public boolean canReUse(RecyclerView.ViewHolder viewHolder, List<Object> payloads) {
                return viewHolder == singleItemTarget;
            }
        };
        mLayoutManager.expectLayouts(2);
        mLayoutManager.mOnLayoutCallbacks = new OnLayoutCallbacks() {
            @Override
            void onLayoutChildren(RecyclerView.Recycler recycler,
                    AnimationLayoutManager lm, RecyclerView.State state) {
                super.onLayoutChildren(recycler, lm, state);
                if (!state.isPreLayout()) {
                    mLayoutManager.addDisappearingView(recycler.getViewForPosition(0));
                    mLayoutManager.addDisappearingView(recycler.getScrapList().get(0).itemView);
                }
            }
        };
        mLayoutManager.mOnLayoutCallbacks.mLayoutItemCount = 8;
        mLayoutManager.mOnLayoutCallbacks.mLayoutMin = 2;
        mTestAdapter.changeAndNotify(0, 2);
        mLayoutManager.waitForLayout(2);
        checkForMainThreadException();
        final RecyclerView.ViewHolder oldTarget = targets[0];
        final RecyclerView.ViewHolder newTarget = targets[1];
        assertNotNull("test sanity", targets[0]);
        assertNotNull("test sanity", targets[1]);
        // changed item should not be laid out and should just receive disappear
        LoggingInfo pre = mAnimator.preLayoutInfoMap.get(oldTarget);
        assertNotNull("test sanity", pre);
        assertNull("test sanity", mAnimator.postLayoutInfoMap.get(oldTarget));

        assertNull("test sanity", mAnimator.preLayoutInfoMap.get(newTarget));
        LoggingInfo post = mAnimator.postLayoutInfoMap.get(newTarget);
        assertNotNull("test sanity", post);
        assertEquals(1, mAnimator.animateChangeList.size());
        assertEquals(1, mAnimator.animateDisappearanceList.size());

        assertEquals(new AnimateChange(oldTarget, newTarget, pre, post),
                mAnimator.animateChangeList.get(0));

        LoggingInfo singleItemPre = mAnimator.preLayoutInfoMap.get(singleItemTarget);
        assertNotNull("test sanity", singleItemPre);
        LoggingInfo singleItemPost = mAnimator.postLayoutInfoMap.get(singleItemTarget);
        assertNotNull("test sanity", singleItemPost);

        assertEquals(new AnimateDisappearance(singleItemTarget, singleItemPre, singleItemPost),
                mAnimator.animateDisappearanceList.get(0));
    }
    @Test
    public void changeMovedOutsideWithPredictive() throws Throwable {
        setupBasic(10);
        final RecyclerView.ViewHolder target = mRecyclerView.findViewHolderForAdapterPosition(0);
        mLayoutManager.expectLayouts(2);
        mLayoutManager.mOnLayoutCallbacks = new OnLayoutCallbacks() {
            @Override
            void onLayoutChildren(RecyclerView.Recycler recycler,
                    AnimationLayoutManager lm, RecyclerView.State state) {
                super.onLayoutChildren(recycler, lm, state);
                List<RecyclerView.ViewHolder> scrapList = recycler.getScrapList();
                assertThat(scrapList.size(), CoreMatchers.is(2));
                mLayoutManager.addDisappearingView(scrapList.get(0).itemView);
                mLayoutManager.addDisappearingView(scrapList.get(0).itemView);
            }
        };
        mLayoutManager.mOnLayoutCallbacks.mLayoutItemCount = 8;
        mLayoutManager.mOnLayoutCallbacks.mLayoutMin = 2;
        mTestAdapter.changeAndNotify(0, 2);
        mLayoutManager.waitForLayout(2);
        checkForMainThreadException();
        // changed item should not be laid out and should just receive disappear
        LoggingInfo pre = mAnimator.preLayoutInfoMap.get(target);
        assertNotNull("test sanity", pre);
        LoggingInfo postInfo = mAnimator.postLayoutInfoMap.get(target);
        assertNotNull("test sanity", postInfo);
        assertTrue(mAnimator.animateChangeList.isEmpty());
        assertEquals(2, mAnimator.animateDisappearanceList.size());
        try {
            assertEquals(new AnimateDisappearance(target, pre, postInfo),
                    mAnimator.animateDisappearanceList.get(0));
        } catch (Throwable t) {
            assertEquals(new AnimateDisappearance(target, pre, postInfo),
                    mAnimator.animateDisappearanceList.get(1));
        }

    }

    @Test
    public void simpleAdd() throws Throwable {
        setupBasic(10);
        mLayoutManager.expectLayouts(2);
        mTestAdapter.addAndNotify(2, 1);
        mLayoutManager.waitForLayout(2);
        RecyclerView.ViewHolder vh = mRecyclerView.findViewHolderForAdapterPosition(2);
        assertEquals(1, mAnimator.animateAppearanceList.size());
        AnimateAppearance log = mAnimator.animateAppearanceList.get(0);
        assertSame(vh, log.viewHolder);
        assertNull(log.preInfo);
        assertEquals(0, log.postInfo.changeFlags);
        // the first two should not receive anything
        for (int i = 0; i < 2; i++) {
            RecyclerView.ViewHolder other = mRecyclerView.findViewHolderForAdapterPosition(i);
            assertEquals(0, mAnimator.preLayoutInfoMap.get(other).changeFlags);
        }
        for (int i = 3; i < mTestAdapter.getItemCount(); i++) {
            RecyclerView.ViewHolder other = mRecyclerView.findViewHolderForAdapterPosition(i);
            assertEquals(FLAG_MOVED, mAnimator.preLayoutInfoMap.get(other).changeFlags);
        }
        checkForMainThreadException();
    }

    @Test
    public void simpleRemove() throws Throwable {
        setupBasic(10);
        RecyclerView.ViewHolder vh = mRecyclerView.findViewHolderForAdapterPosition(2);
        mLayoutManager.expectLayouts(2);
        mTestAdapter.deleteAndNotify(2, 1);
        mLayoutManager.waitForLayout(2);
        checkForMainThreadException();
        assertEquals(1, mAnimator.animateDisappearanceList.size());
        AnimateDisappearance log = mAnimator.animateDisappearanceList.get(0);
        assertSame(vh, log.viewHolder);
        assertFalse(mAnimator.postLayoutInfoMap.containsKey(vh));
        assertEquals(FLAG_REMOVED, log.preInfo.changeFlags);
        // the first two should not receive anything
        for (int i = 0; i < 2; i++) {
            RecyclerView.ViewHolder other = mRecyclerView.findViewHolderForAdapterPosition(i);
            assertEquals(0, mAnimator.preLayoutInfoMap.get(other).changeFlags);
        }
        for (int i = 3; i < mTestAdapter.getItemCount(); i++) {
            RecyclerView.ViewHolder other = mRecyclerView.findViewHolderForAdapterPosition(i);
            assertEquals(FLAG_MOVED, mAnimator.preLayoutInfoMap.get(other).changeFlags);
        }
        checkForMainThreadException();
    }

    @Test
    public void simpleUpdate() throws Throwable {
        setupBasic(10);
        RecyclerView.ViewHolder vh = mRecyclerView.findViewHolderForAdapterPosition(2);
        mLayoutManager.expectLayouts(2);
        mTestAdapter.changeAndNotify(2, 1);
        mLayoutManager.waitForLayout(2);
        assertEquals(1, mAnimator.animateChangeList.size());
        AnimateChange log = mAnimator.animateChangeList.get(0);
        assertSame(vh, log.viewHolder);
        assertSame(vh, log.newHolder);
        assertTrue(mAnimator.preLayoutInfoMap.containsKey(vh));
        assertTrue(mAnimator.postLayoutInfoMap.containsKey(vh));
        assertEquals(FLAG_CHANGED, log.preInfo.changeFlags);
        assertEquals(0, log.postInfo.changeFlags);
        //others should not receive anything
        for (int i = 0; i < mTestAdapter.getItemCount(); i++) {
            if (i == 2) {
                continue;
            }
            RecyclerView.ViewHolder other = mRecyclerView.findViewHolderForAdapterPosition(i);
            assertEquals(0, mAnimator.preLayoutInfoMap.get(other).changeFlags);
        }
        checkForMainThreadException();
    }

    @Test
    public void updateWithDuplicateViewHolder() throws Throwable {
        setupBasic(10);
        final RecyclerView.ViewHolder vh = mRecyclerView.findViewHolderForAdapterPosition(2);
        mAnimator.canReUseCallback = new CanReUseCallback() {
            @Override
            public boolean canReUse(RecyclerView.ViewHolder viewHolder, List<Object> payloads) {
                assertSame(viewHolder, vh);
                return false;
            }
        };
        mLayoutManager.expectLayouts(2);
        mTestAdapter.changeAndNotify(2, 1);
        mLayoutManager.waitForLayout(2);
        final RecyclerView.ViewHolder newVh = mRecyclerView.findViewHolderForAdapterPosition(2);
        assertNotSame(vh, newVh);
        assertEquals(1, mAnimator.animateChangeList.size());
        AnimateChange log = mAnimator.animateChangeList.get(0);
        assertSame(vh, log.viewHolder);
        assertSame(newVh, log.newHolder);
        assertNull(vh.itemView.getParent());
        assertTrue(mAnimator.preLayoutInfoMap.containsKey(vh));
        assertFalse(mAnimator.postLayoutInfoMap.containsKey(vh));
        assertTrue(mAnimator.postLayoutInfoMap.containsKey(newVh));
        assertEquals(FLAG_CHANGED, log.preInfo.changeFlags);
        assertEquals(0, log.postInfo.changeFlags);
        //others should not receive anything
        for (int i = 0; i < mTestAdapter.getItemCount(); i++) {
            if (i == 2) {
                continue;
            }
            RecyclerView.ViewHolder other = mRecyclerView.findViewHolderForAdapterPosition(i);
            assertEquals(0, mAnimator.preLayoutInfoMap.get(other).changeFlags);
        }
        checkForMainThreadException();
    }

    @Test
    public void updateWithOneDuplicateAndOneInPlace() throws Throwable {
        setupBasic(10);
        final RecyclerView.ViewHolder replaced = mRecyclerView.findViewHolderForAdapterPosition(2);
        final RecyclerView.ViewHolder reused = mRecyclerView.findViewHolderForAdapterPosition(3);
        mAnimator.canReUseCallback = new CanReUseCallback() {
            @Override
            public boolean canReUse(RecyclerView.ViewHolder viewHolder, List<Object> payloads) {
                if (viewHolder == replaced) {
                    return false;
                } else if (viewHolder == reused) {
                    return true;
                }
                fail("unpexpected view");
                return false;
            }
        };
        mLayoutManager.expectLayouts(2);
        mTestAdapter.changeAndNotify(2, 2);
        mLayoutManager.waitForLayout(2);
        final RecyclerView.ViewHolder newVh = mRecyclerView.findViewHolderForAdapterPosition(2);

        assertNotSame(replaced, newVh);
        assertSame(reused, mRecyclerView.findViewHolderForAdapterPosition(3));

        assertEquals(2, mAnimator.animateChangeList.size());
        AnimateChange logReplaced = null, logReused = null;
        for (AnimateChange change : mAnimator.animateChangeList) {
            if (change.newHolder == change.viewHolder) {
                logReused = change;
            } else {
                logReplaced = change;
            }
        }
        assertNotNull(logReplaced);
        assertNotNull(logReused);
        assertSame(replaced, logReplaced.viewHolder);
        assertSame(newVh, logReplaced.newHolder);
        assertSame(reused, logReused.viewHolder);
        assertSame(reused, logReused.newHolder);

        assertTrue(mAnimator.preLayoutInfoMap.containsKey(replaced));
        assertTrue(mAnimator.preLayoutInfoMap.containsKey(reused));

        assertTrue(mAnimator.postLayoutInfoMap.containsKey(newVh));
        assertTrue(mAnimator.postLayoutInfoMap.containsKey(reused));
        assertFalse(mAnimator.postLayoutInfoMap.containsKey(replaced));

        assertEquals(FLAG_CHANGED, logReplaced.preInfo.changeFlags);
        assertEquals(FLAG_CHANGED, logReused.preInfo.changeFlags);

        assertEquals(0, logReplaced.postInfo.changeFlags);
        assertEquals(0, logReused.postInfo.changeFlags);
        //others should not receive anything
        for (int i = 0; i < mTestAdapter.getItemCount(); i++) {
            if (i == 2 || i == 3) {
                continue;
            }
            RecyclerView.ViewHolder other = mRecyclerView.findViewHolderForAdapterPosition(i);
            assertEquals(0, mAnimator.preLayoutInfoMap.get(other).changeFlags);
        }
        checkForMainThreadException();
    }

    @Test
    public void changeToDisappear() throws Throwable {
        setupBasic(10);
        RecyclerView.ViewHolder vh = mRecyclerView.findViewHolderForAdapterPosition(9);
        mLayoutManager.mOnLayoutCallbacks.mLayoutItemCount = 9;
        mLayoutManager.expectLayouts(2);
        mTestAdapter.changeAndNotify(9, 1);
        mLayoutManager.waitForLayout(2);
        assertEquals(1, mAnimator.animateDisappearanceList.size());
        AnimateDisappearance log = mAnimator.animateDisappearanceList.get(0);
        assertSame(vh, log.viewHolder);
        assertFalse(mAnimator.postLayoutInfoMap.containsKey(vh));
        assertEquals(FLAG_CHANGED, log.preInfo.changeFlags);
        assertEquals(0, mAnimator.animateChangeList.size());
        assertEquals(0, mAnimator.animateAppearanceList.size());
        assertEquals(9, mAnimator.animatePersistenceList.size());
        checkForMainThreadException();
    }

    @Test
    public void changeToDisappearFromHead() throws Throwable {
        setupBasic(10);
        RecyclerView.ViewHolder vh = mRecyclerView.findViewHolderForAdapterPosition(0);
        mLayoutManager.mOnLayoutCallbacks.mLayoutItemCount = 9;
        mLayoutManager.mOnLayoutCallbacks.mLayoutMin = 1;
        mLayoutManager.expectLayouts(2);
        mTestAdapter.changeAndNotify(0, 1);
        mLayoutManager.waitForLayout(2);
        assertEquals(1, mAnimator.animateDisappearanceList.size());
        AnimateDisappearance log = mAnimator.animateDisappearanceList.get(0);
        assertSame(vh, log.viewHolder);
        assertFalse(mAnimator.postLayoutInfoMap.containsKey(vh));
        assertEquals(FLAG_CHANGED, log.preInfo.changeFlags);
        assertEquals(0, mAnimator.animateChangeList.size());
        assertEquals(0, mAnimator.animateAppearanceList.size());
        assertEquals(9, mAnimator.animatePersistenceList.size());
        checkForMainThreadException();
    }

    @Test
    public void updatePayload() throws Throwable {
        setupBasic(10);
        final RecyclerView.ViewHolder vh = mRecyclerView.findViewHolderForAdapterPosition(2);
        final Object payload = new Object();
        mAnimator.canReUseCallback = new CanReUseCallback() {
            @Override
            public boolean canReUse(RecyclerView.ViewHolder viewHolder, List<Object> payloads) {
                assertSame(vh, viewHolder);
                assertEquals(1, payloads.size());
                assertSame(payload, payloads.get(0));
                return true;
            }
        };
        mLayoutManager.expectLayouts(2);
        mTestAdapter.changeAndNotifyWithPayload(2, 1, payload);
        mLayoutManager.waitForLayout(2);
        assertEquals(1, mAnimator.animateChangeList.size());
        AnimateChange log = mAnimator.animateChangeList.get(0);
        assertSame(vh, log.viewHolder);
        assertSame(vh, log.newHolder);
        assertTrue(mAnimator.preLayoutInfoMap.containsKey(vh));
        assertTrue(mAnimator.postLayoutInfoMap.containsKey(vh));
        assertEquals(FLAG_CHANGED, log.preInfo.changeFlags);
        assertEquals(0, log.postInfo.changeFlags);
        assertNotNull(log.preInfo.payloads);
        assertTrue(log.preInfo.payloads.contains(payload));
        //others should not receive anything
        for (int i = 0; i < mTestAdapter.getItemCount(); i++) {
            if (i == 2) {
                continue;
            }
            RecyclerView.ViewHolder other = mRecyclerView.findViewHolderForAdapterPosition(i);
            assertEquals(0, mAnimator.preLayoutInfoMap.get(other).changeFlags);
        }
        checkForMainThreadException();
    }

    @Test
    public void notifyDataSetChanged() throws Throwable {
        TestAdapter adapter = new TestAdapter(10);
        adapter.setHasStableIds(true);
        setupBasic(10, 0, 10, adapter);
        mLayoutManager.expectLayouts(1);
        mTestAdapter.dispatchDataSetChanged();
        mLayoutManager.waitForLayout(2);
        assertEquals(10, mAnimator.animateChangeList.size());
        for (AnimateChange change : mAnimator.animateChangeList) {
            assertNotNull(change.preInfo);
            assertNotNull(change.postInfo);
            assertSame(change.preInfo.viewHolder, change.postInfo.viewHolder);
        }
        assertEquals(0, mAnimator.animatePersistenceList.size());
        assertEquals(0, mAnimator.animateAppearanceList.size());
        assertEquals(0, mAnimator.animateDisappearanceList.size());
    }

    @Test
    public void notifyDataSetChangedWithoutStableIds() throws Throwable {
        TestAdapter adapter = new TestAdapter(10);
        adapter.setHasStableIds(false);
        setupBasic(10, 0, 10, adapter);
        mLayoutManager.expectLayouts(1);
        mTestAdapter.dispatchDataSetChanged();
        mLayoutManager.waitForLayout(2);
        assertEquals(0, mAnimator.animateChangeList.size());
        assertEquals(0, mAnimator.animatePersistenceList.size());
        assertEquals(0, mAnimator.animateAppearanceList.size());
        assertEquals(0, mAnimator.animateDisappearanceList.size());
    }

    @Test
    public void notifyDataSetChangedWithAppearing() throws Throwable {
        notifyDataSetChangedWithAppearing(false);
    }

    @Test
    public void notifyDataSetChangedWithAppearingNotifyBoth() throws Throwable {
        notifyDataSetChangedWithAppearing(true);
    }

    private void notifyDataSetChangedWithAppearing(final boolean notifyBoth) throws Throwable {
        final TestAdapter adapter = new TestAdapter(10);
        adapter.setHasStableIds(true);
        setupBasic(10, 0, 10, adapter);
        mLayoutManager.expectLayouts(1);
        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    if (notifyBoth) {
                        adapter.addAndNotify(2, 2);
                    } else {
                        adapter.mItems.add(2, new Item(2, "custom 1"));
                        adapter.mItems.add(3, new Item(3, "custom 2"));
                    }

                    adapter.notifyDataSetChanged();
                } catch (Throwable throwable) {
                    throwable.printStackTrace();
                }
            }
        });
        mLayoutManager.waitForLayout(2);
        assertEquals(10, mAnimator.animateChangeList.size());
        assertEquals(0, mAnimator.animatePersistenceList.size());
        assertEquals(2, mAnimator.animateAppearanceList.size());
        assertEquals(0, mAnimator.animateDisappearanceList.size());
    }

    @Test
    public void notifyDataSetChangedWithDispappearing() throws Throwable {
        notifyDataSetChangedWithDispappearing(false);
    }

    @Test
    public void notifyDataSetChangedWithDispappearingNotifyBoth() throws Throwable {
        notifyDataSetChangedWithDispappearing(true);
    }

    private void notifyDataSetChangedWithDispappearing(final boolean notifyBoth) throws Throwable {
        final TestAdapter adapter = new TestAdapter(10);
        adapter.setHasStableIds(true);
        setupBasic(10, 0, 10, adapter);
        mLayoutManager.expectLayouts(1);
        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    if (notifyBoth) {
                        adapter.deleteAndNotify(2, 2);
                    } else {
                        adapter.mItems.remove(2);
                        adapter.mItems.remove(2);
                    }
                    adapter.notifyDataSetChanged();
                } catch (Throwable throwable) {
                    throwable.printStackTrace();
                }
            }
        });
        mLayoutManager.waitForLayout(2);
        assertEquals(8, mAnimator.animateChangeList.size());
        assertEquals(0, mAnimator.animatePersistenceList.size());
        assertEquals(0, mAnimator.animateAppearanceList.size());
        assertEquals(2, mAnimator.animateDisappearanceList.size());
    }

    @Test
    public void notifyUpdateWithChangedAdapterType() throws Throwable {
        final AtomicInteger itemType = new AtomicInteger(1);
        final TestAdapter adapter = new TestAdapter(10) {
            @Override
            public int getItemViewType(int position) {
                return position == 2 ? itemType.get() : 20;
            }
        };
        adapter.setHasStableIds(true);
        setupBasic(10, 0, 10, adapter);
        final RecyclerView.ViewHolder vh = mRecyclerView.findViewHolderForAdapterPosition(2);

        mAnimator.canReUseCallback = new CanReUseCallback() {
            @Override
            public boolean canReUse(RecyclerView.ViewHolder viewHolder, List<Object> payloads) {
                return viewHolder != vh;
            }
        };

        mLayoutManager.expectLayouts(1);
        itemType.set(3);
        adapter.dispatchDataSetChanged();
        mLayoutManager.waitForLayout(2);
        final RecyclerView.ViewHolder newVh = mRecyclerView.findViewHolderForAdapterPosition(2);
        // TODO we should be able to map old type to the new one but doing that change has some
        // recycling side effects.
        assertEquals(9, mAnimator.animateChangeList.size());
        assertEquals(0, mAnimator.animatePersistenceList.size());
        assertEquals(1, mAnimator.animateAppearanceList.size());
        assertEquals(0, mAnimator.animateDisappearanceList.size());
        assertNotSame(vh, newVh);
        for (AnimateChange change : mAnimator.animateChangeList) {
            if (change.viewHolder == vh) {
                assertSame(change.newHolder, newVh);
                assertSame(change.viewHolder, vh);
            } else {
                assertSame(change.newHolder, change.viewHolder);
            }
        }
    }

    LoggingV2Animator mAnimator = new LoggingV2Animator();

    class LoggingV2Animator extends RecyclerView.ItemAnimator {

        CanReUseCallback canReUseCallback = new CanReUseCallback() {
            @Override
            public boolean canReUse(RecyclerView.ViewHolder viewHolder, List<Object> payloads) {
                return true;
            }
        };
        Map<RecyclerView.ViewHolder, LoggingInfo> preLayoutInfoMap = new HashMap<>();
        Map<RecyclerView.ViewHolder, LoggingInfo> postLayoutInfoMap = new HashMap<>();

        List<AnimateAppearance> animateAppearanceList = new ArrayList<>();
        List<AnimateDisappearance> animateDisappearanceList = new ArrayList<>();
        List<AnimatePersistence> animatePersistenceList = new ArrayList<>();
        List<AnimateChange> animateChangeList = new ArrayList<>();

        @Override
        public boolean canReuseUpdatedViewHolder(RecyclerView.ViewHolder viewHolder,
                List<Object> payloads) {
            return canReUseCallback.canReUse(viewHolder, payloads);
        }

        @NonNull
        @Override
        public ItemHolderInfo recordPreLayoutInformation(@NonNull RecyclerView.State state,
                @NonNull RecyclerView.ViewHolder viewHolder,
                @AdapterChanges int changeFlags, @NonNull List<Object> payloads) {
            LoggingInfo loggingInfo = new LoggingInfo(viewHolder, changeFlags, payloads);
            preLayoutInfoMap.put(viewHolder, loggingInfo);
            return loggingInfo;
        }

        @NonNull
        @Override
        public ItemHolderInfo recordPostLayoutInformation(@NonNull RecyclerView.State state,
                @NonNull RecyclerView.ViewHolder viewHolder) {
            LoggingInfo loggingInfo = new LoggingInfo(viewHolder, 0, null);
            postLayoutInfoMap.put(viewHolder, loggingInfo);
            return loggingInfo;
        }

        @Override
        public boolean animateDisappearance(@NonNull RecyclerView.ViewHolder viewHolder,
                @NonNull ItemHolderInfo preLayoutInfo,
                @Nullable ItemHolderInfo postLayoutInfo) {
            animateDisappearanceList.add(new AnimateDisappearance(viewHolder,
                    (LoggingInfo) preLayoutInfo, (LoggingInfo) postLayoutInfo));
            assertSame(preLayoutInfoMap.get(viewHolder), preLayoutInfo);
            assertSame(postLayoutInfoMap.get(viewHolder), postLayoutInfo);
            dispatchAnimationFinished(viewHolder);

            return false;
        }

        @Override
        public boolean animateAppearance(@NonNull RecyclerView.ViewHolder viewHolder,
                ItemHolderInfo preInfo, @NonNull ItemHolderInfo postInfo) {
            animateAppearanceList.add(
                    new AnimateAppearance(viewHolder, (LoggingInfo) preInfo, (LoggingInfo) postInfo));
            assertSame(preLayoutInfoMap.get(viewHolder), preInfo);
            assertSame(postLayoutInfoMap.get(viewHolder), postInfo);
            dispatchAnimationFinished(viewHolder);
            return false;
        }

        @Override
        public boolean animatePersistence(@NonNull RecyclerView.ViewHolder viewHolder,
                @NonNull ItemHolderInfo preInfo, @NonNull ItemHolderInfo postInfo) {
            animatePersistenceList.add(new AnimatePersistence(viewHolder, (LoggingInfo) preInfo,
                    (LoggingInfo) postInfo));
            dispatchAnimationFinished(viewHolder);
            assertSame(preLayoutInfoMap.get(viewHolder), preInfo);
            assertSame(postLayoutInfoMap.get(viewHolder), postInfo);
            return false;
        }

        @Override
        public boolean animateChange(@NonNull RecyclerView.ViewHolder oldHolder,
                @NonNull RecyclerView.ViewHolder newHolder, @NonNull ItemHolderInfo preInfo,
                @NonNull ItemHolderInfo postInfo) {
            animateChangeList.add(new AnimateChange(oldHolder, newHolder, (LoggingInfo) preInfo,
                    (LoggingInfo) postInfo));
            if (oldHolder != null) {
                dispatchAnimationFinished(oldHolder);
                assertSame(preLayoutInfoMap.get(oldHolder), preInfo);
            }
            if (newHolder != null && oldHolder != newHolder) {
                dispatchAnimationFinished(newHolder);
                assertSame(postLayoutInfoMap.get(newHolder), postInfo);
            }

            return false;
        }

        @Override
        public void runPendingAnimations() {

        }

        @Override
        public void endAnimation(RecyclerView.ViewHolder item) {
        }

        @Override
        public void endAnimations() {

        }

        @Override
        public boolean isRunning() {
            return false;
        }
    }

    interface CanReUseCallback {

        boolean canReUse(RecyclerView.ViewHolder viewHolder, List<Object> payloads);
    }
}
