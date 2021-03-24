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
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.drawable.Icon
import android.os.Bundle
import android.os.IBinder
import android.os.Parcel
import android.support.wearable.complications.ComplicationData
import android.support.wearable.complications.ComplicationProviderInfo
import android.support.wearable.complications.IPreviewComplicationDataCallback
import android.support.wearable.complications.IProviderInfoService
import androidx.activity.ComponentActivity
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.wear.complications.ComplicationBounds
import androidx.wear.complications.DefaultComplicationProviderPolicy
import androidx.wear.complications.ProviderChooserIntent
import androidx.wear.complications.ProviderInfoRetriever
import androidx.wear.complications.SystemProviders
import androidx.wear.complications.data.ComplicationType
import androidx.wear.complications.data.LongTextComplicationData
import androidx.wear.complications.data.PlainComplicationText
import androidx.wear.complications.data.ShortTextComplicationData
import androidx.wear.watchface.CanvasComplicationDrawable
import androidx.wear.watchface.Complication
import androidx.wear.watchface.ComplicationsManager
import androidx.wear.watchface.MutableWatchState
import androidx.wear.watchface.RenderParameters
import androidx.wear.watchface.WatchFace
import androidx.wear.watchface.client.WatchFaceId
import androidx.wear.watchface.client.asApiEditorState
import androidx.wear.watchface.complications.rendering.ComplicationDrawable
import androidx.wear.watchface.data.ComplicationBoundsType
import androidx.wear.watchface.editor.data.EditorStateWireFormat
import androidx.wear.watchface.style.Layer
import androidx.wear.watchface.style.UserStyle
import androidx.wear.watchface.style.UserStyleRepository
import androidx.wear.watchface.style.UserStyleSchema
import androidx.wear.watchface.style.UserStyleSetting
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
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
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.coroutines.CoroutineContext

public const val LEFT_COMPLICATION_ID: Int = 1000
public const val RIGHT_COMPLICATION_ID: Int = 1001
public const val BACKGROUND_COMPLICATION_ID: Int = 1111

public val provider1: ComponentName = ComponentName("provider.app1", "provider.class1")
public val provider2: ComponentName = ComponentName("provider.app2", "provider.class2")
public val provider3: ComponentName = ComponentName("provider.app3", "provider.class3")

private const val TIMEOUT_MILLIS = 500L

private const val PROVIDER_CHOOSER_EXTRA_KEY = "PROVIDER_CHOOSER_EXTRA_KEY"
private const val PROVIDER_CHOOSER_EXTRA_VALUE = "PROVIDER_CHOOSER_EXTRA_VALUE"

/** Trivial "editor" which exposes the EditorSession for testing. */
public open class OnWatchFaceEditingTestActivity : ComponentActivity() {
    public lateinit var editorSession: EditorSession

    public val listenableEditorSession: ListenableEditorSession by lazy {
        ListenableEditorSession(editorSession)
    }

    public val providerIcon1: Icon =
        Icon.createWithBitmap(Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888))
    public val providerIcon2: Icon =
        Icon.createWithBitmap(Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888))

    public val immediateCoroutineScope: CoroutineScope = CoroutineScope(
        object : CoroutineDispatcher() {
            override fun dispatch(context: CoroutineContext, block: Runnable) {
                block.run()
            }
        }
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val deferredEditorSession =
            EditorSession.createOnWatchEditingSessionAsyncImpl(
                this@OnWatchFaceEditingTestActivity,
                intent!!,
                object : ProviderInfoRetrieverProvider {
                    override fun getProviderInfoRetriever() = ProviderInfoRetriever(
                        FakeProviderInfoServiceV1(
                            ComponentName("test.package", "test.class"),
                            mapOf(
                                LEFT_COMPLICATION_ID to ComplicationProviderInfo(
                                    "ProviderApp1",
                                    "Provider1",
                                    providerIcon1,
                                    ComplicationType.SHORT_TEXT.toWireComplicationType(),
                                    provider1
                                ),
                                RIGHT_COMPLICATION_ID to ComplicationProviderInfo(
                                    "ProviderApp2",
                                    "Provider2",
                                    providerIcon2,
                                    ComplicationType.LONG_TEXT.toWireComplicationType(),
                                    provider2
                                )
                            ),
                            mapOf(
                                provider1 to
                                    ShortTextComplicationData.Builder(
                                        PlainComplicationText.Builder("Left").build()
                                    ).build().asWireComplicationData(),
                                provider2 to
                                    LongTextComplicationData.Builder(
                                        PlainComplicationText.Builder("Right").build()
                                    ).build().asWireComplicationData(),
                                provider3 to
                                    LongTextComplicationData.Builder(
                                        PlainComplicationText.Builder("Provider3").build()
                                    ).build().asWireComplicationData(),
                            )
                        )
                    )
                }
            )

        immediateCoroutineScope.launch {
            editorSession = deferredEditorSession.await()!!
        }
    }
}

private class FakeProviderInfoServiceV1(
    private val watchFaceComponent: ComponentName,
    private val providerData: Map<Int, ComplicationProviderInfo>,
    private val previewData: Map<ComponentName, ComplicationData>
) : IProviderInfoService.Stub() {
    override fun getProviderInfos(
        watchFaceComponent: ComponentName,
        ids: IntArray
    ): Array<ComplicationProviderInfo>? {
        if (watchFaceComponent != this.watchFaceComponent) {
            return null
        }
        return ArrayList<ComplicationProviderInfo>().apply {
            for (id in ids) {
                providerData[id]?.let { add(it) }
            }
        }.toTypedArray()
    }

    override fun getApiVersion() = 1

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
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        lastIntent = intent

        setResult(
            123,
            Intent().apply {
                putExtra(
                    "android.support.wearable.complications.EXTRA_PROVIDER_INFO",
                    ComplicationProviderInfo(
                        "TestProvider3App",
                        "TestProvider3",
                        Icon.createWithBitmap(
                            Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
                        ),
                        ComplicationType.LONG_TEXT.toWireComplicationType(),
                        provider3
                    )
                )
            }
        )
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

    private val redStyleOption =
        UserStyleSetting.ListUserStyleSetting.ListOption("red_style", "Red", icon = null)

    private val greenStyleOption =
        UserStyleSetting.ListUserStyleSetting.ListOption("green_style", "Green", icon = null)

    private val blueStyleOption =
        UserStyleSetting.ListUserStyleSetting.ListOption("bluestyle", "Blue", icon = null)

    private val colorStyleList = listOf(redStyleOption, greenStyleOption, blueStyleOption)

    private val colorStyleSetting = UserStyleSetting.ListUserStyleSetting(
        "color_style_setting",
        "Colors",
        "Watchface colorization", /* icon = */
        null,
        colorStyleList,
        listOf(Layer.BASE_LAYER)
    )

    private val classicStyleOption =
        UserStyleSetting.ListUserStyleSetting.ListOption("classic_style", "Classic", icon = null)

    private val modernStyleOption =
        UserStyleSetting.ListUserStyleSetting.ListOption("modern_style", "Modern", icon = null)

    private val gothicStyleOption =
        UserStyleSetting.ListUserStyleSetting.ListOption("gothic_style", "Gothic", icon = null)

    private val watchHandStyleList =
        listOf(classicStyleOption, modernStyleOption, gothicStyleOption)

    private val watchHandStyleSetting = UserStyleSetting.ListUserStyleSetting(
        "hand_style_setting",
        "Hand Style",
        "Hand visual look", /* icon = */
        null,
        watchHandStyleList,
        listOf(Layer.TOP_LAYER)
    )

    private val placeholderWatchState = MutableWatchState().asWatchState()
    private val mockLeftCanvasComplication =
        CanvasComplicationDrawable(ComplicationDrawable(), placeholderWatchState)
    private val leftComplication =
        Complication.createRoundRectComplicationBuilder(
            LEFT_COMPLICATION_ID,
            mockLeftCanvasComplication,
            listOf(
                ComplicationType.RANGED_VALUE,
                ComplicationType.LONG_TEXT,
                ComplicationType.SHORT_TEXT,
                ComplicationType.MONOCHROMATIC_IMAGE,
                ComplicationType.SMALL_IMAGE
            ),
            DefaultComplicationProviderPolicy(SystemProviders.SUNRISE_SUNSET),
            ComplicationBounds(RectF(0.2f, 0.4f, 0.4f, 0.6f))
        ).setDefaultProviderType(ComplicationType.SHORT_TEXT)
            .build()

    private val mockRightCanvasComplication =
        CanvasComplicationDrawable(ComplicationDrawable(), placeholderWatchState)
    private val rightComplication =
        Complication.createRoundRectComplicationBuilder(
            RIGHT_COMPLICATION_ID,
            mockRightCanvasComplication,
            listOf(
                ComplicationType.RANGED_VALUE,
                ComplicationType.LONG_TEXT,
                ComplicationType.SHORT_TEXT,
                ComplicationType.MONOCHROMATIC_IMAGE,
                ComplicationType.SMALL_IMAGE
            ),
            DefaultComplicationProviderPolicy(SystemProviders.DAY_OF_WEEK),
            ComplicationBounds(RectF(0.6f, 0.4f, 0.8f, 0.6f))
        ).setDefaultProviderType(ComplicationType.SHORT_TEXT)
            .setConfigExtras(
                Bundle().apply {
                    putString(PROVIDER_CHOOSER_EXTRA_KEY, PROVIDER_CHOOSER_EXTRA_VALUE)
                }
            )
            .build()

    private val mockBackgroundCanvasComplication =
        CanvasComplicationDrawable(ComplicationDrawable(), placeholderWatchState)
    private val backgroundComplication =
        Complication.createBackgroundComplicationBuilder(
            BACKGROUND_COMPLICATION_ID,
            mockBackgroundCanvasComplication,
            emptyList(),
            DefaultComplicationProviderPolicy()
        ).setEnabled(false).build()

    private val fakeBitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
    private val onDestroyLatch = CountDownLatch(1)

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
        previewReferenceTimeMillis: Long = 12345
    ): ActivityScenario<OnWatchFaceEditingTestActivity> {
        val userStyleRepository = UserStyleRepository(UserStyleSchema(userStyleSettings))
        val complicationsManager = ComplicationsManager(complications, userStyleRepository)

        // Mocking getters and setters with mockito at the same time is hard so we do this instead.
        editorDelegate = object : WatchFace.EditorDelegate {
            override val userStyleSchema = userStyleRepository.schema
            override var userStyle: UserStyle
                get() = userStyleRepository.userStyle
                set(value) { userStyleRepository.userStyle = value }

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
        WatchFace.registerEditorDelegate(testComponentName, editorDelegate)

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
            assertThat(userStyleSchema.userStyleSettings[0].id).isEqualTo(colorStyleSetting.id)
            assertThat(userStyleSchema.userStyleSettings[0].options.size)
                .isEqualTo(colorStyleSetting.options.size)
            assertThat(userStyleSchema.userStyleSettings[1].id).isEqualTo(watchHandStyleSetting.id)
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
            CanvasComplicationDrawable(ComplicationDrawable(), placeholderWatchState)
        val fixedLeftComplication =
            Complication.createRoundRectComplicationBuilder(
                LEFT_COMPLICATION_ID,
                mockLeftCanvasComplication,
                listOf(
                    ComplicationType.RANGED_VALUE,
                    ComplicationType.LONG_TEXT,
                    ComplicationType.SHORT_TEXT,
                    ComplicationType.MONOCHROMATIC_IMAGE,
                    ComplicationType.SMALL_IMAGE
                ),
                DefaultComplicationProviderPolicy(SystemProviders.SUNRISE_SUNSET),
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
                    it.editorSession.launchComplicationProviderChooser(LEFT_COMPLICATION_ID)

                    fail(
                        "launchComplicationProviderChooser should fail for a fixed complication " +
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
                val providerComponentName = ComponentName("test.package", "test.class")
                val complicationType = ComplicationData.TYPE_SHORT_TEXT
                val providerIcon =
                    Icon.createWithBitmap(Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888))
                val complicationText = "TestText"
                val mockBinder = Mockito.mock(IBinder::class.java)

                `when`(mockProviderInfoService.apiVersion).thenReturn(1)
                `when`(mockProviderInfoService.asBinder()).thenReturn(mockBinder)

                doAnswer {
                    val callback = it.arguments[2] as IPreviewComplicationDataCallback
                    callback.updateComplicationData(
                        ShortTextComplicationData.Builder(
                            PlainComplicationText.Builder(complicationText).build()
                        ).build().asWireComplicationData()
                    )
                    true
                }.`when`(mockProviderInfoService).requestPreviewComplicationData(
                    eq(providerComponentName),
                    eq(complicationType),
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
    public fun getPreviewData_preRFallback() {
        val scenario = createOnWatchFaceEditingTestActivity(
            emptyList(),
            listOf(leftComplication, rightComplication, backgroundComplication)
        )
        scenario.onActivity {
            runBlocking {
                val editorSession = it.editorSession as OnWatchFaceEditorSessionImpl
                val mockProviderInfoService = Mockito.mock(IProviderInfoService::class.java)
                val complicationType = ComplicationData.TYPE_SHORT_TEXT
                val providerIcon =
                    Icon.createWithBitmap(Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888))

                val providerInfoRetriever = ProviderInfoRetriever(mockProviderInfoService)
                val previewComplication = editorSession.getPreviewData(
                    providerInfoRetriever,
                    // Construct a ComplicationProviderInfo with null providerComponentName.
                    ComplicationProviderInfo(
                        Parcel.obtain().apply {
                            writeBundle(
                                Bundle().apply {
                                    putString("app_name", "provider.app")
                                    putString("provider_name", "provider")
                                    putParcelable("provider_icon", providerIcon)
                                    putInt("complication_type", complicationType)
                                }
                            )
                            setDataPosition(0)
                        }
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
    public fun getPreviewData_postRFallback() {
        val scenario = createOnWatchFaceEditingTestActivity(
            emptyList(),
            listOf(leftComplication, rightComplication, backgroundComplication)
        )
        scenario.onActivity {
            runBlocking {
                val editorSession = it.editorSession as OnWatchFaceEditorSessionImpl
                val mockProviderInfoService = Mockito.mock(IProviderInfoService::class.java)
                val providerComponentName = ComponentName("test.package", "test.class")
                val complicationType = ComplicationData.TYPE_SHORT_TEXT
                val providerIcon =
                    Icon.createWithBitmap(Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888))

                `when`(mockProviderInfoService.apiVersion).thenReturn(1)
                `when`(
                    mockProviderInfoService.requestPreviewComplicationData(
                        eq(providerComponentName),
                        eq(complicationType),
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
            assertTrue(editorSession.launchComplicationProviderChooser(LEFT_COMPLICATION_ID))

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

    @Test
    public fun launchComplicationProviderChooser_ComplicationConfigExtras() {
        ComplicationProviderChooserContract.useTestComplicationHelperActivity = true

        val scenario = createOnWatchFaceEditingTestActivity(
            emptyList(),
            listOf(leftComplication, rightComplication)
        )

        lateinit var editorSession: EditorSession
        scenario.onActivity { activity ->
            editorSession = activity.editorSession
        }

        runBlocking {
            assertTrue(editorSession.launchComplicationProviderChooser(RIGHT_COMPLICATION_ID))

            assertThat(
                TestComplicationHelperActivity.lastIntent?.extras?.getString(
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

        assertThat(result.userStyle[colorStyleSetting.id]).isEqualTo(blueStyleOption.id)
        assertThat(result.userStyle[watchHandStyleSetting.id]).isEqualTo(gothicStyleOption.id)
        assertThat(result.watchFaceId.id).isEqualTo(testInstanceId.id)
        assertTrue(result.shouldCommitChanges)

        // The style change should also have been applied to the watchface
        assertThat(editorDelegate.userStyle[colorStyleSetting]!!.id).isEqualTo(blueStyleOption.id)
        assertThat(editorDelegate.userStyle[watchHandStyleSetting]!!.id)
            .isEqualTo(gothicStyleOption.id)

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
                assertThat(editorDelegate.userStyle[colorStyleSetting]!!.id)
                    .isEqualTo(redStyleOption.id)
                assertThat(editorDelegate.userStyle[watchHandStyleSetting]!!.id)
                    .isEqualTo(classicStyleOption.id)

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
        assertThat(result.userStyle[colorStyleSetting.id]).isEqualTo(blueStyleOption.id)
        assertThat(result.userStyle[watchHandStyleSetting.id]).isEqualTo(gothicStyleOption.id)
        assertFalse(result.shouldCommitChanges)

        // The original style should be applied to the watch face however because
        // commitChangesOnClose is false.
        assertThat(editorDelegate.userStyle[colorStyleSetting]!!.id).isEqualTo(redStyleOption.id)
        assertThat(editorDelegate.userStyle[watchHandStyleSetting]!!.id)
            .isEqualTo(classicStyleOption.id)

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

            val editorRequest = EditorRequest.createFromIntent(intent)!!
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
        assertThat(editorDelegate.userStyle[colorStyleSetting]!!.id).isEqualTo(redStyleOption.id)
        assertThat(editorDelegate.userStyle[watchHandStyleSetting]!!.id)
            .isEqualTo(classicStyleOption.id)

        EditorService.globalEditorService.unregisterObserver(observerId)
    }

    @Test
    public fun closeEditorSessionBeforeWatchFaceDelegateCreated() {
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

        session.onActivity { activity ->
            // This shouldn't throw an exception.
            activity.editorSession.close()
        }
    }
}
