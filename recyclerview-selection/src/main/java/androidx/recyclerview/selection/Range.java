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

import static androidx.core.util.Preconditions.checkArgument;
import static androidx.recyclerview.selection.Shared.DEBUG;
import static androidx.recyclerview.widget.RecyclerView.NO_POSITION;

import android.util.Log;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Class providing support for managing range selections.
 */
final class Range {

    static final int TYPE_PRIMARY = 0;

    /**
     * "Provisional" selection represents a overlay on the primary selection. A provisional
     * selection maybe be eventually added to the primary selection, or it may be abandoned.
     *
     * <p>
     * E.g. BandSelectionHelper creates a provisional selection while a user is actively
     * selecting items with a band. GestureSelectionHelper creates a provisional selection
     * while a user is active selecting via gesture.
     *
     * <p>
     * Provisionally selected items are considered to be selected in
     * {@link Selection#contains(String)} and related methods. A provisional may be abandoned or
     * merged into the promary selection.
     *
     * <p>
     * A provisional selection may intersect with the primary selection, however clearing the
     * provisional selection will not affect the primary selection where the two may intersect.
     */
    static final int TYPE_PROVISIONAL = 1;
    @IntDef({
            TYPE_PRIMARY,
            TYPE_PROVISIONAL
    })
    @Retention(RetentionPolicy.SOURCE)
    @interface RangeType {}

    private static final String TAG = "Range";

    private final Callbacks mCallbacks;
    private final int mBegin;
    private int mEnd = NO_POSITION;

    /**
     * Creates a new range anchored at {@code position}.
     *
     * @param position
     * @param callbacks
     */
    Range(int position, @NonNull Callbacks callbacks) {
        mBegin = position;
        mCallbacks = callbacks;
        if (DEBUG) Log.d(TAG, "Creating new Range anchored @ " + position);
    }

    void extendRange(int position, @RangeType int type) {
        checkArgument(position != NO_POSITION, "Position cannot be NO_POSITION.");

        if (mEnd == NO_POSITION || mEnd == mBegin) {
            // Reset mEnd so it can be established in establishRange.
            mEnd = NO_POSITION;
            establishRange(position, type);
        } else {
            reviseRange(position, type);
        }
    }

    private void establishRange(int position, @RangeType int type) {
        checkArgument(mEnd == NO_POSITION, "End has already been set.");

        mEnd = position;

        if (position > mBegin) {
            if (DEBUG) log(type, "Establishing initial range at @ " + position);
            updateRange(mBegin + 1, position, true, type);
        } else if (position < mBegin) {
            if (DEBUG) log(type, "Establishing initial range at @ " + position);
            updateRange(position, mBegin - 1, true, type);
        }
    }

    private void reviseRange(int position, @RangeType int type) {
        checkArgument(mEnd != NO_POSITION, "End must already be set.");
        checkArgument(mBegin != mEnd, "Beging and end point to same position.");

        if (position == mEnd) {
            if (DEBUG) log(type, "Ignoring no-op revision for range @ " + position);
        }

        if (mEnd > mBegin) {
            reviseAscending(position, type);
        } else if (mEnd < mBegin) {
            reviseDescending(position, type);
        }
        // the "else" case is covered by checkState at beginning of method.

        mEnd = position;
    }

    /**
     * Updates an existing ascending selection.
     */
    private void reviseAscending(int position, @RangeType int type) {
        if (DEBUG) log(type, "*ascending* Revising range @ " + position);

        if (position < mEnd) {
            if (position < mBegin) {
                updateRange(mBegin + 1, mEnd, false, type);
                updateRange(position, mBegin - 1, true, type);
            } else {
                updateRange(position + 1, mEnd, false, type);
            }
        } else if (position > mEnd) {   // Extending the range...
            updateRange(mEnd + 1, position, true, type);
        }
    }

    private void reviseDescending(int position, @RangeType int type) {
        if (DEBUG) log(type, "*descending* Revising range @ " + position);

        if (position > mEnd) {
            if (position > mBegin) {
                updateRange(mEnd, mBegin - 1, false, type);
                updateRange(mBegin + 1, position, true, type);
            } else {
                updateRange(mEnd, position - 1, false, type);
            }
        } else if (position < mEnd) {   // Extending the range...
            updateRange(position, mEnd - 1, true, type);
        }
    }

    /**
     * Try to set selection state for all elements in range. Not that callbacks can cancel
     * selection of specific items, so some or even all items may not reflect the desired state
     * after the update is complete.
     *
     * @param begin    Adapter position for range start (inclusive).
     * @param end      Adapter position for range end (inclusive).
     * @param selected New selection state.
     */
    private void updateRange(
            int begin, int end, boolean selected, @RangeType int type) {
        mCallbacks.updateForRange(begin, end, selected, type);
    }

    @Override
    public String toString() {
        return "Range{begin=" + mBegin + ", end=" + mEnd + "}";
    }

    private void log(@RangeType int type, String message) {
        String opType = type == TYPE_PRIMARY ? "PRIMARY" : "PROVISIONAL";
        Log.d(TAG, String.valueOf(this) + ": " + message + " (" + opType + ")");
    }

    /*
     * @see {@link DefaultSelectionTracker#updateForRange(int, int , boolean, int)}.
     */
    abstract static class Callbacks {
        abstract void updateForRange(
                int begin, int end, boolean selected, @RangeType int type);
    }
}
