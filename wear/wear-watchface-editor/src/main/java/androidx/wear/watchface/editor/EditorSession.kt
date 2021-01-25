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
import androidx.concurrent.futures.ResolvableFuture
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
import androidx.wear.watchface.style.UserStyle
import androidx.wear.watchface.style.UserStyleSchema
import com.google.common.util.concurrent.ListenableFuture
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executor

/**
 * Interface for manipulating watch face state during an editing session for a watch face editing
 * session. The editor should adjust [userStyle] and call [launchComplicationProviderChooser] to
 * configure the watch face and call [Activity.setWatchRequestResult] to record the result.
 */
public interface EditorSession {
    /** The [ComponentName] of the watch face being edited. */
    public val watchFaceComponentName: ComponentName

    /**
     * The instance id of the watch face being edited. Note each distinct [ComponentName] can have
     * multiple instances.
     */
    public val instanceId: String

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
     * [ListenableFuture] for a map of complication ids to preview [ComplicationData] suitable for
     * use in rendering the watch face. Note if a slot is configured to be empty then it will not
     * appear in the map, however disabled complications are included. Note also unlike live data
     * this is static per provider, but it may change (on the UIThread) as a result of
     * [launchComplicationProviderChooser].
     */
    public val complicationPreviewData: ListenableFuture<Map<Int, ComplicationData>>

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
     * Launches the complication provider chooser and returns a [ListenableFuture] which
     * resolves with `true` if the user made a selection or `false` if the activity was canceled.
     */
    @UiThread
    public fun launchComplicationProviderChooser(complicationId: Int): ListenableFuture<Boolean>

    public companion object {
        /** Constructs an [EditorSession] for an on watch face editor. */
        @SuppressWarnings("ExecutorRegistration")
        @JvmStatic
        @UiThread
        public fun createOnWatchEditingSession(
            /** The [ComponentActivity] associated with the EditorSession. */
            activity: ComponentActivity,

            /** [Intent] sent by SysUI to launch the editing session. */
            editIntent: Intent
        ): EditorSession? =
            EditorRequest.createFromIntent(editIntent)?.let { editorRequest ->
                WatchFace.getEditorDelegate(editorRequest.watchFaceComponentName)?.let {
                    OnWatchFaceEditorSessionImpl(
                        activity,
                        editorRequest.watchFaceComponentName,
                        editorRequest.watchFaceInstanceId,
                        editorRequest.initialUserStyle,
                        it
                    )
                }
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
                    it.initialUserStyle!!
                )
            }
    }
}

internal abstract class BaseEditorSession(
    protected val activity: ComponentActivity,
) : EditorSession {

    // NB this map is only modified on the UI thread.
    private val complicationPreviewDataInternal = HashMap<Int, ComplicationData>()

    // This future is resolved when [fetchComplicationPreviewData] has called [getPreviewData] for
    // each complication and each of those future has been resolved.
    private val complicationPreviewDataFuture =
        ResolvableFuture.create<Map<Int, ComplicationData>>()

    override val complicationPreviewData: ResolvableFuture<Map<Int, ComplicationData>> =
        complicationPreviewDataFuture

    // The future returned by [launchComplicationProviderChooser].
    private var pendingFuture: ResolvableFuture<Boolean>? = null

    // The id of the complication being configured due to [launchComplicationProviderChooser].
    private var pendingComplicationProviderId: Int = -1

    private val mainThreadExecutor: Executor = object : Executor {
        private val handler: Handler = Handler(Looper.getMainLooper())
        override fun execute(command: Runnable) {
            if (handler.looper !== Looper.myLooper()) {
                handler.post(command)
            } else {
                command.run()
            }
        }
    }

    private val chooseComplicationProvider =
        activity.registerForActivityResult(ComplicationProviderChooserContract()) {
            if (it != null) {
                // Update preview data and then resolve [pendingFuture].
                val providerInfoRetriever = ProviderInfoRetriever(activity)
                val previewDataListener = getPreviewData(
                    providerInfoRetriever,
                    it.providerInfo
                )
                previewDataListener.addListener(
                    {
                        val previewData = previewDataListener.get()
                        if (previewData == null) {
                            complicationPreviewDataInternal.remove(pendingComplicationProviderId)
                        } else {
                            complicationPreviewDataInternal[pendingComplicationProviderId] =
                                previewData
                        }
                        providerInfoRetriever.close()
                        pendingFuture!!.set(true)
                    },
                    mainThreadExecutor
                )
            } else {
                pendingFuture!!.set(false)
            }
            pendingFuture = null
        }

    override fun launchComplicationProviderChooser(complicationId: Int): ListenableFuture<Boolean> {
        val future = ResolvableFuture.create<Boolean>()
        pendingFuture = future
        pendingComplicationProviderId = complicationId
        chooseComplicationProvider.launch(
            ComplicationProviderChooserRequest(this, complicationId)
        )
        return future
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
     * Returns a future that resolves with the provider's preview [ComplicationData] if possible or
     * fallback preview data based on provider icon and name if not. If the slot is configured to be
     * empty then the future will resolve to `null`.
     *
     * Note providerInfoRetriever.requestPreviewComplicationData which requires R will never be
     * called pre R because providerInfo.providerComponentName is only non null from R onwards.
     */
    @SuppressLint("NewApi")
    internal fun getPreviewData(
        providerInfoRetriever: ProviderInfoRetriever,
        providerInfo: ComplicationProviderInfo?
    ): ListenableFuture<ComplicationData?> {
        val resultFuture = ResolvableFuture.create<ComplicationData>()
        if (providerInfo == null) {
            return resultFuture.apply {
                set(null)
            }
        }

        val providerComponentName = providerInfo.providerComponentName
        if (providerComponentName != null) {
            val future = providerInfoRetriever.requestPreviewComplicationData(
                providerComponentName,
                ComplicationType.fromWireType(providerInfo.complicationType)
            )
            future.addListener(
                {
                    try {
                        resultFuture.set(future.get())
                    } catch (e: ExecutionException) {
                        resultFuture.set(
                            if (e.cause is ProviderInfoRetriever.PreviewNotAvailableException) {
                                // Generate fallback preview data.
                                generatePreviewFromComplicationProviderInfo(providerInfo)
                            } else {
                                null
                            }
                        )
                    }
                },
                { runnable -> runnable.run() }
            )
        } else {
            // Generate fallback preview data.
            resultFuture.set(generatePreviewFromComplicationProviderInfo(providerInfo))
        }
        return resultFuture
    }

    private fun generatePreviewFromComplicationProviderInfo(
        providerInfo: ComplicationProviderInfo
    ): ComplicationData? {
        val providerIcon = providerInfo.providerIcon
        val providerName = providerInfo.providerName

        return when {
            providerName == null -> null

            providerIcon == null ->
                LongTextComplicationData.Builder(ComplicationText.plain(providerName))
                    .build()

            else ->
                ShortTextComplicationData.Builder(ComplicationText.plain(providerName))
                    .setImage(
                        MonochromaticImage.Builder(providerIcon).build()
                    )
                    .build()
        }
    }

    protected fun fetchComplicationPreviewData() {
        val providerInfoRetriever = ProviderInfoRetriever(activity)
        val providerInfoFuture = providerInfoRetriever.retrieveProviderInfo(
            watchFaceComponentName,
            complicationState.keys.toIntArray()
        )
        providerInfoFuture.addListener(
            {
                providerInfoFuture.get()?.let {
                    // We can use a regular int here because we use [mainThreadExecutor].
                    var countDown = it.size
                    for (providerInfo in it) {
                        val previewDataListener = getPreviewData(
                            providerInfoRetriever,
                            providerInfo.info
                        )
                        previewDataListener.addListener(
                            {
                                previewDataListener.get()?.let { previewData ->
                                    complicationPreviewDataInternal[
                                        providerInfo.watchFaceComplicationId
                                    ] = previewData
                                }
                                // If we've generated preview data for all the complications we can
                                // resolve the future.
                                if (--countDown == 0) {
                                    complicationPreviewDataFuture.set(
                                        complicationPreviewDataInternal
                                    )
                                }
                            },
                            mainThreadExecutor
                        )
                    }
                }
                providerInfoRetriever.close()
            },
            { runnable -> runnable.run() }
        )
    }
}

internal class OnWatchFaceEditorSessionImpl(
    activity: ComponentActivity,
    override val watchFaceComponentName: ComponentName,
    override val instanceId: String,
    initialEditorUserStyle: Map<String, String>?,
    private val editorDelegate: WatchFace.EditorDelegate
) : BaseEditorSession(activity) {
    override val userStyleSchema = editorDelegate.userStyleRepository.schema

    override val previewReferenceTimeMillis = editorDelegate.previewReferenceTimeMillis

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
                    ?: ComplicationType.NO_DATA
            )
        }

    // We make a deep copy of the style because assigning to it can otherwise have unexpected
    // side effects (it would apply to the active watch face).
    override var userStyle = UserStyle(editorDelegate.userStyleRepository.userStyle)
        set(value) {
            field = value
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

    init {
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
    override val instanceId: String,
    initialUserStyle: Map<String, String>
) : BaseEditorSession(activity) {
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
public fun Activity.setWatchRequestResult(editorSession: EditorSession) {
    setResult(
        Activity.RESULT_OK,
        Intent().apply {
            putExtra(
                EditorResult.USER_STYLE_KEY,
                ParcelUtils.toParcelable(editorSession.userStyle.toWireFormat())
            )
        }
    )
}
