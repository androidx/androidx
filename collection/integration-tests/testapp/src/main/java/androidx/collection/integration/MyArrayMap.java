/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.collection.integration;

import androidx.annotation.NonNull;
import androidx.collection.ArrayMap;

import java.util.Collection;
import java.util.Set;

/** Integration (actually build) test that ArrayMap can be subclassed. */
class MyArrayMap<K, V> extends ArrayMap<K, V> {
    @NonNull
    @Override
    public Set<Entry<K, V>> entrySet() {
        return super.entrySet();
    }

    @Override
    public boolean isEmpty() {
        return super.isEmpty();
    }

    @Override
    public int size() {
        return super.size();
    }
    @NonNull
    @Override
    public Set<K> keySet() {
        return super.keySet();
    }

    @NonNull
    @Override
    public Collection<V> values() {
        return super.values();
    }
}
