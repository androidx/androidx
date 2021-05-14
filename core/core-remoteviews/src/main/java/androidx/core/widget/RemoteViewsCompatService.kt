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

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Parcel
import android.os.Parcelable
import android.util.Base64
import android.util.Log
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import androidx.annotation.RestrictTo
import androidx.core.content.pm.PackageInfoCompat
import androidx.core.widget.RemoteViewsCompat.RemoteCollectionItems

/**
 * [RemoteViewsService] to provide [RemoteViews] set using [RemoteViewsCompat.setRemoteAdapter].
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class RemoteViewsCompatService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        val appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1)
        check(appWidgetId != -1) { "No app widget id was present in the intent" }

        val viewId = intent.getIntExtra(EXTRA_VIEW_ID, -1)
        check(viewId != -1) { "No view id was present in the intent" }

        return RemoteViewsCompatServiceViewFactory(this, appWidgetId, viewId)
    }

    private class RemoteViewsCompatServiceViewFactory(
        private val mContext: Context,
        private val mAppWidgetId: Int,
        private val mViewId: Int
    ) : RemoteViewsFactory {
        private var mItems = EMPTY

        override fun onCreate() = loadData()

        override fun onDataSetChanged() = loadData()

        private fun loadData() {
            mItems = RemoteViewsCompatServiceData.load(mContext, mAppWidgetId, mViewId) ?: EMPTY
        }

        override fun onDestroy() {}

        override fun getCount() = mItems.itemCount

        override fun getViewAt(position: Int) = mItems.getItemView(position)

        override fun getLoadingView() = null

        override fun getViewTypeCount() = mItems.viewTypeCount

        override fun getItemId(position: Int): Long {
            return mItems.getItemId(position)
        }

        override fun hasStableIds() = mItems.hasStableIds()

        companion object {
            private val EMPTY = RemoteCollectionItems(
                ids = longArrayOf(),
                views = emptyArray(),
                hasStableIds = false,
                viewTypeCount = 1
            )
        }
    }

    /**
     * Wrapper around a serialized [RemoteCollectionItems] with metadata about the versions
     * of Android and the app when the items were created.
     *
     * Our method of serialization and deserialization is to marshall and unmarshall the items to
     * a byte array using their [Parcelable] implementation. As Parcelable definitions can change
     * over time, it is not safe to do this across different versions of a package or Android
     * itself. However, as app widgets are recreated on reboot or when a package is updated, this
     * is not a problem for the approach used here.
     *
     * This data wrapper stores the current build of Android and the provider app at the time of
     * serialization and deserialization is only attempted in [load] if both are the same as at
     * the time of serialization.
     */
    private class RemoteViewsCompatServiceData {
        private val mItemsBytes: ByteArray
        private val mBuildVersion: String
        private val mAppVersion: Long

        private constructor(
            itemsBytes: ByteArray,
            buildVersion: String,
            appVersion: Long
        ) {
            mItemsBytes = itemsBytes
            mBuildVersion = buildVersion
            mAppVersion = appVersion
        }

        constructor(parcel: Parcel) {
            val length = parcel.readInt()
            mItemsBytes = ByteArray(length)
            parcel.readByteArray(mItemsBytes)
            mBuildVersion = parcel.readString()!!
            mAppVersion = parcel.readLong()
        }

        fun writeToParcel(dest: Parcel) {
            dest.writeInt(mItemsBytes.size)
            dest.writeByteArray(mItemsBytes)
            dest.writeString(mBuildVersion)
            dest.writeLong(mAppVersion)
        }

        fun save(context: Context, appWidgetId: Int, viewId: Int) {
            getPrefs(context)
                .edit()
                .putString(
                    getKey(appWidgetId, viewId),
                    serializeToHexString { parcel, _ -> writeToParcel(parcel) }
                )
                .apply()
        }

        companion object {
            private const val PREFS_FILENAME = "androidx.core.widget.prefs.RemoteViewsCompat"

            internal fun getKey(appWidgetId: Int, viewId: Int): String {
                return "$appWidgetId:$viewId"
            }

            internal fun getPrefs(context: Context): SharedPreferences {
                return context.getSharedPreferences(PREFS_FILENAME, MODE_PRIVATE)
            }

            fun create(
                context: Context,
                items: RemoteCollectionItems
            ): RemoteViewsCompatServiceData {
                val versionCode = getVersionCode(context)
                check(versionCode != null) { "Couldn't obtain version code for app" }
                return RemoteViewsCompatServiceData(
                    itemsBytes = serializeToBytes(items::writeToParcel),
                    buildVersion = Build.VERSION.INCREMENTAL,
                    appVersion = versionCode
                )
            }

            /**
             * Returns the stored [RemoteCollectionItems] for the widget/view id, or null if
             * it couldn't be retrieved for any reason.
             */
            internal fun load(
                context: Context,
                appWidgetId: Int,
                viewId: Int
            ): RemoteCollectionItems? {
                val prefs = getPrefs(context)
                val hexString = prefs.getString(getKey(appWidgetId, viewId), /* defValue= */ null)
                if (hexString == null) {
                    Log.w(TAG, "No collection items were stored for widget $appWidgetId")
                    return null
                }
                val data = deserializeFromHexString(hexString) { RemoteViewsCompatServiceData(it) }
                if (Build.VERSION.INCREMENTAL != data.mBuildVersion) {
                    Log.w(
                        TAG,
                        "Android version code has changed, not using stored collection items for " +
                            "widget $appWidgetId"
                    )
                    return null
                }
                val versionCode = getVersionCode(context)
                if (versionCode == null) {
                    Log.w(
                        TAG,
                        "Couldn't get version code, not using stored collection items for widget " +
                            appWidgetId
                    )
                    return null
                }
                if (versionCode != data.mAppVersion) {
                    Log.w(
                        TAG,
                        "App version code has changed, not using stored collection items for " +
                            "widget $appWidgetId"
                    )
                    return null
                }
                return try {
                    deserializeFromBytes(data.mItemsBytes) { RemoteCollectionItems(it) }
                } catch (t: Throwable) {
                    Log.e(
                        TAG,
                        "Unable to deserialize stored collection items for widget $appWidgetId",
                        t
                    )
                    null
                }
            }

            internal fun getVersionCode(context: Context): Long? {
                val packageManager = context.packageManager
                val packageInfo = try {
                    packageManager.getPackageInfo(context.packageName, 0)
                } catch (e: PackageManager.NameNotFoundException) {
                    Log.e(TAG, "Couldn't retrieve version code for " + context.packageManager, e)
                    return null
                }
                return PackageInfoCompat.getLongVersionCode(packageInfo)
            }

            internal fun serializeToHexString(parcelable: (Parcel, Int) -> Unit): String {
                return Base64.encodeToString(serializeToBytes(parcelable), Base64.DEFAULT)
            }

            internal fun serializeToBytes(parcelable: (Parcel, Int) -> Unit): ByteArray {
                val parcel = Parcel.obtain()
                return try {
                    parcel.setDataPosition(0)
                    parcelable(parcel, 0)
                    parcel.marshall()
                } finally {
                    parcel.recycle()
                }
            }

            internal fun <P> deserializeFromHexString(
                hexString: String,
                creator: (Parcel) -> P,
            ): P {
                return deserializeFromBytes(Base64.decode(hexString, Base64.DEFAULT), creator)
            }

            internal fun <P> deserializeFromBytes(
                bytes: ByteArray,
                creator: (Parcel) -> P,
            ): P {
                val parcel = Parcel.obtain()
                return try {
                    parcel.unmarshall(bytes, 0, bytes.size)
                    parcel.setDataPosition(0)
                    creator(parcel)
                } finally {
                    parcel.recycle()
                }
            }
        }
    }

    internal companion object {
        private const val TAG = "RemoteViewsCompatServic"
        private const val EXTRA_VIEW_ID = "androidx.core.widget.extra.view_id"

        /**
         * Returns an intent use with [RemoteViews.setRemoteAdapter]. These intents
         * are uniquely identified by the [appWidgetId] and [viewId].
         */
        fun createIntent(
            context: Context,
            appWidgetId: Int,
            viewId: Int
        ): Intent {
            return Intent(context, RemoteViewsCompatService::class.java)
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                .putExtra(EXTRA_VIEW_ID, viewId)
                .also { intent ->
                    // Set a data Uri to disambiguate Intents for different widget/view ids.
                    intent.data = Uri.parse(intent.toUri(Intent.URI_INTENT_SCHEME))
                }
        }

        /**
         * Stores [items] to the disk to be used by a [RemoteViewsCompatService] for the same
         * [appWidgetId] and [viewId].
         */
        fun saveItems(
            context: Context,
            appWidgetId: Int,
            viewId: Int,
            items: RemoteCollectionItems
        ) {
            RemoteViewsCompatServiceData.create(context, items).save(context, appWidgetId, viewId)
        }
    }
}