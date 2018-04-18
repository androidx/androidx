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

import static androidx.recyclerview.widget.ViewInfoStore.InfoRecord.FLAG_APPEAR;
import static androidx.recyclerview.widget.ViewInfoStore.InfoRecord.FLAG_DISAPPEARED;
import static androidx.recyclerview.widget.ViewInfoStore.InfoRecord.FLAG_POST;
import static androidx.recyclerview.widget.ViewInfoStore.InfoRecord.FLAG_PRE;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import android.support.test.filters.SmallTest;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Pair;
import androidx.recyclerview.widget.RecyclerView.ItemAnimator.ItemHolderInfo;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SuppressWarnings("ConstantConditions")
@RunWith(JUnit4.class)
@SmallTest
public class ViewInfoStoreTest {
    ViewInfoStore mStore;
    LoggingProcessCallback mCallback;
    @Before
    public void prepare() {
        mStore = new ViewInfoStore();
        mCallback = new LoggingProcessCallback();
    }

    @Test
    public void addOverridePre() {
        RecyclerView.ViewHolder vh = new MockViewHolder();
        MockInfo info = new MockInfo();
        mStore.addToPreLayout(vh, info);
        MockInfo info2 = new MockInfo();
        mStore.addToPreLayout(vh, info2);
        assertSame(info2, find(vh, FLAG_PRE));
    }

    @Test
    public void addOverridePost() {
        RecyclerView.ViewHolder vh = new MockViewHolder();
        MockInfo info = new MockInfo();
        mStore.addToPostLayout(vh, info);
        MockInfo info2 = new MockInfo();
        mStore.addToPostLayout(vh, info2);
        assertSame(info2, find(vh, FLAG_POST));
    }

    @Test
    public void addRemoveAndReAdd() {
        RecyclerView.ViewHolder vh = new MockViewHolder();
        MockInfo pre = new MockInfo();
        mStore.addToPreLayout(vh, pre);
        MockInfo post1 = new MockInfo();
        mStore.addToPostLayout(vh, post1);
        mStore.onViewDetached(vh);
        mStore.addToDisappearedInLayout(vh);
    }

    @Test
    public void addToPreLayout() {
        RecyclerView.ViewHolder vh = new MockViewHolder();
        MockInfo info = new MockInfo();
        mStore.addToPreLayout(vh, info);
        assertSame(info, find(vh, FLAG_PRE));
        assertTrue(mStore.isInPreLayout(vh));
        mStore.removeViewHolder(vh);
        assertFalse(mStore.isInPreLayout(vh));
    }

    @Test
    public void addToPostLayout() {
        RecyclerView.ViewHolder vh = new MockViewHolder();
        MockInfo info = new MockInfo();
        mStore.addToPostLayout(vh, info);
        assertSame(info, find(vh, FLAG_POST));
        mStore.removeViewHolder(vh);
        assertNull(find(vh, FLAG_POST));
    }

    @Test
    public void popFromPreLayout() {
        assertEquals(0, sizeOf(FLAG_PRE));
        RecyclerView.ViewHolder vh = new MockViewHolder();
        MockInfo info = new MockInfo();
        mStore.addToPreLayout(vh, info);
        assertSame(info, mStore.popFromPreLayout(vh));
        assertNull(mStore.popFromPreLayout(vh));
    }

    @Test
    public void addToOldChangeHolders() {
        RecyclerView.ViewHolder vh = new MockViewHolder();
        mStore.addToOldChangeHolders(1, vh);
        assertSame(vh, mStore.getFromOldChangeHolders(1));
        mStore.removeViewHolder(vh);
        assertNull(mStore.getFromOldChangeHolders(1));
    }

    @Test
    public void appearListTests() {
        RecyclerView.ViewHolder vh = new MockViewHolder();
        RecyclerView.ItemAnimator.ItemHolderInfo info = new MockInfo();
        mStore.addToAppearedInPreLayoutHolders(vh, info);
        assertEquals(1, sizeOf(FLAG_APPEAR));
        RecyclerView.ViewHolder vh2 = new MockViewHolder();
        mStore.addToAppearedInPreLayoutHolders(vh2, info);
        assertEquals(2, sizeOf(FLAG_APPEAR));
        mStore.removeViewHolder(vh2);
        assertEquals(1, sizeOf(FLAG_APPEAR));
    }

    @Test
    public void disappearListTest() {
        RecyclerView.ViewHolder vh = new MockViewHolder();
        mStore.addToDisappearedInLayout(vh);
        assertEquals(1, sizeOf(FLAG_DISAPPEARED));
        mStore.addToDisappearedInLayout(vh);
        assertEquals(1, sizeOf(FLAG_DISAPPEARED));
        RecyclerView.ViewHolder vh2 = new MockViewHolder();
        mStore.addToDisappearedInLayout(vh2);
        assertEquals(2, sizeOf(FLAG_DISAPPEARED));
        mStore.removeViewHolder(vh2);
        assertEquals(1, sizeOf(FLAG_DISAPPEARED));
        mStore.removeFromDisappearedInLayout(vh);
        assertEquals(0, sizeOf(FLAG_DISAPPEARED));
    }

    @Test
    public void processAppear() {
        ViewHolder vh = new MockViewHolder();
        MockInfo info = new MockInfo();
        mStore.addToPostLayout(vh, info);
        mStore.process(mCallback);
        assertEquals(new Pair<>(null, info), mCallback.appeared.get(vh));
        assertTrue(mCallback.disappeared.isEmpty());
        assertTrue(mCallback.unused.isEmpty());
        assertTrue(mCallback.persistent.isEmpty());
    }

    @Test
    public void processDisappearNormal() {
        ViewHolder vh = new MockViewHolder();
        MockInfo info = new MockInfo();
        mStore.addToPreLayout(vh, info);
        mStore.process(mCallback);
        assertEquals(new Pair<>(info, null), mCallback.disappeared.get(vh));
        assertTrue(mCallback.appeared.isEmpty());
        assertTrue(mCallback.unused.isEmpty());
        assertTrue(mCallback.persistent.isEmpty());
    }

    @Test
    public void processDisappearMissingLayout() {
        ViewHolder vh = new MockViewHolder();
        MockInfo info = new MockInfo();
        mStore.addToPreLayout(vh, info);
        mStore.addToDisappearedInLayout(vh);
        mStore.process(mCallback);
        assertEquals(new Pair<>(info, null), mCallback.disappeared.get(vh));
        assertTrue(mCallback.appeared.isEmpty());
        assertTrue(mCallback.unused.isEmpty());
        assertTrue(mCallback.persistent.isEmpty());
    }

    @Test
    public void processDisappearMoveOut() {
        ViewHolder vh = new MockViewHolder();
        MockInfo pre = new MockInfo();
        MockInfo post = new MockInfo();
        mStore.addToPreLayout(vh, pre);
        mStore.addToDisappearedInLayout(vh);
        mStore.addToPostLayout(vh, post);
        mStore.process(mCallback);
        assertEquals(new Pair<>(pre, post), mCallback.disappeared.get(vh));
        assertTrue(mCallback.appeared.isEmpty());
        assertTrue(mCallback.unused.isEmpty());
        assertTrue(mCallback.persistent.isEmpty());
    }

    @Test
    public void processDisappearAppear() {
        ViewHolder vh = new MockViewHolder();
        MockInfo pre = new MockInfo();
        MockInfo post = new MockInfo();
        mStore.addToPreLayout(vh, pre);
        mStore.addToDisappearedInLayout(vh);
        mStore.addToPostLayout(vh, post);
        mStore.removeFromDisappearedInLayout(vh);
        mStore.process(mCallback);
        assertTrue(mCallback.disappeared.isEmpty());
        assertTrue(mCallback.appeared.isEmpty());
        assertTrue(mCallback.unused.isEmpty());
        assertEquals(mCallback.persistent.get(vh), new Pair<>(pre, post));
    }

    @Test
    public void processAppearAndDisappearInPostLayout() {
        ViewHolder vh = new MockViewHolder();
        MockInfo info1 = new MockInfo();
        mStore.addToPostLayout(vh, info1);
        mStore.addToDisappearedInLayout(vh);
        mStore.process(mCallback);
        assertTrue(mCallback.disappeared.isEmpty());
        assertTrue(mCallback.appeared.isEmpty());
        assertTrue(mCallback.persistent.isEmpty());
        assertSame(mCallback.unused.get(0), vh);
    }

    static class MockViewHolder extends RecyclerView.ViewHolder {
        public MockViewHolder() {
            super(new View(null));
        }
    }

    static class MockInfo extends RecyclerView.ItemAnimator.ItemHolderInfo {

    }

    private int sizeOf(int flags) {
        int cnt = 0;
        final int size = mStore.mLayoutHolderMap.size();
        for (int i = 0; i < size; i ++) {
            ViewInfoStore.InfoRecord record = mStore.mLayoutHolderMap.valueAt(i);
            if ((record.flags & flags) != 0) {
                cnt ++;
            }
        }
        return cnt;
    }

    private RecyclerView.ItemAnimator.ItemHolderInfo find(RecyclerView.ViewHolder viewHolder,
            int flags) {
        final int size = mStore.mLayoutHolderMap.size();
        for (int i = 0; i < size; i ++) {
            ViewInfoStore.InfoRecord record = mStore.mLayoutHolderMap.valueAt(i);
            RecyclerView.ViewHolder holder = mStore.mLayoutHolderMap.keyAt(i);
            if ((record.flags & flags) != 0 && holder == viewHolder) {
                if (flags == FLAG_PRE || flags == FLAG_APPEAR) {
                    return record.preInfo;
                } else if (flags == FLAG_POST) {
                    return record.postInfo;
                }
                throw new UnsupportedOperationException("don't know this flag");
            }
        }
        return null;
    }

    private static class LoggingProcessCallback implements ViewInfoStore.ProcessCallback {
        final Map<ViewHolder, Pair<ItemHolderInfo, ItemHolderInfo>> disappeared = new HashMap<>();
        final Map<ViewHolder, Pair<ItemHolderInfo, ItemHolderInfo>> appeared = new HashMap<>();
        final Map<ViewHolder, Pair<ItemHolderInfo, ItemHolderInfo>> persistent = new HashMap<>();
        final List<ViewHolder> unused = new ArrayList<>();
        @Override
        public void processDisappeared(ViewHolder viewHolder,
                ItemHolderInfo preInfo,
                @Nullable ItemHolderInfo postInfo) {
            assertNotNull(preInfo);
            assertFalse(disappeared.containsKey(viewHolder));
            disappeared.put(viewHolder, new Pair<>(preInfo, postInfo));
        }

        @Override
        public void processAppeared(ViewHolder viewHolder,
                @Nullable ItemHolderInfo preInfo, @NonNull ItemHolderInfo info) {
            assertNotNull(info);
            assertFalse(appeared.containsKey(viewHolder));
            appeared.put(viewHolder, new Pair<>(preInfo, info));
        }

        @Override
        public void processPersistent(ViewHolder viewHolder,
                @NonNull ItemHolderInfo preInfo, @NonNull ItemHolderInfo postInfo) {
            assertFalse(persistent.containsKey(viewHolder));
            assertNotNull(preInfo);
            assertNotNull(postInfo);
            persistent.put(viewHolder, new Pair<>(preInfo, postInfo));
        }

        @Override
        public void unused(ViewHolder holder) {
            unused.add(holder);
        }
    }

}
