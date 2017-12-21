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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.os.Bundle;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import androidx.recyclerview.selection.testing.Bundles;
import androidx.recyclerview.selection.testing.SelectionHelpers;
import androidx.recyclerview.selection.testing.TestData;

@RunWith(AndroidJUnit4.class)
@SmallTest
public final class SelectionStorage_StringsTest {

    private SelectionHelper<String> mSelectionHelper;
    private SelectionStorage<String> mSelectionStorage;
    private Bundle mBundle;

    @Before
    public void setUp() {
        mSelectionHelper = SelectionHelpers.createTestInstance(TestData.createStringData(100));
        mSelectionStorage = new SelectionStorage<>(
                SelectionStorage.TYPE_STRING, mSelectionHelper);
        mBundle = new Bundle();
    }

    @Test
    public void testWritesSelectionToBundle() {
        mSelectionHelper.select("3");
        mSelectionStorage.onSaveInstanceState(mBundle);
        Bundle out = Bundles.forceParceling(mBundle);

        assertTrue(mBundle.containsKey(SelectionStorage.EXTRA_SAVED_SELECTION_ENTRIES));
    }

    @Test
    public void testRestoresFromSelectionInBundle() {
        mSelectionHelper.select("3");
        mSelectionHelper.select("13");
        mSelectionHelper.select("33");

        MutableSelection orig = new MutableSelection();
        mSelectionHelper.copySelection(orig);
        mSelectionStorage.onSaveInstanceState(mBundle);
        Bundle out = Bundles.forceParceling(mBundle);

        mSelectionHelper.clearSelection();

        mSelectionStorage.onRestoreInstanceState(mBundle);
        MutableSelection restored = new MutableSelection();
        mSelectionHelper.copySelection(restored);
        assertEquals(orig, restored);
    }

    @Test
    public void testIgnoresNullBundle() {
        mSelectionStorage.onRestoreInstanceState(null);
    }
}
