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

import static android.support.annotation.RestrictTo.Scope.LIBRARY_GROUP;

import android.support.annotation.RestrictTo;

/**
 * Subclass of Selection exposing public support for mutating the underlying selection data.
 * This is useful for clients of {@link SelectionHelper} that wish to manipulate
 * a copy of selection data obtained via {@link SelectionHelper#copySelection(Selection)}.
 *
 * @param <K> Selection key type. Usually String or Long.
 *
 * @hide
 */
@RestrictTo(LIBRARY_GROUP)
public final class MutableSelection<K> extends Selection<K> {

    @Override
    public boolean add(K key) {
        return super.add(key);
    }

    @Override
    public boolean remove(K key) {
        return super.remove(key);
    }

    @Override
    public void copyFrom(Selection<K> source) {
        super.copyFrom(source);
    }

    @Override
    public void clear() {
        super.clear();
    }
}
