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

package androidx.recyclerview.selection;

import static org.junit.Assert.assertEquals;

import android.os.Bundle;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import androidx.recyclerview.selection.testing.Bundles;
import androidx.recyclerview.selection.testing.SelectionTrackers;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests guaranteeing that two distinct selections can be stored side-by-side.
 */
@RunWith(AndroidJUnit4.class)
@SmallTest
public final class SelectionTracker_InstanceStateTest {

    private SelectionTracker<String> mStringTracker;
    private SelectionTracker<Long> mLongTracker;

    private Bundle mBundle;

    @Before
    public void setUp() {
        mStringTracker = SelectionTrackers.createStringTracker("tracker-isolation-strings", 100);
        mLongTracker = SelectionTrackers.createLongTracker("tracker-isolation-longs", 1000);
        mBundle = new Bundle();
    }

    @Test
    public void testMaintainsDistinctSelections() {
        mStringTracker.select("3");
        mStringTracker.select("13");
        mStringTracker.select("33");

        mLongTracker.select(100L);
        mLongTracker.select(200L);
        mLongTracker.select(300L);

        MutableSelection<String> origStrings = new MutableSelection<>();
        MutableSelection<Long> origLongs = new MutableSelection<>();

        mStringTracker.copySelection(origStrings);
        mLongTracker.copySelection(origLongs);

        mStringTracker.onSaveInstanceState(mBundle);
        mLongTracker.onSaveInstanceState(mBundle);

        Bundle parceled = Bundles.forceParceling(mBundle);

        mStringTracker.clearSelection();
        mLongTracker.clearSelection();

        mStringTracker.onRestoreInstanceState(parceled);
        mLongTracker.onRestoreInstanceState(parceled);

        assertEquals(origStrings, mStringTracker.getSelection());
        assertEquals(origLongs, mLongTracker.getSelection());
    }
}
