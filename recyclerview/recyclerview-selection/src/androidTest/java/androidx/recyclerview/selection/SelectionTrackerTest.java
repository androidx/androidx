/*
 * Copyright 2019 The Android Open Source Project
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

import android.os.Looper;

import androidx.recyclerview.selection.testing.TestAdapter;
import androidx.recyclerview.selection.testing.TestData;
import androidx.recyclerview.selection.testing.TestEvents;
import androidx.recyclerview.selection.testing.TestItemDetailsLookup;
import androidx.recyclerview.selection.testing.TestItemKeyProvider;
import androidx.recyclerview.widget.RecyclerView;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.HashSet;
import java.util.Set;

/**
 * Integration test guaranteeing that SelectionTracker correctly orchestrates
 * the handling of CANCEL events. The only external signal of this is the
 * clearing of provisional selection.
 */
@RunWith(AndroidJUnit4.class)
@SmallTest
public final class SelectionTrackerTest {
    private RecyclerView mRecyclerView;
    private SelectionTracker<String> mTracker;

    @Before
    public void setUp() {
        if (Looper.myLooper() == null) {
            Looper.prepare();
        }

        TestAdapter<String> adapter = new TestAdapter<>(TestData.createStringData(10));
        ItemKeyProvider<String> keyProvider =
                new TestItemKeyProvider<>(ItemKeyProvider.SCOPE_MAPPED, adapter);

        mRecyclerView = new RecyclerView(ApplicationProvider.getApplicationContext());
        mRecyclerView.setAdapter(adapter);
        mTracker = new SelectionTracker.Builder<String>(
            "test-tracker", mRecyclerView, keyProvider,
                new TestItemDetailsLookup(),
                StorageStrategy.createStringStorage())
                .build();
    }

    @Test
    public void testReset_ClearsSelection() {
        mTracker.select("3");
        mTracker.select("13");
        mTracker.select("33");
        mRecyclerView.dispatchTouchEvent(TestEvents.Unknown.CANCEL);
        assertFalse(mTracker.isSelected("3"));
        assertFalse(mTracker.isSelected("13"));
        assertFalse(mTracker.isSelected("33"));
    }

    @Test
    public void testReset_ClearsProvisionalSelection() {
        Set<String> items = new HashSet<String>(3);
        items.add("3");
        items.add("13");
        items.add("33");
        mTracker.setProvisionalSelection(items);
        mRecyclerView.dispatchTouchEvent(TestEvents.Unknown.CANCEL);
        assertFalse(mTracker.isSelected("3"));
        assertFalse(mTracker.isSelected("13"));
        assertFalse(mTracker.isSelected("33"));
    }
}
