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

import android.app.KeyguardManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Canvas
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.os.PowerManager
import android.os.Process
import android.os.RemoteCallbackList
import android.os.RemoteException
import android.os.Trace
import android.service.wallpaper.WallpaperService
import android.support.wearable.complications.ComplicationData as WireComplicationData
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
import androidx.annotation.AnyThread
import androidx.annotation.Px
import androidx.annotation.RequiresApi
import androidx.annotation.RestrictTo
import androidx.annotation.UiThread
import androidx.annotation.VisibleForTesting
import androidx.annotation.WorkerThread
import androidx.versionedparcelable.ParcelUtils
import androidx.wear.watchface.complications.SystemDataSources.DataSourceId
import androidx.wear.watchface.complications.data.ComplicationData
import androidx.wear.watchface.complications.data.ComplicationExperimental
import androidx.wear.watchface.complications.data.ComplicationPersistencePolicies
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.data.NoDataComplicationData
import androidx.wear.watchface.complications.data.toApiComplicationData
import androidx.wear.watchface.complications.data.toWireTypes
import androidx.wear.watchface.control.HeadlessWatchFaceImpl
import androidx.wear.watchface.control.IWatchfaceListener
import androidx.wear.watchface.control.IWatchfaceReadyListener
import androidx.wear.watchface.control.InteractiveInstanceManager
import androidx.wear.watchface.control.InteractiveWatchFaceImpl
import androidx.wear.watchface.control.data.CrashInfoParcel
import androidx.wear.watchface.control.data.HeadlessWatchFaceInstanceParams
import androidx.wear.watchface.control.data.IdTypeAndDefaultProviderPolicyWireFormat
import androidx.wear.watchface.control.data.WallpaperInteractiveWatchFaceInstanceParams
import androidx.wear.watchface.data.ComplicationSlotMetadataWireFormat
import androidx.wear.watchface.data.DeviceConfig
import androidx.wear.watchface.data.IdAndComplicationDataWireFormat
import androidx.wear.watchface.data.WatchUiState
import androidx.wear.watchface.editor.EditorService
import androidx.wear.watchface.style.CurrentUserStyleRepository
import androidx.wear.watchface.style.UserStyle
import androidx.wear.watchface.style.UserStyleData
import androidx.wear.watchface.style.UserStyleFlavors
import androidx.wear.watchface.style.UserStyleSchema
import androidx.wear.watchface.style.UserStyleSetting
import androidx.wear.watchface.style.data.UserStyleFlavorsWireFormat
import androidx.wear.watchface.style.data.UserStyleWireFormat
import androidx.wear.watchface.utility.AsyncTraceEvent
import androidx.wear.watchface.utility.TraceEvent
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.FileDescriptor
import java.io.FileNotFoundException
import java.io.InputStreamReader
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.io.PrintWriter
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.android.asCoroutineDispatcher
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

/**
 * After user code finishes, we need up to 100ms of wake lock holding for the drawing to occur. This
 * isn't the ideal approach, but the framework doesn't expose a callback that would tell us when our
 * Canvas was drawn. 100 ms should give us time for a few frames to be drawn, in case there is a
 * backlog. If we encounter issues with this approach, we should consider asking framework team to
 * expose a callback.
 */
internal const val SURFACE_DRAW_TIMEOUT_MS = 100L

/**
 * WatchFaceService and [WatchFace] are a pair of classes intended to handle much of the boilerplate
 * needed to implement a watch face without being too opinionated. The suggested structure of a
 * WatchFaceService based watch face is:
 *
 * @sample androidx.wear.watchface.samples.kDocCreateExampleWatchFaceService
 *
 * Sub classes of WatchFaceService are expected to implement [createWatchFace] which is the factory
 * for making [WatchFace]s. If the watch faces uses complications then
 * [createComplicationSlotsManager] should be overridden. All [ComplicationSlot]s are assumed to be
 * enumerated up upfront and passed as a collection into [ComplicationSlotsManager]'s constructor
 * which is returned by [createComplicationSlotsManager].
 *
 * Watch face styling (color and visual look of watch face elements such as numeric fonts, watch
 * hands and ticks, etc...) and companion editing is directly supported via [UserStyleSchema] and
 * [UserStyleSetting]. To enable support for styling override [createUserStyleSchema].
 *
 * WatchFaceService can expose pre-populated style presets by overriding [createUserStyleFlavors] or
 * via XML (see below). Each presents represents separate style configured with [UserStyle] and
 * complication slot policies configured with
 * [androidx.wear.watchface.complications.DefaultComplicationDataSourcePolicy]. The system will only
 * access flavors if metadata tag is present in manifest:
 * ```
 *    <meta-data
 *      android:name="androidx.wear.watchface.FLAVORS_SUPPORTED"
 *      android:value="true" />
 * ```
 *
 * WatchFaces are initially constructed on a background thread before being used exclusively on the
 * ui thread afterwards. There is a memory barrier between construction and rendering so no special
 * threading primitives are required.
 *
 * To aid debugging watch face animations, WatchFaceService allows you to speed up or slow down
 * time, and to loop between two instants. This is controlled by MOCK_TIME_INTENT intents with a
 * float extra called "androidx.wear.watchface.extra.MOCK_TIME_SPEED_MULTIPLIE" and to long extras
 * called "androidx.wear.watchface.extra.MOCK_TIME_WRAPPING_MIN_TIME" and
 * "androidx.wear.watchface.extra.MOCK_TIME_WRAPPING_MAX_TIME" (which are UTC time in milliseconds).
 * If minTime is omitted or set to -1 then the current time is sampled as minTime.
 *
 * E.g., to make time go twice as fast: adb shell am broadcast -a androidx.wear.watchface.MockTime \
 * --ef androidx.wear.watchface.extra.MOCK_TIME_SPEED_MULTIPLIER 2.0
 *
 * To use the sample on watch face editor UI, import the wear:wear-watchface-editor-samples library
 * and add the following into your watch face's AndroidManifest.xml:
 * ```
 * <activity
 *   android:name="androidx.wear.watchface.editor.sample.WatchFaceConfigActivity"
 *   android:exported="true"
 *   android:label="Config"
 *   android:theme="@android:style/Theme.Translucent.NoTitleBar">
 *   <intent-filter>
 *     <action android:name="androidx.wear.watchface.editor.action.WATCH_FACE_EDITOR" />
 *     <category android:name=
 *         "com.google.android.wearable.watchface.category.WEARABLE_CONFIGURATION" />
 *     <category android:name="android.intent.category.DEFAULT" />
 *   </intent-filter>
 * </activity>
 * ```
 *
 * To register a WatchFaceService with the system add a <service> tag to the <application> in your
 * watch face's AndroidManifest.xml:
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
 *    <meta-data
 *      android:name=
 *         "com.google.android.wearable.watchface.companionBuiltinConfigurationEnabled"
 *      android:value="true" />
 *  </service>
 * ```
 *
 * Multiple watch faces can be defined in the same package, requiring multiple <service> tags.
 *
 * By default the system will only allow the user to create a single instance of the watch face. You
 * can choose to allow the user to create multiple instances (each with their own styling and a
 * distinct [WatchState.watchFaceInstanceId]) by adding this meta-data to your watch face's
 * manifest:
 * ```
 *    <meta-data
 *      android:name="androidx.wear.watchface.MULTIPLE_INSTANCES_ALLOWED"
 *      android:value="true" />
 * ```
 *
 * A watch face can declare the [UserStyleSchema], [ComplicationSlot]s and [UserStyleFlavors] in
 * XML. The main advantage is simplicity for the developer, however meta data queries (see
 * androidx.wear.watchface.client.WatchFaceMetadataClient) are faster because they can be performed
 * without having to bind to the WatchFaceService.
 *
 * To use xml inflation, add an androidx.wear.watchface.XmlSchemaAndComplicationSlotsDefinition meta
 * date tag to your service:
 * ```
 *     <meta-data
 *         android:name="androidx.wear.watchface.XmlSchemaAndComplicationSlotsDefinition"
 *         android:resource="@xml/my_watchface_definition" />
 * ```
 *
 * And the linked xml/my_watchface_definition resource must contain a XmlWatchFace node. E.g.:
 * ```
 *     <XmlWatchFace xmlns:android="http://schemas.android.com/apk/res/android"
 *             xmlns:app="http://schemas.android.com/apk/res-auto">
 *         <UserStyleSchema>
 *             <ListUserStyleSetting
 *                 android:icon="@drawable/time_style_icon"
 *                 app:affectedWatchFaceLayers="BASE|COMPLICATIONS|COMPLICATIONS_OVERLAY"
 *                 app:defaultOptionIndex="1"
 *                 app:description="@string/time_style_description"
 *                 app:displayName="@string/time_style_name"
 *                 app:id="TimeStyle">
 *                 <ListOption
 *                     android:icon="@drawable/time_style_minimal_icon"
 *                     app:displayName="@string/time_style_minimal_name"
 *                     app:id="minimal" />
 *                 <ListOption
 *                     android:icon="@drawable/time_style_seconds_icon"
 *                     app:displayName="@string/time_style_seconds_name"
 *                     app:id="seconds" />
 *             </ListUserStyleSetting>
 *        </UserStyleSchema>
 *        </ComplicationSlot>
 *             app:slotId="1"
 *             app:boundsType="ROUND_RECT"
 *             app:supportedTypes="SHORT_TEXT|RANGED_VALUE|SMALL_IMAGE"
 *             app:defaultDataSourceType="RANGED_VALUE"
 *             app:systemDataSourceFallback="DATA_SOURCE_WATCH_BATTERY">
 *             <ComplicationSlotBounds
 *                 app:left="0.3" app:top="0.7" app:right="0.7" app:bottom="0.9"/>
 *         </ComplicationSlot>
 *         <UserStyleFlavors>
 *             <UserStyleFlavor app:id="flavor1">
 *                 <StyleOption app:id="TimeStyle" app:value="minimal"/>
 *                 <ComplicationPolicy
 *                     app:slotId="1"
 *                     app:primaryDataSource="com.package/com.app"
 *                     app:primaryDataSourceDefaultType="SHORT_TEXT"
 *                     app:systemDataSourceFallback="DATA_SOURCE_DAY_AND_DATE"
 *                     app:systemDataSourceFallbackDefaultType="SHORT_TEXT"/>
 *             </UserStyleFlavor>
 *         </UserStyleFlavors>
 *    </XmlWatchFace>
 * ```
 *
 * If you use resources references to specify identifiers, they should be locale independent (i.e.
 * translatable="false").
 *
 * If you use XmlSchemaAndComplicationSlotsDefinition then you shouldn't override
 * [createUserStyleSchema] or [createComplicationSlotsManager]. However if <ComplicationSlot> tags
 * are defined then you must override [getComplicationSlotInflationFactory] in order to provide the
 * [CanvasComplicationFactory] and where necessary edge complication [ComplicationTapFilter]s.
 *
 * Note the <ComplicationSlot> tag does not support configExtras because in general a [Bundle] can
 * not be inflated from XML.
 *
 * Note it is an error to define a XmlSchemaAndComplicationSlotsDefinition and not use it.
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

        /**
         * The maximum delay for [awaitDeferredWatchFaceImplThenRunOnUiThreadBlocking] and
         * [awaitDeferredEarlyInitDetailsThenRunOnBinderThread] in milliseconds.
         */
        private const val AWAIT_DEFERRED_TIMEOUT = 10000L

        /** The maximum reasonable wire size for a [UserStyleSchema] in bytes. */
        internal const val MAX_REASONABLE_SCHEMA_WIRE_SIZE_BYTES = 50000

        /** The maximum reasonable wire size for an Icon in a [UserStyleSchema] in pixels. */
        @Px internal const val MAX_REASONABLE_SCHEMA_ICON_WIDTH = 400

        @Px internal const val MAX_REASONABLE_SCHEMA_ICON_HEIGHT = 400

        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        @JvmField
        public val XML_WATCH_FACE_METADATA =
            "androidx.wear.watchface.XmlSchemaAndComplicationSlotsDefinition"

        internal enum class ExecutionThread {
            UI,
            CURRENT
        }

        /**
         * Waits for deferredValue using runBlocking, then executes the task on the thread specified
         * by executionThread param.
         */
        private fun <R, V> awaitDeferredThenRunTaskOnThread(
            engine: EngineWrapper?,
            traceName: String,
            executionThread: ExecutionThread,
            task: (deferredValue: V) -> R,
            waitDeferred: suspend (engine: EngineWrapper) -> V
        ): R? =
            TraceEvent(traceName).use {
                if (Build.TYPE.equals("userdebug")) {
                    Log.d(TAG, "awaitDeferredThenRunTaskOnThread task $traceName")
                }
                if (engine == null) {
                    Log.w(TAG, "Task $traceName posted after close(), ignoring.")
                    return null
                }
                runBlocking {
                    try {
                        withTimeout(AWAIT_DEFERRED_TIMEOUT) {
                            val deferredValue = waitDeferred(engine)
                            when (executionThread) {
                                ExecutionThread.UI -> {
                                    withContext(engine.uiThreadCoroutineScope.coroutineContext) {
                                        task(deferredValue)
                                    }
                                }
                                ExecutionThread.CURRENT -> {
                                    task(deferredValue)
                                }
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Operation $traceName failed", e)
                        throw e
                    }
                }
            }

        internal fun <R> awaitDeferredWatchFaceImplThenRunOnUiThreadBlocking(
            engine: EngineWrapper?,
            traceName: String,
            task: (watchFaceImpl: WatchFaceImpl) -> R
        ): R? =
            awaitDeferredThenRunTaskOnThread(engine, traceName, ExecutionThread.UI, task) {
                it.deferredWatchFaceImpl.await()
            }

        /**
         * During startup tasks will run before those posted by
         * [awaitDeferredWatchFaceImplThenRunOnUiThreadBlocking].
         */
        internal fun <R> awaitDeferredWatchFaceThenRunOnUiThread(
            engine: EngineWrapper?,
            traceName: String,
            task: (watchFace: WatchFace) -> R
        ): R? =
            awaitDeferredThenRunTaskOnThread(engine, traceName, ExecutionThread.UI, task) {
                it.deferredWatchFace.await()
            }

        /**
         * During startup tasks will run before those posted by
         * [awaitDeferredWatchFaceImplThenRunOnUiThreadBlocking] and
         * [awaitDeferredWatchFaceThenRunOnBinderThread].
         */
        internal fun <R> awaitDeferredEarlyInitDetailsThenRunOnThread(
            engine: EngineWrapper?,
            traceName: String,
            executionThread: ExecutionThread,
            task: (earlyInitDetails: EarlyInitDetails) -> R
        ): R? =
            awaitDeferredThenRunTaskOnThread(engine, traceName, executionThread, task) {
                it.deferredEarlyInitDetails.await()
            }
    }

    /**
     * The context used to resolve resources. Unlocks future work.
     *
     */
    protected open val resourcesContext: Context
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) get() = this

    /**
     * Returns the id of the XmlSchemaAndComplicationSlotsDefinition XML resource or 0 if it can't
     * be found.
     *
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @Suppress("DEPRECATION")
    public open fun getXmlWatchFaceResourceId(): Int {
        return try {
            packageManager
                .getServiceInfo(ComponentName(this, javaClass), PackageManager.GET_META_DATA)
                .metaData
                .getInt(XML_WATCH_FACE_METADATA)
        } catch (e: Exception) {
            // If an exception occurs here, we'll ignore it and return 0 meaning it can't be fond.
            0
        }
    }

    private val xmlSchemaAndComplicationSlotsDefinition:
        XmlSchemaAndComplicationSlotsDefinition by lazy {
        val resourceId = getXmlWatchFaceResourceId()
        if (resourceId == 0) {
            XmlSchemaAndComplicationSlotsDefinition(
                schema = null,
                complicationSlots = emptyList(),
                flavors = null
            )
        } else {
            XmlSchemaAndComplicationSlotsDefinition.inflate(resources, resources.getXml(resourceId))
        }
    }

    /**
     * If the WatchFaceService's manifest doesn't define a
     * androidx.wear.watchface.XmlSchemaAndComplicationSlotsDefinition meta data tag then override
     * this factory method to create a non-empty [UserStyleSchema]. A [CurrentUserStyleRepository]
     * constructed with this schema will be passed to [createComplicationSlotsManager],
     * [createUserStyleFlavors] and [createWatchFace]. This is called on a background thread.
     *
     * @return The [UserStyleSchema] to create a [CurrentUserStyleRepository] with, which is passed
     *   to [createComplicationSlotsManager] and [createWatchFace].
     */
    @WorkerThread
    @Suppress("Deprecation") // userStyleSettings
    protected open fun createUserStyleSchema(): UserStyleSchema =
        UserStyleSchema(
            xmlSchemaAndComplicationSlotsDefinition.schema?.userStyleSettings ?: emptyList()
        )

    /**
     * If the WatchFaceService's manifest doesn't define a
     * androidx.wear.watchface.XmlSchemaAndComplicationSlotsDefinition meta data tag then override
     * this factory method to create a non-empty [ComplicationSlotsManager]. This manager will be
     * passed to [createUserStyleFlavors] and [createWatchFace]. This will be called from a
     * background thread but the ComplicationSlotsManager should be accessed exclusively from the
     * UiThread afterwards.
     *
     * @param currentUserStyleRepository The [CurrentUserStyleRepository] constructed using the
     *   [UserStyleSchema] returned by [createUserStyleSchema].
     * @return The [ComplicationSlotsManager] to pass into [createWatchFace].
     */
    @WorkerThread
    protected open fun createComplicationSlotsManager(
        currentUserStyleRepository: CurrentUserStyleRepository
    ): ComplicationSlotsManager =
        if (xmlSchemaAndComplicationSlotsDefinition.complicationSlots.isEmpty())
            ComplicationSlotsManager(emptyList(), currentUserStyleRepository)
        else
            xmlSchemaAndComplicationSlotsDefinition.buildComplicationSlotsManager(
                currentUserStyleRepository,
                getComplicationSlotInflationFactory(currentUserStyleRepository)
            )

    /**
     * Used when inflating [ComplicationSlot]s from XML to provide a
     * [ComplicationSlotInflationFactory] which provides the [CanvasComplicationFactory] and where
     * necessary edge complication [ComplicationTapFilter]s needed for inflating
     * [ComplicationSlot]s.
     *
     * If an androidx.wear.watchface.XmlSchemaAndComplicationSlotsDefinition metadata tag is defined
     * for your WatchFaceService 's manifest, and your XML includes <ComplicationSlot> tags then you
     * must override this method.
     */
    @Deprecated(
        "Use the version with currentUserStyleRepository argument instead",
        ReplaceWith(
            "getComplicationSlotInflationFactory" +
                "(currentUserStyleRepository: CurrentUserStyleRepository)"
        )
    )
    @WorkerThread
    protected open fun getComplicationSlotInflationFactory(): ComplicationSlotInflationFactory? =
        null

    /**
     * Used when inflating [ComplicationSlot]s from XML to provide a
     * [ComplicationSlotInflationFactory] which provides the [CanvasComplicationFactory] and where
     * necessary edge complication [ComplicationTapFilter]s needed for inflating
     * [ComplicationSlot]s.
     *
     * If an androidx.wear.watchface.XmlSchemaAndComplicationSlotsDefinition metadata tag is defined
     * for your WatchFaceService 's manifest, and your XML includes <ComplicationSlot> tags then you
     * must override this method. A [NotImplementedError] exception will be thrown if you don't.
     *
     * @param currentUserStyleRepository The [CurrentUserStyleRepository] constructed using the
     *   [UserStyleSchema] returned by [createUserStyleSchema].
     */
    @Suppress("DEPRECATION")
    @WorkerThread
    protected open fun getComplicationSlotInflationFactory(
        currentUserStyleRepository: CurrentUserStyleRepository
    ): ComplicationSlotInflationFactory =
        getComplicationSlotInflationFactory()
            ?: throw NotImplementedError(
                "You must override WatchFaceService.getComplicationSlotInflationFactory " +
                    "to provide additional details needed to inflate ComplicationSlotsManager"
            )

    /**
     * If the WatchFaceService's manifest doesn't define a
     * androidx.wear.watchface.XmlSchemaAndComplicationSlotsDefinition meta data tag then override
     * this factory method to create non-empty [UserStyleFlavors]. This is called on a background
     * thread. The system reads the flavors once and changes may be ignored until the APK is
     * updated. Metadata tag "androidx.wear.watchface.FLAVORS_SUPPORTED" should be added to let the
     * system know the service supports flavors.
     *
     * @param currentUserStyleRepository The [CurrentUserStyleRepository] constructed using the
     *   [UserStyleSchema] returned by [createUserStyleSchema].
     * @param complicationSlotsManager The [ComplicationSlotsManager] returned by
     *   [createComplicationSlotsManager]
     * @return The [UserStyleFlavors], which is exposed to the system.
     */
    @WorkerThread
    protected open fun createUserStyleFlavors(
        currentUserStyleRepository: CurrentUserStyleRepository,
        complicationSlotsManager: ComplicationSlotsManager
    ): UserStyleFlavors = xmlSchemaAndComplicationSlotsDefinition.flavors ?: UserStyleFlavors()

    /**
     * Override this factory method to create your WatchFaceImpl. This method will be called by the
     * library on a background thread, if possible any expensive initialization should be done
     * asynchronously. The [WatchFace] and its [Renderer] should be accessed exclusively from the
     * UiThread afterwards. There is a memory barrier between construction and rendering so no
     * special threading primitives are required.
     *
     * Warning the system will likely time out waiting for watch face initialization if it takes
     * longer than [MAX_CREATE_WATCHFACE_TIME_MILLIS] milliseconds.
     *
     * @param surfaceHolder The [SurfaceHolder] to pass to the [Renderer]'s constructor.
     * @param watchState The [WatchState] for the watch face.
     * @param complicationSlotsManager The [ComplicationSlotsManager] returned by
     *   [createComplicationSlotsManager].
     * @param currentUserStyleRepository The [CurrentUserStyleRepository] constructed using the
     *   [UserStyleSchema] returned by [createUserStyleSchema].
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
     * Override to force the watchface to be regarded as being visible. This must not be used in
     * production code or significant battery life regressions may occur.
     *
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) open fun forceIsVisibleForTesting() = false

    /* Interface for setting the main thread priority. This exists for testing. */
    internal interface MainThreadPriorityDelegate {
        fun setNormalPriority()

        fun setInteractivePriority()
    }

    internal open fun getMainThreadPriorityDelegate() =
        object : MainThreadPriorityDelegate {
            override fun setNormalPriority() {
                // NB pID is the same as the main thread tID.
                Process.setThreadPriority(Process.myPid(), Process.THREAD_PRIORITY_DEFAULT)
            }

            override fun setInteractivePriority() {
                Process.setThreadPriority(Process.myPid(), Process.THREAD_PRIORITY_DISPLAY)
            }
        }

    /** Used for testing calls to invalidate. */
    internal open fun onInvalidate() {}

    /**
     * Returns the lazily constructed background thread [Handler]. During initialization
     * [createUserStyleSchema], [createComplicationSlotsManager] and [createWatchFace] are posted on
     * this handler.
     */
    public fun getBackgroundThreadHandler(): Handler = getBackgroundThreadHandlerImpl()

    internal var backgroundThread: HandlerThread? = null

    /**
     * Interface for getting the current system time.
     *
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public interface SystemTimeProvider {
        /** Returns the current system time in milliseconds. */
        public fun getSystemTimeMillis(): Long

        /** Returns the current system [ZoneId]. */
        public fun getSystemTimeZoneId(): ZoneId
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public open fun getSystemTimeProvider(): SystemTimeProvider =
        object : SystemTimeProvider {
            override fun getSystemTimeMillis() = System.currentTimeMillis()

            override fun getSystemTimeZoneId() = ZoneId.systemDefault()
        }

    /** This is open for testing. The background thread is used for watch face initialization. */
    internal open fun getBackgroundThreadHandlerImpl(): Handler {
        synchronized(this) {
            if (backgroundThread == null) {
                backgroundThread =
                    HandlerThread(
                            "WatchFaceBackground",
                            Process.THREAD_PRIORITY_FOREGROUND // The user is waiting on WF init.
                        )
                        .apply {
                            uncaughtExceptionHandler =
                                Thread.UncaughtExceptionHandler { _, throwable ->
                                    Log.e(
                                        TAG,
                                        "Uncaught exception on watch face background thread",
                                        throwable
                                    )
                                }
                            start()
                        }
            }
            return Handler(backgroundThread!!.looper)
        }
    }

    /** This is open to allow mocking. */
    internal open fun getMutableWatchState() = MutableWatchState()

    /** This is open for use by tests. */
    internal open fun allowWatchFaceToAnimate() = true

    /**
     * Equivalent to [Build.VERSION.SDK_INT], but allows override for any platform-independent
     * versioning.
     *
     * This is meant to only be used in androidTest, which only support testing on one SDK. In
     * Robolectric tests use `@Config(sdk = [Build.VERSION_CODES.*])`.
     *
     * Note that this cannot override platform-dependent versioning, which means inconsistency.
     */
    @VisibleForTesting internal open val wearPlatformVersion = Build.VERSION.SDK_INT

    /** [Choreographer] isn't supposed to be mocked, so we use a thin wrapper. */
    internal interface ChoreographerWrapper {
        fun postFrameCallback(callback: Choreographer.FrameCallback)
        fun removeFrameCallback(callback: Choreographer.FrameCallback)
    }

    /** This is open to allow mocking. */
    internal open fun getChoreographer(): ChoreographerWrapper =
        object : ChoreographerWrapper {
            private val choreographer = Choreographer.getInstance()

            init {
                require(Looper.myLooper() == Looper.getMainLooper()) {
                    "Creating choreographer not on the main thread"
                }
            }

            override fun postFrameCallback(callback: Choreographer.FrameCallback) {
                choreographer.postFrameCallback(callback)
            }

            override fun removeFrameCallback(callback: Choreographer.FrameCallback) {
                choreographer.removeFrameCallback(callback)
            }
        }

    internal open fun cancelCoroutineScopesInOnDestroy() = true

    /**
     * This is open for use by tests, it allows them to inject a custom [SurfaceHolder].
     *
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
    ): WallpaperInteractiveWatchFaceInstanceParams? =
        TraceEvent("WatchFaceService.readDirectBootPrefs").use {
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
    ): Unit =
        TraceEvent("WatchFaceService.writeDirectBootPrefs").use {
            val directBootContext = context.createDeviceProtectedStorageContext()
            val writer = directBootContext.openFileOutput(fileName, Context.MODE_PRIVATE)
            writer.use { ParcelUtils.toOutputStream(prefs, writer) }
        }

    internal open fun readComplicationDataCacheByteArray(
        context: Context,
        fileName: String
    ): ByteArray? =
        TraceEvent("WatchFaceService.readComplicationCache").use {
            try {
                val directBootContext = context.createDeviceProtectedStorageContext()
                val reader = directBootContext.openFileInput(fileName)
                reader.use { it.readBytes() }
            } catch (e: Exception) {
                null
            }
        }

    internal open fun writeComplicationDataCacheByteArray(
        context: Context,
        fileName: String,
        byteArray: ByteArray
    ) {
        val directBootContext = context.createDeviceProtectedStorageContext()
        val writer = directBootContext.openFileOutput(fileName, Context.MODE_PRIVATE)
        writer.use { writer.write(byteArray) }
    }

    internal fun periodicallyWriteComplicationDataCache(
        context: Context,
        fileName: String,
        complicationsFlow: MutableStateFlow<List<IdAndComplicationDataWireFormat>>
    ) =
        TraceEvent("WatchFaceService.writeComplicationCache").use {
            val backgroundThreadCoroutineScope =
                CoroutineScope(getBackgroundThreadHandler().asCoroutineDispatcher().immediate)
            backgroundThreadCoroutineScope.launch {
                complicationsFlow.collect { complicationDataWireFormats ->
                    try {
                        // The combination of 'collect' which conflates the updates and adding a
                        // delay
                        // here ensures that we write updates at least 1 second apart. The delay is
                        // at
                        // the beginning to delay writes during WF init.
                        delay(1000)
                        val stream = ByteArrayOutputStream()
                        val objectOutputStream = ObjectOutputStream(stream)
                        objectOutputStream.writeInt(complicationDataWireFormats.size)
                        for (wireData in complicationDataWireFormats) {
                            objectOutputStream.writeInt(wireData.id)
                            objectOutputStream.writeObject(
                                if (
                                    (wireData.complicationData.persistencePolicy and
                                        ComplicationPersistencePolicies.DO_NOT_PERSIST) != 0
                                ) {
                                    NoDataComplicationData().asWireComplicationData()
                                } else {
                                    wireData.complicationData
                                }
                            )
                        }
                        objectOutputStream.close()
                        val byteArray = stream.toByteArray()
                        writeComplicationDataCacheByteArray(context, fileName, byteArray)
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to write to complication cache due to exception", e)
                    }
                }
            }
        }

    internal fun readComplicationDataCache(
        context: Context,
        fileName: String
    ): List<IdAndComplicationDataWireFormat>? =
        TraceEvent("WatchFaceService.readComplicationCache").use {
            return readComplicationDataCacheByteArray(context, fileName)?.let {
                try {
                    val objectInputStream = ObjectInputStream(ByteArrayInputStream(it))
                    val complicationData = ArrayList<IdAndComplicationDataWireFormat>()
                    val numComplications = objectInputStream.readInt()
                    for (i in 0 until numComplications) {
                        val id = objectInputStream.readInt()
                        val wireFormatComplication =
                            (objectInputStream.readObject() as WireComplicationData)
                        complicationData.add(
                            IdAndComplicationDataWireFormat(id, wireFormatComplication)
                        )
                        Log.d(TAG, "Read cached complication $id = $wireFormatComplication")
                    }
                    objectInputStream.close()
                    complicationData
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to read to complication cache due to exception", e)
                    null
                }
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
            for ((key, value) in style) {
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
                        complicationSlotId,
                        fallbackSystemDataSource,
                        type
                    )
                }

                if (dataSources != null) {
                    // Iterate in reverse order. This could be O(n^2) but n is expected to be small
                    // and the list is probably an ArrayList so it's probably O(n) in practice.
                    for (i in dataSources.size - 1 downTo 0) {
                        iWatchFaceService.setDefaultComplicationProvider(
                            complicationSlotId,
                            dataSources[i],
                            type
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
                    complicationSlotIds,
                    /* updateAll= */ !complicationsActivated
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
                ),
                fromSysUi = true
            )

            pendingBackgroundAction = null
        }

        @Suppress("DEPRECATION")
        fun onComplicationSlotDataUpdate(extras: Bundle) {
            extras.classLoader = WireComplicationData::class.java.classLoader
            val complicationData =
                extras.getParcelable<WireComplicationData>(Constants.EXTRA_COMPLICATION_DATA)!!
            val complicationSlotId = extras.getInt(Constants.EXTRA_COMPLICATION_ID)
            engineWrapper.setComplicationDataList(
                listOf(IdAndComplicationDataWireFormat(complicationSlotId, complicationData))
            )
        }

        fun onSetBinder(extras: Bundle) {
            val binder = extras.getBinder(Constants.EXTRA_BINDER)
            if (binder == null) {
                Log.w(TAG, "Binder is null.")
                return
            }

            iWatchFaceService = IWatchFaceService.Stub.asInterface(binder)

            // A ParameterlessEngine doesn't exist in WSL flow.
            InteractiveInstanceManager.setParameterlessEngine(null)

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

        private suspend fun maybeCreateWatchFace(): Unit =
            TraceEvent("EngineWrapper.maybeCreateWatchFace").use {
                // To simplify handling of watch face state, we only construct the [WatchFaceImpl]
                // once iWatchFaceService have been initialized and pending properties sent.
                if (
                    iWatchFaceServiceInitialized() &&
                        pendingProperties != null &&
                        !engineWrapper.watchFaceCreatedOrPending()
                ) {
                    watchFaceInitStarted = true

                    // Apply immutable properties to mutableWatchState before creating the watch
                    // face.
                    onPropertiesChanged(pendingProperties!!)
                    pendingProperties = null

                    val watchState = engineWrapper.mutableWatchState.asWatchState()
                    engineWrapper.createWatchFaceInternal(watchState, null, "maybeCreateWatchFace")

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
                    watchFaceImpl.complicationSlotsManager.onComplicationsUpdated()
                }
            }
    }

    internal class EarlyInitDetails(
        val complicationSlotsManager: ComplicationSlotsManager,
        val userStyleRepository: CurrentUserStyleRepository,
        val userStyleFlavors: UserStyleFlavors
    )

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @OptIn(WatchFaceExperimental::class)
    public inner class EngineWrapper(
        private val uiThreadHandler: Handler,
        private val backgroundThreadHandler: Handler,
        headless: Boolean
    ) :
        WallpaperService.Engine(),
        WatchFaceHostApi,
        AccessibilityManager.AccessibilityStateChangeListener {
        internal val backgroundThreadCoroutineScope =
            CoroutineScope(backgroundThreadHandler.asCoroutineDispatcher().immediate)

        internal val uiThreadCoroutineScope =
            CoroutineScope(uiThreadHandler.asCoroutineDispatcher().immediate)

        private val _context = this@WatchFaceService as Context

        // State to support the old WSL style interface
        internal val wslFlow = WslFlow(this)

        /**
         * [deferredEarlyInitDetails] will complete before [deferredWatchFace] and
         * [deferredWatchFaceImpl].
         */
        internal var deferredEarlyInitDetails = CompletableDeferred<EarlyInitDetails>()

        /** [deferredWatchFace] will complete before [deferredWatchFaceImpl]. */
        internal var deferredWatchFace = CompletableDeferred<WatchFace>()

        /** [deferredWatchFaceImpl] will complete after [deferredWatchFace]. */
        @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        public val deferredWatchFaceImpl = CompletableDeferred<WatchFaceImpl>()

        @VisibleForTesting public var deferredValidation = CompletableDeferred<Unit>()

        /**
         * [deferredSurfaceHolder] will complete after [onSurfaceChanged], before then it's not safe
         * to create a UiThread OpenGL context.
         */
        private var deferredSurfaceHolder = CompletableDeferred<SurfaceHolder>()

        private val pendingUpdateTime: CancellableUniqueTask =
            CancellableUniqueTask(getUiThreadHandler())

        internal val mutableWatchState =
            getMutableWatchState().apply {
                isVisible.value = this@EngineWrapper.isVisible || forceIsVisibleForTesting()
                // Watch faces with the old [onSetBinder] init flow don't know whether the system
                // is ambient until they have received a background action wallpaper command.
                // That's supposed to get sent very quickly, but in case it doesn't we initially
                // assume we're not in ambient mode which should be correct most of the time.
                isAmbient.value = false
                isHeadless = headless
                isLocked.value =
                    (getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager).isDeviceLocked
            }

        /**
         * Whether or not we allow watch faces to animate. In some tests or for headless rendering
         * (for remote config) we don't want this.
         */
        internal var allowWatchfaceToAnimate = allowWatchFaceToAnimate()

        internal var destroyed = false
        internal var surfaceDestroyed = false
        internal var systemViewOfContentDescriptionLabelsIsStale = false

        internal lateinit var ambientUpdateWakelock: PowerManager.WakeLock

        private lateinit var choreographer: ChoreographerWrapper
        override val systemTimeProvider = getSystemTimeProvider()
        override val wearSdkVersion = this@WatchFaceService.wearPlatformVersion

        /**
         * Whether we already have a [frameCallback] posted and waiting in the [Choreographer]
         * queue. This protects us from drawing multiple times in a single frame.
         */
        private var frameCallbackPending = false

        private val frameCallback =
            object : Choreographer.FrameCallback {
                @SuppressWarnings("SyntheticAccessor")
                override fun doFrame(frameTimeNs: Long) {
                    if (destroyed) {
                        return
                    }
                    require(allowWatchfaceToAnimate) {
                        "Choreographer doFrame called but allowWatchfaceToAnimate is false"
                    }
                    frameCallbackPending = false

                    val watchFaceImpl: WatchFaceImpl? = getWatchFaceImplOrNull()

                    /**
                     * It's possible we went ambient by the time our callback occurred in which case
                     * there's no point drawing.
                     */
                    if (watchFaceImpl?.renderer?.shouldAnimate() != false) {
                        draw(watchFaceImpl)
                    }
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
        internal var systemHasSentWatchUiState = false

        private var asyncWatchFaceConstructionPending = false

        @VisibleForTesting
        internal val complicationsFlow =
            MutableStateFlow<List<IdAndComplicationDataWireFormat>>(emptyList())

        private var initialUserStyle: UserStyleWireFormat? = null
        internal lateinit var interactiveInstanceId: String

        private var createdBy = "?"

        private val mainThreadPriorityDelegate = getMainThreadPriorityDelegate()

        private val lock = Any()

        /** All members after this are protected by [lock]. */
        private val listeners = RemoteCallbackList<IWatchfaceListener>()
        private var lastWatchFaceColors: WatchFaceColors? = null
        private var lastPreviewImageNeedsUpdateRequest: String? = null

        /**
         * Returns the [WatchFaceImpl] if [deferredWatchFaceImpl] has completed successfully or
         * `null` otherwise. Throws exception if there were problems with watchface validation.
         */
        internal fun getWatchFaceImplOrNull(): WatchFaceImpl? {
            if (deferredValidation.isCompleted) {
                runBlocking {
                    // if validation fails exception will be thrown here
                    deferredValidation.await()
                }
            }

            return if (deferredWatchFaceImpl.isCompleted) {
                runBlocking { deferredWatchFaceImpl.await() }
            } else {
                null
            }
        }

        /**
         * Returns the [EarlyInitDetails] if [deferredEarlyInitDetails] has completed successfully
         * or `null` otherwise.
         */
        internal fun getEarlyInitDetailsOrNull(): EarlyInitDetails? {
            return if (deferredEarlyInitDetails.isCompleted) {
                runBlocking { deferredEarlyInitDetails.await() }
            } else {
                null
            }
        }

        init {
            maybeCreateWCSApi()
        }

        /** Note this function should only be called once. */
        @SuppressWarnings("NewApi")
        @UiThread
        private fun maybeCreateWCSApi(): Unit =
            TraceEvent("EngineWrapper.maybeCreateWCSApi").use {
                // If this is a headless instance then we don't want to create a WCS instance.
                if (mutableWatchState.isHeadless) {
                    return
                }

                var pendingWallpaperInstance =
                    InteractiveInstanceManager.takePendingWallpaperInteractiveWatchFaceInstance()

                // In a direct boot scenario attempt to load the previously serialized parameters.
                if (pendingWallpaperInstance == null && wearSdkVersion >= Build.VERSION_CODES.R) {
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
                                .takePendingWallpaperInteractiveWatchFaceInstance()
                                ?.let {
                                    require(it.params.instanceId == params.instanceId) {
                                        "Mismatch between pendingWallpaperInstance id " +
                                            "${it.params.instanceId} and constructed instance id " +
                                            params.instanceId
                                    }
                                    it.callback.onInteractiveWatchFaceCreated(instance)
                                }
                        } catch (e: Exception) {
                            InteractiveInstanceManager
                                .takePendingWallpaperInteractiveWatchFaceInstance()
                                ?.let {
                                    Log.e(
                                        TAG,
                                        "takePendingWallpaperInteractiveWatchFaceInstance failed",
                                        e
                                    )
                                    it.callback.onInteractiveWatchFaceCrashed(CrashInfoParcel(e))
                                }
                        } finally {
                            asyncTraceEvent.close()
                        }

                        return
                    }
                }

                if (pendingWallpaperInstance == null) {
                    // In this case we don't have any watchface parameters, probably because a WSL
                    // watchface has been upgraded to an AndroidX one. The system has either just
                    // racily attempted to connect (in which case we should carry on normally) or it
                    // probably will connect at a later time. In the latter case we should
                    // register a parameterless engine to allow the subsequent connection to
                    // succeed.
                    pendingWallpaperInstance = InteractiveInstanceManager
                        .setParameterlessEngineOrTakePendingWallpaperInteractiveWatchFaceInstance(
                            this
                        )
                }

                // If there's a pending WallpaperInteractiveWatchFaceInstance then create it.
                if (pendingWallpaperInstance != null) {
                    val asyncTraceEvent =
                        AsyncTraceEvent("Create PendingWallpaperInteractiveWatchFaceInstance")
                    val instance: InteractiveWatchFaceImpl? =
                        try {
                            val instance =
                                createInteractiveInstance(
                                    pendingWallpaperInstance.params,
                                    "Boot with pendingWallpaperInstance"
                                )
                            pendingWallpaperInstance.callback.onInteractiveWatchFaceCreated(
                                instance
                            )
                            instance
                        } catch (e: Exception) {
                            Log.e(TAG, "createInteractiveInstance failed", e)
                            pendingWallpaperInstance.callback.onInteractiveWatchFaceCrashed(
                                CrashInfoParcel(e)
                            )
                            null
                        }
                    asyncTraceEvent.close()
                    val params = pendingWallpaperInstance.params
                    directBootParams = params

                    // Writing even small amounts of data to storage is quite slow and if we did
                    // that immediately, we'd delay the first frame which is rendered via
                    // onSurfaceRedrawNeeded. By posting this task we expedite first frame
                    // rendering. There is a small window where the direct boot could be stale if
                    // the watchface crashed but this seems unlikely in practice.
                    backgroundThreadCoroutineScope.launch {
                        // Wait for init to complete before writing the direct boot prefs, or we
                        // might
                        // sneak in before higher priority init tasks.
                        instance?.engine?.deferredWatchFaceImpl?.await()

                        // We don't want to display complications in direct boot mode so replace
                        // with an
                        // empty list. NB we can't actually serialise complications anyway so that's
                        // just as well...
                        params.idAndComplicationDataWireFormats = emptyList()

                        writeDirectBootPrefs(_context, DIRECT_BOOT_PREFS, params)
                    }
                }
            }

        /** Attaches to a parameterlessEngine if we're completely uninitialized. */
        @SuppressWarnings("NewApi")
        internal fun attachToParameterlessEngine(
            pendingWallpaperInstance:
            InteractiveInstanceManager.PendingWallpaperInteractiveWatchFaceInstance
        ) {
            uiThreadCoroutineScope.launch {
                try {
                    pendingWallpaperInstance.callback.onInteractiveWatchFaceCreated(
                        createInteractiveInstance(
                            pendingWallpaperInstance.params,
                            "attachToParameterlessEngine"
                        )
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "attachToParameterlessEngine failed", e)
                    pendingWallpaperInstance.callback.onInteractiveWatchFaceCrashed(
                        CrashInfoParcel(e)
                    )
                }
            }
        }

        @UiThread
        internal fun ambientTickUpdate(): Unit =
            TraceEvent("EngineWrapper.ambientTickUpdate").use {
                if (mutableWatchState.isAmbient.value!!) {
                    ambientUpdateWakelock.acquire(SURFACE_DRAW_TIMEOUT_MS)
                    try {
                        // It's unlikely an ambient tick would be sent to a watch face that hasn't
                        // loaded yet (if that did happen then draw would be a NOP). The watch face
                        // will
                        // render at least once upon loading so we don't need to do anything special
                        // here.
                        val watchFaceImpl: WatchFaceImpl? = getWatchFaceImplOrNull()
                        draw(watchFaceImpl)
                    } catch (t: Throwable) {
                        Log.e(TAG, "ambientTickUpdate failed", t)
                    } finally {
                        ambientUpdateWakelock.release()
                    }
                }
            }

        @UiThread
        internal fun setWatchUiState(watchUiState: WatchUiState, fromSysUi: Boolean) {
            if (
                firstSetWatchUiState ||
                    watchUiState.inAmbientMode != mutableWatchState.isAmbient.value
            ) {
                mutableWatchState.isAmbient.value = watchUiState.inAmbientMode
            }

            if (
                firstSetWatchUiState ||
                    watchUiState.interruptionFilter != mutableWatchState.interruptionFilter.value
            ) {
                mutableWatchState.interruptionFilter.value = watchUiState.interruptionFilter
            }

            firstSetWatchUiState = false

            if (fromSysUi) {
                systemHasSentWatchUiState = true
                getWatchFaceImplOrNull()?.broadcastsObserver?.onSysUiHasSentWatchUiState()
            }
        }

        fun setUserStyle(userStyle: UserStyleWireFormat): Unit =
            TraceEvent("EngineWrapper.setUserStyle").use {
                uiThreadCoroutineScope.launch {
                    try {
                        setUserStyleImpl(
                            deferredEarlyInitDetails.await().userStyleRepository,
                            userStyle
                        )
                    } catch (e: Exception) {
                        Log.e(TAG, "setUserStyle failed", e)
                        throw e
                    }
                }
            }

        @UiThread
        private fun setUserStyleImpl(
            currentUserStyleRepository: CurrentUserStyleRepository,
            userStyle: UserStyleWireFormat
        ) {
            currentUserStyleRepository.updateUserStyle(
                UserStyle(UserStyleData(userStyle), currentUserStyleRepository.schema)
            )

            // Update direct boot params if we have any.
            val params = directBootParams ?: return
            val currentStyle = currentUserStyleRepository.userStyle.value.toWireFormat()
            if (params.userStyle.equals(currentStyle)) {
                return
            }
            params.userStyle = currentStyle
            // We don't want to display complications in direct boot mode so replace with an empty
            // list. NB we can't actually serialise complications anyway so that's just as well...
            params.idAndComplicationDataWireFormats = emptyList()

            // Let wallpaper manager know the wallpaper has changed.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                NotifyColorsChangedHelper.notifyColorsChanged(this)
            }

            backgroundThreadCoroutineScope.launch {
                writeDirectBootPrefs(_context, DIRECT_BOOT_PREFS, params)
            }
        }

        /** This can be called on any thread. */
        @UiThread
        internal suspend fun addWatchfaceReadyListener(listener: IWatchfaceReadyListener) {
            deferredWatchFaceImpl.await()
            try {
                listener.onWatchfaceReady()
            } catch (e: Exception) {
                Log.e(TAG, "listener.onWatchfaceReady failed", e)
            }
        }

        @UiThread
        internal fun setImmutableSystemState(deviceConfig: DeviceConfig) {
            // These properties never change so set them once only.
            if (!immutableSystemStateDone) {
                mutableWatchState.hasLowBitAmbient = deviceConfig.hasLowBitAmbient
                mutableWatchState.hasBurnInProtection = deviceConfig.hasBurnInProtection
                mutableWatchState.analogPreviewReferenceTimeMillis =
                    deviceConfig.analogPreviewReferenceTimeMillis
                mutableWatchState.digitalPreviewReferenceTimeMillis =
                    deviceConfig.digitalPreviewReferenceTimeMillis

                immutableSystemStateDone = true
            }
        }

        @AnyThread
        internal fun setComplicationDataList(
            complicationDataWireFormats: List<IdAndComplicationDataWireFormat>
        ): Unit =
            TraceEvent("EngineWrapper.setComplicationDataList").use {
                complicationsFlow.update { base ->
                    // We need to merge the updates.
                    val complicationUpdateMap =
                        base.associate { Pair(it.id, it.complicationData) }.toMutableMap()
                    for (data in complicationDataWireFormats) {
                        complicationUpdateMap[data.id] = data.complicationData
                    }
                    complicationUpdateMap.map { IdAndComplicationDataWireFormat(it.key, it.value) }
                }
            }

        @WorkerThread
        private fun listenForComplicationChanges(
            complicationSlotsManager: ComplicationSlotsManager
        ) {
            // Add a listener so we can track changes and automatically apply them on the UIThread
            uiThreadCoroutineScope.launch {
                complicationsFlow.collect { complicationDataWireFormats ->
                    val now = Instant.ofEpochMilli(systemTimeProvider.getSystemTimeMillis())
                    for (idAndComplicationData in complicationDataWireFormats) {
                        complicationSlotsManager.onComplicationDataUpdate(
                            idAndComplicationData.id,
                            idAndComplicationData.complicationData.toApiComplicationData(),
                            now
                        )
                    }
                    complicationSlotsManager.onComplicationsUpdated()
                    invalidate()
                }
            }
        }

        @UiThread
        internal suspend fun updateInstance(newInstanceId: String) {
            val watchFaceImpl = deferredWatchFaceImpl.await()
            // If the favorite ID has changed then the complications are probably invalid.
            setComplicationDataList(
                watchFaceImpl.complicationSlotsManager.complicationSlots.map {
                    IdAndComplicationDataWireFormat(
                        it.key,
                        NoDataComplicationData().asWireComplicationData()
                    )
                }
            )

            // However we may have valid complications cached.
            readComplicationDataCache(_context, newInstanceId)?.let {
                this.setComplicationDataList(it)
            }

            InteractiveInstanceManager.renameInstance(interactiveInstanceId, newInstanceId)
            interactiveInstanceId = newInstanceId
            mutableWatchState.watchFaceInstanceId.value = sanitizeWatchFaceId(newInstanceId)
        }

        override fun getContext(): Context = _context

        override fun getUiThreadHandler(): Handler = uiThreadHandler

        override fun getUiThreadCoroutineScope(): CoroutineScope = uiThreadCoroutineScope

        override fun getBackgroundThreadHandler(): Handler = backgroundThreadHandler

        override fun onCreate(holder: SurfaceHolder): Unit =
            TraceEvent("EngineWrapper.onCreate").use {
                super.onCreate(holder)
                ambientUpdateWakelock =
                    (getSystemService(Context.POWER_SERVICE) as PowerManager).newWakeLock(
                        PowerManager.PARTIAL_WAKE_LOCK,
                        "$TAG:[AmbientUpdate]"
                    )
                // Disable reference counting for our wake lock so that we can use the same wake
                // lock
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
                            // We can sometimes get this callback before the watchface has been
                            // created
                            // in which case it's safe to drop it.
                            if (deferredWatchFaceImpl.isCompleted) {
                                invalidate()
                            }
                        }

                        override fun surfaceDestroyed(holder: SurfaceHolder) {}

                        override fun surfaceCreated(holder: SurfaceHolder) {}
                    }
                )
            }

        override fun onSurfaceChanged(
            holder: SurfaceHolder,
            format: Int,
            width: Int,
            height: Int
        ): Unit =
            TraceEvent("EngineWrapper.onSurfaceChanged").use {
                super.onSurfaceChanged(holder, format, width, height)
                deferredSurfaceHolder.complete(holder)
            }

        override fun onApplyWindowInsets(insets: WindowInsets?): Unit =
            TraceEvent("EngineWrapper.onApplyWindowInsets").use {
                super.onApplyWindowInsets(insets)
                @Px
                val chinHeight =
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
        override fun onDestroy(): Unit =
            TraceEvent("EngineWrapper.onDestroy").use {
                super.onDestroy()
                pendingUpdateTime.cancel()
                if (!mutableWatchState.isHeadless) {
                    mainThreadPriorityDelegate.setNormalPriority()
                }

                destroyed = true
                backgroundThreadCoroutineScope.cancel()
                quitBackgroundThreadIfCreated()
                uiThreadHandler.removeCallbacks(invalidateRunnable)
                if (this::choreographer.isInitialized) {
                    choreographer.removeFrameCallback(frameCallback)
                }
                if (this::interactiveInstanceId.isInitialized) {
                    InteractiveInstanceManager.deleteInstance(interactiveInstanceId)
                }
                stopListeningForAccessibilityStateChanges()

                // NB user code could throw an exception so do this last.
                try {
                    runBlocking {
                        // The WatchFaceImpl is created on the UiThread so if we get here and it's
                        // not
                        // created we can be sure it'll never be created hence we don't need to
                        // destroy
                        // it.
                        if (deferredWatchFaceImpl.isCompleted) {
                            deferredWatchFaceImpl.await().onDestroy()
                        } else if (deferredWatchFace.isCompleted) {
                            // However we should destroy the renderer if its been created.
                            deferredWatchFace.await().renderer.onDestroy()
                        }
                    }
                } catch (e: Exception) {
                    // Throwing an exception here leads to a cascade of errors, log instead.
                    Log.e(
                        TAG,
                        "WatchFace exception observed in onDestroy (may have occurred during init)",
                        e
                    )
                } finally {
                    if (this@EngineWrapper::ambientUpdateWakelock.isInitialized) {
                        // Make sure the WakeLock doesn't retain the WatchFaceService.
                        ambientUpdateWakelock.release()
                    }

                    // StateFlows may retain WatchFaceService via the coroutineScope. Call cancel to
                    // ensure resources are released. Headless watch faces call
                    // cancelCoroutineScopes
                    // themselves since they call onDestroy from a coroutine context.
                    if (cancelCoroutineScopesInOnDestroy() && !mutableWatchState.isHeadless) {
                        cancelCoroutineScopes()
                    }
                }
            }

        internal fun cancelCoroutineScopes() {
            uiThreadCoroutineScope.cancel()
            backgroundThreadCoroutineScope.cancel()
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder) {
            surfaceDestroyed = true
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
            if (wearSdkVersion >= Build.VERSION_CODES.R) {
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
                        val watchFaceImpl = deferredWatchFaceImpl.await()
                        watchFaceImpl.onTapCommand(
                            TapType.UP,
                            TapEvent(
                                x,
                                y,
                                Instant.ofEpochMilli(systemTimeProvider.getSystemTimeMillis())
                            )
                        )
                    }
                Constants.COMMAND_TOUCH ->
                    uiThreadCoroutineScope.runBlockingWithTracing("onCommand COMMAND_TOUCH") {
                        val watchFaceImpl = deferredWatchFaceImpl.await()
                        watchFaceImpl.onTapCommand(
                            TapType.DOWN,
                            TapEvent(
                                x,
                                y,
                                Instant.ofEpochMilli(systemTimeProvider.getSystemTimeMillis())
                            )
                        )
                    }
                Constants.COMMAND_TOUCH_CANCEL ->
                    uiThreadCoroutineScope.runBlockingWithTracing(
                        "onCommand COMMAND_TOUCH_CANCEL"
                    ) {
                        val watchFaceImpl = deferredWatchFaceImpl.await()
                        watchFaceImpl.onTapCommand(
                            TapType.CANCEL,
                            TapEvent(
                                x,
                                y,
                                Instant.ofEpochMilli(systemTimeProvider.getSystemTimeMillis())
                            )
                        )
                    }
                else -> {}
            }
            return null
        }

        override fun getInitialUserStyle(): UserStyleWireFormat? = initialUserStyle

        /** This will be called from a binder thread. */
        @WorkerThread
        internal fun getDefaultProviderPolicies(): Array<IdTypeAndDefaultProviderPolicyWireFormat> {
            return createComplicationSlotsManager(
                    CurrentUserStyleRepository(createUserStyleSchema())
                )
                .getDefaultProviderPolicies()
        }

        /** This will be called from a binder thread. */
        @WorkerThread
        internal fun getUserStyleSchemaWireFormat() = createUserStyleSchema().toWireFormat()

        /** This will be called from a binder thread. */
        @WorkerThread
        internal fun getUserStyleFlavorsWireFormat(): UserStyleFlavorsWireFormat {
            val currentUserStyleRepository = CurrentUserStyleRepository(createUserStyleSchema())
            return createUserStyleFlavors(
                    currentUserStyleRepository,
                    createComplicationSlotsManager(currentUserStyleRepository)
                )
                .toWireFormat()
        }

        /** This will be called from a binder thread. */
        @OptIn(ComplicationExperimental::class)
        @WorkerThread
        internal fun getComplicationSlotMetadataWireFormats() =
            createComplicationSlotsManager(CurrentUserStyleRepository(createUserStyleSchema()))
                .complicationSlots
                .map {
                    val systemDataSourceFallbackDefaultType =
                        it.value.defaultDataSourcePolicy.systemDataSourceFallbackDefaultType
                            .toWireComplicationType()
                    ComplicationSlotMetadataWireFormat(
                        it.key,
                        it.value.complicationSlotBounds.perComplicationTypeBounds.keys
                            .map { it.toWireComplicationType() }
                            .toIntArray(),
                        it.value.complicationSlotBounds.perComplicationTypeBounds.values
                            .toTypedArray(),
                        it.value.complicationSlotBounds.perComplicationTypeMargins.values.toList(),
                        it.value.boundsType,
                        it.value.supportedTypes.toWireTypes(),
                        it.value.defaultDataSourcePolicy.dataSourcesAsList(),
                        it.value.defaultDataSourcePolicy.systemDataSourceFallback,
                        systemDataSourceFallbackDefaultType,
                        it.value.defaultDataSourcePolicy.primaryDataSourceDefaultType
                            ?.toWireComplicationType()
                            ?: systemDataSourceFallbackDefaultType,
                        it.value.defaultDataSourcePolicy.secondaryDataSourceDefaultType
                            ?.toWireComplicationType()
                            ?: systemDataSourceFallbackDefaultType,
                        it.value.initiallyEnabled,
                        it.value.fixedComplicationDataSource,
                        it.value.configExtras,
                        it.value.boundingArc?.toWireFormat()
                    )
                }
                .toTypedArray()

        @RequiresApi(27)
        internal fun createHeadlessInstance(
            params: HeadlessWatchFaceInstanceParams
        ): HeadlessWatchFaceImpl =
            TraceEvent("EngineWrapper.createHeadlessInstance").use {
                require(!watchFaceCreatedOrPending()) {
                    "WatchFace already exists! Created by $createdBy"
                }
                setImmutableSystemState(params.deviceConfig)

                // Fake SurfaceHolder with just enough methods implemented for headless rendering.
                val fakeSurfaceHolder =
                    object : SurfaceHolder {
                        val callbacks = HashSet<SurfaceHolder.Callback>()

                        @Deprecated(
                            message =
                                "this is ignored, this value is set automatically when needed."
                        )
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
                mutableWatchState.watchFaceInstanceId.value = sanitizeWatchFaceId(params.instanceId)
                val watchState = mutableWatchState.asWatchState()

                createWatchFaceInternal(watchState, fakeSurfaceHolder, "createHeadlessInstance")

                mutableWatchState.isVisible.value = true
                mutableWatchState.isAmbient.value = false
                return HeadlessWatchFaceImpl(this, this@WatchFaceService)
            }

        @UiThread
        @RequiresApi(27)
        internal fun createInteractiveInstance(
            params: WallpaperInteractiveWatchFaceInstanceParams,
            _createdBy: String
        ): InteractiveWatchFaceImpl =
            TraceEvent("EngineWrapper.createInteractiveInstance").use {
                mainThreadPriorityDelegate.setInteractivePriority()

                require(!watchFaceCreatedOrPending()) {
                    "WatchFace already exists! Created by $createdBy"
                }
                require(!mutableWatchState.isHeadless)

                setImmutableSystemState(params.deviceConfig)
                setWatchUiState(params.watchUiState, fromSysUi = false)
                initialUserStyle = params.userStyle

                mutableWatchState.watchFaceInstanceId.value = sanitizeWatchFaceId(params.instanceId)
                val watchState = mutableWatchState.asWatchState()

                // Store the initial complications, this could be modified by new data before being
                // applied.
                var initialComplications = params.idAndComplicationDataWireFormats
                if (initialComplications.isNullOrEmpty()) {
                    initialComplications = readComplicationDataCache(_context, params.instanceId)
                }
                if (!initialComplications.isNullOrEmpty()) {
                    Log.d(TAG, "Initial complications for ${params.instanceId}")
                    for (idAndComplication in initialComplications) {
                        Log.d(
                            TAG,
                            "${idAndComplication.id} = ${idAndComplication.complicationData}"
                        )
                    }
                    setComplicationDataList(initialComplications)
                } else {
                    Log.d(TAG, "No initial complications for ${params.instanceId}")
                }

                createWatchFaceInternal(watchState, getWallpaperSurfaceHolderOverride(), _createdBy)

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
            Log.d(TAG, "createInstance id ${watchState.watchFaceInstanceId.value} $_createdBy")
            asyncWatchFaceConstructionPending = true
            createdBy = _createdBy

            // In case of overrideSurfaceHolder provided (tests) return its size instead of real
            // metrics.
            screenBounds =
                if (overrideSurfaceHolder != null) {
                    overrideSurfaceHolder.surfaceFrame
                } else {
                    val displayMetrics = resources.displayMetrics
                    Rect(0, 0, displayMetrics.widthPixels, displayMetrics.heightPixels)
                }

            backgroundThreadCoroutineScope.launch {
                val timeBefore = System.currentTimeMillis()
                val currentUserStyleRepository =
                    TraceEvent("WatchFaceService.createUserStyleSchema").use {
                        CurrentUserStyleRepository(createUserStyleSchema())
                    }
                initStyle(currentUserStyleRepository)

                val complicationSlotsManager =
                    TraceEvent("WatchFaceService.createComplicationsManager").use {
                        createComplicationSlotsManager(currentUserStyleRepository)
                    }
                complicationSlotsManager.watchFaceHostApi = this@EngineWrapper
                complicationSlotsManager.watchState = watchState
                complicationSlotsManager.listenForStyleChanges(uiThreadCoroutineScope)
                listenForComplicationChanges(complicationSlotsManager)
                if (!watchState.isHeadless) {
                    periodicallyWriteComplicationDataCache(
                        _context,
                        watchState.watchFaceInstanceId.value,
                        complicationsFlow
                    )
                }

                val userStyleFlavors =
                    TraceEvent("WatchFaceService.createUserStyleFlavors").use {
                        createUserStyleFlavors(currentUserStyleRepository, complicationSlotsManager)
                    }

                deferredEarlyInitDetails.complete(
                    EarlyInitDetails(
                        complicationSlotsManager,
                        currentUserStyleRepository,
                        userStyleFlavors
                    )
                )

                val deferredWatchFace = CompletableDeferred<WatchFace>()
                val initComplicationsDone = CompletableDeferred<Unit>()

                // WatchFaceImpl (which registers broadcast observers) needs to be constructed
                // on the UIThread. Part of this process can be done in parallel with
                // createWatchFace.
                uiThreadCoroutineScope.launch {
                    createWatchFaceImpl(
                        complicationSlotsManager,
                        currentUserStyleRepository,
                        deferredWatchFace,
                        initComplicationsDone,
                        watchState
                    )
                }

                try {
                    val surfaceHolder = overrideSurfaceHolder ?: deferredSurfaceHolder.await()

                    val watchFace =
                        TraceEvent("WatchFaceService.createWatchFace").use {
                            // Note by awaiting deferredSurfaceHolder we ensure onSurfaceChanged has
                            // been called and we're passing the correct updated surface holder.
                            // This is
                            // important for GL rendering.
                            createWatchFace(
                                surfaceHolder,
                                watchState,
                                complicationSlotsManager,
                                currentUserStyleRepository
                            )
                        }
                    this@EngineWrapper.deferredWatchFace.complete(watchFace)

                    watchFace.renderer.backgroundThreadInitInternal()

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
                                MAX_CREATE_WATCHFACE_TIME_MILLIS +
                                " milliseconds."
                        )
                    }

                    // Perform more initialization on the background thread.
                    initComplications(complicationSlotsManager, watchFace.renderer)

                    // Now init has completed, it's OK to complete deferredWatchFaceImpl.
                    initComplicationsDone.complete(Unit)

                    // validateSchemaWireSize is fairly expensive so only perform it for
                    // interactive watch faces.
                    if (!watchState.isHeadless) {
                        validateSchemaWireSize(currentUserStyleRepository.schema)
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.e(TAG, "WatchFace crashed during init", e)
                    deferredValidation.completeExceptionally(e)
                }

                deferredValidation.complete(Unit)
            }
        }

        /**
         * This function contains the parts of watch face init that have to be done on the UI
         * thread.
         */
        @UiThread
        private suspend fun createWatchFaceImpl(
            complicationSlotsManager: ComplicationSlotsManager,
            currentUserStyleRepository: CurrentUserStyleRepository,
            deferredWatchFace: CompletableDeferred<WatchFace>,
            initStyleAndComplicationsDone: CompletableDeferred<Unit>,
            watchState: WatchState
        ) {
            val broadcastsObserver =
                BroadcastsObserver(
                    watchState,
                    this,
                    deferredWatchFaceImpl,
                    uiThreadCoroutineScope
                )

            // There's no point creating BroadcastsReceiver or listening for Accessibility state
            // changes if this is a headless instance.
            val broadcastsReceiver =
                TraceEvent("create BroadcastsReceiver").use {
                    if (watchState.isHeadless) {
                        null
                    } else {
                        startListeningForAccessibilityStateChanges()
                        BroadcastsReceiver(_context, broadcastsObserver).apply {
                            processBatteryStatus(
                                IntentFilter(Intent.ACTION_BATTERY_CHANGED).let { iFilter ->
                                    _context.registerReceiver(null, iFilter)
                                }
                            )
                        }
                    }
                }

            val watchFace = deferredWatchFace.await()
            TraceEvent("WatchFaceImpl.init").use {
                val watchFaceImpl =
                    WatchFaceImpl(
                        watchFace,
                        this@EngineWrapper,
                        watchState,
                        currentUserStyleRepository,
                        complicationSlotsManager,
                        broadcastsObserver,
                        broadcastsReceiver
                    )

                // Perform UI thread render init.
                if (!surfaceDestroyed) {
                    watchFaceImpl.renderer.uiThreadInitInternal(uiThreadCoroutineScope)
                }

                // Make sure no UI thread rendering (a consequence of completing
                // deferredWatchFaceImpl) occurs before initStyleAndComplications has
                // executed. NB usually we won't have to wait at all.
                initStyleAndComplicationsDone.await()
                deferredWatchFaceImpl.complete(watchFaceImpl)

                asyncWatchFaceConstructionPending = false
                watchFaceImpl.initComplete = true

                // For interactive instances we want to expedite the first frame to get something
                // rendered as soon as its possible to do so. NB in tests we may not always want
                // to draw this expedited first frame.
                if (!watchState.isHeadless && allowWatchFaceToAnimate()) {
                    TraceEvent("WatchFace.drawFirstFrame").use {
                        if (!surfaceDestroyed) {
                            watchFaceImpl.onDraw()
                        }
                    }
                }

                Log.d(TAG, "init complete ${watchState.watchFaceInstanceId.value}")
            }
        }

        /** No UI thread rendering can be done until after this has completed. */
        @WorkerThread
        internal fun initStyle(currentUserStyleRepository: CurrentUserStyleRepository) =
            TraceEvent("initStyle").use {
                // If the system has a stored user style then Home/SysUI is in charge of style
                // persistence, otherwise we need to do our own.
                val storedUserStyle = getInitialUserStyle()
                if (storedUserStyle != null) {
                    TraceEvent("WatchFaceImpl.init apply userStyle").use {
                        currentUserStyleRepository.updateUserStyle(
                            UserStyle(
                                UserStyleData(storedUserStyle),
                                currentUserStyleRepository.schema
                            )
                        )
                    }
                } else {
                    TraceEvent("WatchFaceImpl.init apply userStyle from prefs").use {
                        // The system doesn't support preference persistence we need to do it
                        // ourselves.
                        val preferencesFile = "watchface_prefs_${_context.javaClass.name}.txt"
                        currentUserStyleRepository.updateUserStyle(
                            UserStyle(
                                UserStyleData(readPrefs(_context, preferencesFile)),
                                currentUserStyleRepository.schema
                            )
                        )

                        backgroundThreadCoroutineScope.launch {
                            currentUserStyleRepository.userStyle.collect {
                                writePrefs(_context, preferencesFile, it)
                            }
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
        internal fun initComplications(
            complicationSlotsManager: ComplicationSlotsManager,
            renderer: Renderer
        ) =
            TraceEvent("initStyleAndComplications").use {

                // We need to inhibit an immediate callback during initialization because members
                // are
                // not fully constructed and it will fail. It's also superfluous because we're going
                // to render soon anyway.
                var initFinished = false
                complicationSlotsManager.init(
                    renderer,
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

        override fun onVisibilityChanged(visible: Boolean): Unit =
            TraceEvent("onVisibilityChanged").use {
                super.onVisibilityChanged(visible)

                // In the WSL flow Home doesn't know when WallpaperService has actually launched a
                // watchface after requesting a change. It used [Constants.ACTION_REQUEST_STATE] as
                // a
                // signal to trigger the old boot flow (sending the binder etc). This is no longer
                // required from android R onwards. See (b/181965946).
                if (wearSdkVersion < Build.VERSION_CODES.R) {
                    // We are requesting state every time the watch face changes its visibility
                    // because
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

                // During WF init the watch face is initially not visible but we want to keep UI
                // thread
                // priority high.  Once init has completed we only want the WF UI thread to have
                // high
                // priority when visible.
                if (deferredWatchFaceImpl.isCompleted && !mutableWatchState.isHeadless) {
                    if (visible) {
                        mainThreadPriorityDelegate.setInteractivePriority()
                    } else {
                        mainThreadPriorityDelegate.setNormalPriority()
                    }
                }

                mutableWatchState.isVisible.value = visible || forceIsVisibleForTesting()
                wslFlow.pendingVisibilityChanged = null

                getWatchFaceImplOrNull()?.onVisibility(visible)
            }

        override fun invalidate() {
            this@WatchFaceService.onInvalidate()
            if (!allowWatchfaceToAnimate) {
                return
            }
            if (!frameCallbackPending) {
                if (LOG_VERBOSE) {
                    Log.v(TAG, "invalidate: requesting draw")
                }
                frameCallbackPending = true
                if (!this::choreographer.isInitialized) {
                    choreographer = getChoreographer()
                }
                choreographer.postFrameCallback(frameCallback)
            } else {
                if (LOG_VERBOSE) {
                    Log.v(TAG, "invalidate: draw already requested")
                }
            }
        }

        override fun postInvalidate(delay: Duration) {
            pendingUpdateTime.postDelayedUnique(delay) { invalidate() }
        }

        override fun getComplicationDeniedIntent() =
            getWatchFaceImplOrNull()?.complicationDeniedDialogIntent

        override fun getComplicationRationaleIntent() =
            getWatchFaceImplOrNull()?.complicationRationaleDialogIntent

        override fun onActionTimeTick() {
            // In interactive mode we don't need to do anything with onActionTimeTick() since the
            // watchface should be updating anyway. In ambient mode if the system hasn't sent us
            // UiState then we assume it's not going to send ambient ticks either and we treat this
            // as an ambient tick.
            if (mutableWatchState.isAmbient.value == true && !systemHasSentWatchUiState) {
                ambientTickUpdate()
            }
        }

        /**
         * @param taskName The name to use when logging any exception
         * @param listenerCallback The callback to invoke for each registered [IWatchfaceListener]
         * @return the number of calls to [listenerCallback] that did not throw an exception.
         */
        private fun forEachListener(
            taskName: String,
            listenerCallback: (listener: IWatchfaceListener) -> Unit
        ): Int {
            var i = listeners.beginBroadcast()
            var successCount = 0
            while (i > 0) {
                i--
                val listener = listeners.getBroadcastItem(i)
                try {
                    listenerCallback(listener)
                    successCount++
                } catch (e: Exception) {
                    Log.e(
                        TAG,
                        "In $taskName broadcastToListeners failed for ${listener.asBinder()}",
                        e
                    )
                }
            }
            listeners.finishBroadcast()
            return successCount
        }

        override fun sendPreviewImageNeedsUpdateRequest() {
            synchronized(lock) {
                if (this::interactiveInstanceId.isInitialized) {
                    lastPreviewImageNeedsUpdateRequest = interactiveInstanceId

                    forEachListener("sendPreviewImageNeedsUpdateRequest") {
                        it.onPreviewImageUpdateRequested(interactiveInstanceId)
                    }
                } else {
                    Log.w(
                        TAG,
                        "Ignoring sendPreviewImageNeedsUpdateRequest because " +
                            "interactiveInstanceId not initialized"
                    )
                }
            }
        }

        override fun onWatchFaceColorsChanged(watchFaceColors: WatchFaceColors?) {
            synchronized(lock) {
                lastWatchFaceColors = watchFaceColors

                forEachListener("onWatchFaceColorsChanged") {
                    it.onWatchfaceColorsChanged(lastWatchFaceColors?.toWireFormat())
                }
            }
        }

        internal fun onEngineDetached() {
            synchronized(lock) {
                forEachListener("onWatchFaceColorsChanged") {
                    if (it.apiVersion >= 2) {
                        it.onEngineDetached()
                    }
                }
            }
        }

        internal fun draw(watchFaceImpl: WatchFaceImpl?) {
            try {
                if (TRACE_DRAW) {
                    Trace.beginSection("onDraw")
                }
                if (LOG_VERBOSE) {
                    Log.v(TAG, "drawing frame")
                }
                watchFaceImpl?.onDraw()
            } finally {
                if (TRACE_DRAW) {
                    Trace.endSection()
                }
            }
        }

        internal fun validateSchemaWireSize(schema: UserStyleSchema) =
            TraceEvent("WatchFaceService.validateSchemaWireSize").use {
                var estimatedBytes = 0
                @Suppress("Deprecation") // userStyleSettings
                for (styleSetting in schema.userStyleSettings) {
                    estimatedBytes +=
                        styleSetting.estimateWireSizeInBytesAndValidateIconDimensions(
                            resourcesContext,
                            MAX_REASONABLE_SCHEMA_ICON_WIDTH,
                            MAX_REASONABLE_SCHEMA_ICON_HEIGHT,
                        )
                }
                require(estimatedBytes < MAX_REASONABLE_SCHEMA_WIRE_SIZE_BYTES) {
                    "The estimated wire size of the supplied UserStyleSchemas for watch face " +
                        "$packageName is too big at $estimatedBytes bytes. " +
                        "UserStyleSchemas get sent to the companion over bluetooth and should be " +
                        "as small as possible for this to be performant. The maximum size is " +
                        MAX_REASONABLE_SCHEMA_WIRE_SIZE_BYTES
                }
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

        override fun setActiveComplicationSlots(complicationSlotIds: IntArray): Unit =
            TraceEvent("WatchFaceService.setActiveComplications").use {
                wslFlow.setActiveComplications(complicationSlotIds)
            }

        internal fun startListeningForAccessibilityStateChanges() {
            val accessibilityManager =
                getSystemService(ACCESSIBILITY_SERVICE) as AccessibilityManager
            accessibilityManager.addAccessibilityStateChangeListener(this)
        }

        internal fun stopListeningForAccessibilityStateChanges() {
            val accessibilityManager =
                getSystemService(ACCESSIBILITY_SERVICE) as AccessibilityManager
            accessibilityManager.removeAccessibilityStateChangeListener(this)
        }

        override fun onAccessibilityStateChanged(isEnabled: Boolean) {
            if (systemViewOfContentDescriptionLabelsIsStale && isEnabled) {
                maybeSendContentDescriptionLabelsBroadcast()
            }
        }

        @UiThread
        override fun updateContentDescriptionLabels() {
            val labels = mutableListOf<Pair<Int, ContentDescriptionLabel>>()

            uiThreadCoroutineScope.launch {
                TraceEvent("WatchFaceService.updateContentDescriptionLabels A").close()
                val watchFace = deferredWatchFace.await()
                val earlyInitDetails = deferredEarlyInitDetails.await()

                TraceEvent("WatchFaceService.updateContentDescriptionLabels").use {
                    // The side effects of this need to be applied before deferredWatchFaceImpl is
                    // completed.
                    val renderer = watchFace.renderer
                    val complicationSlotsManager = earlyInitDetails.complicationSlotsManager

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

                    // Add a ContentDescriptionLabel for each enabled complication that isn't empty
                    // or no data.
                    val screenBounds = renderer.screenBounds
                    for ((_, complication) in complicationSlotsManager.complicationSlots) {
                        if (
                            complication.enabled &&
                                when (complication.complicationData.value.type) {
                                    ComplicationType.EMPTY -> false
                                    ComplicationType.NO_DATA -> false
                                    else -> true
                                }
                        ) {
                            if (complication.boundsType == ComplicationSlotBoundsType.BACKGROUND) {
                                ComplicationSlotBoundsType.BACKGROUND
                            } else {
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

                    // Add any additional labels defined by the watch face.
                    for (labelPair in renderer.additionalContentDescriptionLabels) {
                        labels.add(
                            Pair(
                                labelPair.first,
                                ContentDescriptionLabel(
                                        labelPair.second.bounds,
                                        labelPair.second.text.toWireComplicationText()
                                    )
                                    .apply { tapAction = labelPair.second.tapAction }
                            )
                        )
                    }

                    contentDescriptionLabels =
                        labels.sortedBy { it.first }.map { it.second }.toTypedArray()

                    systemViewOfContentDescriptionLabelsIsStale = true
                    maybeSendContentDescriptionLabelsBroadcast()
                }
            }
        }

        /**
         * From Android R lets SysUI know the labels have changed, if the AccessibilityManager is
         * enabled.
         */
        private fun maybeSendContentDescriptionLabelsBroadcast() {
            if (wearSdkVersion >= Build.VERSION_CODES.R && getAccessibilityManager().isEnabled) {
                // TODO(alexclarke): This should require a permission. See http://b/184717802
                _context.sendBroadcast(Intent(Constants.ACTION_WATCH_FACE_REFRESH_A11Y_LABELS))
                systemViewOfContentDescriptionLabelsIsStale = false
            }
        }

        private fun getAccessibilityManager() =
            _context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager

        fun addWatchFaceListener(listener: IWatchfaceListener) {
            synchronized(lock) {
                if (listeners.register(listener)) {
                    Log.d(TAG, "addWatchFaceListener $listener")
                } else {
                    Log.w(
                        TAG,
                        "addWatchFaceListener $listener failed because its already registered"
                    )
                    return
                }
                lastPreviewImageNeedsUpdateRequest?.let {
                    listener.onPreviewImageUpdateRequested(it)
                }
                listener.onWatchfaceColorsChanged(lastWatchFaceColors?.toWireFormat())
            }

            uiThreadCoroutineScope.launch {
                deferredWatchFaceImpl.await()
                try {
                    listener.onWatchfaceReady()
                } catch (e: Exception) {
                    Log.e(TAG, "listener.onWatchfaceReady failed", e)
                }
            }
        }

        fun removeWatchFaceListener(listener: IWatchfaceListener) {
            synchronized(lock) {
                if (listeners.unregister(listener)) {
                    Log.d(TAG, "removeWatchFaceListener $listener")
                } else {
                    Log.w(
                        TAG,
                        "removeWatchFaceListener $listener failed because it's not registered"
                    )
                }
            }
        }

        internal lateinit var screenBounds: Rect
            private set

        @UiThread
        internal fun dump(writer: IndentingPrintWriter) {
            require(uiThreadHandler.looper.isCurrentThread) {
                "dump must be called from the UIThread"
            }
            writer.println("WatchFaceEngine:")
            writer.increaseIndent()
            when {
                wslFlow.iWatchFaceServiceInitialized() -> {
                    writer.println("WSL style init flow")
                    writer.println("watchFaceInitStarted=${wslFlow.watchFaceInitStarted}")
                }
                this.watchFaceCreatedOrPending() -> writer.println("Androidx style init flow")
                wearSdkVersion < Build.VERSION_CODES.R -> writer.println("Expecting WSL style init")
                else -> writer.println("Expecting androidx style style init")
            }

            if (wslFlow.iWatchFaceServiceInitialized()) {
                writer.println(
                    "iWatchFaceService.asBinder().isBinderAlive=" +
                        "${wslFlow.iWatchFaceService.asBinder().isBinderAlive}"
                )
                if (wslFlow.iWatchFaceService.asBinder().isBinderAlive) {
                    writer.println(
                        "iWatchFaceService.apiVersion=${wslFlow.iWatchFaceService.apiVersion}"
                    )
                }
            }
            writer.println("createdBy=$createdBy")
            writer.println("asyncWatchFaceConstructionPending=$asyncWatchFaceConstructionPending")
            writer.println(
                "systemViewOfContentDescriptionLabelsIsStale=" +
                    systemViewOfContentDescriptionLabelsIsStale
            )

            if (this::interactiveInstanceId.isInitialized) {
                writer.println("interactiveInstanceId=$interactiveInstanceId")
            }

            writer.println("frameCallbackPending=$frameCallbackPending")
            writer.println("destroyed=$destroyed")
            writer.println("surfaceDestroyed=$surfaceDestroyed")
            writer.println("lastComplications=${complicationsFlow.value.joinToString()}")
            writer.println("pendingUpdateTime=${pendingUpdateTime.isPending()}")

            synchronized(lock) {
                forEachListener("dump") { writer.println("listener = ${it.asBinder()}") }
            }

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
        if (Build.VERSION.SDK_INT >= 27) {
            HeadlessWatchFaceImpl.dump(indentingPrintWriter)
        }
        indentingPrintWriter.flush()
    }

    private object ChinHeightApi25 {
        @Suppress("DEPRECATION")
        @Px
        fun extractFromWindowInsets(insets: WindowInsets?) = insets?.systemWindowInsetBottom ?: 0
    }

    @RequiresApi(30)
    private object ChinHeightApi30 {
        @Px
        fun extractFromWindowInsets(insets: WindowInsets?) =
            insets?.getInsets(WindowInsets.Type.systemBars())?.bottom ?: 0
    }

    @RequiresApi(27)
    private object NotifyColorsChangedHelper {
        fun notifyColorsChanged(engine: Engine) {
            engine.notifyColorsChanged()
        }
    }
}

/**
 * Runs the supplied task on the handler thread. If we're not on the handler thread a task is
 * posted.
 *
 * @param traceEventName The name of the trace event to emit.
 * @param task The task to post on the handler.
 */
internal fun Handler.runOnHandlerWithTracing(traceEventName: String, task: () -> Unit) =
    TraceEvent(traceEventName).use {
        if (looper == Looper.myLooper()) {
            task.invoke()
        } else {
            post { TraceEvent("$traceEventName invokeTask").use { task.invoke() } }
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
): R =
    TraceEvent(traceEventName).use {
        try {
            // Inside runBlocking, coroutineContext has a different value.
            val desiredContext = coroutineContext
            return runBlocking { withContext(desiredContext) { task() } }
        } catch (e: Exception) {
            Log.e("CoroutineScope", "Exception in traceEventName", e)
            throw e
        }
    }

/**
 * If the instance ID for [MutableWatchState.watchFaceInstanceId] begin with this prefix, then the
 * system sends consistent IDs for interactive, headless and editor sessions.
 *
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
const val SYSTEM_SUPPORTS_CONSISTENT_IDS_PREFIX = "wfId-"

/**
 * Instance ID to use when either there's no system id or it doesn't start with
 * [SYSTEM_SUPPORTS_CONSISTENT_IDS_PREFIX].
 *
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) const val DEFAULT_INSTANCE_ID = "defaultInstance"

/**
 * This is needed to make the instance id consistent between Interactive, Headless and EditorSession
 * for old versions of the system.
 *
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun sanitizeWatchFaceId(instanceId: String?) =
    if (instanceId == null || !instanceId.startsWith(SYSTEM_SUPPORTS_CONSISTENT_IDS_PREFIX)) {
        DEFAULT_INSTANCE_ID
    } else {
        instanceId
    }
