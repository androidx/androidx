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
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.RemoteException
import android.support.wearable.complications.ComplicationProviderInfo
import android.support.wearable.complications.IComplicationManager
import android.support.wearable.complications.IComplicationProvider
import androidx.annotation.IntDef
import androidx.annotation.MainThread
import androidx.annotation.RequiresApi
import androidx.annotation.RestrictTo
import androidx.wear.watchface.complications.data.ComplicationData
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.data.ComplicationType.Companion.fromWireType
import androidx.wear.watchface.complications.data.GoalProgressComplicationData
import androidx.wear.watchface.complications.data.LongTextComplicationData
import androidx.wear.watchface.complications.data.MonochromaticImageComplicationData
import androidx.wear.watchface.complications.data.NoDataComplicationData
import androidx.wear.watchface.complications.data.PhotoImageComplicationData
import androidx.wear.watchface.complications.data.RangedValueComplicationData
import androidx.wear.watchface.complications.data.ShortTextComplicationData
import androidx.wear.watchface.complications.data.SmallImageComplicationData
import androidx.wear.watchface.complications.data.TimeRange
import androidx.wear.watchface.complications.data.WeightedElementsComplicationData
import androidx.wear.watchface.complications.datasource.ComplicationDataSourceService.Companion.METADATA_KEY_IMMEDIATE_UPDATE_PERIOD_MILLISECONDS
import androidx.wear.watchface.complications.datasource.ComplicationDataSourceService.ComplicationRequestListener
import java.util.concurrent.CountDownLatch

/**
 * Data associated with complication request in
 * [ComplicationDataSourceService.onComplicationRequest].
 *
 * @param complicationInstanceId The system's id for the requested complication which is a unique
 *   value for the tuple [Watch face ComponentName, complication slot ID].
 * @param complicationType The type of complication data requested.
 * @param immediateResponseRequired If `true` then [ComplicationRequestListener.onComplicationData]
 *   should be called as soon as possible (ideally less than 100ms instead of the usual 20s
 *   deadline). This will only be `true` within a
 *   [ComplicationDataSourceService.onStartImmediateComplicationRequests]
 *   [ComplicationDataSourceService.onStopImmediateComplicationRequests] pair.
 * @param isForSafeWatchFace Whether this request is on behalf of a 'safe' watch face as defined by
 *   the [ComplicationDataSourceService.METADATA_KEY_SAFE_WATCH_FACES] meta data in the data
 *   source's manifest. The data source may choose to serve different results for a 'safe' watch
 *   face. If the data source does not have the privileged permission
 *   `com.google.wear.permission.GET_IS_FOR_SAFE_WATCH_FACE`, then this must be null.
 */
public class ComplicationRequest
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
constructor(
    complicationInstanceId: Int,
    complicationType: ComplicationType,
    immediateResponseRequired: Boolean,
    @IsForSafeWatchFace isForSafeWatchFace: Int
) {
    /** Constructs a [ComplicationRequest] without setting [isForSafeWatchFace]. */
    @Suppress("NewApi")
    constructor(
        complicationInstanceId: Int,
        complicationType: ComplicationType,
        immediateResponseRequired: Boolean,
    ) : this(
        complicationInstanceId,
        complicationType,
        immediateResponseRequired,
        isForSafeWatchFace = TargetWatchFaceSafety.UNKNOWN
    )

    /**
     * The system's id for the requested complication which is a unique value for the tuple
     * [Watch face ComponentName, complication slot ID].
     */
    public val complicationInstanceId: Int = complicationInstanceId

    /** The [ComplicationType] of complication data requested. */
    public val complicationType: ComplicationType = complicationType

    /**
     * If `true` then [ComplicationRequestListener.onComplicationData] should be called as soon as
     * possible (ideally less than 100ms instead of the usual 20s deadline). This will only be
     * `true` within a [ComplicationDataSourceService.onStartImmediateComplicationRequests]
     * [ComplicationDataSourceService.onStopImmediateComplicationRequests] pair which will not be
     * called unless the [ComplicationDataSourceService] has privileged permission
     * `com.google.android.wearable.permission.USE_IMMEDIATE_COMPLICATION_UPDATE`.
     */
    @get:JvmName("isImmediateResponseRequired")
    public val immediateResponseRequired = immediateResponseRequired

    /**
     * Intended for OEM use, returns whether this request is on behalf of a 'safe' watch face as
     * defined by the [ComplicationDataSourceService.METADATA_KEY_SAFE_WATCH_FACES] meta data in the
     * data source's manifest. The data source may choose to serve different results for a 'safe'
     * watch face.
     *
     * If the [ComplicationDataSourceService.METADATA_KEY_SAFE_WATCH_FACES] meta data is not defined
     * then this will be [TargetWatchFaceSafety.UNKNOWN].
     *
     * Note if the [ComplicationDataSourceService] does not have the privileged permission
     * `com.google.wear.permission.GET_IS_FOR_SAFE_WATCH_FACE`, then this will be
     * [TargetWatchFaceSafety.UNKNOWN].
     */
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    @get:JvmName("isForSafeWatchFace")
    @IsForSafeWatchFace
    public val isForSafeWatchFace: Int = isForSafeWatchFace

    @Deprecated("Use a constructor that specifies responseNeededSoon.")
    constructor(
        complicationInstanceId: Int,
        complicationType: ComplicationType
    ) : this(complicationInstanceId, complicationType, false)
}

/**
 * Defines constants that describe whether or not the watch face the complication is being requested
 * for is deemed to be safe. I.e. if its in the list defined by the
 * [ComplicationDataSourceService.METADATA_KEY_SAFE_WATCH_FACES] meta data in the
 * [ComplicationDataSourceService]'s manifest.
 */
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
public object TargetWatchFaceSafety {
    /**
     * Prior to android T [ComplicationRequest.isForSafeWatchFace] is not supported and it will
     * always be UNKNOWN. It will also be unknown if the [ComplicationDataSourceService]'s manifest
     * doesn't define [ComplicationDataSourceService.METADATA_KEY_SAFE_WATCH_FACES], or if the
     * [ComplicationDataSourceService] does not have the privileged permission
     * `com.google.wear.permission.GET_IS_FOR_SAFE_WATCH_FACE`.
     */
    public const val UNKNOWN: Int = 0

    /**
     * The watch face is a member of the list defined by the [ComplicationDataSourceService]'s
     * [ComplicationDataSourceService.METADATA_KEY_SAFE_WATCH_FACES] meta data in its manifest.
     */
    public const val SAFE: Int = 1

    /**
     * The watch face is NOT a member of the list defined by the [ComplicationDataSourceService]'s
     * [ComplicationDataSourceService.METADATA_KEY_SAFE_WATCH_FACES] meta data in its manifest.
     */
    public const val UNSAFE: Int = 2
}

@IntDef(
    flag = true, // This is a flag to allow for future expansion.
    value =
        [TargetWatchFaceSafety.UNKNOWN, TargetWatchFaceSafety.SAFE, TargetWatchFaceSafety.UNSAFE]
)
@RestrictTo(RestrictTo.Scope.LIBRARY)
public annotation class IsForSafeWatchFace

/**
 * Class for sources of complication data.
 *
 * A complication data source service must implement [onComplicationRequest] to respond to requests
 * for updates from the complication system.
 *
 * Manifest requirements:
 * - The manifest declaration of this service must include an intent filter for
 *   android.support.wearable.complications.ACTION_COMPLICATION_UPDATE_REQUEST.
 * - A ComplicationDataSourceService must include a `meta-data` tag with
 *   android.support.wearable.complications.SUPPORTED_TYPES in its manifest entry. The value of this
 *   tag should be a comma separated list of types supported by the data source, from this table: |
 *   Androidx class | Tag name | |--------------------------------------|-------------------| |
 *   [GoalProgressComplicationData] | GOAL_PROGRESS | | [LongTextComplicationData] | LONG_TEXT | |
 *   [MonochromaticImageComplicationData] | ICON | | [PhotoImageComplicationData] | LARGE_IMAGE | |
 *   [RangedValueComplicationData] | RANGED_TEXT | | [ShortTextComplicationData] | SHORT_TEXT | |
 *   [SmallImageComplicationData] | SMALL_IMAGE | | [WeightedElementsComplicationData] |
 *   WEIGHTED_ELEMENTS |
 *
 * The order in which types are listed has no significance. In the case where a watch face supports
 * multiple types in a single complication slot, the watch face will determine which types it
 * prefers.
 *
 * For example, a complication data source that supports the RANGED_VALUE, SHORT_TEXT, and ICON
 * types would include the following in its manifest entry:
 * ```
 * <meta-data android:name="android.support.wearable.complications.SUPPORTED_TYPES"
 * android:value="RANGED_VALUE,SHORT_TEXT,ICON"/>
 * ```
 *
 * From android T onwards, it is recommended for Complication DataSourceServices to be direct boot
 * aware because the system is able to fetch complications before the lock screen has been removed.
 * To do this add android:directBootAware="true" to your service tag.
 * - A provider can choose to trust one or more watch faces by including the following in its
 *   manifest entry:
 * ```
 * <meta-data android:name="android.support.wearable.complications.SAFE_WATCH_FACES
 * android:value="com.pkg1/com.trusted.wf1,com.pkg2/com.trusted.wf2"/>
 * ```
 *
 * The listed watch faces will not need
 * 'com.google.android.wearable.permission.RECEIVE_COMPLICATION_DATA' in order to receive
 * complications from this provider. Also the provider may choose to serve different types to safe
 * watch faces by including the following in its manifest:
 * ```
 * <meta-data android:name=
 *     "androidx.wear.watchface.complications.datasource.SAFE_WATCH_FACE_SUPPORTED_TYPES"
 *      android:value="ICON"/>
 * ```
 *
 * In addition the provider can learn if a request is for a safe watchface by examining
 * [ComplicationRequest.isForSafeWatchFace]. Note SAFE_WATCH_FACE_SUPPORTED_TYPES and
 * isForSafeWatchFace are gated behind the privileged permission
 * `com.google.wear.permission.GET_IS_FOR_SAFE_WATCH_FACE`.
 * - A ComplicationDataSourceService should include a `meta-data` tag with
 *   android.support.wearable.complications.UPDATE_PERIOD_SECONDS its manifest entry. The value of
 *   this tag is the number of seconds the complication data source would like to elapse between
 *   update requests.
 *
 * Note that update requests are not guaranteed to be sent with this frequency.
 *
 * If a complication data source never needs to receive update requests beyond the one sent when a
 * complication is activated, the value of this tag should be 0.
 *
 * For example, a complication data source that would like to update every ten minutes should
 * include the following in its manifest entry:
 * ```
 * <meta-data android:name="android.support.wearable.complications.UPDATE_PERIOD_SECONDS"
 * android:value="600"/>
 * ```
 *
 * There is a lower limit for android.support.wearable.complications.UPDATE_PERIOD_SECONDS imposed
 * by the system to prevent excessive power drain. For complications with frequent updates they can
 * also register a separate [METADATA_KEY_IMMEDIATE_UPDATE_PERIOD_MILLISECONDS] meta data tag which
 * supports sampling at up to 1Hz when the watch face is visible and non-ambient, however this also
 * requires the DataSourceService to have the privileged permission
 * `com.google.android.wearable.permission.USE_IMMEDIATE_COMPLICATION_UPDATE`.
 *
 * ```
 *   <meta-data android:name=
 *      "androidx.wear.watchface.complications.data.source.IMMEDIATE_UPDATE_PERIOD_MILLISECONDS"
 *   android:value="1000"/>
 * ```
 * - A ComplicationDataSourceService can include a `meta-data` tag with
 *   android.support.wearable.complications.PROVIDER_CONFIG_ACTION its manifest entry to cause a
 *   configuration activity to be shown when the complication data source is selected.
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
 * It is possible to provide additional 'meta-data' tag
 * androidx.watchface.complications.datasource.DEFAULT_CONFIG_SUPPORTED in the service set to "true"
 * to let the system know that the data source is able to provide complication data before it is
 * configured.
 * - The manifest entry for the service should also include an android:icon attribute. The icon
 *   provided there should be a single-color white icon that represents the complication data
 *   source. This icon will be shown in the complication data source chooser interface, and may also
 *   be included in [ComplicationProviderInfo] given to watch faces for display in their
 *   configuration activities.
 * - The manifest entry should also include
 *   `android:permission="com.google.android.wearable.permission.BIND_COMPLICATION_PROVIDER"` to
 *   ensure that only the system can bind to it.
 *
 * Multiple complication data sources in the same APK are supported but in android R there's a soft
 * limit of 100 data sources per APK. Above that the companion watchface editor won't support this
 * complication data source app.
 *
 * There's no need to call setDataSource for any the ComplicationData Builders because the system
 * will append this value on your behalf.
 */
public abstract class ComplicationDataSourceService : Service() {
    private var wrapper: IComplicationProviderWrapper? = null
    internal val mainThreadHandler by lazy { createMainThreadHandler() }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    open fun createMainThreadHandler() = Handler(Looper.getMainLooper())

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
     *   from the complication slot used by the watch face itself.
     * @param type The [ComplicationType] of the activated slot.
     */
    @MainThread
    public open fun onComplicationActivated(complicationInstanceId: Int, type: ComplicationType) {}

    /**
     * Called when a complication data update is requested for the given complication id.
     *
     * In response to this request the result callback should be called with the data to be
     * displayed. If the request can not be fulfilled or no update is needed then null should be
     * passed to the callback.
     *
     * The callback doesn't have be called within onComplicationRequest but it should be called soon
     * after. If this does not occur within around 20 seconds (exact timeout length subject to
     * change), then the system will unbind from this service which may cause your eventual update
     * to not be received. However if [ComplicationRequest.immediateResponseRequired] is `true` then
     * provider should try to deliver the response in under 100 milliseconds, if `false` the
     * deadline is 20 seconds. [ComplicationRequest.immediateResponseRequired] will only ever be
     * `true` if [METADATA_KEY_IMMEDIATE_UPDATE_PERIOD_MILLISECONDS] is present in the manifest, and
     * the provider has the privileged permission
     * `com.google.android.wearable.permission.USE_IMMEDIATE_COMPLICATION_UPDATE`, and the
     * complication is visible and non-ambient.
     *
     * @param request The details about the complication that has been requested.
     * @param listener The callback to pass the result to the system.
     */
    @MainThread
    public abstract fun onComplicationRequest(
        request: ComplicationRequest,
        listener: ComplicationRequestListener
    )

    /**
     * A request for representative preview data for the complication, for use in the editor UI.
     * Preview data is assumed to be static per type. E.g. for a complication that displays the date
     * and time of an event, rather than returning the real time it should return a fixed date and
     * time such as 10:10 Aug 1st. This data may be cached by the system and the result should be
     * constant based on the current locale.
     *
     * This will be called on a background thread.
     *
     * @param type The type of complication preview data requested.
     * @return Preview data for the given complication type.
     */
    public abstract fun getPreviewData(type: ComplicationType): ComplicationData?

    /**
     * Callback for [onComplicationRequest] where only one of [onComplicationData] or
     * [onComplicationDataTimeline] should be called.
     */
    @JvmDefaultWithCompatibility
    public interface ComplicationRequestListener {
        /**
         * Sends the [ComplicationData] to the system. If null is passed then any previous
         * complication data will not be overwritten. Can be called on any thread. Should only be
         * called once. Note this is mutually exclusive with [onComplicationDataTimeline].
         */
        @Throws(RemoteException::class)
        public fun onComplicationData(complicationData: ComplicationData?)

        /**
         * Sends the [ComplicationDataTimeline] to the system. If null is passed then any previous
         * complication data will not be overwritten. Can be called on any thread. Should only be
         * called once. Note this is mutually exclusive with [onComplicationData]. Note only
         * [ComplicationDataTimeline.defaultComplicationData] is supported by older watch faces .
         */
        // TODO(alexclarke): Plumb a capability bit so the developers can know if timelines are
        // supported by the watch face.
        @Throws(RemoteException::class)
        public fun onComplicationDataTimeline(
            complicationDataTimeline: ComplicationDataTimeline?
        ) {}
    }

    /**
     * If a metadata key with [METADATA_KEY_IMMEDIATE_UPDATE_PERIOD_MILLISECONDS] is present in the
     * manifest, and the provider has privileged permission
     * `com.google.android.wearable.permission.USE_IMMEDIATE_COMPLICATION_UPDATE`, then
     * [onStartImmediateComplicationRequests] will be called when the watch face is visible and
     * non-ambient. A series of [onComplicationRequest]s will follow where
     * [ComplicationRequest.immediateResponseRequired] is `true`, ending with a call to
     * [onStopImmediateComplicationRequests].
     *
     * @param complicationInstanceId The system's ID for the complication. Note this ID is distinct
     *   from the complication slot used by the watch face itself.
     */
    @MainThread public open fun onStartImmediateComplicationRequests(complicationInstanceId: Int) {}

    /**
     * If a metadata key with [METADATA_KEY_IMMEDIATE_UPDATE_PERIOD_MILLISECONDS] is present in the
     * manifest, and the provider has privileged permission
     * `com.google.android.wearable.permission.USE_IMMEDIATE_COMPLICATION_UPDATE`, then
     * [onStartImmediateComplicationRequests] will be called when the watch face ceases to be
     * visible and non-ambient. No subsequent calls to [onComplicationRequest] where
     * [ComplicationRequest.immediateResponseRequired] is `true` will be made unless the
     * complication becomes visible and non-ambient again.
     *
     * @param complicationInstanceId The system's ID for the complication. Note this ID is distinct
     *   from the complication slot used by the watch face itself.
     */
    @MainThread public open fun onStopImmediateComplicationRequests(complicationInstanceId: Int) {}

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
     *   from the complication slot used by the watch face itself.
     */
    @MainThread public open fun onComplicationDeactivated(complicationInstanceId: Int) {}

    private inner class IComplicationProviderWrapper : IComplicationProvider.Stub() {
        @SuppressLint("SyntheticAccessor")
        override fun onUpdate(complicationInstanceId: Int, type: Int, manager: IBinder) {
            onUpdate2(complicationInstanceId, type, manager, bundle = null)
        }

        @SuppressLint("SyntheticAccessor")
        override fun onUpdate2(
            complicationInstanceId: Int,
            type: Int,
            manager: IBinder,
            bundle: Bundle?
        ) {
            val isForSafeWatchFace =
                bundle?.getInt(
                    IComplicationProvider.BUNDLE_KEY_IS_SAFE_FOR_WATCHFACE,
                    TargetWatchFaceSafety.UNKNOWN
                )
                    ?: TargetWatchFaceSafety.UNKNOWN
            val expectedDataType = fromWireType(type)
            val iComplicationManager = IComplicationManager.Stub.asInterface(manager)
            mainThreadHandler.post {
                onComplicationRequest(
                    @Suppress("NewApi")
                    ComplicationRequest(
                        complicationInstanceId,
                        expectedDataType,
                        immediateResponseRequired = false,
                        isForSafeWatchFace = isForSafeWatchFace
                    ),
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
                                dataType == ComplicationType.NO_DATA || dataType == expectedDataType
                            ) {
                                "Complication data should match the requested type. " +
                                    "Expected $expectedDataType got $dataType."
                            }
                            if (complicationData is NoDataComplicationData) {
                                complicationData.placeholder?.let {
                                    require(it.type == expectedDataType) {
                                        "Placeholder type must match the requested type. " +
                                            "Expected $expectedDataType got ${it.type}."
                                    }
                                }
                            }
                            // When no update is needed, the complicationData is going to be null.
                            iComplicationManager.updateComplicationData(
                                complicationInstanceId,
                                complicationData?.asWireComplicationData()
                            )
                        }

                        override fun onComplicationDataTimeline(
                            complicationDataTimeline: ComplicationDataTimeline?
                        ) {
                            // This can be run on an arbitrary thread, but that's OK.
                            val defaultComplicationData =
                                complicationDataTimeline?.defaultComplicationData
                            val dataType = defaultComplicationData?.type ?: ComplicationType.NO_DATA
                            require(
                                dataType != ComplicationType.NOT_CONFIGURED &&
                                    dataType != ComplicationType.EMPTY
                            ) {
                                "Cannot send data of TYPE_NOT_CONFIGURED or " +
                                    "TYPE_EMPTY. Use TYPE_NO_DATA instead."
                            }
                            require(
                                dataType == ComplicationType.NO_DATA || dataType == expectedDataType
                            ) {
                                "Complication data should match the requested type. " +
                                    "Expected $expectedDataType got $dataType."
                            }
                            if (
                                defaultComplicationData != null &&
                                    defaultComplicationData is NoDataComplicationData
                            ) {
                                defaultComplicationData.placeholder?.let {
                                    require(it.type == expectedDataType) {
                                        "Placeholder type must match the requested type. " +
                                            "Expected $expectedDataType got ${it.type}."
                                    }
                                }
                            }
                            complicationDataTimeline?.timelineEntries?.let { timelineEntries ->
                                for (timelineEntry in timelineEntries) {
                                    val timelineComplicationData = timelineEntry.complicationData
                                    if (timelineComplicationData is NoDataComplicationData) {
                                        timelineComplicationData.placeholder?.let {
                                            require(it.type == expectedDataType) {
                                                "Timeline entry Placeholder types must match the " +
                                                    "requested type. Expected $expectedDataType " +
                                                    "got ${timelineComplicationData.type}."
                                            }
                                        }
                                    } else {
                                        require(timelineComplicationData.type == expectedDataType) {
                                            "Timeline entry types must match the requested type. " +
                                                "Expected $expectedDataType got " +
                                                "${timelineComplicationData.type}."
                                        }
                                    }
                                }
                            }
                            // When no update is needed, the complicationData is going to be null.
                            iComplicationManager.updateComplicationData(
                                complicationInstanceId,
                                complicationDataTimeline?.asWireComplicationData()
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
        ): android.support.wearable.complications.ComplicationData? {
            val expectedDataType = fromWireType(type)
            val complicationData = getPreviewData(expectedDataType)
            val dataType = complicationData?.type ?: ComplicationType.NO_DATA
            require(dataType == ComplicationType.NO_DATA || dataType == expectedDataType) {
                "Preview data should match the requested type. " +
                    "Expected $expectedDataType got $dataType."
            }

            if (complicationData != null) {
                require(complicationData.validTimeRange == TimeRange.ALWAYS) {
                    "Preview data should have time range set to ALWAYS."
                }
                require(!complicationData.asWireComplicationData().hasExpression()) {
                    "Preview data must not have expressions."
                }
            }
            return complicationData?.asWireComplicationData()
        }

        override fun onStartSynchronousComplicationRequests(complicationInstanceId: Int) {
            mainThreadHandler.post {
                this@ComplicationDataSourceService.onStartImmediateComplicationRequests(
                    complicationInstanceId
                )
            }
        }

        override fun onStopSynchronousComplicationRequests(complicationInstanceId: Int) {
            mainThreadHandler.post {
                this@ComplicationDataSourceService.onStopImmediateComplicationRequests(
                    complicationInstanceId
                )
            }
        }

        override fun onSynchronousComplicationRequest(complicationInstanceId: Int, type: Int) =
            onSynchronousComplicationRequest2(complicationInstanceId, type, bundle = null)

        override fun onSynchronousComplicationRequest2(
            complicationInstanceId: Int,
            type: Int,
            bundle: Bundle?
        ): android.support.wearable.complications.ComplicationData? {
            val isForSafeWatchFace =
                bundle?.getInt(
                    IComplicationProvider.BUNDLE_KEY_IS_SAFE_FOR_WATCHFACE,
                    TargetWatchFaceSafety.UNKNOWN
                )
                    ?: TargetWatchFaceSafety.UNKNOWN
            val expectedDataType = fromWireType(type)
            val complicationType = fromWireType(type)
            val latch = CountDownLatch(1)
            var wireComplicationData: android.support.wearable.complications.ComplicationData? =
                null
            mainThreadHandler.post {
                this@ComplicationDataSourceService.onComplicationRequest(
                    @Suppress("NewApi")
                    ComplicationRequest(
                        complicationInstanceId,
                        complicationType,
                        immediateResponseRequired = true,
                        isForSafeWatchFace = isForSafeWatchFace
                    ),
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
                                dataType == ComplicationType.NO_DATA || dataType == expectedDataType
                            ) {
                                "Complication data should match the requested type. " +
                                    "Expected $expectedDataType got $dataType."
                            }

                            // When no update is needed, the complicationData is going to be null.
                            wireComplicationData = complicationData?.asWireComplicationData()
                            latch.countDown()
                        }

                        override fun onComplicationDataTimeline(
                            complicationDataTimeline: ComplicationDataTimeline?
                        ) {
                            // This can be run on an arbitrary thread, but that's OK.
                            val dataType =
                                complicationDataTimeline?.defaultComplicationData?.type
                                    ?: ComplicationType.NO_DATA
                            require(
                                dataType != ComplicationType.NOT_CONFIGURED &&
                                    dataType != ComplicationType.EMPTY
                            ) {
                                "Cannot send data of TYPE_NOT_CONFIGURED or " +
                                    "TYPE_EMPTY. Use TYPE_NO_DATA instead."
                            }
                            require(
                                dataType == ComplicationType.NO_DATA || dataType == expectedDataType
                            ) {
                                "Complication data should match the requested type. " +
                                    "Expected $expectedDataType got $dataType."
                            }

                            // When no update is needed, the complicationData is going to be null.
                            wireComplicationData =
                                complicationDataTimeline?.asWireComplicationData()
                            latch.countDown()
                        }
                    }
                )
            }
            latch.await()
            return wireComplicationData
        }
    }

    public companion object {
        /**
         * The intent action used to send update requests to the data source.
         * [ComplicationDataSourceService] must declare an intent filter for this action in the
         * manifest.
         */
        // TODO(b/192233205): Migrate value to androidx.
        @SuppressWarnings("ActionValue")
        public const val ACTION_COMPLICATION_UPDATE_REQUEST: String =
            "android.support.wearable.complications.ACTION_COMPLICATION_UPDATE_REQUEST"

        /**
         * Metadata key used to declare supported complication types.
         *
         * A [ComplicationDataSourceService] must include a `meta-data` tag with this name in its
         * manifest entry. The value of this tag should be a comma separated list of types supported
         * by the complication data source. Types should be given as named as per the type fields in
         * the [ComplicationData], but omitting the "TYPE_" prefix, e.g. `SHORT_TEXT`, `LONG_TEXT`,
         * `RANGED_VALUE`.
         *
         * The order in which types are listed has no significance. In the case where a watch face
         * supports multiple types in a single complication slot, the watch face will determine
         * which types it prefers.
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
         * Metadata key used to declare supported complication types for safe watch faces.
         *
         * Gated behind the privileged permission
         * `com.google.wear.permission.GET_IS_FOR_SAFE_WATCH_FACE', this overrides the
         * [METADATA_KEY_SUPPORTED_TYPES] list for 'safe' watch faces. I.e. watch faces in the
         * [METADATA_KEY_SAFE_WATCH_FACES] metadata list.
         *
         * This means for example trusted watch faces could receive [ComplicationType.SHORT_TEXT]
         * and untrusted ones [ComplicationType.MONOCHROMATIC_IMAGE].
         */
        public const val METADATA_KEY_SAFE_WATCH_FACE_SUPPORTED_TYPES: String =
            "androidx.wear.watchface.complications.datasource.SAFE_WATCH_FACE_SUPPORTED_TYPES"

        /**
         * Metadata key used to declare the requested frequency of update requests.
         *
         * A [ComplicationDataSourceService] should include a `meta-data` tag with this name in its
         * manifest entry. The value of this tag is the number of seconds the complication data
         * source would like to elapse between update requests.
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
         * Metadata key used to request elevated frequency of [onComplicationRequest]s when the
         * watch face is visible and non-ambient.
         *
         * To do this [ComplicationDataSourceService] should include a `meta-data` tag with this
         * name in its manifest entry. The value of this tag is the number of milliseconds the
         * complication data source would like to elapse between [onComplicationRequest]s requests
         * when the watch face is visible and non-ambient.
         *
         * Note in addition to this meta-data, the data source must also request the privileged
         * permission com.google.android.wearable.permission.USE_IMMEDIATE_COMPLICATION_UPDATE.
         *
         * Note that update requests are not guaranteed to be sent with this frequency and a lower
         * limit exists (initially 1 second).
         */
        public const val METADATA_KEY_IMMEDIATE_UPDATE_PERIOD_MILLISECONDS: String =
            "androidx.wear.watchface.complications.data.source." +
                "IMMEDIATE_UPDATE_PERIOD_MILLISECONDS"

        /**
         * Metadata key used to declare a list of watch faces that may receive data from a
         * complication data source before they are granted the RECEIVE_COMPLICATION_DATA
         * permission. This allows the listed watch faces to set the complication data source as a
         * default and have the complication populate when the watch face is first seen.
         *
         * Only trusted watch faces that will set this complication data source as a default should
         * be included in this list.
         *
         * Note that if a watch face is in the same app package as the complication data source, it
         * does not need to be added to this list.
         *
         * The value of this tag should be a comma separated list of watch faces or packages. An
         * entry can be a flattened component, as if [ComponentName.flattenToString] had been
         * called, to declare a specific watch face as safe. An entry can also be a package name, as
         * if [ComponentName.getPackageName] had been called, in which case any watch face under the
         * app with that package name will be considered safe for this complication data source.
         *
         * From Android T, if this provider has the privileged permission
         * `com.google.wear.permission.GET_IS_FOR_SAFE_WATCH_FACE`, then
         * [ComplicationRequest.isForSafeWatchFace] will be populated.
         */
        // TODO(b/192233205): Migrate value to androidx.
        public const val METADATA_KEY_SAFE_WATCH_FACES: String =
            "android.support.wearable.complications.SAFE_WATCH_FACES"

        /**
         * Metadata key used to declare that the complication data source should be hidden from the
         * complication data source chooser interface. If set to "true", users will not be able to
         * select this complication data source. The complication data source may still be specified
         * as a default complication data source by watch faces.
         */
        // TODO(b/192233205): Migrate value to androidx.
        internal const val METADATA_KEY_HIDDEN: String =
            "android.support.wearable.complications.HIDDEN"

        /**
         * Metadata key used to declare an action for a configuration activity for a complication
         * data source.
         *
         * A [ComplicationDataSourceService] can include a `meta-data` tag with this name in its
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
         * Metadata key. Setting to "true" indicates to the system that this complication data
         * source with a PROVIDER_CONFIG_ACTION metadata tag is able to provide complication data
         * before it is configured. See [METADATA_KEY_DATA_SOURCE_CONFIG_ACTION].
         */
        public const val METADATA_KEY_DATA_SOURCE_DEFAULT_CONFIG_SUPPORTED: String =
            "androidx.watchface.complications.datasource.DEFAULT_CONFIG_SUPPORTED"

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
