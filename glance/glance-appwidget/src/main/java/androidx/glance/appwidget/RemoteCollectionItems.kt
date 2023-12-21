/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.glance.appwidget

import android.annotation.SuppressLint
import android.widget.RemoteViews

/** Representation of a fixed list of items to be displayed in a RemoteViews collection.  */
internal class RemoteCollectionItems private constructor(
    private val ids: LongArray,
    private val views: Array<RemoteViews>,
    private val hasStableIds: Boolean,
    private val _viewTypeCount: Int
) {
    init {
        require(ids.size == views.size) {
            "RemoteCollectionItems has different number of ids and views"
        }
        require(_viewTypeCount >= 1) { "View type count must be >= 1" }
        val layoutIdCount = views.map { it.layoutId }.distinct().count()
        require(layoutIdCount <= _viewTypeCount) {
            "View type count is set to $_viewTypeCount, but the collection contains " +
                "$layoutIdCount different layout ids"
        }
    }

    /**
     * Returns the id for [position]. See [hasStableIds] for whether this id should be
     * considered meaningful across collection updates.
     *
     * @return Id for the position.
     */
    fun getItemId(position: Int): Long = ids[position]

    /**
     * Returns the [RemoteViews] to display at [position].
     *
     * @return RemoteViews for the position.
     */
    fun getItemView(position: Int): RemoteViews = views[position]

    /**
     * Returns the number of elements in the collection.
     *
     * @return Count of items.
     */
    val itemCount: Int
        get() = ids.size

    /**
     * Returns the view type count for the collection when used in an adapter
     *
     * @return Count of view types for the collection when used in an adapter.
     * @see android.widget.Adapter.getViewTypeCount
     */
    val viewTypeCount: Int
        get() = _viewTypeCount

    /**
     * Indicates whether the item ids are stable across changes to the underlying data.
     *
     * @return True if the same id always refers to the same object.
     * @see android.widget.Adapter.hasStableIds
     */
    fun hasStableIds(): Boolean = hasStableIds

    /** Builder class for [RemoteCollectionItems] objects. */
    class Builder {
        private val ids = arrayListOf<Long>()
        private val views = arrayListOf<RemoteViews>()
        private var hasStableIds = false
        private var viewTypeCount = 0

        /**
         * Adds a [RemoteViews] to the collection.
         *
         * @param id Id to associate with the row. Use [.setHasStableIds] to indicate that ids are
         * stable across changes to the collection.
         * @param view RemoteViews to display for the row.
         */
        // Covered by getItemId, getItemView, getItemCount.
        @SuppressLint("MissingGetterMatchingBuilder")
        fun addItem(id: Long, view: RemoteViews): Builder {
            ids.add(id)
            views.add(view)
            return this
        }

        /**
         * Sets whether the item ids are stable across changes to the underlying data.
         *
         * @see android.widget.Adapter.hasStableIds
         */
        fun setHasStableIds(hasStableIds: Boolean): Builder {
            this.hasStableIds = hasStableIds
            return this
        }

        /**
         * Sets the view type count for the collection when used in an adapter. This can be set
         * to the maximum number of different layout ids that will be used by RemoteViews in
         * this collection.
         *
         * If this value is not set, then a value will be inferred from the provided items. As
         * a result, the adapter may need to be recreated when the list is updated with
         * previously unseen RemoteViews layouts for new items.
         *
         * @see android.widget.Adapter.getViewTypeCount
         */
        fun setViewTypeCount(viewTypeCount: Int): Builder {
            this.viewTypeCount = viewTypeCount
            return this
        }

        /** Creates the [RemoteCollectionItems] defined by this builder.  */
        fun build(): RemoteCollectionItems {
            if (viewTypeCount < 1) {
                // If a view type count wasn't specified, set it to be the number of distinct
                // layout ids used in the items.
                viewTypeCount = views.map { it.layoutId }.distinct().count()
            }
            return RemoteCollectionItems(
                ids.toLongArray(),
                views.toTypedArray(),
                hasStableIds,
                maxOf(viewTypeCount, 1)
            )
        }
    }

    companion object {
        val Empty = RemoteCollectionItems(
            ids = longArrayOf(),
            views = emptyArray(),
            hasStableIds = false,
            _viewTypeCount = 1
        )
    }
}
