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
import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import android.support.wearable.complications.ComplicationProviderInfo
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContract
import androidx.annotation.Px
import androidx.annotation.RequiresApi
import androidx.annotation.UiThread
import androidx.versionedparcelable.ParcelUtils
import androidx.wear.complications.ComplicationHelperActivity
import androidx.wear.complications.ProviderInfoRetriever
import androidx.wear.complications.data.ComplicationData
import androidx.wear.complications.data.ComplicationText
import androidx.wear.complications.data.ComplicationType
import androidx.wear.complications.data.LongTextComplicationData
import androidx.wear.complications.data.MonochromaticImage
import androidx.wear.complications.data.ShortTextComplicationData
import androidx.wear.watchface.RenderParameters
import androidx.wear.watchface.WatchFace
import androidx.wear.watchface.client.ComplicationState
import androidx.wear.watchface.client.HeadlessWatchFaceClient
import androidx.wear.watchface.data.ComplicationBoundsType
import androidx.wear.watchface.data.IdAndComplicationDataWireFormat
import androidx.wear.watchface.style.UserStyle
import androidx.wear.watchface.style.UserStyleSchema
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.android.asCoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

/**
 * Interface for manipulating watch face state during an editing session for a watch face editing
 * session. The editor should adjust [userStyle] and call [launchComplicationProviderChooser] to
 * configure the watch face and call [Activity.setWatchRequestResult] to record the result.
 */
public interface EditorSession {
    /** The [ComponentName] of the watch face being edited. */
    public val watchFaceComponentName: ComponentName

    /**
     * Unique ID for the instance of the watch face being edited, only defined for Android R and
     * beyond, it's `null` on Android P and earlier. Note each distinct [ComponentName] can have
     * multiple instances.
     */
    public val instanceId: String?

    /** The current [UserStyle]. Assigning to this will cause the style to update. */
    public var userStyle: UserStyle

    /** The UTC reference preview time for this watch face in milliseconds since the epoch. */
    public val previewReferenceTimeMillis: Long

    /** The watch face's [UserStyleSchema]. */
    public val userStyleSchema: UserStyleSchema

    /**
     * Map of complication ids to [ComplicationState] for each complication slot. Note
     * [ComplicationState] can change, typically in response to styling.
     */
    public val complicationState: Map<Int, ComplicationState>

    /**
     * Returns a map of complication ids to preview [ComplicationData] suitable for use in rendering
     * the watch face. Note if a slot is configured to be empty then it will not appear in the map,
     * however disabled complications are included. Note also unlike live data this is static per
     * provider, but it may change (on the UIThread) as a result of
     * [launchComplicationProviderChooser].
     */
    public suspend fun getComplicationPreviewData(): Map<Int, ComplicationData>

    /** The ID of the background complication or `null` if there isn't one. */
    @get:SuppressWarnings("AutoBoxing")
    public val backgroundComplicationId: Int?

    /** Returns the ID of the complication at the given coordinates or `null` if there isn't one. */
    @SuppressWarnings("AutoBoxing")
    @UiThread
    public fun getComplicationIdAt(@Px x: Int, @Px y: Int): Int?

    /**
     * Takes a screen shot of the watch face using the current [userStyle].
     *
     * @param renderParameters The [RenderParameters] to render with
     * @param calendarTimeMillis The UTC time in milliseconds since the epoch to render with
     * @param idToComplicationData The [ComplicationData] for each complication to render with
     */
    @UiThread
    public fun takeWatchFaceScreenshot(
        renderParameters: RenderParameters,
        calendarTimeMillis: Long,
        idToComplicationData: Map<Int, ComplicationData>?
    ): Bitmap

    /**
     * Launches the complication provider chooser and returns `true` if the user made a selection or
     * `false` if the activity was canceled.
     */
    @UiThread
    public suspend fun launchComplicationProviderChooser(complicationId: Int): Boolean

    /** Should be called when the activity is going away so we can release resources. */
    public fun onDestroy()

    public companion object {
        /**
         * Constructs an [EditorSession] for an on watch face editor. This registers an activity
         * result handler and so it must be called during an Activity or Fragment initialization
         * path.
         */
        @SuppressWarnings("ExecutorRegistration")
        @JvmStatic
        @UiThread
        public fun createOnWatchEditingSessionAsync(
            /** The [ComponentActivity] associated with the EditorSession. */
            activity: ComponentActivity,

            /** [Intent] sent by SysUI to launch the editing session. */
            editIntent: Intent
        ): Deferred<EditorSession?> = createOnWatchEditingSessionAsyncImpl(
            activity,
            editIntent,
            object : ProviderInfoRetrieverProvider {
                override fun getProviderInfoRetriever() = ProviderInfoRetriever(activity)
            }
        )

        // Used by tests.
        internal fun createOnWatchEditingSessionAsyncImpl(
            activity: ComponentActivity,
            editIntent: Intent,
            providerInfoRetrieverProvider: ProviderInfoRetrieverProvider
        ): Deferred<EditorSession?> {
            val coroutineScope =
                CoroutineScope(Handler(Looper.getMainLooper()).asCoroutineDispatcher())
            return EditorRequest.createFromIntent(editIntent)?.let { editorRequest ->
                // We need to respect the lifecycle and register the ActivityResultListener now.
                val session = OnWatchFaceEditorSessionImpl(
                    activity,
                    editorRequest.watchFaceComponentName,
                    editorRequest.watchFaceInstanceId,
                    editorRequest.initialUserStyle,
                    providerInfoRetrieverProvider,
                    coroutineScope
                )

                // But full initialization has to be deferred because
                // [WatchFace.getOrCreateEditorDelegate] is async.
                coroutineScope.async {
                    session.setEditorDelegate(
                        WatchFace.getOrCreateEditorDelegate(
                            editorRequest.watchFaceComponentName
                        ).await()!!
                    )

                    // Resolve the Deferred<EditorSession?> only after init has been completed.
                    session
                }
            } ?: CompletableDeferred(null)
        }

        /** Constructs an [EditorSession] for a remote watch face editor. */
        @JvmStatic
        @RequiresApi(27)
        @UiThread
        public fun createHeadlessEditingSession(
            /** The [ComponentActivity] associated with the EditorSession. */
            activity: ComponentActivity,

            /** [Intent] sent by SysUI to launch the editing session. */
            editIntent: Intent,

            headlessWatchFaceClient: HeadlessWatchFaceClient
        ): EditorSession? =
            EditorRequest.createFromIntent(editIntent)?.let {
                HeadlessEditorSession(
                    activity,
                    headlessWatchFaceClient,
                    it.watchFaceComponentName,
                    it.watchFaceInstanceId,
                    it.initialUserStyle!!,
                    object : ProviderInfoRetrieverProvider {
                        override fun getProviderInfoRetriever() = ProviderInfoRetriever(activity)
                    },
                    CoroutineScope(Handler(Looper.getMainLooper()).asCoroutineDispatcher())
                )
            }
    }
}

// Helps inject mock ProviderInfoRetrievers for testing.
internal interface ProviderInfoRetrieverProvider {
    fun getProviderInfoRetriever(): ProviderInfoRetriever
}

internal abstract class BaseEditorSession(
    protected val activity: ComponentActivity,
    protected val providerInfoRetrieverProvider: ProviderInfoRetrieverProvider,
    internal val coroutineScope: CoroutineScope
) : EditorSession {
    // This is completed when [fetchComplicationPreviewData] has called [getPreviewData] for
    // each complication and each of those have been completed.
    private val deferredComplicationPreviewDataMap =
        CompletableDeferred<MutableMap<Int, ComplicationData>>()

    override suspend fun getComplicationPreviewData(): Map<Int, ComplicationData> {
        return deferredComplicationPreviewDataMap.await()
    }

    // Pending result for [launchComplicationProviderChooser].
    private var pendingComplicationProviderChooserResult: CompletableDeferred<Boolean>? = null

    // The id of the complication being configured due to [launchComplicationProviderChooser].
    private var pendingComplicationProviderId: Int = -1

    private val chooseComplicationProvider =
        activity.registerForActivityResult(ComplicationProviderChooserContract()) {
            if (it != null) {
                coroutineScope.launch {
                    // Update preview data.
                    val providerInfoRetriever =
                        providerInfoRetrieverProvider.getProviderInfoRetriever()
                    val previewData = getPreviewData(providerInfoRetriever, it.providerInfo)
                    val complicationPreviewDataMap = deferredComplicationPreviewDataMap.await()
                    if (previewData == null) {
                        complicationPreviewDataMap.remove(pendingComplicationProviderId)
                    } else {
                        complicationPreviewDataMap[pendingComplicationProviderId] = previewData
                    }
                    providerInfoRetriever.close()
                    pendingComplicationProviderChooserResult!!.complete(true)
                    pendingComplicationProviderChooserResult = null
                }
            } else {
                pendingComplicationProviderChooserResult!!.complete(false)
                pendingComplicationProviderChooserResult = null
            }
        }

    override suspend fun launchComplicationProviderChooser(complicationId: Int): Boolean {
        require(!complicationState[complicationId]!!.fixedComplicationProvider) {
            "Can't configure fixed complication ID $complicationId"
        }
        pendingComplicationProviderChooserResult = CompletableDeferred<Boolean>()
        pendingComplicationProviderId = complicationId
        chooseComplicationProvider.launch(
            ComplicationProviderChooserRequest(this, complicationId)
        )
        return pendingComplicationProviderChooserResult!!.await()
    }

    override val backgroundComplicationId by lazy {
        complicationState.entries.firstOrNull {
            it.value.boundsType == ComplicationBoundsType.BACKGROUND
        }?.key
    }

    override fun getComplicationIdAt(@Px x: Int, @Px y: Int): Int? =
        complicationState.entries.firstOrNull {
            it.value.isEnabled && when (it.value.boundsType) {
                ComplicationBoundsType.ROUND_RECT -> it.value.bounds.contains(x, y)
                ComplicationBoundsType.BACKGROUND -> false
                ComplicationBoundsType.EDGE -> false
                else -> false
            }
        }?.key

    /**
     * Returns the provider's preview [ComplicationData] if possible or fallback preview data based
     * on provider icon and name if not. If the slot is configured to be empty then it will return
     * `null`.
     *
     * Note providerInfoRetriever.requestPreviewComplicationData which requires R will never be
     * called pre R because providerInfo.providerComponentName is only non null from R onwards.
     */
    @SuppressLint("NewApi")
    internal suspend fun getPreviewData(
        providerInfoRetriever: ProviderInfoRetriever,
        providerInfo: ComplicationProviderInfo?
    ): ComplicationData? {
        if (providerInfo == null) {
            return null
        }
        // Fetch preview ComplicationData if possible.
        return providerInfo.providerComponentName?.let {
            providerInfoRetriever.requestPreviewComplicationData(
                it,
                ComplicationType.fromWireType(providerInfo.complicationType)
            )
        } ?: makeFallbackPreviewData(providerInfo)
    }

    private fun makeFallbackPreviewData(
        providerInfo: ComplicationProviderInfo
    ) = when {
        providerInfo.providerName == null -> null

        providerInfo.providerIcon == null ->
            LongTextComplicationData.Builder(
                ComplicationText.plain(providerInfo.providerName!!)
            ).build()

        else ->
            ShortTextComplicationData.Builder(
                ComplicationText.plain(providerInfo.providerName!!)
            ).setMonochromaticImage(
                MonochromaticImage.Builder(providerInfo.providerIcon!!).build()
            ).build()
    }

    protected fun fetchComplicationPreviewData() {
        coroutineScope.launch {
            val providerInfoRetriever = providerInfoRetrieverProvider.getProviderInfoRetriever()
            val providerInfoArray = providerInfoRetriever.retrieveProviderInfo(
                watchFaceComponentName,
                complicationState.keys.toIntArray()
            )
            deferredComplicationPreviewDataMap.complete(
                // Parallel fetch preview ComplicationData.
                providerInfoArray?.associateBy(
                    { it.watchFaceComplicationId },
                    { async { getPreviewData(providerInfoRetriever, it.info) } }
                    // Coerce to a Map<Int, ComplicationData> omitting null values.
                    // If mapNotNullValues existed we would use it here.
                )?.filterValues {
                    it.await() != null
                }?.mapValues {
                    it.value.await()!!
                }?.toMutableMap() ?: mutableMapOf()
            )
            providerInfoRetriever.close()
        }
    }
}

internal class OnWatchFaceEditorSessionImpl(
    activity: ComponentActivity,
    override val watchFaceComponentName: ComponentName,
    override val instanceId: String?,
    private val initialEditorUserStyle: Map<String, String>?,
    providerInfoRetrieverProvider: ProviderInfoRetrieverProvider,
    coroutineScope: CoroutineScope
) : BaseEditorSession(activity, providerInfoRetrieverProvider, coroutineScope) {
    private lateinit var editorDelegate: WatchFace.EditorDelegate

    override val userStyleSchema by lazy {
        editorDelegate.userStyleRepository.schema
    }

    override val previewReferenceTimeMillis by lazy { editorDelegate.previewReferenceTimeMillis }

    override val complicationState
        get() = editorDelegate.complicationsManager.complications.mapValues {
            ComplicationState(
                it.value.computeBounds(editorDelegate.screenBounds),
                it.value.boundsType,
                it.value.supportedTypes,
                it.value.defaultProviderPolicy,
                it.value.defaultProviderType,
                it.value.enabled,
                it.value.renderer.idAndData?.complicationData?.type
                    ?: ComplicationType.NO_DATA,
                it.value.fixedComplicationProvider
            )
        }

    private var _userStyle: UserStyle? = null

    // We make a deep copy of the style because assigning to it can otherwise have unexpected
    // side effects (it would apply to the active watch face).
    override var userStyle: UserStyle
        get() {
            if (_userStyle == null) {
                _userStyle = UserStyle(editorDelegate.userStyleRepository.userStyle)
            }
            return _userStyle!!
        }
        set(value) {
            _userStyle = value
            editorDelegate.userStyleRepository.userStyle = UserStyle(value)
        }

    override fun takeWatchFaceScreenshot(
        renderParameters: RenderParameters,
        calendarTimeMillis: Long,
        idToComplicationData: Map<Int, ComplicationData>?
    ) = editorDelegate.takeScreenshot(
        renderParameters,
        calendarTimeMillis,
        idToComplicationData
    )

    override fun onDestroy() {
        editorDelegate.onDestroy()
    }

    fun setEditorDelegate(editorDelegate: WatchFace.EditorDelegate) {
        this.editorDelegate = editorDelegate

        // Apply any initial style from the intent.  Note we don't restore the previous style at
        // the end since we assume we're editing the current active watchface.
        if (initialEditorUserStyle != null) {
            editorDelegate.userStyleRepository.userStyle =
                UserStyle(initialEditorUserStyle, editorDelegate.userStyleRepository.schema)
        }

        fetchComplicationPreviewData()
    }
}

@RequiresApi(27)
internal class HeadlessEditorSession(
    activity: ComponentActivity,
    private val headlessWatchFaceClient: HeadlessWatchFaceClient,
    override val watchFaceComponentName: ComponentName,
    override val instanceId: String?,
    initialUserStyle: Map<String, String>,
    providerInfoRetrieverProvider: ProviderInfoRetrieverProvider,
    coroutineScope: CoroutineScope
) : BaseEditorSession(activity, providerInfoRetrieverProvider, coroutineScope) {
    override val userStyleSchema = headlessWatchFaceClient.userStyleSchema

    override var userStyle = UserStyle(initialUserStyle, userStyleSchema)

    override val previewReferenceTimeMillis = headlessWatchFaceClient.previewReferenceTimeMillis

    override val complicationState = headlessWatchFaceClient.complicationState

    override fun takeWatchFaceScreenshot(
        renderParameters: RenderParameters,
        calendarTimeMillis: Long,
        idToComplicationData: Map<Int, ComplicationData>?
    ) = headlessWatchFaceClient.takeWatchFaceScreenshot(
        renderParameters,
        100,
        calendarTimeMillis,
        userStyle,
        idToComplicationData
    )

    override fun onDestroy() {
        headlessWatchFaceClient.close()
    }

    init {
        fetchComplicationPreviewData()
    }
}

internal class ComplicationProviderChooserRequest(
    internal val editorSession: EditorSession,
    internal val complicationId: Int
)

internal class ComplicationProviderChooserResult(
    /** The updated [ComplicationProviderInfo] or `null` if the operation was canceled. */
    internal val providerInfo: ComplicationProviderInfo?
)

/** An [ActivityResultContract] for invoking the complication provider chooser. */
internal class ComplicationProviderChooserContract : ActivityResultContract<
    ComplicationProviderChooserRequest, ComplicationProviderChooserResult>() {

    internal companion object {
        const val EXTRA_PROVIDER_INFO = "android.support.wearable.complications.EXTRA_PROVIDER_INFO"
    }

    override fun createIntent(context: Context, input: ComplicationProviderChooserRequest): Intent =
        ComplicationHelperActivity.createProviderChooserHelperIntent(
            context,
            input.editorSession.watchFaceComponentName,
            input.complicationId,
            input.editorSession.complicationState[input.complicationId]!!.supportedTypes
        )

    override fun parseResult(resultCode: Int, intent: Intent?): ComplicationProviderChooserResult {
        return ComplicationProviderChooserResult(intent?.getParcelableExtra(EXTRA_PROVIDER_INFO))
    }
}

/** Sets the [Activity]s result with [EditorResult]. */
public suspend fun Activity.setWatchRequestResult(editorSession: EditorSession) {
    setResult(
        Activity.RESULT_OK,
        Intent().apply {
            putExtra(
                USER_STYLE_KEY,
                ParcelUtils.toParcelable(editorSession.userStyle.toWireFormat())
            )
            putExtra(
                PREVIEW_COMPLICATIONS_KEY,
                editorSession.getComplicationPreviewData().map {
                    ParcelUtils.toParcelable(
                        IdAndComplicationDataWireFormat(
                            it.key,
                            it.value.asWireComplicationData()
                        )
                    )
                }.toTypedArray()
            )
            putExtra(INSTANCE_ID_KEY, editorSession.instanceId)
        }
    )
}
