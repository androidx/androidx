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

import androidx.recyclerview.selection.DefaultSelectionTracker;
import androidx.recyclerview.selection.EventBridge;
import androidx.recyclerview.selection.ItemKeyProvider;
import androidx.recyclerview.selection.SelectionPredicates;
import androidx.recyclerview.selection.SelectionTracker;
import androidx.recyclerview.selection.StorageStrategy;

public final class SelectionTrackers {

    private SelectionTrackers() {}

    public static SelectionTracker<String> createStringTracker(String id, int numItems) {
        TestAdapter<String> adapter = new TestAdapter<>(TestData.createStringData(numItems));
        ItemKeyProvider<String> keyProvider =
                new TestItemKeyProvider<>(ItemKeyProvider.SCOPE_MAPPED, adapter);
        SelectionTracker<String> tracker = new DefaultSelectionTracker<>(
                id,
                keyProvider,
                SelectionPredicates.createSelectAnything(),
                StorageStrategy.createStringStorage());

        EventBridge.install(adapter, tracker, keyProvider);

        return tracker;
    }

    public static SelectionTracker<Long> createLongTracker(String id, int numItems) {
        TestAdapter<Long> adapter = new TestAdapter<>(TestData.createLongData(numItems));
        ItemKeyProvider<Long> keyProvider =
                new TestItemKeyProvider<>(ItemKeyProvider.SCOPE_MAPPED, adapter);
        SelectionTracker<Long> tracker = new DefaultSelectionTracker<>(
                id,
                keyProvider,
                SelectionPredicates.createSelectAnything(),
                StorageStrategy.createLongStorage());

        EventBridge.install(adapter, tracker, keyProvider);

        return tracker;
    }
}
