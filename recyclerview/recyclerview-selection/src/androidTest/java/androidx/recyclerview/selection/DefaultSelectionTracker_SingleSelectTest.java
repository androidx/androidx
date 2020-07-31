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

import androidx.recyclerview.selection.testing.SelectionProbe;
import androidx.recyclerview.selection.testing.TestAdapter;
import androidx.recyclerview.selection.testing.TestItemKeyProvider;
import androidx.recyclerview.selection.testing.TestSelectionObserver;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class DefaultSelectionTracker_SingleSelectTest {

    private SelectionTracker<String> mTracker;
    private TestAdapter<String> mAdapter;
    private TestSelectionObserver<String> mListener;
    private SelectionProbe mSelection;

    @Before
    public void setUp() throws Exception {
        mAdapter = TestAdapter.createStringAdapter(100);
        mListener = new TestSelectionObserver<>();

        ItemKeyProvider<String> keyProvider =
                new TestItemKeyProvider<>(ItemKeyProvider.SCOPE_MAPPED, mAdapter);
        mTracker = new DefaultSelectionTracker<>(
                "single-selection-test",
                keyProvider,
                SelectionPredicates.createSelectSingleAnything(),
                StorageStrategy.createStringStorage());

        mTracker.addObserver(mListener);

        mSelection = new SelectionProbe(mTracker);
    }

    @Test
    public void testSimpleSelect() {
        mTracker.select(mAdapter.getSelectionKey(3));
        mTracker.select(mAdapter.getSelectionKey(4));
        mListener.assertSelectionChanged();
        // 3 should no longer be selected because of single select mode.
        mSelection.assertSelection(4);
    }

    @Test
    public void testRangeSelectionNotEstablished() {
        mTracker.select(mAdapter.getSelectionKey(3));
        mListener.reset();

        mTracker.extendRange(10);

        mListener.assertSelectionUnchanged();
        mSelection.assertSelection(3);
    }

    @Test
    public void testProvisionalRangeSelection_Ignored() {
        mTracker.startRange(13);
        mTracker.extendProvisionalRange(15);
        mSelection.assertSelection(13);
    }
}
