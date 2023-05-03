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
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.drawable.Icon
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.os.Looper
import android.support.wearable.complications.ComplicationProviderInfo as WireComplicationProviderInfo
import android.support.wearable.complications.IPreviewComplicationDataCallback
import android.support.wearable.complications.IProviderInfoService
import android.support.wearable.watchface.Constants
import android.view.Surface
import android.view.SurfaceHolder
import androidx.activity.ComponentActivity
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.wear.watchface.BroadcastsObserver
import androidx.wear.watchface.CanvasComplication
import androidx.wear.watchface.CanvasType
import androidx.wear.watchface.ComplicationDataSourceChooserIntent
import androidx.wear.watchface.ComplicationHelperActivity
import androidx.wear.watchface.ComplicationSlot
import androidx.wear.watchface.ComplicationSlotBoundsType
import androidx.wear.watchface.ComplicationSlotsManager
import androidx.wear.watchface.DEFAULT_INSTANCE_ID
import androidx.wear.watchface.MutableWatchState
import androidx.wear.watchface.RenderParameters
import androidx.wear.watchface.Renderer
import androidx.wear.watchface.SYSTEM_SUPPORTS_CONSISTENT_IDS_PREFIX
import androidx.wear.watchface.WatchFace
import androidx.wear.watchface.WatchFaceHostApi
import androidx.wear.watchface.WatchFaceImpl
import androidx.wear.watchface.WatchFaceService
import androidx.wear.watchface.WatchFaceType
import androidx.wear.watchface.WatchState
import androidx.wear.watchface.client.DeviceConfig
import androidx.wear.watchface.client.WatchFaceId
import androidx.wear.watchface.client.asApiEditorState
import androidx.wear.watchface.complications.ComplicationDataSourceInfo
import androidx.wear.watchface.complications.ComplicationDataSourceInfoRetriever
import androidx.wear.watchface.complications.ComplicationSlotBounds
import androidx.wear.watchface.complications.DefaultComplicationDataSourcePolicy
import androidx.wear.watchface.complications.SystemDataSources
import androidx.wear.watchface.complications.data.ComplicationData
import androidx.wear.watchface.complications.data.ComplicationExperimental
import androidx.wear.watchface.complications.data.ComplicationText
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.data.EmptyComplicationData
import androidx.wear.watchface.complications.data.LongTextComplicationData
import androidx.wear.watchface.complications.data.PlainComplicationText
import androidx.wear.watchface.complications.data.ShortTextComplicationData
import androidx.wear.watchface.complications.rendering.CanvasComplicationDrawable
import androidx.wear.watchface.complications.rendering.ComplicationDrawable
import androidx.wear.watchface.editor.EditorSession.Companion.EDITING_SESSION_TIMEOUT
import androidx.wear.watchface.editor.data.EditorStateWireFormat
import androidx.wear.watchface.style.CurrentUserStyleRepository
import androidx.wear.watchface.style.UserStyle
import androidx.wear.watchface.style.UserStyleData
import androidx.wear.watchface.style.UserStyleSchema
import androidx.wear.watchface.style.UserStyleSetting
import androidx.wear.watchface.style.UserStyleSetting.ListUserStyleSetting.ListOption
import androidx.wear.watchface.style.UserStyleSetting.Option
import androidx.wear.watchface.style.WatchFaceLayer
import com.google.common.truth.Truth.assertThat
import java.lang.IllegalArgumentException
import java.time.Instant
import java.time.ZonedDateTime
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeUnit.MILLISECONDS
import kotlin.test.assertFailsWith
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.android.asCoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mockito
import org.mockito.Mockito.doAnswer
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`

public const val LEFT_COMPLICATION_ID: Int = 1000
public const val RIGHT_COMPLICATION_ID: Int = 1001
public const val BACKGROUND_COMPLICATION_ID: Int = 1111

public val dataSource1: ComponentName = ComponentName("dataSource.app1", "dataSource.class1")
public val dataSource2: ComponentName = ComponentName("dataSource.app2", "dataSource.class2")
public val dataSource3: ComponentName = ComponentName("dataSource.app3", "dataSource.class3")

private const val TIMEOUT_MILLIS = 500L

private const val PROVIDER_CHOOSER_EXTRA_KEY = "PROVIDER_CHOOSER_EXTRA_KEY"
private const val PROVIDER_CHOOSER_EXTRA_VALUE = "PROVIDER_CHOOSER_EXTRA_VALUE"
private const val PROVIDER_CHOOSER_RESULT_EXTRA_KEY = "PROVIDER_CHOOSER_RESULT_EXTRA_KEY"
private const val PROVIDER_CHOOSER_RESULT_EXTRA_VALUE = "PROVIDER_CHOOSER_RESULT_EXTRA_VALUE"

internal val redStyleOption = ListOption(Option.Id("red_style"), "Red", "Red", icon = null)
internal val greenStyleOption = ListOption(Option.Id("green_style"), "Green", "Green", icon = null)
internal val blueStyleOption = ListOption(Option.Id("blue_style"), "Blue", "Blue", icon = null)
internal val colorStyleList = listOf(redStyleOption, greenStyleOption, blueStyleOption)
internal val colorStyleSetting =
    UserStyleSetting.ListUserStyleSetting(
        UserStyleSetting.Id("color_style_setting"),
        "Colors",
        "Watchface colorization",
        /* icon = */ null,
        colorStyleList,
        listOf(WatchFaceLayer.BASE)
    )

internal val classicStyleOption =
    ListOption(Option.Id("classic_style"), "Classic", "Classic", icon = null)
internal val modernStyleOption =
    ListOption(Option.Id("modern_style"), "Modern", "Modern", icon = null)
internal val gothicStyleOption =
    ListOption(Option.Id("gothic_style"), "Gothic", "Gothic", icon = null)
internal val watchHandStyleList = listOf(classicStyleOption, modernStyleOption, gothicStyleOption)
internal val watchHandStyleSetting =
    UserStyleSetting.ListUserStyleSetting(
        UserStyleSetting.Id("hand_style_setting"),
        "Hand Style",
        "Hand visual look",
        /* icon = */ null,
        watchHandStyleList,
        listOf(WatchFaceLayer.COMPLICATIONS_OVERLAY)
    )

private val mockInvalidateCallback = Mockito.mock(CanvasComplication.InvalidateCallback::class.java)
private val placeholderWatchState = MutableWatchState().asWatchState()
private val mockLeftCanvasComplication =
    CanvasComplicationDrawable(
        ComplicationDrawable(),
        placeholderWatchState,
        mockInvalidateCallback
    )
private val mockRightCanvasComplication =
    CanvasComplicationDrawable(
        ComplicationDrawable(),
        placeholderWatchState,
        mockInvalidateCallback
    )
private val mockBackgroundCanvasComplication =
    CanvasComplicationDrawable(
        ComplicationDrawable(),
        placeholderWatchState,
        mockInvalidateCallback
    )
@OptIn(ComplicationExperimental::class)
private val backgroundComplication =
    ComplicationSlot.createBackgroundComplicationSlotBuilder(
            BACKGROUND_COMPLICATION_ID,
            { _, _ -> mockBackgroundCanvasComplication },
            emptyList(),
            DefaultComplicationDataSourcePolicy()
        )
        .setEnabled(false)
        .build()

private val bothComplicationsOption =
    UserStyleSetting.ComplicationSlotsUserStyleSetting.ComplicationSlotsOption(
        Option.Id("LEFT_AND_RIGHT_COMPLICATIONS"),
        "Left And Right",
        "Show left and right complications",
        null,
        // An empty list means use the initial config.
        emptyList()
    )
private val leftOnlyComplicationsOption =
    UserStyleSetting.ComplicationSlotsUserStyleSetting.ComplicationSlotsOption(
        Option.Id("LEFT_COMPLICATION"),
        "Left",
        "Show left complication only",
        null,
        listOf(
            UserStyleSetting.ComplicationSlotsUserStyleSetting.ComplicationSlotOverlay.Builder(
                    RIGHT_COMPLICATION_ID
                )
                .setEnabled(false)
                .build()
        )
    )
private val rightOnlyComplicationsOption =
    UserStyleSetting.ComplicationSlotsUserStyleSetting.ComplicationSlotsOption(
        Option.Id("RIGHT_COMPLICATION"),
        "Right",
        "Show right complication only",
        null,
        listOf(
            UserStyleSetting.ComplicationSlotsUserStyleSetting.ComplicationSlotOverlay.Builder(
                    LEFT_COMPLICATION_ID
                )
                .setEnabled(false)
                .build()
        )
    )
private val complicationsStyleSetting =
    UserStyleSetting.ComplicationSlotsUserStyleSetting(
        UserStyleSetting.Id("complications_style_setting"),
        "AllComplicationSlots",
        "Number and position",
        icon = null,
        complicationConfig =
            listOf(
                bothComplicationsOption,
                leftOnlyComplicationsOption,
                rightOnlyComplicationsOption
            ),
        affectsWatchFaceLayers = listOf(WatchFaceLayer.COMPLICATIONS)
    )

/** A trivial [WatchFaceService] used for testing headless editor instances. */
public class TestHeadlessWatchFaceService : WatchFaceService() {
    override fun createUserStyleSchema() =
        UserStyleSchema(listOf(colorStyleSetting, watchHandStyleSetting))

    override fun createComplicationSlotsManager(
        currentUserStyleRepository: CurrentUserStyleRepository
    ) = ComplicationSlotsManager(emptyList(), currentUserStyleRepository)

    override suspend fun createWatchFace(
        surfaceHolder: SurfaceHolder,
        watchState: WatchState,
        complicationSlotsManager: ComplicationSlotsManager,
        currentUserStyleRepository: CurrentUserStyleRepository
    ) =
        WatchFace(
            WatchFaceType.ANALOG,
            @Suppress("deprecation")
            object :
                Renderer.CanvasRenderer(
                    surfaceHolder,
                    currentUserStyleRepository,
                    watchState,
                    CanvasType.SOFTWARE,
                    100
                ) {
                override fun render(canvas: Canvas, bounds: Rect, zonedDateTime: ZonedDateTime) {
                    when (currentUserStyleRepository.userStyle.value[colorStyleSetting]!!) {
                        redStyleOption -> canvas.drawColor(Color.RED)
                        greenStyleOption -> canvas.drawColor(Color.GREEN)
                        blueStyleOption -> canvas.drawColor(Color.BLUE)
                    }
                }

                override fun renderHighlightLayer(
                    canvas: Canvas,
                    bounds: Rect,
                    zonedDateTime: ZonedDateTime
                ) {
                    // NOP
                }
            }
        )
}

/** Trivial "editor" which exposes the EditorSession for testing. */
public open class OnWatchFaceEditingTestActivity : ComponentActivity() {
    public lateinit var editorSession: EditorSession
    public lateinit var onCreateException: Exception
    public val creationLatch: CountDownLatch = CountDownLatch(1)
    public val deferredDone = CompletableDeferred<Unit>()

    public val listenableEditorSession: ListenableEditorSession by lazy {
        ListenableEditorSession(editorSession)
    }

    internal companion object {
        internal var complicationDataSourceInfoRetrieverProvider:
            ComplicationDataSourceInfoRetrieverProvider? =
            null
    }

    public val immediateCoroutineScope: CoroutineScope = CoroutineScope(Dispatchers.Main.immediate)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        immediateCoroutineScope.launch {
            try {
                editorSession =
                    EditorSession.createOnWatchEditorSessionImpl(
                        this@OnWatchFaceEditingTestActivity,
                        intent!!,
                        complicationDataSourceInfoRetrieverProvider!!
                    )
            } catch (e: Exception) {
                onCreateException = e
            } finally {
                deferredDone.complete(Unit)
                creationLatch.countDown()
            }
        }
    }
}

public open class TestComplicationDataSourceInfoRetrieverProvider(
    val getProviderInfosLatch: CountDownLatch? = null
) : ComplicationDataSourceInfoRetrieverProvider, IProviderInfoService.Stub() {

    private val dataSourceIcon1: Icon =
        Icon.createWithBitmap(Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888))
    private val dataSourceIcon2: Icon =
        Icon.createWithBitmap(Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888))

    private val watchFaceComponent = ComponentName("test.package", "test.class")
    private val dataSourceInfos =
        mapOf(
            LEFT_COMPLICATION_ID to
                ComplicationDataSourceInfo(
                    "DataSourceApp1",
                    "DataSource1",
                    dataSourceIcon1,
                    ComplicationType.SHORT_TEXT,
                    dataSource1
                ),
            RIGHT_COMPLICATION_ID to
                ComplicationDataSourceInfo(
                    "DataSourceApp2",
                    "DataSource2",
                    dataSourceIcon2,
                    ComplicationType.LONG_TEXT,
                    dataSource2
                )
        )
    private val previewData =
        mapOf(
            dataSource1 to
                ShortTextComplicationData.Builder(
                        PlainComplicationText.Builder("Left").build(),
                        ComplicationText.EMPTY
                    )
                    .build()
                    .asWireComplicationData(),
            dataSource2 to
                LongTextComplicationData.Builder(
                        PlainComplicationText.Builder("Right").build(),
                        ComplicationText.EMPTY
                    )
                    .build()
                    .asWireComplicationData(),
            dataSource3 to
                LongTextComplicationData.Builder(
                        PlainComplicationText.Builder("DataSource3").build(),
                        ComplicationText.EMPTY
                    )
                    .build()
                    .asWireComplicationData(),
        )

    public lateinit var lastComplicationDataSourceInfoRetriever: ComplicationDataSourceInfoRetriever

    override fun getComplicationDataSourceInfoRetriever(): ComplicationDataSourceInfoRetriever {
        lastComplicationDataSourceInfoRetriever = ComplicationDataSourceInfoRetriever(this)
        return lastComplicationDataSourceInfoRetriever
    }

    override fun getProviderInfos(
        watchFaceComponent: ComponentName,
        ids: IntArray
    ): Array<WireComplicationProviderInfo?>? {
        getProviderInfosLatch?.await()
        if (watchFaceComponent != this.watchFaceComponent) {
            return null
        }
        return ArrayList<WireComplicationProviderInfo?>()
            .apply {
                for (id in ids) {
                    add(dataSourceInfos[id]?.toWireComplicationProviderInfo())
                }
            }
            .toTypedArray()
    }

    override fun getApiVersion(): Int = 1

    override fun requestPreviewComplicationData(
        dataSourceComponent: ComponentName,
        complicationType: Int,
        previewComplicationDataCallback: IPreviewComplicationDataCallback
    ): Boolean {
        previewComplicationDataCallback.updateComplicationData(previewData[dataSourceComponent])
        return true
    }
}

/** Fake ComplicationHelperActivity for testing. */
public class TestComplicationHelperActivity : Activity() {

    public companion object {
        public var lastIntent: Intent? = null
        public var resultIntent: CompletableDeferred<Intent?>? = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        lastIntent = intent

        CoroutineScope(Dispatchers.Main.immediate).launch {
            setResult(123, resultIntent!!.await())
            finish()
        }
    }
}

/** Fake complication data source chooser for testing. */
public class TestComplicationDataSourceChooserActivity : Activity() {

    public companion object {
        public var lastIntent: Intent? = null
        public var resultIntent: Intent? = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        lastIntent = intent

        setResult(123, resultIntent)
        finish()
    }
}

public class TestEditorObserver : IEditorObserver.Stub() {
    private lateinit var editorState: EditorStateWireFormat
    private var latch = CountDownLatch(1)

    override fun getApiVersion() = IEditorObserver.API_VERSION

    override fun onEditorStateChange(editorState: EditorStateWireFormat) {
        this.editorState = editorState
        latch.countDown()
    }

    fun awaitEditorStateChange(timeout: Long, unit: TimeUnit): EditorStateWireFormat {
        require(latch.await(timeout, unit))
        return editorState
    }

    fun stateChangeObserved() = this::editorState.isInitialized
}

@RunWith(AndroidJUnit4::class)
@MediumTest
public class EditorSessionTest {
    private val headlessWatchFaceComponentName =
        ComponentName("test.package", TestHeadlessWatchFaceService::class.qualifiedName!!)
    private val testEditorPackageName = "test.package"
    private val testInstanceId = WatchFaceId(SYSTEM_SUPPORTS_CONSISTENT_IDS_PREFIX + "1")
    private lateinit var editorDelegate: WatchFace.EditorDelegate
    private val screenBounds = Rect(0, 0, 400, 400)

    private val fakeBitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
    private val onDestroyLatch = CountDownLatch(1)
    private val dataSourceIcon =
        Icon.createWithBitmap(Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888))
    private val dataSourceComponentName = ComponentName("test.package", "test.class")
    internal val complicationDeniedDialogIntent = Intent("ComplicationDeniedDialog")
    internal val complicationRationaleDialogIntent = Intent("ComplicationRationaleDialog")

    @OptIn(ComplicationExperimental::class)
    private val leftComplication =
        ComplicationSlot.createRoundRectComplicationSlotBuilder(
                LEFT_COMPLICATION_ID,
                { _, _ -> mockLeftCanvasComplication },
                listOf(
                    ComplicationType.RANGED_VALUE,
                    ComplicationType.LONG_TEXT,
                    ComplicationType.SHORT_TEXT,
                    ComplicationType.MONOCHROMATIC_IMAGE,
                    ComplicationType.SMALL_IMAGE
                ),
                DefaultComplicationDataSourcePolicy(
                    ComponentName("com.primary.package", "com.primary.app"),
                    ComplicationType.LONG_TEXT,
                    SystemDataSources.DATA_SOURCE_SUNRISE_SUNSET,
                    ComplicationType.SHORT_TEXT
                ),
                ComplicationSlotBounds(
                    ComplicationType.values().associateWith {
                        if (it == ComplicationType.LONG_TEXT) {
                            RectF(0.1f, 0.4f, 0.4f, 0.6f)
                        } else {
                            RectF(0.3f, 0.4f, 0.4f, 0.6f)
                        }
                    },
                    ComplicationType.values().associateWith { RectF() }
                )
            )
            .build()

    @OptIn(ComplicationExperimental::class)
    private val rightComplication =
        ComplicationSlot.createRoundRectComplicationSlotBuilder(
                RIGHT_COMPLICATION_ID,
                { _, _ -> mockRightCanvasComplication },
                listOf(
                    ComplicationType.RANGED_VALUE,
                    ComplicationType.LONG_TEXT,
                    ComplicationType.SHORT_TEXT,
                    ComplicationType.MONOCHROMATIC_IMAGE,
                    ComplicationType.SMALL_IMAGE,
                    ComplicationType.PHOTO_IMAGE
                ),
                DefaultComplicationDataSourcePolicy(
                    ComponentName("com.primary.package", "com.primary.app"),
                    ComplicationType.LONG_TEXT,
                    ComponentName("com.secondary.package", "com.secondary.app"),
                    ComplicationType.PHOTO_IMAGE,
                    SystemDataSources.DATA_SOURCE_DAY_OF_WEEK,
                    ComplicationType.SHORT_TEXT
                ),
                @Suppress("DEPRECATION")
                ComplicationSlotBounds(
                    ComplicationType.values().associateWith {
                        if (it == ComplicationType.LONG_TEXT) {
                            RectF(0.6f, 0.4f, 0.9f, 0.6f)
                        } else {
                            RectF(0.6f, 0.4f, 0.7f, 0.6f)
                        }
                    },
                    ComplicationType.values().associateWith { RectF() }
                )
            )
            .setConfigExtras(
                Bundle().apply {
                    putString(PROVIDER_CHOOSER_EXTRA_KEY, PROVIDER_CHOOSER_EXTRA_VALUE)
                }
            )
            .build()

    @SuppressLint("NewApi") // EditorRequest
    private fun createOnWatchFaceEditingTestActivity(
        userStyleSettings: List<UserStyleSetting>,
        complicationSlots: List<ComplicationSlot>,
        watchFaceId: WatchFaceId = testInstanceId,
        previewReferenceInstant: Instant = Instant.ofEpochMilli(12345),
        complicationDataSourceInfoRetrieverProvider: ComplicationDataSourceInfoRetrieverProvider =
            TestComplicationDataSourceInfoRetrieverProvider(),
        shouldTimeout: Boolean = false,
        preRFlow: Boolean = false,
        headlessDeviceConfig: DeviceConfig? = null,
        initialUserStyle: UserStyleData? = null,
        watchComponentName: ComponentName = ComponentName("test.package", "test.class"),
        previewScreenshotParams: PreviewScreenshotParams? = null
    ): ActivityScenario<OnWatchFaceEditingTestActivity> {
        val userStyleRepository = CurrentUserStyleRepository(UserStyleSchema(userStyleSettings))
        val mockSurfaceHolder = `mock`(SurfaceHolder::class.java)
        `when`(mockSurfaceHolder.surfaceFrame).thenReturn(screenBounds)
        @Suppress("Deprecation")
        val fakeRenderer =
            object :
                Renderer.CanvasRenderer(
                    mockSurfaceHolder,
                    userStyleRepository,
                    MutableWatchState().asWatchState(),
                    CanvasType.HARDWARE,
                    interactiveDrawModeUpdateDelayMillis = 16,
                    clearWithBackgroundTintBeforeRenderingHighlightLayer = false
                ) {
                override fun render(canvas: Canvas, bounds: Rect, zonedDateTime: ZonedDateTime) {}

                override fun renderHighlightLayer(
                    canvas: Canvas,
                    bounds: Rect,
                    zonedDateTime: ZonedDateTime
                ) {}
            }

        val complicationSlotsManager =
            ComplicationSlotsManager(complicationSlots, userStyleRepository, fakeRenderer)
        complicationSlotsManager.watchState = placeholderWatchState
        complicationSlotsManager.listenForStyleChanges(CoroutineScope(Dispatchers.Main.immediate))

        // Mocking getters and setters with mockito at the same time is hard so we do this instead.
        editorDelegate =
            object : WatchFace.EditorDelegate {
                private val backgroundHandlerThread =
                    HandlerThread("TestBackgroundThread").apply { start() }

                private val backgroundHandler = Handler(backgroundHandlerThread.looper)

                override val userStyleSchema = userStyleRepository.schema
                override var userStyle: UserStyle
                    get() = userStyleRepository.userStyle.value
                    set(value) {
                        userStyleRepository.updateUserStyle(value)
                    }

                override val complicationSlotsManager = complicationSlotsManager
                override val screenBounds = this@EditorSessionTest.screenBounds
                override val previewReferenceInstant = previewReferenceInstant
                override val backgroundThreadHandler = backgroundHandler
                override val complicationDeniedDialogIntent =
                    this@EditorSessionTest.complicationDeniedDialogIntent
                override val complicationRationaleDialogIntent =
                    this@EditorSessionTest.complicationRationaleDialogIntent

                override fun renderWatchFaceToBitmap(
                    renderParameters: RenderParameters,
                    instant: Instant,
                    slotIdToComplicationData: Map<Int, ComplicationData>?
                ) = fakeBitmap

                override fun onDestroy() {
                    onDestroyLatch.countDown()
                    backgroundHandlerThread.quitSafely()
                }

                override fun setComplicationSlotConfigExtrasChangeCallback(
                    callback: WatchFace.ComplicationSlotConfigExtrasChangeCallback?
                ) {
                    complicationSlotsManager.configExtrasChangeCallback = callback
                }
            }
        if (!shouldTimeout) {
            WatchFace.registerEditorDelegate(watchComponentName, editorDelegate)
        }

        OnWatchFaceEditingTestActivity.complicationDataSourceInfoRetrieverProvider =
            complicationDataSourceInfoRetrieverProvider

        if (preRFlow) {
            return ActivityScenario.launch(
                Intent().apply {
                    putExtra(Constants.EXTRA_WATCH_FACE_COMPONENT, watchComponentName)
                    component =
                        ComponentName(
                            ApplicationProvider.getApplicationContext<Context>(),
                            OnWatchFaceEditingTestActivity::class.java
                        )
                }
            )
        }

        return ActivityScenario.launch(
            WatchFaceEditorContract()
                .createIntent(
                    ApplicationProvider.getApplicationContext<Context>(),
                    EditorRequest(
                        watchComponentName,
                        testEditorPackageName,
                        initialUserStyle,
                        watchFaceId,
                        headlessDeviceConfig,
                        previewScreenshotParams
                    )
                )
                .apply {
                    component =
                        ComponentName(
                            ApplicationProvider.getApplicationContext<Context>(),
                            OnWatchFaceEditingTestActivity::class.java
                        )
                }
        )
    }

    private fun createOnWatchFaceEditingTestActivityThatThrowsTimeoutException():
        ActivityScenario<OnWatchFaceEditingTestActivity> =
        createOnWatchFaceEditingTestActivity(
            emptyList(),
            emptyList(), /* other params are default */
            shouldTimeout = true
        )

    @After
    public fun tearDown() {
        OnWatchFaceEditingTestActivity.complicationDataSourceInfoRetrieverProvider = null
        ComplicationDataSourceChooserContract.useTestComplicationHelperActivity = false
        ComplicationHelperActivity.useTestComplicationDataSourceChooserActivity = false
        ComplicationHelperActivity.skipPermissionCheck = false
        WatchFace.clearAllEditorDelegates()
        EditorService.globalEditorService.clearCloseCallbacks()
    }

    @Test
    public fun createOnWatchEditingSessionThrowsTimeoutException() {
        val scenario = createOnWatchFaceEditingTestActivityThatThrowsTimeoutException()
        lateinit var activity: OnWatchFaceEditingTestActivity
        scenario.onActivity { activity = it }
        activity.creationLatch.await(EDITING_SESSION_TIMEOUT.toMillis() + 500, MILLISECONDS)
        assertThat(activity.onCreateException is TimeoutCancellationException).isTrue()
    }

    @Test
    public fun watchFaceComponentName() {
        val scenario = createOnWatchFaceEditingTestActivity(emptyList(), emptyList())
        scenario.onActivity {
            assertThat(it.editorSession.watchFaceComponentName)
                .isEqualTo(ComponentName("test.package", "test.class"))
        }
    }

    @Test
    public fun watchFaceComponentName_headless() {
        val scenario =
            createOnWatchFaceEditingTestActivity(
                emptyList(),
                emptyList(),
                headlessDeviceConfig = DeviceConfig(false, false, 0, 0),
                watchComponentName = headlessWatchFaceComponentName
            )
        lateinit var activity: OnWatchFaceEditingTestActivity
        scenario.onActivity { activity = it }
        runBlocking { activity.deferredDone.await() }
        assertThat(activity.editorSession.watchFaceComponentName)
            .isEqualTo(headlessWatchFaceComponentName)
    }

    @Test
    public fun instanceId() {
        val scenario = createOnWatchFaceEditingTestActivity(emptyList(), emptyList())
        scenario.onActivity {
            assertThat(it.editorSession.watchFaceId.id).isEqualTo(testInstanceId.id)
        }
    }

    @Test
    public fun instanceId_headless() {
        val scenario =
            createOnWatchFaceEditingTestActivity(
                emptyList(),
                emptyList(),
                headlessDeviceConfig = DeviceConfig(false, false, 0, 0),
                watchComponentName = headlessWatchFaceComponentName
            )
        lateinit var activity: OnWatchFaceEditingTestActivity
        scenario.onActivity { activity = it }
        runBlocking { activity.deferredDone.await() }
        assertThat(activity.editorSession.watchFaceId.id).isEqualTo(testInstanceId.id)
    }

    @Test
    public fun backgroundComplicationId_noBackgroundComplication() {
        val scenario = createOnWatchFaceEditingTestActivity(emptyList(), emptyList())
        scenario.onActivity {
            assertThat(it.editorSession.backgroundComplicationSlotId).isEqualTo(null)
        }
    }

    @Test
    public fun previewReferenceInstant() {
        val scenario =
            createOnWatchFaceEditingTestActivity(
                emptyList(),
                emptyList(),
                previewReferenceInstant = Instant.ofEpochMilli(54321L)
            )
        scenario.onActivity {
            assertThat(it.editorSession.previewReferenceInstant)
                .isEqualTo(Instant.ofEpochMilli(54321L))
        }
    }

    @Test
    @Suppress("Deprecation") // userStyleSettings
    public fun userStyleSchema() {
        val scenario =
            createOnWatchFaceEditingTestActivity(
                listOf(colorStyleSetting, watchHandStyleSetting),
                emptyList()
            )
        scenario.onActivity {
            val userStyleSchema = it.editorSession.userStyleSchema
            assertThat(userStyleSchema.userStyleSettings.size).isEqualTo(2)
            assertThat(userStyleSchema.userStyleSettings[0].id.value)
                .isEqualTo(colorStyleSetting.id.value)
            assertThat(userStyleSchema.userStyleSettings[0].options.size)
                .isEqualTo(colorStyleSetting.options.size)
            assertThat(userStyleSchema.userStyleSettings[1].id.value)
                .isEqualTo(watchHandStyleSetting.id.value)
            assertThat(userStyleSchema.userStyleSettings[1].options.size)
                .isEqualTo(watchHandStyleSetting.options.size)
            // We could test more state but this should be enough.
        }
    }

    @Test
    public fun backgroundComplicationId() {
        val scenario =
            createOnWatchFaceEditingTestActivity(emptyList(), listOf(backgroundComplication))
        scenario.onActivity {
            assertThat(it.editorSession.backgroundComplicationSlotId)
                .isEqualTo(BACKGROUND_COMPLICATION_ID)
        }
    }

    @Test
    @Suppress("DEPRECATION") // defaultDataSourceType
    public fun complicationState() {
        val scenario =
            createOnWatchFaceEditingTestActivity(
                emptyList(),
                listOf(leftComplication, rightComplication, backgroundComplication)
            )
        scenario.onActivity {
            val complicationSlotsState = it.editorSession.complicationSlotsState.value
            assertThat(complicationSlotsState.size).isEqualTo(3)
            val leftSlot = complicationSlotsState[LEFT_COMPLICATION_ID]!!
            assertThat(leftSlot.bounds).isEqualTo(Rect(120, 160, 160, 240))
            assertThat(leftSlot.boundsType).isEqualTo(ComplicationSlotBoundsType.ROUND_RECT)
            assertFalse(leftSlot.fixedComplicationDataSource)
            assertTrue(leftSlot.isInitiallyEnabled)
            assertThat(leftSlot.defaultDataSourcePolicy.primaryDataSource)
                .isEqualTo(ComponentName("com.primary.package", "com.primary.app"))
            assertThat(leftSlot.defaultDataSourcePolicy.primaryDataSourceDefaultType)
                .isEqualTo(ComplicationType.LONG_TEXT)
            assertThat(leftSlot.defaultDataSourcePolicy.secondaryDataSource).isNull()
            assertThat(leftSlot.defaultDataSourcePolicy.secondaryDataSourceDefaultType).isNull()
            assertThat(leftSlot.defaultDataSourcePolicy.systemDataSourceFallback)
                .isEqualTo(SystemDataSources.DATA_SOURCE_SUNRISE_SUNSET)
            assertThat(leftSlot.defaultDataSourcePolicy.systemDataSourceFallbackDefaultType)
                .isEqualTo(ComplicationType.SHORT_TEXT)
            assertThat(leftSlot.defaultDataSourceType).isEqualTo(ComplicationType.SHORT_TEXT)

            val rightSlot = complicationSlotsState[RIGHT_COMPLICATION_ID]!!
            assertThat(rightSlot.bounds).isEqualTo(Rect(240, 160, 280, 240))
            assertThat(rightSlot.boundsType).isEqualTo(ComplicationSlotBoundsType.ROUND_RECT)
            assertFalse(rightSlot.fixedComplicationDataSource)
            assertTrue(rightSlot.isInitiallyEnabled)
            assertThat(rightSlot.defaultDataSourcePolicy.primaryDataSource)
                .isEqualTo(ComponentName("com.primary.package", "com.primary.app"))
            assertThat(rightSlot.defaultDataSourcePolicy.primaryDataSourceDefaultType)
                .isEqualTo(ComplicationType.LONG_TEXT)
            assertThat(rightSlot.defaultDataSourcePolicy.secondaryDataSource)
                .isEqualTo(ComponentName("com.secondary.package", "com.secondary.app"))
            assertThat(rightSlot.defaultDataSourcePolicy.secondaryDataSourceDefaultType)
                .isEqualTo(ComplicationType.PHOTO_IMAGE)
            assertThat(rightSlot.defaultDataSourcePolicy.systemDataSourceFallbackDefaultType)
                .isEqualTo(ComplicationType.SHORT_TEXT)
            assertThat(rightSlot.defaultDataSourceType).isEqualTo(ComplicationType.SHORT_TEXT)

            val backgroundSlot = complicationSlotsState[BACKGROUND_COMPLICATION_ID]!!
            assertThat(backgroundSlot.bounds).isEqualTo(screenBounds)
            assertThat(backgroundSlot.boundsType).isEqualTo(ComplicationSlotBoundsType.BACKGROUND)
            assertFalse(backgroundSlot.fixedComplicationDataSource)
            assertFalse(backgroundSlot.isInitiallyEnabled)
            // We could test more state but this should be enough.
        }
    }

    @OptIn(ComplicationExperimental::class)
    @Suppress("DEPRECATION") // Old DefaultComplicationDataSourcePolicy constructor
    @Test
    public fun fixedComplicationDataSource() {
        val mockLeftCanvasComplication =
            CanvasComplicationDrawable(
                ComplicationDrawable(),
                placeholderWatchState,
                mockInvalidateCallback
            )
        val fixedLeftComplication =
            ComplicationSlot.createRoundRectComplicationSlotBuilder(
                    LEFT_COMPLICATION_ID,
                    { _, _ -> mockLeftCanvasComplication },
                    listOf(
                        ComplicationType.RANGED_VALUE,
                        ComplicationType.LONG_TEXT,
                        ComplicationType.SHORT_TEXT,
                        ComplicationType.MONOCHROMATIC_IMAGE,
                        ComplicationType.SMALL_IMAGE
                    ),
                    DefaultComplicationDataSourcePolicy(
                        SystemDataSources.DATA_SOURCE_SUNRISE_SUNSET
                    ),
                    ComplicationSlotBounds(RectF(0.2f, 0.4f, 0.4f, 0.6f))
                )
                .setDefaultDataSourceType(ComplicationType.SHORT_TEXT)
                .setFixedComplicationDataSource(true)
                .build()

        val scenario =
            createOnWatchFaceEditingTestActivity(emptyList(), listOf(fixedLeftComplication))
        scenario.onActivity {
            assertTrue(
                it.editorSession.complicationSlotsState.value[LEFT_COMPLICATION_ID]!!
                    .fixedComplicationDataSource
            )

            try {
                runBlocking {
                    it.editorSession.openComplicationDataSourceChooser(LEFT_COMPLICATION_ID)

                    fail(
                        "openComplicationDataSourceChooser should fail for a fixed complication " +
                            "data source"
                    )
                }
            } catch (e: Exception) {
                // Expected.
            }
        }
    }

    @Test
    public fun getPreviewData_null_dataSourceInfo() {
        val scenario =
            createOnWatchFaceEditingTestActivity(
                emptyList(),
                listOf(leftComplication, rightComplication, backgroundComplication)
            )
        scenario.onActivity {
            runBlocking {
                val editorSession = it.editorSession as OnWatchFaceEditorSessionImpl
                val mockProviderInfoService = Mockito.mock(IProviderInfoService::class.java)

                val dataSourceInfoRetriever =
                    ComplicationDataSourceInfoRetriever(mockProviderInfoService)
                assertThat(editorSession.getPreviewData(dataSourceInfoRetriever, null)).isNull()
                dataSourceInfoRetriever.close()
            }
        }
    }

    @Test
    public fun getPreviewData() {
        val scenario =
            createOnWatchFaceEditingTestActivity(
                emptyList(),
                listOf(leftComplication, rightComplication, backgroundComplication)
            )
        scenario.onActivity {
            runBlocking {
                val editorSession = it.editorSession as OnWatchFaceEditorSessionImpl
                val mockProviderInfoService = Mockito.mock(IProviderInfoService::class.java)
                val complicationType = ComplicationType.SHORT_TEXT
                val complicationText = "TestText"
                val mockBinder = Mockito.mock(IBinder::class.java)

                `when`(mockProviderInfoService.apiVersion).thenReturn(1)
                `when`(mockProviderInfoService.asBinder()).thenReturn(mockBinder)

                doAnswer {
                        val callback = it.arguments[2] as IPreviewComplicationDataCallback
                        callback.updateComplicationData(
                            ShortTextComplicationData.Builder(
                                    PlainComplicationText.Builder(complicationText).build(),
                                    ComplicationText.EMPTY
                                )
                                .build()
                                .asWireComplicationData()
                        )
                        true
                    }
                    .`when`(mockProviderInfoService)
                    .requestPreviewComplicationData(
                        eq(dataSourceComponentName),
                        eq(complicationType.toWireComplicationType()),
                        any()
                    )

                val complicationDataSourceInfoRetriever =
                    ComplicationDataSourceInfoRetriever(mockProviderInfoService)
                val deferredPreviewData = async {
                    editorSession.getPreviewData(
                        complicationDataSourceInfoRetriever,
                        ComplicationDataSourceInfo(
                            "dataSource.app",
                            "dataSource",
                            dataSourceIcon,
                            complicationType,
                            dataSourceComponentName
                        )
                    )
                }

                val result = deferredPreviewData.await()
                assertThat(result).isInstanceOf(ShortTextComplicationData::class.java)
                assertThat(
                        (result as ShortTextComplicationData)
                            .text
                            .getTextAt(
                                ApplicationProvider.getApplicationContext<Context>().resources,
                                Instant.EPOCH
                            )
                    )
                    .isEqualTo(complicationText)

                complicationDataSourceInfoRetriever.close()
            }
        }
    }

    @Test
    public fun getPreviewData_dataSourceSendsWrongType() {
        val scenario =
            createOnWatchFaceEditingTestActivity(
                emptyList(),
                listOf(leftComplication, rightComplication, backgroundComplication)
            )
        scenario.onActivity {
            runBlocking {
                val editorSession = it.editorSession as OnWatchFaceEditorSessionImpl
                val mockProviderInfoService = Mockito.mock(IProviderInfoService::class.java)
                val complicationType = ComplicationType.LONG_TEXT
                val complicationText = "TestText"
                val mockBinder = Mockito.mock(IBinder::class.java)

                `when`(mockProviderInfoService.apiVersion).thenReturn(1)
                `when`(mockProviderInfoService.asBinder()).thenReturn(mockBinder)

                doAnswer {
                        val callback = it.arguments[2] as IPreviewComplicationDataCallback
                        callback.updateComplicationData(
                            ShortTextComplicationData.Builder(
                                    PlainComplicationText.Builder(complicationText).build(),
                                    ComplicationText.EMPTY
                                )
                                .build()
                                .asWireComplicationData()
                        )
                        true
                    }
                    .`when`(mockProviderInfoService)
                    .requestPreviewComplicationData(
                        eq(dataSourceComponentName),
                        eq(complicationType.toWireComplicationType()),
                        any()
                    )

                val complicationDataSourceInfoRetriever =
                    ComplicationDataSourceInfoRetriever(mockProviderInfoService)
                val deferredPreviewData = async {
                    editorSession.getPreviewData(
                        complicationDataSourceInfoRetriever,
                        ComplicationDataSourceInfo(
                            "dataSource.app",
                            "dataSource",
                            dataSourceIcon,
                            complicationType,
                            dataSourceComponentName
                        )
                    )
                }

                val result = deferredPreviewData.await()
                assertThat(result).isInstanceOf(LongTextComplicationData::class.java)
                assertThat(
                        (result as LongTextComplicationData)
                            .text
                            .getTextAt(
                                ApplicationProvider.getApplicationContext<Context>().resources,
                                Instant.EPOCH
                            )
                    )
                    .isEqualTo("dataSource") // Fallback has been used.

                complicationDataSourceInfoRetriever.close()
            }
        }
    }

    @Test
    @SdkSuppress(maxSdkVersion = 28)
    public fun getPreviewData_preRFallback() {
        val scenario =
            createOnWatchFaceEditingTestActivity(
                emptyList(),
                listOf(leftComplication, rightComplication, backgroundComplication)
            )
        scenario.onActivity {
            runBlocking {
                val editorSession = it.editorSession as OnWatchFaceEditorSessionImpl
                val mockProviderInfoService = Mockito.mock(IProviderInfoService::class.java)
                val complicationType = ComplicationType.SHORT_TEXT

                val complicationDataSourceInfoRetriever =
                    ComplicationDataSourceInfoRetriever(mockProviderInfoService)
                val previewComplication =
                    editorSession.getPreviewData(
                        complicationDataSourceInfoRetriever,
                        // Construct a ComplicationDataSourceInfo with null componentName.
                        ComplicationDataSourceInfo(
                            "dataSource.app",
                            "dataSource",
                            dataSourceIcon,
                            complicationType,
                            null,
                        )
                    ) as ShortTextComplicationData

                assertThat(
                        previewComplication.text.getTextAt(
                            ApplicationProvider.getApplicationContext<Context>().resources,
                            Instant.EPOCH
                        )
                    )
                    .isEqualTo("dataSou")

                complicationDataSourceInfoRetriever.close()
            }
        }
    }

    @Test
    @SdkSuppress(minSdkVersion = 30)
    public fun getPreviewData_postRFallback() {
        val scenario =
            createOnWatchFaceEditingTestActivity(
                emptyList(),
                listOf(leftComplication, rightComplication, backgroundComplication)
            )
        scenario.onActivity {
            runBlocking {
                val editorSession = it.editorSession as OnWatchFaceEditorSessionImpl
                val mockProviderInfoService = Mockito.mock(IProviderInfoService::class.java)
                val complicationType = ComplicationType.SHORT_TEXT

                `when`(mockProviderInfoService.apiVersion).thenReturn(1)
                `when`(
                        mockProviderInfoService.requestPreviewComplicationData(
                            eq(dataSourceComponentName),
                            eq(complicationType.toWireComplicationType()),
                            any(IPreviewComplicationDataCallback::class.java)
                        )
                    )
                    .thenReturn(false) // Triggers the ExecutionException.

                val previewComplication =
                    editorSession.getPreviewData(
                        ComplicationDataSourceInfoRetriever(mockProviderInfoService),
                        ComplicationDataSourceInfo(
                            "dataSource.app",
                            "dataSource",
                            dataSourceIcon,
                            complicationType,
                            dataSourceComponentName
                        )
                    ) as ShortTextComplicationData

                assertThat(
                        previewComplication.text.getTextAt(
                            ApplicationProvider.getApplicationContext<Context>().resources,
                            Instant.EPOCH
                        )
                    )
                    .isEqualTo("dataSou") // Fallback truncates for short text.
            }
        }
    }

    @Suppress("DEPRECATION")
    @Test
    public fun launchComplicationDataSourceChooser() {
        ComplicationDataSourceChooserContract.useTestComplicationHelperActivity = true
        val chosenComplicationDataSourceInfo =
            ComplicationDataSourceInfo(
                "TestDataSource3App",
                "TestDataSource3",
                Icon.createWithBitmap(Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)),
                ComplicationType.LONG_TEXT,
                dataSource3
            )
        TestComplicationHelperActivity.resultIntent =
            CompletableDeferred(
                Intent().apply {
                    putExtra(
                        ComplicationDataSourceChooserIntent.EXTRA_PROVIDER_INFO,
                        chosenComplicationDataSourceInfo.toWireComplicationProviderInfo()
                    )
                }
            )

        val scenario =
            createOnWatchFaceEditingTestActivity(
                emptyList(),
                listOf(leftComplication, rightComplication)
            )

        lateinit var editorSession: EditorSession
        scenario.onActivity { activity -> editorSession = activity.editorSession }

        runBlocking {
            assertThat(editorSession.complicationSlotsState.value[LEFT_COMPLICATION_ID]!!.bounds)
                .isEqualTo(Rect(120, 160, 160, 240))
            assertThat(editorSession.complicationsDataSourceInfo.value[LEFT_COMPLICATION_ID]!!.name)
                .isEqualTo("DataSource1")

            /**
             * Invoke [TestComplicationHelperActivity] which will change the complication data
             * source (and hence the preview data) for [LEFT_COMPLICATION_ID].
             */
            val chosenComplicationDataSource =
                editorSession.openComplicationDataSourceChooser(LEFT_COMPLICATION_ID)
            assertThat(chosenComplicationDataSource).isNotNull()
            checkNotNull(chosenComplicationDataSource)
            assertThat(chosenComplicationDataSource.complicationSlotId)
                .isEqualTo(LEFT_COMPLICATION_ID)
            assertEquals(
                chosenComplicationDataSourceInfo,
                chosenComplicationDataSource.complicationDataSourceInfo
            )

            // This should update the preview data to point to the updated DataSource3 data.
            val previewComplication =
                editorSession.complicationsPreviewData.value[LEFT_COMPLICATION_ID]
                    as LongTextComplicationData

            assertThat(
                    previewComplication.text.getTextAt(
                        ApplicationProvider.getApplicationContext<Context>().resources,
                        Instant.EPOCH
                    )
                )
                .isEqualTo("DataSource3")

            assertThat(editorSession.complicationsDataSourceInfo.value[LEFT_COMPLICATION_ID]!!.name)
                .isEqualTo("TestDataSource3")

            assertThat(
                    TestComplicationHelperActivity.lastIntent
                        ?.extras
                        ?.getString(ComplicationDataSourceChooserIntent.EXTRA_WATCHFACE_INSTANCE_ID)
                )
                .isEqualTo(testInstanceId.id)

            assertThat(
                    (TestComplicationHelperActivity.lastIntent?.getParcelableExtra(
                            ComplicationDataSourceChooserIntent.EXTRA_COMPLICATION_DENIED
                        ) as Intent?)
                        ?.action
                )
                .isEqualTo(complicationDeniedDialogIntent.action)

            assertThat(
                    (TestComplicationHelperActivity.lastIntent?.getParcelableExtra(
                            ComplicationDataSourceChooserIntent.EXTRA_COMPLICATION_RATIONALE
                        ) as Intent?)
                        ?.action
                )
                .isEqualTo(complicationRationaleDialogIntent.action)

            // Selecting a LONG_TEXT complication should enlarge the complication's bounds due to
            // it's set up.
            assertThat(editorSession.complicationSlotsState.value[LEFT_COMPLICATION_ID]!!.bounds)
                .isEqualTo(Rect(40, 160, 160, 240))
        }
    }

    @Test
    public fun launchComplicationDataSourceChooserTwiceBackToBack() {
        ComplicationDataSourceChooserContract.useTestComplicationHelperActivity = true
        TestComplicationHelperActivity.resultIntent =
            CompletableDeferred(
                Intent().apply {
                    putExtra(
                        ComplicationDataSourceChooserIntent.EXTRA_PROVIDER_INFO,
                        ComplicationDataSourceInfo(
                                "TestDataSource3App",
                                "TestDataSource3",
                                Icon.createWithBitmap(
                                    Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
                                ),
                                ComplicationType.LONG_TEXT,
                                dataSource3
                            )
                            .toWireComplicationProviderInfo()
                    )
                }
            )

        val scenario =
            createOnWatchFaceEditingTestActivity(
                emptyList(),
                listOf(leftComplication, rightComplication)
            )

        lateinit var editorSession: EditorSession
        scenario.onActivity { activity -> editorSession = activity.editorSession }

        runBlocking {
            assertThat(editorSession.openComplicationDataSourceChooser(LEFT_COMPLICATION_ID))
                .isNotNull()

            // This shouldn't crash.
            assertThat(editorSession.openComplicationDataSourceChooser(LEFT_COMPLICATION_ID))
                .isNotNull()
        }
    }

    @Test
    public fun launchConcurrentComplicationDataSourceChoosers() {
        ComplicationDataSourceChooserContract.useTestComplicationHelperActivity = true
        TestComplicationHelperActivity.resultIntent = CompletableDeferred()

        val scenario =
            createOnWatchFaceEditingTestActivity(
                emptyList(),
                listOf(leftComplication, rightComplication)
            )

        lateinit var editorSession: EditorSession
        scenario.onActivity { activity -> editorSession = activity.editorSession }

        runBlocking {
            // This won't complete till later.
            val firstComplicationDataSourceChooserResult = async {
                editorSession.openComplicationDataSourceChooser(LEFT_COMPLICATION_ID)
            }

            async {
                try {
                    editorSession.openComplicationDataSourceChooser(LEFT_COMPLICATION_ID)
                    fail("A concurrent openComplicationDataSourceChooser should throw an exception")
                } catch (e: Exception) {
                    assertThat(e).isInstanceOf(IllegalStateException::class.java)
                }

                // Allow firstComplicationDataSourceChooserResult to complete.
                TestComplicationHelperActivity.resultIntent!!.complete(
                    Intent().apply {
                        putExtra(
                            ComplicationDataSourceChooserIntent.EXTRA_PROVIDER_INFO,
                            ComplicationDataSourceInfo(
                                    "TestDataSource3App",
                                    "TestDataSource3",
                                    Icon.createWithBitmap(
                                        Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
                                    ),
                                    ComplicationType.LONG_TEXT,
                                    dataSource3
                                )
                                .toWireComplicationProviderInfo()
                        )
                    }
                )
            }

            assertThat(firstComplicationDataSourceChooserResult.await()).isNotNull()
        }
    }

    @Test
    public fun launchComplicationDataSourceChooser_chooseEmpty() {
        ComplicationDataSourceChooserContract.useTestComplicationHelperActivity = true
        TestComplicationHelperActivity.resultIntent = CompletableDeferred(Intent())

        val scenario =
            createOnWatchFaceEditingTestActivity(
                emptyList(),
                listOf(leftComplication, rightComplication)
            )

        lateinit var editorSession: EditorSession
        scenario.onActivity { activity -> editorSession = activity.editorSession }

        runBlocking {
            /**
             * Invoke [TestComplicationHelperActivity] which will change the complication data
             * source (and hence the preview data) for [LEFT_COMPLICATION_ID].
             */
            val chosenComplicationDataSource =
                editorSession.openComplicationDataSourceChooser(LEFT_COMPLICATION_ID)
            assertThat(chosenComplicationDataSource).isNotNull()
            checkNotNull(chosenComplicationDataSource)
            assertThat(chosenComplicationDataSource.complicationSlotId)
                .isEqualTo(LEFT_COMPLICATION_ID)
            assertThat(chosenComplicationDataSource.complicationDataSourceInfo).isNull()
            assertThat(editorSession.complicationsPreviewData.value[LEFT_COMPLICATION_ID])
                .isInstanceOf(EmptyComplicationData::class.java)
        }
    }

    @Test
    public fun launchComplicationDataSourceChooser_cancel() {
        ComplicationDataSourceChooserContract.useTestComplicationHelperActivity = true
        // NB CompletableDeferred(null) doesn't do what we expect...
        TestComplicationHelperActivity.resultIntent =
            CompletableDeferred<Intent?>().apply { complete(null) }

        val scenario =
            createOnWatchFaceEditingTestActivity(
                emptyList(),
                listOf(leftComplication, rightComplication)
            )

        lateinit var editorSession: EditorSession
        scenario.onActivity { activity -> editorSession = activity.editorSession }

        runBlocking {
            /** Invoke [TestComplicationHelperActivity] which will simulate the user canceling. */
            assertThat(editorSession.openComplicationDataSourceChooser(LEFT_COMPLICATION_ID))
                .isNull()
        }
    }

    @Suppress("DEPRECATION")
    @Test
    public fun launchComplicationDataSourceChooser_ComplicationConfigExtrasToHelper() {
        ComplicationDataSourceChooserContract.useTestComplicationHelperActivity = true
        val chosenComplicationDataSourceInfo =
            ComplicationDataSourceInfo(
                "TestDataSource3App",
                "TestDataSource3",
                Icon.createWithBitmap(Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)),
                ComplicationType.LONG_TEXT,
                dataSource3
            )
        TestComplicationHelperActivity.resultIntent =
            CompletableDeferred(
                Intent().apply {
                    putExtra(
                        ComplicationDataSourceChooserIntent.EXTRA_PROVIDER_INFO,
                        chosenComplicationDataSourceInfo.toWireComplicationProviderInfo()
                    )
                    putExtra(PROVIDER_CHOOSER_RESULT_EXTRA_KEY, PROVIDER_CHOOSER_RESULT_EXTRA_VALUE)
                }
            )

        val scenario =
            createOnWatchFaceEditingTestActivity(
                emptyList(),
                listOf(leftComplication, rightComplication)
            )

        lateinit var editorSession: EditorSession
        scenario.onActivity { activity -> editorSession = activity.editorSession }

        runBlocking {
            val chosenComplicationDataSource =
                editorSession.openComplicationDataSourceChooser(RIGHT_COMPLICATION_ID)
            assertThat(chosenComplicationDataSource).isNotNull()
            checkNotNull(chosenComplicationDataSource)
            assertThat(chosenComplicationDataSource.complicationSlotId)
                .isEqualTo(RIGHT_COMPLICATION_ID)
            assertEquals(
                chosenComplicationDataSourceInfo,
                chosenComplicationDataSource.complicationDataSourceInfo
            )
            assertThat(chosenComplicationDataSource.extras[PROVIDER_CHOOSER_RESULT_EXTRA_KEY])
                .isEqualTo(PROVIDER_CHOOSER_RESULT_EXTRA_VALUE)

            assertThat(
                    TestComplicationHelperActivity.lastIntent
                        ?.extras
                        ?.getString(PROVIDER_CHOOSER_EXTRA_KEY)
                )
                .isEqualTo(PROVIDER_CHOOSER_EXTRA_VALUE)
        }
    }

    @Suppress("DEPRECATION")
    @Test
    public fun launchComplicationDataSourceChooser_ComplicationConfigExtrasToChooser() {
        // Invoke the test data source chooser to record the result.
        ComplicationHelperActivity.useTestComplicationDataSourceChooserActivity = true
        // Invoke the data source chooser without checking for permissions first.
        ComplicationHelperActivity.skipPermissionCheck = true

        val chosenComplicationDataSourceInfo =
            ComplicationDataSourceInfo(
                "TestDataSource3App",
                "TestDataSource3",
                Icon.createWithBitmap(Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)),
                ComplicationType.LONG_TEXT,
                dataSource3
            )
        TestComplicationDataSourceChooserActivity.resultIntent =
            Intent().apply {
                putExtra(
                    ComplicationDataSourceChooserIntent.EXTRA_PROVIDER_INFO,
                    chosenComplicationDataSourceInfo.toWireComplicationProviderInfo()
                )
                putExtra(PROVIDER_CHOOSER_RESULT_EXTRA_KEY, PROVIDER_CHOOSER_RESULT_EXTRA_VALUE)
            }

        val scenario =
            createOnWatchFaceEditingTestActivity(
                emptyList(),
                listOf(leftComplication, rightComplication)
            )

        lateinit var editorSession: EditorSession
        scenario.onActivity { activity -> editorSession = activity.editorSession }

        runBlocking {
            val chosenComplicationDataSource =
                editorSession.openComplicationDataSourceChooser(RIGHT_COMPLICATION_ID)
            assertThat(chosenComplicationDataSource).isNotNull()
            checkNotNull(chosenComplicationDataSource)
            assertThat(chosenComplicationDataSource.complicationSlotId)
                .isEqualTo(RIGHT_COMPLICATION_ID)
            assertEquals(
                chosenComplicationDataSourceInfo,
                chosenComplicationDataSource.complicationDataSourceInfo
            )
            assertThat(chosenComplicationDataSource.extras[PROVIDER_CHOOSER_RESULT_EXTRA_KEY])
                .isEqualTo(PROVIDER_CHOOSER_RESULT_EXTRA_VALUE)

            assertThat(
                    TestComplicationDataSourceChooserActivity.lastIntent
                        ?.extras
                        ?.getString(PROVIDER_CHOOSER_EXTRA_KEY)
                )
                .isEqualTo(PROVIDER_CHOOSER_EXTRA_VALUE)
        }
    }

    @Test
    public fun mutable_configExtras() {
        // Invoke the test data source chooser to record the result.
        ComplicationHelperActivity.useTestComplicationDataSourceChooserActivity = true
        // Invoke the data source chooser without checking for permissions first.
        ComplicationHelperActivity.skipPermissionCheck = true

        val chosenComplicationDataSourceInfo =
            ComplicationDataSourceInfo(
                "TestDataSource3App",
                "TestDataSource3",
                Icon.createWithBitmap(Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)),
                ComplicationType.LONG_TEXT,
                dataSource3
            )
        TestComplicationDataSourceChooserActivity.lastIntent = null
        TestComplicationDataSourceChooserActivity.resultIntent =
            Intent().apply {
                putExtra(
                    ComplicationDataSourceChooserIntent.EXTRA_PROVIDER_INFO,
                    chosenComplicationDataSourceInfo.toWireComplicationProviderInfo()
                )
            }

        val scenario =
            createOnWatchFaceEditingTestActivity(
                emptyList(),
                listOf(leftComplication, rightComplication)
            )

        lateinit var editorSession: EditorSession
        scenario.onActivity { activity -> editorSession = activity.editorSession }

        runBlocking {
            rightComplication.configExtras =
                Bundle().apply { putString(PROVIDER_CHOOSER_EXTRA_KEY, "Updated") }

            val chosenComplicationDataSource =
                editorSession.openComplicationDataSourceChooser(RIGHT_COMPLICATION_ID)
            assertThat(chosenComplicationDataSource).isNotNull()

            assertThat(
                    TestComplicationDataSourceChooserActivity.lastIntent
                        ?.extras
                        ?.getString(PROVIDER_CHOOSER_EXTRA_KEY)
                )
                .isEqualTo("Updated")
        }
    }

    @Test
    public fun getComplicationIdAt() {
        val scenario =
            createOnWatchFaceEditingTestActivity(
                emptyList(),
                listOf(leftComplication, rightComplication, backgroundComplication)
            )
        scenario.onActivity {
            assertThat(it.editorSession.getComplicationSlotIdAt(0, 0)).isEqualTo(null)
            assertThat(it.editorSession.getComplicationSlotIdAt(125, 165))
                .isEqualTo(LEFT_COMPLICATION_ID)
            assertThat(it.editorSession.getComplicationSlotIdAt(245, 165))
                .isEqualTo(RIGHT_COMPLICATION_ID)
        }
    }

    @Test
    public fun renderWatchFaceToBitmap() {
        val scenario = createOnWatchFaceEditingTestActivity(emptyList(), emptyList())

        scenario.onActivity {
            assertThat(
                    it.editorSession.renderWatchFaceToBitmap(
                        RenderParameters.DEFAULT_INTERACTIVE,
                        Instant.ofEpochMilli(1234L),
                        null
                    )
                )
                .isEqualTo(fakeBitmap)
        }
    }

    @Test
    public fun initialUserStyle() {
        val scenario =
            createOnWatchFaceEditingTestActivity(
                listOf(colorStyleSetting, watchHandStyleSetting),
                listOf(leftComplication, rightComplication),
                initialUserStyle =
                    UserStyleData(
                        mapOf(
                            colorStyleSetting.id.value to greenStyleOption.id.value,
                            watchHandStyleSetting.id.value to modernStyleOption.id.value,
                        )
                    )
            )

        scenario.onActivity { activity ->
            assertThat(activity.editorSession.userStyle.value[colorStyleSetting])
                .isEqualTo(greenStyleOption)
            assertThat(activity.editorSession.userStyle.value[watchHandStyleSetting])
                .isEqualTo(modernStyleOption)
        }
    }

    @Suppress("NewApi", "Deprecation") // result.watchFaceId,  deprecation: userStyleSettings
    @Test
    public fun userStyleAndComplicationPreviewDataInEditorObserver() {
        val scenario =
            createOnWatchFaceEditingTestActivity(
                listOf(colorStyleSetting, watchHandStyleSetting),
                listOf(leftComplication, rightComplication)
            )

        val editorObserver = TestEditorObserver()
        val observerId = EditorService.globalEditorService.registerObserver(editorObserver)

        scenario.onActivity { activity ->
            // Select [blueStyleOption] and [gothicStyleOption].
            val mutableUserStyle = activity.editorSession.userStyle.value.toMutableUserStyle()
            for (userStyleSetting in activity.editorSession.userStyleSchema.userStyleSettings) {
                mutableUserStyle[userStyleSetting] = userStyleSetting.options.last()
            }
            activity.editorSession.userStyle.value = mutableUserStyle.toUserStyle()
            activity.editorSession.close()
            activity.finish()
        }

        val result =
            editorObserver
                .awaitEditorStateChange(TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)
                .asApiEditorState()

        assertThat(result.userStyle.userStyleMap[colorStyleSetting.id.value])
            .isEqualTo(blueStyleOption.id.value)
        assertThat(result.userStyle.userStyleMap[watchHandStyleSetting.id.value])
            .isEqualTo(gothicStyleOption.id.value)
        assertThat(result.watchFaceId.id).isEqualTo(testInstanceId.id)
        assertTrue(result.shouldCommitChanges)

        // The style change should also be applied to the watchface.
        assertThat(editorDelegate.userStyle[colorStyleSetting]!!.id.value)
            .isEqualTo(blueStyleOption.id.value)
        assertThat(editorDelegate.userStyle[watchHandStyleSetting]!!.id.value)
            .isEqualTo(gothicStyleOption.id.value)

        assertThat(result.previewComplicationsData.size).isEqualTo(2)
        val leftComplicationData =
            result.previewComplicationsData[LEFT_COMPLICATION_ID] as ShortTextComplicationData
        assertThat(
                leftComplicationData.text.getTextAt(
                    ApplicationProvider.getApplicationContext<Context>().resources,
                    Instant.EPOCH
                )
            )
            .isEqualTo("Left")

        val rightComplicationData =
            result.previewComplicationsData[RIGHT_COMPLICATION_ID] as LongTextComplicationData
        assertThat(
                rightComplicationData.text.getTextAt(
                    ApplicationProvider.getApplicationContext<Context>().resources,
                    Instant.EPOCH
                )
            )
            .isEqualTo("Right")

        EditorService.globalEditorService.unregisterObserver(observerId)
    }

    @Suppress("NewApi") // result.watchFaceId
    @Test
    public fun editorStatePreviewComplicationData_onlyContainsEnabledComplications() {
        val scenario =
            createOnWatchFaceEditingTestActivity(
                listOf(complicationsStyleSetting),
                listOf(leftComplication, rightComplication)
            )

        val editorObserver = TestEditorObserver()
        val observerId = EditorService.globalEditorService.registerObserver(editorObserver)

        scenario.onActivity { activity ->
            // Hide the left complication.
            val mutableUserStyle = activity.editorSession.userStyle.value.toMutableUserStyle()
            mutableUserStyle[complicationsStyleSetting] = rightOnlyComplicationsOption
            activity.editorSession.userStyle.value = mutableUserStyle.toUserStyle()
            activity.editorSession.close()
            activity.finish()
        }

        val result =
            editorObserver
                .awaitEditorStateChange(TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)
                .asApiEditorState()

        assertThat(result.previewComplicationsData.size).isEqualTo(1)
        assertThat(result.previewComplicationsData[RIGHT_COMPLICATION_ID]).isNotNull()
        EditorService.globalEditorService.unregisterObserver(observerId)
    }

    @SuppressLint("NewApi") // result.watchFaceId
    @Test
    public fun emptyInstanceId() {
        val scenario =
            createOnWatchFaceEditingTestActivity(
                listOf(colorStyleSetting, watchHandStyleSetting),
                emptyList(),
                watchFaceId = WatchFaceId("")
            )

        val editorObserver = TestEditorObserver()
        val observerId = EditorService.globalEditorService.registerObserver(editorObserver)

        scenario.onActivity { activity ->
            runBlocking {
                assertThat(activity.editorSession.watchFaceId.id).isEqualTo(DEFAULT_INSTANCE_ID)
                activity.editorSession.close()
                activity.finish()
            }
        }

        val result =
            editorObserver
                .awaitEditorStateChange(TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)
                .asApiEditorState()

        // We need to return the same ID we were sent (or lack there of).
        assertThat(result.watchFaceId.id).isEmpty()

        EditorService.globalEditorService.unregisterObserver(observerId)
    }

    @SuppressLint("NewApi") // result.watchFaceId
    @Test
    public fun invalidOldStyleInstanceId() {
        val scenario =
            createOnWatchFaceEditingTestActivity(
                listOf(colorStyleSetting, watchHandStyleSetting),
                emptyList(),
                watchFaceId = WatchFaceId("instance-1")
            )

        val editorObserver = TestEditorObserver()
        val observerId = EditorService.globalEditorService.registerObserver(editorObserver)

        scenario.onActivity { activity ->
            runBlocking {
                assertThat(activity.editorSession.watchFaceId.id).isEqualTo(DEFAULT_INSTANCE_ID)
                activity.editorSession.close()
                activity.finish()
            }
        }

        val result =
            editorObserver
                .awaitEditorStateChange(TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)
                .asApiEditorState()

        // We need to return the same ID we were sent.
        assertThat(result.watchFaceId.id).isEqualTo("instance-1")

        EditorService.globalEditorService.unregisterObserver(observerId)
    }

    @Test
    public fun emptyComplicationPreviewDataInEditorObserver() {
        val scenario = createOnWatchFaceEditingTestActivity(emptyList(), emptyList())
        val editorObserver = TestEditorObserver()
        val observerId = EditorService.globalEditorService.registerObserver(editorObserver)
        scenario.onActivity { activity ->
            runBlocking {
                activity.editorSession.close()
                activity.finish()
            }
        }

        val result =
            editorObserver
                .awaitEditorStateChange(TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)
                .asApiEditorState()
        assertThat(result.previewComplicationsData).isEmpty()

        EditorService.globalEditorService.unregisterObserver(observerId)
    }

    @SdkSuppress(maxSdkVersion = 32) // b/275361339
    @Test
    @Suppress("Deprecation") // userStyleSettings
    public fun commit_headless() {
        val scenario =
            createOnWatchFaceEditingTestActivity(
                listOf(colorStyleSetting, watchHandStyleSetting),
                emptyList(),
                headlessDeviceConfig = DeviceConfig(false, false, 0, 0),
                watchComponentName = headlessWatchFaceComponentName
            )
        val editorObserver = TestEditorObserver()
        val observerId = EditorService.globalEditorService.registerObserver(editorObserver)
        scenario.onActivity { activity ->
            runBlocking { activity.deferredDone.await() }
            assertThat(editorDelegate.userStyle[colorStyleSetting]!!.id.value)
                .isEqualTo(redStyleOption.id.value)
            assertThat(editorDelegate.userStyle[watchHandStyleSetting]!!.id.value)
                .isEqualTo(classicStyleOption.id.value)

            // Select [blueStyleOption] and [gothicStyleOption].
            val mutableUserStyle = activity.editorSession.userStyle.value.toMutableUserStyle()
            for (userStyleSetting in activity.editorSession.userStyleSchema.userStyleSettings) {
                mutableUserStyle[userStyleSetting] = userStyleSetting.options.last()
            }
            activity.editorSession.userStyle.value = mutableUserStyle.toUserStyle()

            // The editorDelegate should be unaffected because a separate headless instance is
            // used.
            assertThat(editorDelegate.userStyle[colorStyleSetting]!!.id.value)
                .isEqualTo(redStyleOption.id.value)
            assertThat(editorDelegate.userStyle[watchHandStyleSetting]!!.id.value)
                .isEqualTo(classicStyleOption.id.value)

            activity.editorSession.close()
            activity.finish()
        }

        val result =
            editorObserver
                .awaitEditorStateChange(TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)
                .asApiEditorState()
        assertThat(result.userStyle.userStyleMap[colorStyleSetting.id.value])
            .isEqualTo(blueStyleOption.id.value)
        assertThat(result.userStyle.userStyleMap[watchHandStyleSetting.id.value])
            .isEqualTo(gothicStyleOption.id.value)
        assertTrue(result.shouldCommitChanges)
        assertNull(result.previewImage)

        EditorService.globalEditorService.unregisterObserver(observerId)
    }

    @SdkSuppress(maxSdkVersion = 32) // b/275361339
    @SuppressLint("NewApi")
    @Suppress("Deprecation") // userStyleSettings
    @Test
    public fun commitWithPreviewImage() {
        val scenario =
            createOnWatchFaceEditingTestActivity(
                listOf(colorStyleSetting, watchHandStyleSetting),
                emptyList(),
                headlessDeviceConfig = DeviceConfig(false, false, 0, 0),
                watchComponentName = headlessWatchFaceComponentName,
                previewScreenshotParams =
                    PreviewScreenshotParams(RenderParameters.DEFAULT_INTERACTIVE, Instant.EPOCH)
            )
        val editorObserver = TestEditorObserver()
        val observerId = EditorService.globalEditorService.registerObserver(editorObserver)
        scenario.onActivity { activity ->
            runBlocking { activity.deferredDone.await() }
            // Select [blueStyleOption] and [gothicStyleOption].
            val mutableUserStyle = activity.editorSession.userStyle.value.toMutableUserStyle()
            for (userStyleSetting in activity.editorSession.userStyleSchema.userStyleSettings) {
                mutableUserStyle[userStyleSetting] = userStyleSetting.options.last()
            }
            activity.editorSession.userStyle.value = mutableUserStyle.toUserStyle()
            activity.editorSession.close()
            activity.finish()
        }

        val result =
            editorObserver
                .awaitEditorStateChange(TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)
                .asApiEditorState()

        // previewImage is only supported from API 27 onwards.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            assertNotNull(result.previewImage)
            assertThat(result.previewImage!!.getPixel(0, 0)).isEqualTo(Color.BLUE)
        } else {
            assertNull(result.previewImage)
        }

        EditorService.globalEditorService.unregisterObserver(observerId)
    }

    @Test
    @Suppress("Deprecation") // userStyleSettings
    public fun doNotCommit() {
        val scenario =
            createOnWatchFaceEditingTestActivity(
                listOf(colorStyleSetting, watchHandStyleSetting),
                emptyList(),
                previewScreenshotParams =
                    PreviewScreenshotParams(RenderParameters.DEFAULT_INTERACTIVE, Instant.EPOCH)
            )
        val editorObserver = TestEditorObserver()
        val observerId = EditorService.globalEditorService.registerObserver(editorObserver)
        scenario.onActivity { activity ->
            assertThat(editorDelegate.userStyle[colorStyleSetting]!!.id.value)
                .isEqualTo(redStyleOption.id.value)
            assertThat(editorDelegate.userStyle[watchHandStyleSetting]!!.id.value)
                .isEqualTo(classicStyleOption.id.value)

            // Select [blueStyleOption] and [gothicStyleOption].
            val mutableUserStyle = activity.editorSession.userStyle.value.toMutableUserStyle()
            for (userStyleSetting in activity.editorSession.userStyleSchema.userStyleSettings) {
                mutableUserStyle[userStyleSetting] = userStyleSetting.options.last()
            }
            activity.editorSession.userStyle.value = mutableUserStyle.toUserStyle()

            // This should cause the style on the to be reverted back to the initial style.
            activity.editorSession.commitChangesOnClose = false
            activity.editorSession.close()
            activity.finish()
        }

        val result =
            editorObserver
                .awaitEditorStateChange(TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)
                .asApiEditorState()
        assertThat(result.userStyle.userStyleMap[colorStyleSetting.id.value])
            .isEqualTo(blueStyleOption.id.value)
        assertThat(result.userStyle.userStyleMap[watchHandStyleSetting.id.value])
            .isEqualTo(gothicStyleOption.id.value)
        assertFalse(result.shouldCommitChanges)
        assertNull(result.previewImage)

        // The original style should be applied to the watch face however because
        // commitChangesOnClose is false.
        assertThat(editorDelegate.userStyle[colorStyleSetting]!!.id.value)
            .isEqualTo(redStyleOption.id.value)
        assertThat(editorDelegate.userStyle[watchHandStyleSetting]!!.id.value)
            .isEqualTo(classicStyleOption.id.value)

        EditorService.globalEditorService.unregisterObserver(observerId)
    }

    @Test
    @Suppress("Deprecation") // userStyleSettings
    public fun commitChanges_preRFlow() {
        val scenario =
            createOnWatchFaceEditingTestActivity(
                listOf(colorStyleSetting, watchHandStyleSetting),
                emptyList(),
                preRFlow = true
            )
        val editorObserver = TestEditorObserver()
        val observerId = EditorService.globalEditorService.registerObserver(editorObserver)
        scenario.onActivity { activity ->
            assertThat(editorDelegate.userStyle[colorStyleSetting]!!.id.value)
                .isEqualTo(redStyleOption.id.value)
            assertThat(editorDelegate.userStyle[watchHandStyleSetting]!!.id.value)
                .isEqualTo(classicStyleOption.id.value)

            // Select [blueStyleOption] and [gothicStyleOption].
            val mutableUserStyle = activity.editorSession.userStyle.value.toMutableUserStyle()
            for (userStyleSetting in activity.editorSession.userStyleSchema.userStyleSettings) {
                mutableUserStyle[userStyleSetting] = userStyleSetting.options.last()
            }
            activity.editorSession.userStyle.value = mutableUserStyle.toUserStyle()

            activity.editorSession.close()
            activity.finish()
        }

        val result =
            editorObserver
                .awaitEditorStateChange(TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)
                .asApiEditorState()
        assertThat(result.userStyle.userStyleMap[colorStyleSetting.id.value])
            .isEqualTo(blueStyleOption.id.value)
        assertThat(result.userStyle.userStyleMap[watchHandStyleSetting.id.value])
            .isEqualTo(gothicStyleOption.id.value)
        assertTrue(result.shouldCommitChanges)

        // Changes should be applied to the delegate too.
        assertThat(editorDelegate.userStyle[colorStyleSetting]!!.id.value)
            .isEqualTo(blueStyleOption.id.value)
        assertThat(editorDelegate.userStyle[watchHandStyleSetting]!!.id.value)
            .isEqualTo(gothicStyleOption.id.value)

        EditorService.globalEditorService.unregisterObserver(observerId)
    }

    @Suppress("NewApi") // EditorRequest
    @Test
    public fun watchFaceEditorContract_createIntent() {
        val testComponentName = ComponentName("test.package", "test.class")
        val intent =
            WatchFaceEditorContract()
                .createIntent(
                    ApplicationProvider.getApplicationContext<Context>(),
                    EditorRequest(
                        testComponentName,
                        testEditorPackageName,
                        null,
                        testInstanceId,
                        null,
                        null
                    )
                )
        assertThat(intent.getPackage()).isEqualTo(testEditorPackageName)

        val editorRequest = EditorRequest.createFromIntent(intent)
        assertThat(editorRequest.editorPackageName).isEqualTo(testEditorPackageName)
        assertThat(editorRequest.initialUserStyle).isNull()
        assertThat(editorRequest.watchFaceComponentName).isEqualTo(testComponentName)
        assertThat(editorRequest.watchFaceId.id).isEqualTo(testInstanceId.id)
    }

    @Test
    public fun forceCloseEditorSessionDuring_fetchComplicationsData() {
        val getProviderInfosLatch = CountDownLatch(1)
        val complicationDataSourceInfoRetrieverProvider =
            TestComplicationDataSourceInfoRetrieverProvider(getProviderInfosLatch)

        val scenario =
            createOnWatchFaceEditingTestActivity(
                listOf(colorStyleSetting, watchHandStyleSetting),
                listOf(leftComplication, rightComplication),
                complicationDataSourceInfoRetrieverProvider =
                    complicationDataSourceInfoRetrieverProvider
            )

        scenario.onActivity { activity ->
            activity.immediateCoroutineScope.launch {
                activity.editorSession.complicationsPreviewData.collect {}
                @Suppress("UNREACHABLE_CODE")
                fail("We shouldn't get here due to the editor closing")
            }
        }

        EditorService.globalEditorService.closeEditor()
        getProviderInfosLatch.countDown()
        assertTrue(onDestroyLatch.await(5L, TimeUnit.SECONDS))

        assertTrue(
            complicationDataSourceInfoRetrieverProvider.lastComplicationDataSourceInfoRetriever
                .closed
        )
    }

    @Test
    @Suppress("Deprecation") // userStyleSettings
    public fun forceCloseEditorSession() {
        val scenario =
            createOnWatchFaceEditingTestActivity(
                listOf(colorStyleSetting, watchHandStyleSetting),
                listOf(leftComplication, rightComplication)
            )

        val editorObserver = TestEditorObserver()
        val observerId = EditorService.globalEditorService.registerObserver(editorObserver)

        scenario.onActivity { activity ->
            // Select [blueStyleOption] and [gothicStyleOption].
            val mutableUserStyle = activity.editorSession.userStyle.value.toMutableUserStyle()
            for (userStyleSetting in activity.editorSession.userStyleSchema.userStyleSettings) {
                mutableUserStyle[userStyleSetting] = userStyleSetting.options.last()
            }
            activity.editorSession.userStyle.value = mutableUserStyle.toUserStyle()
        }

        EditorService.globalEditorService.closeEditor()
        assertTrue(onDestroyLatch.await(5L, TimeUnit.SECONDS))

        // We don't expect the observer to have fired.
        assertFalse(editorObserver.stateChangeObserved())

        // The style change should not have been applied to the watchface.
        assertThat(editorDelegate.userStyle[colorStyleSetting]!!.id.value)
            .isEqualTo(redStyleOption.id.value)
        assertThat(editorDelegate.userStyle[watchHandStyleSetting]!!.id.value)
            .isEqualTo(classicStyleOption.id.value)

        EditorService.globalEditorService.unregisterObserver(observerId)
    }

    @Test
    @Suppress("Deprecation") // userStyleSettings
    public fun observedDeathForceClosesEditorSession() {
        val scenario =
            createOnWatchFaceEditingTestActivity(
                listOf(colorStyleSetting, watchHandStyleSetting),
                listOf(leftComplication, rightComplication)
            )

        val editorObserver = Mockito.mock(IEditorObserver::class.java)
        val mockBinder = Mockito.mock(IBinder::class.java)
        `when`(editorObserver.asBinder()).thenReturn(mockBinder)

        val observerId = EditorService.globalEditorService.registerObserver(editorObserver)

        val deathRecipientCaptor = ArgumentCaptor.forClass(IBinder.DeathRecipient::class.java)
        verify(mockBinder).linkToDeath(deathRecipientCaptor.capture(), anyInt())

        scenario.onActivity { activity ->
            // Select [blueStyleOption] and [gothicStyleOption].
            val mutableUserStyle = activity.editorSession.userStyle.value.toMutableUserStyle()
            for (userStyleSetting in activity.editorSession.userStyleSchema.userStyleSettings) {
                mutableUserStyle[userStyleSetting] = userStyleSetting.options.last()
            }
            activity.editorSession.userStyle.value = mutableUserStyle.toUserStyle()
        }

        // Pretend the binder died, this should force close the editor session.
        deathRecipientCaptor.value.binderDied()

        assertTrue(onDestroyLatch.await(5L, TimeUnit.SECONDS))

        // The style change should not have been applied to the watchface.
        assertThat(editorDelegate.userStyle[colorStyleSetting]!!.id.value)
            .isEqualTo(redStyleOption.id.value)
        assertThat(editorDelegate.userStyle[watchHandStyleSetting]!!.id.value)
            .isEqualTo(classicStyleOption.id.value)

        EditorService.globalEditorService.unregisterObserver(observerId)
    }

    @SuppressLint("NewApi") // EditorRequest
    @Test
    public fun closeEditorSessionBeforeInitCompleted() {
        val testComponentName = ComponentName("test.package", "test.class")
        OnWatchFaceEditingTestActivity.complicationDataSourceInfoRetrieverProvider =
            TestComplicationDataSourceInfoRetrieverProvider()
        val session: ActivityScenario<OnWatchFaceEditingTestActivity> =
            ActivityScenario.launch(
                WatchFaceEditorContract()
                    .createIntent(
                        ApplicationProvider.getApplicationContext<Context>(),
                        EditorRequest(
                            testComponentName,
                            testEditorPackageName,
                            null,
                            WatchFaceId("instanceId"),
                            null,
                            null
                        )
                    )
                    .apply {
                        component =
                            ComponentName(
                                ApplicationProvider.getApplicationContext<Context>(),
                                OnWatchFaceEditingTestActivity::class.java
                            )
                    }
            )

        session.onActivity {
            // This shouldn't throw an exception.
            EditorService.globalEditorService.closeEditor()
        }
    }

    @Test
    public fun cancelDuring_updatePreviewData() {
        ComplicationDataSourceChooserContract.useTestComplicationHelperActivity = true
        TestComplicationHelperActivity.resultIntent =
            CompletableDeferred(
                Intent().apply {
                    putExtra(
                        ComplicationDataSourceChooserIntent.EXTRA_PROVIDER_INFO,
                        ComplicationDataSourceInfo(
                                "TestDataSource3App",
                                "TestDataSource3",
                                Icon.createWithBitmap(
                                    Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
                                ),
                                ComplicationType.LONG_TEXT,
                                dataSource3
                            )
                            .toWireComplicationProviderInfo()
                    )
                }
            )

        lateinit var baseEditorSession: BaseEditorSession
        lateinit var complicationDataSourceInfoRetriever: ComplicationDataSourceInfoRetriever
        var requestPreviewComplicationDataCount = 0
        val scenario =
            createOnWatchFaceEditingTestActivity(
                emptyList(),
                listOf(leftComplication, rightComplication),
                complicationDataSourceInfoRetrieverProvider =
                    object : TestComplicationDataSourceInfoRetrieverProvider() {

                        override fun getComplicationDataSourceInfoRetriever():
                            ComplicationDataSourceInfoRetriever {
                            complicationDataSourceInfoRetriever =
                                super.getComplicationDataSourceInfoRetriever()
                            return complicationDataSourceInfoRetriever
                        }

                        override fun requestPreviewComplicationData(
                            dataSourceComponent: ComponentName,
                            complicationType: Int,
                            previewComplicationDataCallback: IPreviewComplicationDataCallback
                        ): Boolean {
                            // Force close the third time this is invoked in response to
                            // openComplicationDataSourceChooser and a result being selected. The
                            // previous two
                            // invocations where done to prime the map for
                            // getComplicationsPreviewData().
                            if (++requestPreviewComplicationDataCount == 3) {
                                baseEditorSession.forceClose()
                            } else {
                                previewComplicationDataCallback.updateComplicationData(
                                    ShortTextComplicationData.Builder(
                                            PlainComplicationText.Builder("TestData").build(),
                                            ComplicationText.EMPTY
                                        )
                                        .build()
                                        .asWireComplicationData()
                                )
                            }
                            return true
                        }
                    }
            )

        scenario.onActivity { activity ->
            baseEditorSession = activity.editorSession as BaseEditorSession
        }

        try {
            runBlocking {
                withContext(baseEditorSession.coroutineScope.coroutineContext) {
                    baseEditorSession.openComplicationDataSourceChooser(RIGHT_COMPLICATION_ID)

                    // Make sure everything that was going to run has run.
                    baseEditorSession.coroutineScope.coroutineContext.job.join()

                    fail("Should have failed with a JobCancellationException")
                }
            }
        } catch (e: Exception) {
            assertThat(e.toString()).contains("kotlinx.coroutines.JobCancellationException")
        }

        // Ensure the complicationDataSourceInfoRetriever was closed despite forceClose() being
        // called.
        assertThat(complicationDataSourceInfoRetriever.closed).isTrue()
    }

    @Test
    public fun getComplicationsPreviewData() {
        val scenario =
            createOnWatchFaceEditingTestActivity(
                listOf(colorStyleSetting, watchHandStyleSetting),
                listOf(leftComplication, rightComplication)
            )

        scenario.onActivity { activity ->
            runBlocking {
                val previewData = activity.editorSession.complicationsPreviewData.value
                assertThat(previewData.size).isEqualTo(2)
                assertThat(previewData[LEFT_COMPLICATION_ID])
                    .isInstanceOf(ShortTextComplicationData::class.java)
                val leftComplicationData =
                    previewData[LEFT_COMPLICATION_ID] as ShortTextComplicationData
                assertThat(
                        leftComplicationData.text.getTextAt(
                            ApplicationProvider.getApplicationContext<Context>().resources,
                            Instant.EPOCH
                        )
                    )
                    .isEqualTo("Left")

                assertThat(previewData[RIGHT_COMPLICATION_ID])
                    .isInstanceOf(LongTextComplicationData::class.java)
                val rightComplicationData =
                    previewData[RIGHT_COMPLICATION_ID] as LongTextComplicationData
                assertThat(
                        rightComplicationData.text.getTextAt(
                            ApplicationProvider.getApplicationContext<Context>().resources,
                            Instant.EPOCH
                        )
                    )
                    .isEqualTo("Right")
            }
        }
    }

    public fun getComplicationsPreviewData_withEmptyBackgroundComplication() {
        val scenario =
            createOnWatchFaceEditingTestActivity(
                listOf(colorStyleSetting, watchHandStyleSetting),
                listOf(leftComplication, backgroundComplication)
            )

        scenario.onActivity { activity ->
            runBlocking {
                val previewData = activity.editorSession.complicationsPreviewData.value
                assertThat(previewData.size).isEqualTo(2)
                assertThat(previewData[LEFT_COMPLICATION_ID])
                    .isInstanceOf(ShortTextComplicationData::class.java)
                val leftComplicationData =
                    previewData[LEFT_COMPLICATION_ID] as ShortTextComplicationData
                assertThat(
                        leftComplicationData.text.getTextAt(
                            ApplicationProvider.getApplicationContext<Context>().resources,
                            Instant.EPOCH
                        )
                    )
                    .isEqualTo("Left")

                // TestComplicationDataSourceInfoRetrieverProvider isn't configured with a data
                // source for the background complication which means it behaves as if it was an
                // empty complication as far as fetching preview data is concerned.
                assertThat(previewData[BACKGROUND_COMPLICATION_ID])
                    .isInstanceOf(EmptyComplicationData::class.java)
            }
        }
    }

    @SuppressLint("NewApi") // EditorRequest
    @Test
    public fun testComponentNameMismatch() {
        val testComponentName = ComponentName("test.package", "test.class")
        val watchFaceId = WatchFaceId("ID-1")

        OnWatchFaceEditingTestActivity.complicationDataSourceInfoRetrieverProvider =
            TestComplicationDataSourceInfoRetrieverProvider()

        val scenario: ActivityScenario<OnWatchFaceEditingTestActivity> =
            ActivityScenario.launch(
                WatchFaceEditorContract()
                    .createIntent(
                        ApplicationProvider.getApplicationContext<Context>(),
                        EditorRequest(
                            testComponentName,
                            testEditorPackageName,
                            null,
                            watchFaceId,
                            null,
                            null
                        )
                    )
                    .apply {
                        component =
                            ComponentName(
                                ApplicationProvider.getApplicationContext<Context>(),
                                OnWatchFaceEditingTestActivity::class.java
                            )
                    }
            )

        scenario.onActivity { activity ->
            val mockWatchFaceHostApi = mock(WatchFaceHostApi::class.java)
            val handler = Handler(Looper.myLooper()!!)
            `when`(mockWatchFaceHostApi.getUiThreadHandler()).thenReturn(handler)
            `when`(mockWatchFaceHostApi.getContext())
                .thenReturn(ApplicationProvider.getApplicationContext<Context>())
            val watchState = MutableWatchState().asWatchState()
            val currentUserStyleRepository =
                CurrentUserStyleRepository(UserStyleSchema(emptyList()))
            val mockSurfaceHolder = mock(SurfaceHolder::class.java)
            val mockSurface = mock(Surface::class.java)
            `when`(mockSurfaceHolder.surface).thenReturn(mockSurface)
            `when`(mockSurfaceHolder.surfaceFrame).thenReturn(Rect())
            `when`(mockSurface.isValid).thenReturn(false)

            // Construct a WatchFaceImpl which creates a delegate whose ComponentName doesn't match
            // testComponentName.
            WatchFaceImpl(
                WatchFace(
                    WatchFaceType.DIGITAL,
                    @Suppress("deprecation")
                    object :
                        Renderer.CanvasRenderer(
                            mockSurfaceHolder,
                            currentUserStyleRepository,
                            watchState,
                            CanvasType.SOFTWARE,
                            16
                        ) {
                        override fun render(
                            canvas: Canvas,
                            bounds: Rect,
                            zonedDateTime: ZonedDateTime
                        ) {}

                        override fun renderHighlightLayer(
                            canvas: Canvas,
                            bounds: Rect,
                            zonedDateTime: ZonedDateTime
                        ) {}
                    }
                ),
                mockWatchFaceHostApi,
                watchState,
                currentUserStyleRepository,
                ComplicationSlotsManager(emptyList(), currentUserStyleRepository),
                BroadcastsObserver(
                    watchState,
                    mockWatchFaceHostApi,
                    CompletableDeferred(),
                    CoroutineScope(handler.asCoroutineDispatcher())
                ),
                null
            )

            assertThat(activity.onCreateException).isInstanceOf(IllegalStateException::class.java)
            assertThat(activity.onCreateException.message)
                .isEqualTo(
                    "Expected ComponentInfo{test.package/test.class} to be created but " +
                        "got ComponentInfo{androidx.wear.watchface.editor.test/" +
                        "android.app.Application}"
                )
        }
    }

    @Test
    @Suppress("DEPRECATION")
    public fun watchfaceSupportsHeadlessEditing() {
        val mockPackageManager = Mockito.mock(PackageManager::class.java)

        `when`(
                mockPackageManager.getServiceInfo(
                    ComponentName("test.package", EditorRequest.WATCHFACE_CONTROL_SERVICE),
                    PackageManager.GET_META_DATA
                )
            )
            .thenReturn(
                ServiceInfo().apply {
                    metaData =
                        Bundle().apply { putInt(EditorRequest.ANDROIDX_WATCHFACE_API_VERSION, 4) }
                }
            )

        assertThat(
                EditorRequest.supportsWatchFaceHeadlessEditing(mockPackageManager, "test.package")
            )
            .isTrue()
    }

    @Test
    @Suppress("DEPRECATION")
    public fun watchfaceSupportsHeadlessEditing_oldApi() {
        val mockPackageManager = Mockito.mock(PackageManager::class.java)

        `when`(
                mockPackageManager.getServiceInfo(
                    ComponentName("test.package", EditorRequest.WATCHFACE_CONTROL_SERVICE),
                    PackageManager.GET_META_DATA
                )
            )
            .thenReturn(
                ServiceInfo().apply {
                    metaData =
                        Bundle().apply { putInt(EditorRequest.ANDROIDX_WATCHFACE_API_VERSION, 3) }
                }
            )

        assertThat(
                EditorRequest.supportsWatchFaceHeadlessEditing(mockPackageManager, "test.package")
            )
            .isFalse()
    }

    @Test
    public fun cantAssignUnrelatedUserStyle() {
        val redOption = ListOption(Option.Id("red"), "Red", "Red", icon = null)
        val greenOption = ListOption(Option.Id("green"), "Green", "Green", icon = null)
        val colorStyleList = listOf(redOption, greenOption)
        val watchColorSetting =
            UserStyleSetting.ListUserStyleSetting(
                UserStyleSetting.Id("color_id"),
                "Color",
                "Watch face color",
                /* icon = */ null,
                colorStyleList,
                listOf(WatchFaceLayer.COMPLICATIONS_OVERLAY)
            )

        val scenario =
            createOnWatchFaceEditingTestActivity(
                listOf(colorStyleSetting, watchHandStyleSetting),
                listOf(leftComplication, rightComplication)
            )

        scenario.onActivity { activity ->
            try {
                // Trying to set an unrelated UserStyle should fail.
                assertFailsWith<IllegalArgumentException> {
                    activity.editorSession.userStyle.value =
                        UserStyle(mapOf(watchColorSetting to greenOption))
                }
            } finally {
                activity.editorSession.close()
                activity.finish()
            }
        }
    }

    @Test
    public fun cantAssignUnrelatedUserStyle_compareAndSet() {
        val redOption = ListOption(Option.Id("red"), "Red", "Red", icon = null)
        val greenOption = ListOption(Option.Id("green"), "Green", "Green", icon = null)
        val colorStyleList = listOf(redOption, greenOption)
        val watchColorSetting =
            UserStyleSetting.ListUserStyleSetting(
                UserStyleSetting.Id("color_id"),
                "Color",
                "Watch face color",
                /* icon = */ null,
                colorStyleList,
                listOf(WatchFaceLayer.COMPLICATIONS_OVERLAY)
            )

        val scenario =
            createOnWatchFaceEditingTestActivity(
                listOf(colorStyleSetting, watchHandStyleSetting),
                listOf(leftComplication, rightComplication)
            )

        scenario.onActivity { activity ->
            try {
                // Trying to set an unrelated UserStyle should fail.
                assertFailsWith<IllegalArgumentException> {
                    // NB update uses compareAndSet under the hood.
                    activity.editorSession.userStyle.update {
                        UserStyle(mapOf(watchColorSetting to greenOption))
                    }
                }
            } finally {
                activity.editorSession.close()
                activity.finish()
            }
        }
    }
}

@SuppressLint("NewApi") // icon.type
internal fun assertEquals(
    expected: ComplicationDataSourceInfo?,
    actual: ComplicationDataSourceInfo?
) =
    when (expected) {
        null -> assertThat(actual).isNull()
        else -> {
            assertThat(actual).isNotNull()
            checkNotNull(actual)
            assertThat(actual.appName).isEqualTo(expected.appName)
            assertThat(actual.name).isEqualTo(expected.name)
            // Check the type as a proxy for it being the same icon.
            assertThat(actual.icon.type).isEqualTo(expected.icon.type)
            assertThat(actual.componentName).isEqualTo(expected.componentName)
        }
    }
