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

import android.view.MotionEvent;
import android.view.View;

import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.jspecify.annotations.NonNull;

/**
 * Provides a means of controlling when and where band selection can be initiated.
 *
 * <p>
 * Two default implementations are provided: {@link EmptyArea}, and {@link NonDraggableArea}.
 *
 * @see SelectionTracker.Builder#withBandPredicate(BandPredicate)
 */
public abstract class BandPredicate {

    /**
     * @return true if band selection can be initiated in response to the {@link MotionEvent}.
     */
    public abstract boolean canInitiate(@NonNull MotionEvent e);

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    static boolean hasSupportedLayoutManager(@NonNull RecyclerView recyclerView) {
        RecyclerView.LayoutManager lm = recyclerView.getLayoutManager();
        return lm instanceof GridLayoutManager
                || lm instanceof LinearLayoutManager;
    }

    /**
     * A BandPredicate that allows initiation of band selection only in areas of RecyclerView
     * that map to {@link RecyclerView#NO_POSITION}. In most cases, this will be the empty areas
     * between views.
     *
     * <p>
     * Use this implementation to permit band selection only in empty areas
     * surrounding view items. But be advised that if there is no empy area around
     * view items, band selection cannot be initiated.
     */
    public static final class EmptyArea extends BandPredicate {

        private final RecyclerView mRecyclerView;

        /**
         * @param recyclerView the owner RecyclerView
         */
        public EmptyArea(@NonNull RecyclerView recyclerView) {
            checkArgument(recyclerView != null);

            mRecyclerView = recyclerView;
        }

        @Override
        public boolean canInitiate(@NonNull MotionEvent e) {
            if (!hasSupportedLayoutManager(mRecyclerView)
                    || mRecyclerView.hasPendingAdapterUpdates()) {
                return false;
            }

            View itemView = mRecyclerView.findChildViewUnder(e.getX(), e.getY());
            int position = itemView != null
                    ? mRecyclerView.getChildAdapterPosition(itemView)
                    : RecyclerView.NO_POSITION;

            return position == RecyclerView.NO_POSITION;
        }
    }

    /**
     * A BandPredicate that allows initiation of band selection in any area that is not
     * draggable as determined by consulting
     * {@link ItemDetailsLookup.ItemDetails#inDragRegion(MotionEvent)}. By default empty
     * areas (those with a position that maps to {@link RecyclerView#NO_POSITION}
     * are considered non-draggable.
     *
     * <p>
     * Use this implementation in order to permit band selection in
     * otherwise empty areas of a View. This is useful especially in
     * list layouts where there is no empty space surrounding the list items,
     * and individual list items may contain extra white space (like
     * in a list of varying length words).
     *
     * @see ItemDetailsLookup.ItemDetails#inDragRegion(MotionEvent)
     */
    public static final class NonDraggableArea extends BandPredicate {

        private final RecyclerView mRecyclerView;
        private final ItemDetailsLookup<?> mDetailsLookup;

        /**
         * Creates a new instance.
         *
         * @param recyclerView  the owner RecyclerView
         * @param detailsLookup provides access to item details.
         */
        public NonDraggableArea(
                @NonNull RecyclerView recyclerView, @NonNull ItemDetailsLookup<?> detailsLookup) {

            checkArgument(recyclerView != null);
            checkArgument(detailsLookup != null);

            mRecyclerView = recyclerView;
            mDetailsLookup = detailsLookup;
        }

        @Override
        public boolean canInitiate(@NonNull MotionEvent e) {
            if (!hasSupportedLayoutManager(mRecyclerView)
                    || mRecyclerView.hasPendingAdapterUpdates()) {
                return false;
            }

            ItemDetailsLookup.ItemDetails<?> details =
                    mDetailsLookup.getItemDetails(e);
            return (details == null) || !details.inDragRegion(e);
        }
    }
}
