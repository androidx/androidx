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
import android.content.res.ColorStateList
import android.graphics.BlendMode
import android.graphics.drawable.Icon
import android.os.Build
import android.os.Parcel
import android.os.Parcelable
import android.widget.RemoteViews
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.annotation.DimenRes
import androidx.annotation.DrawableRes
import androidx.annotation.IdRes
import androidx.annotation.LayoutRes
import androidx.annotation.Px
import androidx.annotation.RequiresApi
import androidx.annotation.StringRes

/** Helper for accessing features in [RemoteViews]. */
public object RemoteViewsCompat {
    /**
     * Creates a simple Adapter for the widgetId and viewId specified. The viewId must point to an
     * AdapterView, ie. [android.widget.ListView], [android.widget.GridView],
     * [android.widget.StackView], or [android.widget.AdapterViewAnimator].
     *
     * This is a simpler but less flexible approach to populating collection widgets. Its use is
     * encouraged for most scenarios, as long as the total memory within the list of RemoteViews is
     * relatively small (ie. doesn't contain large or numerous Bitmaps, see
     * [RemoteViews.setImageViewBitmap]). In the case of numerous images, the use of API is still
     * possible by setting image URIs instead of Bitmaps, see [RemoteViews.setImageViewUri].
     *
     * If you use this API, you should not call [AppWidgetManager.notifyAppWidgetViewDataChanged]
     * and should instead update your app widget, calling this method with the new
     * [RemoteCollectionItems].
     *
     * @param context The [Context] of the app providing the widget.
     * @param remoteViews The [RemoteViews] to receive the adapter.
     * @param appWidgetId the id of the widget for which the adapter is being set.
     * @param viewId The id of the [android.widget.AdapterView].
     * @param items The items to display in the [android.widget.AdapterView].
     */
    @JvmStatic
    @Suppress("DEPRECATION")
    public fun setRemoteAdapter(
        context: Context,
        remoteViews: RemoteViews,
        appWidgetId: Int,
        @IdRes viewId: Int,
        items: RemoteCollectionItems,
    ) {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.S) {
            // Due to an inefficient Parcelable implementation, the platform collections API is
            // unsuitable on API 31.
            CollectionItemsApi31Impl.setRemoteAdapter(remoteViews, viewId, items)
            return
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

    /** Representation of a fixed list of items to be displayed in a RemoteViews collection. */
    public class RemoteCollectionItems {
        private val mIds: LongArray
        private val mViews: Array<RemoteViews>
        private val mHasStableIds: Boolean
        private val mViewTypeCount: Int

        internal constructor(
            ids: LongArray,
            views: Array<RemoteViews>,
            hasStableIds: Boolean,
            viewTypeCount: Int,
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

        internal constructor(parcel: Parcel) {
            val length = parcel.readInt()
            mIds = LongArray(length)
            parcel.readLongArray(mIds)
            mViews = parcel.readNonNullTypedArray(length, RemoteViews.CREATOR)
            mHasStableIds = parcel.readInt() == 1
            mViewTypeCount = parcel.readInt()
        }

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
             * @param id Id to associate with the row. Use [.setHasStableIds] to indicate that ids
             *   are stable across changes to the collection.
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
             * If this value is not set, then a value will be inferred from the provided items. As a
             * result, the adapter may need to be recreated when the list is updated with previously
             * unseen RemoteViews layouts for new items.
             *
             * @see android.widget.Adapter.getViewTypeCount
             */
            public fun setViewTypeCount(viewTypeCount: Int): Builder {
                mViewTypeCount = viewTypeCount
                return this
            }

            /** Creates the [RemoteCollectionItems] defined by this builder. */
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
                    maxOf(mViewTypeCount, 1),
                )
            }
        }

        private companion object {
            /** Reads a non-null array of [T] of [size] from the [Parcel]. */
            inline fun <reified T : Any> Parcel.readNonNullTypedArray(
                size: Int,
                creator: Parcelable.Creator<T>,
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
    private object CollectionItemsApi31Impl {
        fun setRemoteAdapter(remoteViews: RemoteViews, viewId: Int, items: RemoteCollectionItems) {
            remoteViews.setRemoteAdapter(viewId, toPlatformCollectionItems(items))
        }

        /**
         * Returns a [RemoteViews.RemoteCollectionItems] equivalent to this [RemoteCollectionItems].
         */
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

    /**
     * Equivalent to calling [android.widget.Chronometer.setBase].
     *
     * @param viewId The id of the target view
     * @param base The time at which the timer would have read 0:00. This time should be based off
     *   of [android.os.SystemClock.elapsedRealtime].
     */
    @JvmStatic
    public fun RemoteViews.setChronometerBase(@IdRes viewId: Int, base: Long) {
        setLong(viewId, "setBase", base)
    }

    /**
     * Equivalent to calling [android.widget.Chronometer.setFormat].
     *
     * @param viewId The id of the target view
     * @param format The Chronometer format string, or null to simply display the timer value.
     */
    @JvmStatic
    public fun RemoteViews.setChronometerFormat(@IdRes viewId: Int, format: String?) {
        setString(viewId, "setFormat", format)
    }

    /**
     * Equivalent to calling [android.widget.CompoundButton.setButtonDrawable].
     *
     * @param viewId The id of the target view
     * @param resId The resource identifier of the drawable, or 0 to clear the button.
     */
    @RequiresApi(31)
    @JvmStatic
    public fun RemoteViews.setCompoundButtonDrawable(@IdRes viewId: Int, @DrawableRes resId: Int) {
        setInt(viewId, "setButtonDrawable", resId)
    }

    /**
     * Equivalent to calling [android.widget.CompoundButton.setButtonIcon].
     *
     * @param viewId The id of the target view
     * @param icon An Icon holding the desired button, or null to clear the button.
     */
    @RequiresApi(31)
    @JvmStatic
    public fun RemoteViews.setCompoundButtonIcon(@IdRes viewId: Int, icon: Icon?) {
        setIcon(viewId, "setButtonIcon", icon)
    }

    /**
     * Equivalent to calling [android.widget.CompoundButton.setButtonTintBlendMode].
     *
     * @param viewId The id of the target view
     * @param tintMode The blending mode used to apply the tint, may be null to clear tint.
     */
    @RequiresApi(31)
    @JvmStatic
    public fun RemoteViews.setCompoundButtonTintBlendMode(
        @IdRes viewId: Int,
        tintMode: BlendMode?,
    ) {
        Api31Impl.setBlendMode(this, viewId, "setButtonTintBlendMode", tintMode)
    }

    /**
     * Equivalent to calling [android.widget.CompoundButton.setButtonTintList].
     *
     * @param viewId The id of the target view
     * @param tint The tint to apply, may be null to clear tint.
     */
    @RequiresApi(31)
    @JvmStatic
    public fun RemoteViews.setCompoundButtonTintList(@IdRes viewId: Int, tint: ColorStateList?) {
        Api31Impl.setColorStateList(this, viewId, "setButtonTintList", tint)
    }

    /**
     * Equivalent to calling [android.widget.CompoundButton.setButtonTintList].
     *
     * @param viewId The id of the target view
     * @param notNight The tint to apply when the UI is not in night mode, may be null to clear
     *   tint.
     * @param night The tint to apply when the UI is in night mode, may be null to clear tint.
     */
    @RequiresApi(31)
    @JvmStatic
    public fun RemoteViews.setCompoundButtonTintList(
        @IdRes viewId: Int,
        notNight: ColorStateList?,
        night: ColorStateList?,
    ) {
        Api31Impl.setColorStateList(this, viewId, "setButtonTintList", notNight, night)
    }

    /**
     * Equivalent to calling [android.widget.CompoundButton.setButtonTintList].
     *
     * @param viewId The id of the target view
     * @param resId The resource id for the tint to apply, may be 0 to clear tint.
     */
    @RequiresApi(31)
    @JvmStatic
    public fun RemoteViews.setCompoundButtonTintList(@IdRes viewId: Int, @ColorRes resId: Int) {
        Api31Impl.setColorStateList(this, viewId, "setButtonTintList", resId)
    }

    /**
     * Equivalent to calling [android.widget.CompoundButton.setButtonTintList].
     *
     * @param viewId The id of the target view
     * @param resId The attribute id for the tint to apply, may be 0 to clear tint.
     */
    @RequiresApi(31)
    @JvmStatic
    public fun RemoteViews.setCompoundButtonTintListAttr(@IdRes viewId: Int, @AttrRes resId: Int) {
        Api31Impl.setColorStateListAttr(this, viewId, "setButtonTintList", resId)
    }

    /**
     * Equivalent to calling [android.widget.FrameLayout.setForegroundGravity].
     *
     * @param viewId The id of the target view
     * @param foregroundGravity See [android.view.Gravity].
     */
    @JvmStatic
    public fun RemoteViews.setFrameLayoutForegroundGravity(
        @IdRes viewId: Int,
        foregroundGravity: Int,
    ) {
        setInt(viewId, "setForegroundGravity", foregroundGravity)
    }

    /**
     * Equivalent to calling [android.widget.FrameLayout.setMeasureAllChildren].
     *
     * @param viewId The id of the target view
     * @param measureAll True to consider children marked GONE, false otherwise.
     */
    @JvmStatic
    public fun RemoteViews.setFrameLayoutMeasureAllChildren(
        @IdRes viewId: Int,
        measureAll: Boolean,
    ) {
        setBoolean(viewId, "setMeasureAllChildren", measureAll)
    }

    /**
     * Equivalent to calling [android.widget.GridLayout.setAlignmentMode].
     *
     * @param viewId The id of the target view
     * @param alignmentMode Either [android.widget.GridLayout.ALIGN_BOUNDS] or
     *   [android.widget.GridLayout.ALIGN_MARGINS].
     */
    @RequiresApi(31)
    @JvmStatic
    public fun RemoteViews.setGridLayoutAlignmentMode(@IdRes viewId: Int, alignmentMode: Int) {
        setInt(viewId, "setAlignmentMode", alignmentMode)
    }

    /**
     * Equivalent to calling [android.widget.GridLayout.setColumnCount].
     *
     * @param viewId The id of the target view
     * @param columnCount The number of columns.
     */
    @RequiresApi(31)
    @JvmStatic
    public fun RemoteViews.setGridLayoutColumnCount(@IdRes viewId: Int, columnCount: Int) {
        setInt(viewId, "setColumnCount", columnCount)
    }

    /**
     * Equivalent to calling [android.widget.GridLayout.setRowCount].
     *
     * @param viewId The id of the target view
     * @param rowCount The number of rows.
     */
    @RequiresApi(31)
    @JvmStatic
    public fun RemoteViews.setGridLayoutRowCount(@IdRes viewId: Int, rowCount: Int) {
        setInt(viewId, "setRowCount", rowCount)
    }

    /**
     * Equivalent to calling [android.widget.GridView.setColumnWidth].
     *
     * @param viewId The id of the target view
     * @param value The column width.
     * @param unit The unit for [value], e.g. [android.util.TypedValue.COMPLEX_UNIT_DIP].
     */
    @RequiresApi(31)
    @JvmStatic
    public fun RemoteViews.setGridViewColumnWidth(@IdRes viewId: Int, value: Float, unit: Int) {
        Api31Impl.setIntDimen(this, viewId, "setColumnWidth", value, unit)
    }

    /**
     * Equivalent to calling [android.widget.GridView.setColumnWidth].
     *
     * @param viewId The id of the target view
     * @param columnWidth The resource id of a dimension resource for the column width.
     */
    @RequiresApi(31)
    @JvmStatic
    public fun RemoteViews.setGridViewColumnWidthDimen(
        @IdRes viewId: Int,
        @DimenRes columnWidth: Int,
    ) {
        Api31Impl.setIntDimen(this, viewId, "setColumnWidth", columnWidth)
    }

    /**
     * Equivalent to calling [android.widget.GridView.setColumnWidth].
     *
     * @param viewId The id of the target view
     * @param columnWidth The resource id of a dimension resource for the column width.
     */
    @RequiresApi(31)
    @JvmStatic
    public fun RemoteViews.setGridViewColumnWidthDimenAttr(
        @IdRes viewId: Int,
        @AttrRes columnWidth: Int,
    ) {
        Api31Impl.setIntDimenAttr(this, viewId, "setColumnWidth", columnWidth)
    }

    /**
     * Equivalent to calling [android.widget.GridView.setGravity].
     *
     * @param viewId The id of the target view
     * @param gravity The gravity to apply to this grid's children.
     */
    @RequiresApi(31)
    @JvmStatic
    public fun RemoteViews.setGridViewGravity(@IdRes viewId: Int, gravity: Int) {
        setInt(viewId, "setGravity", gravity)
    }

    /**
     * Equivalent to calling [android.widget.GridView.setHorizontalSpacing].
     *
     * @param viewId The id of the target view
     * @param value The amount of horizontal space between items.
     * @param unit The unit for [value], e.g. [android.util.TypedValue.COMPLEX_UNIT_DIP].
     */
    @RequiresApi(31)
    @JvmStatic
    public fun RemoteViews.setGridViewHorizontalSpacing(
        @IdRes viewId: Int,
        value: Float,
        unit: Int,
    ) {
        Api31Impl.setIntDimen(this, viewId, "setHorizontalSpacing", value, unit)
    }

    /**
     * Equivalent to calling [android.widget.GridView.setHorizontalSpacing].
     *
     * @param viewId The id of the target view
     * @param resId The resource id of a dimension resource for the amount of horizontal space
     *   between items.
     */
    @RequiresApi(31)
    @JvmStatic
    public fun RemoteViews.setGridViewHorizontalSpacingDimen(
        @IdRes viewId: Int,
        @DimenRes resId: Int,
    ) {
        Api31Impl.setIntDimen(this, viewId, "setHorizontalSpacing", resId)
    }

    /**
     * Equivalent to calling [android.widget.GridView.setHorizontalSpacing].
     *
     * @param viewId The id of the target view
     * @param resId The resource id of a dimension attribute for the amount of horizontal space
     *   between items.
     */
    @RequiresApi(31)
    @JvmStatic
    public fun RemoteViews.setGridViewHorizontalSpacingDimenAttr(
        @IdRes viewId: Int,
        @AttrRes resId: Int,
    ) {
        Api31Impl.setIntDimenAttr(this, viewId, "setHorizontalSpacing", resId)
    }

    /**
     * Equivalent to calling [android.widget.GridView.setNumColumns].
     *
     * @param viewId The id of the target view
     * @param numColumns The desired number of columns.
     */
    @RequiresApi(31)
    @JvmStatic
    public fun RemoteViews.setGridViewNumColumns(@IdRes viewId: Int, numColumns: Int) {
        setInt(viewId, "setNumColumns", numColumns)
    }

    /**
     * Equivalent to calling [android.widget.GridView.setStretchMode].
     *
     * @param viewId The id of the target view
     * @param stretchMode Either [android.widget.GridView.NO_STRETCH],
     *   [android.widget.GridView.STRETCH_SPACING],
     *   [android.widget.GridView.STRETCH_SPACING_UNIFORM], or
     *   [android.widget.GridView.STRETCH_COLUMN_WIDTH].
     */
    @RequiresApi(31)
    @JvmStatic
    public fun RemoteViews.setGridViewStretchMode(@IdRes viewId: Int, stretchMode: Int) {
        setInt(viewId, "setStretchMode", stretchMode)
    }

    /**
     * Equivalent to calling [android.widget.GridView.setVerticalSpacing].
     *
     * @param viewId The id of the target view
     * @param value The amount of vertical space between items.
     * @param unit The unit for [value], e.g. [android.util.TypedValue.COMPLEX_UNIT_DIP].
     */
    @RequiresApi(31)
    @JvmStatic
    public fun RemoteViews.setGridViewVerticalSpacing(@IdRes viewId: Int, value: Float, unit: Int) {
        Api31Impl.setIntDimen(this, viewId, "setVerticalSpacing", value, unit)
    }

    /**
     * Equivalent to calling [android.widget.GridView.setVerticalSpacing].
     *
     * @param viewId The id of the target view
     * @param resId The resource id of a dimension resource for the amount of vertical space between
     *   items.
     */
    @RequiresApi(31)
    @JvmStatic
    public fun RemoteViews.setGridViewVerticalSpacingDimen(
        @IdRes viewId: Int,
        @DimenRes resId: Int,
    ) {
        Api31Impl.setIntDimen(this, viewId, "setVerticalSpacing", resId)
    }

    /**
     * Equivalent to calling [android.widget.GridView.setVerticalSpacing].
     *
     * @param viewId The id of the target view
     * @param resId The resource id of a dimension attribute for the amount of vertical space
     *   between items.
     */
    @RequiresApi(31)
    @JvmStatic
    public fun RemoteViews.setGridViewVerticalSpacingDimenAttr(
        @IdRes viewId: Int,
        @AttrRes resId: Int,
    ) {
        Api31Impl.setIntDimenAttr(this, viewId, "setVerticalSpacing", resId)
    }

    /**
     * Equivalent to calling [android.widget.ImageView.setAdjustViewBounds].
     *
     * @param viewId The id of the target view
     * @param adjustViewBounds Whether to adjust the bounds of this view to preserve the original
     *   aspect ratio of the drawable.
     */
    @JvmStatic
    public fun RemoteViews.setImageViewAdjustViewBounds(
        @IdRes viewId: Int,
        adjustViewBounds: Boolean,
    ) {
        setBoolean(viewId, "setAdjustViewBounds", adjustViewBounds)
    }

    /**
     * Equivalent to calling [android.widget.ImageView.setColorFilter].
     *
     * @param viewId The id of the target view
     * @param color Color tint to apply.
     */
    @JvmStatic
    public fun RemoteViews.setImageViewColorFilter(@IdRes viewId: Int, @ColorInt color: Int) {
        setInt(viewId, "setColorFilter", color)
    }

    /**
     * Equivalent to calling [android.widget.ImageView.setColorFilter].
     *
     * @param viewId The id of the target view
     * @param notNight The color tint to apply when the UI is not in night mode.
     * @param night The color tint to apply when the UI is in night mode.
     */
    @RequiresApi(31)
    @JvmStatic
    public fun RemoteViews.setImageViewColorFilter(
        @IdRes viewId: Int,
        @ColorInt notNight: Int,
        @ColorInt night: Int,
    ) {
        Api31Impl.setColorInt(this, viewId, "setColorFilter", notNight, night)
    }

    /**
     * Equivalent to calling [android.widget.ImageView.setColorFilter].
     *
     * @param viewId The id of the target view
     * @param resId The resource id of the color tint to apply.
     */
    @RequiresApi(31)
    @JvmStatic
    public fun RemoteViews.setImageViewColorFilterResource(
        @IdRes viewId: Int,
        @ColorRes resId: Int,
    ) {
        Api31Impl.setColor(this, viewId, "setColorFilter", resId)
    }

    /**
     * Equivalent to calling [android.widget.ImageView.setColorFilter].
     *
     * @param viewId The id of the target view
     * @param resId The attribute id of the color tint to apply.
     */
    @RequiresApi(31)
    @JvmStatic
    public fun RemoteViews.setImageViewColorFilterAttr(@IdRes viewId: Int, @AttrRes resId: Int) {
        Api31Impl.setColorAttr(this, viewId, "setColorFilter", resId)
    }

    /**
     * Equivalent to calling [android.widget.ImageView.setImageLevel].
     *
     * @param viewId The id of the target view
     * @param level The new level for the image.
     */
    @JvmStatic
    public fun RemoteViews.setImageViewImageLevel(@IdRes viewId: Int, level: Int) {
        setInt(viewId, "setImageLevel", level)
    }

    /**
     * Equivalent to calling [android.widget.ImageView.setImageAlpha].
     *
     * @param viewId The id of the target view
     * @param alpha The alpha value that should be applied to the image (between 0 and 255
     *   inclusive, with 0 being transparent and 255 being opaque)
     */
    @JvmStatic
    public fun RemoteViews.setImageViewImageAlpha(@IdRes viewId: Int, alpha: Int) {
        // Note: setImageAlpha was added and is preferred to setAlpha since API 16.
        setInt(viewId, "setImageAlpha", alpha)
    }

    /**
     * Equivalent to calling [android.widget.ImageView.setImageTintBlendMode].
     *
     * @param viewId The id of the target view
     * @param blendMode The blending mode used to apply the tint, may be null to clear.
     */
    @RequiresApi(31)
    @JvmStatic
    public fun RemoteViews.setImageViewImageTintBlendMode(
        @IdRes viewId: Int,
        blendMode: BlendMode?,
    ) {
        Api31Impl.setBlendMode(this, viewId, "setImageTintBlendMode", blendMode)
    }

    /**
     * Equivalent to calling [android.widget.ImageView.setImageTintList].
     *
     * @param viewId The id of the target view
     * @param tint The tint to apply, may be null to clear tint
     */
    @RequiresApi(31)
    @JvmStatic
    public fun RemoteViews.setImageViewImageTintList(@IdRes viewId: Int, tint: ColorStateList?) {
        Api31Impl.setColorStateList(this, viewId, "setImageTintList", tint)
    }

    /**
     * Equivalent to calling [android.widget.ImageView.setImageTintList].
     *
     * @param viewId The id of the target view
     * @param notNightTint The tint to apply when the UI is not in night mode, may be null to clear
     *   tint.
     * @param nightTint The tint to apply when the UI is in night mode, may be null to clear tint.
     */
    @RequiresApi(31)
    @JvmStatic
    public fun RemoteViews.setImageViewImageTintList(
        @IdRes viewId: Int,
        notNightTint: ColorStateList?,
        nightTint: ColorStateList?,
    ) {
        Api31Impl.setColorStateList(this, viewId, "setImageTintList", notNightTint, nightTint)
    }

    /**
     * Equivalent to calling [android.widget.ImageView.setImageTintList].
     *
     * @param viewId The id of the target view
     * @param resId The resource id of the tint to apply, may be 0 to clear tint.
     */
    @RequiresApi(31)
    @JvmStatic
    public fun RemoteViews.setImageViewImageTintList(@IdRes viewId: Int, @ColorRes resId: Int) {
        Api31Impl.setColorStateList(this, viewId, "setImageTintList", resId)
    }

    /**
     * Equivalent to calling [android.widget.ImageView.setImageTintList].
     *
     * @param viewId The id of the target view
     * @param resId The attribute of the tint to apply, may be 0 to clear tint.
     */
    @RequiresApi(31)
    @JvmStatic
    public fun RemoteViews.setImageViewImageTintListAttr(@IdRes viewId: Int, @AttrRes resId: Int) {
        Api31Impl.setColorStateListAttr(this, viewId, "setImageTintList", resId)
    }

    /**
     * Equivalent to calling [android.widget.ImageView.setMaxHeight].
     *
     * @param viewId The id of the target view
     * @param maxHeight The maximum height of the view, in pixels.
     */
    @JvmStatic
    public fun RemoteViews.setImageViewMaxHeight(@IdRes viewId: Int, @Px maxHeight: Int) {
        setInt(viewId, "setMaxHeight", maxHeight)
    }

    /**
     * Equivalent to calling [android.widget.ImageView.setMaxHeight].
     *
     * @param viewId The id of the target view
     * @param value The maximum height of the view.
     * @param unit The unit for [value], e.g. [android.util.TypedValue.COMPLEX_UNIT_DIP].
     */
    @RequiresApi(31)
    @JvmStatic
    public fun RemoteViews.setImageViewMaxHeight(@IdRes viewId: Int, value: Float, unit: Int) {
        Api31Impl.setIntDimen(this, viewId, "setMaxHeight", value, unit)
    }

    /**
     * Equivalent to calling [android.widget.ImageView.setMaxHeight].
     *
     * @param viewId The id of the target view
     * @param resId A dimension resource identifier for maximum height of the view.
     */
    @RequiresApi(31)
    @JvmStatic
    public fun RemoteViews.setImageViewMaxHeightDimen(@IdRes viewId: Int, @DimenRes resId: Int) {
        Api31Impl.setIntDimen(this, viewId, "setMaxHeight", resId)
    }

    /**
     * Equivalent to calling [android.widget.ImageView.setMaxHeight].
     *
     * @param viewId The id of the target view
     * @param resId A dimension resource attribute for maximum height of the view.
     */
    @RequiresApi(31)
    @JvmStatic
    public fun RemoteViews.setImageViewMaxHeightDimenAttr(@IdRes viewId: Int, @AttrRes resId: Int) {
        Api31Impl.setIntDimenAttr(this, viewId, "setMaxHeight", resId)
    }

    /**
     * Equivalent to calling [android.widget.ImageView.setMaxWidth].
     *
     * @param viewId The id of the target view
     * @param maxWidth The maximum width of the view, in pixels.
     */
    @JvmStatic
    public fun RemoteViews.setImageViewMaxWidth(@IdRes viewId: Int, @Px maxWidth: Int) {
        setInt(viewId, "setMaxWidth", maxWidth)
    }

    /**
     * Equivalent to calling [android.widget.ImageView.setMaxWidth].
     *
     * @param viewId The id of the target view
     * @param value The maximum width of the view.
     * @param unit The unit for [value], e.g. [android.util.TypedValue.COMPLEX_UNIT_DIP].
     */
    @RequiresApi(31)
    @JvmStatic
    public fun RemoteViews.setImageViewMaxWidth(@IdRes viewId: Int, value: Float, unit: Int) {
        Api31Impl.setIntDimen(this, viewId, "setMaxWidth", value, unit)
    }

    /**
     * Equivalent to calling [android.widget.ImageView.setMaxWidth].
     *
     * @param viewId The id of the target view
     * @param resId A dimension resource identifier for maximum width of the view.
     */
    @RequiresApi(31)
    @JvmStatic
    public fun RemoteViews.setImageViewMaxWidthDimen(@IdRes viewId: Int, @DimenRes resId: Int) {
        Api31Impl.setIntDimen(this, viewId, "setMaxWidth", resId)
    }

    /**
     * Equivalent to calling [android.widget.ImageView.setMaxWidth].
     *
     * @param viewId The id of the target view
     * @param resId A dimension resource attribute for maximum width of the view.
     */
    @RequiresApi(31)
    @JvmStatic
    public fun RemoteViews.setImageViewMaxWidthDimenAttr(@IdRes viewId: Int, @AttrRes resId: Int) {
        Api31Impl.setIntDimenAttr(this, viewId, "setMaxWidth", resId)
    }

    /**
     * Equivalent to calling [android.widget.LinearLayout.setBaselineAligned].
     *
     * @param viewId The id of the target view
     * @param baselineAligned True to align widgets on their baseline, false otherwise.
     */
    @JvmStatic
    public fun RemoteViews.setLinearLayoutBaselineAligned(
        @IdRes viewId: Int,
        baselineAligned: Boolean,
    ) {
        setBoolean(viewId, "setBaselineAligned", baselineAligned)
    }

    /**
     * Equivalent to calling [android.widget.LinearLayout.setBaselineAlignedChildIndex].
     *
     * @param viewId The id of the target view
     * @param i True to align widgets on their baseline, false otherwise.
     */
    @JvmStatic
    public fun RemoteViews.setLinearLayoutBaselineAlignedChildIndex(@IdRes viewId: Int, i: Int) {
        setInt(viewId, "setBaselineAlignedChildIndex", i)
    }

    /**
     * Equivalent to calling [android.widget.LinearLayout.setGravity].
     *
     * @param viewId The id of the target view
     * @param gravity See [android.view.Gravity].
     */
    @JvmStatic
    public fun RemoteViews.setLinearLayoutGravity(@IdRes viewId: Int, gravity: Int) {
        setInt(viewId, "setGravity", gravity)
    }

    /**
     * Equivalent to calling [android.widget.LinearLayout.setHorizontalGravity].
     *
     * @param viewId The id of the target view
     * @param horizontalGravity See [android.view.Gravity].
     */
    @JvmStatic
    public fun RemoteViews.setLinearLayoutHorizontalGravity(
        @IdRes viewId: Int,
        horizontalGravity: Int,
    ) {
        setInt(viewId, "setHorizontalGravity", horizontalGravity)
    }

    /**
     * Equivalent to calling [android.widget.LinearLayout.setVerticalGravity].
     *
     * @param viewId The id of the target view
     * @param verticalGravity See [android.view.Gravity].
     */
    @JvmStatic
    public fun RemoteViews.setLinearLayoutVerticalGravity(
        @IdRes viewId: Int,
        verticalGravity: Int,
    ) {
        setInt(viewId, "setVerticalGravity", verticalGravity)
    }

    /**
     * Equivalent to calling [android.widget.LinearLayout.setMeasureWithLargestChildEnabled].
     *
     * @param viewId The id of the target view
     * @param enabled True to measure children with a weight using the minimum size of the largest
     *   child, false otherwise.
     */
    @JvmStatic
    public fun RemoteViews.setLinearLayoutMeasureWithLargestChildEnabled(
        @IdRes viewId: Int,
        enabled: Boolean,
    ) {
        setBoolean(viewId, "setMeasureWithLargestChildEnabled", enabled)
    }

    /**
     * Equivalent to calling [android.widget.LinearLayout.setWeightSum].
     *
     * @param viewId The id of the target view
     * @param weightSum A number greater than 0.0f, or a number lower than or equals to 0.0f if the
     *   weight sum should be computed from the children's layout_weight
     */
    @JvmStatic
    public fun RemoteViews.setLinearLayoutWeightSum(@IdRes viewId: Int, weightSum: Float) {
        setFloat(viewId, "setWeightSum", weightSum)
    }

    /**
     * Equivalent to calling [android.widget.ProgressBar.setIndeterminate].
     *
     * @param viewId The id of the target view
     * @param indeterminate True to enable the indeterminate mode.
     */
    @JvmStatic
    public fun RemoteViews.setProgressBarIndeterminate(@IdRes viewId: Int, indeterminate: Boolean) {
        setBoolean(viewId, "setIndeterminate", indeterminate)
    }

    /**
     * Equivalent to calling [android.widget.ProgressBar.setIndeterminateTintBlendMode].
     *
     * @param viewId The id of the target view
     * @param blendMode The blending mode used to apply the tint, may be null to clear.
     */
    @RequiresApi(31)
    @JvmStatic
    public fun RemoteViews.setProgressBarIndeterminateTintBlendMode(
        @IdRes viewId: Int,
        blendMode: BlendMode?,
    ) {
        Api31Impl.setBlendMode(this, viewId, "setIndeterminateTintBlendMode", blendMode)
    }

    /**
     * Equivalent to calling [android.widget.ProgressBar.setIndeterminateTintList].
     *
     * @param viewId The id of the target view
     * @param tint The tint to apply, may be null to clear tint.
     */
    @RequiresApi(31)
    @JvmStatic
    public fun RemoteViews.setProgressBarIndeterminateTintList(
        @IdRes viewId: Int,
        tint: ColorStateList?,
    ) {
        Api31Impl.setColorStateList(this, viewId, "setIndeterminateTintList", tint)
    }

    /**
     * Equivalent to calling [android.widget.ProgressBar.setIndeterminateTintList].
     *
     * @param viewId The id of the target view
     * @param notNightTint The tint to apply when the UI is not in night mode, may be null to clear
     *   tint.
     * @param nightTint The tint to apply when the UI is in night mode, may be null to clear tint.
     */
    @RequiresApi(31)
    @JvmStatic
    public fun RemoteViews.setProgressBarIndeterminateTintList(
        @IdRes viewId: Int,
        notNightTint: ColorStateList?,
        nightTint: ColorStateList?,
    ) {
        Api31Impl.setColorStateList(
            this,
            viewId,
            "setIndeterminateTintList",
            notNightTint,
            nightTint,
        )
    }

    /**
     * Equivalent to calling [android.widget.ProgressBar.setIndeterminateTintList].
     *
     * @param viewId The id of the target view
     * @param resId The resource id of the tint to apply, may be 0 to clear tint.
     */
    @RequiresApi(31)
    @JvmStatic
    public fun RemoteViews.setProgressBarIndeterminateTintList(
        @IdRes viewId: Int,
        @ColorRes resId: Int,
    ) {
        Api31Impl.setColorStateList(this, viewId, "setIndeterminateTintList", resId)
    }

    /**
     * Equivalent to calling [android.widget.ProgressBar.setIndeterminateTintList].
     *
     * @param viewId The id of the target view
     * @param resId The attribute id of the tint to apply, may be 0 to clear tint.
     */
    @RequiresApi(31)
    @JvmStatic
    public fun RemoteViews.setProgressBarIndeterminateTintListAttr(
        @IdRes viewId: Int,
        @AttrRes resId: Int,
    ) {
        Api31Impl.setColorStateListAttr(this, viewId, "setIndeterminateTintList", resId)
    }

    /**
     * Equivalent to calling [android.widget.ProgressBar.setMax].
     *
     * @param viewId The id of the target view
     * @param max The upper range of this progress bar.
     */
    @JvmStatic
    public fun RemoteViews.setProgressBarMax(@IdRes viewId: Int, max: Int) {
        setInt(viewId, "setMax", max)
    }

    /**
     * Equivalent to calling [android.widget.ProgressBar.setMin].
     *
     * @param viewId The id of the target view
     * @param min The lower range of this progress bar.
     */
    @RequiresApi(26)
    @JvmStatic
    public fun RemoteViews.setProgressBarMin(@IdRes viewId: Int, min: Int) {
        requireSdk(26, "setMin")
        setInt(viewId, "setMin", min)
    }

    /**
     * Equivalent to calling [android.widget.ProgressBar.setProgressTintBlendMode].
     *
     * @param viewId The id of the target view
     * @param blendMode The blending mode used to apply the tint, may be null to clear.
     */
    @RequiresApi(31)
    @JvmStatic
    public fun RemoteViews.setProgressBarProgressTintBlendMode(
        @IdRes viewId: Int,
        blendMode: BlendMode?,
    ) {
        Api31Impl.setBlendMode(this, viewId, "setProgressTintBlendMode", blendMode)
    }

    /**
     * Equivalent to calling [android.widget.ProgressBar.setProgress].
     *
     * @param viewId The id of the target view
     * @param progress The new progress.
     */
    @JvmStatic
    public fun RemoteViews.setProgressBarProgress(@IdRes viewId: Int, progress: Int) {
        setInt(viewId, "setProgress", progress)
    }

    /**
     * Equivalent to calling [android.widget.ProgressBar.setProgressTintList].
     *
     * @param viewId The id of the target view
     * @param tint The tint to apply, may be null to clear tint.
     */
    @RequiresApi(31)
    @JvmStatic
    public fun RemoteViews.setProgressBarProgressTintList(
        @IdRes viewId: Int,
        tint: ColorStateList?,
    ) {
        Api31Impl.setColorStateList(this, viewId, "setProgressTintList", tint)
    }

    /**
     * Equivalent to calling [android.widget.ProgressBar.setProgressTintList].
     *
     * @param viewId The id of the target view
     * @param notNightTint The tint to apply when the UI is not in night mode, may be null to clear
     *   tint.
     * @param nightTint The tint to apply when the UI is in night mode, may be null to clear tint.
     */
    @RequiresApi(31)
    @JvmStatic
    public fun RemoteViews.setProgressBarProgressTintList(
        @IdRes viewId: Int,
        notNightTint: ColorStateList?,
        nightTint: ColorStateList?,
    ) {
        Api31Impl.setColorStateList(this, viewId, "setProgressTintList", notNightTint, nightTint)
    }

    /**
     * Equivalent to calling [android.widget.ProgressBar.setProgressTintList].
     *
     * @param viewId The id of the target view
     * @param resId The resource id of the tint to apply, may be 0 to clear tint.
     */
    @RequiresApi(31)
    @JvmStatic
    public fun RemoteViews.setProgressBarProgressTintList(
        @IdRes viewId: Int,
        @ColorRes resId: Int,
    ) {
        Api31Impl.setColorStateList(this, viewId, "setProgressTintList", resId)
    }

    /**
     * Equivalent to calling [android.widget.ProgressBar.setProgressTintList].
     *
     * @param viewId The id of the target view
     * @param resId The attribute id of the tint to apply, may be 0 to clear tint.
     */
    @RequiresApi(31)
    @JvmStatic
    public fun RemoteViews.setProgressBarProgressTintListAttr(
        @IdRes viewId: Int,
        @AttrRes resId: Int,
    ) {
        Api31Impl.setColorStateListAttr(this, viewId, "setProgressTintList", resId)
    }

    /**
     * Equivalent to calling [android.widget.ProgressBar.setProgressBackgroundTintBlendMode].
     *
     * @param viewId The id of the target view
     * @param blendMode The blending mode used to apply the tint, may be null to clear.
     */
    @RequiresApi(31)
    @JvmStatic
    public fun RemoteViews.setProgressBarProgressBackgroundTintBlendMode(
        @IdRes viewId: Int,
        blendMode: BlendMode?,
    ) {
        Api31Impl.setBlendMode(this, viewId, "setProgressBackgroundTintBlendMode", blendMode)
    }

    /**
     * Equivalent to calling [android.widget.ProgressBar.setProgressBackgroundTintList].
     *
     * @param viewId The id of the target view
     * @param tint The tint to apply, may be null to clear tint.
     */
    @RequiresApi(31)
    @JvmStatic
    public fun RemoteViews.setProgressBarProgressBackgroundTintList(
        @IdRes viewId: Int,
        tint: ColorStateList?,
    ) {
        Api31Impl.setColorStateList(this, viewId, "setProgressBackgroundTintList", tint)
    }

    /**
     * Equivalent to calling [android.widget.ProgressBar.setProgressBackgroundTintList].
     *
     * @param viewId The id of the target view
     * @param notNightTint The tint to apply when the UI is not in night mode, may be null to clear
     *   tint.
     * @param nightTint The tint to apply when the UI is in night mode, may be null to clear tint.
     */
    @RequiresApi(31)
    @JvmStatic
    public fun RemoteViews.setProgressBarProgressBackgroundTintList(
        @IdRes viewId: Int,
        notNightTint: ColorStateList?,
        nightTint: ColorStateList?,
    ) {
        Api31Impl.setColorStateList(
            this,
            viewId,
            "setProgressBackgroundTintList",
            notNightTint,
            nightTint,
        )
    }

    /**
     * Equivalent to calling [android.widget.ProgressBar.setProgressBackgroundTintList].
     *
     * @param viewId The id of the target view
     * @param resId The resource id of the tint to apply, may be 0 to clear tint.
     */
    @RequiresApi(31)
    @JvmStatic
    public fun RemoteViews.setProgressBarProgressBackgroundTintList(
        @IdRes viewId: Int,
        @ColorRes resId: Int,
    ) {
        Api31Impl.setColorStateList(this, viewId, "setProgressBackgroundTintList", resId)
    }

    /**
     * Equivalent to calling [android.widget.ProgressBar.setProgressBackgroundTintList].
     *
     * @param viewId The id of the target view
     * @param resId The attribute of the tint to apply, may be 0 to clear tint.
     */
    @RequiresApi(31)
    @JvmStatic
    public fun RemoteViews.setProgressBarProgressBackgroundTintListAttr(
        @IdRes viewId: Int,
        @AttrRes resId: Int,
    ) {
        Api31Impl.setColorStateListAttr(this, viewId, "setProgressBackgroundTintList", resId)
    }

    /**
     * Equivalent to calling [android.widget.ProgressBar.setSecondaryProgress].
     *
     * @param viewId The id of the target view
     * @param secondaryProgress The new secondary progress.
     */
    @JvmStatic
    public fun RemoteViews.setProgressBarSecondaryProgress(
        @IdRes viewId: Int,
        secondaryProgress: Int,
    ) {
        setInt(viewId, "setSecondaryProgress", secondaryProgress)
    }

    /**
     * Equivalent to calling [android.widget.ProgressBar.setSecondaryProgressTintBlendMode].
     *
     * @param viewId The id of the target view
     * @param blendMode The blending mode used to apply the tint, may be null to clear.
     */
    @RequiresApi(31)
    @JvmStatic
    public fun RemoteViews.setProgressBarSecondaryProgressTintBlendMode(
        @IdRes viewId: Int,
        blendMode: BlendMode?,
    ) {
        Api31Impl.setBlendMode(this, viewId, "setSecondaryProgressTintBlendMode", blendMode)
    }

    /**
     * Equivalent to calling [android.widget.ProgressBar.setSecondaryProgressTintList].
     *
     * @param viewId The id of the target view
     * @param tint The tint to apply, may be null to clear tint.
     */
    @RequiresApi(31)
    @JvmStatic
    public fun RemoteViews.setProgressBarSecondaryProgressTintList(
        @IdRes viewId: Int,
        tint: ColorStateList?,
    ) {
        Api31Impl.setColorStateList(this, viewId, "setSecondaryProgressTintList", tint)
    }

    /**
     * Equivalent to calling [android.widget.ProgressBar.setSecondaryProgressTintList].
     *
     * @param viewId The id of the target view
     * @param notNightTint The tint to apply when the UI is not in night mode, may be null to clear
     *   tint.
     * @param nightTint The tint to apply when the UI is in night mode, may be null to clear tint.
     */
    @RequiresApi(31)
    @JvmStatic
    public fun RemoteViews.setProgressBarSecondaryProgressTintList(
        @IdRes viewId: Int,
        notNightTint: ColorStateList?,
        nightTint: ColorStateList?,
    ) {
        Api31Impl.setColorStateList(
            this,
            viewId,
            "setSecondaryProgressTintList",
            notNightTint,
            nightTint,
        )
    }

    /**
     * Equivalent to calling [android.widget.ProgressBar.setSecondaryProgressTintList].
     *
     * @param viewId The id of the target view
     * @param resId The resource id of the tint to apply, may be 0 to clear tint.
     */
    @RequiresApi(31)
    @JvmStatic
    public fun RemoteViews.setProgressBarSecondaryProgressTintList(
        @IdRes viewId: Int,
        @ColorRes resId: Int,
    ) {
        Api31Impl.setColorStateList(this, viewId, "setSecondaryProgressTintList", resId)
    }

    /**
     * Equivalent to calling [android.widget.ProgressBar.setSecondaryProgressTintList].
     *
     * @param viewId The id of the target view
     * @param resId The attribute of the tint to apply, may be 0 to clear tint.
     */
    @RequiresApi(31)
    @JvmStatic
    public fun RemoteViews.setProgressBarSecondaryProgressTintListAttr(
        @IdRes viewId: Int,
        @AttrRes resId: Int,
    ) {
        Api31Impl.setColorStateListAttr(this, viewId, "setSecondaryProgressTintList", resId)
    }

    /**
     * Equivalent to calling [android.widget.ProgressBar.setStateDescription].
     *
     * @param viewId The id of the target view
     * @param stateDescription The state description, or null to reset to the default ProgressBar
     *   state description.
     */
    @RequiresApi(31)
    @JvmStatic
    public fun RemoteViews.setProgressBarStateDescription(
        @IdRes viewId: Int,
        stateDescription: CharSequence?,
    ) {
        requireSdk(31, "setStateDescription")
        setCharSequence(viewId, "setStateDescription", stateDescription)
    }

    /**
     * Equivalent to calling [android.widget.ProgressBar.setStateDescription].
     *
     * @param viewId The id of the target view
     * @param resId The resource id of the state description, or 0 to reset to the default
     *   ProgressBar state description.
     */
    @RequiresApi(31)
    @JvmStatic
    public fun RemoteViews.setProgressBarStateDescription(
        @IdRes viewId: Int,
        @StringRes resId: Int,
    ) {
        Api31Impl.setCharSequence(this, viewId, "setStateDescription", resId)
    }

    /**
     * Equivalent to calling [android.widget.ProgressBar.setStateDescription].
     *
     * @param viewId The id of the target view
     * @param resId The attribute id of the state description, or 0 to reset to the default
     *   ProgressBar state description.
     */
    @RequiresApi(31)
    @JvmStatic
    public fun RemoteViews.setProgressBarStateDescriptionAttr(
        @IdRes viewId: Int,
        @AttrRes resId: Int,
    ) {
        Api31Impl.setCharSequenceAttr(this, viewId, "setStateDescription", resId)
    }

    /**
     * Equivalent to calling [android.widget.RelativeLayout.setGravity].
     *
     * @param viewId The id of the target view
     * @param gravity See [android.view.Gravity].
     */
    @JvmStatic
    public fun RemoteViews.setRelativeLayoutGravity(@IdRes viewId: Int, gravity: Int) {
        setInt(viewId, "setGravity", gravity)
    }

    /**
     * Equivalent to calling [android.widget.RelativeLayout.setHorizontalGravity].
     *
     * @param viewId The id of the target view
     * @param horizontalGravity See [android.view.Gravity].
     */
    @JvmStatic
    public fun RemoteViews.setRelativeLayoutHorizontalGravity(
        @IdRes viewId: Int,
        horizontalGravity: Int,
    ) {
        setInt(viewId, "setHorizontalGravity", horizontalGravity)
    }

    /**
     * Equivalent to calling [android.widget.RelativeLayout.setIgnoreGravity].
     *
     * @param viewId The id of the target view
     * @param childViewId The id of the child View to be ignored by gravity, or 0 if no View should
     *   be ignored.
     */
    @JvmStatic
    public fun RemoteViews.setRelativeLayoutIgnoreGravity(
        @IdRes viewId: Int,
        @IdRes childViewId: Int,
    ) {
        setInt(viewId, "setIgnoreGravity", childViewId)
    }

    /**
     * Equivalent to calling [android.widget.RelativeLayout.setVerticalGravity].
     *
     * @param viewId The id of the target view
     * @param verticalGravity See [android.view.Gravity].
     */
    @JvmStatic
    public fun RemoteViews.setRelativeLayoutVerticalGravity(
        @IdRes viewId: Int,
        verticalGravity: Int,
    ) {
        setInt(viewId, "setVerticalGravity", verticalGravity)
    }

    /**
     * Equivalent to calling [android.widget.Switch.setSwitchMinWidth].
     *
     * @param viewId The id of the target view
     * @param value Minimum width of the switch.
     * @param unit The unit for [value], e.g. [android.util.TypedValue.COMPLEX_UNIT_DIP].
     */
    @RequiresApi(31)
    @JvmStatic
    public fun RemoteViews.setSwitchMinWidth(@IdRes viewId: Int, value: Float, unit: Int) {
        Api31Impl.setIntDimen(this, viewId, "setSwitchMinWidth", value, unit)
    }

    /**
     * Equivalent to calling [android.widget.Switch.setSwitchMinWidth].
     *
     * @param viewId The id of the target view
     * @param resId The id of a dimension resource for the minimum width of the switch.
     */
    @RequiresApi(31)
    @JvmStatic
    public fun RemoteViews.setSwitchMinWidthDimen(@IdRes viewId: Int, @DimenRes resId: Int) {
        Api31Impl.setIntDimen(this, viewId, "setSwitchMinWidth", resId)
    }

    /**
     * Equivalent to calling [android.widget.Switch.setSwitchMinWidth].
     *
     * @param viewId The id of the target view
     * @param resId The id of a dimension attribute for the minimum width of the switch.
     */
    @RequiresApi(31)
    @JvmStatic
    public fun RemoteViews.setSwitchMinWidthDimenAttr(@IdRes viewId: Int, @AttrRes resId: Int) {
        Api31Impl.setIntDimenAttr(this, viewId, "setSwitchMinWidth", resId)
    }

    /**
     * Equivalent to calling [android.widget.Switch.setSwitchPadding].
     *
     * @param viewId The id of the target view
     * @param value Amount of padding.
     * @param unit The unit for [value], e.g. [android.util.TypedValue.COMPLEX_UNIT_DIP].
     */
    @RequiresApi(31)
    @JvmStatic
    public fun RemoteViews.setSwitchPadding(@IdRes viewId: Int, value: Float, unit: Int) {
        Api31Impl.setIntDimen(this, viewId, "setSwitchPadding", value, unit)
    }

    /**
     * Equivalent to calling [android.widget.Switch.setSwitchPadding].
     *
     * @param viewId The id of the target view
     * @param resId The id of a dimension resource for the amount of padding.
     */
    @RequiresApi(31)
    @JvmStatic
    public fun RemoteViews.setSwitchPaddingDimen(@IdRes viewId: Int, @DimenRes resId: Int) {
        Api31Impl.setIntDimen(this, viewId, "setSwitchPadding", resId)
    }

    /**
     * Equivalent to calling [android.widget.Switch.setSwitchPadding].
     *
     * @param viewId The id of the target view
     * @param resId The id of a dimension attribute for the amount of padding.
     */
    @RequiresApi(31)
    @JvmStatic
    public fun RemoteViews.setSwitchPaddingDimenAttr(@IdRes viewId: Int, @AttrRes resId: Int) {
        Api31Impl.setIntDimenAttr(this, viewId, "setSwitchPadding", resId)
    }

    /**
     * Equivalent to calling [android.widget.Switch.setShowText].
     *
     * @param viewId The id of the target view
     * @param showText True to display on/off text.
     */
    @RequiresApi(31)
    @JvmStatic
    public fun RemoteViews.setSwitchShowText(@IdRes viewId: Int, showText: Boolean) {
        requireSdk(31, "setShowText")
        setBoolean(viewId, "setShowText", showText)
    }

    /**
     * Equivalent to calling [android.widget.Switch.setSplitTrack].
     *
     * @param viewId The id of the target view
     * @param splitTrack Whether the track should be split by the thumb.
     */
    @RequiresApi(31)
    @JvmStatic
    public fun RemoteViews.setSwitchSplitTrack(@IdRes viewId: Int, splitTrack: Boolean) {
        requireSdk(31, "setSplitTrack")
        setBoolean(viewId, "setSplitTrack", splitTrack)
    }

    /**
     * Equivalent to calling [android.widget.Switch.setTextOff].
     *
     * @param viewId The id of the target view
     * @param textOff The text displayed when the button is not in the checked state.
     */
    @RequiresApi(31)
    @JvmStatic
    public fun RemoteViews.setSwitchTextOff(@IdRes viewId: Int, textOff: CharSequence?) {
        requireSdk(31, "setTextOff")
        setCharSequence(viewId, "setTextOff", textOff)
    }

    /**
     * Equivalent to calling [android.widget.Switch.setTextOff].
     *
     * @param viewId The id of the target view
     * @param resId The resource id for the text displayed when the button is not in the checked
     *   state.
     */
    @RequiresApi(31)
    @JvmStatic
    public fun RemoteViews.setSwitchTextOff(@IdRes viewId: Int, @StringRes resId: Int) {
        Api31Impl.setCharSequence(this, viewId, "setTextOff", resId)
    }

    /**
     * Equivalent to calling [android.widget.Switch.setTextOff].
     *
     * @param viewId The id of the target view
     * @param resId The attribute id for the text displayed when the button is not in the checked
     *   state.
     */
    @RequiresApi(31)
    @JvmStatic
    public fun RemoteViews.setSwitchTextOffAttr(@IdRes viewId: Int, @AttrRes resId: Int) {
        Api31Impl.setCharSequenceAttr(this, viewId, "setTextOff", resId)
    }

    /**
     * Equivalent to calling [android.widget.Switch.setTextOn].
     *
     * @param viewId The id of the target view
     * @param textOn The text displayed when the button is in the checked state.
     */
    @RequiresApi(31)
    @JvmStatic
    public fun RemoteViews.setSwitchTextOn(@IdRes viewId: Int, textOn: CharSequence?) {
        setCharSequence(viewId, "setTextOn", textOn)
    }

    /**
     * Equivalent to calling [android.widget.Switch.setTextOn].
     *
     * @param viewId The id of the target view
     * @param resId The resource id for the text displayed when the button is in the checked state.
     */
    @RequiresApi(31)
    @JvmStatic
    public fun RemoteViews.setSwitchTextOn(@IdRes viewId: Int, @StringRes resId: Int) {
        Api31Impl.setCharSequence(this, viewId, "setTextOn", resId)
    }

    /**
     * Equivalent to calling [android.widget.Switch.setTextOn].
     *
     * @param viewId The id of the target view
     * @param resId The attribute id for the text displayed when the button is in the checked state.
     */
    @RequiresApi(31)
    @JvmStatic
    public fun RemoteViews.setSwitchTextOnAttr(@IdRes viewId: Int, @AttrRes resId: Int) {
        Api31Impl.setCharSequenceAttr(this, viewId, "setTextOn", resId)
    }

    /**
     * Equivalent to calling [android.widget.Switch.setThumbIcon].
     *
     * @param viewId The id of the target view
     * @param icon An Icon holding the desired thumb, or null to clear the thumb.
     */
    @RequiresApi(31)
    @JvmStatic
    public fun RemoteViews.setSwitchThumbIcon(@IdRes viewId: Int, icon: Icon?) {
        setIcon(viewId, "setThumbIcon", icon)
    }

    /**
     * Equivalent to calling [android.widget.Switch.setThumbIcon].
     *
     * @param viewId The id of the target view
     * @param notNight An Icon holding the desired thumb when the UI is not in night mode, or null
     *   to clear the thumb.
     * @param night An Icon holding the desired thumb when the UI is in night mode, or null to clear
     *   the thumb.
     */
    @RequiresApi(31)
    @JvmStatic
    public fun RemoteViews.setSwitchThumbIcon(@IdRes viewId: Int, notNight: Icon?, night: Icon?) {
        Api31Impl.setIcon(this, viewId, "setThumbIcon", notNight, night)
    }

    /**
     * Equivalent to calling [android.widget.Switch.setThumbResource].
     *
     * @param viewId The id of the target view
     * @param resId Resource id of a thumb drawable.
     */
    @RequiresApi(31)
    @JvmStatic
    public fun RemoteViews.setSwitchThumbResource(@IdRes viewId: Int, @DrawableRes resId: Int) {
        requireSdk(31, "setThumbResource")
        setInt(viewId, "setThumbResource", resId)
    }

    /**
     * Equivalent to calling [android.widget.Switch.setThumbTextPadding].
     *
     * @param viewId The id of the target view
     * @param value Horizontal padding for switch thumb text.
     * @param unit The unit for [value], e.g. [android.util.TypedValue.COMPLEX_UNIT_DIP].
     */
    @RequiresApi(31)
    @JvmStatic
    public fun RemoteViews.setSwitchThumbTextPadding(@IdRes viewId: Int, value: Float, unit: Int) {
        Api31Impl.setIntDimen(this, viewId, "setThumbTextPadding", value, unit)
    }

    /**
     * Equivalent to calling [android.widget.Switch.setThumbTextPadding].
     *
     * @param viewId The id of the target view
     * @param resId The id of a dimension resource for the horizontal padding for switch thumb text.
     */
    @RequiresApi(31)
    @JvmStatic
    public fun RemoteViews.setSwitchThumbTextPaddingDimen(
        @IdRes viewId: Int,
        @DimenRes resId: Int,
    ) {
        Api31Impl.setIntDimen(this, viewId, "setThumbTextPadding", resId)
    }

    /**
     * Equivalent to calling [android.widget.Switch.setThumbTextPadding].
     *
     * @param viewId The id of the target view
     * @param resId The id of a dimension attribute for the horizontal padding for switch thumb
     *   text.
     */
    @RequiresApi(31)
    @JvmStatic
    public fun RemoteViews.setSwitchThumbTextPaddingDimenAttr(
        @IdRes viewId: Int,
        @AttrRes resId: Int,
    ) {
        Api31Impl.setIntDimenAttr(this, viewId, "setThumbTextPadding", resId)
    }

    /**
     * Equivalent to calling [android.widget.Switch.setThumbTintBlendMode].
     *
     * @param viewId The id of the target view
     * @param blendMode The blending mode used to apply the tint, may be null to clear tint.
     */
    @RequiresApi(31)
    @JvmStatic
    public fun RemoteViews.setSwitchThumbTintBlendMode(@IdRes viewId: Int, blendMode: BlendMode?) {
        Api31Impl.setBlendMode(this, viewId, "setThumbTintBlendMode", blendMode)
    }

    /**
     * Equivalent to calling [android.widget.Switch.setThumbTintList].
     *
     * @param viewId The id of the target view
     * @param tint The tint to apply, may be null to clear tint.
     */
    @RequiresApi(31)
    @JvmStatic
    public fun RemoteViews.setSwitchThumbTintList(@IdRes viewId: Int, tint: ColorStateList?) {
        Api31Impl.setColorStateList(this, viewId, "setThumbTintList", tint)
    }

    /**
     * Equivalent to calling [android.widget.Switch.setThumbTintList].
     *
     * @param viewId The id of the target view
     * @param notNight The tint to apply when the UI is not in night mode, may be null to clear
     *   tint.
     * @param night The tint to apply when the UI is in night mode, may be null to clear tint.
     */
    @RequiresApi(31)
    @JvmStatic
    public fun RemoteViews.setSwitchThumbTintList(
        @IdRes viewId: Int,
        notNight: ColorStateList?,
        night: ColorStateList?,
    ) {
        Api31Impl.setColorStateList(this, viewId, "setThumbTintList", notNight, night)
    }

    /**
     * Equivalent to calling [android.widget.Switch.setThumbTintList].
     *
     * @param viewId The id of the target view
     * @param resId The resource id for the tint to apply, may be 0 to clear tint.
     */
    @RequiresApi(31)
    @JvmStatic
    public fun RemoteViews.setSwitchThumbTintList(@IdRes viewId: Int, @ColorRes resId: Int) {
        Api31Impl.setColorStateList(this, viewId, "setThumbTintList", resId)
    }

    /**
     * Equivalent to calling [android.widget.Switch.setThumbTintList].
     *
     * @param viewId The id of the target view
     * @param resId The attribute id for the tint to apply, may be 0 to clear tint.
     */
    @RequiresApi(31)
    @JvmStatic
    public fun RemoteViews.setSwitchThumbTintListAttr(@IdRes viewId: Int, @AttrRes resId: Int) {
        Api31Impl.setColorStateListAttr(this, viewId, "setThumbTintList", resId)
    }

    /**
     * Equivalent to calling [android.widget.Switch.setTrackIcon].
     *
     * @param viewId The id of the target view
     * @param icon An Icon holding the desired track, or null to clear the track.
     */
    @RequiresApi(31)
    @JvmStatic
    public fun RemoteViews.setSwitchTrackIcon(@IdRes viewId: Int, icon: Icon?) {
        setIcon(viewId, "setTrackIcon", icon)
    }

    /**
     * Equivalent to calling [android.widget.Switch.setTrackIcon].
     *
     * @param viewId The id of the target view
     * @param notNight An Icon holding the desired track when the UI is not in night mode, or null
     *   to clear the track.
     * @param night An Icon holding the desired track when the UI is in night mode, or null to clear
     *   the track.
     */
    @RequiresApi(31)
    @JvmStatic
    public fun RemoteViews.setSwitchTrackIcon(@IdRes viewId: Int, notNight: Icon?, night: Icon?) {
        Api31Impl.setIcon(this, viewId, "setTrackIcon", notNight, night)
    }

    /**
     * Equivalent to calling [android.widget.Switch.setTrackResource].
     *
     * @param viewId The id of the target view
     * @param resId Resource id of a track drawable.
     */
    @RequiresApi(31)
    @JvmStatic
    public fun RemoteViews.setSwitchTrackResource(@IdRes viewId: Int, @DrawableRes resId: Int) {
        requireSdk(31, "setTrackResource")
        setInt(viewId, "setTrackResource", resId)
    }

    /**
     * Equivalent to calling [android.widget.Switch.setTrackTintBlendMode].
     *
     * @param viewId The id of the target view
     * @param blendMode The blending mode used to apply the tint, may be null to clear tint.
     */
    @RequiresApi(31)
    @JvmStatic
    public fun RemoteViews.setSwitchTrackTintBlendMode(@IdRes viewId: Int, blendMode: BlendMode?) {
        Api31Impl.setBlendMode(this, viewId, "setTrackTintBlendMode", blendMode)
    }

    /**
     * Equivalent to calling [android.widget.Switch.setTrackTintList].
     *
     * @param viewId The id of the target view
     * @param tint The tint to apply, may be null to clear tint.
     */
    @RequiresApi(31)
    @JvmStatic
    public fun RemoteViews.setSwitchTrackTintList(@IdRes viewId: Int, tint: ColorStateList?) {
        Api31Impl.setColorStateList(this, viewId, "setTrackTintList", tint)
    }

    /**
     * Equivalent to calling [android.widget.Switch.setTrackTintList].
     *
     * @param viewId The id of the target view
     * @param notNight The tint to apply when the UI is not in night mode, may be null to clear
     *   tint.
     * @param night The tint to apply when the UI is in night mode, may be null to clear tint.
     */
    @RequiresApi(31)
    @JvmStatic
    public fun RemoteViews.setSwitchTrackTintList(
        @IdRes viewId: Int,
        notNight: ColorStateList?,
        night: ColorStateList?,
    ) {
        Api31Impl.setColorStateList(this, viewId, "setTrackTintList", notNight, night)
    }

    /**
     * Equivalent to calling [android.widget.Switch.setTrackTintList].
     *
     * @param viewId The id of the target view
     * @param resId The resource id for the tint to apply, may be 0 to clear tint.
     */
    @RequiresApi(31)
    @JvmStatic
    public fun RemoteViews.setSwitchTrackTintList(@IdRes viewId: Int, @ColorRes resId: Int) {
        Api31Impl.setColorStateList(this, viewId, "setTrackTintList", resId)
    }

    /**
     * Equivalent to calling [android.widget.Switch.setTrackTintList].
     *
     * @param viewId The id of the target view
     * @param resId The attribute id for the tint to apply, may be 0 to clear tint.
     */
    @RequiresApi(31)
    @JvmStatic
    public fun RemoteViews.setSwitchTrackTintListAttr(@IdRes viewId: Int, @AttrRes resId: Int) {
        Api31Impl.setColorStateListAttr(this, viewId, "setTrackTintList", resId)
    }

    /**
     * Equivalent to calling [android.widget.TextClock.setFormat12Hour].
     *
     * @param viewId The id of the target view
     * @param format A date/time formatting pattern as described in
     *   [android.text.format.DateFormat].
     */
    @JvmStatic
    public fun RemoteViews.setTextClockFormat12Hour(@IdRes viewId: Int, format: CharSequence?) {
        setCharSequence(viewId, "setFormat12Hour", format)
    }

    /**
     * Equivalent to calling [android.widget.TextClock.setFormat12Hour].
     *
     * @param viewId The id of the target view
     * @param resId A resource id for a date/time formatting pattern as described in
     *   [android.text.format.DateFormat].
     */
    @RequiresApi(31)
    @JvmStatic
    public fun RemoteViews.setTextClockFormat12Hour(@IdRes viewId: Int, @StringRes resId: Int) {
        Api31Impl.setCharSequence(this, viewId, "setFormat12Hour", resId)
    }

    /**
     * Equivalent to calling [android.widget.TextClock.setFormat12Hour].
     *
     * @param viewId The id of the target view
     * @param resId An attribute id for a date/time formatting pattern as described in
     *   [android.text.format.DateFormat].
     */
    @RequiresApi(31)
    @JvmStatic
    public fun RemoteViews.setTextClockFormat12HourAttr(@IdRes viewId: Int, @AttrRes resId: Int) {
        Api31Impl.setCharSequenceAttr(this, viewId, "setFormat12Hour", resId)
    }

    /**
     * Equivalent to calling [android.widget.TextClock.setFormat24Hour].
     *
     * @param viewId The id of the target view
     * @param format A date/time formatting pattern as described in
     *   [android.text.format.DateFormat].
     */
    @JvmStatic
    public fun RemoteViews.setTextClockFormat24Hour(@IdRes viewId: Int, format: CharSequence?) {
        setCharSequence(viewId, "setFormat24Hour", format)
    }

    /**
     * Equivalent to calling [android.widget.TextClock.setFormat24Hour].
     *
     * @param viewId The id of the target view
     * @param resId A resource id for a date/time formatting pattern as described in
     *   [android.text.format.DateFormat].
     */
    @RequiresApi(31)
    @JvmStatic
    public fun RemoteViews.setTextClockFormat24Hour(@IdRes viewId: Int, @StringRes resId: Int) {
        Api31Impl.setCharSequence(this, viewId, "setFormat24Hour", resId)
    }

    /**
     * Equivalent to calling [android.widget.TextClock.setFormat24Hour].
     *
     * @param viewId The id of the target view
     * @param resId An attribute id for a date/time formatting pattern as described in
     *   [android.text.format.DateFormat].
     */
    @RequiresApi(31)
    @JvmStatic
    public fun RemoteViews.setTextClockFormat24HourAttr(@IdRes viewId: Int, @AttrRes resId: Int) {
        Api31Impl.setCharSequenceAttr(this, viewId, "setFormat24Hour", resId)
    }

    /**
     * Equivalent to calling [android.widget.TextClock.setTimeZone].
     *
     * @param viewId The id of the target view
     * @param timeZone The desired time zone's ID as specified in [java.util.TimeZone] or null to
     *   use the time zone specified by the user (system time zone).
     */
    @JvmStatic
    public fun RemoteViews.setTextClockTimeZone(@IdRes viewId: Int, timeZone: String?) {
        setString(viewId, "setTimeZone", timeZone)
    }

    /**
     * Equivalent to calling [android.widget.TextView.setAllCaps].
     *
     * @param viewId The id of the target view
     * @param allCaps Whether the text should display in all caps.
     */
    @RequiresApi(31)
    @JvmStatic
    public fun RemoteViews.setTextViewAllCaps(@IdRes viewId: Int, allCaps: Boolean) {
        requireSdk(31, "setAllCaps")
        setBoolean(viewId, "setAllCaps", allCaps)
    }

    /**
     * Equivalent to calling [android.widget.TextView.setAutoLinkMask].
     *
     * @param viewId The id of the target view
     * @param mask See [android.text.util.Linkify.ALL] and peers for possible values.
     */
    @JvmStatic
    public fun RemoteViews.setTextViewAutoLinkMask(@IdRes viewId: Int, mask: Int) {
        setInt(viewId, "setAutoLinkMask", mask)
    }

    /**
     * Equivalent to calling [android.widget.TextView.setCompoundDrawablePadding].
     *
     * @param viewId The id of the target view
     * @param pad The padding between the compound drawables and the text, in pixels.
     */
    @JvmStatic
    public fun RemoteViews.setTextViewCompoundDrawablePadding(@IdRes viewId: Int, @Px pad: Int) {
        setInt(viewId, "setCompoundDrawablePadding", pad)
    }

    /**
     * Equivalent to calling [android.widget.TextView.setCompoundDrawablePadding].
     *
     * @param viewId The id of the target view
     * @param value The padding between the compound drawables and the text.
     * @param unit The unit for [value], e.g. [android.util.TypedValue.COMPLEX_UNIT_DIP].
     */
    @RequiresApi(31)
    @JvmStatic
    public fun RemoteViews.setTextViewCompoundDrawablePadding(
        @IdRes viewId: Int,
        value: Float,
        unit: Int,
    ) {
        Api31Impl.setIntDimen(this, viewId, "setCompoundDrawablePadding", value, unit)
    }

    /**
     * Equivalent to calling [android.widget.TextView.setCompoundDrawablePadding].
     *
     * @param viewId The id of the target view
     * @param resId The id of a dimension resource for the padding between the compound drawables
     *   and the text.
     */
    @RequiresApi(31)
    @JvmStatic
    public fun RemoteViews.setTextViewCompoundDrawablePaddingDimen(
        @IdRes viewId: Int,
        @DimenRes resId: Int,
    ) {
        Api31Impl.setIntDimen(this, viewId, "setCompoundDrawablePadding", resId)
    }

    /**
     * Equivalent to calling [android.widget.TextView.setCompoundDrawablePadding].
     *
     * @param viewId The id of the target view
     * @param resId The id of a dimension attribute for the padding between the compound drawables
     *   and the text.
     */
    @RequiresApi(31)
    @JvmStatic
    public fun RemoteViews.setTextViewCompoundDrawablePaddingDimenAttr(
        @IdRes viewId: Int,
        @AttrRes resId: Int,
    ) {
        Api31Impl.setIntDimenAttr(this, viewId, "setCompoundDrawablePadding", resId)
    }

    /**
     * Equivalent to calling [android.widget.TextView.setEms].
     *
     * @param viewId The id of the target view
     * @param ems The width of the TextView, in ems.
     */
    @JvmStatic
    public fun RemoteViews.setTextViewEms(@IdRes viewId: Int, ems: Int) {
        setInt(viewId, "setEms", ems)
    }

    /**
     * Equivalent to calling [android.widget.TextView.setError].
     *
     * @param viewId The id of the target view
     * @param error The error message for the TextView.
     */
    @JvmStatic
    public fun RemoteViews.setTextViewError(@IdRes viewId: Int, error: CharSequence?) {
        setCharSequence(viewId, "setError", error)
    }

    /**
     * Equivalent to calling [android.widget.TextView.setError].
     *
     * @param viewId The id of the target view
     * @param resId A string resource for the error.
     */
    @RequiresApi(31)
    @JvmStatic
    public fun RemoteViews.setTextViewError(@IdRes viewId: Int, @StringRes resId: Int) {
        // Note: Unlike setHint and setText, there's no API to pass a resId as an int directly, so
        // this is only available with the setCharSequence API adding in API 31.
        Api31Impl.setCharSequence(this, viewId, "setError", resId)
    }

    /**
     * Equivalent to calling [android.widget.TextView.setError].
     *
     * @param viewId The id of the target view
     * @param resId A string attribute for the error.
     */
    @RequiresApi(31)
    @JvmStatic
    public fun RemoteViews.setTextViewErrorAttr(@IdRes viewId: Int, @AttrRes resId: Int) {
        Api31Impl.setCharSequenceAttr(this, viewId, "setError", resId)
    }

    /**
     * Equivalent to calling [android.widget.TextView.setFontFeatureSettings].
     *
     * @param viewId The id of the target view
     * @param fontFeatureSettings Font feature settings represented as CSS compatible string.
     */
    @JvmStatic
    public fun RemoteViews.setTextViewFontFeatureSettings(
        @IdRes viewId: Int,
        fontFeatureSettings: String,
    ) {
        setString(viewId, "setFontFeatureSettings", fontFeatureSettings)
    }

    /**
     * Equivalent to calling [android.widget.TextView.setGravity].
     *
     * @param viewId The id of the target view
     * @param gravity The gravity value, from [android.view.Gravity].
     */
    @RequiresApi(31)
    @JvmStatic
    public fun RemoteViews.setTextViewGravity(@IdRes viewId: Int, gravity: Int) {
        requireSdk(31, "setGravity")
        setInt(viewId, "setGravity", gravity)
    }

    /**
     * Equivalent to calling [android.widget.TextView.setHeight].
     *
     * @param viewId The id of the target view
     * @param pixels The height of the TextView, in pixels.
     */
    @JvmStatic
    public fun RemoteViews.setTextViewHeight(@IdRes viewId: Int, @Px pixels: Int) {
        setInt(viewId, "setHeight", pixels)
    }

    /**
     * Equivalent to calling [android.widget.TextView.setHeight].
     *
     * @param viewId The id of the target view
     * @param value The height of the TextView.
     * @param unit The unit for [value], e.g. [android.util.TypedValue.COMPLEX_UNIT_DIP].
     */
    @RequiresApi(31)
    @JvmStatic
    public fun RemoteViews.setTextViewHeight(@IdRes viewId: Int, value: Float, unit: Int) {
        Api31Impl.setIntDimen(this, viewId, "setHeight", value, unit)
    }

    /**
     * Equivalent to calling [android.widget.TextView.setHeight].
     *
     * @param viewId The id of the target view
     * @param resId The id of a dimension resource for the height of the TextView.
     */
    @RequiresApi(31)
    @JvmStatic
    public fun RemoteViews.setTextViewHeightDimen(@IdRes viewId: Int, @DimenRes resId: Int) {
        Api31Impl.setIntDimen(this, viewId, "setHeight", resId)
    }

    /**
     * Equivalent to calling [android.widget.TextView.setHeight].
     *
     * @param viewId The id of the target view
     * @param resId The id of a dimension attribute for the height of the TextView.
     */
    @RequiresApi(31)
    @JvmStatic
    public fun RemoteViews.setTextViewHeightDimenAttr(@IdRes viewId: Int, @AttrRes resId: Int) {
        Api31Impl.setIntDimenAttr(this, viewId, "setHeight", resId)
    }

    /**
     * Equivalent to calling [android.widget.TextView.setHighlightColor].
     *
     * @param viewId The id of the target view
     * @param color The highlight color to use.
     */
    @JvmStatic
    public fun RemoteViews.setTextViewHighlightColor(@IdRes viewId: Int, @ColorInt color: Int) {
        setInt(viewId, "setHighlightColor", color)
    }

    /**
     * Equivalent to calling [android.widget.TextView.setHighlightColor].
     *
     * @param viewId The id of the target view
     * @param notNight The highlight color to use when night mode is not active.
     * @param night The highlight color to use when night mode is active.
     */
    @RequiresApi(31)
    @JvmStatic
    public fun RemoteViews.setTextViewHighlightColor(
        @IdRes viewId: Int,
        @ColorInt notNight: Int,
        @ColorInt night: Int,
    ) {
        Api31Impl.setColorInt(this, viewId, "setHighlightColor", notNight, night)
    }

    /**
     * Equivalent to calling [android.widget.TextView.setHighlightColor].
     *
     * @param viewId The id of the target view
     * @param resId The resource id for the highlight color.
     */
    @RequiresApi(31)
    @JvmStatic
    public fun RemoteViews.setTextViewHighlightColorResource(
        @IdRes viewId: Int,
        @ColorRes resId: Int,
    ) {
        Api31Impl.setColor(this, viewId, "setHighlightColor", resId)
    }

    /**
     * Equivalent to calling [android.widget.TextView.setHighlightColor].
     *
     * @param viewId The id of the target view
     * @param resId The attribute id for the highlight color.
     */
    @RequiresApi(31)
    @JvmStatic
    public fun RemoteViews.setTextViewHighlightColorAttr(@IdRes viewId: Int, @AttrRes resId: Int) {
        Api31Impl.setColorAttr(this, viewId, "setHighlightColor", resId)
    }

    /**
     * Equivalent to calling [android.widget.TextView.setHint].
     *
     * @param viewId The id of the target view
     * @param hint The hint for the TextView.
     */
    @JvmStatic
    public fun RemoteViews.setTextViewHint(@IdRes viewId: Int, hint: CharSequence?) {
        setCharSequence(viewId, "setHint", hint)
    }

    /**
     * Equivalent to calling [android.widget.TextView.setHint].
     *
     * @param viewId The id of the target view
     * @param resId A string resource for the hint.
     */
    @JvmStatic
    public fun RemoteViews.setTextViewHint(@IdRes viewId: Int, @StringRes resId: Int) {
        // Note: TextView.setHint(int) can be used to do this on any API instead of needing the
        // setCharSequence method added in API 31.
        setInt(viewId, "setHint", resId)
    }

    /**
     * Equivalent to calling [android.widget.TextView.setHint].
     *
     * @param viewId The id of the target view
     * @param resId A string attribute for the hint.
     */
    @RequiresApi(31)
    @JvmStatic
    public fun RemoteViews.setTextViewHintAttr(@IdRes viewId: Int, @AttrRes resId: Int) {
        Api31Impl.setCharSequenceAttr(this, viewId, "setHint", resId)
    }

    /**
     * Equivalent to calling [android.widget.TextView.setHintTextColor].
     *
     * @param viewId The id of the target view
     * @param color The hint text color to use.
     */
    @JvmStatic
    public fun RemoteViews.setTextViewHintTextColor(@IdRes viewId: Int, @ColorInt color: Int) {
        setInt(viewId, "setHintTextColor", color)
    }

    /**
     * Equivalent to calling [android.widget.TextView.setHintTextColor].
     *
     * @param viewId The id of the target view
     * @param notNight The hint text color to use when night mode is not active.
     * @param night The hint text color to use when night mode is active.
     */
    @RequiresApi(31)
    @JvmStatic
    public fun RemoteViews.setTextViewHintTextColor(
        @IdRes viewId: Int,
        @ColorInt notNight: Int,
        @ColorInt night: Int,
    ) {
        Api31Impl.setColorInt(this, viewId, "setHintTextColor", notNight, night)
    }

    /**
     * Equivalent to calling [android.widget.TextView.setHintTextColor].
     *
     * @param viewId The id of the target view
     * @param resId The resource id for the hint text color.
     */
    @RequiresApi(31)
    @JvmStatic
    public fun RemoteViews.setTextViewHintTextColorResource(
        @IdRes viewId: Int,
        @ColorRes resId: Int,
    ) {
        Api31Impl.setColor(this, viewId, "setHintTextColor", resId)
    }

    /**
     * Equivalent to calling [android.widget.TextView.setHintTextColor].
     *
     * @param viewId The id of the target view
     * @param resId The attribute id for the hint text color.
     */
    @RequiresApi(31)
    @JvmStatic
    public fun RemoteViews.setTextViewHintTextColorAttr(@IdRes viewId: Int, @AttrRes resId: Int) {
        Api31Impl.setColorAttr(this, viewId, "setHintTextColor", resId)
    }

    /**
     * Equivalent to calling [android.widget.TextView.setJustificationMode].
     *
     * @param viewId The id of the target view
     * @param justificationMode The justification mode to set.
     */
    @RequiresApi(31)
    @JvmStatic
    public fun RemoteViews.setTextViewJustificationMode(
        @IdRes viewId: Int,
        justificationMode: Int,
    ) {
        requireSdk(31, "setJustificationMode")
        setInt(viewId, "setJustificationMode", justificationMode)
    }

    /**
     * Equivalent to calling [android.widget.TextView.setLetterSpacing].
     *
     * @param viewId The id of the target view
     * @param letterSpacing A text letter-space value in ems.
     */
    @JvmStatic
    public fun RemoteViews.setTextViewLetterSpacing(@IdRes viewId: Int, letterSpacing: Float) {
        setFloat(viewId, "setLetterSpacing", letterSpacing)
    }

    /**
     * Equivalent to calling [android.widget.TextView.setLineHeight].
     *
     * @param viewId The id of the target view
     * @param value The value of the dimension for the line height.
     * @param unit The unit for [value], e.g. [android.util.TypedValue.COMPLEX_UNIT_DIP].
     */
    @RequiresApi(31)
    @JvmStatic
    public fun RemoteViews.setTextViewLineHeight(@IdRes viewId: Int, value: Float, unit: Int) {
        Api31Impl.setIntDimen(this, viewId, "setLineHeight", value, unit)
    }

    /**
     * Equivalent to calling [android.widget.TextView.setLineHeight].
     *
     * @param viewId The id of the target view
     * @param resId The id of a dimension resource for the line height.
     */
    @RequiresApi(31)
    @JvmStatic
    public fun RemoteViews.setTextViewLineHeightDimen(@IdRes viewId: Int, @DimenRes resId: Int) {
        Api31Impl.setIntDimen(this, viewId, "setLineHeight", resId)
    }

    /**
     * Equivalent to calling [android.widget.TextView.setLineHeight].
     *
     * @param viewId The id of the target view
     * @param resId The id of a dimension attribute for the line height.
     */
    @RequiresApi(31)
    @JvmStatic
    public fun RemoteViews.setTextViewLineHeightDimenAttr(@IdRes viewId: Int, @AttrRes resId: Int) {
        Api31Impl.setIntDimenAttr(this, viewId, "setLineHeight", resId)
    }

    /**
     * Equivalent to calling [android.widget.TextView.setLines].
     *
     * @param viewId The id of the target view
     * @param lines The number of lines for the height of the TextView.
     */
    @JvmStatic
    public fun RemoteViews.setTextViewLines(@IdRes viewId: Int, lines: Int) {
        setInt(viewId, "setLines", lines)
    }

    /**
     * Equivalent to calling [android.widget.TextView.setLinkTextColor].
     *
     * @param viewId The id of the target view
     * @param color The link text color to use.
     */
    @JvmStatic
    public fun RemoteViews.setTextViewLinkTextColor(@IdRes viewId: Int, @ColorInt color: Int) {
        setInt(viewId, "setLinkTextColor", color)
    }

    /**
     * Equivalent to calling [android.widget.TextView.setLinkTextColor].
     *
     * @param viewId The id of the target view
     * @param notNight The link text color to use when night mode is not active.
     * @param night The link text color to use when night mode is active.
     */
    @RequiresApi(31)
    @JvmStatic
    public fun RemoteViews.setTextViewLinkTextColor(
        @IdRes viewId: Int,
        @ColorInt notNight: Int,
        @ColorInt night: Int,
    ) {
        Api31Impl.setColorInt(this, viewId, "setLinkTextColor", notNight, night)
    }

    /**
     * Equivalent to calling [android.widget.TextView.setLinkTextColor].
     *
     * @param viewId The id of the target view
     * @param resId The resource id for the link text color.
     */
    @RequiresApi(31)
    @JvmStatic
    public fun RemoteViews.setTextViewLinkTextColorResource(
        @IdRes viewId: Int,
        @ColorRes resId: Int,
    ) {
        Api31Impl.setColor(this, viewId, "setLinkTextColor", resId)
    }

    /**
     * Equivalent to calling [android.widget.TextView.setLinkTextColor].
     *
     * @param viewId The id of the target view
     * @param resId The attribute id for the link text color.
     */
    @RequiresApi(31)
    @JvmStatic
    public fun RemoteViews.setTextViewLinkTextColorAttr(@IdRes viewId: Int, @AttrRes resId: Int) {
        Api31Impl.setColorAttr(this, viewId, "setLinkTextColor", resId)
    }

    /**
     * Equivalent to calling [android.widget.TextView.setLinksClickable].
     *
     * @param viewId The id of the target view
     * @param whether Whether detected links will be clickable (see TextView documentation).
     */
    @JvmStatic
    public fun RemoteViews.setTextViewLinksClickable(@IdRes viewId: Int, whether: Boolean) {
        setBoolean(viewId, "setLinksClickable", whether)
    }

    /**
     * Equivalent to calling [android.widget.TextView.setMaxHeight].
     *
     * @param viewId The id of the target view
     * @param maxHeight The maximum height of the TextView, in pixels.
     */
    @JvmStatic
    public fun RemoteViews.setTextViewMaxHeight(@IdRes viewId: Int, @Px maxHeight: Int) {
        setInt(viewId, "setMaxHeight", maxHeight)
    }

    /**
     * Equivalent to calling [android.widget.TextView.setMaxHeight].
     *
     * @param viewId The id of the target view
     * @param value The maximum height of the TextView.
     * @param unit The unit for [value], e.g. [android.util.TypedValue.COMPLEX_UNIT_DIP].
     */
    @RequiresApi(31)
    @JvmStatic
    public fun RemoteViews.setTextViewMaxHeight(@IdRes viewId: Int, value: Float, unit: Int) {
        Api31Impl.setIntDimen(this, viewId, "setMaxHeight", value, unit)
    }

    /**
     * Equivalent to calling [android.widget.TextView.setMaxHeight].
     *
     * @param viewId The id of the target view
     * @param resId The id of a dimension resource for the maximum height of the TextView.
     */
    @RequiresApi(31)
    @JvmStatic
    public fun RemoteViews.setTextViewMaxHeightDimen(@IdRes viewId: Int, @DimenRes resId: Int) {
        Api31Impl.setIntDimen(this, viewId, "setMaxHeight", resId)
    }

    /**
     * Equivalent to calling [android.widget.TextView.setMaxHeight].
     *
     * @param viewId The id of the target view
     * @param resId The id of a dimension attribute for the maximum height of the TextView.
     */
    @RequiresApi(31)
    @JvmStatic
    public fun RemoteViews.setTextViewMaxHeightDimenAttr(@IdRes viewId: Int, @AttrRes resId: Int) {
        Api31Impl.setIntDimenAttr(this, viewId, "setMaxHeight", resId)
    }

    /**
     * Equivalent to calling [android.widget.TextView.setMaxEms].
     *
     * @param viewId The id of the target view
     * @param maxems The maximum width of the TextView, in ems.
     */
    @JvmStatic
    public fun RemoteViews.setTextViewMaxEms(@IdRes viewId: Int, maxems: Int) {
        setInt(viewId, "setMaxEms", maxems)
    }

    /**
     * Equivalent to calling [android.widget.TextView.setMaxLines].
     *
     * @param viewId The id of the target view
     * @param maxLines The maximum number of lines for the height of the TextView.
     */
    @JvmStatic
    public fun RemoteViews.setTextViewMaxLines(@IdRes viewId: Int, maxLines: Int) {
        setInt(viewId, "setMaxLines", maxLines)
    }

    /**
     * Equivalent to calling [android.widget.TextView.setMinEms].
     *
     * @param viewId The id of the target view
     * @param minems The minimum width of the TextView, in ems.
     */
    @JvmStatic
    public fun RemoteViews.setTextViewMinEms(@IdRes viewId: Int, minems: Int) {
        setInt(viewId, "setMinEms", minems)
    }

    /**
     * Equivalent to calling [android.widget.TextView.setMaxWidth].
     *
     * @param viewId The id of the target view
     * @param maxWidth The maximum width of the TextView, in pixels.
     */
    @JvmStatic
    public fun RemoteViews.setTextViewMaxWidth(@IdRes viewId: Int, @Px maxWidth: Int) {
        setInt(viewId, "setMaxWidth", maxWidth)
    }

    /**
     * Equivalent to calling [android.widget.TextView.setMaxWidth].
     *
     * @param viewId The id of the target view
     * @param value The maximum width of the TextView.
     * @param unit The unit for [value], e.g. [android.util.TypedValue.COMPLEX_UNIT_DIP].
     */
    @RequiresApi(31)
    @JvmStatic
    public fun RemoteViews.setTextViewMaxWidth(@IdRes viewId: Int, value: Float, unit: Int) {
        Api31Impl.setIntDimen(this, viewId, "setMaxWidth", value, unit)
    }

    /**
     * Equivalent to calling [android.widget.TextView.setMaxWidth].
     *
     * @param viewId The id of the target view
     * @param resId The id of a dimension resource for the maximum width of the TextView.
     */
    @RequiresApi(31)
    @JvmStatic
    public fun RemoteViews.setTextViewMaxWidthDimen(@IdRes viewId: Int, @DimenRes resId: Int) {
        Api31Impl.setIntDimen(this, viewId, "setMaxWidth", resId)
    }

    /**
     * Equivalent to calling [android.widget.TextView.setMaxWidth].
     *
     * @param viewId The id of the target view
     * @param resId The id of a dimension attribute for the maximum width of the TextView.
     */
    @RequiresApi(31)
    @JvmStatic
    public fun RemoteViews.setTextViewMaxWidthDimenAttr(@IdRes viewId: Int, @AttrRes resId: Int) {
        Api31Impl.setIntDimenAttr(this, viewId, "setMaxWidth", resId)
    }

    /**
     * Equivalent to calling [android.widget.TextView.setMinHeight].
     *
     * @param viewId The id of the target view
     * @param minHeight The minimum height of the TextView, in pixels.
     */
    @JvmStatic
    public fun RemoteViews.setTextViewMinHeight(@IdRes viewId: Int, @Px minHeight: Int) {
        setInt(viewId, "setMinHeight", minHeight)
    }

    /**
     * Equivalent to calling [android.widget.TextView.setMinHeight].
     *
     * @param viewId The id of the target view
     * @param value The minimum height of the TextView.
     * @param unit The unit for [value], e.g. [android.util.TypedValue.COMPLEX_UNIT_DIP].
     */
    @RequiresApi(31)
    @JvmStatic
    public fun RemoteViews.setTextViewMinHeight(@IdRes viewId: Int, value: Float, unit: Int) {
        Api31Impl.setIntDimen(this, viewId, "setMinHeight", value, unit)
    }

    /**
     * Equivalent to calling [android.widget.TextView.setMinHeight].
     *
     * @param viewId The id of the target view
     * @param resId The id of a dimension resource for the minimum height of the TextView.
     */
    @RequiresApi(31)
    @JvmStatic
    public fun RemoteViews.setTextViewMinHeightDimen(@IdRes viewId: Int, @DimenRes resId: Int) {
        Api31Impl.setIntDimen(this, viewId, "setMinHeight", resId)
    }

    /**
     * Equivalent to calling [android.widget.TextView.setMinHeight].
     *
     * @param viewId The id of the target view
     * @param resId The id of a dimension attribute for the minimum height of the TextView.
     */
    @RequiresApi(31)
    @JvmStatic
    public fun RemoteViews.setTextViewMinHeightDimenAttr(@IdRes viewId: Int, @AttrRes resId: Int) {
        Api31Impl.setIntDimenAttr(this, viewId, "setMinHeight", resId)
    }

    /**
     * Equivalent to calling [android.widget.TextView.setMinLines].
     *
     * @param viewId The id of the target view
     * @param minLines The minimum number of lines for the height of the TextView.
     */
    @JvmStatic
    public fun RemoteViews.setTextViewMinLines(@IdRes viewId: Int, minLines: Int) {
        setInt(viewId, "setMinLines", minLines)
    }

    /**
     * Equivalent to calling [android.widget.TextView.setMinWidth].
     *
     * @param viewId The id of the target view
     * @param minWidth The minimum width of the TextView, in pixels.
     */
    @JvmStatic
    public fun RemoteViews.setTextViewMinWidth(@IdRes viewId: Int, @Px minWidth: Int) {
        setInt(viewId, "setMinWidth", minWidth)
    }

    /**
     * Equivalent to calling [android.widget.TextView.setMinWidth].
     *
     * @param viewId The id of the target view
     * @param value The minimum width of the TextView.
     * @param unit The unit for [value], e.g. [android.util.TypedValue.COMPLEX_UNIT_DIP].
     */
    @RequiresApi(31)
    @JvmStatic
    public fun RemoteViews.setTextViewMinWidth(@IdRes viewId: Int, value: Float, unit: Int) {
        Api31Impl.setIntDimen(this, viewId, "setMinWidth", value, unit)
    }

    /**
     * Equivalent to calling [android.widget.TextView.setMinWidth].
     *
     * @param viewId The id of the target view
     * @param resId The id of a dimension resource for the minimum width of the TextView.
     */
    @RequiresApi(31)
    @JvmStatic
    public fun RemoteViews.setTextViewMinWidthDimen(@IdRes viewId: Int, @DimenRes resId: Int) {
        Api31Impl.setIntDimen(this, viewId, "setMinWidth", resId)
    }

    /**
     * Equivalent to calling [android.widget.TextView.setMinWidth].
     *
     * @param viewId The id of the target view
     * @param resId The id of a dimension attribute for the minimum width of the TextView.
     */
    @RequiresApi(31)
    @JvmStatic
    public fun RemoteViews.setTextViewMinWidthDimenAttr(@IdRes viewId: Int, @AttrRes resId: Int) {
        Api31Impl.setIntDimenAttr(this, viewId, "setMinWidth", resId)
    }

    /**
     * Equivalent to calling [android.widget.TextView.setPaintFlags].
     *
     * @param viewId The id of the target view
     * @param flags The flags for the text paint.
     */
    @JvmStatic
    public fun RemoteViews.setTextViewPaintFlags(@IdRes viewId: Int, flags: Int) {
        setInt(viewId, "setPaintFlags", flags)
    }

    /**
     * Equivalent to calling [android.widget.TextView.setSelectAllOnFocus].
     *
     * @param viewId The id of the target view
     * @param selectAllOnFocus Whether to select all text when the TextView is focused.
     */
    @JvmStatic
    public fun RemoteViews.setTextViewSelectAllOnFocus(
        @IdRes viewId: Int,
        selectAllOnFocus: Boolean,
    ) {
        setBoolean(viewId, "setSelectAllOnFocus", selectAllOnFocus)
    }

    /**
     * Equivalent to calling [android.widget.TextView.setSingleLine].
     *
     * @param viewId The id of the target view
     * @param singleLine Whether the TextView is single-line.
     */
    @JvmStatic
    public fun RemoteViews.setTextViewSingleLine(@IdRes viewId: Int, singleLine: Boolean) {
        setBoolean(viewId, "setSingleLine", singleLine)
    }

    /**
     * Equivalent to calling [android.widget.TextView.setText].
     *
     * @param viewId The id of the target view
     * @param resId A string resource for the text.
     */
    @JvmStatic
    public fun RemoteViews.setTextViewText(@IdRes viewId: Int, @StringRes resId: Int) {
        // Note: TextView.setText(int) can be used to do this on any API instead of needing the
        // setCharSequence method added in API 31.
        setInt(viewId, "setText", resId)
    }

    /**
     * Equivalent to calling [android.widget.TextView.setText].
     *
     * @param viewId The id of the target view
     * @param resId A string attribute for the text.
     */
    @RequiresApi(31)
    @JvmStatic
    public fun RemoteViews.setTextViewTextAttr(@IdRes viewId: Int, @AttrRes resId: Int) {
        Api31Impl.setCharSequenceAttr(this, viewId, "setText", resId)
    }

    /**
     * Equivalent to calling [android.widget.TextView.setTextColor].
     *
     * @param viewId The id of the target view
     * @param color The text color to use.
     */
    @JvmStatic
    public fun RemoteViews.setTextViewTextColor(@IdRes viewId: Int, @ColorInt color: Int) {
        setTextColor(viewId, color)
    }

    /**
     * Equivalent to calling [android.widget.TextView.setTextColor].
     *
     * @param viewId The id of the target view
     * @param colors The text colors to use.
     */
    @RequiresApi(31)
    @JvmStatic
    public fun RemoteViews.setTextViewTextColor(@IdRes viewId: Int, colors: ColorStateList) {
        Api31Impl.setColorStateList(this, viewId, "setTextColor", colors)
    }

    /**
     * Equivalent to calling [android.widget.TextView.setTextColor].
     *
     * @param viewId The id of the target view
     * @param notNight The text colors to use when night mode is not active.
     * @param night The text colors to use when night mode is active.
     */
    @RequiresApi(31)
    @JvmStatic
    public fun RemoteViews.setTextViewTextColor(
        @IdRes viewId: Int,
        notNight: ColorStateList,
        night: ColorStateList,
    ) {
        Api31Impl.setColorStateList(this, viewId, "setTextColor", notNight, night)
    }

    /**
     * Equivalent to calling [android.widget.TextView.setTextColor].
     *
     * @param viewId The id of the target view
     * @param notNight The text color to use when night mode is not active.
     * @param night The text color to use when night mode is active.
     */
    @RequiresApi(31)
    @JvmStatic
    public fun RemoteViews.setTextViewTextColor(
        @IdRes viewId: Int,
        @ColorInt notNight: Int,
        @ColorInt night: Int,
    ) {
        Api31Impl.setColorInt(this, viewId, "setTextColor", notNight, night)
    }

    /**
     * Equivalent to calling [android.widget.TextView.setTextColor].
     *
     * @param viewId The id of the target view
     * @param resId The resource id for the text color.
     */
    @RequiresApi(31)
    @JvmStatic
    public fun RemoteViews.setTextViewTextColorResource(@IdRes viewId: Int, @ColorRes resId: Int) {
        // Note: As both setTextColor(int) and setTextColor(ColorStateList) exist, we could cal
        // either setColor or setColorStateList. As both methods are valid if the color resource is
        // a single color, but only setColorStateList is valid if the resource is a color state
        // list, we call setColorStateList and don't provide an alternative wrapper for setColor.
        Api31Impl.setColorStateList(this, viewId, "setTextColor", resId)
    }

    /**
     * Equivalent to calling [android.widget.TextView.setTextColor].
     *
     * @param viewId The id of the target view
     * @param resId The attribute id for the text color.
     */
    @RequiresApi(31)
    @JvmStatic
    public fun RemoteViews.setTextViewTextColorAttr(@IdRes viewId: Int, @AttrRes resId: Int) {
        // Note: As both setTextColor(int) and setTextColor(ColorStateList) exist, we could call
        // either setColorAttr or setColorStateListAttr. As both methods are valid if the color
        // attribute is a single color, but only setColorStateList is valid if the attribute is a
        // color state list, we call setColorStateList and don't provide an alternative wrapper for
        // setColor.
        Api31Impl.setColorStateListAttr(this, viewId, "setTextColor", resId)
    }

    /**
     * Equivalent to calling [android.widget.TextView.setTextScaleX].
     *
     * @param viewId The id of the target view
     * @param size The horizontal scale factor.
     */
    @JvmStatic
    public fun RemoteViews.setTextViewTextScaleX(@IdRes viewId: Int, size: Float) {
        setFloat(viewId, "setTextScaleX", size)
    }

    /**
     * Equivalent to calling [android.widget.TextView.setTextSize].
     *
     * @param viewId The id of the target view
     * @param resId The id of a dimension resource for the text size.
     */
    @RequiresApi(31)
    @JvmStatic
    public fun RemoteViews.setTextViewTextSizeDimen(@IdRes viewId: Int, @DimenRes resId: Int) {
        Api31Impl.setIntDimen(this, viewId, "setTextSize", resId)
    }

    /**
     * Equivalent to calling [android.widget.TextView.setTextSize].
     *
     * @param viewId The id of the target view
     * @param resId The id of a dimension attribute for the text size.
     */
    @RequiresApi(31)
    @JvmStatic
    public fun RemoteViews.setTextViewTextSizeDimenAttr(@IdRes viewId: Int, @AttrRes resId: Int) {
        Api31Impl.setIntDimenAttr(this, viewId, "setTextSize", resId)
    }

    /**
     * Equivalent to calling [android.widget.TextView.setWidth].
     *
     * @param viewId The id of the target view
     * @param pixels The width of the TextView, in pixels.
     */
    @JvmStatic
    public fun RemoteViews.setTextViewWidth(@IdRes viewId: Int, @Px pixels: Int) {
        setInt(viewId, "setWidth", pixels)
    }

    /**
     * Equivalent to calling [android.widget.TextView.setWidth].
     *
     * @param viewId The id of the target view
     * @param value The width of the TextView.
     * @param unit The unit for [value], e.g. [android.util.TypedValue.COMPLEX_UNIT_DIP].
     */
    @RequiresApi(31)
    @JvmStatic
    public fun RemoteViews.setTextViewWidth(@IdRes viewId: Int, value: Float, unit: Int) {
        Api31Impl.setIntDimen(this, viewId, "setWidth", value, unit)
    }

    /**
     * Equivalent to calling [android.widget.TextView.setWidth].
     *
     * @param viewId The id of the target view
     * @param resId The id of a dimension resource for the width of the TextView.
     */
    @RequiresApi(31)
    @JvmStatic
    public fun RemoteViews.setTextViewWidthDimen(@IdRes viewId: Int, @DimenRes resId: Int) {
        Api31Impl.setIntDimen(this, viewId, "setWidth", resId)
    }

    /**
     * Equivalent to calling [android.widget.TextView.setWidth].
     *
     * @param viewId The id of the target view
     * @param resId The id of a dimension attribute for the width of the TextView.
     */
    @RequiresApi(31)
    @JvmStatic
    public fun RemoteViews.setTextViewWidthDimenAttr(@IdRes viewId: Int, @AttrRes resId: Int) {
        Api31Impl.setIntDimenAttr(this, viewId, "setWidth", resId)
    }

    /**
     * Equivalent to calling [android.view.View.setAlpha].
     *
     * @param viewId The id of the target view
     * @param alpha The opacity of the view.
     */
    @RequiresApi(31)
    @JvmStatic
    public fun RemoteViews.setViewAlpha(@IdRes viewId: Int, alpha: Float) {
        requireSdk(31, "setAlpha")
        setFloat(viewId, "setAlpha", alpha)
    }

    /**
     * Equivalent to calling [android.view.View.setBackgroundColor].
     *
     * @param viewId The id of the target view
     * @param color The color of the background.
     */
    @JvmStatic
    public fun RemoteViews.setViewBackgroundColor(@IdRes viewId: Int, @ColorInt color: Int) {
        setInt(viewId, "setBackgroundColor", color)
    }

    /**
     * Equivalent to calling [android.view.View.setBackgroundColor].
     *
     * @param viewId The id of the target view
     * @param notNight The color of the background when night mode is not active.
     * @param night The color of the background when night mode is active.
     */
    @RequiresApi(31)
    @JvmStatic
    public fun RemoteViews.setViewBackgroundColor(
        @IdRes viewId: Int,
        @ColorInt notNight: Int,
        @ColorInt night: Int,
    ) {
        Api31Impl.setColorInt(this, viewId, "setBackgroundColor", notNight, night)
    }

    /**
     * Equivalent to calling [android.view.View.setBackgroundColor].
     *
     * @param viewId The id of the target view
     * @param resId A color resource for the background.
     */
    @JvmStatic
    public fun RemoteViews.setViewBackgroundColorResource(
        @IdRes viewId: Int,
        @ColorRes resId: Int,
    ) {
        if (Build.VERSION.SDK_INT >= 31) {
            Api31Impl.setColor(this, viewId, "setBackgroundColor", resId)
        } else {
            // It's valid to pass @ColorRes to Context.getDrawable, it will return a ColorDrawable.
            setInt(viewId, "setBackgroundResource", resId)
        }
    }

    /**
     * Equivalent to calling [android.view.View.setBackgroundColor].
     *
     * @param viewId The id of the target view
     * @param resId A color attribute for the background.
     */
    @RequiresApi(31)
    @JvmStatic
    public fun RemoteViews.setViewBackgroundColorAttr(@IdRes viewId: Int, @AttrRes resId: Int) {
        Api31Impl.setColorAttr(this, viewId, "setBackgroundColor", resId)
    }

    /**
     * Equivalent to calling [android.view.View.setBackgroundResource].
     *
     * @param viewId The id of the target view
     * @param resId The identifier of the resource, or 0 to remove the background.
     */
    @JvmStatic
    public fun RemoteViews.setViewBackgroundResource(@IdRes viewId: Int, @DrawableRes resId: Int) {
        setInt(viewId, "setBackgroundResource", resId)
    }

    /**
     * Equivalent to calling [android.view.View.setBackgroundTintList].
     *
     * @param viewId The id of the target view
     * @param blendMode The blending mode used to apply the tint, may be null to clear.
     */
    @RequiresApi(31)
    @JvmStatic
    public fun RemoteViews.setViewBackgroundTintBlendMode(
        @IdRes viewId: Int,
        blendMode: BlendMode?,
    ) {
        Api31Impl.setBlendMode(this, viewId, "setBackgroundTintBlendMode", blendMode)
    }

    /**
     * Equivalent to calling [android.view.View.setBackgroundTintList].
     *
     * @param viewId The id of the target view
     * @param tint The tint to apply, may be null to clear tint
     */
    @RequiresApi(31)
    @JvmStatic
    public fun RemoteViews.setViewBackgroundTintList(@IdRes viewId: Int, tint: ColorStateList?) {
        Api31Impl.setColorStateList(this, viewId, "setBackgroundTintList", tint)
    }

    /**
     * Equivalent to calling [android.view.View.setBackgroundTintList].
     *
     * @param viewId The id of the target view
     * @param notNightTint The tint to apply when the UI is not in night mode.
     * @param nightTint The tint to apply when the UI is in night mode.
     */
    @RequiresApi(31)
    @JvmStatic
    public fun RemoteViews.setViewBackgroundTintList(
        @IdRes viewId: Int,
        notNightTint: ColorStateList?,
        nightTint: ColorStateList?,
    ) {
        Api31Impl.setColorStateList(this, viewId, "setBackgroundTintList", notNightTint, nightTint)
    }

    /**
     * Equivalent to calling [android.view.View.setBackgroundTintList].
     *
     * @param viewId The id of the target view
     * @param resId The resource id of the tint to apply.
     */
    @RequiresApi(31)
    @JvmStatic
    public fun RemoteViews.setViewBackgroundTintList(@IdRes viewId: Int, @ColorRes resId: Int) {
        Api31Impl.setColorStateList(this, viewId, "setBackgroundTintList", resId)
    }

    /**
     * Equivalent to calling [android.view.View.setBackgroundTintList].
     *
     * @param viewId The id of the target view
     * @param resId The attribute of the tint to apply.
     */
    @RequiresApi(31)
    @JvmStatic
    public fun RemoteViews.setViewBackgroundTintListAttr(@IdRes viewId: Int, @AttrRes resId: Int) {
        Api31Impl.setColorStateListAttr(this, viewId, "setBackgroundTintList", resId)
    }

    /**
     * Equivalent to calling [android.view.View.setClipToOutline].
     *
     * @param viewId The id of the target view
     * @param clipToOutline Whether the View's Outline should be used to clip the contents of the
     *   View.
     */
    @RequiresApi(31)
    @JvmStatic
    public fun RemoteViews.setViewClipToOutline(@IdRes viewId: Int, clipToOutline: Boolean) {
        requireSdk(31, "setClipToOutline")
        setBoolean(viewId, "setClipToOutline", clipToOutline)
    }

    /**
     * Equivalent to calling [android.view.View.setContentDescription].
     *
     * @param viewId The id of the target view
     * @param contentDescription The content description.
     */
    @JvmStatic
    public fun RemoteViews.setViewContentDescription(
        @IdRes viewId: Int,
        contentDescription: CharSequence?,
    ) {
        setCharSequence(viewId, "setContentDescription", contentDescription)
    }

    /**
     * Equivalent to calling [android.view.View.setContentDescription].
     *
     * @param viewId The id of the target view
     * @param resId The resource id for the content description.
     */
    @RequiresApi(31)
    @JvmStatic
    public fun RemoteViews.setViewContentDescription(@IdRes viewId: Int, @StringRes resId: Int) {
        Api31Impl.setCharSequence(this, viewId, "setContentDescription", resId)
    }

    /**
     * Equivalent to calling [android.view.View.setContentDescription].
     *
     * @param viewId The id of the target view
     * @param resId The attribute id for the content description.
     */
    @RequiresApi(31)
    @JvmStatic
    public fun RemoteViews.setViewContentDescriptionAttr(@IdRes viewId: Int, @AttrRes resId: Int) {
        Api31Impl.setCharSequenceAttr(this, viewId, "setContentDescription", resId)
    }

    /**
     * Equivalent to calling [android.view.View.setElevation].
     *
     * @param viewId The id of the target view
     * @param value The base elevation of this view.
     * @param unit The unit for [value], e.g. [android.util.TypedValue.COMPLEX_UNIT_DIP].
     */
    @RequiresApi(31)
    @JvmStatic
    public fun RemoteViews.setViewElevationDimen(@IdRes viewId: Int, value: Float, unit: Int) {
        Api31Impl.setFloatDimen(this, viewId, "setElevation", value, unit)
    }

    /**
     * Equivalent to calling [android.view.View.setElevation].
     *
     * @param viewId The id of the target view
     * @param resId The id of a dimension resource for the base elevation of this view.
     */
    @RequiresApi(31)
    @JvmStatic
    public fun RemoteViews.setViewElevationDimen(@IdRes viewId: Int, @DimenRes resId: Int) {
        Api31Impl.setFloatDimen(this, viewId, "setElevation", resId)
    }

    /**
     * Equivalent to calling [android.view.View.setElevation].
     *
     * @param viewId The id of the target view
     * @param resId The id of a dimension attribute for the base elevation of this view.
     */
    @RequiresApi(31)
    @JvmStatic
    public fun RemoteViews.setViewElevationDimenAttr(@IdRes viewId: Int, @AttrRes resId: Int) {
        Api31Impl.setFloatDimenAttr(this, viewId, "setElevation", resId)
    }

    /**
     * Equivalent to calling [android.view.View.setEnabled].
     *
     * Note: setEnabled can only be called on TextView and its descendants from API 24, but is safe
     * to call on other Views on older SDKs using [RemoteViews.setBoolean] directly.
     *
     * @param viewId The id of the target view
     * @param enabled True if this view is enabled, false otherwise.
     */
    @RequiresApi(24)
    @JvmStatic
    public fun RemoteViews.setViewEnabled(@IdRes viewId: Int, enabled: Boolean) {
        setBoolean(viewId, "setEnabled", enabled)
    }

    /**
     * Equivalent to calling [android.view.View.setFocusable].
     *
     * @param viewId The id of the target view
     * @param focusable If true, this view can receive the focus.
     */
    @RequiresApi(31)
    @JvmStatic
    public fun RemoteViews.setViewFocusable(@IdRes viewId: Int, focusable: Boolean) {
        requireSdk(31, "setFocusable")
        setBoolean(viewId, "setFocusable", focusable)
    }

    /**
     * Equivalent to calling [android.view.View.setFocusable].
     *
     * @param viewId The id of the target view
     * @param focusable One of [android.view.View.NOT_FOCUSABLE], [android.view.View.FOCUSABLE], or
     *   [android.view.View.FOCUSABLE_AUTO].
     */
    @RequiresApi(31)
    @JvmStatic
    public fun RemoteViews.setViewFocusable(@IdRes viewId: Int, focusable: Int) {
        requireSdk(31, "setFocusable")
        setInt(viewId, "setFocusable", focusable)
    }

    /**
     * Equivalent to calling [android.view.View.setFocusedByDefault].
     *
     * @param viewId The id of the target view
     * @param isFocusedByDefault true to set this view as the default-focus view, false otherwise.
     */
    @RequiresApi(31)
    @JvmStatic
    public fun RemoteViews.setViewFocusedByDefault(
        @IdRes viewId: Int,
        isFocusedByDefault: Boolean,
    ) {
        requireSdk(31, "setFocusedByDefault")
        setBoolean(viewId, "setFocusedByDefault", isFocusedByDefault)
    }

    /**
     * Equivalent to calling [android.view.View.setFocusableInTouchMode].
     *
     * @param viewId The id of the target view
     * @param focusableInTouchMode If true, this view can receive the focus while in touch mode.
     */
    @RequiresApi(31)
    @JvmStatic
    public fun RemoteViews.setViewFocusableInTouchMode(
        @IdRes viewId: Int,
        focusableInTouchMode: Boolean,
    ) {
        requireSdk(31, "setFocusableInTouchMode")
        setBoolean(viewId, "setFocusableInTouchMode", focusableInTouchMode)
    }

    /**
     * Equivalent to calling [android.view.View.setForegroundTintBlendMode].
     *
     * @param viewId The id of the target view
     * @param blendMode The blending mode used to apply the tint, may be null to clear.
     */
    @RequiresApi(31)
    @JvmStatic
    public fun RemoteViews.setViewForegroundTintBlendMode(
        @IdRes viewId: Int,
        blendMode: BlendMode?,
    ) {
        Api31Impl.setBlendMode(this, viewId, "setForegroundTintBlendMode", blendMode)
    }

    /**
     * Equivalent to calling [android.view.View.setForegroundTintList].
     *
     * @param viewId The id of the target view
     * @param tint The tint to apply, may be null to clear tint
     */
    @RequiresApi(31)
    @JvmStatic
    public fun RemoteViews.setViewForegroundTintList(@IdRes viewId: Int, tint: ColorStateList?) {
        Api31Impl.setColorStateList(this, viewId, "setForegroundTintList", tint)
    }

    /**
     * Equivalent to calling [android.view.View.setForegroundTintList].
     *
     * @param viewId The id of the target view
     * @param notNightTint The tint to apply when the UI is not in night mode.
     * @param nightTint The tint to apply when the UI is in night mode.
     */
    @RequiresApi(31)
    @JvmStatic
    public fun RemoteViews.setViewForegroundTintList(
        @IdRes viewId: Int,
        notNightTint: ColorStateList?,
        nightTint: ColorStateList?,
    ) {
        Api31Impl.setColorStateList(this, viewId, "setForegroundTintList", notNightTint, nightTint)
    }

    /**
     * Equivalent to calling [android.view.View.setForegroundTintList].
     *
     * @param viewId The id of the target view
     * @param resId The resource id of the tint to apply.
     */
    @RequiresApi(31)
    @JvmStatic
    public fun RemoteViews.setViewForegroundTintList(@IdRes viewId: Int, @ColorRes resId: Int) {
        Api31Impl.setColorStateList(this, viewId, "setForegroundTintList", resId)
    }

    /**
     * Equivalent to calling [android.view.View.setForegroundTintList].
     *
     * @param viewId The id of the target view
     * @param resId The attribute of the tint to apply.
     */
    @RequiresApi(31)
    @JvmStatic
    public fun RemoteViews.setViewForegroundTintListAttr(@IdRes viewId: Int, @AttrRes resId: Int) {
        Api31Impl.setColorStateListAttr(this, viewId, "setForegroundTintList", resId)
    }

    /**
     * Equivalent to calling [android.view.View.setLayoutDirection].
     *
     * @param viewId The id of the target view
     * @param layoutDirection One of [android.view.View.LAYOUT_DIRECTION_LTR],
     *   [android.view.View.LAYOUT_DIRECTION_RTL], [android.view.View.LAYOUT_DIRECTION_INHERIT], or
     *   [android.view.View.LAYOUT_DIRECTION_LOCALE].
     */
    @JvmStatic
    public fun RemoteViews.setViewLayoutDirection(@IdRes viewId: Int, layoutDirection: Int) {
        setInt(viewId, "setLayoutDirection", layoutDirection)
    }

    /**
     * Equivalent to calling [android.view.View.setMinimumHeight].
     *
     * @param viewId The id of the target view
     * @param minHeight The minimum height the view will try to be, in pixels.
     */
    @RequiresApi(24)
    @JvmStatic
    public fun RemoteViews.setViewMinimumHeight(@IdRes viewId: Int, @Px minHeight: Int) {
        requireSdk(24, "setMinimumHeight")
        setInt(viewId, "setMinimumHeight", minHeight)
    }

    /**
     * Equivalent to calling [android.widget.TextView.setMinimumHeight].
     *
     * @param viewId The id of the target view
     * @param value The minimum height the view will try to be.
     * @param unit The unit for [value], e.g. [android.util.TypedValue.COMPLEX_UNIT_DIP].
     */
    @RequiresApi(31)
    @JvmStatic
    public fun RemoteViews.setViewMinimumHeight(@IdRes viewId: Int, value: Float, unit: Int) {
        Api31Impl.setIntDimen(this, viewId, "setMinimumHeight", value, unit)
    }

    /**
     * Equivalent to calling [android.view.View.setMinimumHeight].
     *
     * @param viewId The id of the target view
     * @param resId The id of a dimension resource for the minimum height the view will try to be.
     */
    @RequiresApi(31)
    @JvmStatic
    public fun RemoteViews.setViewMinimumHeightDimen(@IdRes viewId: Int, @DimenRes resId: Int) {
        Api31Impl.setIntDimen(this, viewId, "setMinimumHeight", resId)
    }

    /**
     * Equivalent to calling [android.view.View.setMinimumHeight].
     *
     * @param viewId The id of the target view
     * @param resId The id of a dimension attribute for the minimum height the view will try to be.
     */
    @RequiresApi(31)
    @JvmStatic
    public fun RemoteViews.setViewMinimumHeightDimenAttr(@IdRes viewId: Int, @AttrRes resId: Int) {
        Api31Impl.setIntDimenAttr(this, viewId, "setMinimumHeight", resId)
    }

    /**
     * Equivalent to calling [android.widget.TextView.setMinimumWidth].
     *
     * @param viewId The id of the target view
     * @param value The minimum width the view will try to be.
     * @param unit The unit for [value], e.g. [android.util.TypedValue.COMPLEX_UNIT_DIP].
     */
    @RequiresApi(31)
    @JvmStatic
    public fun RemoteViews.setViewMinimumWidth(@IdRes viewId: Int, value: Float, unit: Int) {
        Api31Impl.setIntDimen(this, viewId, "setMinimumWidth", value, unit)
    }

    /**
     * Equivalent to calling [android.view.View.setMinimumWidth].
     *
     * @param viewId The id of the target view
     * @param resId The id of a dimension resource for the minimum width the view will try to be.
     */
    @RequiresApi(31)
    @JvmStatic
    public fun RemoteViews.setViewMinimumWidthDimen(@IdRes viewId: Int, @DimenRes resId: Int) {
        Api31Impl.setIntDimen(this, viewId, "setMinimumWidth", resId)
    }

    /**
     * Equivalent to calling [android.view.View.setMinimumWidth].
     *
     * @param viewId The id of the target view
     * @param resId The id of a dimension attribute for the minimum width the view will try to be.
     */
    @RequiresApi(31)
    @JvmStatic
    public fun RemoteViews.setViewMinimumWidthDimenAttr(@IdRes viewId: Int, @AttrRes resId: Int) {
        Api31Impl.setIntDimenAttr(this, viewId, "setMinimumWidth", resId)
    }

    /**
     * Equivalent to calling [android.view.View.setPivotX].
     *
     * @param viewId The id of the target view
     * @param pivotX The x location of the pivot point.
     */
    @RequiresApi(31)
    @JvmStatic
    public fun RemoteViews.setViewPivotX(@IdRes viewId: Int, pivotX: Float) {
        requireSdk(31, "setPivotX")
        setFloat(viewId, "setPivotX", pivotX)
    }

    /**
     * Equivalent to calling [android.view.View.setPivotY].
     *
     * @param viewId The id of the target view
     * @param pivotY The y location of the pivot point.
     */
    @RequiresApi(31)
    @JvmStatic
    public fun RemoteViews.setViewPivotY(@IdRes viewId: Int, pivotY: Float) {
        requireSdk(31, "setPivotY")
        setFloat(viewId, "setPivotY", pivotY)
    }

    /**
     * Equivalent to calling [android.view.View.setRotation].
     *
     * @param viewId The id of the target view
     * @param rotation The degrees of rotation.
     */
    @RequiresApi(31)
    @JvmStatic
    public fun RemoteViews.setViewRotation(@IdRes viewId: Int, rotation: Float) {
        requireSdk(31, "setRotation")
        setFloat(viewId, "setRotation", rotation)
    }

    /**
     * Equivalent to calling [android.view.View.setRotationX].
     *
     * @param viewId The id of the target view
     * @param rotationX The degrees of X rotation.
     */
    @RequiresApi(31)
    @JvmStatic
    public fun RemoteViews.setViewRotationX(@IdRes viewId: Int, rotationX: Float) {
        requireSdk(31, "setRotationX")
        setFloat(viewId, "setRotationX", rotationX)
    }

    /**
     * Equivalent to calling [android.view.View.setRotationY].
     *
     * @param viewId The id of the target view
     * @param rotationY The degrees of Y rotation.
     */
    @RequiresApi(31)
    @JvmStatic
    public fun RemoteViews.setViewRotationY(@IdRes viewId: Int, rotationY: Float) {
        requireSdk(31, "setRotationY")
        setFloat(viewId, "setRotationY", rotationY)
    }

    /**
     * Equivalent to calling [android.view.View.setScaleX].
     *
     * @param viewId The id of the target view
     * @param scaleX The scaling factor.
     */
    @RequiresApi(31)
    @JvmStatic
    public fun RemoteViews.setViewScaleX(@IdRes viewId: Int, scaleX: Float) {
        requireSdk(31, "setScaleX")
        setFloat(viewId, "setScaleX", scaleX)
    }

    /**
     * Equivalent to calling [android.view.View.setScaleY].
     *
     * @param viewId The id of the target view
     * @param scaleY The scaling factor.
     */
    @RequiresApi(31)
    @JvmStatic
    public fun RemoteViews.setViewScaleY(@IdRes viewId: Int, scaleY: Float) {
        requireSdk(31, "setScaleY")
        setFloat(viewId, "setScaleY", scaleY)
    }

    /**
     * Equivalent to calling [android.view.View.setScrollIndicators].
     *
     * @param viewId The id of the target view
     * @param scrollIndicators A bitmask of indicators that should be enabled, or 0 to disable all
     *   indicators.
     */
    @RequiresApi(31)
    @JvmStatic
    public fun RemoteViews.setViewScrollIndicators(@IdRes viewId: Int, scrollIndicators: Int) {
        requireSdk(31, "setScrollIndicators")
        setInt(viewId, "setScrollIndicators", scrollIndicators)
    }

    /**
     * Equivalent to calling [android.view.View.setStateDescription].
     *
     * @param viewId The id of the target view
     * @param stateDescription The state description.
     */
    @RequiresApi(30)
    @JvmStatic
    public fun RemoteViews.setViewStateDescription(
        @IdRes viewId: Int,
        stateDescription: CharSequence?,
    ) {
        requireSdk(30, "setStateDescription")
        setCharSequence(viewId, "setStateDescription", stateDescription)
    }

    /**
     * Equivalent to calling [android.view.View.setStateDescription].
     *
     * @param viewId The id of the target view
     * @param resId The resource id for the state description.
     */
    @RequiresApi(31)
    @JvmStatic
    public fun RemoteViews.setViewStateDescription(@IdRes viewId: Int, @StringRes resId: Int) {
        Api31Impl.setCharSequence(this, viewId, "setStateDescription", resId)
    }

    /**
     * Equivalent to calling [android.view.View.setStateDescription].
     *
     * @param viewId The id of the target view
     * @param resId The attribute id for the state description.
     */
    @RequiresApi(31)
    @JvmStatic
    public fun RemoteViews.setViewStateDescriptionAttr(@IdRes viewId: Int, @AttrRes resId: Int) {
        Api31Impl.setCharSequenceAttr(this, viewId, "setStateDescription", resId)
    }

    /**
     * Equivalent to calling [android.view.ViewStub.setInflatedId].
     *
     * Note that ViewStub may be used in RemoteViews layouts as of API 16.
     *
     * @param viewId The id of the target view
     * @param inflatedId A positive integer used to identify the inflated view or
     *   [android.view.View.NO_ID] if the inflated view should keep its id.
     */
    @JvmStatic
    public fun RemoteViews.setViewStubInflatedId(@IdRes viewId: Int, inflatedId: Int) {
        setInt(viewId, "setInflatedId", inflatedId)
    }

    /**
     * Equivalent to calling [android.view.ViewStub.setLayoutResource].
     *
     * Note that ViewStub may be used in RemoteViews layouts as of API 16.
     *
     * @param viewId The id of the target view
     * @param layoutResource A valid layout resource identifier (different from 0).
     */
    @JvmStatic
    public fun RemoteViews.setViewStubLayoutResource(
        @IdRes viewId: Int,
        @LayoutRes layoutResource: Int,
    ) {
        setInt(viewId, "setLayoutResource", layoutResource)
    }

    /**
     * Equivalent to calling [android.widget.TextView.setTranslationX].
     *
     * @param viewId The id of the target view
     * @param value The horizontal position of this view relative to its left position.
     * @param unit The unit for [value], e.g. [android.util.TypedValue.COMPLEX_UNIT_DIP].
     */
    @RequiresApi(31)
    @JvmStatic
    public fun RemoteViews.setViewTranslationXDimen(@IdRes viewId: Int, value: Float, unit: Int) {
        Api31Impl.setFloatDimen(this, viewId, "setTranslationX", value, unit)
    }

    /**
     * Equivalent to calling [android.view.View.setTranslationX].
     *
     * @param viewId The id of the target view
     * @param resId The id of a dimension resource for the horizontal position of this view relative
     *   to its left position.
     */
    @RequiresApi(31)
    @JvmStatic
    public fun RemoteViews.setViewTranslationXDimen(@IdRes viewId: Int, @DimenRes resId: Int) {
        Api31Impl.setFloatDimen(this, viewId, "setTranslationX", resId)
    }

    /**
     * Equivalent to calling [android.view.View.setTranslationX].
     *
     * @param viewId The id of the target view
     * @param resId The id of a dimension attribute for the horizontal position of this view
     *   relative to its left position.
     */
    @RequiresApi(31)
    @JvmStatic
    public fun RemoteViews.setViewTranslationXDimenAttr(@IdRes viewId: Int, @AttrRes resId: Int) {
        Api31Impl.setFloatDimenAttr(this, viewId, "setTranslationX", resId)
    }

    /**
     * Equivalent to calling [android.widget.TextView.setTranslationY].
     *
     * @param viewId The id of the target view
     * @param value The vertical position of this view relative to its top position.
     * @param unit The unit for [value], e.g. [android.util.TypedValue.COMPLEX_UNIT_DIP].
     */
    @RequiresApi(31)
    @JvmStatic
    public fun RemoteViews.setViewTranslationYDimen(@IdRes viewId: Int, value: Float, unit: Int) {
        Api31Impl.setFloatDimen(this, viewId, "setTranslationY", value, unit)
    }

    /**
     * Equivalent to calling [android.view.View.setTranslationY].
     *
     * @param viewId The id of the target view
     * @param resId The id of a dimension resource for the vertical position of this view relative
     *   to its top position.
     */
    @RequiresApi(31)
    @JvmStatic
    public fun RemoteViews.setViewTranslationYDimen(@IdRes viewId: Int, @DimenRes resId: Int) {
        Api31Impl.setFloatDimen(this, viewId, "setTranslationY", resId)
    }

    /**
     * Equivalent to calling [android.view.View.setTranslationY].
     *
     * @param viewId The id of the target view
     * @param resId The id of a dimension attribute for the vertical position of this view relative
     *   to its top position.
     */
    @RequiresApi(31)
    @JvmStatic
    public fun RemoteViews.setViewTranslationYDimenAttr(@IdRes viewId: Int, @AttrRes resId: Int) {
        Api31Impl.setFloatDimenAttr(this, viewId, "setTranslationY", resId)
    }

    /**
     * Equivalent to calling [android.widget.TextView.setTranslationZ].
     *
     * @param viewId The id of the target view
     * @param value The depth of this view relative to its elevation.
     * @param unit The unit for [value], e.g. [android.util.TypedValue.COMPLEX_UNIT_DIP].
     */
    @RequiresApi(31)
    @JvmStatic
    public fun RemoteViews.setViewTranslationZDimen(@IdRes viewId: Int, value: Float, unit: Int) {
        Api31Impl.setFloatDimen(this, viewId, "setTranslationZ", value, unit)
    }

    /**
     * Equivalent to calling [android.view.View.setTranslationZ].
     *
     * @param viewId The id of the target view
     * @param resId The id of a dimension resource for the depth of this view relative to its
     *   elevation.
     */
    @RequiresApi(31)
    @JvmStatic
    public fun RemoteViews.setViewTranslationZDimen(@IdRes viewId: Int, @DimenRes resId: Int) {
        Api31Impl.setFloatDimen(this, viewId, "setTranslationZ", resId)
    }

    /**
     * Equivalent to calling [android.view.View.setTranslationZ].
     *
     * @param viewId The id of the target view
     * @param resId The id of a dimension attribute for the depth of this view relative to its
     *   elevation.
     */
    @RequiresApi(31)
    @JvmStatic
    public fun RemoteViews.setViewTranslationZDimenAttr(@IdRes viewId: Int, @AttrRes resId: Int) {
        Api31Impl.setFloatDimenAttr(this, viewId, "setTranslationZ", resId)
    }

    /**
     * Requires that the sdk is at least [minSdk] and throws with a helpful error message if it's
     * not. This will only occur if the user has ignored the @RequiresApi lint. This is used to
     * catch issues where the target method is not annotated with @RemotableViewMethod on the sdk,
     * but where the RemoteViews.set* method is available. If the RemoteViews.set* method is
     * unavailable, this function is unncessary since that call will fail.
     */
    private fun requireSdk(minSdk: Int, method: String) {
        require(Build.VERSION.SDK_INT >= minSdk) {
            "$method is only available on SDK $minSdk and higher"
        }
    }

    @RequiresApi(31)
    private object Api31Impl {
        @JvmStatic
        fun setBlendMode(rv: RemoteViews, @IdRes id: Int, method: String, mode: BlendMode?) {
            rv.setBlendMode(id, method, mode)
        }

        @JvmStatic
        fun setCharSequence(
            rv: RemoteViews,
            @IdRes id: Int,
            method: String,
            @StringRes resId: Int,
        ) {
            rv.setCharSequence(id, method, resId)
        }

        @JvmStatic
        fun setCharSequenceAttr(
            rv: RemoteViews,
            @IdRes id: Int,
            method: String,
            @AttrRes resId: Int,
        ) {
            rv.setCharSequenceAttr(id, method, resId)
        }

        @JvmStatic
        fun setColor(rv: RemoteViews, @IdRes id: Int, method: String, @ColorRes resId: Int) {
            rv.setColor(id, method, resId)
        }

        @JvmStatic
        fun setColorAttr(rv: RemoteViews, @IdRes id: Int, method: String, @AttrRes resId: Int) {
            rv.setColorAttr(id, method, resId)
        }

        @JvmStatic
        fun setColorInt(
            rv: RemoteViews,
            @IdRes id: Int,
            method: String,
            @ColorInt notNight: Int,
            @ColorInt night: Int,
        ) {
            rv.setColorInt(id, method, notNight, night)
        }

        @JvmStatic
        fun setColorStateList(
            rv: RemoteViews,
            @IdRes id: Int,
            method: String,
            colorStateList: ColorStateList?,
        ) {
            rv.setColorStateList(id, method, colorStateList)
        }

        @JvmStatic
        fun setColorStateList(
            rv: RemoteViews,
            @IdRes id: Int,
            method: String,
            notNight: ColorStateList?,
            night: ColorStateList?,
        ) {
            rv.setColorStateList(id, method, notNight, night)
        }

        @JvmStatic
        fun setColorStateList(
            rv: RemoteViews,
            @IdRes id: Int,
            method: String,
            @ColorRes resId: Int,
        ) {
            rv.setColorStateList(id, method, resId)
        }

        @JvmStatic
        fun setColorStateListAttr(
            rv: RemoteViews,
            @IdRes id: Int,
            method: String,
            @AttrRes resId: Int,
        ) {
            rv.setColorStateListAttr(id, method, resId)
        }

        @JvmStatic
        fun setIcon(
            rv: RemoteViews,
            @IdRes id: Int,
            method: String,
            notNight: Icon?,
            night: Icon?,
        ) {
            rv.setIcon(id, method, notNight, night)
        }

        @JvmStatic
        fun setIntDimen(rv: RemoteViews, @IdRes id: Int, method: String, value: Float, unit: Int) {
            rv.setIntDimen(id, method, value, unit)
        }

        @JvmStatic
        fun setIntDimen(rv: RemoteViews, @IdRes id: Int, method: String, @DimenRes resId: Int) {
            rv.setIntDimen(id, method, resId)
        }

        @JvmStatic
        fun setIntDimenAttr(rv: RemoteViews, @IdRes id: Int, method: String, @AttrRes resId: Int) {
            rv.setIntDimenAttr(id, method, resId)
        }

        @JvmStatic
        fun setFloatDimen(
            rv: RemoteViews,
            @IdRes id: Int,
            method: String,
            value: Float,
            unit: Int,
        ) {
            rv.setFloatDimen(id, method, value, unit)
        }

        @JvmStatic
        fun setFloatDimen(rv: RemoteViews, @IdRes id: Int, method: String, @DimenRes resId: Int) {
            rv.setFloatDimen(id, method, resId)
        }

        @JvmStatic
        fun setFloatDimenAttr(
            rv: RemoteViews,
            @IdRes id: Int,
            method: String,
            @AttrRes resId: Int,
        ) {
            rv.setFloatDimenAttr(id, method, resId)
        }
    }
}
