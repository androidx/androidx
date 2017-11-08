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

import java.util.List;

import androidx.recyclerview.selection.DefaultSelectionHelper;
import androidx.recyclerview.selection.EventBridge;
import androidx.recyclerview.selection.ItemKeyProvider;
import androidx.recyclerview.selection.SelectionHelper;
import androidx.recyclerview.selection.SelectionPredicates;

public final class SelectionHelpers {

    private SelectionHelpers() {}

    public static <K> SelectionHelper<K> createTestInstance(List<K> items) {
        TestAdapter<K> adapter = new TestAdapter<>(items);
        ItemKeyProvider<K> keyProvider =
                new TestItemKeyProvider<>(ItemKeyProvider.SCOPE_MAPPED, adapter);
        SelectionHelper<K> helper = new DefaultSelectionHelper<>(
                keyProvider,
                SelectionPredicates.selectAnything());

        EventBridge.install(adapter, helper, keyProvider);

        return helper;
    }
}
