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

package androidx.wear.watchface.editor

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.support.wearable.watchface.Constants
import android.support.wearable.watchface.SharedMemoryImage
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContract
import androidx.annotation.Px
import androidx.annotation.RequiresApi
import androidx.annotation.RestrictTo
import androidx.annotation.UiThread
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.wear.watchface.ComplicationHelperActivity
import androidx.wear.watchface.ComplicationSlotBoundsType
import androidx.wear.watchface.DrawMode
import androidx.wear.watchface.RenderParameters
import androidx.wear.watchface.WatchFace
import androidx.wear.watchface.client.ComplicationSlotState
import androidx.wear.watchface.client.EditorListener
import androidx.wear.watchface.client.EditorServiceClient
import androidx.wear.watchface.client.EditorState
import androidx.wear.watchface.client.HeadlessWatchFaceClient
import androidx.wear.watchface.client.InteractiveWatchFaceClient
import androidx.wear.watchface.client.WatchFaceId
import androidx.wear.watchface.complications.ComplicationDataSourceInfo
import androidx.wear.watchface.complications.ComplicationDataSourceInfoRetriever
import androidx.wear.watchface.complications.data.ComplicationData
import androidx.wear.watchface.complications.data.ComplicationExperimental
import androidx.wear.watchface.complications.data.EmptyComplicationData
import androidx.wear.watchface.complications.toApiComplicationDataSourceInfo
import androidx.wear.watchface.control.data.HeadlessWatchFaceInstanceParams
import androidx.wear.watchface.data.IdAndComplicationDataWireFormat
import androidx.wear.watchface.editor.EditorSession.Companion.createHeadlessEditorSession
import androidx.wear.watchface.editor.data.EditorStateWireFormat
import androidx.wear.watchface.sanitizeWatchFaceId
import androidx.wear.watchface.style.UserStyle
import androidx.wear.watchface.style.UserStyleData
import androidx.wear.watchface.style.UserStyleSchema
import androidx.wear.watchface.utility.AsyncTraceEvent
import androidx.wear.watchface.utility.TraceEvent
import androidx.wear.watchface.utility.launchWithTracing
import java.lang.reflect.Proxy
import java.time.Duration
import java.time.Instant
import kotlin.coroutines.resume
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.android.asCoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

private const val TAG = "EditorSession"

@JvmDefaultWithCompatibility
/**
 * Interface for manipulating watch face state during a watch face editing session. The editor
 * should adjust [userStyle] and call [openComplicationDataSourceChooser] to configure the watch
 * face and call [close] when done. This reports the updated [EditorState] to the [EditorListener]s
 * registered via [EditorServiceClient.addListener].
 *
 * For EditorSessions backed by a headless instance (see [createHeadlessEditorSession] and
 * [EditorRequest.headlessDeviceConfig]), style changes are not applied to the interactive instance
 * and it's up to the system to apply them. For EditorSessions backed by an interactive instance
 * style changes are applied immediately. Its possible the system might fail to persist the style
 * changes (e.g. to data base write failure or a crash) and if this happens it's the responsibility
 * of the system to revert the style change.
 */
public interface EditorSession : AutoCloseable {
    /** The [ComponentName] of the watch face being edited. */
    public val watchFaceComponentName: ComponentName

    /**
     * Unique ID for the instance of the watch face being edited. Note each distinct [ComponentName]
     * can sometimes have multiple instances. See [WatchFaceId] for more details.
     */
    public val watchFaceId: WatchFaceId

    /**
     * The current [UserStyle]. Assigning to this will cause the style to update. However, styling
     * changes to the watch face will be reverted upon exit.
     *
     * If accessed from java, consider using [androidx.lifecycle.FlowLiveDataConversions.asLiveData]
     * to observe changes.
     */
    public val userStyle: MutableStateFlow<UserStyle>

    /** The reference preview [Instant] for this watch face to render previews with. */
    public val previewReferenceInstant: Instant

    /** The watch face's [UserStyleSchema]. */
    public val userStyleSchema: UserStyleSchema

    /**
     * A [Flow] of a Map of complication slot ids to [ComplicationSlotState] for each complication
     * slot.
     *
     * If accessed from java, consider using [androidx.lifecycle.FlowLiveDataConversions.asLiveData]
     * to observe changes.
     */
    public val complicationSlotsState: StateFlow<Map<Int, ComplicationSlotState>>

    /**
     * Whether any changes should be committed when the session is closed (defaults to `true`).
     *
     * Note due to SysUI requirements [EditorState] can't reliably be sent in the activity result
     * because there are circumstances where [ComponentActivity.onStop] doesn't get called but the
     * UX requires us to commit changes.
     *
     * Regardless of the value, on completion of the editor session, the original UserStyle is
     * restored. Note we need SysUI's help to revert any complication data source changes. Caveat
     * some complication data sources have their own config (e.g. the world clock has a timezone
     * setting) and that config currently can't be reverted.
     */
    @get:UiThread
    @Suppress("INAPPLICABLE_JVM_NAME")
    @get:JvmName("isCommitChangesOnClose")
    @set:UiThread
    public var commitChangesOnClose: Boolean

    /**
     * Returns a flow of maps of [androidx.wear.watchface.ComplicationSlot] ids to preview
     * [ComplicationData] suitable for use in rendering a preview of the watch face. This data is
     * fetched asynchronously and the map will initially be empty. For watch faces without
     * complications this will always be empty.
     *
     * Note new data may be pushed as a result of running [openComplicationDataSourceChooser].
     *
     * Note if a slot is configured to be empty then the map will contain an instance of
     * [EmptyComplicationData] for that slot. Disabled complicationSlots are included in the map.
     *
     * If accessed from java, consider using [androidx.lifecycle.FlowLiveDataConversions.asLiveData]
     * to observe changes.
     */
    public val complicationsPreviewData: StateFlow<Map<Int, ComplicationData>>

    /**
     * Returns a flow of maps of [androidx.wear.watchface.ComplicationSlot] ids to
     * [ComplicationDataSourceInfo] that represent the information available about the data source
     * for each complication. This data is fetched asynchronously and the map will initially be
     * empty. For watch faces without complications this will always be empty.
     *
     * Note new data may be pushed as result of running [openComplicationDataSourceChooser].
     *
     * Note a `null` [ComplicationDataSourceInfo] will be associated with a complication slot id if
     * the [androidx.wear.watchface.ComplicationSlot] is configured to show the empty complication
     * data source.
     */
    public val complicationsDataSourceInfo: StateFlow<Map<Int, ComplicationDataSourceInfo?>>

    /** The ID of the background complication or `null` if there isn't one. */
    @get:SuppressWarnings("AutoBoxing") public val backgroundComplicationSlotId: Int?

    /**
     * Returns the ID of the complication at the given coordinates or `null` if there isn't one.
     * Only [androidx.wear.watchface.ComplicationSlot]s with [ComplicationSlotBoundsType.ROUND_RECT]
     * are supported by this function.
     */
    @SuppressWarnings("AutoBoxing")
    @UiThread
    public fun getComplicationSlotIdAt(@Px x: Int, @Px y: Int): Int?

    /**
     * For the duration of the editor session, applies an override to complications rendered via
     * [renderWatchFaceToBitmap]. If you need to render multiple times with the same
     * [slotIdToComplicationData] it's more efficient to use this API and call
     * [renderWatchFaceToBitmap] with null slotIdToComplicationData. When the editor session ends
     * this override will be removed.
     *
     * Note if after this call updated complications are sent via
     * [InteractiveWatchFaceClient.updateComplicationData], they will only be applied once the
     * editor session has ended.
     *
     * @param slotIdToComplicationData The complications you wish to set. Any slots not covered by
     * this map will be unchanged.
     */
    public fun setOverrideComplications(slotIdToComplicationData: Map<Int, ComplicationData>) {
        // We expect this to be overridden.
        throw UnsupportedOperationException()
    }

    /**
     * Renders the watch face to a [Bitmap] using the current [userStyle].
     *
     * @param renderParameters The [RenderParameters] to render with. Must be [DrawMode.INTERACTIVE]
     * @param instant The [Instant] to render with
     * @param slotIdToComplicationData Override [ComplicationData] for each
     *   [androidx.wear.watchface.ComplicationSlot] to render with. Note using this feature is
     *   somewhat computationally expensive because under the hood it saves and restores the backing
     *   watch face instance's complications. If you need to render multiple times with the same
     *   slotIdToComplicationData, consider using [renderWatchFaceToBitmap] for a more efficient
     *   alternative.
     * @return A [Bitmap] containing the screen shot with the specified parameters
     */
    @UiThread
    public fun renderWatchFaceToBitmap(
        renderParameters: RenderParameters,
        instant: Instant,
        slotIdToComplicationData: Map<Int, ComplicationData>?
    ): Bitmap

    /**
     * Opens the complication data source chooser and returns the chosen complication data source
     * for the specified [androidx.wear.watchface.ComplicationSlot].
     *
     * The result returns `null` if the operation was cancelled and otherwise returned an instance
     * of [ChosenComplicationDataSource] that contains information about the chosen data source.
     *
     * @param complicationSlotId The id of the [androidx.wear.watchface.ComplicationSlot] to select
     *   a complication data source for.
     * @throws IllegalStateException if a previous invocation of openComplicationDataSourceChooser
     *   is still running when openComplicationDataSourceChooser is called.
     */
    @UiThread
    public suspend fun openComplicationDataSourceChooser(
        complicationSlotId: Int
    ): ChosenComplicationDataSource?

    public companion object {
        /**
         * If passed [renderWatchFaceToBitmap] this will signal that the watch face's default
         * preview time should be used.
         */
        @JvmField val DEFAULT_PREVIEW_INSTANT: Instant = Instant.ofEpochMilli(-1L)

        /**
         * Constructs an [EditorSession] for an on watch face editor. This registers an activity
         * result handler and so it must be called during an Activity or Fragment initialization
         * path. The EditorSession is lifecycle aware and will automatically close when onDestroy is
         * received.
         *
         * @param activity The [ComponentActivity] associated with the [EditorSession].
         * @return Deferred<EditorSession?> which is resolved with either the [EditorSession] or
         *   `null` if it can't be constructed.
         * @throws [TimeoutCancellationException] if it takes longer than [EDITING_SESSION_TIMEOUT]
         *   to create a watch face editor.
         */
        @JvmStatic
        @UiThread
        @Throws(TimeoutCancellationException::class)
        public suspend fun createOnWatchEditorSession(activity: ComponentActivity): EditorSession {
            var editorSession: EditorSession? = null
            // Wait until Lifecycle.Event.ON_CREATE.
            suspendCancellableCoroutine<Unit> { continuation ->
                activity.lifecycle.addObserver(
                    object : DefaultLifecycleObserver {
                        override fun onCreate(owner: LifecycleOwner) {
                            continuation.resume(Unit)
                        }

                        override fun onDestroy(owner: LifecycleOwner) {
                            editorSession?.close()
                            editorSession = null
                        }
                    }
                )
            }
            editorSession =
                createOnWatchEditorSessionImpl(
                    activity,
                    activity.intent,
                    object : ComplicationDataSourceInfoRetrieverProvider {
                        override fun getComplicationDataSourceInfoRetriever() =
                            ComplicationDataSourceInfoRetriever(activity)
                    }
                )
            return editorSession!!
        }

        // Used by tests.
        @Suppress("DEPRECATION")
        @Throws(TimeoutCancellationException::class)
        internal suspend fun createOnWatchEditorSessionImpl(
            activity: ComponentActivity,
            editIntent: Intent,
            complicationDataSourceInfoRetrieverProvider: ComplicationDataSourceInfoRetrieverProvider
        ): EditorSession =
            TraceEvent("EditorSession.createOnWatchEditorSessionAsyncImpl").use {
                val editorRequest =
                    editIntent
                        .getParcelableExtra<ComponentName>(Constants.EXTRA_WATCH_FACE_COMPONENT)
                        ?.let { EditorRequest(it, "", null) }
                        ?: EditorRequest.createFromIntent(editIntent)
                Log.d(
                    TAG,
                    "createOnWatchEditorSession ${editorRequest.watchFaceComponentName} " +
                        "${editorRequest.watchFaceId}"
                )
                // We need to respect the lifecycle and register the ActivityResultListener now.
                val session =
                    OnWatchFaceEditorSessionImpl(
                        activity,
                        editorRequest.watchFaceComponentName,
                        editorRequest.watchFaceId,
                        editorRequest.initialUserStyle,
                        complicationDataSourceInfoRetrieverProvider,
                        activity.lifecycleScope,
                        editorRequest.previewScreenshotParams
                    )
                // But full initialization has to be deferred because
                // [WatchFace.getOrCreateEditorDelegate] is async.
                // Resolve only after init has been completed.
                withContext(activity.lifecycleScope.coroutineContext) {
                    withTimeout(EDITING_SESSION_TIMEOUT.toMillis()) {
                        session.setEditorDelegate(
                            // Either create a delegate for a new headless client or await an
                            // interactive one.
                            if (editorRequest.headlessDeviceConfig != null) {
                                WatchFace.createHeadlessSessionDelegate(
                                    editorRequest.watchFaceComponentName,
                                    HeadlessWatchFaceInstanceParams(
                                        editorRequest.watchFaceComponentName,
                                        editorRequest.headlessDeviceConfig.asWireDeviceConfig(),
                                        activity.resources.displayMetrics.widthPixels,
                                        activity.resources.displayMetrics.heightPixels,
                                        editorRequest.watchFaceId.id
                                    ),
                                    activity
                                )
                            } else {
                                WatchFace.getOrCreateEditorDelegate(
                                        editorRequest.watchFaceComponentName
                                    )
                                    .await()
                            }
                        )
                        // Resolve only after init has been completed.
                        session
                    }
                }
            }

        /**
         * Constructs an [EditorSession] for a remote watch face editor.
         *
         * Caution the remote watchface must have been built with the same version of the watch face
         * libraries.
         *
         * @param activity The [ComponentActivity] associated with the EditorSession.
         * @param editIntent The [Intent] sent by SysUI to launch the editing session.
         * @param headlessWatchFaceClient The [HeadlessWatchFaceClient] to use for rendering etc...
         * @return The [EditorSession] or `null` if it could not be constructed.
         */
        @JvmStatic
        @RequiresApi(27)
        @UiThread
        public fun createHeadlessEditorSession(
            activity: ComponentActivity,
            editIntent: Intent,
            headlessWatchFaceClient: HeadlessWatchFaceClient
        ): EditorSession =
            TraceEvent("EditorSession.createHeadlessEditorSession").use {
                EditorRequest.createFromIntent(editIntent).let {
                    Log.d(
                        TAG,
                        "createHeadlessEditorSession ${it.watchFaceComponentName} ${it.watchFaceId}"
                    )
                    HeadlessEditorSession(
                        activity,
                        headlessWatchFaceClient,
                        it.watchFaceComponentName,
                        it.watchFaceId,
                        it.initialUserStyle!!,
                        object : ComplicationDataSourceInfoRetrieverProvider {
                            override fun getComplicationDataSourceInfoRetriever() =
                                ComplicationDataSourceInfoRetriever(activity)
                        },
                        CoroutineScope(
                            Handler(Looper.getMainLooper()).asCoroutineDispatcher().immediate
                        ),
                        it.previewScreenshotParams
                    )
                }
            }

        /** Timeout allowed for waiting for creating the watch face editing session. */
        @JvmField public val EDITING_SESSION_TIMEOUT: Duration = Duration.ofSeconds(4)
    }
}

/**
 * The complication data source that was chosen by the user for a given
 * [androidx.wear.watchface.ComplicationSlot] id as a result to a call to
 * [EditorSession.openComplicationDataSourceChooser].
 *
 * @param complicationSlotId The ID of the complication slot that was configured.
 * @param complicationDataSourceInfo The complication data source that was chosen for this slot, or
 *   `null` if the empty complication source was was chosen.
 * @param extras Any additional extras returned by the complication data source chooser.
 */
public class ChosenComplicationDataSource(
    public val complicationSlotId: Int,
    public val complicationDataSourceInfo: ComplicationDataSourceInfo?,
    public val extras: Bundle,
) {
    override fun toString(): String =
        "$complicationSlotId,$complicationDataSourceInfo,${extras.asString()}"
}

// Helps inject mock ComplicationDataSourceInfoRetrievers for testing.
internal interface ComplicationDataSourceInfoRetrieverProvider {
    fun getComplicationDataSourceInfoRetriever(): ComplicationDataSourceInfoRetriever
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public abstract class BaseEditorSession
internal constructor(
    private var activity: ComponentActivity?,
    private var complicationDataSourceInfoRetrieverProvider:
        ComplicationDataSourceInfoRetrieverProvider?,
    public val coroutineScope: CoroutineScope,
    private val previewScreenshotParams: PreviewScreenshotParams?,
    internal val watchFaceIdInternal: WatchFaceId
) : EditorSession {
    protected var closed: Boolean = false
    protected var forceClosed: Boolean = false
    protected open var editorObscuresWatchFace = false

    private val editorSessionTraceEvent = AsyncTraceEvent("EditorSession")
    private val closeCallback =
        object : EditorService.CloseCallback() {
            override fun onClose() {
                // onClose could be called on any thread but forceClose needs to be called from the
                // UI
                // thread.
                coroutineScope.launch { forceClose() }
            }
        }

    override val watchFaceId = WatchFaceId(sanitizeWatchFaceId(watchFaceIdInternal.id))

    private companion object {
        /** Timeout for fetching ComplicationsPreviewData in [BaseEditorSession.close]. */
        private const val CLOSE_BROADCAST_TIMEOUT_MILLIS = 500L
    }

    init {
        Log.d(TAG, "Session started")
        EditorService.globalEditorService.addCloseCallback(closeCallback)
    }

    override var commitChangesOnClose: Boolean = true

    /**
     * This is completed when [fetchComplicationsData] has called [getPreviewData] for each
     * complication and each of those have been completed.
     */
    internal val deferredComplicationPreviewDataAvailable = CompletableDeferred<Unit>()

    override val complicationsPreviewData = MutableStateFlow<Map<Int, ComplicationData>>(emptyMap())

    override val complicationsDataSourceInfo =
        MutableStateFlow<Map<Int, ComplicationDataSourceInfo?>>(emptyMap())

    /** Pending result for ComplicationDataSourceChooserRequest. */
    internal var pendingComplicationDataSourceChooserResult:
        CompletableDeferred<ComplicationDataSourceChooserResult?>? =
        null

    private var chooseComplicationDataSource:
        ActivityResultLauncher<ComplicationDataSourceChooserRequest>? =
        activity!!.registerForActivityResult(ComplicationDataSourceChooserContract()) {
            onComplicationDataSourceChooserResult(it)
        }

    // Fetches the current ComplicationSlotState for each complication.
    internal abstract fun fetchComplicationSlotsState(): Map<Int, ComplicationSlotState>

    private val complicationSlotsStateDelegate = lazy {
        MutableStateFlow(fetchComplicationSlotsState())
    }
    override val complicationSlotsState by complicationSlotsStateDelegate

    /** Updates [complicationSlotsState] if it has been initialized. */
    internal fun maybeUpdateComplicationSlotsState() {
        if (complicationSlotsStateDelegate.isInitialized()) {
            complicationSlotsState.value = fetchComplicationSlotsState()
        }
    }

    internal fun onComplicationDataSourceChooserResult(
        complicationDataSourceChooserResult: ComplicationDataSourceChooserResult?
    ) {
        synchronized(this) {
                val deferredResult = pendingComplicationDataSourceChooserResult
                if (deferredResult == null) {
                    Log.w(
                        TAG,
                        "Ignoring onComplicationDataSourceChooserResult due to null " +
                            "pendingComplicationDataSourceChooserResult"
                    )
                    return
                }
                Log.d(TAG, "onComplicationDataSourceChooserResult")
                pendingComplicationDataSourceChooserResult = null
                deferredResult
            }
            .complete(complicationDataSourceChooserResult)
    }

    override suspend fun openComplicationDataSourceChooser(
        complicationSlotId: Int
    ): ChosenComplicationDataSource? =
        TraceEvent("BaseEditorSession.openComplicationDataSourceChooser $complicationSlotId").use {
            Log.d(TAG, "openComplicationDataSourceChooser")
            requireNotClosed()
            require(
                !complicationSlotsState.value[complicationSlotId]!!.fixedComplicationDataSource
            ) {
                "Can't configure fixed complication ID $complicationSlotId"
            }

            // Don't animate the watch face while the provider is running, because that makes
            // hardware rendering of the complication preview images very much slower.
            editorObscuresWatchFace = true

            val deferredResult = CompletableDeferred<ComplicationDataSourceChooserResult?>()

            synchronized(this) {
                // The ComplicationDataSourceChooser is modal so it doesn't make sense to allow
                // concurrent invocations so bail out if there's a pending result.
                if (pendingComplicationDataSourceChooserResult != null) {
                    throw IllegalStateException(
                        "Concurrent openComplicationDataSourceChooser invocation is not supported"
                    )
                }
                pendingComplicationDataSourceChooserResult = deferredResult

                chooseComplicationDataSource!!.launch(
                    ComplicationDataSourceChooserRequest(
                        this,
                        complicationSlotId,
                        watchFaceIdInternal.id,
                        showComplicationDeniedDialogIntent,
                        showComplicationRationaleDialogIntent
                    )
                )
            }

            val complicationDataSourceChooserResult =
                try {
                    deferredResult.await()
                } finally {
                    synchronized(this) { pendingComplicationDataSourceChooserResult = null }
                }

            editorObscuresWatchFace = false

            // If deferredResult was null then the user canceled so return null.
            if (complicationDataSourceChooserResult == null) {
                return null
            }

            val complicationDataSourceInfoRetriever =
                complicationDataSourceInfoRetrieverProvider!!
                    .getComplicationDataSourceInfoRetriever()

            try {
                deferredComplicationPreviewDataAvailable.await()
                val previousDataSourceInfo: ComplicationDataSourceInfo? =
                    complicationsDataSourceInfo.value[complicationSlotId]

                // Emit an updated complicationsDataSourceInfoMap.
                complicationsDataSourceInfo.value =
                    HashMap(complicationsDataSourceInfo.value).apply {
                        this[complicationSlotId] =
                            complicationDataSourceChooserResult.dataSourceInfo
                    }

                val previewData =
                    getPreviewData(
                        complicationDataSourceInfoRetriever,
                        complicationDataSourceChooserResult.dataSourceInfo
                    )

                // Emit an updated complicationPreviewDataMap.
                complicationsPreviewData.value =
                    HashMap(complicationsPreviewData.value).apply {
                        this[complicationSlotId] = previewData ?: EmptyComplicationData()
                    }
                onComplicationUpdated(
                    complicationSlotId,
                    from = previousDataSourceInfo,
                    to = complicationDataSourceChooserResult.dataSourceInfo,
                )

                return ChosenComplicationDataSource(
                    complicationSlotId,
                    complicationDataSourceChooserResult.dataSourceInfo,
                    complicationDataSourceChooserResult.extras,
                )
            } finally {
                // This gets called after the above coroutine has finished.
                complicationDataSourceInfoRetriever.close()

                // Changing a complication data source could result in the type and therefore the
                // bounds
                // changing. This needs to be done after updating the preview data in case that
                // affects
                // the ComplicationType and hence the bounds of the complication.
                maybeUpdateComplicationSlotsState()
            }
        }

    override val backgroundComplicationSlotId: Int? by lazy {
        requireNotClosed()
        complicationSlotsState.value.entries
            .firstOrNull { it.value.boundsType == ComplicationSlotBoundsType.BACKGROUND }
            ?.key
    }

    /**
     * Returns the complication data source's preview [ComplicationData] if possible or fallback
     * preview data based on complication data source icon and name if not. If the slot is
     * configured to be empty then it will return `null`.
     *
     * Note complicationDataSourceInfoRetriever.requestPreviewComplicationData which requires R will
     * never be called pre R because [ComplicationDataSourceInfo.componentName] is only non null
     * from R onwards.
     */
    @SuppressLint("NewApi")
    internal suspend fun getPreviewData(
        complicationDataSourceInfoRetriever: ComplicationDataSourceInfoRetriever,
        dataSourceInfo: ComplicationDataSourceInfo?
    ): ComplicationData? =
        TraceEvent("BaseEditorSession.getPreviewData").use {
            if (dataSourceInfo == null) {
                return null
            }
            // Fetch preview ComplicationData if possible.
            if (dataSourceInfo.componentName == null) {
                return dataSourceInfo.fallbackPreviewData
            }
            try {
                val previewData =
                    complicationDataSourceInfoRetriever.retrievePreviewComplicationData(
                        dataSourceInfo.componentName!!,
                        dataSourceInfo.type
                    )
                // If the data source sends us ComplicationData of the wrong type then use fallback
                // data.
                if (previewData == null || previewData.type != dataSourceInfo.type) {
                    return dataSourceInfo.fallbackPreviewData
                }
                return previewData
            } catch (e: Exception) {
                // Something went wrong, so use fallback preview data.
                return dataSourceInfo.fallbackPreviewData
            }
        }

    protected fun fetchComplicationsData(fetchCoroutineScope: CoroutineScope): Job {
        val complicationDataSourceInfoRetriever =
            complicationDataSourceInfoRetrieverProvider!!.getComplicationDataSourceInfoRetriever()
        return fetchCoroutineScope.launchWithTracing("BaseEditorSession.fetchComplicationsData") {
            try {
                // Unlikely but WCS could conceivably crash during this call. We could retry but
                // it's not obvious if that'd succeed or if WCS session state is recoverable,
                // it's probably better to crash and start over.
                val dataSourceInfoArray =
                    complicationDataSourceInfoRetriever.retrieveComplicationDataSourceInfo(
                        watchFaceComponentName,
                        complicationSlotsState.value.keys.toIntArray()
                    )
                complicationsDataSourceInfo.value =
                    extractComplicationsDataSourceInfoMap(dataSourceInfoArray) ?: emptyMap()

                // Parallel fetch preview ComplicationData.
                complicationsPreviewData.value =
                    dataSourceInfoArray
                        ?.associateBy(
                            { it.slotId },
                            {
                                async {
                                    getPreviewData(complicationDataSourceInfoRetriever, it.info)
                                }
                            }
                            // Coerce to a Map<Int, ComplicationData> omitting null values.
                            // If mapNotNullValues existed we would use it here.
                        )
                        ?.mapValues { it.value.await() ?: EmptyComplicationData() }
                        ?: emptyMap()
                deferredComplicationPreviewDataAvailable.complete(Unit)
            } finally {
                complicationDataSourceInfoRetriever.close()
            }
        }
    }

    override fun close() {
        Log.d(TAG, "close")
        // Silently do nothing if we've been force closed, this simplifies the editor activity.
        if (forceClosed) {
            return
        }
        requireNotClosed()
        EditorService.globalEditorService.removeCloseCallback(closeCallback)
        // We need to send the preview data which we obtain asynchronously, however we need to
        // shutdown gracefully in the same task or we risk leaking the
        // ComplicationDataSourceInfoRetriever.
        runBlocking {
            try {
                withTimeout(CLOSE_BROADCAST_TIMEOUT_MILLIS) {
                    deferredComplicationPreviewDataAvailable.await()
                    val previewImage =
                        if (
                            commitChangesOnClose &&
                                previewScreenshotParams != null &&
                                Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1
                        ) {
                            SharedMemoryImage.ashmemWriteImageBundle(
                                renderWatchFaceToBitmap(
                                    previewScreenshotParams.renderParameters,
                                    previewScreenshotParams.instant,
                                    complicationsPreviewData.value
                                )
                            )
                        } else {
                            null
                        }
                    EditorService.globalEditorService.broadcastEditorState(
                        EditorStateWireFormat(
                            watchFaceIdInternal.id,
                            userStyle.value.toWireFormat(),
                            complicationsPreviewData.value.mapNotNull {
                                if (complicationSlotsState.value[it.key]!!.isEnabled) {
                                    IdAndComplicationDataWireFormat(
                                        it.key,
                                        it.value.asWireComplicationData()
                                    )
                                } else null
                            },
                            commitChangesOnClose,
                            previewImage
                        )
                    )
                }
            } catch (e: TimeoutCancellationException) {
                Log.w(TAG, "Ignoring exception in close", e)
                // Ignore this, nothing we can do.
            }

            releaseResources()
            closed = true
            editorSessionTraceEvent.close()
            activity = null
            complicationDataSourceInfoRetrieverProvider = null
            chooseComplicationDataSource = null
        }
    }

    @UiThread
    internal fun forceClose() {
        Log.d(TAG, "forceClose")
        commitChangesOnClose = false
        closed = true
        forceClosed = true
        releaseResources()
        EditorService.globalEditorService.removeCloseCallback(closeCallback)
        editorSessionTraceEvent.close()
        activity?.finish()
        activity = null
        complicationDataSourceInfoRetrieverProvider = null
        chooseComplicationDataSource = null
    }

    protected fun requireNotClosed() {
        require(!closed or forceClosed) { "EditorSession method called after close()" }
    }

    @UiThread protected abstract fun releaseResources()

    protected open val showComplicationDeniedDialogIntent: Intent? = null

    protected open val showComplicationRationaleDialogIntent: Intent? = null

    protected open fun onComplicationUpdated(
        complicationSlotId: Int,
        from: ComplicationDataSourceInfo?,
        to: ComplicationDataSourceInfo?,
    ) {}
}

/**
 * @param activity The editor's [ComponentActivity].
 * @param watchFaceIdInternal The original ID sent to us in the [WatchFaceEditorContract]. We need
 *   this because the system expects [EditorState.watchFaceId] to match.
 * @param complicationDataSourceInfoRetrieverProvider Used to obtain
 *   [ComplicationDataSourceInfoRetriever]
 * @param coroutineScope The main thread [CoroutineScope]
 * @param previewScreenshotParams Optional [PreviewScreenshotParams]
 */
internal class OnWatchFaceEditorSessionImpl(
    activity: ComponentActivity,
    override val watchFaceComponentName: ComponentName,
    watchFaceIdInternal: WatchFaceId,
    private val initialEditorUserStyle: UserStyleData?,
    complicationDataSourceInfoRetrieverProvider: ComplicationDataSourceInfoRetrieverProvider,
    coroutineScope: CoroutineScope,
    previewScreenshotParams: PreviewScreenshotParams?
) :
    BaseEditorSession(
        activity,
        complicationDataSourceInfoRetrieverProvider,
        coroutineScope,
        previewScreenshotParams,
        watchFaceIdInternal
    ) {
    private lateinit var editorDelegate: WatchFace.EditorDelegate

    private companion object {
        private const val TAG = "OnWatchFaceEditorSessionImpl"
    }

    override val userStyleSchema by lazy {
        requireNotClosed()
        editorDelegate.userStyleSchema
    }

    override val previewReferenceInstant: Instant by lazy { editorDelegate.previewReferenceInstant }

    override val watchFaceId = WatchFaceId(sanitizeWatchFaceId(watchFaceIdInternal.id))

    @OptIn(ComplicationExperimental::class)
    override fun fetchComplicationSlotsState(): Map<Int, ComplicationSlotState> {
        return editorDelegate.complicationSlotsManager.complicationSlots.mapValues {
            requireNotClosed()
            // Get the ComplicationType from the preview data if available. This is important
            // because the type and therefore bounds may change based on the selected complication
            // data source.
            val type =
                complicationsPreviewData.value.let { previewDataMap ->
                    previewDataMap[it.key]?.type ?: it.value.complicationData.value.type
                }
            ComplicationSlotState(
                it.value.computeBounds(editorDelegate.screenBounds, type, applyMargins = false),
                it.value.boundsType,
                it.value.supportedTypes,
                it.value.defaultDataSourcePolicy,
                it.value.enabled,
                it.value.initiallyEnabled,
                it.value.renderer.getData().type,
                it.value.fixedComplicationDataSource,
                it.value.configExtras,
                it.value.nameResourceId,
                it.value.screenReaderNameResourceId,
                it.value.boundingArc
            )
        }
    }

    internal val wrappedUserStyle by lazy { MutableStateFlow(editorDelegate.userStyle) }

    override var editorObscuresWatchFace: Boolean
        get() = editorDelegate.editorObscuresWatchFace
        set(value) {
            editorDelegate.editorObscuresWatchFace = value
        }

    // Unfortunately a dynamic proxy is the only way we can reasonably validate the UserStyle,
    // exceptions thrown within a coroutine are lost and the MutableStateFlow interface includes
    // internal unstable methods so we can't use a static proxy...
    @Suppress("BanUncheckedReflection", "UNCHECKED_CAST")
    override val userStyle =
        Proxy.newProxyInstance(
            MutableStateFlow::class.java.classLoader,
            arrayOf<Class<*>>(MutableStateFlow::class.java)
        ) { _, method, args ->
            if (args == null) {
                method?.invoke(wrappedUserStyle)
            } else {
                val result = method?.invoke(wrappedUserStyle, *args)
                when (method?.name) {
                    "setValue" -> validateAndUpdateUserStyle(args[0] as UserStyle)
                    "compareAndSet" -> {
                        if (result is Boolean && result == true) {
                            validateAndUpdateUserStyle(args[1] as UserStyle)
                        }
                    }
                    else -> {
                        Log.e(
                            TAG,
                            "userStyle proxy encountered unexpected method name '${method.name}'" +
                                " please check your proguard rules."
                        )
                    }
                }
                result
            }
        } as MutableStateFlow<UserStyle>

    @Suppress("Deprecation") // userStyleSettings
    internal fun validateAndUpdateUserStyle(userStyle: UserStyle) {
        for (userStyleSetting in userStyle.keys) {
            require(userStyleSchema.userStyleSettings.contains(userStyleSetting)) {
                "A userStyleSetting ($userStyleSetting) in userStyle does not match " +
                    "references in EditorSession's userStyleSchema."
            }
        }

        editorDelegate.userStyle = userStyle

        // Changing the style may enable/disable complications.
        maybeUpdateComplicationSlotsState()
    }

    private lateinit var previousWatchFaceUserStyle: UserStyle
    private lateinit var backgroundCoroutineScope: CoroutineScope
    private lateinit var fetchComplicationsDataJob: Job

    override fun renderWatchFaceToBitmap(
        renderParameters: RenderParameters,
        instant: Instant,
        slotIdToComplicationData: Map<Int, ComplicationData>?
    ): Bitmap {
        requireNotClosed()
        require(renderParameters.drawMode == DrawMode.INTERACTIVE) {
            "Currently only DrawMode.INTERACTIVE is supported"
        }
        return editorDelegate.renderWatchFaceToBitmap(
            renderParameters,
            if (instant == EditorSession.DEFAULT_PREVIEW_INSTANT) {
                editorDelegate.previewReferenceInstant
            } else {
                instant
            },
            slotIdToComplicationData
        )
    }

    override fun setOverrideComplications(slotIdToComplicationData: Map<Int, ComplicationData>) {
        editorDelegate.setOverrideComplications(slotIdToComplicationData)
    }

    override fun releaseResources() {
        // If commitChangesOnClose is true, the userStyle is not restored which for non-headless
        // watch faces meaning the style is applied immediately. It's possible for the System to
        // fail to persist this change and we rely on the system reverting the style change in this
        // eventuality.
        if (!commitChangesOnClose && this::previousWatchFaceUserStyle.isInitialized) {
            userStyle.value = previousWatchFaceUserStyle
        }
        if (this::editorDelegate.isInitialized) {
            editorDelegate.complicationSlotsManager.unfreezeAllSlotsForEdit(
                clearData = commitChangesOnClose
            )
        }

        if (this::fetchComplicationsDataJob.isInitialized) {
            // Wait until the fetchComplicationsDataJob has finished and released the
            // complicationDataSourceInfoRetriever. This is important because if the service
            // finishes before this is finished we'll get errors complaining that the service
            // wasn't unbound.
            runBlocking {
                // Canceling the scope & the job means the join will be fast and we won't block for
                // long. In practice we often won't block at all because fetchComplicationsDataJob
                // is run only once during editor initialization and it will usually be finished
                // by the time the user closes the editor.
                backgroundCoroutineScope.cancel()
                fetchComplicationsDataJob.join()
            }
        }

        // Note this has to be done last to ensure tests are not racy.
        if (this::editorDelegate.isInitialized) {
            editorDelegate.setComplicationSlotConfigExtrasChangeCallback(null)
            editorDelegate.onDestroy()
        }
    }

    fun setEditorDelegate(editorDelegate: WatchFace.EditorDelegate) {
        this.editorDelegate = editorDelegate

        previousWatchFaceUserStyle = editorDelegate.userStyle

        // Apply any initial style from the intent.  Note we don't restore the previous style at
        // the end since we assume we're editing the current active watchface.
        if (initialEditorUserStyle != null) {
            editorDelegate.userStyle =
                UserStyle(initialEditorUserStyle, editorDelegate.userStyleSchema)
        }

        backgroundCoroutineScope =
            CoroutineScope(editorDelegate.backgroundThreadHandler.asCoroutineDispatcher().immediate)

        fetchComplicationsDataJob = fetchComplicationsData(backgroundCoroutineScope)

        editorDelegate.setComplicationSlotConfigExtrasChangeCallback(
            object : WatchFace.ComplicationSlotConfigExtrasChangeCallback {
                override fun onComplicationSlotConfigExtrasChanged() {
                    maybeUpdateComplicationSlotsState()
                }
            }
        )
    }

    override val showComplicationDeniedDialogIntent
        get() = editorDelegate.complicationDeniedDialogIntent

    override val showComplicationRationaleDialogIntent
        get() = editorDelegate.complicationRationaleDialogIntent

    override fun getComplicationSlotIdAt(@Px x: Int, @Px y: Int): Int? {
        requireNotClosed()
        return editorDelegate.complicationSlotsManager.getComplicationSlotAt(x, y)?.id
    }

    override fun onComplicationUpdated(
        complicationSlotId: Int,
        from: ComplicationDataSourceInfo?,
        to: ComplicationDataSourceInfo?,
    ) {
        editorDelegate.complicationSlotsManager.freezeSlotForEdit(
            complicationSlotId,
            from = from,
            to = to,
        )
    }
}

@RequiresApi(27)
internal class HeadlessEditorSession(
    activity: ComponentActivity,
    private val headlessWatchFaceClient: HeadlessWatchFaceClient,
    override val watchFaceComponentName: ComponentName,
    watchFaceIdInternal: WatchFaceId,
    initialUserStyle: UserStyleData,
    complicationDataSourceInfoRetrieverProvider: ComplicationDataSourceInfoRetrieverProvider,
    coroutineScope: CoroutineScope,
    previewScreenshotParams: PreviewScreenshotParams?
) :
    BaseEditorSession(
        activity,
        complicationDataSourceInfoRetrieverProvider,
        coroutineScope,
        previewScreenshotParams,
        watchFaceIdInternal
    ) {
    override val userStyleSchema = headlessWatchFaceClient.userStyleSchema

    override val userStyle = MutableStateFlow(UserStyle(initialUserStyle, userStyleSchema))

    private val overrideComplicationData = HashMap<Int, ComplicationData>()

    init {
        coroutineScope.launch {
            var first = true
            userStyle.collect {
                // Changing the style may enable/disable complications. We can ignore the first
                // callback because it's for the initial style.
                if (!first) {
                    maybeUpdateComplicationSlotsState()
                }
                first = false
            }
        }
    }

    override val previewReferenceInstant = headlessWatchFaceClient.previewReferenceInstant

    // Fetches the current ComplicationSlotState for each complication.
    override fun fetchComplicationSlotsState(): Map<Int, ComplicationSlotState> =
        headlessWatchFaceClient.complicationSlotsState

    override fun renderWatchFaceToBitmap(
        renderParameters: RenderParameters,
        instant: Instant,
        slotIdToComplicationData: Map<Int, ComplicationData>?
    ): Bitmap {
        requireNotClosed()

        var complications = slotIdToComplicationData
        if (overrideComplicationData.isNotEmpty() && complications != null) {
            // Merge overrideComplicationData with slotIdToComplicationData
            val merged = HashMap<Int, ComplicationData>(overrideComplicationData)
            for (pair in complications) {
                merged[pair.key] = pair.value
            }
            complications = merged
        }

        return headlessWatchFaceClient.renderWatchFaceToBitmap(
            renderParameters,
            if (instant == EditorSession.DEFAULT_PREVIEW_INSTANT) {
                headlessWatchFaceClient.previewReferenceInstant
            } else {
                instant
            },
            userStyle.value,
            complications
        )
    }

    override fun setOverrideComplications(slotIdToComplicationData: Map<Int, ComplicationData>) {
        // This isn't actually an optimization, however HeadlessEditorSession is not commonly used
        // and this is just here for compatibility.
        for (pair in slotIdToComplicationData) {
            overrideComplicationData[pair.key] = pair.value
        }
    }

    override fun releaseResources() {
        headlessWatchFaceClient.close()
    }

    init {
        fetchComplicationsData(coroutineScope)
    }

    override fun getComplicationSlotIdAt(@Px x: Int, @Px y: Int): Int? {
        requireNotClosed()
        return complicationSlotsState.value.entries
            .firstOrNull {
                it.value.isEnabled &&
                    when (it.value.boundsType) {
                        ComplicationSlotBoundsType.ROUND_RECT -> it.value.bounds.contains(x, y)
                        ComplicationSlotBoundsType.BACKGROUND -> false
                        ComplicationSlotBoundsType.EDGE -> false
                        else -> false
                    }
            }
            ?.key
    }
}

internal class ComplicationDataSourceChooserRequest(
    internal val editorSession: EditorSession,
    internal val complicationSlotId: Int,
    internal val instanceId: String?,
    internal var showComplicationDeniedDialogIntent: Intent?,
    internal val showComplicationRationaleDialogIntent: Intent?
)

internal class ComplicationDataSourceChooserResult(
    /** The updated [ComplicationDataSourceInfo] or `null` if the empty data source was chosen. */
    internal val dataSourceInfo: ComplicationDataSourceInfo?,
    /** Any additional extras returned by complication data source chooser. */
    internal val extras: Bundle,
)

/**
 * An [ActivityResultContract] for invoking the complication data source chooser. If the user
 * cancels the data source chooser than the result will be `null`.
 */
internal class ComplicationDataSourceChooserContract :
    ActivityResultContract<
        ComplicationDataSourceChooserRequest, ComplicationDataSourceChooserResult?
    >() {

    internal companion object {
        const val EXTRA_PROVIDER_INFO = "android.support.wearable.complications.EXTRA_PROVIDER_INFO"

        /**
         * Whether to invoke a test activity instead of the [ComplicationHelperActivity].
         *
         * To be used in tests.
         */
        internal var useTestComplicationHelperActivity = false
    }

    override fun createIntent(
        context: Context,
        input: ComplicationDataSourceChooserRequest
    ): Intent {
        val complicationSlotsState = input.editorSession.complicationSlotsState.value
        val intent =
            ComplicationHelperActivity.createComplicationDataSourceChooserHelperIntent(
                context,
                input.editorSession.watchFaceComponentName,
                input.complicationSlotId,
                complicationSlotsState[input.complicationSlotId]!!.supportedTypes,
                input.instanceId,
                input.showComplicationDeniedDialogIntent,
                input.showComplicationRationaleDialogIntent,
                input.editorSession.userStyle.value.toUserStyleData()
            )
        val complicationState = complicationSlotsState[input.complicationSlotId]!!
        intent.replaceExtras(
            Bundle(complicationState.complicationConfigExtras).apply { putAll(intent.extras!!) }
        )
        if (useTestComplicationHelperActivity) {
            intent.component =
                ComponentName(
                    "androidx.wear.watchface.editor.test",
                    "androidx.wear.watchface.editor.TestComplicationHelperActivity"
                )
        }
        return intent
    }

    @Suppress("DEPRECATION")
    override fun parseResult(resultCode: Int, intent: Intent?) =
        intent?.let {
            val extras =
                intent.extras?.let { extras ->
                    Bundle(extras).apply { remove(EXTRA_PROVIDER_INFO) }
                }
                    ?: Bundle.EMPTY
            ComplicationDataSourceChooserResult(
                it.getParcelableExtra<
                        android.support.wearable.complications.ComplicationProviderInfo
                    >(
                        EXTRA_PROVIDER_INFO
                    )
                    ?.toApiComplicationDataSourceInfo(),
                extras
            )
        }
}

/**
 * Extracts a map from complication ID to the corresponding [ComplicationDataSourceInfo] from the
 * given array of [ComplicationDataSourceInfoRetriever.Result].
 */
internal fun extractComplicationsDataSourceInfoMap(
    resultArray: Array<ComplicationDataSourceInfoRetriever.Result>?
): Map<Int, ComplicationDataSourceInfo?>? = resultArray?.associateBy({ it.slotId }, { it.info })

@Suppress("DEPRECATION") internal fun Bundle.asString() = keySet().map { "$it: ${get(it)}" }
