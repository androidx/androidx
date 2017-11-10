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

import android.support.annotation.Nullable;
import android.support.annotation.RestrictTo;
import android.support.annotation.VisibleForTesting;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * Object representing the current selection and provisional selection. Provides read only public
 * access, and private write access.
 * <p>
 * This class tracks selected items by managing two sets:
 *
 * <li>primary selection
 *
 * Primary selection consists of items tapped by a user or by lassoed by band select operation.
 *
 * <li>provisional selection
 *
 * Provisional selections are selections which have been temporarily created
 * by an in-progress band select or gesture selection. Once the user releases the mouse button
 * or lifts their finger the corresponding provisional selection should be converted into
 * primary selection.
 *
 * <p>The total selection is the combination of
 * both the core selection and the provisional selection. Tracking both separately is necessary to
 * ensure that items in the core selection are not "erased" from the core selection when they
 * are temporarily included in a secondary selection (like band selection).
 *
 * @param <K> Selection key type. Usually String or Long.
 *
 * @hide
 */
@RestrictTo(LIBRARY_GROUP)
public class Selection<K> implements Iterable<K> {

    // NOTE: Not currently private as DefaultSelectionHelper directly manipulates values.
    final Set<K> mSelection;
    final Set<K> mProvisionalSelection;

    Selection() {
        mSelection = new HashSet<>();
        mProvisionalSelection = new HashSet<>();
    }

    /**
     * Used by {@link SelectionStorage} when restoring selection.
     */
    Selection(Set<K> selection) {
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
    Map<K, Boolean> setProvisionalSelection(Set<K> newSelection) {
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
    @VisibleForTesting
    protected void mergeProvisionalSelection() {
        mSelection.addAll(mProvisionalSelection);
        mProvisionalSelection.clear();
    }

    /**
     * Abandons the existing provisional selection so that all items provisionally selected are
     * now deselected.
     */
    @VisibleForTesting
    void clearProvisionalSelection() {
        mProvisionalSelection.clear();
    }

    /**
     * Adds a new item to the primary selection.
     *
     * @return true if the operation resulted in a modification to the selection.
     */
    boolean add(K key) {
        if (mSelection.contains(key)) {
            return false;
        }

        mSelection.add(key);
        return true;
    }

    /**
     * Removes an item from the primary selection.
     *
     * @return true if the operation resulted in a modification to the selection.
     */
    boolean remove(K key) {
        if (!mSelection.contains(key)) {
            return false;
        }

        mSelection.remove(key);
        return true;
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
    void copyFrom(Selection<K> source) {
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
        if (this == other) {
            return true;
        }

        return other instanceof Selection && isEqualTo((Selection) other);
    }

    private boolean isEqualTo(Selection other) {
        return mSelection.equals(other.mSelection)
                && mProvisionalSelection.equals(other.mProvisionalSelection);
    }
}
