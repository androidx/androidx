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

package androidx.recyclerview.selection.testing;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import androidx.recyclerview.selection.DefaultSelectionTracker;
import androidx.recyclerview.selection.Selection;
import androidx.recyclerview.selection.SelectionTracker;

/**
 * Helper class for making assertions against the state of a {@link DefaultSelectionTracker}
 * instance and the consistency of states between {@link DefaultSelectionTracker} and
 * {@link DefaultSelectionTracker.SelectionObserver}.
 */
public final class SelectionProbe {

    private final SelectionTracker<String> mMgr;
    private final TestSelectionObserver<String> mSelectionListener;

    public SelectionProbe(SelectionTracker<String> mgr) {
        mMgr = mgr;
        mSelectionListener = new TestSelectionObserver<String>();
        mMgr.addObserver(mSelectionListener);
    }

    public SelectionProbe(
            SelectionTracker<String> mgr, TestSelectionObserver<String> selectionListener) {
        mMgr = mgr;
        mSelectionListener = selectionListener;
    }

    public void assertRangeSelected(int begin, int end) {
        for (int i = begin; i <= end; i++) {
            assertSelected(i);
        }
    }

    public void assertRangeNotSelected(int begin, int end) {
        for (int i = begin; i <= end; i++) {
            assertNotSelected(i);
        }
    }

    public void assertRangeSelection(int begin, int end) {
        assertSelectionSize(end - begin + 1);
        assertRangeSelected(begin, end);
    }

    public void assertSelectionSize(int expected) {
        Selection<String> selection = mMgr.getSelection();
        assertEquals(selection.toString(), expected, selection.size());

        mSelectionListener.assertSelectionSize(expected);
    }

    public void assertNoSelection() {
        assertSelectionSize(0);

        mSelectionListener.assertNoSelection();
    }

    public void assertSelection(int... ids) {
        assertSelected(ids);
        assertEquals(ids.length, mMgr.getSelection().size());

        mSelectionListener.assertSelectionSize(ids.length);
    }

    public void assertSelected(int... ids) {
        Selection<String> sel = mMgr.getSelection();
        for (int id : ids) {
            String sid = String.valueOf(id);
            assertTrue(sid + " is not in selection " + sel, sel.contains(sid));

            mSelectionListener.assertSelected(sid);
        }
    }

    public void assertNotSelected(int... ids) {
        Selection<String> sel = mMgr.getSelection();
        for (int id : ids) {
            String sid = String.valueOf(id);
            assertFalse(sid + " is in selection " + sel, sel.contains(sid));

            mSelectionListener.assertNotSelected(sid);
        }
    }
}
