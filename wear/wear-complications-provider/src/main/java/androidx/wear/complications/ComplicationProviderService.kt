/*
 * Copyright 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *            http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package androidx.wear.complications

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
import androidx.wear.complications.data.ComplicationData
import androidx.wear.complications.data.ComplicationType
import androidx.wear.complications.data.ComplicationType.Companion.fromWireType

/**
 * Data associated with complication request in [ComplicationProviderService.onComplicationRequest].
 * @param complicationInstanceId The system's id for the requested complication which is a unique
 * value for the tuple [Watch face ComponentName, complication slot ID].
 * @param complicationType The type of complication data requested.
 */
public class ComplicationRequest(
    public val complicationInstanceId: Int,
    public val complicationType: ComplicationType
)

/**
 * Class for providers of complication data.
 *
 * A provider service must implement [onComplicationRequest] to respond to requests for updates
 * from the complication system.
 *
 * Manifest requirements:
 *
 * - The manifest declaration of this service must include an
 * intent filter for android.support.wearable.complications.ACTION_COMPLICATION_UPDATE_REQUEST.
 *
 * - A ComplicationProviderService must include a `meta-data` tag with
 * android.support.wearable.complications.SUPPORTED_TYPES in its manifest entry. The value of this
 * tag should be a comma separated list of types supported by the provider. Types should be given as
 * named as per the type fields in the [ComplicationData], but omitting the "TYPE_" prefix, e.g.
 * `SHORT_TEXT`, `LONG_TEXT`, `RANGED_VALUE`.
 *
 * The order in which types are listed has no significance. In the case where a watch face
 * supports multiple types in a single complication slot, the watch face will determine which types
 * it prefers.
 *
 * For example, a provider that supports the RANGED_VALUE, SHORT_TEXT, and ICON types would
 * include the following in its manifest entry:
 *
 * ```
 * <meta-data android:name="android.support.wearable.complications.SUPPORTED_TYPES"
 * android:value="RANGED_VALUE,SHORT_TEXT,ICON"/>
 * ```
 *
 *
 * - A ComplicationProviderService should include a `meta-data` tag with
 * android.support.wearable.complications.UPDATE_PERIOD_SECONDS its manifest entry. The value of
 * this tag is the number of seconds the provider would like to elapse between update requests.
 *
 * Note that update requests are not guaranteed to be sent with this frequency.
 *
 * If a provider never needs to receive update requests beyond the one sent when a complication
 * is activated, the value of this tag should be 0.
 *
 * For example, a provider that would like to update every ten minutes should include the
 * following in its manifest entry:
 *
 * ```
 * <meta-data android:name="android.support.wearable.complications.UPDATE_PERIOD_SECONDS"
 * android:value="600"/>
 * ```
 *
 *
 * - A ComplicationProviderService can include a `meta-data` tag with
 * android.support.wearable.complications.PROVIDER_CONFIG_ACTION its manifest entry to cause a
 * configuration activity to be shown when the provider is selected.
 *
 * The configuration activity must reside in the same package as the provider, and must register
 * an intent filter for the action specified here, including
 * android.support.wearable.complications.category.PROVIDER_CONFIG as well as
 * [Intent.CATEGORY_DEFAULT] as categories.
 *
 * The complication id being configured will be included in the intent that starts the config
 * activity using the extra key android.support.wearable.complications.EXTRA_CONFIG_COMPLICATION_ID.
 *
 * The complication type that will be requested from the provider will also be included, using
 * the extra key android.support.wearable.complications.EXTRA_CONFIG_COMPLICATION_TYPE.
 *
 * The provider's [ComponentName] will also be included in the intent that starts the config
 * activity, using the extra key
 * android.support.wearable.complications.EXTRA_CONFIG_PROVIDER_COMPONENT.
 *
 * The config activity must call [Activity.setResult] with either [Activity.RESULT_OK] or
 * [Activity.RESULT_CANCELED] before it is finished, to tell the system whether or not the provider
 * should be set on the given complication.
 *
 * - The manifest entry for the service should also include an android:icon attribute. The icon
 * provided there should be a single-color white icon that represents the provider. This icon will
 * be shown in the provider chooser interface, and may also be included in
 * [ComplicationProviderInfo] given to watch faces for display in their configuration activities.
 *
 *
 * - The manifest entry should also include
 * `android:permission="com.google.android.wearable.permission.BIND_COMPLICATION_PROVIDER"` to
 * ensure that only the system can bind to it.
 *
 * Multiple providers in the same APK are supported but in android R there's a soft limit of 100
 * providers per APK. Above that the companion watchface editor won't support this provider app.
 */
public abstract class ComplicationProviderService : Service() {
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
     * This occurs when the watch face calls setActiveComplications, or when this provider is
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
                this@ComplicationProviderService.onComplicationDeactivated(complicationInstanceId)
            }
        }

        @SuppressLint("SyntheticAccessor")
        override fun onComplicationActivated(
            complicationInstanceId: Int,
            type: Int,
            manager: IBinder
        ) {
            mainThreadHandler.post {
                this@ComplicationProviderService.onComplicationActivated(
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
         * The intent action used to send update requests to the provider. Complication provider
         * services must declare an intent filter for this action in the manifest.
         */
        @SuppressWarnings("ActionValue")
        public const val ACTION_COMPLICATION_UPDATE_REQUEST: String =
            "android.support.wearable.complications.ACTION_COMPLICATION_UPDATE_REQUEST"

        /**
         * Metadata key used to declare supported complication types.
         *
         * A ComplicationProviderService must include a `meta-data` tag with this name in its
         * manifest entry. The value of this tag should be a comma separated list of types supported
         * by the provider. Types should be given as named as per the type fields in the
         * [ComplicationData], but omitting the "TYPE_" prefix, e.g. `SHORT_TEXT`, `LONG_TEXT`,
         * `RANGED_VALUE`.
         *
         * The order in which types are listed has no significance. In the case where a watch
         * face supports multiple types in a single complication slot, the watch face will
         * determine which types it prefers.
         *
         * For example, a provider that supports the RANGED_VALUE, SHORT_TEXT, and ICON types
         * would include the following in its manifest entry:
         * ```
         * <meta-data android:name="android.support.wearable.complications.SUPPORTED_TYPES"
         * android:value="RANGED_VALUE,SHORT_TEXT,ICON"/>
         * ```
         */
        public const val METADATA_KEY_SUPPORTED_TYPES: String =
            "android.support.wearable.complications.SUPPORTED_TYPES"

        /**
         * Metadata key used to declare the requested frequency of update requests.
         *
         * A ComplicationProviderService should include a `meta-data` tag with this name in its
         * manifest entry. The value of this tag is the number of seconds the provider would like to
         * elapse between update requests.
         *
         * Note that update requests are not guaranteed to be sent with this frequency.
         *
         * If a provider never needs to receive update requests beyond the one sent when a
         * complication is activated, the value of this tag should be 0.
         *
         * For example, a provider that would like to update every ten minutes should include the
         * following in its manifest entry:
         * ```
         * <meta-data android:name="android.support.wearable.complications.UPDATE_PERIOD_SECONDS"
         * android:value="600"/>
         * ```
         */
        public const val METADATA_KEY_UPDATE_PERIOD_SECONDS: String =
            "android.support.wearable.complications.UPDATE_PERIOD_SECONDS"

        /**
         * Metadata key used to declare a list of watch faces that may receive data from a provider
         * before they are granted the RECEIVE_COMPLICATION_DATA permission. This allows the listed
         * watch faces to set the provider as a default and have the complication populate when the
         * watch face is first seen.
         *
         * Only trusted watch faces that will set this provider as a default should be included
         * in this list.
         *
         * Note that if a watch face is in the same app package as the provider, it does not need
         * to be added to this list.
         *
         * The value of this tag should be a comma separated list of watch faces or packages. An
         * entry can be a flattened component, as if [ComponentName.flattenToString] had been
         * called, to declare a specific watch face as safe. An entry can also be a package name,
         * as if [ComponentName.getPackageName] had been called, in which case any watch face
         * under the app with that package name will be considered safe for this provider.
         */
        public const val METADATA_KEY_SAFE_WATCH_FACES: String =
            "android.support.wearable.complications.SAFE_WATCH_FACES"

        /**
         * Metadata key used to declare that the provider should be hidden from the provider chooser
         * interface. If set to "true", users will not be able to select this provider. The provider
         * may still be specified as a default provider by watch faces.
         *
         * @hide
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        public const val METADATA_KEY_HIDDEN: String =
            "android.support.wearable.complications.HIDDEN"

        /**
         * Metadata key used to declare an action for a configuration activity for a provider.
         *
         * A ComplicationProviderService can include a `meta-data` tag with this name in its
         * manifest entry to cause a configuration activity to be shown when the provider is
         * selected.
         *
         * The configuration activity must reside in the same package as the provider, and must
         * register an intent filter for the action specified here, including
         * [CATEGORY_PROVIDER_CONFIG] as well as [Intent.CATEGORY_DEFAULT] as categories.
         *
         * The complication id being configured will be included in the intent that starts the
         * config activity using the extra key [EXTRA_CONFIG_COMPLICATION_ID].
         *
         * The complication type that will be requested from the provider will also be included,
         * using the extra key [EXTRA_CONFIG_COMPLICATION_TYPE].
         *
         * The provider's [ComponentName] will also be included in the intent that starts the
         * config activity, using the extra key [EXTRA_CONFIG_PROVIDER_COMPONENT].
         *
         * The config activity must call [Activity.setResult] with either [Activity.RESULT_OK] or
         * [Activity.RESULT_CANCELED] before it is finished, to tell the system whether or not the
         * provider should be set on the given complication.
         */
        @SuppressLint("IntentName")
        public const val METADATA_KEY_PROVIDER_CONFIG_ACTION: String =
            "android.support.wearable.complications.PROVIDER_CONFIG_ACTION"

        /**
         * Category for provider config activities. The configuration activity for a complication
         * provider must specify this category in its intent filter.
         *
         * @see METADATA_KEY_PROVIDER_CONFIG_ACTION
         */
        @SuppressLint("IntentName")
        public const val CATEGORY_PROVIDER_CONFIG: String =
            "android.support.wearable.complications.category.PROVIDER_CONFIG"

        /** Extra used to supply the complication id to a provider configuration activity. */
        @SuppressLint("ActionValue")
        public const val EXTRA_CONFIG_COMPLICATION_ID: String =
            "android.support.wearable.complications.EXTRA_CONFIG_COMPLICATION_ID"

        /** Extra used to supply the complication type to a provider configuration activity. */
        @SuppressLint("ActionValue")
        public const val EXTRA_CONFIG_COMPLICATION_TYPE: String =
            "android.support.wearable.complications.EXTRA_CONFIG_COMPLICATION_TYPE"

        /** Extra used to supply the provider component to a provider configuration activity. */
        @SuppressLint("ActionValue")
        public const val EXTRA_CONFIG_PROVIDER_COMPONENT: String =
            "android.support.wearable.complications.EXTRA_CONFIG_PROVIDER_COMPONENT"
    }
}
