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

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.drawable.Icon
import android.icu.util.Calendar
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.os.Looper
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
import androidx.wear.complications.ComplicationDataSourceInfo
import androidx.wear.complications.ComplicationDataSourceInfoRetriever
import androidx.wear.complications.ComplicationSlotBounds
import androidx.wear.complications.DefaultComplicationDataSourcePolicy
import androidx.wear.complications.SystemDataSources
import androidx.wear.complications.data.ComplicationText
import androidx.wear.complications.data.ComplicationType
import androidx.wear.complications.data.EmptyComplicationData
import androidx.wear.complications.data.LongTextComplicationData
import androidx.wear.complications.data.PlainComplicationText
import androidx.wear.complications.data.ShortTextComplicationData
import androidx.wear.watchface.BroadcastsObserver
import androidx.wear.watchface.CanvasComplication
import androidx.wear.watchface.CanvasType
import androidx.wear.watchface.ComplicationDataSourceChooserIntent
import androidx.wear.watchface.ComplicationHelperActivity
import androidx.wear.watchface.ComplicationSlot
import androidx.wear.watchface.ComplicationSlotsManager
import androidx.wear.watchface.MutableWatchState
import androidx.wear.watchface.RenderParameters
import androidx.wear.watchface.Renderer
import androidx.wear.watchface.WatchFace
import androidx.wear.watchface.WatchFaceHostApi
import androidx.wear.watchface.WatchFaceImpl
import androidx.wear.watchface.WatchFaceType
import androidx.wear.watchface.client.WatchFaceId
import androidx.wear.watchface.client.asApiEditorState
import androidx.wear.watchface.complications.rendering.CanvasComplicationDrawable
import androidx.wear.watchface.complications.rendering.ComplicationDrawable
import androidx.wear.watchface.data.ComplicationSlotBoundsType
import androidx.wear.watchface.editor.EditorSession.Companion.EDITING_SESSION_TIMEOUT_MILLIS
import androidx.wear.watchface.editor.data.EditorStateWireFormat
import androidx.wear.watchface.style.CurrentUserStyleRepository
import androidx.wear.watchface.style.UserStyle
import androidx.wear.watchface.style.UserStyleSchema
import androidx.wear.watchface.style.UserStyleSetting
import androidx.wear.watchface.style.UserStyleSetting.ListUserStyleSetting.ListOption
import androidx.wear.watchface.style.UserStyleSetting.Option
import androidx.wear.watchface.style.WatchFaceLayer
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.android.asCoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mockito
import org.mockito.Mockito.`when`
import org.mockito.Mockito.doAnswer
import org.mockito.Mockito.mock
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeUnit.MILLISECONDS

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

private typealias WireComplicationProviderInfo =
    android.support.wearable.complications.ComplicationProviderInfo

/** Trivial "editor" which exposes the EditorSession for testing. */
public open class OnWatchFaceEditingTestActivity : ComponentActivity() {
    public lateinit var editorSession: EditorSession
    public lateinit var onCreateException: Exception
    public val creationLatch: CountDownLatch = CountDownLatch(1)

    public val listenableEditorSession: ListenableEditorSession by lazy {
        ListenableEditorSession(editorSession)
    }

    internal companion object {
        internal var complicationDataSourceInfoRetrieverProvider:
            ComplicationDataSourceInfoRetrieverProvider? = null
    }

    public val immediateCoroutineScope: CoroutineScope =
        CoroutineScope(Handler(Looper.getMainLooper()).asCoroutineDispatcher().immediate)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        immediateCoroutineScope.launch {
            try {
                editorSession = EditorSession.createOnWatchEditingSessionImpl(
                    this@OnWatchFaceEditingTestActivity,
                    intent!!,
                    complicationDataSourceInfoRetrieverProvider!!
                )
            } catch (e: Exception) {
                onCreateException = e
            } finally {
                creationLatch.countDown()
            }
        }
    }
}

public open class TestComplicationDataSourceInfoRetrieverProvider :
    ComplicationDataSourceInfoRetrieverProvider, IProviderInfoService.Stub() {

    private val dataSourceIcon1: Icon =
        Icon.createWithBitmap(Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888))
    private val dataSourceIcon2: Icon =
        Icon.createWithBitmap(Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888))

    private val watchFaceComponent = ComponentName("test.package", "test.class")
    private val dataSourceInfos = mapOf(
        LEFT_COMPLICATION_ID to ComplicationDataSourceInfo(
            "DataSourceApp1",
            "DataSource1",
            dataSourceIcon1,
            ComplicationType.SHORT_TEXT,
            dataSource1
        ),
        RIGHT_COMPLICATION_ID to ComplicationDataSourceInfo(
            "DataSourceApp2",
            "DataSource2",
            dataSourceIcon2,
            ComplicationType.LONG_TEXT,
            dataSource2
        )
    )
    private val previewData = mapOf(
        dataSource1 to
            ShortTextComplicationData.Builder(
                PlainComplicationText.Builder("Left").build(),
                ComplicationText.EMPTY
            ).build().asWireComplicationData(),
        dataSource2 to
            LongTextComplicationData.Builder(
                PlainComplicationText.Builder("Right").build(),
                ComplicationText.EMPTY
            ).build().asWireComplicationData(),
        dataSource3 to
            LongTextComplicationData.Builder(
                PlainComplicationText.Builder("DataSource3").build(),
                ComplicationText.EMPTY
            ).build().asWireComplicationData(),
    )

    override fun getComplicationDataSourceInfoRetriever(): ComplicationDataSourceInfoRetriever =
        ComplicationDataSourceInfoRetriever(this)

    override fun getProviderInfos(
        watchFaceComponent: ComponentName,
        ids: IntArray
    ): Array<WireComplicationProviderInfo?>? {
        if (watchFaceComponent != this.watchFaceComponent) {
            return null
        }
        return ArrayList<WireComplicationProviderInfo?>().apply {
            for (id in ids) {
                add(dataSourceInfos[id]?.toWireComplicationProviderInfo())
            }
        }.toTypedArray()
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

        CoroutineScope(Handler(Looper.getMainLooper()).asCoroutineDispatcher()).launch {
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

@RunWith(AndroidJUnit4::class)
@MediumTest
public class EditorSessionTest {
    private val testComponentName = ComponentName("test.package", "test.class")
    private val testEditorPackageName = "test.package"
    private val testInstanceId = WatchFaceId("TEST_INSTANCE_ID")
    private lateinit var editorDelegate: WatchFace.EditorDelegate
    private val screenBounds = Rect(0, 0, 400, 400)

    private val redStyleOption = ListOption(Option.Id("red_style"), "Red", icon = null)

    private val greenStyleOption = ListOption(Option.Id("green_style"), "Green", icon = null)

    private val blueStyleOption = ListOption(Option.Id("bluestyle"), "Blue", icon = null)

    private val colorStyleList = listOf(redStyleOption, greenStyleOption, blueStyleOption)

    private val colorStyleSetting = UserStyleSetting.ListUserStyleSetting(
        UserStyleSetting.Id("color_style_setting"),
        "Colors",
        "Watchface colorization", /* icon = */
        null,
        colorStyleList,
        listOf(WatchFaceLayer.BASE)
    )

    private val classicStyleOption = ListOption(Option.Id("classic_style"), "Classic", icon = null)

    private val modernStyleOption = ListOption(Option.Id("modern_style"), "Modern", icon = null)

    private val gothicStyleOption = ListOption(Option.Id("gothic_style"), "Gothic", icon = null)

    private val watchHandStyleList =
        listOf(classicStyleOption, modernStyleOption, gothicStyleOption)

    private val watchHandStyleSetting = UserStyleSetting.ListUserStyleSetting(
        UserStyleSetting.Id("hand_style_setting"),
        "Hand Style",
        "Hand visual look", /* icon = */
        null,
        watchHandStyleList,
        listOf(WatchFaceLayer.COMPLICATIONS_OVERLAY)
    )

    private val mockInvalidateCallback =
        Mockito.mock(CanvasComplication.InvalidateCallback::class.java)
    private val placeholderWatchState = MutableWatchState().asWatchState()
    private val mockLeftCanvasComplication = CanvasComplicationDrawable(
        ComplicationDrawable(),
        placeholderWatchState,
        mockInvalidateCallback
    )
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
            DefaultComplicationDataSourcePolicy(SystemDataSources.DATA_SOURCE_SUNRISE_SUNSET),
            ComplicationSlotBounds(RectF(0.2f, 0.4f, 0.4f, 0.6f))
        ).setDefaultDataSourceType(ComplicationType.SHORT_TEXT)
            .build()

    private val mockRightCanvasComplication = CanvasComplicationDrawable(
        ComplicationDrawable(),
        placeholderWatchState,
        mockInvalidateCallback
    )
    private val rightComplication =
        ComplicationSlot.createRoundRectComplicationSlotBuilder(
            RIGHT_COMPLICATION_ID,
            { _, _ -> mockRightCanvasComplication },
            listOf(
                ComplicationType.RANGED_VALUE,
                ComplicationType.LONG_TEXT,
                ComplicationType.SHORT_TEXT,
                ComplicationType.MONOCHROMATIC_IMAGE,
                ComplicationType.SMALL_IMAGE
            ),
            DefaultComplicationDataSourcePolicy(SystemDataSources.DATA_SOURCE_DAY_OF_WEEK),
            ComplicationSlotBounds(RectF(0.6f, 0.4f, 0.8f, 0.6f))
        ).setDefaultDataSourceType(ComplicationType.SHORT_TEXT)
            .setConfigExtras(
                Bundle().apply {
                    putString(PROVIDER_CHOOSER_EXTRA_KEY, PROVIDER_CHOOSER_EXTRA_VALUE)
                }
            )
            .build()

    private val mockBackgroundCanvasComplication =
        CanvasComplicationDrawable(
            ComplicationDrawable(),
            placeholderWatchState,
            mockInvalidateCallback
        )
    private val backgroundComplication =
        ComplicationSlot.createBackgroundComplicationSlotBuilder(
            BACKGROUND_COMPLICATION_ID,
            { _, _ -> mockBackgroundCanvasComplication },
            emptyList(),
            DefaultComplicationDataSourcePolicy()
        ).setEnabled(false).build()

    private val fakeBitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
    private val onDestroyLatch = CountDownLatch(1)
    private val dataSourceIcon =
        Icon.createWithBitmap(Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888))
    private val dataSourceComponentName = ComponentName("test.package", "test.class")

    private class TestEditorObserver : IEditorObserver.Stub() {
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

    private fun createOnWatchFaceEditingTestActivity(
        userStyleSettings: List<UserStyleSetting>,
        complicationSlots: List<ComplicationSlot>,
        watchFaceId: WatchFaceId = testInstanceId,
        previewReferenceTimeMillis: Long = 12345,
        complicationDataSourceInfoRetrieverProvider: ComplicationDataSourceInfoRetrieverProvider =
            TestComplicationDataSourceInfoRetrieverProvider(),
        shouldTimeout: Boolean = false,
        preRFlow: Boolean = false
    ): ActivityScenario<OnWatchFaceEditingTestActivity> {
        val userStyleRepository = CurrentUserStyleRepository(UserStyleSchema(userStyleSettings))
        val complicationSlotsManager =
            ComplicationSlotsManager(complicationSlots, userStyleRepository)
        complicationSlotsManager.watchState = placeholderWatchState

        // Mocking getters and setters with mockito at the same time is hard so we do this instead.
        editorDelegate = object : WatchFace.EditorDelegate {
            private val backgroundHandlerThread = HandlerThread("TestBackgroundThread").apply {
                start()
            }

            private val backgroundHandler = Handler(backgroundHandlerThread.looper)

            override val userStyleSchema = userStyleRepository.schema
            override var userStyle: UserStyle
                get() = userStyleRepository.userStyle
                set(value) {
                    userStyleRepository.userStyle = value
                }

            override val complicationSlotsManager = complicationSlotsManager
            override val screenBounds = this@EditorSessionTest.screenBounds
            override val previewReferenceTimeMillis = previewReferenceTimeMillis
            override val backgroundThreadHandler = backgroundHandler

            override fun renderWatchFaceToBitmap(
                renderParameters: RenderParameters,
                calendarTimeMillis: Long,
                slotIdToComplicationData:
                    Map<Int, androidx.wear.complications.data.ComplicationData>?
            ) = fakeBitmap

            override fun onDestroy() {
                onDestroyLatch.countDown()
                backgroundHandlerThread.quitSafely()
            }
        }
        if (!shouldTimeout) {
            WatchFace.registerEditorDelegate(testComponentName, editorDelegate)
        }

        OnWatchFaceEditingTestActivity.complicationDataSourceInfoRetrieverProvider =
            complicationDataSourceInfoRetrieverProvider

        if (preRFlow) {
            return ActivityScenario.launch(
                Intent().apply {
                    putExtra(Constants.EXTRA_WATCH_FACE_COMPONENT, testComponentName)
                    component = ComponentName(
                        ApplicationProvider.getApplicationContext<Context>(),
                        OnWatchFaceEditingTestActivity::class.java
                    )
                }
            )
        }

        return ActivityScenario.launch(
            WatchFaceEditorContract().createIntent(
                ApplicationProvider.getApplicationContext<Context>(),
                EditorRequest(testComponentName, testEditorPackageName, null, watchFaceId)
            ).apply {
                component = ComponentName(
                    ApplicationProvider.getApplicationContext<Context>(),
                    OnWatchFaceEditingTestActivity::class.java
                )
            }
        )
    }

    private fun createOnWatchFaceEditingTestActivityThatThrowsTimeoutException():
        ActivityScenario<OnWatchFaceEditingTestActivity> =
            createOnWatchFaceEditingTestActivity(
                emptyList(), emptyList(), /* other params are default */ shouldTimeout = true
            )

    @After
    public fun tearDown() {
        ComplicationDataSourceChooserContract.useTestComplicationHelperActivity = false
        ComplicationHelperActivity.useTestComplicationDataSourceChooserActivity = false
        ComplicationHelperActivity.skipPermissionCheck = false
        WatchFace.clearAllEditorDelegates()
    }

    @Test
    public fun createOnWatchEditingSessionThrowsTimeoutException() {
        val scenario = createOnWatchFaceEditingTestActivityThatThrowsTimeoutException()
        lateinit var activity: OnWatchFaceEditingTestActivity
        scenario.onActivity { activity = it }
        activity.creationLatch.await(EDITING_SESSION_TIMEOUT_MILLIS + 500, MILLISECONDS)
        assert(activity.onCreateException is TimeoutCancellationException)
    }

    @Test
    public fun watchFaceComponentName() {
        val scenario = createOnWatchFaceEditingTestActivity(emptyList(), emptyList())
        scenario.onActivity {
            assertThat(it.editorSession.watchFaceComponentName).isEqualTo(testComponentName)
        }
    }

    @Test
    public fun instanceId() {
        val scenario = createOnWatchFaceEditingTestActivity(emptyList(), emptyList())
        scenario.onActivity {
            assertThat(it.editorSession.watchFaceId.id).isEqualTo(testInstanceId.id)
        }
    }

    @Test
    public fun backgroundComplicationId_noBackgroundComplication() {
        val scenario = createOnWatchFaceEditingTestActivity(emptyList(), emptyList())
        scenario.onActivity {
            assertThat(it.editorSession.backgroundComplicationSlotId).isEqualTo(null)
        }
    }

    @Test
    public fun previewReferenceTimeMillis() {
        val scenario = createOnWatchFaceEditingTestActivity(
            emptyList(),
            emptyList(),
            previewReferenceTimeMillis = 54321L
        )
        scenario.onActivity {
            assertThat(it.editorSession.previewReferenceTimeMillis).isEqualTo(54321L)
        }
    }

    @Test
    public fun userStyleSchema() {
        val scenario = createOnWatchFaceEditingTestActivity(
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
        val scenario = createOnWatchFaceEditingTestActivity(
            emptyList(),
            listOf(backgroundComplication)
        )
        scenario.onActivity {
            assertThat(it.editorSession.backgroundComplicationSlotId).isEqualTo(
                BACKGROUND_COMPLICATION_ID
            )
        }
    }

    @Test
    public fun complicationState() {
        val scenario = createOnWatchFaceEditingTestActivity(
            emptyList(),
            listOf(leftComplication, rightComplication, backgroundComplication)
        )
        scenario.onActivity {
            assertThat(it.editorSession.complicationSlotsState.size).isEqualTo(3)
            assertThat(it.editorSession.complicationSlotsState[LEFT_COMPLICATION_ID]!!.bounds)
                .isEqualTo(Rect(80, 160, 160, 240))
            assertThat(it.editorSession.complicationSlotsState[LEFT_COMPLICATION_ID]!!.boundsType)
                .isEqualTo(ComplicationSlotBoundsType.ROUND_RECT)
            assertFalse(
                it.editorSession.complicationSlotsState[
                    LEFT_COMPLICATION_ID
                ]!!.fixedComplicationDataSource
            )
            assertTrue(
                it.editorSession.complicationSlotsState[LEFT_COMPLICATION_ID]!!.isInitiallyEnabled
            )

            assertThat(it.editorSession.complicationSlotsState[RIGHT_COMPLICATION_ID]!!.bounds)
                .isEqualTo(Rect(240, 160, 320, 240))
            assertThat(it.editorSession.complicationSlotsState[RIGHT_COMPLICATION_ID]!!.boundsType)
                .isEqualTo(ComplicationSlotBoundsType.ROUND_RECT)
            assertFalse(
                it.editorSession.complicationSlotsState[RIGHT_COMPLICATION_ID]!!
                    .fixedComplicationDataSource
            )
            assertTrue(
                it.editorSession.complicationSlotsState[RIGHT_COMPLICATION_ID]!!.isInitiallyEnabled
            )

            assertThat(it.editorSession.complicationSlotsState[BACKGROUND_COMPLICATION_ID]!!.bounds)
                .isEqualTo(screenBounds)
            assertThat(
                it.editorSession.complicationSlotsState[BACKGROUND_COMPLICATION_ID]!!.boundsType
            ).isEqualTo(ComplicationSlotBoundsType.BACKGROUND)
            assertFalse(
                it.editorSession.complicationSlotsState[BACKGROUND_COMPLICATION_ID]!!
                    .fixedComplicationDataSource
            )
            assertFalse(
                it.editorSession.complicationSlotsState[
                    BACKGROUND_COMPLICATION_ID
                ]!!.isInitiallyEnabled
            )
            // We could test more state but this should be enough.
        }
    }

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
                DefaultComplicationDataSourcePolicy(SystemDataSources.DATA_SOURCE_SUNRISE_SUNSET),
                ComplicationSlotBounds(RectF(0.2f, 0.4f, 0.4f, 0.6f))
            ).setDefaultDataSourceType(ComplicationType.SHORT_TEXT)
                .setFixedComplicationDataSource(true)
                .build()

        val scenario = createOnWatchFaceEditingTestActivity(
            emptyList(),
            listOf(fixedLeftComplication)
        )
        scenario.onActivity {
            assertTrue(
                it.editorSession.complicationSlotsState[
                    LEFT_COMPLICATION_ID
                ]!!.fixedComplicationDataSource
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
        val scenario = createOnWatchFaceEditingTestActivity(
            emptyList(),
            listOf(leftComplication, rightComplication, backgroundComplication)
        )
        scenario.onActivity {
            runBlocking {
                val editorSession = it.editorSession as OnWatchFaceEditorSessionImpl
                val mockProviderInfoService = Mockito.mock(IProviderInfoService::class.java)

                val dataSourceInfoRetriever =
                    ComplicationDataSourceInfoRetriever(mockProviderInfoService)
                assertThat(
                    editorSession.getPreviewData(dataSourceInfoRetriever, null)
                ).isNull()
                dataSourceInfoRetriever.close()
            }
        }
    }

    @Test
    public fun getPreviewData() {
        val scenario = createOnWatchFaceEditingTestActivity(
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
                        ).build().asWireComplicationData()
                    )
                    true
                }.`when`(mockProviderInfoService).requestPreviewComplicationData(
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
                    (result as ShortTextComplicationData).text.getTextAt(
                        ApplicationProvider.getApplicationContext<Context>().resources,
                        0
                    )
                ).isEqualTo(complicationText)

                complicationDataSourceInfoRetriever.close()
            }
        }
    }

    @Test
    @SdkSuppress(maxSdkVersion = 28)
    public fun getPreviewData_preRFallback() {
        val scenario = createOnWatchFaceEditingTestActivity(
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
                val previewComplication = editorSession.getPreviewData(
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
                        0
                    )
                ).isEqualTo("dataSource")

                complicationDataSourceInfoRetriever.close()
            }
        }
    }

    @Test
    @SdkSuppress(minSdkVersion = 30)
    public fun getPreviewData_postRFallback() {
        val scenario = createOnWatchFaceEditingTestActivity(
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
                ).thenReturn(false) // Triggers the ExecutionException.

                val previewComplication = editorSession.getPreviewData(
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
                        0
                    )
                ).isEqualTo("dataSource")
            }
        }
    }

    @Test
    public fun launchComplicationDataSourceChooser() {
        ComplicationDataSourceChooserContract.useTestComplicationHelperActivity = true
        val chosenComplicationDataSourceInfo = ComplicationDataSourceInfo(
            "TestDataSource3App",
            "TestDataSource3",
            Icon.createWithBitmap(
                Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
            ),
            ComplicationType.LONG_TEXT,
            dataSource3
        )
        TestComplicationHelperActivity.resultIntent = CompletableDeferred(
            Intent().apply {
                putExtra(
                    ComplicationDataSourceChooserIntent.EXTRA_PROVIDER_INFO,
                    chosenComplicationDataSourceInfo.toWireComplicationProviderInfo()
                )
            }
        )

        val scenario = createOnWatchFaceEditingTestActivity(
            emptyList(),
            listOf(leftComplication, rightComplication)
        )

        lateinit var editorSession: EditorSession
        scenario.onActivity { activity ->
            editorSession = activity.editorSession
        }

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
            assertEquals(
                chosenComplicationDataSourceInfo,
                chosenComplicationDataSource.complicationDataSourceInfo
            )

            // This should update the preview data to point to the updated DataSource3 data.
            val previewComplication =
                editorSession.getComplicationsPreviewData()[LEFT_COMPLICATION_ID]
                    as LongTextComplicationData

            assertThat(
                previewComplication.text.getTextAt(
                    ApplicationProvider.getApplicationContext<Context>().resources,
                    0
                )
            ).isEqualTo("DataSource3")

            assertThat(
                TestComplicationHelperActivity.lastIntent?.extras?.getString(
                    ComplicationDataSourceChooserIntent.EXTRA_WATCHFACE_INSTANCE_ID
                )
            ).isEqualTo(testInstanceId.id)
        }
    }

    @Test
    public fun launchComplicationDataSourceChooserTwiceBackToBack() {
        ComplicationDataSourceChooserContract.useTestComplicationHelperActivity = true
        TestComplicationHelperActivity.resultIntent = CompletableDeferred(
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
                    ).toWireComplicationProviderInfo()
                )
            }
        )

        val scenario = createOnWatchFaceEditingTestActivity(
            emptyList(),
            listOf(leftComplication, rightComplication)
        )

        lateinit var editorSession: EditorSession
        scenario.onActivity { activity ->
            editorSession = activity.editorSession
        }

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

        val scenario = createOnWatchFaceEditingTestActivity(
            emptyList(),
            listOf(leftComplication, rightComplication)
        )

        lateinit var editorSession: EditorSession
        scenario.onActivity { activity ->
            editorSession = activity.editorSession
        }

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
                            ).toWireComplicationProviderInfo()
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

        val scenario = createOnWatchFaceEditingTestActivity(
            emptyList(),
            listOf(leftComplication, rightComplication)
        )

        lateinit var editorSession: EditorSession
        scenario.onActivity { activity ->
            editorSession = activity.editorSession
        }

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
            assertThat(editorSession.getComplicationsPreviewData()[LEFT_COMPLICATION_ID])
                .isInstanceOf(EmptyComplicationData::class.java)
        }
    }

    @Test
    public fun launchComplicationDataSourceChooser_cancel() {
        ComplicationDataSourceChooserContract.useTestComplicationHelperActivity = true
        // NB CompletableDeferred(null) doesn't do what we expect...
        TestComplicationHelperActivity.resultIntent = CompletableDeferred<Intent?>().apply {
            complete(null)
        }

        val scenario = createOnWatchFaceEditingTestActivity(
            emptyList(),
            listOf(leftComplication, rightComplication)
        )

        lateinit var editorSession: EditorSession
        scenario.onActivity { activity ->
            editorSession = activity.editorSession
        }

        runBlocking {
            /**
             * Invoke [TestComplicationHelperActivity] which will simulate the user canceling.
             */
            assertThat(editorSession.openComplicationDataSourceChooser(LEFT_COMPLICATION_ID))
                .isNull()
        }
    }

    @Test
    public fun launchComplicationDataSourceChooser_ComplicationConfigExtrasToHelper() {
        ComplicationDataSourceChooserContract.useTestComplicationHelperActivity = true
        val chosenComplicationDataSourceInfo = ComplicationDataSourceInfo(
            "TestDataSource3App",
            "TestDataSource3",
            Icon.createWithBitmap(
                Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
            ),
            ComplicationType.LONG_TEXT,
            dataSource3
        )
        TestComplicationHelperActivity.resultIntent = CompletableDeferred(
            Intent().apply {
                putExtra(
                    ComplicationDataSourceChooserIntent.EXTRA_PROVIDER_INFO,
                    chosenComplicationDataSourceInfo.toWireComplicationProviderInfo()
                )
                putExtra(PROVIDER_CHOOSER_RESULT_EXTRA_KEY, PROVIDER_CHOOSER_RESULT_EXTRA_VALUE)
            }
        )

        val scenario = createOnWatchFaceEditingTestActivity(
            emptyList(),
            listOf(leftComplication, rightComplication)
        )

        lateinit var editorSession: EditorSession
        scenario.onActivity { activity ->
            editorSession = activity.editorSession
        }

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
            assertThat(
                chosenComplicationDataSource.extras[PROVIDER_CHOOSER_RESULT_EXTRA_KEY]
            ).isEqualTo(PROVIDER_CHOOSER_RESULT_EXTRA_VALUE)

            assertThat(
                TestComplicationHelperActivity.lastIntent?.extras?.getString(
                    PROVIDER_CHOOSER_EXTRA_KEY
                )
            ).isEqualTo(PROVIDER_CHOOSER_EXTRA_VALUE)
        }
    }

    @Test
    public fun launchComplicationDataSourceChooser_ComplicationConfigExtrasToChooser() {
        // Invoke the test data source chooser to record the result.
        ComplicationHelperActivity.useTestComplicationDataSourceChooserActivity = true
        // Invoke the data source chooser without checking for permissions first.
        ComplicationHelperActivity.skipPermissionCheck = true

        val chosenComplicationDataSourceInfo = ComplicationDataSourceInfo(
            "TestDataSource3App",
            "TestDataSource3",
            Icon.createWithBitmap(
                Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
            ),
            ComplicationType.LONG_TEXT,
            dataSource3
        )
        TestComplicationDataSourceChooserActivity.resultIntent = Intent().apply {
            putExtra(
                ComplicationDataSourceChooserIntent.EXTRA_PROVIDER_INFO,
                chosenComplicationDataSourceInfo.toWireComplicationProviderInfo()
            )
            putExtra(PROVIDER_CHOOSER_RESULT_EXTRA_KEY, PROVIDER_CHOOSER_RESULT_EXTRA_VALUE)
        }

        val scenario = createOnWatchFaceEditingTestActivity(
            emptyList(),
            listOf(leftComplication, rightComplication)
        )

        lateinit var editorSession: EditorSession
        scenario.onActivity { activity ->
            editorSession = activity.editorSession
        }

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
            assertThat(
                chosenComplicationDataSource.extras[PROVIDER_CHOOSER_RESULT_EXTRA_KEY]
            ).isEqualTo(PROVIDER_CHOOSER_RESULT_EXTRA_VALUE)

            assertThat(
                TestComplicationDataSourceChooserActivity.lastIntent?.extras?.getString(
                    PROVIDER_CHOOSER_EXTRA_KEY
                )
            ).isEqualTo(PROVIDER_CHOOSER_EXTRA_VALUE)
        }
    }

    @Test
    public fun getComplicationIdAt() {
        val scenario = createOnWatchFaceEditingTestActivity(
            emptyList(),
            listOf(leftComplication, rightComplication, backgroundComplication)
        )
        scenario.onActivity {
            assertThat(it.editorSession.getComplicationSlotIdAt(0, 0)).isEqualTo(null)
            assertThat(it.editorSession.getComplicationSlotIdAt(85, 165))
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
                    1234L,
                    null
                )
            ).isEqualTo(fakeBitmap)
        }
    }

    @Test
    public fun userStyleAndComplicationPreviewDataInEditorObserver() {
        val scenario = createOnWatchFaceEditingTestActivity(
            listOf(colorStyleSetting, watchHandStyleSetting),
            listOf(leftComplication, rightComplication)
        )

        val editorObserver = TestEditorObserver()
        val observerId = EditorService.globalEditorService.registerObserver(editorObserver)

        val oldWFColorStyleSetting = editorDelegate.userStyle[colorStyleSetting]!!.id.value
        val oldWFWatchHandStyleSetting = editorDelegate.userStyle[watchHandStyleSetting]!!.id.value

        scenario.onActivity { activity ->
            runBlocking {
                // Select [blueStyleOption] and [gothicStyleOption].
                val styleMap = activity.editorSession.userStyle.selectedOptions.toMutableMap()
                for (userStyleSetting in activity.editorSession.userStyleSchema.userStyleSettings) {
                    styleMap[userStyleSetting] = userStyleSetting.options.last()
                }
                activity.editorSession.userStyle = UserStyle(styleMap)
                activity.editorSession.close()
                activity.finish()
            }
        }

        val result = editorObserver.awaitEditorStateChange(
            TIMEOUT_MILLIS,
            TimeUnit.MILLISECONDS
        ).asApiEditorState()

        assertThat(result.userStyle.userStyleMap[colorStyleSetting.id.value])
            .isEqualTo(blueStyleOption.id.value)
        assertThat(result.userStyle.userStyleMap[watchHandStyleSetting.id.value])
            .isEqualTo(gothicStyleOption.id.value)
        assertThat(result.watchFaceId.id).isEqualTo(testInstanceId.id)
        assertTrue(result.shouldCommitChanges)

        // The style change shouldn't be applied to the watchface as it gets reverted to the old
        // one when editor closes.
        assertThat(editorDelegate.userStyle[colorStyleSetting]!!.id.value)
            .isEqualTo(oldWFColorStyleSetting)
        assertThat(editorDelegate.userStyle[watchHandStyleSetting]!!.id.value)
            .isEqualTo(oldWFWatchHandStyleSetting)

        assertThat(result.previewComplicationsData.size).isEqualTo(2)
        val leftComplicationData = result.previewComplicationsData[LEFT_COMPLICATION_ID] as
            ShortTextComplicationData
        assertThat(
            leftComplicationData.text.getTextAt(
                ApplicationProvider.getApplicationContext<Context>().resources,
                0
            )
        ).isEqualTo("Left")

        val rightComplicationData = result.previewComplicationsData[RIGHT_COMPLICATION_ID] as
            LongTextComplicationData
        assertThat(
            rightComplicationData.text.getTextAt(
                ApplicationProvider.getApplicationContext<Context>().resources,
                0
            )
        ).isEqualTo("Right")

        EditorService.globalEditorService.unregisterObserver(observerId)
    }

    @Test
    public fun emptyInstanceId() {
        val scenario = createOnWatchFaceEditingTestActivity(
            listOf(colorStyleSetting, watchHandStyleSetting),
            emptyList(),
            watchFaceId = WatchFaceId("")
        )

        val editorObserver = TestEditorObserver()
        val observerId = EditorService.globalEditorService.registerObserver(editorObserver)

        scenario.onActivity { activity ->
            runBlocking {
                assertThat(activity.editorSession.watchFaceId.id).isEmpty()
                activity.editorSession.close()
                activity.finish()
            }
        }

        val result = editorObserver.awaitEditorStateChange(
            TIMEOUT_MILLIS,
            TimeUnit.MILLISECONDS
        ).asApiEditorState()
        assertThat(result.watchFaceId.id).isEmpty()

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

        val result = editorObserver.awaitEditorStateChange(
            TIMEOUT_MILLIS,
            TimeUnit.MILLISECONDS
        ).asApiEditorState()
        assertThat(result.previewComplicationsData).isEmpty()

        EditorService.globalEditorService.unregisterObserver(observerId)
    }

    @Test
    public fun dotNotCommit() {
        val scenario = createOnWatchFaceEditingTestActivity(
            listOf(colorStyleSetting, watchHandStyleSetting),
            emptyList()
        )
        val editorObserver = TestEditorObserver()
        val observerId = EditorService.globalEditorService.registerObserver(editorObserver)
        scenario.onActivity { activity ->
            runBlocking {
                assertThat(editorDelegate.userStyle[colorStyleSetting]!!.id.value)
                    .isEqualTo(redStyleOption.id.value)
                assertThat(editorDelegate.userStyle[watchHandStyleSetting]!!.id.value)
                    .isEqualTo(classicStyleOption.id.value)

                // Select [blueStyleOption] and [gothicStyleOption].
                val styleMap = activity.editorSession.userStyle.selectedOptions.toMutableMap()
                for (userStyleSetting in activity.editorSession.userStyleSchema.userStyleSettings) {
                    styleMap[userStyleSetting] = userStyleSetting.options.last()
                }
                activity.editorSession.userStyle = UserStyle(styleMap)

                // This should cause the style on the to be reverted back to the initial style.
                activity.editorSession.commitChangesOnClose = false
                activity.editorSession.close()
                activity.finish()
            }
        }

        val result = editorObserver.awaitEditorStateChange(
            TIMEOUT_MILLIS,
            TimeUnit.MILLISECONDS
        ).asApiEditorState()
        assertThat(result.userStyle.userStyleMap[colorStyleSetting.id.value])
            .isEqualTo(blueStyleOption.id.value)
        assertThat(result.userStyle.userStyleMap[watchHandStyleSetting.id.value])
            .isEqualTo(gothicStyleOption.id.value)
        assertFalse(result.shouldCommitChanges)

        // The original style should be applied to the watch face however because
        // commitChangesOnClose is false.
        assertThat(editorDelegate.userStyle[colorStyleSetting]!!.id.value)
            .isEqualTo(redStyleOption.id.value)
        assertThat(editorDelegate.userStyle[watchHandStyleSetting]!!.id.value)
            .isEqualTo(classicStyleOption.id.value)

        EditorService.globalEditorService.unregisterObserver(observerId)
    }

    @Test
    public fun commitChanges_preRFlow() {
        val scenario = createOnWatchFaceEditingTestActivity(
            listOf(colorStyleSetting, watchHandStyleSetting),
            emptyList(),
            preRFlow = true
        )
        val editorObserver = TestEditorObserver()
        val observerId = EditorService.globalEditorService.registerObserver(editorObserver)
        scenario.onActivity { activity ->
            runBlocking {
                assertThat(editorDelegate.userStyle[colorStyleSetting]!!.id.value)
                    .isEqualTo(redStyleOption.id.value)
                assertThat(editorDelegate.userStyle[watchHandStyleSetting]!!.id.value)
                    .isEqualTo(classicStyleOption.id.value)

                // Select [blueStyleOption] and [gothicStyleOption].
                val styleMap = activity.editorSession.userStyle.selectedOptions.toMutableMap()
                for (userStyleSetting in activity.editorSession.userStyleSchema.userStyleSettings) {
                    styleMap[userStyleSetting] = userStyleSetting.options.last()
                }
                activity.editorSession.userStyle = UserStyle(styleMap)

                activity.editorSession.close()
                activity.finish()
            }
        }

        val result = editorObserver.awaitEditorStateChange(
            TIMEOUT_MILLIS,
            TimeUnit.MILLISECONDS
        ).asApiEditorState()
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

    @Test
    public fun watchFaceEditorContract_createIntent() {
        runBlocking {
            val intent = WatchFaceEditorContract().createIntent(
                ApplicationProvider.getApplicationContext<Context>(),
                EditorRequest(testComponentName, testEditorPackageName, null, testInstanceId)
            )
            assertThat(intent.getPackage()).isEqualTo(testEditorPackageName)

            val editorRequest = EditorRequest.createFromIntent(intent)
            assertThat(editorRequest.editorPackageName).isEqualTo(testEditorPackageName)
            assertThat(editorRequest.initialUserStyle).isNull()
            assertThat(editorRequest.watchFaceComponentName).isEqualTo(testComponentName)
            assertThat(editorRequest.watchFaceId.id).isEqualTo(testInstanceId.id)
        }
    }

    @Test
    public fun forceCloseEditorSession() {
        val scenario = createOnWatchFaceEditingTestActivity(
            listOf(colorStyleSetting, watchHandStyleSetting),
            listOf(leftComplication, rightComplication)
        )

        val editorObserver = TestEditorObserver()
        val observerId = EditorService.globalEditorService.registerObserver(editorObserver)

        scenario.onActivity { activity ->
            runBlocking {
                // Select [blueStyleOption] and [gothicStyleOption].
                val styleMap = activity.editorSession.userStyle.selectedOptions.toMutableMap()
                for (userStyleSetting in activity.editorSession.userStyleSchema.userStyleSettings) {
                    styleMap[userStyleSetting] = userStyleSetting.options.last()
                }
                activity.editorSession.userStyle = UserStyle(styleMap)
            }
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
    public fun closeEditorSessionBeforeInitCompleted() {
        val session: ActivityScenario<OnWatchFaceEditingTestActivity> = ActivityScenario.launch(
            WatchFaceEditorContract().createIntent(
                ApplicationProvider.getApplicationContext<Context>(),
                EditorRequest(
                    testComponentName,
                    testEditorPackageName,
                    null,
                    WatchFaceId("instanceId")
                )
            ).apply {
                component = ComponentName(
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
        TestComplicationHelperActivity.resultIntent = CompletableDeferred(
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
                    ).toWireComplicationProviderInfo()
                )
            }
        )

        lateinit var baseEditorSession: BaseEditorSession
        lateinit var complicationDataSourceInfoRetriever: ComplicationDataSourceInfoRetriever
        var requestPreviewComplicationDataCount = 0
        val scenario = createOnWatchFaceEditingTestActivity(
            emptyList(),
            listOf(leftComplication, rightComplication),
            complicationDataSourceInfoRetrieverProvider = object :
                TestComplicationDataSourceInfoRetrieverProvider() {

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
                    // openComplicationDataSourceChooser and a result being selected. The previous two
                    // invocations where done to prime the map for getComplicationsPreviewData().
                    if (++requestPreviewComplicationDataCount == 3) {
                        baseEditorSession.forceClose()
                    } else {
                        previewComplicationDataCallback.updateComplicationData(
                            ShortTextComplicationData.Builder(
                                PlainComplicationText.Builder("TestData").build(),
                                ComplicationText.EMPTY
                            ).build().asWireComplicationData()
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
        val scenario = createOnWatchFaceEditingTestActivity(
            listOf(colorStyleSetting, watchHandStyleSetting),
            listOf(leftComplication, rightComplication)
        )

        scenario.onActivity { activity ->
            runBlocking {
                val previewData = activity.editorSession.getComplicationsPreviewData()
                assertThat(previewData.size).isEqualTo(2)
                assertThat(previewData[LEFT_COMPLICATION_ID])
                    .isInstanceOf(ShortTextComplicationData::class.java)
                val leftComplicationData =
                    previewData[LEFT_COMPLICATION_ID] as ShortTextComplicationData
                assertThat(
                    leftComplicationData.text.getTextAt(
                        ApplicationProvider.getApplicationContext<Context>().resources,
                        0
                    )
                ).isEqualTo("Left")

                assertThat(previewData[RIGHT_COMPLICATION_ID])
                    .isInstanceOf(LongTextComplicationData::class.java)
                val rightComplicationData =
                    previewData[RIGHT_COMPLICATION_ID] as LongTextComplicationData
                assertThat(
                    rightComplicationData.text.getTextAt(
                        ApplicationProvider.getApplicationContext<Context>().resources,
                        0
                    )
                ).isEqualTo("Right")
            }
        }
    }

    public fun getComplicationsPreviewData_withEmptyBackgroundComplication() {
        val scenario = createOnWatchFaceEditingTestActivity(
            listOf(colorStyleSetting, watchHandStyleSetting),
            listOf(leftComplication, backgroundComplication)
        )

        scenario.onActivity { activity ->
            runBlocking {
                val previewData = activity.editorSession.getComplicationsPreviewData()
                assertThat(previewData.size).isEqualTo(2)
                assertThat(previewData[LEFT_COMPLICATION_ID])
                    .isInstanceOf(ShortTextComplicationData::class.java)
                val leftComplicationData =
                    previewData[LEFT_COMPLICATION_ID] as ShortTextComplicationData
                assertThat(
                    leftComplicationData.text.getTextAt(
                        ApplicationProvider.getApplicationContext<Context>().resources,
                        0
                    )
                ).isEqualTo("Left")

                // TestComplicationDataSourceInfoRetrieverProvider isn't configured with a data
                // source for the background complication which means it behaves as if it was an
                // empty complication as far as fetching preview data is concerned.
                assertThat(previewData[BACKGROUND_COMPLICATION_ID])
                    .isInstanceOf(EmptyComplicationData::class.java)
            }
        }
    }

    @Test
    public fun testComponentNameMismatch() {
        val watchFaceId = WatchFaceId("ID-1")
        val scenario: ActivityScenario<OnWatchFaceEditingTestActivity> = ActivityScenario.launch(
            WatchFaceEditorContract().createIntent(
                ApplicationProvider.getApplicationContext<Context>(),
                EditorRequest(testComponentName, testEditorPackageName, null, watchFaceId)
            ).apply {
                component = ComponentName(
                    ApplicationProvider.getApplicationContext<Context>(),
                    OnWatchFaceEditingTestActivity::class.java
                )
            }
        )

        scenario.onActivity { activity ->
            val mockWatchFaceHostApi = mock(WatchFaceHostApi::class.java)
            val mockHandler = mock(Handler::class.java)
            `when`(mockWatchFaceHostApi.getUiThreadHandler()).thenReturn(mockHandler)
            `when`(mockWatchFaceHostApi.getContext()).thenReturn(
                ApplicationProvider.getApplicationContext<Context>()
            )
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
                    object : Renderer.CanvasRenderer(
                        mockSurfaceHolder,
                        currentUserStyleRepository,
                        watchState, CanvasType.SOFTWARE,
                        16
                    ) {
                        override fun render(canvas: Canvas, bounds: Rect, calendar: Calendar) {}

                        override fun renderHighlightLayer(
                            canvas: Canvas,
                            bounds: Rect,
                            calendar: Calendar
                        ) {
                        }
                    }
                ),
                mockWatchFaceHostApi,
                watchState,
                currentUserStyleRepository,
                ComplicationSlotsManager(emptyList(), currentUserStyleRepository),
                Calendar.getInstance(),
                BroadcastsObserver(
                    watchState,
                    mockWatchFaceHostApi,
                    CompletableDeferred(),
                    CoroutineScope(mockHandler.asCoroutineDispatcher())
                ),
                null
            )

            assertThat(activity.onCreateException).isInstanceOf(IllegalStateException::class.java)
            assertThat(activity.onCreateException.message).isEqualTo(
                "Expected ComponentInfo{test.package/test.class} to be created but " +
                    "got ComponentInfo{androidx.wear.watchface.editor.test/" +
                    "android.app.Application}"
            )
        }
    }
}

internal fun assertEquals(
    expected: ComplicationDataSourceInfo?,
    actual: ComplicationDataSourceInfo?
) = when (expected) {
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
