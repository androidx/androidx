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
import androidx.annotation.Nullable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * Object representing a "primary" selection and a "provisional" selection.
 *
 * <p>
 * This class tracks selected items by managing two sets:
 *
 * <p>
 * <b>Primary Selection</b>
 *
 * <p>
 * Primary selection consists of items selected by a user. This represents the selection
 * "at rest", as the selection does not contains items that are in a "provisional" selected
 * state created by way of an ongoing gesture or band operation.
 *
 * <p>
 * <b>Provisional Selection</b>
 *
 * <p>
 * Provisional selections are selections which are interim in nature.
 *
 * <p>
 * Provisional selection exists to address issues where a transitory selection might
 * momentarily intersect with a previously established selection resulting in a some
 * or all of the established selection being erased. Such situations may arise
 * when band selection is being performed in "additive" mode (e.g. SHIFT or CTRL is pressed
 * on the keyboard prior to mouse down), or when there's an active gesture selection
 * (which can be initiated by long pressing an unselected item while there is an
 * existing selection).
 *
 * @see MutableSelection
 *
 * @param <K> Selection key type. @see {@link StorageStrategy} for supported types.
 */
public class Selection<K> implements Iterable<K> {

    // NOTE: Not currently private as DefaultSelectionTracker directly manipulates values.
    final Set<K> mSelection;
    final Set<K> mProvisionalSelection;

    Selection() {
        mSelection = new HashSet<>();
        mProvisionalSelection = new HashSet<>();
    }

    /**
     * Used by {@link StorageStrategy} when restoring selection.
     */
    Selection(@NonNull Set<K> selection) {
        mSelection = selection;
        mProvisionalSelection = new HashSet<>();
    }

    /**
     * @param key
     * @return true if the position is currently selected.
     */
    public boolean contains(@Nullable K key) {
        return mSelection.contains(key) || mProvisionalSelection.contains(key);
    }

    /**
     * Returns an {@link Iterator} that iterators over the selection, *excluding*
     * any provisional selection.
     *
     * {@inheritDoc}
     */
    @Override
    public Iterator<K> iterator() {
        return mSelection.iterator();
    }

    /**
     * @return size of the selection including both final and provisional selected items.
     */
    public int size() {
        return mSelection.size() + mProvisionalSelection.size();
    }

    /**
     * @return true if the selection is empty.
     */
    public boolean isEmpty() {
        return mSelection.isEmpty() && mProvisionalSelection.isEmpty();
    }

    /**
     * Sets the provisional selection, which is a temporary selection that can be saved,
     * canceled, or adjusted at a later time. When a new provision selection is applied, the old
     * one (if it exists) is abandoned.
     * @return Map of ids added or removed. Added ids have a value of true, removed are false.
     */
    Map<K, Boolean> setProvisionalSelection(@NonNull Set<K> newSelection) {
        Map<K, Boolean> delta = new HashMap<>();

        for (K key: mProvisionalSelection) {
            // Mark each item that used to be in the provisional selection
            // but is not in the new provisional selection.
            if (!newSelection.contains(key) && !mSelection.contains(key)) {
                delta.put(key, false);
            }
        }

        for (K key: mSelection) {
            // Mark each item that used to be in the selection but is unsaved and not in the new
            // provisional selection.
            if (!newSelection.contains(key)) {
                delta.put(key, false);
            }
        }

        for (K key: newSelection) {
            // Mark each item that was not previously in the selection but is in the new
            // provisional selection.
            if (!mSelection.contains(key) && !mProvisionalSelection.contains(key)) {
                delta.put(key, true);
            }
        }

        // Now, iterate through the changes and actually add/remove them to/from the current
        // selection. This could not be done in the previous loops because changing the size of
        // the selection mid-iteration changes iteration order erroneously.
        for (Map.Entry<K, Boolean> entry: delta.entrySet()) {
            K key = entry.getKey();
            if (entry.getValue()) {
                mProvisionalSelection.add(key);
            } else {
                mProvisionalSelection.remove(key);
            }
        }

        return delta;
    }

    /**
     * Saves the existing provisional selection. Once the provisional selection is saved,
     * subsequent provisional selections which are different from this existing one cannot
     * cause items in this existing provisional selection to become deselected.
     */
    void mergeProvisionalSelection() {
        mSelection.addAll(mProvisionalSelection);
        mProvisionalSelection.clear();
    }

    /**
     * Abandons the existing provisional selection so that all items provisionally selected are
     * now deselected.
     */
    void clearProvisionalSelection() {
        mProvisionalSelection.clear();
    }

    /**
     * Adds a new item to the primary selection.
     *
     * @return true if the operation resulted in a modification to the selection.
     */
    boolean add(@NonNull K key) {
        return mSelection.add(key);
    }

    /**
     * Removes an item from the primary selection.
     *
     * @return true if the operation resulted in a modification to the selection.
     */
    boolean remove(@NonNull K key) {
        return mSelection.remove(key);
    }

    /**
     * Clears the primary selection. The provisional selection, if any, is unaffected.
     */
    void clear() {
        mSelection.clear();
    }

    /**
     * Clones primary and provisional selection from supplied {@link Selection}.
     * Does not copy active range data.
     */
    void copyFrom(@NonNull Selection<K> source) {
        mSelection.clear();
        mSelection.addAll(source.mSelection);

        mProvisionalSelection.clear();
        mProvisionalSelection.addAll(source.mProvisionalSelection);
    }

    @Override
    public String toString() {
        if (size() <= 0) {
            return "size=0, items=[]";
        }

        StringBuilder buffer = new StringBuilder(size() * 28);
        buffer.append("Selection{")
            .append("primary{size=" + mSelection.size())
            .append(", entries=" + mSelection)
            .append("}, provisional{size=" + mProvisionalSelection.size())
            .append(", entries=" + mProvisionalSelection)
            .append("}}");
        return buffer.toString();
    }

    @Override
    public int hashCode() {
        return mSelection.hashCode() ^ mProvisionalSelection.hashCode();
    }

    @Override
    public boolean equals(Object other) {
        return (this == other)
                || (other instanceof Selection && isEqualTo((Selection) other));
    }

    private boolean isEqualTo(Selection other) {
        return mSelection.equals(other.mSelection)
                && mProvisionalSelection.equals(other.mProvisionalSelection);
    }
}
