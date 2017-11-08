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

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.annotation.RestrictTo;

import java.util.Set;

/**
 * SelectionManager provides support for managing selection within a RecyclerView instance.
 *
 * @see DefaultSelectionHelper for details on instantiation.
 *
 * @param <K> Selection key type. Usually String or Long.
 *
 * @hide
 */
@RestrictTo(LIBRARY_GROUP)
public abstract class SelectionHelper<K> {

    /**
     * This value is included in the payload when SelectionHelper implementations
     * notify RecyclerView of changes. Clients can look for this in
     * {@code onBindViewHolder} to know if the bind event is occurring in response
     * to a selection state change.
     */
    public static final String SELECTION_CHANGED_MARKER = "Selection-Changed";

    /**
     * Adds {@code observer} to be notified when changes to selection occur.
     * This method allows observers to closely track changes to selection
     * avoiding the need to poll selection at performance critical points.
     */
    public abstract void addObserver(SelectionObserver observer);

    /** @return true if has a selection */
    public abstract boolean hasSelection();

    /**
     * Returns a Selection object that provides a live view on the current selection.
     *
     * @return The current selection.
     * @see #copySelection(Selection) on how to get a snapshot
     * of the selection that will not reflect future changes
     * to selection.
     */
    public abstract Selection getSelection();

    /**
     * Updates {@code dest} to reflect the current selection.
     */
    public abstract void copySelection(Selection dest);

    /**
     * @return true if the item specified by its id is selected. Shorthand for
     * {@code getSelection().contains(K)}.
     */
    public abstract boolean isSelected(@Nullable K key);

    /**
     * Restores the selected state of specified items. Used in cases such as restore the selection
     * after rotation etc. Provisional selection, being provisional 'n all, isn't restored.
     *
     * <p>This affords clients the ability to restore selection from selection saved
     * in Activity state. See {@link android.app.Activity#onCreate(Bundle)}.
     *
     * @param savedSelection selection being restored.
     */
    public abstract void restoreSelection(Selection savedSelection);

    abstract void onDataSetChanged();

    /**
     * Clears both primary selection and provisional selection.
     *
     * @return true if anything changed.
     */
    public abstract boolean clear();

    /**
     * Clears the selection and notifies (if something changes).
     */
    public abstract void clearSelection();

    /**
     * Sets the selected state of the specified items. Note that the callback will NOT
     * be consulted to see if an item can be selected.
     */
    public abstract boolean setItemsSelected(Iterable<K> keys, boolean selected);

    /**
     * Attempts to select an item.
     *
     * @return true if the item was selected. False if the item was not selected, or was
     * was already selected prior to the method being called.
     */
    public abstract boolean select(K key);

    /**
     * Attempts to deselect an item.
     *
     * @return true if the item was deselected. False if the item was not deselected, or was
     * was already deselected prior to the method being called.
     */
    public abstract boolean deselect(K key);

    /**
     * Selects the item at position and establishes the "anchor" for a range selection,
     * replacing any existing range anchor.
     *
     * @param position The anchor position for the selection range.
     */
    public abstract void startRange(int position);

    /**
     * Sets the end point for the active range selection.
     *
     * <p>This function should only be called when a range selection is active
     * (see {@link #isRangeActive()}. Items in the range [anchor, end] will be
     * selected.
     *
     * @param position  The new end position for the selection range.
     * @throws IllegalStateException if a range selection is not active. Range selection
     *         must have been started by a call to {@link #startRange(int)}.
     */
    public abstract void extendRange(int position);

    /**
     * Stops an in-progress range selection. All selection done with
     * {@link #extendProvisionalRange(int)} will be lost if
     * {@link Selection#mergeProvisionalSelection()} is not called beforehand.
     */
    public abstract void endRange();

    /**
     * @return Whether or not there is a current range selection active.
     */
    public abstract boolean isRangeActive();

    /**
     * Establishes the "anchor" at which a selection range begins. This "anchor" is consulted
     * when determining how to extend, and modify selection ranges. Calling this when a
     * range selection is active will reset the range selection.
     *
     * @param position the anchor position. Must already be selected.
     */
    protected abstract void anchorRange(int position);

    /**
     * @param position
     */
    // TODO: This is smelly. Maybe this type of logic needs to move into range selection,
    // then selection manager can have a startProvisionalRange and startRange. Or
    // maybe ranges always start life as provisional.
    protected abstract void extendProvisionalRange(int position);

    /**
     * Sets the provisional selection, replacing any existing selection.
     * @param newSelection
     */
    public abstract void setProvisionalSelection(Set<K> newSelection);

    /** Clears any existing provisional selection */
    public abstract void clearProvisionalSelection();

    /**
     * Converts the provisional selection into primary selection, then clears
     * provisional selection.
     */
    public abstract void mergeProvisionalSelection();

    /**
     * Observer interface providing access to information about Selection state changes.
     *
     * @param <K> Selection key type. Usually String or Long.
     *
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    public abstract static class SelectionObserver<K> {

        /**
         * Called when state of an item has been changed.
         */
        public void onItemStateChanged(K key, boolean selected) {
        }

        /**
         * Called when the underlying data set has change. After this method is called
         * the selection manager will attempt traverse the existing selection,
         * calling {@link #onItemStateChanged(K, boolean)} for each selected item,
         * and deselecting any items that cannot be selected given the updated dataset.
         */
        public void onSelectionReset() {
        }

        /**
         * Called immediately after completion of any set of changes, excluding
         * those resulting in calls to {@link #onSelectionReset()} and
         * {@link #onSelectionRestored()}.
         */
        public void onSelectionChanged() {
        }

        /**
         * Called immediately after selection is restored.
         * {@link #onItemStateChanged(K, boolean)} will not be called
         * for individual items in the selection.
         */
        public void onSelectionRestored() {
        }
    }

    /**
     * Implement SelectionPredicate to control when items can be selected or unselected.
     *
     * @param <K> Selection key type. Usually String or Long.
     *
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    public abstract static class SelectionPredicate<K> {

        /** @return true if the item at {@code id} can be set to {@code nextState}. */
        public abstract boolean canSetStateForKey(K key, boolean nextState);

        /** @return true if the item at {@code id} can be set to {@code nextState}. */
        public abstract boolean canSetStateAtPosition(int position, boolean nextState);

        /** @return true if more than a single item can be selected. */
        public abstract boolean canSelectMultiple();
    }
}
