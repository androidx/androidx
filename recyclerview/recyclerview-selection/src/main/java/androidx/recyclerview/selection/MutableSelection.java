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

import androidx.annotation.NonNull;

/**
 * Subclass of {@link Selection} exposing public support for mutating the underlying
 * selection data. This is useful for clients of {@link SelectionTracker} that wish to
 * manipulate a copy of selection data obtained via
 * {@link SelectionTracker#copySelection(MutableSelection)}.
 *
 * <p>
 * While the {@link Selection} class is not intrinsically immutable, it is not mutable
 * by non-library code. Furthermore the value returned from {@link SelectionTracker#getSelection()}
 * is a live view of the underlying selection, mutable by the library itself.
 *
 * <p>
 * {@link MutableSelection} allows clients to obtain a mutable copy of the Selection
 * state held by the selection library. This is useful in situations where a stable
 * snapshot of the selection is required.
 *
 *
 * <p><b>Example</b>
 *
 * <p>
 * <pre>
 * MutableSelection snapshot = new MutableSelection();
 * selectionTracker.copySelection(snapshot);
 *
 * // Clear the user visible selection.
 * selectionTracker.clearSelection();
 * // tracker.getSelection().isEmpty() will be true.
 * // shapshot has a copy of the previous selection.
 * </pre>
 *
 * @see android.text.Selection
 *
 * @param <K> Selection key type. @see {@link StorageStrategy} for supported types.
 */
public final class MutableSelection<K> extends Selection<K> {

    @Override
    public boolean add(@NonNull K key) {
        return super.add(key);
    }

    @Override
    public boolean remove(@NonNull K key) {
        return super.remove(key);
    }

    @Override
    public void copyFrom(@NonNull Selection<K> source) {
        super.copyFrom(source);
    }

    @Override
    public void clear() {
        super.clear();
    }
}
