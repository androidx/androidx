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
import androidx.test.filters.FlakyTest
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.wear.complications.ComplicationBounds
import androidx.wear.complications.ComplicationHelperActivity
import androidx.wear.complications.ComplicationProviderInfo
import androidx.wear.complications.DefaultComplicationProviderPolicy
import androidx.wear.complications.ProviderChooserIntent
import androidx.wear.complications.ProviderInfoRetriever
import androidx.wear.complications.SystemProviders
import androidx.wear.complications.data.ComplicationText
import androidx.wear.complications.data.ComplicationType
import androidx.wear.complications.data.EmptyComplicationData
import androidx.wear.complications.data.LongTextComplicationData
import androidx.wear.complications.data.PlainComplicationText
import androidx.wear.complications.data.ShortTextComplicationData
import androidx.wear.watchface.BroadcastsObserver
import androidx.wear.watchface.CanvasComplication
import androidx.wear.watchface.CanvasType
import androidx.wear.watchface.Complication
import androidx.wear.watchface.ComplicationsManager
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
import androidx.wear.watchface.data.ComplicationBoundsType
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

public val provider1: ComponentName = ComponentName("provider.app1", "provider.class1")
public val provider2: ComponentName = ComponentName("provider.app2", "provider.class2")
public val provider3: ComponentName = ComponentName("provider.app3", "provider.class3")

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
        internal var providerInfoRetrieverProvider: ProviderInfoRetrieverProvider? = null
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
                    providerInfoRetrieverProvider!!
                )
            } catch (e: Exception) {
                onCreateException = e
            } finally {
                creationLatch.countDown()
            }
        }
    }
}

public open class TestProviderInfoRetrieverProvider :
    ProviderInfoRetrieverProvider, IProviderInfoService.Stub() {

    private val providerIcon1: Icon =
        Icon.createWithBitmap(Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888))
    private val providerIcon2: Icon =
        Icon.createWithBitmap(Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888))

    private val watchFaceComponent = ComponentName("test.package", "test.class")
    private val providerData = mapOf(
        LEFT_COMPLICATION_ID to ComplicationProviderInfo(
            "ProviderApp1",
            "Provider1",
            providerIcon1,
            ComplicationType.SHORT_TEXT,
            provider1
        ),
        RIGHT_COMPLICATION_ID to ComplicationProviderInfo(
            "ProviderApp2",
            "Provider2",
            providerIcon2,
            ComplicationType.LONG_TEXT,
            provider2
        )
    )
    private val previewData = mapOf(
        provider1 to
            ShortTextComplicationData.Builder(
                PlainComplicationText.Builder("Left").build(),
                ComplicationText.EMPTY
            ).build().asWireComplicationData(),
        provider2 to
            LongTextComplicationData.Builder(
                PlainComplicationText.Builder("Right").build(),
                ComplicationText.EMPTY
            ).build().asWireComplicationData(),
        provider3 to
            LongTextComplicationData.Builder(
                PlainComplicationText.Builder("Provider3").build(),
                ComplicationText.EMPTY
            ).build().asWireComplicationData(),
    )

    override fun getProviderInfoRetriever(): ProviderInfoRetriever = ProviderInfoRetriever(this)

    override fun getProviderInfos(
        watchFaceComponent: ComponentName,
        ids: IntArray
    ): Array<WireComplicationProviderInfo?>? {
        if (watchFaceComponent != this.watchFaceComponent) {
            return null
        }
        return ArrayList<WireComplicationProviderInfo?>().apply {
            for (id in ids) {
                add(providerData[id]?.toWireComplicationProviderInfo())
            }
        }.toTypedArray()
    }

    override fun getApiVersion(): Int = 1

    override fun requestPreviewComplicationData(
        providerComponent: ComponentName,
        complicationType: Int,
        previewComplicationDataCallback: IPreviewComplicationDataCallback
    ): Boolean {
        previewComplicationDataCallback.updateComplicationData(previewData[providerComponent])
        return true
    }
}

/** Fake ComplicationHelperActivity for testing. */
public class TestComplicationHelperActivity : Activity() {

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

/** Fake complication provider choooser for testing. */
public class TestComplicationProviderChooserActivity : Activity() {

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
        Complication.createRoundRectComplicationBuilder(
            LEFT_COMPLICATION_ID,
            { _, _ -> mockLeftCanvasComplication },
            listOf(
                ComplicationType.RANGED_VALUE,
                ComplicationType.LONG_TEXT,
                ComplicationType.SHORT_TEXT,
                ComplicationType.MONOCHROMATIC_IMAGE,
                ComplicationType.SMALL_IMAGE
            ),
            DefaultComplicationProviderPolicy(SystemProviders.PROVIDER_SUNRISE_SUNSET),
            ComplicationBounds(RectF(0.2f, 0.4f, 0.4f, 0.6f))
        ).setDefaultProviderType(ComplicationType.SHORT_TEXT)
            .build()

    private val mockRightCanvasComplication = CanvasComplicationDrawable(
        ComplicationDrawable(),
        placeholderWatchState,
        mockInvalidateCallback
    )
    private val rightComplication =
        Complication.createRoundRectComplicationBuilder(
            RIGHT_COMPLICATION_ID,
            { _, _ -> mockRightCanvasComplication },
            listOf(
                ComplicationType.RANGED_VALUE,
                ComplicationType.LONG_TEXT,
                ComplicationType.SHORT_TEXT,
                ComplicationType.MONOCHROMATIC_IMAGE,
                ComplicationType.SMALL_IMAGE
            ),
            DefaultComplicationProviderPolicy(SystemProviders.PROVIDER_DAY_OF_WEEK),
            ComplicationBounds(RectF(0.6f, 0.4f, 0.8f, 0.6f))
        ).setDefaultProviderType(ComplicationType.SHORT_TEXT)
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
        Complication.createBackgroundComplicationBuilder(
            BACKGROUND_COMPLICATION_ID,
            { _, _ -> mockBackgroundCanvasComplication },
            emptyList(),
            DefaultComplicationProviderPolicy()
        ).setEnabled(false).build()

    private val fakeBitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
    private val onDestroyLatch = CountDownLatch(1)
    private val providerIcon =
        Icon.createWithBitmap(Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888))
    private val providerComponentName = ComponentName("test.package", "test.class")

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
        complications: List<Complication>,
        watchFaceId: WatchFaceId = testInstanceId,
        previewReferenceTimeMillis: Long = 12345,
        providerInfoRetrieverProvider: ProviderInfoRetrieverProvider =
            TestProviderInfoRetrieverProvider(),
        shouldTimeout: Boolean = false,
        preRFlow: Boolean = false
    ): ActivityScenario<OnWatchFaceEditingTestActivity> {
        val userStyleRepository = CurrentUserStyleRepository(UserStyleSchema(userStyleSettings))
        val complicationsManager = ComplicationsManager(complications, userStyleRepository)
        complicationsManager.watchState = placeholderWatchState

        // Mocking getters and setters with mockito at the same time is hard so we do this instead.
        editorDelegate = object : WatchFace.EditorDelegate {
            override val userStyleSchema = userStyleRepository.schema
            override var userStyle: UserStyle
                get() = userStyleRepository.userStyle
                set(value) {
                    userStyleRepository.userStyle = value
                }

            override val complicationsManager = complicationsManager
            override val screenBounds = this@EditorSessionTest.screenBounds
            override val previewReferenceTimeMillis = previewReferenceTimeMillis

            override fun renderWatchFaceToBitmap(
                renderParameters: RenderParameters,
                calendarTimeMillis: Long,
                idToComplicationData: Map<Int, androidx.wear.complications.data.ComplicationData>?
            ) = fakeBitmap

            override fun onDestroy() {
                onDestroyLatch.countDown()
            }
        }
        if (!shouldTimeout) {
            WatchFace.registerEditorDelegate(testComponentName, editorDelegate)
        }

        OnWatchFaceEditingTestActivity.providerInfoRetrieverProvider = providerInfoRetrieverProvider

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
        ComplicationProviderChooserContract.useTestComplicationHelperActivity = false
        ComplicationHelperActivity.useTestComplicationProviderChooserActivity = false
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
            assertThat(it.editorSession.backgroundComplicationId).isEqualTo(null)
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
            assertThat(it.editorSession.backgroundComplicationId).isEqualTo(
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
            assertThat(it.editorSession.complicationsState.size).isEqualTo(3)
            assertThat(it.editorSession.complicationsState[LEFT_COMPLICATION_ID]!!.bounds)
                .isEqualTo(Rect(80, 160, 160, 240))
            assertThat(it.editorSession.complicationsState[LEFT_COMPLICATION_ID]!!.boundsType)
                .isEqualTo(ComplicationBoundsType.ROUND_RECT)
            assertFalse(
                it.editorSession.complicationsState[
                    LEFT_COMPLICATION_ID
                ]!!.fixedComplicationProvider
            )
            assertTrue(
                it.editorSession.complicationsState[LEFT_COMPLICATION_ID]!!.isInitiallyEnabled
            )

            assertThat(it.editorSession.complicationsState[RIGHT_COMPLICATION_ID]!!.bounds)
                .isEqualTo(Rect(240, 160, 320, 240))
            assertThat(it.editorSession.complicationsState[RIGHT_COMPLICATION_ID]!!.boundsType)
                .isEqualTo(ComplicationBoundsType.ROUND_RECT)
            assertFalse(
                it.editorSession.complicationsState[RIGHT_COMPLICATION_ID]!!
                    .fixedComplicationProvider
            )
            assertTrue(
                it.editorSession.complicationsState[RIGHT_COMPLICATION_ID]!!.isInitiallyEnabled
            )

            assertThat(it.editorSession.complicationsState[BACKGROUND_COMPLICATION_ID]!!.bounds)
                .isEqualTo(screenBounds)
            assertThat(it.editorSession.complicationsState[BACKGROUND_COMPLICATION_ID]!!.boundsType)
                .isEqualTo(ComplicationBoundsType.BACKGROUND)
            assertFalse(
                it.editorSession.complicationsState[BACKGROUND_COMPLICATION_ID]!!
                    .fixedComplicationProvider
            )
            assertFalse(
                it.editorSession.complicationsState[BACKGROUND_COMPLICATION_ID]!!.isInitiallyEnabled
            )
            // We could test more state but this should be enough.
        }
    }

    @Test
    public fun fixedComplicationProvider() {
        val mockLeftCanvasComplication =
            CanvasComplicationDrawable(
                ComplicationDrawable(),
                placeholderWatchState,
                mockInvalidateCallback
            )
        val fixedLeftComplication =
            Complication.createRoundRectComplicationBuilder(
                LEFT_COMPLICATION_ID,
                { _, _ -> mockLeftCanvasComplication },
                listOf(
                    ComplicationType.RANGED_VALUE,
                    ComplicationType.LONG_TEXT,
                    ComplicationType.SHORT_TEXT,
                    ComplicationType.MONOCHROMATIC_IMAGE,
                    ComplicationType.SMALL_IMAGE
                ),
                DefaultComplicationProviderPolicy(SystemProviders.PROVIDER_SUNRISE_SUNSET),
                ComplicationBounds(RectF(0.2f, 0.4f, 0.4f, 0.6f))
            ).setDefaultProviderType(ComplicationType.SHORT_TEXT)
                .setFixedComplicationProvider(true)
                .build()

        val scenario = createOnWatchFaceEditingTestActivity(
            emptyList(),
            listOf(fixedLeftComplication)
        )
        scenario.onActivity {
            assertTrue(
                it.editorSession.complicationsState[
                    LEFT_COMPLICATION_ID
                ]!!.fixedComplicationProvider
            )

            try {
                runBlocking {
                    it.editorSession.openComplicationProviderChooser(LEFT_COMPLICATION_ID)

                    fail(
                        "openComplicationProviderChooser should fail for a fixed complication " +
                            "provider"
                    )
                }
            } catch (e: Exception) {
                // Expected.
            }
        }
    }

    @Test
    public fun getPreviewData_null_providerInfo() {
        val scenario = createOnWatchFaceEditingTestActivity(
            emptyList(),
            listOf(leftComplication, rightComplication, backgroundComplication)
        )
        scenario.onActivity {
            runBlocking {
                val editorSession = it.editorSession as OnWatchFaceEditorSessionImpl
                val mockProviderInfoService = Mockito.mock(IProviderInfoService::class.java)

                val providerInfoRetriever = ProviderInfoRetriever(mockProviderInfoService)
                assertThat(
                    editorSession.getPreviewData(providerInfoRetriever, null)
                ).isNull()
                providerInfoRetriever.close()
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
                    eq(providerComponentName),
                    eq(complicationType.toWireComplicationType()),
                    any()
                )

                val providerInfoRetriever = ProviderInfoRetriever(mockProviderInfoService)
                val deferredPreviewData = async {
                    editorSession.getPreviewData(
                        providerInfoRetriever,
                        ComplicationProviderInfo(
                            "provider.app",
                            "provider",
                            providerIcon,
                            complicationType,
                            providerComponentName
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

                providerInfoRetriever.close()
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

                val providerInfoRetriever = ProviderInfoRetriever(mockProviderInfoService)
                val previewComplication = editorSession.getPreviewData(
                    providerInfoRetriever,
                    // Construct a ComplicationProviderInfo with null providerComponentName.
                    ComplicationProviderInfo(
                        "provider.app",
                        "provider",
                        providerIcon,
                        complicationType,
                        null,
                    )
                ) as ShortTextComplicationData

                assertThat(
                    previewComplication.text.getTextAt(
                        ApplicationProvider.getApplicationContext<Context>().resources,
                        0
                    )
                ).isEqualTo("provider")

                providerInfoRetriever.close()
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
                        eq(providerComponentName),
                        eq(complicationType.toWireComplicationType()),
                        any(IPreviewComplicationDataCallback::class.java)
                    )
                ).thenReturn(false) // Triggers the ExecutionException.

                val previewComplication = editorSession.getPreviewData(
                    ProviderInfoRetriever(mockProviderInfoService),
                    ComplicationProviderInfo(
                        "provider.app",
                        "provider",
                        providerIcon,
                        complicationType,
                        providerComponentName
                    )
                ) as ShortTextComplicationData

                assertThat(
                    previewComplication.text.getTextAt(
                        ApplicationProvider.getApplicationContext<Context>().resources,
                        0
                    )
                ).isEqualTo("provider")
            }
        }
    }

    @Test
    public fun launchComplicationProviderChooser() {
        ComplicationProviderChooserContract.useTestComplicationHelperActivity = true
        val chosenComplicationProviderInfo = ComplicationProviderInfo(
            "TestProvider3App",
            "TestProvider3",
            Icon.createWithBitmap(
                Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
            ),
            ComplicationType.LONG_TEXT,
            provider3
        )
        TestComplicationHelperActivity.resultIntent = Intent().apply {
            putExtra(
                "android.support.wearable.complications.EXTRA_PROVIDER_INFO",
                chosenComplicationProviderInfo.toWireComplicationProviderInfo()
            )
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
             * Invoke [TestComplicationHelperActivity] which will change the provider (and hence
             * the preview data) for [LEFT_COMPLICATION_ID].
             */
            val chosenComplicationProvider =
                editorSession.openComplicationProviderChooser(LEFT_COMPLICATION_ID)
            assertThat(chosenComplicationProvider).isNotNull()
            checkNotNull(chosenComplicationProvider)
            assertThat(chosenComplicationProvider.complicationId).isEqualTo(LEFT_COMPLICATION_ID)
            assertEquals(
                chosenComplicationProviderInfo,
                chosenComplicationProvider.complicationProviderInfo
            )

            // This should update the preview data to point to the updated provider3 data.
            val previewComplication =
                editorSession.getComplicationsPreviewData()[LEFT_COMPLICATION_ID]
                    as LongTextComplicationData

            assertThat(
                previewComplication.text.getTextAt(
                    ApplicationProvider.getApplicationContext<Context>().resources,
                    0
                )
            ).isEqualTo("Provider3")

            assertThat(
                TestComplicationHelperActivity.lastIntent?.extras?.getString(
                    ProviderChooserIntent.EXTRA_WATCHFACE_INSTANCE_ID
                )
            ).isEqualTo(testInstanceId.id)
        }
    }

    @FlakyTest(bugId = 189939975)
    @Test
    public fun launchComplicationProviderChooserTwiceBackToBack() {
        ComplicationProviderChooserContract.useTestComplicationHelperActivity = true
        TestComplicationHelperActivity.resultIntent = Intent().apply {
            putExtra(
                "android.support.wearable.complications.EXTRA_PROVIDER_INFO",
                ComplicationProviderInfo(
                    "TestProvider3App",
                    "TestProvider3",
                    Icon.createWithBitmap(
                        Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
                    ),
                    ComplicationType.LONG_TEXT,
                    provider3
                ).toWireComplicationProviderInfo()
            )
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
            val pendingResult = async {
                editorSession.openComplicationProviderChooser(LEFT_COMPLICATION_ID)
            }

            // This shouldn't crash.
            assertThat(editorSession.openComplicationProviderChooser(LEFT_COMPLICATION_ID))
                .isNotNull()
            assertThat(pendingResult).isNotNull()
        }
    }

    @Test
    public fun launchComplicationProviderChooser_chooseEmpty() {
        ComplicationProviderChooserContract.useTestComplicationHelperActivity = true
        TestComplicationHelperActivity.resultIntent = Intent().apply {}

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
             * Invoke [TestComplicationHelperActivity] which will change the provider (and hence
             * the preview data) for [LEFT_COMPLICATION_ID].
             */
            val chosenComplicationProvider =
                editorSession.openComplicationProviderChooser(LEFT_COMPLICATION_ID)
            assertThat(chosenComplicationProvider).isNotNull()
            checkNotNull(chosenComplicationProvider)
            assertThat(chosenComplicationProvider.complicationId).isEqualTo(LEFT_COMPLICATION_ID)
            assertThat(chosenComplicationProvider.complicationProviderInfo).isNull()
            assertThat(editorSession.getComplicationsPreviewData()[LEFT_COMPLICATION_ID])
                .isInstanceOf(EmptyComplicationData::class.java)
        }
    }

    @Test
    public fun launchComplicationProviderChooser_cancel() {
        ComplicationProviderChooserContract.useTestComplicationHelperActivity = true
        TestComplicationHelperActivity.resultIntent = null

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
            assertThat(editorSession.openComplicationProviderChooser(LEFT_COMPLICATION_ID)).isNull()
        }
    }

    @Test
    public fun launchComplicationProviderChooser_ComplicationConfigExtrasToHelper() {
        ComplicationProviderChooserContract.useTestComplicationHelperActivity = true
        val chosenComplicationProviderInfo = ComplicationProviderInfo(
            "TestProvider3App",
            "TestProvider3",
            Icon.createWithBitmap(
                Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
            ),
            ComplicationType.LONG_TEXT,
            provider3
        )
        TestComplicationHelperActivity.resultIntent = Intent().apply {
            putExtra(
                "android.support.wearable.complications.EXTRA_PROVIDER_INFO",
                chosenComplicationProviderInfo.toWireComplicationProviderInfo()
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
            val chosenComplicationProvider =
                editorSession.openComplicationProviderChooser(RIGHT_COMPLICATION_ID)
            assertThat(chosenComplicationProvider).isNotNull()
            checkNotNull(chosenComplicationProvider)
            assertThat(chosenComplicationProvider.complicationId).isEqualTo(RIGHT_COMPLICATION_ID)
            assertEquals(
                chosenComplicationProviderInfo,
                chosenComplicationProvider.complicationProviderInfo
            )
            assertThat(
                chosenComplicationProvider.extras[PROVIDER_CHOOSER_RESULT_EXTRA_KEY]
            ).isEqualTo(PROVIDER_CHOOSER_RESULT_EXTRA_VALUE)

            assertThat(
                TestComplicationHelperActivity.lastIntent?.extras?.getString(
                    PROVIDER_CHOOSER_EXTRA_KEY
                )
            ).isEqualTo(PROVIDER_CHOOSER_EXTRA_VALUE)
        }
    }

    @Test
    public fun launchComplicationProviderChooser_ComplicationConfigExtrasToChooser() {
        // Invoke the test provider chooser to record the result.
        ComplicationHelperActivity.useTestComplicationProviderChooserActivity = true
        // Invoke the provider chooser without checking for permissions first.
        ComplicationHelperActivity.skipPermissionCheck = true

        val chosenComplicationProviderInfo = ComplicationProviderInfo(
            "TestProvider3App",
            "TestProvider3",
            Icon.createWithBitmap(
                Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
            ),
            ComplicationType.LONG_TEXT,
            provider3
        )
        TestComplicationProviderChooserActivity.resultIntent = Intent().apply {
            putExtra(
                "android.support.wearable.complications.EXTRA_PROVIDER_INFO",
                chosenComplicationProviderInfo.toWireComplicationProviderInfo()
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
            val chosenComplicationProvider =
                editorSession.openComplicationProviderChooser(RIGHT_COMPLICATION_ID)
            assertThat(chosenComplicationProvider).isNotNull()
            checkNotNull(chosenComplicationProvider)
            assertThat(chosenComplicationProvider.complicationId).isEqualTo(RIGHT_COMPLICATION_ID)
            assertEquals(
                chosenComplicationProviderInfo,
                chosenComplicationProvider.complicationProviderInfo
            )
            assertThat(
                chosenComplicationProvider.extras[PROVIDER_CHOOSER_RESULT_EXTRA_KEY]
            ).isEqualTo(PROVIDER_CHOOSER_RESULT_EXTRA_VALUE)

            assertThat(
                TestComplicationProviderChooserActivity.lastIntent?.extras?.getString(
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
            assertThat(it.editorSession.getComplicationIdAt(0, 0)).isEqualTo(null)
            assertThat(it.editorSession.getComplicationIdAt(85, 165))
                .isEqualTo(LEFT_COMPLICATION_ID)
            assertThat(it.editorSession.getComplicationIdAt(245, 165))
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
        lateinit var baseEditorSession: BaseEditorSession
        lateinit var providerInfoRetriever: ProviderInfoRetriever
        var forceClosed = false
        val scenario = createOnWatchFaceEditingTestActivity(
            emptyList(),
            listOf(leftComplication, rightComplication),
            providerInfoRetrieverProvider = object : TestProviderInfoRetrieverProvider() {

                override fun getProviderInfoRetriever(): ProviderInfoRetriever {
                    providerInfoRetriever = super.getProviderInfoRetriever()
                    return providerInfoRetriever
                }

                override fun requestPreviewComplicationData(
                    providerComponent: ComponentName,
                    complicationType: Int,
                    previewComplicationDataCallback: IPreviewComplicationDataCallback
                ): Boolean {
                    if (!forceClosed) {
                        baseEditorSession.forceClose()
                        forceClosed = true
                    }
                    return true
                }
            }
        )
        scenario.onActivity {
            baseEditorSession = it.editorSession as BaseEditorSession
            baseEditorSession.pendingComplicationProviderChooserResult = CompletableDeferred()
            baseEditorSession.onComplicationProviderChooserResult(
                ComplicationProviderChooserResult(
                    ComplicationProviderInfo(
                        "provider.app",
                        "provider",
                        providerIcon,
                        ComplicationType.SHORT_TEXT,
                        providerComponentName
                    ),
                    Bundle.EMPTY
                )
            )
        }

        // Make sure everything that was going to run has run.
        runBlocking {
            baseEditorSession.coroutineScope.coroutineContext.job.join()
        }

        // Ensure the providerInfoRetriever was closed despite forceClose() being called.
        assertThat(providerInfoRetriever.closed).isTrue()
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

                // TestProviderInfoRetrieverProvider isn't configured with a provider for the
                // background complication which means it behaves as if it was an empty
                // complication as far as fetching preview data is concerned.
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
                ComplicationsManager(emptyList(), currentUserStyleRepository),
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

internal fun assertEquals(expected: ComplicationProviderInfo?, actual: ComplicationProviderInfo?) =
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
