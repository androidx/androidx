/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.wear.watchface

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Rect
import android.icu.util.Calendar
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.os.PowerManager
import android.os.RemoteException
import android.os.Trace
import android.service.wallpaper.WallpaperService
import android.support.wearable.watchface.Constants
import android.support.wearable.watchface.IWatchFaceService
import android.support.wearable.watchface.accessibility.AccessibilityUtils
import android.support.wearable.watchface.accessibility.ContentDescriptionLabel
import android.util.Base64
import android.util.Log
import android.view.Choreographer
import android.view.Surface
import android.view.SurfaceHolder
import android.view.WindowInsets
import android.view.accessibility.AccessibilityManager
import androidx.annotation.IntDef
import androidx.annotation.Px
import androidx.annotation.RequiresApi
import androidx.annotation.RestrictTo
import androidx.annotation.UiThread
import androidx.annotation.WorkerThread
import androidx.versionedparcelable.ParcelUtils
import androidx.wear.complications.SystemDataSources.DataSourceId
import androidx.wear.complications.data.ComplicationData
import androidx.wear.complications.data.toApiComplicationData
import androidx.wear.complications.data.toWireTypes
import androidx.wear.utility.AsyncTraceEvent
import androidx.wear.utility.TraceEvent
import androidx.wear.watchface.control.HeadlessWatchFaceImpl
import androidx.wear.watchface.control.IInteractiveWatchFace
import androidx.wear.watchface.control.InteractiveInstanceManager
import androidx.wear.watchface.control.InteractiveWatchFaceImpl
import androidx.wear.watchface.control.data.CrashInfoParcel
import androidx.wear.watchface.control.data.HeadlessWatchFaceInstanceParams
import androidx.wear.watchface.control.data.IdTypeAndDefaultProviderPolicyWireFormat
import androidx.wear.watchface.control.data.WallpaperInteractiveWatchFaceInstanceParams
import androidx.wear.watchface.data.ComplicationSlotBoundsType
import androidx.wear.watchface.data.ComplicationSlotMetadataWireFormat
import androidx.wear.watchface.data.DeviceConfig
import androidx.wear.watchface.data.IdAndComplicationDataWireFormat
import androidx.wear.watchface.data.WatchUiState
import androidx.wear.watchface.editor.EditorService
import androidx.wear.watchface.style.CurrentUserStyleRepository
import androidx.wear.watchface.style.UserStyle
import androidx.wear.watchface.style.UserStyleData
import androidx.wear.watchface.style.UserStyleSchema
import androidx.wear.watchface.style.UserStyleSetting
import androidx.wear.watchface.style.data.UserStyleWireFormat
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.android.asCoroutineDispatcher
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.FileDescriptor
import java.io.FileNotFoundException
import java.io.InputStreamReader
import java.io.PrintWriter
import java.util.concurrent.CountDownLatch

/** The wire format for [ComplicationData]. */
internal typealias WireComplicationData = android.support.wearable.complications.ComplicationData

/**
 * After user code finishes, we need up to 100ms of wake lock holding for the drawing to occur. This
 * isn't the ideal approach, but the framework doesn't expose a callback that would tell us when our
 * Canvas was drawn. 100 ms should give us time for a few frames to be drawn, in case there is a
 * backlog. If we encounter issues with this approach, we should consider asking framework team to
 * expose a callback.
 */
internal const val SURFACE_DRAW_TIMEOUT_MS = 100L

/** @hide */
@IntDef(
    value = [
        TapType.DOWN,
        TapType.UP,
        TapType.CANCEL
    ]
)
public annotation class TapType {
    public companion object {
        /**
         * Used to indicate a "down" touch event on the watch face.
         *
         * The watch face will receive an [UP] or a [CANCEL] event to follow this event, to
         * indicate whether this down event corresponds to a tap gesture to be handled by the watch
         * face, or a different type of gesture that is handled by the system, respectively.
         */
        public const val DOWN: Int = IInteractiveWatchFace.TAP_TYPE_DOWN

        /**
         * Used in to indicate that a previous [TapType.DOWN] touch event has been canceled. This
         * generally happens when the watch face is touched but then a move or long press occurs.
         *
         * The watch face should not trigger any action, as the system is already processing the
         * gesture.
         */
        public const val CANCEL: Int = IInteractiveWatchFace.TAP_TYPE_CANCEL

        /**
         * Used to indicate that an "up" event on the watch face has occurred that has not been
         * consumed by the system. A [TapType.DOWN] will always occur first. This event will not
         * be sent if a [TapType.CANCEL] is sent.
         *
         * Therefore, a [TapType.DOWN] event and the successive [TapType.UP] event are guaranteed
         * to be close enough to be considered a tap according to the value returned by
         * [android.view.ViewConfiguration.getScaledTouchSlop].
         */
        public const val UP: Int = IInteractiveWatchFace.TAP_TYPE_UP
    }
}

/**
 * WatchFaceService and [WatchFace] are a pair of classes intended to handle much of
 * the boilerplate needed to implement a watch face without being too opinionated. The suggested
 * structure of a WatchFaceService based watch face is:
 *
 * @sample androidx.wear.watchface.samples.kDocCreateExampleWatchFaceService
 *
 * Sub classes of WatchFaceService are expected to implement [createWatchFace] which is the
 * factory for making [WatchFace]s. If the watch faces uses complications then
 * [createComplicationSlotsManager] should be overridden. All [ComplicationSlot]s are assumed to be
 * enumerated up upfront and passed as a collection into [ComplicationSlotsManager]'s constructor
 * which is returned by [createComplicationSlotsManager].
 *
 * Watch face styling (color and visual look of watch face elements such as numeric fonts, watch
 * hands and ticks, etc...) and companion editing is directly supported via [UserStyleSchema] and
 * [UserStyleSetting]. To enable support for styling override [createUserStyleSchema].
 *
 * WatchFaces are initially constructed on a background thread before being used exclusively on
 * the ui thread afterwards. There is a memory barrier between construction and rendering so no
 * special threading primitives are required.
 *
 * To aid debugging watch face animations, WatchFaceService allows you to speed up or slow down
 * time, and to loop between two instants.  This is controlled by MOCK_TIME_INTENT intents
 * with a float extra called "androidx.wear.watchface.extra.MOCK_TIME_SPEED_MULTIPLIE" and to long
 * extras called "androidx.wear.watchface.extra.MOCK_TIME_WRAPPING_MIN_TIME" and
 * "androidx.wear.watchface.extra.MOCK_TIME_WRAPPING_MAX_TIME" (which are UTC time in milliseconds).
 * If minTime is omitted or set to -1 then the current time is sampled as minTime.
 *
 * E.g, to make time go twice as fast:
 *  adb shell am broadcast -a androidx.wear.watchface.MockTime \
 *            --ef androidx.wear.watchface.extra.MOCK_TIME_SPEED_MULTIPLIER 2.0
 *
 *
 * To use the default watch face configuration UI add the following into your watch face's
 * AndroidManifest.xml:
 *
 * ```
 * <activity
 *   android:name="androidx.wear.watchface.ui.WatchFaceConfigActivity"
 *   android:exported="true"
 *   android:directBootAware="true"
 *   android:label="Config"
 *   android:theme="@android:style/Theme.Translucent.NoTitleBar">
 *   <intent-filter>
 *     <action android:name="com.google.android.clockwork.watchfaces.complication.CONFIG_DIGITAL" />
 *       <category android:name=
 *            "com.google.android.wearable.watchface.category.WEARABLE_CONFIGURATION" />
 *       <category android:name="android.intent.category.DEFAULT" />
 *    </intent-filter>
 * </activity>
 * ```
 *
 * To register a WatchFaceService with the system add a <service> tag to the <application> in your
 * watch face's AndroidManifest.xml:
 *
 * ```
 *  <service
 *    android:name=".MyWatchFaceServiceClass"
 *    android:exported="true"
 *    android:label="@string/watch_face_name"
 *    android:permission="android.permission.BIND_WALLPAPER">
 *    <intent-filter>
 *      <action android:name="android.service.wallpaper.WallpaperService" />
 *      <category android:name="com.google.android.wearable.watchface.category.WATCH_FACE" />
 *    </intent-filter>
 *    <meta-data
 *       android:name="com.google.android.wearable.watchface.preview"
 *       android:resource="@drawable/my_watch_preview" />
 *    <meta-data
 *      android:name="com.google.android.wearable.watchface.preview_circular"
 *      android:resource="@drawable/my_watch_circular_preview" />
 *    <meta-data
 *      android:name="com.google.android.wearable.watchface.wearableConfigurationAction"
 *      android:value="androidx.wear.watchface.editor.action.WATCH_FACE_EDITOR"/>
 *    <meta-data
 *      android:name="android.service.wallpaper"
 *      android:resource="@xml/watch_face" />
 *  </service>
 * ```
 *
 * Multiple watch faces can be defined in the same package, requiring multiple <service> tags.
 */
public abstract class WatchFaceService : WallpaperService() {

    public companion object {
        private const val TAG = "WatchFaceService"

        /** Whether to log every frame. */
        private const val LOG_VERBOSE = false

        /**
         * Whether to enable tracing for each call to [WatchFaceImpl.onDraw()] and
         * [WatchFaceImpl.onSurfaceRedrawNeeded()]
         */
        private const val TRACE_DRAW = false

        // Reference time for editor screenshots for analog watch faces.
        // 2020/10/10 at 09:30 Note the date doesn't matter, only the hour.
        private const val ANALOG_WATCHFACE_REFERENCE_TIME_MS = 1602318600000L

        // Reference time for editor screenshots for digital watch faces.
        // 2020/10/10 at 10:10 Note the date doesn't matter, only the hour.
        private const val DIGITAL_WATCHFACE_REFERENCE_TIME_MS = 1602321000000L

        // Filename for persisted preferences to be used in a direct boot scenario.
        private const val DIRECT_BOOT_PREFS = "directboot.prefs"

        // The index of the watch element in the content description labels. Usually it will be
        // first.
        private const val WATCH_ELEMENT_ACCESSIBILITY_TRAVERSAL_INDEX = -1

        /** The maximum permitted duration of [WatchFaceService.createWatchFace]. */
        public const val MAX_CREATE_WATCHFACE_TIME_MILLIS: Int = 5000
    }

    /**
     * Override this factory method to create a non-empty [UserStyleSchema]. A
     * [CurrentUserStyleRepository] constructed with this schema will be passed to
     * [createComplicationSlotsManager] and [createWatchFace]. This is called on a background thread.
     *
     * @return The [UserStyleSchema] to create a [CurrentUserStyleRepository] with, which is passed
     * to [createComplicationSlotsManager] and [createWatchFace].
     */
    @WorkerThread
    protected open fun createUserStyleSchema(): UserStyleSchema = UserStyleSchema(emptyList())

    /**
     * Override this factory method to create a non-empty [ComplicationSlotsManager]. This manager
     * will be passed to [createWatchFace]. This will be called from a background thread but the
     * ComplicationSlotsManager should be accessed exclusively from the UiThread afterwards.
     *
     * @param currentUserStyleRepository The [CurrentUserStyleRepository] constructed using the
     * [UserStyleSchema] returned by [createUserStyleSchema].
     * @return The [ComplicationSlotsManager] to pass into [createWatchFace].
     */
    @WorkerThread
    protected open fun createComplicationSlotsManager(
        currentUserStyleRepository: CurrentUserStyleRepository
    ): ComplicationSlotsManager = ComplicationSlotsManager(emptyList(), currentUserStyleRepository)

    /**
     * Override this factory method to create your [WatchFace]. This method will be called by the
     * library on the UiThread. If possible any expensive initialization should be done on a
     * background thread to avoid blocking the UiThread. This will be called from a background
     * thread but the [WatchFace] and its [Renderer] should be accessed exclusively from the
     * UiThread afterwards. There is a memory barrier between construction and rendering so no
     * special threading primitives are required.
     *
     * Warning the system will likely time out waiting for watch face initialization if it takes
     * longer than [MAX_CREATE_WATCHFACE_TIME_MILLIS] milliseconds.
     *
     * @param surfaceHolder The [SurfaceHolder] to pass to the [Renderer]'s constructor.
     * @param watchState The [WatchState] for the watch face.
     * @param complicationSlotsManager The [ComplicationSlotsManager] returned by
     * [createComplicationSlotsManager].
     * @param currentUserStyleRepository The [CurrentUserStyleRepository] constructed using the
     * [UserStyleSchema] returned by [createUserStyleSchema].
     * @return A [WatchFace] whose [Renderer] uses the provided [surfaceHolder].
     */
    @WorkerThread
    protected abstract suspend fun createWatchFace(
        surfaceHolder: SurfaceHolder,
        watchState: WatchState,
        complicationSlotsManager: ComplicationSlotsManager,
        currentUserStyleRepository: CurrentUserStyleRepository
    ): WatchFace

    /** Creates an interactive engine for WallpaperService. */
    final override fun onCreateEngine(): Engine =
        EngineWrapper(getUiThreadHandler(), getBackgroundThreadHandler(), false)

    /** Creates a headless engine. */
    internal fun createHeadlessEngine(): Engine =
        EngineWrapper(getUiThreadHandler(), getBackgroundThreadHandler(), true)

    /** Returns the ui thread [Handler]. */
    public fun getUiThreadHandler(): Handler = getUiThreadHandlerImpl()

    /** This is open for testing. */
    internal open fun getUiThreadHandlerImpl(): Handler = Handler(Looper.getMainLooper())

    /**
     * Returns the lazily constructed background thread [Handler]. During initialization
     * [createUserStyleSchema], [createComplicationSlotsManager] and [createWatchFace] are posted on
     * this handler.
     */
    public fun getBackgroundThreadHandler(): Handler = getBackgroundThreadHandlerImpl()

    internal var backgroundThread: HandlerThread? = null

    /** This is open for testing. */
    internal open fun getBackgroundThreadHandlerImpl(): Handler {
        synchronized(this) {
            if (backgroundThread == null) {
                backgroundThread = HandlerThread("WatchFaceBackground").apply { start() }
            }
            return Handler(backgroundThread!!.looper)
        }
    }

    /** This is open to allow mocking. */
    internal open fun getMutableWatchState() = MutableWatchState()

    /** This is open for use by tests. */
    internal open fun allowWatchFaceToAnimate() = true

    /**
     * Whether or not the pre R style init flow (SET_BINDER wallpaper command) is expected.
     * This is open for use by tests.
     */
    internal open fun expectPreRInitFlow() = Build.VERSION.SDK_INT < Build.VERSION_CODES.R

    /**
     * This is open for use by tests, it allows them to inject a custom [SurfaceHolder].
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public open fun getWallpaperSurfaceHolderOverride(): SurfaceHolder? = null

    internal fun setContext(context: Context) {
        attachBaseContext(context)
    }

    /**
     * Reads WallpaperInteractiveWatchFaceInstanceParams from a file. This is only used in the
     * android R flow.
     */
    internal open fun readDirectBootPrefs(
        context: Context,
        fileName: String
    ): WallpaperInteractiveWatchFaceInstanceParams? = TraceEvent(
        "WatchFaceService.readDirectBootPrefs"
    ).use {
        try {
            val directBootContext = context.createDeviceProtectedStorageContext()
            val reader = directBootContext.openFileInput(fileName)
            reader.use {
                ParcelUtils.fromInputStream<WallpaperInteractiveWatchFaceInstanceParams>(reader)
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Writes WallpaperInteractiveWatchFaceInstanceParams to a file. This is only used in the
     * android R flow.
     */
    internal open fun writeDirectBootPrefs(
        context: Context,
        fileName: String,
        prefs: WallpaperInteractiveWatchFaceInstanceParams
    ): Unit = TraceEvent("WatchFaceService.writeDirectBootPrefs").use {
        val directBootContext = context.createDeviceProtectedStorageContext()
        val writer = directBootContext.openFileOutput(fileName, Context.MODE_PRIVATE)
        writer.use {
            ParcelUtils.toOutputStream(prefs, writer)
        }
    }

    /** Reads user style from a file. This is only used in the pre-android R flow. */
    internal fun readPrefs(context: Context, fileName: String): UserStyleWireFormat {
        val hashMap = HashMap<String, ByteArray>()
        try {
            val reader = InputStreamReader(context.openFileInput(fileName)).buffered()
            reader.use {
                while (true) {
                    val key = reader.readLine() ?: break
                    val value = reader.readLine() ?: break
                    hashMap[key] = Base64.decode(value, Base64.NO_WRAP)
                }
            }
        } catch (e: FileNotFoundException) {
            // We don't need to do anything special here.
        }
        return UserStyleWireFormat(hashMap)
    }

    /** Reads the user style to a file. This is only used in the pre-android R flow. */
    internal fun writePrefs(context: Context, fileName: String, style: UserStyle) {
        val writer = context.openFileOutput(fileName, Context.MODE_PRIVATE).bufferedWriter()
        writer.use {
            for ((key, value) in style.selectedOptions) {
                writer.write(key.id.value)
                writer.newLine()
                writer.write(Base64.encodeToString(value.id.value, Base64.NO_WRAP))
                writer.newLine()
            }
        }
    }

    /** This is the old pre Android R flow that's needed for backwards compatibility. */
    internal class WslFlow(private val engineWrapper: EngineWrapper) {
        class PendingComplicationData(val complicationSlotId: Int, val data: ComplicationData)

        lateinit var iWatchFaceService: IWatchFaceService

        var pendingBackgroundAction: Bundle? = null
        var pendingProperties: Bundle? = null
        var pendingSetWatchFaceStyle = false
        var pendingVisibilityChanged: Boolean? = null
        var pendingComplicationDataUpdates = ArrayList<PendingComplicationData>()
        var complicationsActivated = false
        var watchFaceInitStarted = false
        var lastActiveComplicationSlots: IntArray? = null

        // Only valid after onSetBinder has been called.
        var systemApiVersion = -1

        fun iWatchFaceServiceInitialized() = this::iWatchFaceService.isInitialized

        fun requestWatchFaceStyle() {
            engineWrapper.uiThreadCoroutineScope.launch {
                TraceEvent("requestWatchFaceStyle").use {
                    try {
                        iWatchFaceService.setStyle(
                            engineWrapper.deferredWatchFaceImpl.await().getWatchFaceStyle()
                        )
                    } catch (e: RemoteException) {
                        Log.e(TAG, "Failed to set WatchFaceStyle: ", e)
                    }

                    val activeComplications = lastActiveComplicationSlots
                    if (activeComplications != null) {
                        engineWrapper.setActiveComplicationSlots(activeComplications)
                    }

                    if (engineWrapper.contentDescriptionLabels.isNotEmpty()) {
                        engineWrapper.contentDescriptionLabels =
                            engineWrapper.contentDescriptionLabels
                    }
                }
            }
        }

        fun setDefaultComplicationProviderWithFallbacks(
            complicationSlotId: Int,
            dataSources: List<ComponentName>?,
            @DataSourceId fallbackSystemDataSource: Int,
            type: Int
        ) {

            // For android R flow iWatchFaceService won't have been set.
            if (!iWatchFaceServiceInitialized()) {
                return
            }

            if (systemApiVersion >= 2) {
                iWatchFaceService.setDefaultComplicationProviderWithFallbacks(
                    complicationSlotId,
                    dataSources,
                    fallbackSystemDataSource,
                    type
                )
            } else {
                // If the implementation doesn't support the new API we emulate its behavior by
                // setting complication data sources in the reverse order. This works because if
                // setDefaultComplicationProvider attempts to set a non-existent or incompatible
                // data source it does nothing, which allows us to emulate the same semantics as
                // setDefaultComplicationProviderWithFallbacks albeit with more calls.
                if (fallbackSystemDataSource != WatchFaceImpl.NO_DEFAULT_DATA_SOURCE) {
                    iWatchFaceService.setDefaultSystemComplicationProvider(
                        complicationSlotId, fallbackSystemDataSource, type
                    )
                }

                if (dataSources != null) {
                    // Iterate in reverse order. This could be O(n^2) but n is expected to be small
                    // and the list is probably an ArrayList so it's probably O(n) in practice.
                    for (i in dataSources.size - 1 downTo 0) {
                        iWatchFaceService.setDefaultComplicationProvider(
                            complicationSlotId, dataSources[i], type
                        )
                    }
                }
            }
        }

        fun setActiveComplications(complicationSlotIds: IntArray) {
            // For android R flow iWatchFaceService won't have been set.
            if (!iWatchFaceServiceInitialized()) {
                return
            }

            lastActiveComplicationSlots = complicationSlotIds

            try {
                iWatchFaceService.setActiveComplications(
                    complicationSlotIds, /* updateAll= */ !complicationsActivated
                )
                complicationsActivated = true
            } catch (e: RemoteException) {
                Log.e(TAG, "Failed to set active complicationSlots: ", e)
            }
        }

        fun onRequestStyle() {
            // We can't guarantee the binder has been set and onSurfaceChanged called before this
            // command.
            if (!engineWrapper.watchFaceCreated()) {
                pendingSetWatchFaceStyle = true
                return
            }
            requestWatchFaceStyle()
            pendingSetWatchFaceStyle = false
        }

        @UiThread
        fun onBackgroundAction(extras: Bundle) {
            // We can't guarantee the binder has been set and onSurfaceChanged called before this
            // command.
            if (!engineWrapper.watchFaceCreated()) {
                pendingBackgroundAction = extras
                return
            }

            engineWrapper.setWatchUiState(
                WatchUiState(
                    extras.getBoolean(
                        Constants.EXTRA_AMBIENT_MODE,
                        engineWrapper.mutableWatchState.isAmbient.getValueOr(false)
                    ),
                    extras.getInt(
                        Constants.EXTRA_INTERRUPTION_FILTER,
                        engineWrapper.mutableWatchState.interruptionFilter.getValueOr(0)
                    )
                )
            )

            pendingBackgroundAction = null
        }

        fun onComplicationSlotDataUpdate(extras: Bundle) {
            extras.classLoader = WireComplicationData::class.java.classLoader
            val complicationData: WireComplicationData =
                extras.getParcelable(Constants.EXTRA_COMPLICATION_DATA)!!
            engineWrapper.setComplicationSlotData(
                extras.getInt(Constants.EXTRA_COMPLICATION_ID),
                complicationData.toApiComplicationData()
            )
        }

        fun onSetBinder(extras: Bundle) {
            val binder = extras.getBinder(Constants.EXTRA_BINDER)
            if (binder == null) {
                Log.w(TAG, "Binder is null.")
                return
            }

            iWatchFaceService = IWatchFaceService.Stub.asInterface(binder)

            try {
                // Note if the implementation doesn't support getVersion this will return zero
                // rather than throwing an exception.
                systemApiVersion = iWatchFaceService.apiVersion
            } catch (e: RemoteException) {
                Log.w(TAG, "Failed to getVersion: ", e)
            }

            engineWrapper.uiThreadCoroutineScope.launch { maybeCreateWatchFace() }
        }

        @UiThread
        fun onPropertiesChanged(properties: Bundle) {
            if (!watchFaceInitStarted) {
                pendingProperties = properties
                engineWrapper.uiThreadCoroutineScope.launch { maybeCreateWatchFace() }
                return
            }

            engineWrapper.setImmutableSystemState(
                DeviceConfig(
                    properties.getBoolean(Constants.PROPERTY_LOW_BIT_AMBIENT),
                    properties.getBoolean(Constants.PROPERTY_BURN_IN_PROTECTION),
                    ANALOG_WATCHFACE_REFERENCE_TIME_MS,
                    DIGITAL_WATCHFACE_REFERENCE_TIME_MS
                )
            )
        }

        private suspend fun maybeCreateWatchFace(): Unit = TraceEvent(
            "EngineWrapper.maybeCreateWatchFace"
        ).use {
            // To simplify handling of watch face state, we only construct the [WatchFaceImpl]
            // once iWatchFaceService have been initialized and pending properties sent.
            if (iWatchFaceServiceInitialized() &&
                pendingProperties != null && !engineWrapper.watchFaceCreatedOrPending()
            ) {
                watchFaceInitStarted = true

                // Apply immutable properties to mutableWatchState before creating the watch face.
                onPropertiesChanged(pendingProperties!!)
                pendingProperties = null

                val watchState = engineWrapper.mutableWatchState.asWatchState()
                engineWrapper.createWatchFaceInternal(
                    watchState, null, "maybeCreateWatchFace"
                )

                // Wait for watchface init to complete.
                val watchFaceImpl = engineWrapper.deferredWatchFaceImpl.await()

                val backgroundAction = pendingBackgroundAction
                if (backgroundAction != null) {
                    onBackgroundAction(backgroundAction)
                    pendingBackgroundAction = null
                }
                if (pendingSetWatchFaceStyle) {
                    onRequestStyle()
                }
                val visibility = pendingVisibilityChanged
                if (visibility != null) {
                    engineWrapper.onVisibilityChanged(visibility)
                    pendingVisibilityChanged = null
                }
                for (complicationDataUpdate in pendingComplicationDataUpdates) {
                    watchFaceImpl.onComplicationSlotDataUpdate(
                        complicationDataUpdate.complicationSlotId,
                        complicationDataUpdate.data
                    )
                }
                watchFaceImpl.complicationSlotsManager.onComplicationsUpdated()
            }
        }
    }

    private class RendererAndComplicationSlotsManager(
        val renderer: Renderer,
        val complicationSlotsManager: ComplicationSlotsManager
    )

    /** @hide */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public inner class EngineWrapper(
        private val uiThreadHandler: Handler,
        private val backgroundThreadHandler: Handler,
        headless: Boolean
    ) : WallpaperService.Engine(), WatchFaceHostApi {
        internal val backgroundThreadCoroutineScope =
            CoroutineScope(backgroundThreadHandler.asCoroutineDispatcher().immediate)

        internal val uiThreadCoroutineScope =
            CoroutineScope(uiThreadHandler.asCoroutineDispatcher().immediate)

        private val _context = this@WatchFaceService as Context

        // State to support the old WSL style interface
        internal val wslFlow = WslFlow(this)

        /**
         * [deferredRendererAndComplicationManager] will complete before [deferredWatchFaceImpl].
         */
        private var deferredRendererAndComplicationManager =
            CompletableDeferred<RendererAndComplicationSlotsManager>()

        /**
         * [deferredWatchFaceImpl] will complete after [deferredRendererAndComplicationManager].
         */
        internal var deferredWatchFaceImpl = CompletableDeferred<WatchFaceImpl>()

        /**
         * [deferredSurfaceHolder] will complete after [onSurfaceChanged], before then it's not
         * safe to create a UiThread OpenGL context.
         */
        internal var deferredSurfaceHolder = CompletableDeferred<SurfaceHolder>()

        internal val mutableWatchState = getMutableWatchState().apply {
            isVisible.value = this@EngineWrapper.isVisible
            // Watch faces with the old [onSetBinder] init flow don't know whether the system
            // is ambient until they have received a background action wallpaper command.
            // That's supposed to get sent very quickly, but in case it doesn't we initially
            // assume we're not in ambient mode which should be correct most of the time.
            isAmbient.value = false
            isHeadless = headless
        }

        /**
         * Whether or not we allow watchfaces to animate. In some tests or for headless
         * rendering (for remote config) we don't want this.
         */
        internal var allowWatchfaceToAnimate = allowWatchFaceToAnimate()

        internal var destroyed = false

        internal lateinit var ambientUpdateWakelock: PowerManager.WakeLock

        private lateinit var choreographer: Choreographer

        /**
         * Whether we already have a [frameCallback] posted and waiting in the [Choreographer]
         * queue. This protects us from drawing multiple times in a single frame.
         */
        private var frameCallbackPending = false

        private val frameCallback = object : Choreographer.FrameCallback {
            @SuppressWarnings("SyntheticAccessor")
            override fun doFrame(frameTimeNs: Long) {
                if (destroyed) {
                    return
                }
                require(allowWatchfaceToAnimate) {
                    "Choreographer doFrame called but allowWatchfaceToAnimate is false"
                }
                frameCallbackPending = false
                draw()
            }
        }

        private val invalidateRunnable = Runnable(this::invalidate)

        // If non-null then changes to the style must be persisted.
        private var directBootParams: WallpaperInteractiveWatchFaceInstanceParams? = null

        internal var contentDescriptionLabels: Array<ContentDescriptionLabel> = emptyArray()
            set(value) {
                field = value

                // For the old pre-android R flow.
                if (wslFlow.iWatchFaceServiceInitialized()) {
                    try {
                        wslFlow.iWatchFaceService.setContentDescriptionLabels(value)
                    } catch (e: RemoteException) {
                        Log.e(TAG, "Failed to set accessibility labels: ", e)
                    }
                }
            }

        internal var firstSetWatchUiState = true
        internal var immutableSystemStateDone = false
        internal var immutableChinHeightDone = false

        private var asyncWatchFaceConstructionPending = false

        // Stores the initial ComplicationSlots which could get updated before they're applied.
        private var pendingInitialComplications: List<IdAndComplicationDataWireFormat>? = null

        private var initialUserStyle: UserStyleWireFormat? = null
        private lateinit var interactiveInstanceId: String

        private var createdBy = "?"

        /**
         * Returns the [WatchFaceImpl] if [deferredWatchFaceImpl] has completed successfully or
         * `null` otherwise.
         */
        internal fun getWatchFaceImplOrNull(): WatchFaceImpl? =
            if (deferredWatchFaceImpl.isCompleted) {
                runBlocking {
                    try {
                        deferredWatchFaceImpl.await()
                    } catch (e: Exception) {
                        // The watch face crashed during init. This has already been logged so we
                        // silently ignore.
                        null
                    }
                }
            } else {
                null
            }

        init {
            maybeCreateWCSApi()
        }

        /** Note this function should only be called once. */
        @SuppressWarnings("NewApi")
        @UiThread
        private fun maybeCreateWCSApi(): Unit = TraceEvent(
            "EngineWrapper.maybeCreateWCSApi"
        ).use {
            // If this is a headless instance then we don't want to create a WCS instance.
            if (mutableWatchState.isHeadless) {
                return
            }

            val pendingWallpaperInstance =
                InteractiveInstanceManager.takePendingWallpaperInteractiveWatchFaceInstance()

            // In a direct boot scenario attempt to load the previously serialized parameters.
            if (pendingWallpaperInstance == null && !expectPreRInitFlow()) {
                val params = readDirectBootPrefs(_context, DIRECT_BOOT_PREFS)
                directBootParams = params
                // In tests a watchface may already have been created.
                if (params != null && !watchFaceCreatedOrPending()) {
                    val asyncTraceEvent = AsyncTraceEvent("DirectBoot")
                    try {
                        val instance = createInteractiveInstance(params, "DirectBoot")
                        // WatchFace init is async so its possible we now have a pending
                        // WallpaperInteractiveWatchFaceInstance request.
                        InteractiveInstanceManager
                            .takePendingWallpaperInteractiveWatchFaceInstance()?.let {
                                require(it.params.instanceId == params.instanceId) {
                                    "Mismatch between pendingWallpaperInstance id " +
                                        "${it.params.instanceId} and constructed instance id " +
                                        params.instanceId
                                }
                                it.callback.onInteractiveWatchFaceCreated(instance)
                            }
                    } catch (e: Exception) {
                        InteractiveInstanceManager
                            .takePendingWallpaperInteractiveWatchFaceInstance()?.let {
                                it.callback.onInteractiveWatchFaceCrashed(
                                    CrashInfoParcel(e)
                                )
                            }
                    } finally {
                        asyncTraceEvent.close()
                    }
                }
            }

            // If there's a pending WallpaperInteractiveWatchFaceInstance then create it.
            if (pendingWallpaperInstance != null) {
                val asyncTraceEvent =
                    AsyncTraceEvent("Create PendingWallpaperInteractiveWatchFaceInstance")
                try {
                    pendingWallpaperInstance.callback.onInteractiveWatchFaceCreated(
                        createInteractiveInstance(
                            pendingWallpaperInstance.params,
                            "Boot with pendingWallpaperInstance"
                        )
                    )
                } catch (e: Exception) {
                    pendingWallpaperInstance.callback.onInteractiveWatchFaceCrashed(
                        CrashInfoParcel(e)
                    )
                }
                asyncTraceEvent.close()
                val params = pendingWallpaperInstance.params
                directBootParams = params
                // We don't want to display complications in direct boot mode so replace with an
                // empty list. NB we can't actually serialise complications anyway so that's just as
                // well...
                params.idAndComplicationDataWireFormats = emptyList()

                // Writing even small amounts of data to storage is quite slow and if we did
                // that immediately, we'd delay the first frame which is rendered via
                // onSurfaceRedrawNeeded. By posting this task we expedite first frame
                // rendering. There is a small window where the direct boot could be stale if
                // the watchface crashed but this seems unlikely in practice.
                backgroundThreadHandler.post {
                    writeDirectBootPrefs(_context, DIRECT_BOOT_PREFS, params)
                }
            }
        }

        @UiThread
        internal fun ambientTickUpdate(): Unit = TraceEvent("EngineWrapper.ambientTickUpdate").use {
            if (mutableWatchState.isAmbient.value) {
                ambientUpdateWakelock.acquire()
                // It's unlikely an ambient tick would be sent to a watch face that hasn't loaded
                // yet. The watch face will render at least once upon loading so we don't need to do
                // anyhting special here.
                getWatchFaceImplOrNull()?.renderer?.invalidate()
                ambientUpdateWakelock.acquire(SURFACE_DRAW_TIMEOUT_MS)
            }
        }

        @UiThread
        internal fun setWatchUiState(watchUiState: WatchUiState) {
            if (firstSetWatchUiState ||
                watchUiState.inAmbientMode != mutableWatchState.isAmbient.value
            ) {
                mutableWatchState.isAmbient.value = watchUiState.inAmbientMode
            }

            if (firstSetWatchUiState ||
                watchUiState.interruptionFilter != mutableWatchState.interruptionFilter.value
            ) {
                mutableWatchState.interruptionFilter.value = watchUiState.interruptionFilter
            }

            firstSetWatchUiState = false
        }

        @UiThread
        internal suspend fun setUserStyle(
            userStyle: UserStyleWireFormat
        ): Unit = TraceEvent("EngineWrapper.setUserStyle").use {
            val watchFaceImpl = deferredWatchFaceImpl.await()
            watchFaceImpl.onSetStyleInternal(
                UserStyle(UserStyleData(userStyle), watchFaceImpl.currentUserStyleRepository.schema)
            )

            // Update direct boot params if we have any.
            val params = directBootParams ?: return
            val currentStyle = watchFaceImpl.currentUserStyleRepository.userStyle.toWireFormat()
            if (params.userStyle.equals(currentStyle)) {
                return
            }
            params.userStyle = currentStyle
            // We don't want to display complications in direct boot mode so replace with an empty
            // list. NB we can't actually serialise complications anyway so that's just as well...
            params.idAndComplicationDataWireFormats = emptyList()

            backgroundThreadHandler.post {
                writeDirectBootPrefs(_context, DIRECT_BOOT_PREFS, params)
            }
        }

        @UiThread
        internal fun setImmutableSystemState(deviceConfig: DeviceConfig) {
            // These properties never change so set them once only.
            if (!immutableSystemStateDone) {
                mutableWatchState.hasLowBitAmbient = deviceConfig.hasLowBitAmbient
                mutableWatchState.hasBurnInProtection =
                    deviceConfig.hasBurnInProtection
                mutableWatchState.analogPreviewReferenceTimeMillis =
                    deviceConfig.analogPreviewReferenceTimeMillis
                mutableWatchState.digitalPreviewReferenceTimeMillis =
                    deviceConfig.digitalPreviewReferenceTimeMillis

                immutableSystemStateDone = true
            }
        }

        @SuppressLint("SyntheticAccessor")
        internal fun setComplicationSlotData(
            complicationSlotId: Int,
            data: ComplicationData
        ): Unit = TraceEvent("EngineWrapper.setComplicationSlotData").use {
            val watchFaceImpl = getWatchFaceImplOrNull()
            if (watchFaceImpl != null) {
                watchFaceImpl.onComplicationSlotDataUpdate(complicationSlotId, data)
                watchFaceImpl.complicationSlotsManager.onComplicationsUpdated()
            } else {
                // If the watch face hasn't loaded yet then we append
                // pendingComplicationDataUpdates so it can be applied later.
                wslFlow.pendingComplicationDataUpdates.add(
                    WslFlow.PendingComplicationData(complicationSlotId, data)
                )
            }
        }

        internal fun clearComplicationData() {
            val watchFaceImpl = getWatchFaceImplOrNull()
            require(watchFaceImpl != null) {
                "WatchFace must have been created first"
            }
            watchFaceImpl.clearComplicationData()
        }

        @UiThread
        internal fun setComplicationDataList(
            complicationDataWireFormats: MutableList<IdAndComplicationDataWireFormat>
        ): Unit = TraceEvent("EngineWrapper.setComplicationDataList").use {
            val watchFaceImpl = getWatchFaceImplOrNull()
            if (watchFaceImpl != null) {
                for (idAndComplicationData in complicationDataWireFormats) {
                    watchFaceImpl.onComplicationSlotDataUpdate(
                        idAndComplicationData.id,
                        idAndComplicationData.complicationData.toApiComplicationData()
                    )
                }
                watchFaceImpl.complicationSlotsManager.onComplicationsUpdated()
            } else {
                // If the watchface hasn't been created yet, update pendingInitialComplications so
                // it can be applied later.
                pendingInitialComplications = complicationDataWireFormats
            }
        }

        override fun getContext(): Context = _context

        override fun getUiThreadHandler(): Handler = uiThreadHandler

        override fun getBackgroundThreadHandler(): Handler = backgroundThreadHandler

        override fun onCreate(
            holder: SurfaceHolder
        ): Unit = TraceEvent("EngineWrapper.onCreate").use {
            super.onCreate(holder)

            ambientUpdateWakelock =
                (getSystemService(Context.POWER_SERVICE) as PowerManager)
                    .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "$TAG:[AmbientUpdate]")
            // Disable reference counting for our wake lock so that we can use the same wake lock
            // for user code in invalidate() and after that for having canvas drawn.
            ambientUpdateWakelock.setReferenceCounted(false)

            // Rerender watch face if the surface changes.
            holder.addCallback(
                object : SurfaceHolder.Callback {
                    override fun surfaceChanged(
                        holder: SurfaceHolder,
                        format: Int,
                        width: Int,
                        height: Int
                    ) {
                        // We can sometimes get this callback before the watchface has been created
                        // in which case it's safe to drop it.
                        if (deferredWatchFaceImpl.isCompleted) {
                            invalidate()
                        }
                    }

                    override fun surfaceDestroyed(holder: SurfaceHolder) {
                    }

                    override fun surfaceCreated(holder: SurfaceHolder) {
                    }
                }
            )
        }

        override fun onSurfaceChanged(
            holder: SurfaceHolder,
            format: Int,
            width: Int,
            height: Int
        ): Unit = TraceEvent("EngineWrapper.onSurfaceChanged").use {
            super.onSurfaceChanged(holder, format, width, height)
            deferredSurfaceHolder.complete(holder)
        }

        override fun onApplyWindowInsets(
            insets: WindowInsets?
        ): Unit = TraceEvent("EngineWrapper.onApplyWindowInsets").use {
            super.onApplyWindowInsets(insets)
            @Px val chinHeight =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    ChinHeightApi30.extractFromWindowInsets(insets)
                } else {
                    ChinHeightApi25.extractFromWindowInsets(insets)
                }
            if (immutableChinHeightDone) {
                // The chin size cannot change so this should be called only once.
                if (mutableWatchState.chinHeight != chinHeight) {
                    Log.w(
                        TAG,
                        "unexpected chin size change ignored: " +
                            "${mutableWatchState.chinHeight} != $chinHeight"
                    )
                }
                return
            }
            mutableWatchState.chinHeight = chinHeight
            immutableChinHeightDone = true
        }

        private fun quitBackgroundThreadIfCreated() {
            synchronized(this) {
                backgroundThread?.quitSafely()
                backgroundThread = null
            }
        }

        @UiThread
        override fun onDestroy(): Unit = TraceEvent("EngineWrapper.onDestroy").use {
            destroyed = true
            backgroundThreadCoroutineScope.cancel()
            quitBackgroundThreadIfCreated()
            uiThreadHandler.removeCallbacks(invalidateRunnable)
            if (this::choreographer.isInitialized) {
                choreographer.removeFrameCallback(frameCallback)
            }
            // The WatchFaceImpl is created on the UiThread so if we get here and it's not crated we
            // can be sure it'll never be created hence we don't need to destroy.
            getWatchFaceImplOrNull()?.onDestroy()
            if (this::interactiveInstanceId.isInitialized) {
                InteractiveInstanceManager.deleteInstance(interactiveInstanceId)
            }

            super.onDestroy()
        }

        override fun onCommand(
            action: String?,
            x: Int,
            y: Int,
            z: Int,
            extras: Bundle?,
            resultRequested: Boolean
        ): Bundle? {
            // From android R onwards the integration changes and no wallpaper commands are allowed
            // or expected and can/should be ignored.
            if (!expectPreRInitFlow()) {
                TraceEvent("onCommand Ignored").close()
                return null
            }
            when (action) {
                Constants.COMMAND_AMBIENT_UPDATE ->
                    uiThreadHandler.runOnHandlerWithTracing("onCommand COMMAND_AMBIENT_UPDATE") {
                        ambientTickUpdate()
                    }
                Constants.COMMAND_BACKGROUND_ACTION ->
                    uiThreadHandler.runOnHandlerWithTracing("onCommand COMMAND_BACKGROUND_ACTION") {
                        wslFlow.onBackgroundAction(extras!!)
                    }
                Constants.COMMAND_COMPLICATION_DATA ->
                    uiThreadHandler.runOnHandlerWithTracing("onCommand COMMAND_COMPLICATION_DATA") {
                        wslFlow.onComplicationSlotDataUpdate(extras!!)
                    }
                Constants.COMMAND_REQUEST_STYLE ->
                    uiThreadHandler.runOnHandlerWithTracing("onCommand COMMAND_REQUEST_STYLE") {
                        wslFlow.onRequestStyle()
                    }
                Constants.COMMAND_SET_BINDER ->
                    uiThreadHandler.runOnHandlerWithTracing("onCommand COMMAND_SET_BINDER") {
                        wslFlow.onSetBinder(extras!!)
                    }
                Constants.COMMAND_SET_PROPERTIES ->
                    uiThreadHandler.runOnHandlerWithTracing("onCommand COMMAND_SET_PROPERTIES") {
                        wslFlow.onPropertiesChanged(extras!!)
                    }
                Constants.COMMAND_TAP ->
                    uiThreadCoroutineScope.runBlockingWithTracing("onCommand COMMAND_TAP") {
                        deferredWatchFaceImpl.await().onTapCommand(TapType.UP, x, y)
                    }
                Constants.COMMAND_TOUCH ->
                    uiThreadCoroutineScope.runBlockingWithTracing("onCommand COMMAND_TOUCH") {
                        deferredWatchFaceImpl.await().onTapCommand(TapType.DOWN, x, y)
                    }
                Constants.COMMAND_TOUCH_CANCEL ->
                    uiThreadCoroutineScope.runBlockingWithTracing(
                        "onCommand COMMAND_TOUCH_CANCEL"
                    ) {
                        deferredWatchFaceImpl.await().onTapCommand(TapType.CANCEL, x, y)
                    }
                else -> {
                }
            }
            return null
        }

        override fun getInitialUserStyle(): UserStyleWireFormat? = initialUserStyle

        /** This will be called from a binder thread. */
        @WorkerThread
        internal fun getDefaultProviderPolicies(): Array<IdTypeAndDefaultProviderPolicyWireFormat> {
            return createComplicationSlotsManager(
                CurrentUserStyleRepository(createUserStyleSchema())
            ).getDefaultProviderPolicies()
        }

        /** This will be called from a binder thread. */
        @WorkerThread
        internal fun getUserStyleSchemaWireFormat() = createUserStyleSchema().toWireFormat()

        /** This will be called from a binder thread. */
        @WorkerThread
        internal fun getComplicationSlotMetadataWireFormats() =
            createComplicationSlotsManager(
                CurrentUserStyleRepository(createUserStyleSchema())
            ).complicationSlots.map {
                ComplicationSlotMetadataWireFormat(
                    it.key,
                    it.value.complicationSlotBounds.perComplicationTypeBounds.keys.map {
                        it.toWireComplicationType()
                    }.toIntArray(),
                    it.value.complicationSlotBounds.perComplicationTypeBounds
                        .values.toTypedArray(),
                    it.value.boundsType,
                    it.value.supportedTypes.toWireTypes(),
                    it.value.defaultDataSourcePolicy.dataSourcesAsList(),
                    it.value.defaultDataSourcePolicy.systemDataSourceFallback,
                    it.value.defaultDataSourceType.toWireComplicationType(),
                    it.value.initiallyEnabled,
                    it.value.fixedComplicationDataSource,
                    it.value.configExtras
                )
            }.toTypedArray()

        @RequiresApi(27)
        internal fun createHeadlessInstance(
            params: HeadlessWatchFaceInstanceParams
        ): HeadlessWatchFaceImpl = TraceEvent("EngineWrapper.createHeadlessInstance").use {
            require(!watchFaceCreatedOrPending()) {
                "WatchFace already exists! Created by $createdBy"
            }
            setImmutableSystemState(params.deviceConfig)

            // Fake SurfaceHolder with just enough methods implemented for headless rendering.
            val fakeSurfaceHolder = object : SurfaceHolder {
                val callbacks = HashSet<SurfaceHolder.Callback>()

                override fun setType(type: Int) {
                    throw NotImplementedError()
                }

                override fun getSurface(): Surface {
                    throw NotImplementedError()
                }

                override fun setSizeFromLayout() {
                    throw NotImplementedError()
                }

                override fun lockCanvas(): Canvas {
                    throw NotImplementedError()
                }

                override fun lockCanvas(dirty: Rect?): Canvas {
                    throw NotImplementedError()
                }

                override fun getSurfaceFrame() = Rect(0, 0, params.width, params.height)

                override fun setFixedSize(width: Int, height: Int) {
                    throw NotImplementedError()
                }

                override fun removeCallback(callback: SurfaceHolder.Callback) {
                    callbacks.remove(callback)
                }

                override fun isCreating(): Boolean {
                    throw NotImplementedError()
                }

                override fun addCallback(callback: SurfaceHolder.Callback) {
                    callbacks.add(callback)
                }

                override fun setFormat(format: Int) {
                    throw NotImplementedError()
                }

                override fun setKeepScreenOn(screenOn: Boolean) {
                    throw NotImplementedError()
                }

                override fun unlockCanvasAndPost(canvas: Canvas?) {
                    throw NotImplementedError()
                }
            }

            allowWatchfaceToAnimate = false
            require(mutableWatchState.isHeadless)
            val watchState = mutableWatchState.asWatchState()

            createWatchFaceInternal(
                watchState,
                fakeSurfaceHolder,
                "createHeadlessInstance"
            )

            mutableWatchState.isVisible.value = true
            mutableWatchState.isAmbient.value = false
            return HeadlessWatchFaceImpl(this)
        }

        @UiThread
        @RequiresApi(27)
        internal fun createInteractiveInstance(
            params: WallpaperInteractiveWatchFaceInstanceParams,
            _createdBy: String
        ): InteractiveWatchFaceImpl = TraceEvent(
            "EngineWrapper.createInteractiveInstance"
        ).use {
            require(!watchFaceCreatedOrPending()) {
                "WatchFace already exists! Created by $createdBy"
            }
            require(!mutableWatchState.isHeadless)

            setImmutableSystemState(params.deviceConfig)
            setWatchUiState(params.watchUiState)
            initialUserStyle = params.userStyle

            val watchState = mutableWatchState.asWatchState()

            // Store the initial complications, this could be modified by new data before being
            // applied.
            pendingInitialComplications = params.idAndComplicationDataWireFormats

            createWatchFaceInternal(
                watchState,
                getWallpaperSurfaceHolderOverride(),
                _createdBy
            )

            val instance = InteractiveWatchFaceImpl(this, params.instanceId)
            InteractiveInstanceManager.addInstance(instance)
            interactiveInstanceId = params.instanceId
            return instance
        }

        override fun onSurfaceRedrawNeeded(holder: SurfaceHolder) {
            if (TRACE_DRAW) {
                Trace.beginSection("onSurfaceRedrawNeeded")
            }
            // The watch face will draw at least once upon creation so it doesn't matter if it's
            // not been created yet.
            getWatchFaceImplOrNull()?.onSurfaceRedrawNeeded()
            if (TRACE_DRAW) {
                Trace.endSection()
            }
        }

        internal fun createWatchFaceInternal(
            watchState: WatchState,
            overrideSurfaceHolder: SurfaceHolder?,
            _createdBy: String
        ) {
            asyncWatchFaceConstructionPending = true
            createdBy = _createdBy

            backgroundThreadCoroutineScope.launch {
                val timeBefore = System.currentTimeMillis()
                val currentUserStyleRepository =
                    TraceEvent("WatchFaceService.createUserStyleSchema").use {
                        CurrentUserStyleRepository(createUserStyleSchema())
                    }
                val complicationSlotsManager =
                    TraceEvent("WatchFaceService.createComplicationsManager").use {
                        createComplicationSlotsManager(currentUserStyleRepository)
                    }
                complicationSlotsManager.watchState = watchState

                val calendar = Calendar.getInstance()
                val deferredWatchFace = CompletableDeferred<WatchFace>()
                val initStyleAndComplicationsDone = CompletableDeferred<Unit>()

                // WatchFaceImpl (which registers broadcast observers) needs to be constructed
                // on the UIThread. Part of this process can be done in parallel with
                // createWatchFace.
                uiThreadCoroutineScope.launch {
                    createWatchFaceImpl(
                        calendar,
                        complicationSlotsManager,
                        currentUserStyleRepository,
                        deferredWatchFace,
                        initStyleAndComplicationsDone,
                        watchState
                    )
                }

                try {
                    val watchFace = TraceEvent("WatchFaceService.createWatchFace").use {
                        // Note by awaiting deferredSurfaceHolder we ensure onSurfaceChanged has
                        // been called and we're passing the correct updated surface holder. This is
                        // important for GL rendering.
                        createWatchFace(
                            overrideSurfaceHolder ?: deferredSurfaceHolder.await(),
                            watchState,
                            complicationSlotsManager,
                            currentUserStyleRepository
                        )
                    }
                    deferredRendererAndComplicationManager.complete(
                        RendererAndComplicationSlotsManager(
                            watchFace.renderer,
                            complicationSlotsManager
                        )
                    )

                    if (watchFace.renderer is Renderer.GlesRenderer) {
                        watchFace.renderer.initBackgroundThreadOpenGlContext()
                    }

                    // For Gles watch faces this will trigger UIThread context creation and must be
                    // done after initBackgroundThreadOpenGlContext.
                    deferredWatchFace.complete(watchFace)

                    val timeAfter = System.currentTimeMillis()
                    val timeTaken = timeAfter - timeBefore
                    if (timeTaken > MAX_CREATE_WATCHFACE_TIME_MILLIS) {
                        Log.e(
                            TAG,
                            "createUserStyleSchema, createComplicationSlotsManager and " +
                                "createWatchFace should complete in less than " +
                                MAX_CREATE_WATCHFACE_TIME_MILLIS + " milliseconds."
                        )
                    }

                    // Perform more initialization on the background thread.
                    initStyleAndComplications(
                        complicationSlotsManager,
                        currentUserStyleRepository,
                        watchFace.renderer,
                        calendar
                    )

                    // Now init has completed, it's OK to complete deferredWatchFaceImpl.
                    initStyleAndComplicationsDone.complete(Unit)
                } catch (e: Exception) {
                    Log.e(TAG, "WatchFace crashed during init", e)
                    deferredWatchFaceImpl.completeExceptionally(e)
                }
            }
        }

        /**
         * This function contains the parts of watch face init that have to be done on the UI
         * thread.
         */
        @UiThread
        private suspend fun createWatchFaceImpl(
            calendar: Calendar,
            complicationSlotsManager: ComplicationSlotsManager,
            currentUserStyleRepository: CurrentUserStyleRepository,
            deferredWatchFace: CompletableDeferred<WatchFace>,
            initStyleAndComplicationsDone: CompletableDeferred<Unit>,
            watchState: WatchState
        ) {
            pendingInitialComplications?.let {
                for (idAndData in it) {
                    complicationSlotsManager.onComplicationDataUpdate(
                        idAndData.id,
                        idAndData.complicationData.toApiComplicationData()
                    )
                }
            }

            val broadcastsObserver = BroadcastsObserver(
                watchState,
                this,
                deferredWatchFaceImpl,
                uiThreadCoroutineScope
            )

            // There's no point creating BroadcastsReceiver for headless instances.
            val broadcastsReceiver = TraceEvent("create BroadcastsReceiver").use {
                if (watchState.isHeadless) {
                    null
                } else {
                    BroadcastsReceiver(_context, broadcastsObserver)
                }
            }

            val watchFace = deferredWatchFace.await()
            TraceEvent("WatchFaceImpl.init").use {
                val watchFaceImpl = WatchFaceImpl(
                    watchFace,
                    this@EngineWrapper,
                    watchState,
                    currentUserStyleRepository,
                    complicationSlotsManager,
                    calendar,
                    broadcastsObserver,
                    broadcastsReceiver
                )

                // Make sure no UI thread rendering (a consequence of completing
                // deferredWatchFaceImpl) occurs before initStyleAndComplications has
                // executed. NB usually we won't have to wait at all.
                initStyleAndComplicationsDone.await()
                deferredWatchFaceImpl.complete(watchFaceImpl)
                asyncWatchFaceConstructionPending = false

                // For interactive instances we want to expedite the first frame to get something
                // rendered as soon as its possible to do so. NB in tests we may not always want
                // to draw this expedited first frame.
                if (!watchState.isHeadless && allowWatchFaceToAnimate()) {
                    TraceEvent("WatchFace.drawFirstFrame").use {
                        watchFaceImpl.onDraw()
                    }
                }
            }
        }

        /**
         * It is OK to call this from a worker thread because we carefully ensure there's no
         * concurrent writes to the ComplicationSlotsManager. No UI thread rendering can be done
         * until after this has completed.
         */
        @WorkerThread
        internal fun initStyleAndComplications(
            complicationSlotsManager: ComplicationSlotsManager,
            currentUserStyleRepository: CurrentUserStyleRepository,
            renderer: Renderer,
            calendar: Calendar
        ) = TraceEvent("initStyleAndComplications").use {
            // If the system has a stored user style then Home/SysUI is in charge of style
            // persistence, otherwise we need to do our own.
            val storedUserStyle = getInitialUserStyle()
            if (storedUserStyle != null) {
                TraceEvent("WatchFaceImpl.init apply userStyle").use {
                    currentUserStyleRepository.userStyle =
                        UserStyle(UserStyleData(storedUserStyle), currentUserStyleRepository.schema)
                }
            } else {
                TraceEvent("WatchFaceImpl.init apply userStyle from prefs").use {
                    // The system doesn't support preference persistence we need to do it ourselves.
                    val preferencesFile = "watchface_prefs_${_context.javaClass.name}.txt"

                    currentUserStyleRepository.userStyle = UserStyle(
                        UserStyleData(readPrefs(_context, preferencesFile)),
                        currentUserStyleRepository.schema
                    )

                    currentUserStyleRepository.addUserStyleChangeListener(
                        object : CurrentUserStyleRepository.UserStyleChangeListener {
                            @SuppressLint("SyntheticAccessor")
                            override fun onUserStyleChanged(userStyle: UserStyle) {
                                writePrefs(_context, preferencesFile, userStyle)
                            }
                        }
                    )
                }
            }

            // We need to inhibit an immediate callback during initialization because members are
            // not fully constructed and it will fail. It's also superfluous because we're going
            // to render soon anyway.
            var initFinished = false
            complicationSlotsManager.init(
                this, calendar, renderer,
                object : ComplicationSlot.InvalidateListener {
                    @SuppressWarnings("SyntheticAccessor")
                    override fun onInvalidate() {
                        // This could be called on any thread.
                        uiThreadHandler.runOnHandlerWithTracing("onInvalidate") {
                            if (initFinished) {
                                getWatchFaceImplOrNull()?.invalidateIfNotAnimating()
                            }
                        }
                    }
                }
            )
            initFinished = true
        }

        override fun onVisibilityChanged(visible: Boolean): Unit = TraceEvent(
            "onVisibilityChanged"
        ).use {
            super.onVisibilityChanged(visible)

            // In the WSL flow Home doesn't know when WallpaperService has actually launched a
            // watchface after requesting a change. It used [Constants.ACTION_REQUEST_STATE] as a
            // signal to trigger the old boot flow (sending the binder etc). This is no longer
            // required from android R onwards. See (b/181965946).
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
                // We are requesting state every time the watch face changes its visibility because
                // wallpaper commands have a tendency to be dropped. By requesting it on every
                // visibility change, we ensure that we don't become a victim of some race
                // condition.
                sendBroadcast(
                    Intent(Constants.ACTION_REQUEST_STATE).apply {
                        putExtra(Constants.EXTRA_WATCH_FACE_VISIBLE, visible)
                    }
                )

                // We can't guarantee the binder has been set and onSurfaceChanged called before
                // this command.
                if (!watchFaceCreated()) {
                    wslFlow.pendingVisibilityChanged = visible
                    return
                }
            }

            mutableWatchState.isVisible.value = visible
            wslFlow.pendingVisibilityChanged = null
        }

        override fun invalidate() {
            if (!allowWatchfaceToAnimate) {
                return
            }
            if (!frameCallbackPending) {
                if (LOG_VERBOSE) {
                    Log.v(TAG, "invalidate: requesting draw")
                }
                frameCallbackPending = true
                if (!this::choreographer.isInitialized) {
                    choreographer = Choreographer.getInstance()
                }
                choreographer.postFrameCallback(frameCallback)
            } else {
                if (LOG_VERBOSE) {
                    Log.v(TAG, "invalidate: draw already requested")
                }
            }
        }

        internal fun draw() {
            try {
                if (TRACE_DRAW) {
                    Trace.beginSection("onDraw")
                }
                if (LOG_VERBOSE) {
                    Log.v(WatchFaceService.TAG, "drawing frame")
                }

                val watchFaceImpl: WatchFaceImpl? = getWatchFaceImplOrNull()
                watchFaceImpl?.onDraw() ?: drawBlack()
            } finally {
                if (TRACE_DRAW) {
                    Trace.endSection()
                }
            }
        }

        private fun drawBlack() {
            // TODO(b/189452267): We don't know if the watchface will use hardware or software
            // canvas and mixing the two leads to skia crashes. For now we do nothing rather than
            // drawing a blank frame.
        }

        internal fun watchFaceCreated() = deferredWatchFaceImpl.isCompleted

        internal fun watchFaceCreatedOrPending() =
            watchFaceCreated() || asyncWatchFaceConstructionPending

        override fun setDefaultComplicationDataSourceWithFallbacks(
            complicationSlotId: Int,
            dataSources: List<ComponentName>?,
            @DataSourceId fallbackSystemProvider: Int,
            type: Int
        ) {
            wslFlow.setDefaultComplicationProviderWithFallbacks(
                complicationSlotId,
                dataSources,
                fallbackSystemProvider,
                type
            )
        }

        override fun setActiveComplicationSlots(complicationSlotIds: IntArray): Unit = TraceEvent(
            "WatchFaceService.setActiveComplications"
        ).use {
            wslFlow.setActiveComplications(complicationSlotIds)
        }

        @UiThread
        override fun updateContentDescriptionLabels() {
            val labels = mutableListOf<Pair<Int, ContentDescriptionLabel>>()

            uiThreadCoroutineScope.launch {
                TraceEvent(
                    "WatchFaceService.updateContentDescriptionLabels A"
                ).close()
                val rendererAndComplicationManager =
                    deferredRendererAndComplicationManager.await()

                TraceEvent(
                    "WatchFaceService.updateContentDescriptionLabels"
                ).use {
                    // The side effects of this need to be applied before deferredWatchFaceImpl is
                    // completed.
                    val renderer = rendererAndComplicationManager.renderer
                    val complicationSlotsManager =
                        rendererAndComplicationManager.complicationSlotsManager

                    // Add a ContentDescriptionLabel for the main clock element.
                    labels.add(
                        Pair(
                            WATCH_ELEMENT_ACCESSIBILITY_TRAVERSAL_INDEX,
                            ContentDescriptionLabel(
                                renderer.getMainClockElementBounds(),
                                AccessibilityUtils.makeTimeAsComplicationText(_context)
                            )
                        )
                    )

                    // Add a ContentDescriptionLabel for each enabled complication.
                    val screenBounds = renderer.screenBounds
                    for ((_, complication) in complicationSlotsManager.complicationSlots) {
                        if (complication.enabled) {
                            if (complication.boundsType == ComplicationSlotBoundsType.BACKGROUND) {
                                ComplicationSlotBoundsType.BACKGROUND
                            } else {
                                if (complication.complicationData.hasValue()) {
                                    labels.add(
                                        Pair(
                                            complication.accessibilityTraversalIndex,
                                            ContentDescriptionLabel(
                                                _context,
                                                complication.computeBounds(screenBounds),
                                                complication.complicationData.value
                                                    .asWireComplicationData()
                                            )
                                        )
                                    )
                                }
                            }
                        }
                    }

                    // Add any additional labels defined by the watch face.
                    for (labelPair in renderer.additionalContentDescriptionLabels) {
                        labels.add(
                            Pair(
                                labelPair.first,
                                ContentDescriptionLabel(
                                    labelPair.second.bounds,
                                    labelPair.second.text.toWireComplicationText()
                                ).apply {
                                    tapAction = labelPair.second.tapAction
                                }
                            )
                        )
                    }

                    contentDescriptionLabels =
                        labels.sortedBy { it.first }.map { it.second }.toTypedArray()

                    // From Android R Let SysUI know the labels have changed if the accessibility
                    // manager is enabled.
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R &&
                        getAccessibilityManager().isEnabled
                    ) {
                        // TODO(alexclarke): This should require a permission. See http://b/184717802
                        _context.sendBroadcast(
                            Intent(Constants.ACTION_WATCH_FACE_REFRESH_A11Y_LABELS)
                        )
                    }
                }
            }
        }

        private fun getAccessibilityManager() =
            _context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager

        @UiThread
        internal fun dump(writer: IndentingPrintWriter) {
            require(uiThreadHandler.looper.isCurrentThread) {
                "dump must be called from the UIThread"
            }
            writer.println("WatchFaceEngine:")
            writer.increaseIndent()
            when {
                wslFlow.iWatchFaceServiceInitialized() -> writer.println("WSL style init flow")
                this.watchFaceCreatedOrPending() -> writer.println("Androidx style init flow")
                expectPreRInitFlow() -> writer.println("Expecting WSL style init")
                else -> writer.println("Expecting androidx style style init")
            }

            if (wslFlow.iWatchFaceServiceInitialized()) {
                writer.println(
                    "iWatchFaceService.asBinder().isBinderAlive=" +
                        "${wslFlow.iWatchFaceService.asBinder().isBinderAlive}"
                )
                if (wslFlow.iWatchFaceService.asBinder().isBinderAlive) {
                    writer.println(
                        "iWatchFaceService.apiVersion=" +
                            "${wslFlow.iWatchFaceService.apiVersion}"
                    )
                }
            }
            writer.println("createdBy=$createdBy")
            writer.println("watchFaceInitStarted=$wslFlow.watchFaceInitStarted")
            writer.println("asyncWatchFaceConstructionPending=$asyncWatchFaceConstructionPending")

            if (this::interactiveInstanceId.isInitialized) {
                writer.println("interactiveInstanceId=$interactiveInstanceId")
            }

            writer.println("frameCallbackPending=$frameCallbackPending")
            writer.println("destroyed=$destroyed")

            if (!destroyed) {
                getWatchFaceImplOrNull()?.dump(writer)
            }
            writer.decreaseIndent()
        }
    }

    @UiThread
    override fun dump(fd: FileDescriptor, writer: PrintWriter, args: Array<String>) {
        super.dump(fd, writer, args)
        val indentingPrintWriter = IndentingPrintWriter(writer)
        indentingPrintWriter.println("AndroidX WatchFaceService $packageName")
        InteractiveInstanceManager.dump(indentingPrintWriter)
        EditorService.globalEditorService.dump(indentingPrintWriter)
        HeadlessWatchFaceImpl.dump(indentingPrintWriter)
        indentingPrintWriter.flush()
    }

    private object ChinHeightApi25 {
        @Suppress("DEPRECATION")
        @Px
        fun extractFromWindowInsets(insets: WindowInsets?) =
            insets?.systemWindowInsetBottom ?: 0
    }

    @RequiresApi(30)
    private object ChinHeightApi30 {
        @Px
        fun extractFromWindowInsets(insets: WindowInsets?) =
            insets?.getInsets(WindowInsets.Type.systemBars())?.bottom ?: 0
    }
}

/**
 * Runs the supplied task on the handler thread. If we're not on the handler thread a task is posted
 * and we block until it's been processed.
 *
 * AIDL calls are dispatched from a thread pool, but for simplicity WatchFaceImpl code is largely
 * single threaded so we need to post tasks to the UI thread and wait for them to execute.
 *
 * @param traceEventName The name of the trace event to emit.
 * @param task The task to post on the handler.
 * @return [R] the return value of [task].
 */
internal fun <R> Handler.runBlockingOnHandlerWithTracing(
    traceEventName: String,
    task: () -> R
): R = TraceEvent(traceEventName).use {
    if (looper == Looper.myLooper()) {
        task.invoke()
    } else {
        val latch = CountDownLatch(1)
        var returnVal: R? = null
        var exception: Exception? = null
        if (post {
            try {
                returnVal = TraceEvent("$traceEventName invokeTask").use {
                    task.invoke()
                }
            } catch (e: Exception) {
                // Will rethrow on the calling thread.
                exception = e
            }
            latch.countDown()
        }
        ) {
            latch.await()
            if (exception != null) {
                throw exception as Exception
            }
        }
        // R might be nullable so we can't assert nullability here.
        @Suppress("UNCHECKED_CAST")
        returnVal as R
    }
}

/**
 * Runs the supplied task on the handler thread. If we're not on the handler thread a task is
 * posted.
 *
 * @param traceEventName The name of the trace event to emit.
 * @param task The task to post on the handler.
 */
internal fun Handler.runOnHandlerWithTracing(
    traceEventName: String,
    task: () -> Unit
) = TraceEvent(traceEventName).use {
    if (looper == Looper.myLooper()) {
        task.invoke()
    } else {
        post {
            TraceEvent("$traceEventName invokeTask").use { task.invoke() }
        }
    }
}

/**
 * Runs a task in the [CoroutineScope] and blocks until it has completed.
 *
 * @param traceEventName The name of the trace event to emit.
 * @param task The task to run on the [CoroutineScope].
 */
internal fun <R> CoroutineScope.runBlockingWithTracing(
    traceEventName: String,
    task: suspend () -> R
): R = TraceEvent(traceEventName).use {
    val latch = CountDownLatch(1)
    var r: R? = null
    launch {
        try {
            r = task()
        } catch (e: Exception) {
            Log.e("CoroutineScope", "Exception in traceEventName", e)
            throw e
        }
        latch.countDown()
    }
    latch.await()
    return r!!
}
