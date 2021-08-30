/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.core.widget

import android.annotation.SuppressLint
import android.appwidget.AppWidgetManager
import android.content.Context
import android.os.Parcel
import android.os.Parcelable
import android.widget.RemoteViews
import androidx.annotation.DoNotInline
import androidx.annotation.IdRes
import androidx.annotation.RequiresApi
import androidx.core.os.BuildCompat

/**
 * Helper for accessing features in [RemoteViews].
 */
public object RemoteViewsCompat {
    /**
     * Creates a simple Adapter for the widgetId and viewId specified. The viewId must point to an
     * AdapterView, ie. [android.widget.ListView], [android.widget.GridView],
     * [android.widget.StackView], or [android.widget.AdapterViewAnimator].
     *
     * This is a simpler but less flexible approach to populating collection widgets. Its use is
     * encouraged for most scenarios, as long as the total memory within the list of RemoteViews
     * is relatively small (ie. doesn't contain large or numerous Bitmaps, see
     * [RemoteViews.setImageViewBitmap]). In the case of numerous images, the use of API is
     * still possible by setting image URIs instead of Bitmaps, see [RemoteViews.setImageViewUri].
     *
     * If you use this API, you should not call
     * [AppWidgetManager.notifyAppWidgetViewDataChanged] and should instead update
     * your app widget, calling this method with the new [RemoteCollectionItems].
     *
     * @param context     The [Context] of the app providing the widget.
     * @param remoteViews The [RemoteViews] to receive the adapter.
     * @param appWidgetId the id of the widget for which the adapter is being set.
     * @param viewId      The id of the [android.widget.AdapterView].
     * @param items       The items to display in the [android.widget.AdapterView].
     */
    @JvmStatic
    public fun setRemoteAdapter(
        context: Context,
        remoteViews: RemoteViews,
        appWidgetId: Int,
        @IdRes viewId: Int,
        items: RemoteCollectionItems
    ) {
        if (BuildCompat.isAtLeastS()) {
            try {
                Api31Impl.setRemoteAdapter(remoteViews, viewId, items)
                return
            } catch (e: LinkageError) {
                // This will occur if the API doesn't exist yet on this version of S. We can simply
                // fall back to the approach we use on pre-S devices.
            }
        }
        val intent = RemoteViewsCompatService.createIntent(context, appWidgetId, viewId)
        check(context.packageManager.resolveService(intent, /* flags= */ 0) != null) {
            "RemoteViewsCompatService could not be resolved, ensure that you have declared it in " +
                "your app manifest."
        }
        remoteViews.setRemoteAdapter(viewId, intent)
        RemoteViewsCompatService.saveItems(context, appWidgetId, viewId, items)
        AppWidgetManager.getInstance(context).notifyAppWidgetViewDataChanged(appWidgetId, viewId)
    }

    /** Representation of a fixed list of items to be displayed in a RemoteViews collection.  */
    public class RemoteCollectionItems {
        private val mIds: LongArray
        private val mViews: Array<RemoteViews>
        private val mHasStableIds: Boolean
        private val mViewTypeCount: Int

        internal constructor(
            ids: LongArray,
            views: Array<RemoteViews>,
            hasStableIds: Boolean,
            viewTypeCount: Int
        ) {
            mIds = ids
            mViews = views
            mHasStableIds = hasStableIds
            mViewTypeCount = viewTypeCount

            require(ids.size == views.size) {
                "RemoteCollectionItems has different number of ids and views"
            }
            require(viewTypeCount >= 1) { "View type count must be >= 1" }

            val layoutIdCount = views.map { it.layoutId }.distinct().count()
            require(layoutIdCount <= viewTypeCount) {
                "View type count is set to $viewTypeCount, but the collection contains " +
                    "$layoutIdCount different layout ids"
            }
        }

        /** @hide */
        internal constructor(parcel: Parcel) {
            val length = parcel.readInt()
            mIds = LongArray(length)
            parcel.readLongArray(mIds)
            mViews = parcel.readNonNullTypedArray(length, RemoteViews.CREATOR)
            mHasStableIds = parcel.readInt() == 1
            mViewTypeCount = parcel.readInt()
        }

        /** @hide */
        internal fun writeToParcel(dest: Parcel, flags: Int) {
            dest.writeInt(mIds.size)
            dest.writeLongArray(mIds)
            dest.writeTypedArray(mViews, flags)
            dest.writeInt(if (mHasStableIds) 1 else 0)
            dest.writeInt(mViewTypeCount)
        }

        /**
         * Returns the id for [position]. See [hasStableIds] for whether this id should be
         * considered meaningful across collection updates.
         *
         * @return Id for the position.
         */
        public fun getItemId(position: Int): Long = mIds[position]

        /**
         * Returns the [RemoteViews] to display at [position].
         *
         * @return RemoteViews for the position.
         */
        public fun getItemView(position: Int): RemoteViews = mViews[position]

        /**
         * Returns the number of elements in the collection.
         *
         * @return Count of items.
         */
        public val itemCount: Int
            get() = mIds.size

        /**
         * Returns the view type count for the collection when used in an adapter
         *
         * @return Count of view types for the collection when used in an adapter.
         * @see android.widget.Adapter.getViewTypeCount
         */
        public val viewTypeCount: Int
            get() = mViewTypeCount

        /**
         * Indicates whether the item ids are stable across changes to the underlying data.
         *
         * @return True if the same id always refers to the same object.
         * @see android.widget.Adapter.hasStableIds
         */
        public fun hasStableIds(): Boolean = mHasStableIds

        /** Builder class for [RemoteCollectionItems] objects. */
        public class Builder {
            private val mIds = arrayListOf<Long>()
            private val mViews = arrayListOf<RemoteViews>()
            private var mHasStableIds = false
            private var mViewTypeCount = 0

            /**
             * Adds a [RemoteViews] to the collection.
             *
             * @param id   Id to associate with the row. Use [.setHasStableIds] to
             * indicate that ids are stable across changes to the collection.
             * @param view RemoteViews to display for the row.
             */
            // Covered by getItemId, getItemView, getItemCount.
            @SuppressLint("MissingGetterMatchingBuilder")
            public fun addItem(id: Long, view: RemoteViews): Builder {
                mIds.add(id)
                mViews.add(view)
                return this
            }

            /**
             * Sets whether the item ids are stable across changes to the underlying data.
             *
             * @see android.widget.Adapter.hasStableIds
             */
            public fun setHasStableIds(hasStableIds: Boolean): Builder {
                mHasStableIds = hasStableIds
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
            public fun setViewTypeCount(viewTypeCount: Int): Builder {
                mViewTypeCount = viewTypeCount
                return this
            }

            /** Creates the [RemoteCollectionItems] defined by this builder.  */
            public fun build(): RemoteCollectionItems {
                if (mViewTypeCount < 1) {
                    // If a view type count wasn't specified, set it to be the number of distinct
                    // layout ids used in the items.
                    mViewTypeCount = mViews.map { it.layoutId }.distinct().count()
                }
                return RemoteCollectionItems(
                    mIds.toLongArray(),
                    mViews.toTypedArray(),
                    mHasStableIds,
                    maxOf(mViewTypeCount, 1)
                )
            }
        }

        private companion object {
            /** Reads a non-null array of [T] of [size] from the [Parcel]. */
            inline fun <reified T : Any> Parcel.readNonNullTypedArray(
                size: Int,
                creator: Parcelable.Creator<T>
            ): Array<T> {
                val array = arrayOfNulls<T?>(size)
                readTypedArray(array, creator)
                return array.requireNoNulls()
            }
        }
    }

    /**
     * Version-specific static inner class to avoid verification errors that negatively affect
     * run-time performance.
     */
    @RequiresApi(31)
    private object Api31Impl {
        @DoNotInline
        fun setRemoteAdapter(remoteViews: RemoteViews, viewId: Int, items: RemoteCollectionItems) {
            remoteViews.setRemoteAdapter(viewId, toPlatformCollectionItems(items))
        }

        /**
         * Returns a [RemoteViews.RemoteCollectionItems] equivalent to this [RemoteCollectionItems].
         */
        @DoNotInline
        private fun toPlatformCollectionItems(
            items: RemoteCollectionItems
        ): RemoteViews.RemoteCollectionItems {
            return RemoteViews.RemoteCollectionItems.Builder()
                .setHasStableIds(items.hasStableIds())
                .setViewTypeCount(items.viewTypeCount)
                .also { builder ->
                    repeat(items.itemCount) { index ->
                        builder.addItem(items.getItemId(index), items.getItemView(index))
                    }
                }
                .build()
        }
    }
}