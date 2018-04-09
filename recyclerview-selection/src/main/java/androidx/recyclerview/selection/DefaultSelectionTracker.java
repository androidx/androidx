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

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP;
import static androidx.core.util.Preconditions.checkArgument;
import static androidx.core.util.Preconditions.checkState;
import static androidx.recyclerview.selection.Shared.DEBUG;

import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.annotation.VisibleForTesting;
import androidx.recyclerview.selection.Range.RangeType;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.AdapterDataObserver;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * {@link SelectionTracker} providing support for traditional multi-item selection on top
 * of {@link RecyclerView}.
 *
 * <p>
 * The class supports running in a single-select mode, which can be enabled using
 * {@link SelectionPredicate#canSelectMultiple()}.
 *
 * @param <K> Selection key type. @see {@link StorageStrategy} for supported types.
 *
 * @hide
 */
@RestrictTo(LIBRARY_GROUP)
public class DefaultSelectionTracker<K> extends SelectionTracker<K> {

    private static final String TAG = "DefaultSelectionTracker";
    private static final String EXTRA_SELECTION_PREFIX = "androidx.recyclerview.selection";

    private final Selection<K> mSelection = new Selection<>();
    private final List<SelectionObserver> mObservers = new ArrayList<>(1);
    private final ItemKeyProvider<K> mKeyProvider;
    private final SelectionPredicate<K> mSelectionPredicate;
    private final StorageStrategy<K> mStorage;
    private final RangeCallbacks mRangeCallbacks;
    private final AdapterObserver mAdapterObserver;
    private final boolean mSingleSelect;
    private final String mSelectionId;

    private @Nullable Range mRange;

    /**
     * Creates a new instance.
     *
     * @param selectionId A unique string identifying this selection in the context
     *        of the activity or fragment.
     * @param keyProvider client supplied class providing access to stable ids.
     * @param selectionPredicate A predicate allowing the client to disallow selection
     * @param storage Strategy for storing typed selection in bundle.
     */
    public DefaultSelectionTracker(
            @NonNull String selectionId,
            @NonNull ItemKeyProvider keyProvider,
            @NonNull SelectionPredicate selectionPredicate,
            @NonNull StorageStrategy<K> storage) {

        checkArgument(selectionId != null);
        checkArgument(!selectionId.trim().isEmpty());
        checkArgument(keyProvider != null);
        checkArgument(selectionPredicate != null);
        checkArgument(storage != null);

        mSelectionId = selectionId;
        mKeyProvider = keyProvider;
        mSelectionPredicate = selectionPredicate;
        mStorage = storage;

        mRangeCallbacks = new RangeCallbacks();

        mSingleSelect = !selectionPredicate.canSelectMultiple();

        mAdapterObserver = new AdapterObserver(this);
    }

    @Override
    public void addObserver(@NonNull SelectionObserver callback) {
        checkArgument(callback != null);
        mObservers.add(callback);
    }

    @Override
    public boolean hasSelection() {
        return !mSelection.isEmpty();
    }

    @Override
    public Selection getSelection() {
        return mSelection;
    }

    @Override
    public void copySelection(@NonNull MutableSelection dest) {
        dest.copyFrom(mSelection);
    }

    @Override
    public boolean isSelected(@Nullable K key) {
        return mSelection.contains(key);
    }

    @Override
    protected void restoreSelection(@NonNull Selection other) {
        checkArgument(other != null);
        setItemsSelectedQuietly(other.mSelection, true);
        // NOTE: We intentionally don't restore provisional selection. It's provisional.
        notifySelectionRestored();
    }

    @Override
    public boolean setItemsSelected(@NonNull Iterable<K> keys, boolean selected) {
        boolean changed = setItemsSelectedQuietly(keys, selected);
        notifySelectionChanged();
        return changed;
    }

    private boolean setItemsSelectedQuietly(@NonNull Iterable<K> keys, boolean selected) {
        boolean changed = false;
        for (K key: keys) {
            boolean itemChanged = selected
                    ? canSetState(key, true) && mSelection.add(key)
                    : canSetState(key, false) && mSelection.remove(key);
            if (itemChanged) {
                notifyItemStateChanged(key, selected);
            }
            changed |= itemChanged;
        }
        return changed;
    }

    @Override
    public boolean clearSelection() {
        if (!hasSelection()) {
            return false;
        }

        clearProvisionalSelection();
        clearPrimarySelection();
        return true;
    }

    private void clearPrimarySelection() {
        if (!hasSelection()) {
            return;
        }

        Selection prev = clearSelectionQuietly();
        notifySelectionCleared(prev);
        notifySelectionChanged();
    }

    /**
     * Clears the selection, without notifying selection listeners.
     * Returns items in previous selection. Callers are responsible for notifying
     * listeners about changes.
     */
    private Selection clearSelectionQuietly() {
        mRange = null;

        MutableSelection prevSelection = new MutableSelection();
        if (hasSelection()) {
            copySelection(prevSelection);
            mSelection.clear();
        }

        return prevSelection;
    }

    @Override
    public boolean select(@NonNull K key) {
        checkArgument(key != null);

        if (mSelection.contains(key)) {
            return false;
        }

        if (!canSetState(key, true)) {
            if (DEBUG) Log.d(TAG, "Select cancelled by selection predicate test.");
            return false;
        }

        // Enforce single selection policy.
        if (mSingleSelect && hasSelection()) {
            Selection prev = clearSelectionQuietly();
            notifySelectionCleared(prev);
        }

        mSelection.add(key);
        notifyItemStateChanged(key, true);
        notifySelectionChanged();

        return true;
    }

    @Override
    public boolean deselect(@NonNull K key) {
        checkArgument(key != null);

        if (mSelection.contains(key)) {
            if (!canSetState(key, false)) {
                if (DEBUG) Log.d(TAG, "Deselect cancelled by selection predicate test.");
                return false;
            }
            mSelection.remove(key);
            notifyItemStateChanged(key, false);
            notifySelectionChanged();
            if (mSelection.isEmpty() && isRangeActive()) {
                // if there's nothing in the selection and there is an active ranger it results
                // in unexpected behavior when the user tries to start range selection: the item
                // which the ranger 'thinks' is the already selected anchor becomes unselectable
                endRange();
            }
            return true;
        }

        return false;
    }

    @Override
    public void startRange(int position) {
        if (mSelection.contains(mKeyProvider.getKey(position))
                || select(mKeyProvider.getKey(position))) {
            anchorRange(position);
        }
    }

    @Override
    public void extendRange(int position) {
        extendRange(position, Range.TYPE_PRIMARY);
    }

    @Override
    public void endRange() {
        mRange = null;
        // Clean up in case there was any leftover provisional selection
        clearProvisionalSelection();
    }

    @Override
    public void anchorRange(int position) {
        checkArgument(position != RecyclerView.NO_POSITION);
        checkArgument(mSelection.contains(mKeyProvider.getKey(position)));

        mRange = new Range(position, mRangeCallbacks);
    }

    @Override
    public void extendProvisionalRange(int position) {
        if (mSingleSelect) {
            return;
        }

        if (DEBUG) Log.i(TAG, "Extending provision range to position: " + position);
        checkState(isRangeActive(), "Range start point not set.");
        extendRange(position, Range.TYPE_PROVISIONAL);
    }

    /**
     * Sets the end point for the current range selection, started by a call to
     * {@link #startRange(int)}. This function should only be called when a range selection
     * is active (see {@link #isRangeActive()}. Items in the range [anchor, end] will be
     * selected or in provisional select, depending on the type supplied. Note that if the type is
     * provisional selection, one should do {@link #mergeProvisionalSelection()} at some
     * point before calling on {@link #endRange()}.
     *
     * @param position The new end position for the selection range.
     * @param type The type of selection the range should utilize.
     */
    private void extendRange(int position, @RangeType int type) {
        checkState(isRangeActive(), "Range start point not set.");

        mRange.extendRange(position, type);

        // We're being lazy here notifying even when something might not have changed.
        // To make this more correct, we'd need to update the Ranger class to return
        // information about what has changed.
        notifySelectionChanged();
    }

    @Override
    public void setProvisionalSelection(@NonNull Set<K> newSelection) {
        if (mSingleSelect) {
            return;
        }

        Map<K, Boolean> delta = mSelection.setProvisionalSelection(newSelection);
        for (Map.Entry<K, Boolean> entry: delta.entrySet()) {
            notifyItemStateChanged(entry.getKey(), entry.getValue());
        }

        notifySelectionChanged();
    }

    @Override
    public void mergeProvisionalSelection() {
        mSelection.mergeProvisionalSelection();

        // Note, that for almost all functional purposes, merging a provisional selection
        // into a the primary selection doesn't change the selection, just an internal
        // representation of it. But there are some nuanced areas cases where
        // that isn't true. equality for 1. So, we notify regardless.

        notifySelectionChanged();
    }

    @Override
    public void clearProvisionalSelection() {
        for (K key : mSelection.mProvisionalSelection) {
            notifyItemStateChanged(key, false);
        }
        mSelection.clearProvisionalSelection();
    }

    @Override
    public boolean isRangeActive() {
        return mRange != null;
    }

    private boolean canSetState(@NonNull K key, boolean nextState) {
        return mSelectionPredicate.canSetStateForKey(key, nextState);
    }

    @Override
    AdapterDataObserver getAdapterDataObserver() {
        return mAdapterObserver;
    }

    private void onDataSetChanged() {
        mSelection.clearProvisionalSelection();

        notifySelectionRefresh();

        for (K key : mSelection) {
            // If the underlying data set has changed, before restoring
            // selection we must re-verify that it can be selected.
            // Why? Because if the dataset has changed, then maybe the
            // selectability of an item has changed.
            if (!canSetState(key, true)) {
                deselect(key);
            } else {
                int lastListener = mObservers.size() - 1;
                for (int i = lastListener; i >= 0; i--) {
                    mObservers.get(i).onItemStateChanged(key, true);
                }
            }
        }

        notifySelectionChanged();
    }

    /**
     * Notifies registered listeners when the selection status of a single item
     * (identified by {@code position}) changes.
     */
    private void notifyItemStateChanged(@NonNull K key, boolean selected) {
        checkArgument(key != null);

        int lastListenerIndex = mObservers.size() - 1;
        for (int i = lastListenerIndex; i >= 0; i--) {
            mObservers.get(i).onItemStateChanged(key, selected);
        }
    }

    private void notifySelectionCleared(@NonNull Selection<K> selection) {
        for (K key: selection.mSelection) {
            notifyItemStateChanged(key, false);
        }
        for (K key: selection.mProvisionalSelection) {
            notifyItemStateChanged(key, false);
        }
    }

    /**
     * Notifies registered listeners when the selection has changed. This
     * notification should be sent only once a full series of changes
     * is complete, e.g. clearingSelection, or updating the single
     * selection from one item to another.
     */
    private void notifySelectionChanged() {
        int lastListenerIndex = mObservers.size() - 1;
        for (int i = lastListenerIndex; i >= 0; i--) {
            mObservers.get(i).onSelectionChanged();
        }
    }

    private void notifySelectionRestored() {
        int lastListenerIndex = mObservers.size() - 1;
        for (int i = lastListenerIndex; i >= 0; i--) {
            mObservers.get(i).onSelectionRestored();
        }
    }

    private void notifySelectionRefresh() {
        int lastListenerIndex = mObservers.size() - 1;
        for (int i = lastListenerIndex; i >= 0; i--) {
            mObservers.get(i).onSelectionRefresh();
        }
    }

    private void updateForRange(int begin, int end, boolean selected, @RangeType int type) {
        switch (type) {
            case Range.TYPE_PRIMARY:
                updateForRegularRange(begin, end, selected);
                break;
            case Range.TYPE_PROVISIONAL:
                updateForProvisionalRange(begin, end, selected);
                break;
            default:
                throw new IllegalArgumentException("Invalid range type: " + type);
        }
    }

    private void updateForRegularRange(int begin, int end, boolean selected) {
        checkArgument(end >= begin);

        for (int i = begin; i <= end; i++) {
            K key = mKeyProvider.getKey(i);
            if (key == null) {
                continue;
            }

            if (selected) {
                select(key);
            } else {
                deselect(key);
            }
        }
    }

    private void updateForProvisionalRange(int begin, int end, boolean selected) {
        checkArgument(end >= begin);

        for (int i = begin; i <= end; i++) {
            K key = mKeyProvider.getKey(i);
            if (key == null) {
                continue;
            }

            boolean changedState = false;
            if (selected) {
                boolean canSelect = canSetState(key, true);
                if (canSelect && !mSelection.mSelection.contains(key)) {
                    mSelection.mProvisionalSelection.add(key);
                    changedState = true;
                }
            } else {
                mSelection.mProvisionalSelection.remove(key);
                changedState = true;
            }

            // Only notify item callbacks when something's state is actually changed in provisional
            // selection.
            if (changedState) {
                notifyItemStateChanged(key, selected);
            }
        }

        notifySelectionChanged();
    }

    @VisibleForTesting
    String getInstanceStateKey() {
        return EXTRA_SELECTION_PREFIX + ":" + mSelectionId;
    }

    @Override
    @SuppressWarnings("unchecked")
    public final void onSaveInstanceState(@NonNull Bundle state) {
        if (mSelection.isEmpty()) {
            return;
        }

        state.putBundle(getInstanceStateKey(), mStorage.asBundle(mSelection));
    }

    @Override
    public final void onRestoreInstanceState(@Nullable Bundle state) {
        if (state == null) {
            return;
        }

        @Nullable Bundle selectionState = state.getBundle(getInstanceStateKey());
        if (selectionState == null) {
            return;
        }

        Selection<K> selection = mStorage.asSelection(selectionState);
        if (selection != null && !selection.isEmpty()) {
            restoreSelection(selection);
        }
    }

    private final class RangeCallbacks extends Range.Callbacks {
        @Override
        void updateForRange(int begin, int end, boolean selected, int type) {
            switch (type) {
                case Range.TYPE_PRIMARY:
                    updateForRegularRange(begin, end, selected);
                    break;
                case Range.TYPE_PROVISIONAL:
                    updateForProvisionalRange(begin, end, selected);
                    break;
                default:
                    throw new IllegalArgumentException("Invalid range type: " + type);
            }
        }
    }

    private static final class AdapterObserver extends AdapterDataObserver {

        private final DefaultSelectionTracker<?> mSelectionTracker;

        AdapterObserver(@NonNull DefaultSelectionTracker<?> selectionTracker) {
            checkArgument(selectionTracker != null);
            mSelectionTracker = selectionTracker;
        }

        @Override
        public void onChanged() {
            mSelectionTracker.onDataSetChanged();
        }

        @Override
        public void onItemRangeChanged(int startPosition, int itemCount, @Nullable Object payload) {
            if (!SelectionTracker.SELECTION_CHANGED_MARKER.equals(payload)) {
                mSelectionTracker.onDataSetChanged();
            }
        }

        @Override
        public void onItemRangeInserted(int startPosition, int itemCount) {
            mSelectionTracker.endRange();
        }

        @Override
        public void onItemRangeRemoved(int startPosition, int itemCount) {
            mSelectionTracker.endRange();
        }

        @Override
        public void onItemRangeMoved(int fromPosition, int toPosition, int itemCount) {
            mSelectionTracker.endRange();
        }
    }
}
