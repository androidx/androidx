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
import static android.support.v4.util.Preconditions.checkArgument;

import android.support.annotation.Nullable;
import android.support.annotation.RestrictTo;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.MotionEvent;
import android.view.View;

/**
 * Provides a means of controlling when and where band selection can be initiated.
 *
 * @hide
 */
@RestrictTo(LIBRARY_GROUP)
public abstract class BandPredicate {

    /** @return true if band selection can be initiated in response to the {@link MotionEvent}. */
    public abstract boolean canInitiate(MotionEvent e);

    private static boolean hasSupportedLayoutManager(RecyclerView recView) {
        RecyclerView.LayoutManager lm = recView.getLayoutManager();
        return lm instanceof GridLayoutManager
                || lm instanceof LinearLayoutManager;
    }

    /**
     * Creates a new band predicate that permits initiation of band on areas
     * of a RecyclerView that map to RecyclerView.NO_POSITION.
     *
     * @param recView
     * @return
     */
    @SuppressWarnings("unused")
    public static BandPredicate noPosition(RecyclerView recView) {
        return new NoPosition(recView);
    }

    /**
     * Creates a new band predicate that permits initiation of band
     * anywhere doesn't correspond to a draggable region of a item.
     *
     * @param detailsLookup
     * @return
     */
    public static BandPredicate notDraggable(
            RecyclerView recView, ItemDetailsLookup detailsLookup) {
        return new NotDraggable(recView, detailsLookup);
    }

    /**
     * A BandPredicate that allows initiation of band selection only in areas of RecyclerView
     * that have {@link RecyclerView#NO_POSITION}. In most cases, this will be the empty areas
     * between views.
     */
    private static final class NoPosition extends BandPredicate {

        private final RecyclerView mRecView;

        NoPosition(RecyclerView recView) {
            checkArgument(recView != null);

            mRecView = recView;
        }

        @Override
        public boolean canInitiate(MotionEvent e) {
            if (!hasSupportedLayoutManager(mRecView)
                    || mRecView.hasPendingAdapterUpdates()) {
                return false;
            }

            View itemView = mRecView.findChildViewUnder(e.getX(), e.getY());
            int position = itemView != null
                    ? mRecView.getChildAdapterPosition(itemView)
                    : RecyclerView.NO_POSITION;

            return position == RecyclerView.NO_POSITION;
        }
    }

    /**
     * A BandPredicate that allows initiation of band selection in any area that is not
     * draggable as determined by consulting
     * {@link ItemDetailsLookup#inItemDragRegion(MotionEvent)}.
     */
    private static final class NotDraggable extends BandPredicate {

        private final RecyclerView mRecView;
        private final ItemDetailsLookup mDetailsLookup;

        NotDraggable(RecyclerView recView, ItemDetailsLookup detailsLookup) {
            checkArgument(recView != null);
            checkArgument(detailsLookup != null);

            mRecView = recView;
            mDetailsLookup = detailsLookup;
        }

        @Override
        public boolean canInitiate(MotionEvent e) {
            if (!hasSupportedLayoutManager(mRecView)
                    || mRecView.hasPendingAdapterUpdates()) {
                return false;
            }

            @Nullable ItemDetailsLookup.ItemDetails details = mDetailsLookup.getItemDetails(e);
            return (details == null) || !details.inDragRegion(e);
        }
    }
}
