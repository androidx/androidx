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
package androidx.wear.watchface.complications.datasource

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Service
import android.content.ComponentName
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.RemoteException
import android.support.wearable.complications.ComplicationProviderInfo
import android.support.wearable.complications.IComplicationManager
import android.support.wearable.complications.IComplicationProvider
import androidx.annotation.RestrictTo
import androidx.annotation.UiThread
import androidx.wear.watchface.complications.data.ComplicationData
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.data.ComplicationType.Companion.fromWireType
import androidx.wear.watchface.complications.data.TimeRange

/**
 * Data associated with complication request in [ComplicationDataSourceService.onComplicationRequest].
 * @param complicationInstanceId The system's id for the requested complication which is a unique
 * value for the tuple [Watch face ComponentName, complication slot ID].
 * @param complicationType The type of complication data requested.
 */
public class ComplicationRequest(
    public val complicationInstanceId: Int,
    public val complicationType: ComplicationType
)

/**
 * Class for sources of complication data.
 *
 * A complication data source service must implement [onComplicationRequest] to respond to requests
 * for updates from the complication system.
 *
 * Manifest requirements:
 *
 * - The manifest declaration of this service must include an
 * intent filter for android.support.wearable.complications.ACTION_COMPLICATION_UPDATE_REQUEST.
 *
 * - A ComplicationDataSourceService must include a `meta-data` tag with
 * android.support.wearable.complications.SUPPORTED_TYPES in its manifest entry. The value of this
 * tag should be a comma separated list of types supported by the data source. Types should be given
 * as named as per the type fields in the [ComplicationData], but omitting the "TYPE_" prefix, e.g.
 * `SHORT_TEXT`, `LONG_TEXT`, `RANGED_VALUE`.
 *
 * The order in which types are listed has no significance. In the case where a watch face
 * supports multiple types in a single complication slot, the watch face will determine which types
 * it prefers.
 *
 * For example, a complication data source that supports the RANGED_VALUE, SHORT_TEXT, and ICON
 * types would include the following in its manifest entry:
 *
 * ```
 * <meta-data android:name="android.support.wearable.complications.SUPPORTED_TYPES"
 * android:value="RANGED_VALUE,SHORT_TEXT,ICON"/>
 * ```
 *
 *
 * - A ComplicationDataSourceService should include a `meta-data` tag with
 * android.support.wearable.complications.UPDATE_PERIOD_SECONDS its manifest entry. The value of
 * this tag is the number of seconds the complication data source would like to elapse between
 * update requests.
 *
 * Note that update requests are not guaranteed to be sent with this frequency.
 *
 * If a complication data source never needs to receive update requests beyond the one sent when a
 * complication is activated, the value of this tag should be 0.
 *
 * For example, a complication data source that would like to update every ten minutes should
 * include the following in its manifest entry:
 *
 * ```
 * <meta-data android:name="android.support.wearable.complications.UPDATE_PERIOD_SECONDS"
 * android:value="600"/>
 * ```
 *
 *
 * - A ComplicationDataSourceService can include a `meta-data` tag with
 * android.support.wearable.complications.PROVIDER_CONFIG_ACTION its manifest entry to cause a
 * configuration activity to be shown when the complication data source is selected.
 *
 * The configuration activity must reside in the same package as the complication data source, and
 * must register an intent filter for the action specified here, including
 * android.support.wearable.complications.category.PROVIDER_CONFIG as well as
 * [Intent.CATEGORY_DEFAULT] as categories.
 *
 * The complication id being configured will be included in the intent that starts the config
 * activity using the extra key android.support.wearable.complications.EXTRA_CONFIG_COMPLICATION_ID.
 *
 * The complication type that will be requested from the complication data source will also be
 * included, using the extra key android.support.wearable.complications
 * .EXTRA_CONFIG_COMPLICATION_TYPE.
 *
 * The complication data source's [ComponentName] will also be included in the intent that starts
 * the config activity, using the extra key
 * android.support.wearable.complications.EXTRA_CONFIG_PROVIDER_COMPONENT.
 *
 * The config activity must call [Activity.setResult] with either [Activity.RESULT_OK] or
 * [Activity.RESULT_CANCELED] before it is finished, to tell the system whether or not the
 * complication data source should be set on the given complication.
 *
 * - The manifest entry for the service should also include an android:icon attribute. The icon
 * provided there should be a single-color white icon that represents the complication data source.
 * This icon will be shown in the complication data source chooser interface, and may also be
 * included in [ComplicationProviderInfo] given to watch faces for display in their configuration
 * activities.
 *
 * - The manifest entry should also include
 * `android:permission="com.google.android.wearable.permission.BIND_COMPLICATION_PROVIDER"` to
 * ensure that only the system can bind to it.
 *
 * Multiple complication data sources in the same APK are supported but in android R there's a
 * soft limit of 100 data sources per APK. Above that the companion watchface editor won't
 * support this complication data source app.
 */
public abstract class ComplicationDataSourceService : Service() {
    private var wrapper: IComplicationProviderWrapper? = null
    private val mainThreadHandler = Handler(Looper.getMainLooper())

    @SuppressLint("SyntheticAccessor")
    final override fun onBind(intent: Intent): IBinder? {
        if (ACTION_COMPLICATION_UPDATE_REQUEST == intent.action) {
            if (wrapper == null) {
                wrapper = IComplicationProviderWrapper()
            }
            return wrapper
        }
        return null
    }

    /**
     * Called when a complication is activated.
     *
     * This occurs when the watch face calls setActiveComplications, or when this data source is
     * chosen for a complication which is already active.
     *
     * This will usually be followed by a call to [onComplicationRequest].
     *
     * This will be called on the main thread.
     *
     * @param complicationInstanceId The system's ID for the complication. Note this ID is distinct
     * from the complication slot used by the watch face itself.
     * @param type The [ComplicationType] of the activated slot.
     */
    @UiThread
    public open fun onComplicationActivated(complicationInstanceId: Int, type: ComplicationType) {}

    /**
     * Called when a complication data update is requested for the given complication id.
     *
     * In response to this request the result callback should be called with the data to be
     * displayed. If the request can not be fulfilled or no update is needed then null should be
     * passed to the callback.
     *
     * The callback doesn't have be called within onComplicationRequest but it should be called
     * soon after. If this does not occur within around 20 seconds (exact timeout length subject to
     * change), then the system will unbind from this service which may cause your eventual update
     * to not be received.
     *
     * @param request The details about the complication that has been requested.
     * @param listener The callback to pass the result to the system.
     */
    @UiThread
    public abstract fun onComplicationRequest(
        request: ComplicationRequest,
        listener: ComplicationRequestListener
    )

    /**
     * A request for representative preview data for the complication, for use in the editor UI.
     * Preview data is assumed to be static per type. E.g. for a complication that displays the date
     * and time of an event, rather than returning the real time it should return a fixed date and
     * time such as 10:10 Aug 1st.
     *
     * This will be called on a background thread.
     *
     * @param type The type of complication preview data requested.
     * @return Preview data for the given complication type.
     */
    public abstract fun getPreviewData(type: ComplicationType): ComplicationData?

    /** Callback for [onComplicationRequest]. */
    public interface ComplicationRequestListener {
        /**
         * Sends the complicationData to the system. If null is passed then any previous
         * complication data will not be overwritten. Can be called on any thread. Should only be
         * called once.
         */
        @Throws(RemoteException::class)
        public fun onComplicationData(complicationData: ComplicationData?)
    }

    /**
     * Called when a complication is deactivated.
     *
     * This occurs when the current watch face changes, or when the watch face calls
     * setActiveComplications and does not include the given complication (usually because the watch
     * face has stopped displaying it).
     *
     * This will be called on the main thread.
     *
     * @param complicationInstanceId The system's ID for the complication. Note this ID is distinct
     * from the complication slot used by the watch face itself.
     */
    @UiThread public open fun onComplicationDeactivated(complicationInstanceId: Int) {}

    private inner class IComplicationProviderWrapper : IComplicationProvider.Stub() {
        @SuppressLint("SyntheticAccessor")
        override fun onUpdate(complicationInstanceId: Int, type: Int, manager: IBinder) {
            val complicationType = fromWireType(type)
            val iComplicationManager = IComplicationManager.Stub.asInterface(manager)
            mainThreadHandler.post {
                onComplicationRequest(
                    ComplicationRequest(complicationInstanceId, complicationType),
                    object : ComplicationRequestListener {
                        override fun onComplicationData(complicationData: ComplicationData?) {
                            // This can be run on an arbitrary thread, but that's OK.
                            val dataType = complicationData?.type ?: ComplicationType.NO_DATA
                            require(
                                dataType != ComplicationType.NOT_CONFIGURED &&
                                    dataType != ComplicationType.EMPTY
                            ) {
                                "Cannot send data of TYPE_NOT_CONFIGURED or " +
                                    "TYPE_EMPTY. Use TYPE_NO_DATA instead."
                            }

                            require(
                                dataType == ComplicationType.NO_DATA ||
                                    dataType == complicationType
                            ) {
                                "Preview data should match the requested type. " +
                                    "Expected $complicationType got $dataType."
                            }

                            if (complicationData != null) {
                                require(complicationData.validTimeRange == TimeRange.ALWAYS) {
                                    "Preview data should have time range set to ALWAYS."
                                }
                            }

                            // When no update is needed, the complicationData is going to be
                            // null.
                            iComplicationManager.updateComplicationData(
                                complicationInstanceId,
                                complicationData?.asWireComplicationData()
                            )
                        }
                    }
                )
            }
        }

        @SuppressLint("SyntheticAccessor")
        override fun onComplicationDeactivated(complicationInstanceId: Int) {
            mainThreadHandler.post {
                this@ComplicationDataSourceService.onComplicationDeactivated(complicationInstanceId)
            }
        }

        @SuppressLint("SyntheticAccessor")
        override fun onComplicationActivated(
            complicationInstanceId: Int,
            type: Int,
            manager: IBinder
        ) {
            mainThreadHandler.post {
                this@ComplicationDataSourceService.onComplicationActivated(
                    complicationInstanceId,
                    fromWireType(type)
                )
            }
        }

        override fun getApiVersion(): Int {
            return API_VERSION
        }

        @SuppressLint("SyntheticAccessor")
        override fun getComplicationPreviewData(
            type: Int
        ): android.support.wearable.complications.ComplicationData? =
            getPreviewData(fromWireType(type))?.asWireComplicationData()
    }

    public companion object {
        /**
         * The intent action used to send update requests to the data source. Complication data
         * source services must declare an intent filter for this action in the manifest.
         */
        // TODO(b/192233205): Migrate value to androidx.
        @SuppressWarnings("ActionValue")
        public const val ACTION_COMPLICATION_UPDATE_REQUEST: String =
            "android.support.wearable.complications.ACTION_COMPLICATION_UPDATE_REQUEST"

        /**
         * Metadata key used to declare supported complication types.
         *
         * A ComplicationDataSourceService must include a `meta-data` tag with this name in its
         * manifest entry. The value of this tag should be a comma separated list of types supported
         * by the complication data source. Types should be given as named as per the type fields in
         * the [ComplicationData], but omitting the "TYPE_" prefix, e.g. `SHORT_TEXT`, `LONG_TEXT`,
         * `RANGED_VALUE`.
         *
         * The order in which types are listed has no significance. In the case where a watch
         * face supports multiple types in a single complication slot, the watch face will
         * determine which types it prefers.
         *
         * For example, a complication data source that supports the RANGED_VALUE, SHORT_TEXT, and
         * ICON type would include the following in its manifest entry:
         * ```
         * <meta-data android:name="android.support.wearable.complications.SUPPORTED_TYPES"
         * android:value="RANGED_VALUE,SHORT_TEXT,ICON"/>
         * ```
         */
        // TODO(b/192233205): Migrate value to androidx.
        public const val METADATA_KEY_SUPPORTED_TYPES: String =
            "android.support.wearable.complications.SUPPORTED_TYPES"

        /**
         * Metadata key used to declare the requested frequency of update requests.
         *
         * A ComplicationDataSourceService should include a `meta-data` tag with this name in its
         * manifest entry. The value of this tag is the number of seconds the complication data
         * source would like to
         * elapse between update requests.
         *
         * Note that update requests are not guaranteed to be sent with this frequency.
         *
         * If a complication data source never needs to receive update requests beyond the one sent
         * when a complication is activated, the value of this tag should be 0.
         *
         * For example, a complication data source that would like to update every ten minutes
         * should include the following in its manifest entry:
         * ```
         * <meta-data android:name="android.support.wearable.complications.UPDATE_PERIOD_SECONDS"
         * android:value="600"/>
         * ```
         */
        // TODO(b/192233205): Migrate value to androidx.
        public const val METADATA_KEY_UPDATE_PERIOD_SECONDS: String =
            "android.support.wearable.complications.UPDATE_PERIOD_SECONDS"

        /**
         * Metadata key used to declare a list of watch faces that may receive data from a
         * complication data source before they are granted the RECEIVE_COMPLICATION_DATA
         * permission. This allows the listed watch faces to set the complication data source as
         * a default and have the complication populate when the watch face is first seen.
         *
         * Only trusted watch faces that will set this complication data source as a default should
         * be included in this list.
         *
         * Note that if a watch face is in the same app package as the complication data source, it
         * does not need o be added to this list.
         *
         * The value of this tag should be a comma separated list of watch faces or packages. An
         * entry can be a flattened component, as if [ComponentName.flattenToString] had been
         * called, to declare a specific watch face as safe. An entry can also be a package name,
         * as if [ComponentName.getPackageName] had been called, in which case any watch face
         * under the app with that package name will be considered safe for this complication data
         * source.
         */
        // TODO(b/192233205): Migrate value to androidx.
        public const val METADATA_KEY_SAFE_WATCH_FACES: String =
            "android.support.wearable.complications.SAFE_WATCH_FACES"

        /**
         * Metadata key used to declare that the complication data source should be hidden from the
         * complication data source chooser interface. If set to "true", users will not be able
         * to select this complication data source. The complication data source may still be
         * specified as a default complication data source by watch faces.
         *
         * @hide
         */
        // TODO(b/192233205): Migrate value to androidx.
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        public const val METADATA_KEY_HIDDEN: String =
            "android.support.wearable.complications.HIDDEN"

        /**
         * Metadata key used to declare an action for a configuration activity for a complication
         * data source.
         *
         * A ComplicationDataSourceService can include a `meta-data` tag with this name in its
         * manifest entry to cause a configuration activity to be shown when the complication data
         * source is selected.
         *
         * The configuration activity must reside in the same package as the complication data
         * source, and must register an intent filter for the action specified here, including
         * [CATEGORY_DATA_SOURCE_CONFIG] as well as [Intent.CATEGORY_DEFAULT] as categories.
         *
         * The complication id being configured will be included in the intent that starts the
         * config activity using the extra key [EXTRA_CONFIG_COMPLICATION_ID].
         *
         * The complication type that will be requested from the complication data source will also
         * be included, using the extra key [EXTRA_CONFIG_COMPLICATION_TYPE].
         *
         * The complication data source's [ComponentName] will also be included in the intent that
         * starts the config activity, using the extra key [EXTRA_CONFIG_DATA_SOURCE_COMPONENT].
         *
         * The config activity must call [Activity.setResult] with either [Activity.RESULT_OK] or
         * [Activity.RESULT_CANCELED] before it is finished, to tell the system whether or not the
         * complication data source should be set on the given complication.
         */
        // TODO(b/192233205): Migrate value to androidx.
        @SuppressLint("IntentName")
        public const val METADATA_KEY_DATA_SOURCE_CONFIG_ACTION: String =
            "android.support.wearable.complications.PROVIDER_CONFIG_ACTION"

        /**
         * Category for complication data source config activities. The configuration activity for a
         * complication complication data source must specify this category in its intent filter.
         *
         * @see METADATA_KEY_DATA_SOURCE_CONFIG_ACTION
         */
        // TODO(b/192233205): Migrate value to androidx.
        @SuppressLint("IntentName")
        public const val CATEGORY_DATA_SOURCE_CONFIG: String =
            "android.support.wearable.complications.category.PROVIDER_CONFIG"

        /**
         * Extra used to supply the complication id to a complication data source configuration
         * activity.
         */
        // TODO(b/192233205): Migrate value to androidx.
        @SuppressLint("ActionValue")
        public const val EXTRA_CONFIG_COMPLICATION_ID: String =
            "android.support.wearable.complications.EXTRA_CONFIG_COMPLICATION_ID"

        /**
         * Extra used to supply the complication type to a complication data source configuration
         * activity.
         */
        // TODO(b/192233205): Migrate value to androidx.
        @SuppressLint("ActionValue")
        public const val EXTRA_CONFIG_COMPLICATION_TYPE: String =
            "android.support.wearable.complications.EXTRA_CONFIG_COMPLICATION_TYPE"

        /**
         * Extra used to supply the complication data source component to a complication data source
         * configuration activity.
         */
        // TODO(b/192233205): Migrate value to androidx.
        @SuppressLint("ActionValue")
        public const val EXTRA_CONFIG_DATA_SOURCE_COMPONENT: String =
            "android.support.wearable.complications.EXTRA_CONFIG_PROVIDER_COMPONENT"
    }
}
