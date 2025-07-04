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

import static androidx.annotation.RestrictTo.Scope.LIBRARY;

import android.view.MotionEvent;

import androidx.annotation.IntDef;
import androidx.annotation.RestrictTo;
import androidx.recyclerview.widget.RecyclerView;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * The Selection library calls {@link #getItemDetails(MotionEvent)} when it needs
 * access to information about the area and/or {@link ItemDetails} under a {@link MotionEvent}.
 * Your implementation must negotiate
 * {@link RecyclerView.ViewHolder ViewHolder} lookup with the
 * corresponding RecyclerView instance, and the subsequent conversion of the ViewHolder
 * instance to an {@link ItemDetails} instance.
 *
 * <p>
 * <b>Example</b>
 * <pre>
 * final class MyDetailsLookup extends ItemDetailsLookup<Uri> {
 *
 *   private final RecyclerView mRecyclerView;
 *
 *   MyDetailsLookup(RecyclerView recyclerView) {
 *       mRecyclerView = recyclerView;
 *   }
 *
 *   public @Nullable ItemDetails<Uri> getItemDetails(@NonNull MotionEvent e) {
 *       View view = mRecyclerView.findChildViewUnder(e.getX(), e.getY());
 *       if (view != null) {
 *           ViewHolder holder = mRecyclerView.getChildViewHolder(view);
 *           if (holder instanceof MyHolder) {
 *               return ((MyHolder) holder).getItemDetails();
 *           }
 *       }
 *       return null;
 *   }
 *}
 * </pre>
 *
 * @param <K> Selection key type. @see {@link StorageStrategy} for supported types.
 */
public abstract class ItemDetailsLookup<K> {

    /**
     * @return true if there is an item at the event coordinates.
     */
    final boolean overItem(@NonNull MotionEvent e) {
        return getItemPosition(e) != RecyclerView.NO_POSITION;
    }

    /**
     * @return true if there is an item w/ a stable ID at the event coordinates.
     */
    @RestrictTo(LIBRARY)
    @Deprecated
    protected boolean overItemWithSelectionKey(@NonNull MotionEvent e) {
        return overItem(e) && hasSelectionKey(getItemDetails(e));
    }

    /**
     * @return the item (if it exists) with a stable ID at the event coordinates.
     */
    @RestrictTo(LIBRARY)
    protected @Nullable ItemDetails<K> overItemWithSelectionKeyAsItem(@NonNull MotionEvent e) {
        ItemDetails<K> item = getItemDetails(e);
        return ((item != null)
                && (item.getPosition() != RecyclerView.NO_POSITION)
                && item.hasSelectionKey()) ? item : null;
    }

    /**
     * @return true if the event coordinates are in an area of the item
     * that can result in dragging the item. List items frequently have a white
     * area that is not draggable allowing band selection to be initiated
     * in that area.
     */
    final boolean inItemDragRegion(@NonNull MotionEvent e) {
        return overItem(e) && getItemDetails(e).inDragRegion(e);
    }

    /**
     * @return true if the event coordinates are in a "selection hot spot"
     * region of an item. Contact in these regions result in immediate
     * selection, even when there is no existing selection.
     */
    final boolean inItemSelectRegion(@NonNull MotionEvent e) {
        return overItem(e)
            && (getItemDetails(e).classifySelectionHotspot(e)
                    != ItemDetails.SELECTION_HOTSPOT_OUTSIDE);
    }

    /**
     * @return the adapter position of the item at the event coordinates.
     */
    final int getItemPosition(@NonNull MotionEvent e) {
        ItemDetails<?> item = getItemDetails(e);
        return item != null
                ? item.getPosition()
                : RecyclerView.NO_POSITION;
    }

    private static boolean hasSelectionKey(@Nullable ItemDetails<?> item) {
        return item != null && item.getSelectionKey() != null;
    }

    /**
     * @return the ItemDetails for the item under the event, or null.
     */
    public abstract @Nullable ItemDetails<K> getItemDetails(@NonNull MotionEvent e);

    /**
     * An ItemDetails implementation provides the selection library with access to information
     * about a specific RecyclerView item. This class is a key component in controling
     * the behaviors of the selection library in the context of a specific activity.
     *
     * <p>
     * <b>Selection Hotspot</b>
     *
     * <p>
     * This is an optional feature identifying an area within a view that
     * is single-tap to select. Ordinarily a single tap on an item when there is no
     * existing selection will result in that item being activated. If the tap
     * occurs within the "selection hotspot" the item will instead be selected.
     *
     * <p>
     * See {@link OnItemActivatedListener} for details on handling item activation.
     *
     * <p>
     * <b>Drag Region</b>
     *
     * <p>
     * The selection library provides support for mouse driven band selection. The "lasso"
     * typically associated with mouse selection can be started only in an empty
     * area of the RecyclerView (an area where the item position == RecyclerView#NO_POSITION,
     * or where RecyclerView#findChildViewUnder returns null). But in many instances
     * the item views presented by RecyclerView will contain areas that may be perceived
     * by the user as being empty. The user may expect to be able to initiate band
     * selection in these empty areas.
     *
     * <p>
     * The "drag region" concept exists in large part to accommodate this user expectation.
     * Drag region is the content in an item view that the user doesn't otherwise
     * perceive to be empty or part of the background of recycler view.
     *
     * Take for example a traditional single column layout where
     * the view layout width is "match_parent":
     * <pre>
     * -------------------------------------------------------
     * | [icon]  A string label.   ...empty space...         |
     * -------------------------------------------------------
     *   < ---  drag region  --> < --treated as background-->
     *</pre>
     *
     * <p>
     * Further more, within a drag region, a mouse click and drag will immediately
     * initiate drag and drop (if supported by your configuration).
     *
     * <p>
     * As user expectations around touch and mouse input differ substantially,
     * "drag region" has no effect on handling of touch input.
     *
     * @param <K> Selection key type. @see {@link StorageStrategy} for supported types.
     */
    public abstract static class ItemDetails<K> {

        /**
         * Indicates that a UI event is outside the "selection hotspot" and leads to the default,
         * fallback behavior of that touch tap, mouse click, etc.
         */
        public static final int SELECTION_HOTSPOT_OUTSIDE = 0;

        /**
         * Indicates that a UI event toggles the item's selectedness (unselected becomes selected
         * and vice versa), like a checkbox UI element.
         */
        public static final int SELECTION_HOTSPOT_INSIDE_TOGGLE_MULTI = 1;

        /**
         * Indicates that a UI event clears any existing selections and then, unless the item was
         * previously the sole selection, selects it.
         */
        public static final int SELECTION_HOTSPOT_INSIDE_TOGGLE_SOLO = 2;

        /**
         * Indicates that a UI event clears any existing selections and then selects the item, like
         * a radio button UI element.
         */
        public static final int SELECTION_HOTSPOT_INSIDE_CLEAR_AND_THEN_SET = 3;

        @RestrictTo(LIBRARY)
        @IntDef({
            SELECTION_HOTSPOT_OUTSIDE,
            SELECTION_HOTSPOT_INSIDE_TOGGLE_MULTI,
            SELECTION_HOTSPOT_INSIDE_TOGGLE_SOLO,
            SELECTION_HOTSPOT_INSIDE_CLEAR_AND_THEN_SET
        })
        @Retention(RetentionPolicy.SOURCE)
        public @interface SelectionHotspotResult {}

        /**
         * Returns the adapter position of the item. See
         * {@link RecyclerView.ViewHolder#getAbsoluteAdapterPosition() ViewHolder
         * .getAbsoluteAdapterPosition}
         *
         * @return the position of an item.
         */
        public abstract int getPosition();

        /**
         * @return true if the item has a selection key.
         */
        public boolean hasSelectionKey() {
            return getSelectionKey() != null;
        }

        /**
         * @return the selection key of an item.
         */
        public abstract @Nullable K getSelectionKey();

        /**
         * Like classifySelectionHotspot but returns a boolean instead of an int.
         *
         * This method is provided for backwards compatibility and is only called when the
         * ItemDetails subclass does not override classifySelectionHotspot. New code should
         * override classifySelectionHotspot instead of this method.
         *
         * @return false for SELECTION_HOTSPOT_OUTSIDE,
         *         true  for SELECTION_HOTSPOT_INSIDE_TOGGLE_MULTI.
         * @deprecated override classifySelectionHotspot instead.
         */
        @Deprecated
        public boolean inSelectionHotspot(@NonNull MotionEvent e) {
            return false;
        }

        /**
         * Tapping (touch) or clicking (mouse) on an item usually leads to some default behavior:
         * activating or opening the item, for touch, or deselecting the item (or deselecting
         * everything), for mouse.
         *
         * This method allows identifying a "selection hotspot" - an inner region of an item's view
         * where tapping or clicking on it will change the selection state (typically by selecting
         * that item) instead of that default behavior.
         *
         * One example is icons to the left of an email message - hotspots that are like selection
         * checkboxes. Having classifySelectionHotspot return SELECTION_HOTSPOT_INSIDE_TOGGLE_MULTI
         * indicates to the library that some part of a view is checkbox-like.
         *
         * Similarly, having a MotionEvent trigger SELECTION_HOTSPOT_INSIDE_CLEAR_AND_THEN_SET is
         * radio-button-like. Returning SELECTION_HOTSPOT_INSIDE_TOGGLE_SOLO is a third option.
         *
         * When returning a non-zero value (a value that's not SELECTION_HOTSPOT_OUTSIDE), the
         * nuances in behavior is relevant for mouse clicks but less so for touch taps. For touch:
         *
         * - If nothing was previously selected, SELECTION_HOTSPOT_OUTSIDE activates that item.
         * - If nothing was previously selected, any non-zero value just selects that item.
         * - If something was previously selected, touch taps (but not mouse clicks) anywhere in
         *   the item's view (not just in its "selection hotspot") is treated as toggling selection
         *   (like a Ctrl-click), for historical reasons.
         *
         * @return a value that's not SELECTION_HOTSPOT_OUTSIDE if the event is in an area of the
         * item that should be directly interpreted as a user wishing to select the item.
         */
        public @SelectionHotspotResult int classifySelectionHotspot(@NonNull MotionEvent e) {
            return inSelectionHotspot(e)
                    ? SELECTION_HOTSPOT_INSIDE_TOGGLE_MULTI
                    : SELECTION_HOTSPOT_OUTSIDE;
        }

        /**
         * "Item Drag Region" identifies areas of an item that are not considered when the library
         * evaluates whether or not to initiate band-selection for mouse input. The drag region
         * will usually correspond to an area of an item that represents user visible content.
         * Mouse driven band selection operations are only ever initiated in non-drag-regions.
         * This is a consideration as many layouts may not include empty space between
         * RecyclerView items where band selection can be initiated.
         *
         * <p>
         * For example. You may present a single column list of contact names in a
         * RecyclerView instance in which the individual view items expand to fill all
         * available space.
         * But within the expanded view item after the contact name there may be empty space that a
         * user would reasonably expect to initiate band selection. When a MotionEvent occurs
         * in such an area, you should return identify this as NOT in a drag region.
         *
         * <p>
         * Further more, within a drag region, a mouse click and drag will immediately
         * initiate drag and drop (if supported by your configuration).
         *
         * @return true if the item is in an area of the item that can result in dragging
         * the item. List items frequently have a white area that is not draggable allowing
         * mouse driven band selection to be initiated in that area.
         */
        public boolean inDragRegion(@NonNull MotionEvent e) {
            return false;
        }

        @Override
        public boolean equals(@Nullable Object obj) {
            return (obj instanceof ItemDetails)
                    && isEqualTo((ItemDetails<?>) obj);
        }

        private boolean isEqualTo(@NonNull ItemDetails<?> other) {
            K key = getSelectionKey();
            boolean sameKeys = false;
            if (key == null) {
                sameKeys = other.getSelectionKey() == null;
            } else {
                sameKeys = key.equals(other.getSelectionKey());
            }
            return sameKeys && this.getPosition() == other.getPosition();
        }

        @Override
        public int hashCode() {
            return getPosition() >>> 8;
        }
    }
}
