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
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContract
import androidx.annotation.Px
import androidx.annotation.RequiresApi
import androidx.annotation.RestrictTo
import androidx.annotation.UiThread
import androidx.wear.complications.ComplicationDataSourceInfo
import androidx.wear.complications.ComplicationDataSourceInfoRetriever
import androidx.wear.complications.data.ComplicationData
import androidx.wear.complications.data.ComplicationText
import androidx.wear.complications.data.ComplicationType
import androidx.wear.complications.data.EmptyComplicationData
import androidx.wear.complications.data.MonochromaticImage
import androidx.wear.complications.data.PlainComplicationText
import androidx.wear.complications.data.ShortTextComplicationData
import androidx.wear.complications.toApiComplicationDataSourceInfo
import androidx.wear.watchface.ComplicationHelperActivity
import androidx.wear.utility.AsyncTraceEvent
import androidx.wear.utility.TraceEvent
import androidx.wear.utility.launchWithTracing
import androidx.wear.watchface.DrawMode
import androidx.wear.watchface.RenderParameters
import androidx.wear.watchface.WatchFace
import androidx.wear.watchface.client.ComplicationSlotState
import androidx.wear.watchface.client.EditorListener
import androidx.wear.watchface.client.EditorServiceClient
import androidx.wear.watchface.client.EditorState
import androidx.wear.watchface.client.HeadlessWatchFaceClient
import androidx.wear.watchface.client.WatchFaceId
import androidx.wear.watchface.control.data.HeadlessWatchFaceInstanceParams
import androidx.wear.watchface.data.ComplicationSlotBoundsType
import androidx.wear.watchface.data.IdAndComplicationDataWireFormat
import androidx.wear.watchface.editor.data.EditorStateWireFormat
import androidx.wear.watchface.style.UserStyle
import androidx.wear.watchface.style.UserStyleData
import androidx.wear.watchface.style.UserStyleSchema
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.android.asCoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

private typealias WireComplicationProviderInfo =
    android.support.wearable.complications.ComplicationProviderInfo

/**
 * Interface for manipulating watch face state during a watch face editing session. The editor
 * should adjust [userStyle] and call [openComplicationDataSourceChooser] to configure the watch
 * face and call [close] when done. This reports the updated [EditorState] to the [EditorListener]s
 * registered via [EditorServiceClient.addListener].
 *
 * For EditorSessions backed by a headless instance (see [createHeadlessEditingSession] and
 * [EditorRequest.headlessDeviceConfig]), style changes are not applied to the interactive
 * instance and it's up to the system to apply them. For EditorSessions backed by an
 * interactive instance style changes are applied immediately. Its possible the system might fail to
 * persist the style changes (e.g. to data base write failure or a crash) and if this happens its
 * the responsibiltiy of the system to revert the style change.
 */
public abstract class EditorSession : AutoCloseable {
    /** The [ComponentName] of the watch face being edited. */
    public abstract val watchFaceComponentName: ComponentName

    /**
     * Unique ID for the instance of the watch face being edited, only defined for Android R and
     * beyond, it's `null` on Android P and earlier. Note each distinct [ComponentName] can have
     * multiple instances.
     */
    @get:RequiresApi(Build.VERSION_CODES.R)
    public abstract val watchFaceId: WatchFaceId

    /** The current [UserStyle]. Assigning to this will cause the style to update. However, styling
     * changes to the watch face will be reverted upon exit. */
    public abstract var userStyle: UserStyle

    /** The UTC reference preview time for this watch face in milliseconds since the epoch. */
    public abstract val previewReferenceTimeMillis: Long

    /** The watch face's [UserStyleSchema]. */
    public abstract val userStyleSchema: UserStyleSchema

    /**
     * Map of complication slot ids to [ComplicationSlotState] for each complication slot. Note
     * [ComplicationSlotState] can change, typically in response to styling.
     */
    public abstract val complicationSlotsState: Map<Int, ComplicationSlotState>

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
    @get:JvmName("isCommitChangesOnClose")
    @set:UiThread
    public var commitChangesOnClose: Boolean = true

    /**
     * Returns a map of [androidx.wear.watchface.ComplicationSlot] ids to preview [ComplicationData]
     * suitable for use in rendering a preview of the watch face. Note if a slot is configured to
     * be empty then it will an instance of [EmptyComplicationData]. Disabled complicationSlots
     * are included. Note also unlike live data this is static per complication data source, but it
     * may update (on the UiThread) as a result of [openComplicationDataSourceChooser].
     */
    @UiThread
    public abstract suspend fun getComplicationsPreviewData(): Map<Int, ComplicationData>

    /**
     * Returns a map of [androidx.wear.watchface.ComplicationSlot] ids to
     * [ComplicationDataSourceInfo] that represent the information available about the data
     * source for each complication.
     *
     * A `null` [ComplicationDataSourceInfo] will be associated with a complication slot id if the
     * [androidx.wear.watchface.ComplicationSlot] is configured to show the empty complication
     * data source.
     */
    @UiThread
    public abstract suspend fun getComplicationsDataSourceInfo():
        Map<Int, ComplicationDataSourceInfo?>

    /** The ID of the background complication or `null` if there isn't one. */
    @get:SuppressWarnings("AutoBoxing")
    public abstract val backgroundComplicationSlotId: Int?

    /**
     * Returns the ID of the complication at the given coordinates or `null` if there isn't one.
     * Only [androidx.wear.watchface.ComplicationSlot]s with
     * [ComplicationSlotBoundsType.ROUND_RECT] are supported by this function.
     */
    @SuppressWarnings("AutoBoxing")
    @UiThread
    public abstract fun getComplicationSlotIdAt(@Px x: Int, @Px y: Int): Int?

    /**
     * Renders the watch face to a [Bitmap] using the current [userStyle].
     *
     * @param renderParameters The [RenderParameters] to render with. Must be [DrawMode.INTERACTIVE]
     * @param calendarTimeMillis The UTC time in milliseconds since the epoch to render with
     * @param slotIdToComplicationData The [ComplicationData] for each
     * [androidx.wear.watchface.ComplicationSlot] to render with
     */
    @UiThread
    public abstract fun renderWatchFaceToBitmap(
        renderParameters: RenderParameters,
        calendarTimeMillis: Long,
        slotIdToComplicationData: Map<Int, ComplicationData>?
    ): Bitmap

    /**
     * Opens the complication data source chooser and returns the chosen complication data source
     * for the specified [androidx.wear.watchface.ComplicationSlot].
     *
     * The result returns `null` if the operation was cancelled and otherwise returned an
     * instance of [ChosenComplicationDataSource] that contains information about the chosen
     * data source.
     *
     * If the complication data source was changed then the map returned by
     * [getComplicationsPreviewData] is updated (on the UiThread).
     *
     * @param complicationSlotId The id of the [androidx.wear.watchface.ComplicationSlot] to select
     * a complication data source for.
     * @throws IllegalStateException if a previous invocation of openComplicationDataSourceChooser
     * is still running when openComplicationDataSourceChooser is called.
     */
    @UiThread
    public abstract suspend fun openComplicationDataSourceChooser(complicationSlotId: Int):
        ChosenComplicationDataSource?

    public companion object {
        /**
         * Constructs an [EditorSession] for an on watch face editor. This registers an activity
         * result handler and so it must be called during an Activity or Fragment initialization
         * path.
         *
         * @param activity The [ComponentActivity] associated with the [EditorSession].
         * @param editIntent The [Intent] sent by SysUI to launch the editing session.
         * @return Deferred<EditorSession?> which is resolved with either the [EditorSession] or
         * `null` if it can't be constructed.
         * @throws [TimeoutCancellationException] if it takes more than
         * [EDITING_SESSION_TIMEOUT_MILLIS] milliseconds to create a watch face editor.
         */
        @SuppressWarnings("ExecutorRegistration")
        @JvmStatic
        @UiThread
        @Throws(TimeoutCancellationException::class)
        public suspend fun createOnWatchEditingSession(
            activity: ComponentActivity,
            editIntent: Intent
        ): EditorSession = createOnWatchEditingSessionImpl(
            activity,
            editIntent,
            object : ComplicationDataSourceInfoRetrieverProvider {
                override fun getComplicationDataSourceInfoRetriever() =
                    ComplicationDataSourceInfoRetriever(activity)
            }
        )

        // Used by tests.
        @Throws(TimeoutCancellationException::class)
        internal suspend fun createOnWatchEditingSessionImpl(
            activity: ComponentActivity,
            editIntent: Intent,
            complicationDataSourceInfoRetrieverProvider: ComplicationDataSourceInfoRetrieverProvider
        ): EditorSession = TraceEvent(
            "EditorSession.createOnWatchEditingSessionAsyncImpl"
        ).use {
            val coroutineScope =
                CoroutineScope(Handler(Looper.getMainLooper()).asCoroutineDispatcher().immediate)
            var isRFlow = true
            val editorRequest = editIntent.getParcelableExtra<ComponentName>(
                Constants.EXTRA_WATCH_FACE_COMPONENT
            )?.let {
                isRFlow = false
                EditorRequest(it, "", null)
            } ?: EditorRequest.createFromIntent(editIntent)
            // We need to respect the lifecycle and register the ActivityResultListener now.
            val session = OnWatchFaceEditorSessionImpl(
                activity,
                editorRequest.watchFaceComponentName,
                editorRequest.watchFaceId,
                editorRequest.initialUserStyle,
                complicationDataSourceInfoRetrieverProvider,
                coroutineScope,
                isRFlow,
                editorRequest.previewScreenshotParams
            )
            // But full initialization has to be deferred because
            // [WatchFace.getOrCreateEditorDelegate] is async.
            // Resolve only after init has been completed.
            withContext(coroutineScope.coroutineContext) {
                withTimeout(EDITING_SESSION_TIMEOUT_MILLIS) {
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
                                    activity.resources.displayMetrics.heightPixels
                                ),
                                activity
                            )
                        } else {
                            WatchFace.getOrCreateEditorDelegate(
                                editorRequest.watchFaceComponentName
                            ).await()
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
         * @param activity The [ComponentActivity] associated with the EditorSession.
         * @param editIntent The [Intent] sent by SysUI to launch the editing session.
         * @param headlessWatchFaceClient The [HeadlessWatchFaceClient] to use for rendering etc...
         * @return The [EditorSession] or `null` if it could not be constructed.
         */
        @JvmStatic
        @RequiresApi(27)
        @UiThread
        public fun createHeadlessEditingSession(
            activity: ComponentActivity,
            editIntent: Intent,
            headlessWatchFaceClient: HeadlessWatchFaceClient
        ): EditorSession = TraceEvent("EditorSession.createHeadlessEditingSession").use {
            EditorRequest.createFromIntent(editIntent).let {
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
        public const val EDITING_SESSION_TIMEOUT_MILLIS: Long = 4000L
    }
}

/**
 * The complication data source that was chosen by the user for a given
 * [androidx.wear.watchface.ComplicationSlot] id as a result to a call to
 * [EditorSession.openComplicationDataSourceChooser].
 *
 * @param complicationSlotId The ID of the complication slot that was configured.
 * @param complicationDataSourceInfo The complication data source that was chosen for this slot, or
 * `null` if the empty complication source was was chosen.
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

/**
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public abstract class BaseEditorSession internal constructor(
    private val activity: ComponentActivity,
    private val complicationDataSourceInfoRetrieverProvider:
        ComplicationDataSourceInfoRetrieverProvider,
    public val coroutineScope: CoroutineScope,
    private val previewScreenshotParams: PreviewScreenshotParams?
) : EditorSession() {
    protected var closed: Boolean = false
    protected var forceClosed: Boolean = false

    private val editorSessionTraceEvent = AsyncTraceEvent("EditorSession")
    private val closeCallback = object : EditorService.CloseCallback() {
        override fun onClose() {
            // onClose could be called on any thread but forceClose needs to be called from the UI
            // thread.
            coroutineScope.launch {
                forceClose()
            }
        }
    }

    private companion object {
        /** Timeout for fetching ComplicationsPreviewData in [BaseEditorSession.close]. */
        private const val CLOSE_BROADCAST_TIMEOUT_MILLIS = 500L
    }

    init {
        EditorService.globalEditorService.addCloseCallback(closeCallback)
    }

    /**
     * This is completed when [fetchComplicationsData] has called [getPreviewData] for each
     * complication and each of those have been completed.
     */
    private val deferredComplicationPreviewDataMap =
        CompletableDeferred<MutableMap<Int, ComplicationData>>()

    override suspend fun getComplicationsPreviewData(): Map<Int, ComplicationData> {
        return deferredComplicationPreviewDataMap.await()
    }

    // This is completed when [fetchDataSourceInfo] has called [getDataSourceInfo] for each
    // complication and each of those have been completed.
    private val deferredComplicationsDataSourceInfoMap =
        CompletableDeferred<MutableMap<Int, ComplicationDataSourceInfo?>>()

    override suspend fun getComplicationsDataSourceInfo(): Map<Int, ComplicationDataSourceInfo?> =
        deferredComplicationsDataSourceInfoMap.await()

    /** Pending result for ComplicationDataSourceChooserRequest. */
    internal var pendingComplicationDataSourceChooserResult:
        CompletableDeferred<ComplicationDataSourceChooserResult?>? = null

    private val chooseComplicationDataSource =
        activity.registerForActivityResult(ComplicationDataSourceChooserContract()) {
            onComplicationDataSourceChooserResult(it)
        }

    internal fun onComplicationDataSourceChooserResult(
        complicationDataSourceChooserResult: ComplicationDataSourceChooserResult?
    ) {
        synchronized(this) {
            val deferredResult = pendingComplicationDataSourceChooserResult
            pendingComplicationDataSourceChooserResult = null
            deferredResult
        }!!.complete(complicationDataSourceChooserResult)
    }

    override suspend fun openComplicationDataSourceChooser(
        complicationSlotId: Int
    ): ChosenComplicationDataSource? = TraceEvent(
        "BaseEditorSession.openComplicationDataSourceChooser $complicationSlotId"
    ).use {
        requireNotClosed()
        require(!complicationSlotsState[complicationSlotId]!!.fixedComplicationDataSource) {
            "Can't configure fixed complication ID $complicationSlotId"
        }

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

            chooseComplicationDataSource.launch(
                ComplicationDataSourceChooserRequest(
                    this,
                    complicationSlotId,
                    watchFaceId.id
                )
            )
        }

        val complicationDataSourceChooserResult = try {
            deferredResult.await()
        } finally {
            synchronized(this) {
                pendingComplicationDataSourceChooserResult = null
            }
        }

        // If deferredResult was null then the user canceled so return null.
        if (complicationDataSourceChooserResult == null) {
            return null
        }

        val complicationDataSourceInfoRetriever =
            complicationDataSourceInfoRetrieverProvider.getComplicationDataSourceInfoRetriever()

        try {
            val complicationsDataSourceInfoMap = deferredComplicationsDataSourceInfoMap.await()
            complicationsDataSourceInfoMap[complicationSlotId] =
                complicationDataSourceChooserResult.dataSourceInfo
            val previewData = getPreviewData(
                complicationDataSourceInfoRetriever,
                complicationDataSourceChooserResult.dataSourceInfo
            )
            val complicationPreviewDataMap = deferredComplicationPreviewDataMap.await()
            if (previewData == null) {
                complicationPreviewDataMap[complicationSlotId] =
                    EmptyComplicationData()
            } else {
                complicationPreviewDataMap[complicationSlotId] = previewData
            }
            return ChosenComplicationDataSource(
                complicationSlotId,
                complicationDataSourceChooserResult.dataSourceInfo,
                complicationDataSourceChooserResult.extras,
            )
        } finally {
            // This gets called after the above coroutine has finished.
            complicationDataSourceInfoRetriever.close()
        }
    }

    override val backgroundComplicationSlotId: Int? by lazy {
        requireNotClosed()
        complicationSlotsState.entries.firstOrNull {
            it.value.boundsType == ComplicationSlotBoundsType.BACKGROUND
        }?.key
    }

    override fun getComplicationSlotIdAt(@Px x: Int, @Px y: Int): Int? {
        requireNotClosed()
        return complicationSlotsState.entries.firstOrNull {
            it.value.isEnabled && when (it.value.boundsType) {
                ComplicationSlotBoundsType.ROUND_RECT -> it.value.bounds.contains(x, y)
                ComplicationSlotBoundsType.BACKGROUND -> false
                ComplicationSlotBoundsType.EDGE -> false
                else -> false
            }
        }?.key
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
    ): ComplicationData? = TraceEvent("BaseEditorSession.getPreviewData").use {
        if (dataSourceInfo == null) {
            return null
        }
        // Fetch preview ComplicationData if possible.
        dataSourceInfo.componentName?.let {
            try {
                complicationDataSourceInfoRetriever.retrievePreviewComplicationData(
                    it,
                    dataSourceInfo.type
                )
            } catch (e: Exception) {
                // Something went wrong, so use fallback preview data.
                makeFallbackPreviewData(dataSourceInfo)
            }
        } ?: makeFallbackPreviewData(dataSourceInfo)
    }

    private fun makeFallbackPreviewData(
        dataSourceInfo: ComplicationDataSourceInfo
    ) = ShortTextComplicationData.Builder(
        PlainComplicationText.Builder(dataSourceInfo.name).build(),
        ComplicationText.EMPTY
    ).setMonochromaticImage(
        MonochromaticImage.Builder(dataSourceInfo.icon).build()
    ).build()

    protected fun fetchComplicationsData(fetchCoroutineScope: CoroutineScope) {
        val complicationDataSourceInfoRetriever =
            complicationDataSourceInfoRetrieverProvider.getComplicationDataSourceInfoRetriever()
        fetchCoroutineScope.launchWithTracing("BaseEditorSession.fetchComplicationsData") {
            try {
                // Unlikely but WCS could conceivably crash during this call. We could retry but
                // it's not obvious if that'd succeed or if WCS session state is recoverable,
                // it's probably better to crash and start over.
                val dataSourceInfoArray =
                    complicationDataSourceInfoRetriever.retrieveComplicationDataSourceInfo(
                        watchFaceComponentName,
                        complicationSlotsState.keys.toIntArray()
                    )
                deferredComplicationsDataSourceInfoMap.complete(
                    extractComplicationsDataSourceInfoMap(dataSourceInfoArray)?.toMutableMap()
                        ?: mutableMapOf()
                )
                deferredComplicationPreviewDataMap.complete(
                    // Parallel fetch preview ComplicationData.
                    dataSourceInfoArray?.associateBy(
                        { it.slotId },
                        {
                            async {
                                getPreviewData(complicationDataSourceInfoRetriever, it.info)
                            }
                        }
                        // Coerce to a Map<Int, ComplicationData> omitting null values.
                        // If mapNotNullValues existed we would use it here.
                    )?.mapValues {
                        it.value.await() ?: EmptyComplicationData()
                    }?.toMutableMap() ?: mutableMapOf()
                )
            } finally {
                complicationDataSourceInfoRetriever.close()
            }
        }
    }

    override fun close() {
        // Silently do nothing if we've been force closed, this simplifies the editor activity.
        if (forceClosed) {
            return
        }
        requireNotClosed()
        EditorService.globalEditorService.removeCloseCallback(closeCallback)
        // We need to send the preview data which we obtain asynchronously.
        coroutineScope.launchWithTracing("BaseEditorSession.close") {
            try {
                withTimeout(CLOSE_BROADCAST_TIMEOUT_MILLIS) {
                    val previewImage =
                        if (commitChangesOnClose && previewScreenshotParams != null &&
                            Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1
                        ) {
                            SharedMemoryImage.ashmemWriteImageBundle(
                                renderWatchFaceToBitmap(
                                    previewScreenshotParams.renderParameters,
                                    previewScreenshotParams.calendarTimeMillis,
                                    getComplicationsPreviewData()
                                )
                            )
                        } else {
                            null
                        }
                    EditorService.globalEditorService.broadcastEditorState(
                        EditorStateWireFormat(
                            watchFaceId.id,
                            userStyle.toWireFormat(),
                            getComplicationsPreviewData().map {
                                IdAndComplicationDataWireFormat(
                                    it.key,
                                    it.value.asWireComplicationData()
                                )
                            },
                            commitChangesOnClose,
                            previewImage
                        )
                    )
                }
            } catch (e: TimeoutCancellationException) {
                // Ignore this, nothing we can do.
            }

            releaseResources()
            closed = true
            editorSessionTraceEvent.close()
            coroutineScope.cancel()
        }
    }

    @UiThread
    internal fun forceClose() {
        commitChangesOnClose = false
        closed = true
        forceClosed = true
        releaseResources()
        EditorService.globalEditorService.removeCloseCallback(closeCallback)
        editorSessionTraceEvent.close()
        coroutineScope.cancel()
        activity.finish()
    }

    protected fun requireNotClosed() {
        require(!closed or forceClosed) {
            "EditorSession method called after close()"
        }
    }

    @UiThread
    protected abstract fun releaseResources()
}

internal class OnWatchFaceEditorSessionImpl(
    activity: ComponentActivity,
    override val watchFaceComponentName: ComponentName,
    override val watchFaceId: WatchFaceId,
    private val initialEditorUserStyle: UserStyleData?,
    complicationDataSourceInfoRetrieverProvider: ComplicationDataSourceInfoRetrieverProvider,
    coroutineScope: CoroutineScope,
    private val isRFlow: Boolean,
    previewScreenshotParams: PreviewScreenshotParams?
) : BaseEditorSession(
    activity,
    complicationDataSourceInfoRetrieverProvider,
    coroutineScope,
    previewScreenshotParams
) {
    private lateinit var editorDelegate: WatchFace.EditorDelegate

    override val userStyleSchema by lazy {
        requireNotClosed()
        editorDelegate.userStyleSchema
    }

    override val previewReferenceTimeMillis by lazy { editorDelegate.previewReferenceTimeMillis }

    override val complicationSlotsState
        get() = editorDelegate.complicationSlotsManager.complicationSlots.mapValues {
            requireNotClosed()
            ComplicationSlotState(
                it.value.computeBounds(editorDelegate.screenBounds),
                it.value.boundsType,
                it.value.supportedTypes,
                it.value.defaultDataSourcePolicy,
                it.value.defaultDataSourceType,
                it.value.enabled,
                it.value.initiallyEnabled,
                it.value.renderer.getData()?.type ?: ComplicationType.NO_DATA,
                it.value.fixedComplicationDataSource,
                it.value.configExtras
            )
        }

    private var _userStyle: UserStyle? = null

    // We make a deep copy of the style because assigning to it can otherwise have unexpected
    // side effects (it would apply to the active watch face).
    override var userStyle: UserStyle
        get() {
            requireNotClosed()
            if (_userStyle == null) {
                _userStyle = UserStyle(editorDelegate.userStyle)
            }
            return _userStyle!!
        }
        set(value) {
            requireNotClosed()
            _userStyle = value
            editorDelegate.userStyle = UserStyle(value)
        }

    private lateinit var previousWatchFaceUserStyle: UserStyle
    private lateinit var backgroundCoroutineScope: CoroutineScope

    override fun renderWatchFaceToBitmap(
        renderParameters: RenderParameters,
        calendarTimeMillis: Long,
        slotIdToComplicationData: Map<Int, ComplicationData>?
    ): Bitmap {
        requireNotClosed()
        require(renderParameters.drawMode == DrawMode.INTERACTIVE) {
            "Currently only DrawMode.INTERACTIVE is supported"
        }
        return editorDelegate.renderWatchFaceToBitmap(
            renderParameters,
            calendarTimeMillis,
            slotIdToComplicationData
        )
    }

    override fun releaseResources() {
        // If commitChangesOnClose is true, the userStyle is not restored which for non-headless
        // watch faces meaning the style is applied immediately. It's possible for the System to
        // fail to persist this change and we rely on the system reverting the style change in this
        // eventuality.
        if (!commitChangesOnClose && this::previousWatchFaceUserStyle.isInitialized) {
            userStyle = previousWatchFaceUserStyle
        }

        // Note this has to be done after resetting userStyle to ensure tests are not racy.
        if (this::editorDelegate.isInitialized) {
            editorDelegate.onDestroy()
        }

        if (this::backgroundCoroutineScope.isInitialized) {
            backgroundCoroutineScope.cancel()
        }
    }

    fun setEditorDelegate(editorDelegate: WatchFace.EditorDelegate) {
        this.editorDelegate = editorDelegate

        previousWatchFaceUserStyle = UserStyle(editorDelegate.userStyle)

        // Apply any initial style from the intent.  Note we don't restore the previous style at
        // the end since we assume we're editing the current active watchface.
        if (initialEditorUserStyle != null) {
            editorDelegate.userStyle =
                UserStyle(initialEditorUserStyle, editorDelegate.userStyleSchema)
        }

        backgroundCoroutineScope = CoroutineScope(
            editorDelegate.backgroundThreadHandler.asCoroutineDispatcher().immediate
        )

        fetchComplicationsData(backgroundCoroutineScope)
    }
}

@RequiresApi(27)
internal class HeadlessEditorSession(
    activity: ComponentActivity,
    private val headlessWatchFaceClient: HeadlessWatchFaceClient,
    override val watchFaceComponentName: ComponentName,
    override val watchFaceId: WatchFaceId,
    initialUserStyle: UserStyleData,
    complicationDataSourceInfoRetrieverProvider: ComplicationDataSourceInfoRetrieverProvider,
    coroutineScope: CoroutineScope,
    previewScreenshotParams: PreviewScreenshotParams?
) : BaseEditorSession(
    activity,
    complicationDataSourceInfoRetrieverProvider,
    coroutineScope,
    previewScreenshotParams
) {
    override val userStyleSchema = headlessWatchFaceClient.userStyleSchema

    override var userStyle = UserStyle(initialUserStyle, userStyleSchema)

    override val previewReferenceTimeMillis = headlessWatchFaceClient.previewReferenceTimeMillis

    override val complicationSlotsState = headlessWatchFaceClient.complicationSlotsState

    override fun renderWatchFaceToBitmap(
        renderParameters: RenderParameters,
        calendarTimeMillis: Long,
        slotIdToComplicationData: Map<Int, ComplicationData>?
    ): Bitmap {
        requireNotClosed()
        return headlessWatchFaceClient.renderWatchFaceToBitmap(
            renderParameters,
            calendarTimeMillis,
            userStyle,
            slotIdToComplicationData
        )
    }

    override fun releaseResources() {
        headlessWatchFaceClient.close()
    }

    init {
        fetchComplicationsData(coroutineScope)
    }
}

internal class ComplicationDataSourceChooserRequest(
    internal val editorSession: EditorSession,
    internal val complicationSlotId: Int,
    internal val instanceId: String?
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
internal class ComplicationDataSourceChooserContract : ActivityResultContract<
    ComplicationDataSourceChooserRequest, ComplicationDataSourceChooserResult?>() {

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
        val intent = ComplicationHelperActivity.createComplicationDataSourceChooserHelperIntent(
            context,
            input.editorSession.watchFaceComponentName,
            input.complicationSlotId,
            input.editorSession.complicationSlotsState[input.complicationSlotId]!!.supportedTypes,
            input.instanceId
        )
        val complicationState =
            input.editorSession.complicationSlotsState[input.complicationSlotId]!!
        intent.replaceExtras(
            Bundle(complicationState.complicationConfigExtras).apply { putAll(intent.extras!!) }
        )
        if (useTestComplicationHelperActivity) {
            intent.component = ComponentName(
                "androidx.wear.watchface.editor.test",
                "androidx.wear.watchface.editor.TestComplicationHelperActivity"
            )
        }
        return intent
    }

    override fun parseResult(resultCode: Int, intent: Intent?) = intent?.let {
        val extras = intent.extras?.let {
            Bundle(it).apply { remove(EXTRA_PROVIDER_INFO) }
        } ?: Bundle.EMPTY
        ComplicationDataSourceChooserResult(
            it.getParcelableExtra<android.support.wearable.complications.ComplicationProviderInfo>(
                EXTRA_PROVIDER_INFO
            )?.toApiComplicationDataSourceInfo(),
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
): Map<Int, ComplicationDataSourceInfo?>? =
    resultArray?.associateBy(
        { it.slotId },
        { it.info }
    )

internal fun Bundle.asString() = keySet().map { "$it: ${get(it)}" }
