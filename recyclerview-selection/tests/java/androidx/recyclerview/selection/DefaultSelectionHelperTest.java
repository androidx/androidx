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

import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;
import android.util.SparseBooleanArray;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import androidx.recyclerview.selection.SelectionHelper.SelectionPredicate;
import androidx.recyclerview.selection.testing.SelectionProbe;
import androidx.recyclerview.selection.testing.TestAdapter;
import androidx.recyclerview.selection.testing.TestItemKeyProvider;
import androidx.recyclerview.selection.testing.TestSelectionObserver;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class DefaultSelectionHelperTest {

    private List<String> mItems;
    private Set<String> mIgnored;
    private TestAdapter mAdapter;
    private DefaultSelectionHelper<String> mHelper;
    private TestSelectionObserver<String> mListener;
    private SelectionProbe mSelection;

    @Before
    public void setUp() throws Exception {
        mIgnored = new HashSet<>();
        mItems = TestAdapter.createItemList(100);
        mListener = new TestSelectionObserver<>();
        mAdapter = new TestAdapter();
        mAdapter.updateTestModelIds(mItems);

        SelectionPredicate selectionPredicate = new SelectionPredicate<String>() {

            @Override
            public boolean canSetStateForKey(String id, boolean nextState) {
                return !nextState || !mIgnored.contains(id);
            }

            @Override
            public boolean canSetStateAtPosition(int position, boolean nextState) {
                throw new UnsupportedOperationException("Not implemented.");
            }

            @Override
            public boolean canSelectMultiple() {
                return true;
            }
        };

        ItemKeyProvider<String> keyProvider =
                new TestItemKeyProvider<String>(ItemKeyProvider.SCOPE_MAPPED, mAdapter);
        mHelper = new DefaultSelectionHelper<>(
                keyProvider,
                selectionPredicate);

        EventBridge.install(mAdapter, mHelper, keyProvider);

        mHelper.addObserver(mListener);

        mSelection = new SelectionProbe(mHelper, mListener);

        mIgnored.clear();
    }

    @Test
    public void testSelect() {
        mHelper.select(mItems.get(7));

        mSelection.assertSelection(7);
    }

    @Test
    public void testDeselect() {
        mHelper.select(mItems.get(7));
        mHelper.deselect(mItems.get(7));

        mSelection.assertNoSelection();
    }

    @Test
    public void testSelection_DoNothingOnUnselectableItem() {
        mIgnored.add(mItems.get(7));
        boolean selected = mHelper.select(mItems.get(7));

        assertFalse(selected);
        mSelection.assertNoSelection();
    }

    @Test
    public void testSelect_NotifiesListenersOfChange() {
        mHelper.select(mItems.get(7));

        mListener.assertSelectionChanged();
    }

    @Test
    public void testSelect_NotifiesAdapterOfSelect() {
        mHelper.select(mItems.get(7));

        mAdapter.assertNotifiedOfSelectionChange(7);
    }

    @Test
    public void testSelect_NotifiesAdapterOfDeselect() {
        mHelper.select(mItems.get(7));
        mAdapter.resetSelectionNotifications();
        mHelper.deselect(mItems.get(7));
        mAdapter.assertNotifiedOfSelectionChange(7);
    }

    @Test
    public void testDeselect_NotifiesSelectionChanged() {
        mHelper.select(mItems.get(7));
        mHelper.deselect(mItems.get(7));

        mListener.assertSelectionChanged();
    }

    @Test
    public void testSelection_PersistsOnUpdate() {
        mHelper.select(mItems.get(7));
        mAdapter.updateTestModelIds(mItems);

        mSelection.assertSelection(7);
    }

    @Test
    public void testSetItemsSelected() {
        mHelper.setItemsSelected(getStringIds(6, 7, 8), true);

        mSelection.assertRangeSelected(6, 8);
    }

    @Test
    public void testSetItemsSelected_SkipUnselectableItem() {
        mIgnored.add(mItems.get(7));

        mHelper.setItemsSelected(getStringIds(6, 7, 8), true);

        mSelection.assertSelected(6);
        mSelection.assertNotSelected(7);
        mSelection.assertSelected(8);
    }

    @Test
    public void testClear_RemovesPrimarySelection() {
        mHelper.select(mItems.get(1));
        mHelper.select(mItems.get(2));
        mHelper.clear();

        assertFalse(mHelper.hasSelection());
    }

    @Test
    public void testClear_RemovesProvisionalSelection() {
        Set<String> prov = new HashSet<>();
        prov.add(mItems.get(1));
        prov.add(mItems.get(2));
        mHelper.clear();
        // if there is a provisional selection, convert it to regular selection so we can poke it.
        mHelper.mergeProvisionalSelection();

        assertFalse(mHelper.hasSelection());
    }

    @Test
    public void testRangeSelection() {
        mHelper.startRange(15);
        mHelper.extendRange(19);
        mSelection.assertRangeSelection(15, 19);
    }

    @Test
    public void testRangeSelection_SkipUnselectableItem() {
        mIgnored.add(mItems.get(17));

        mHelper.startRange(15);
        mHelper.extendRange(19);

        mSelection.assertRangeSelected(15, 16);
        mSelection.assertNotSelected(17);
        mSelection.assertRangeSelected(18, 19);
    }

    @Test
    public void testRangeSelection_snapExpand() {
        mHelper.startRange(15);
        mHelper.extendRange(19);
        mHelper.extendRange(27);
        mSelection.assertRangeSelection(15, 27);
    }

    @Test
    public void testRangeSelection_snapContract() {
        mHelper.startRange(15);
        mHelper.extendRange(27);
        mHelper.extendRange(19);
        mSelection.assertRangeSelection(15, 19);
    }

    @Test
    public void testRangeSelection_snapInvert() {
        mHelper.startRange(15);
        mHelper.extendRange(27);
        mHelper.extendRange(3);
        mSelection.assertRangeSelection(3, 15);
    }

    @Test
    public void testRangeSelection_multiple() {
        mHelper.startRange(15);
        mHelper.extendRange(27);
        mHelper.endRange();
        mHelper.startRange(42);
        mHelper.extendRange(57);
        mSelection.assertSelectionSize(29);
        mSelection.assertRangeSelected(15, 27);
        mSelection.assertRangeSelected(42, 57);
    }

    @Test
    public void testProvisionalRangeSelection() {
        mHelper.startRange(13);
        mHelper.extendProvisionalRange(15);
        mSelection.assertRangeSelection(13, 15);
        mHelper.getSelection().mergeProvisionalSelection();
        mHelper.endRange();
        mSelection.assertSelectionSize(3);
    }

    @Test
    public void testProvisionalRangeSelection_endEarly() {
        mHelper.startRange(13);
        mHelper.extendProvisionalRange(15);
        mSelection.assertRangeSelection(13, 15);

        mHelper.endRange();
        // If we end range selection prematurely for provision selection, nothing should be selected
        // except the first item
        mSelection.assertSelectionSize(1);
    }

    @Test
    public void testProvisionalRangeSelection_snapExpand() {
        mHelper.startRange(13);
        mHelper.extendProvisionalRange(15);
        mSelection.assertRangeSelection(13, 15);
        mHelper.getSelection().mergeProvisionalSelection();
        mHelper.extendRange(18);
        mSelection.assertRangeSelection(13, 18);
    }

    @Test
    public void testCombinationRangeSelection_IntersectsOldSelection() {
        mHelper.startRange(13);
        mHelper.extendRange(15);
        mSelection.assertRangeSelection(13, 15);

        mHelper.startRange(11);
        mHelper.extendProvisionalRange(18);
        mSelection.assertRangeSelected(11, 18);
        mHelper.endRange();
        mSelection.assertRangeSelected(13, 15);
        mSelection.assertRangeSelected(11, 11);
        mSelection.assertSelectionSize(4);
    }

    @Test
    public void testProvisionalSelection() {
        Selection s = mHelper.getSelection();
        mSelection.assertNoSelection();

        // Mimicking band selection case -- BandController notifies item callback by itself.
        mListener.onItemStateChanged(mItems.get(1), true);
        mListener.onItemStateChanged(mItems.get(2), true);

        SparseBooleanArray provisional = new SparseBooleanArray();
        provisional.append(1, true);
        provisional.append(2, true);
        s.setProvisionalSelection(getItemIds(provisional));
        mSelection.assertSelection(1, 2);
    }

    @Test
    public void testProvisionalSelection_Replace() {
        Selection s = mHelper.getSelection();

        // Mimicking band selection case -- BandController notifies item callback by itself.
        mListener.onItemStateChanged(mItems.get(1), true);
        mListener.onItemStateChanged(mItems.get(2), true);
        SparseBooleanArray provisional = new SparseBooleanArray();
        provisional.append(1, true);
        provisional.append(2, true);
        s.setProvisionalSelection(getItemIds(provisional));

        mListener.onItemStateChanged(mItems.get(1), false);
        mListener.onItemStateChanged(mItems.get(2), false);
        provisional.clear();

        mListener.onItemStateChanged(mItems.get(3), true);
        mListener.onItemStateChanged(mItems.get(4), true);
        provisional.append(3, true);
        provisional.append(4, true);
        s.setProvisionalSelection(getItemIds(provisional));
        mSelection.assertSelection(3, 4);
    }

    @Test
    public void testProvisionalSelection_IntersectsExistingProvisionalSelection() {
        Selection s = mHelper.getSelection();

        // Mimicking band selection case -- BandController notifies item callback by itself.
        mListener.onItemStateChanged(mItems.get(1), true);
        mListener.onItemStateChanged(mItems.get(2), true);
        SparseBooleanArray provisional = new SparseBooleanArray();
        provisional.append(1, true);
        provisional.append(2, true);
        s.setProvisionalSelection(getItemIds(provisional));

        mListener.onItemStateChanged(mItems.get(1), false);
        mListener.onItemStateChanged(mItems.get(2), false);
        provisional.clear();

        mListener.onItemStateChanged(mItems.get(1), true);
        provisional.append(1, true);
        s.setProvisionalSelection(getItemIds(provisional));
        mSelection.assertSelection(1);
    }

    @Test
    public void testProvisionalSelection_Apply() {
        Selection s = mHelper.getSelection();

        // Mimicking band selection case -- BandController notifies item callback by itself.
        mListener.onItemStateChanged(mItems.get(1), true);
        mListener.onItemStateChanged(mItems.get(2), true);
        SparseBooleanArray provisional = new SparseBooleanArray();
        provisional.append(1, true);
        provisional.append(2, true);
        s.setProvisionalSelection(getItemIds(provisional));
        s.mergeProvisionalSelection();

        mSelection.assertSelection(1, 2);
    }

    @Test
    public void testProvisionalSelection_Cancel() {
        mHelper.select(mItems.get(1));
        mHelper.select(mItems.get(2));
        Selection s = mHelper.getSelection();

        SparseBooleanArray provisional = new SparseBooleanArray();
        provisional.append(3, true);
        provisional.append(4, true);
        s.setProvisionalSelection(getItemIds(provisional));
        s.clearProvisionalSelection();

        // Original selection should remain.
        mSelection.assertSelection(1, 2);
    }

    @Test
    public void testProvisionalSelection_IntersectsAppliedSelection() {
        mHelper.select(mItems.get(1));
        mHelper.select(mItems.get(2));
        Selection s = mHelper.getSelection();

        // Mimicking band selection case -- BandController notifies item callback by itself.
        mListener.onItemStateChanged(mItems.get(3), true);
        SparseBooleanArray provisional = new SparseBooleanArray();
        provisional.append(2, true);
        provisional.append(3, true);
        s.setProvisionalSelection(getItemIds(provisional));
        mSelection.assertSelection(1, 2, 3);
    }

    private Set<String> getItemIds(SparseBooleanArray selection) {
        Set<String> ids = new HashSet<>();

        int count = selection.size();
        for (int i = 0; i < count; ++i) {
            ids.add(mItems.get(selection.keyAt(i)));
        }

        return ids;
    }

    @Test
    public void testObserverOnChanged_NotifiesListenersOfChange() {
        mAdapter.notifyDataSetChanged();

        mListener.assertSelectionChanged();
    }

    private Iterable<String> getStringIds(int... ids) {
        List<String> stringIds = new ArrayList<>(ids.length);
        for (int id : ids) {
            stringIds.add(mItems.get(id));
        }
        return stringIds;
    }
}
