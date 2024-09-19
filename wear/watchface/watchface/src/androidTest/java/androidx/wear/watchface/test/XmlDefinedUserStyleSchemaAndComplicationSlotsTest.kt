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

package androidx.wear.watchface.test

import android.content.ComponentName
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Rect
import android.graphics.RectF
import android.os.Build
import android.view.Surface
import android.view.SurfaceHolder
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.wear.watchface.CanvasComplication
import androidx.wear.watchface.CanvasComplicationFactory
import androidx.wear.watchface.CanvasType
import androidx.wear.watchface.ComplicationSlotBoundsType
import androidx.wear.watchface.ComplicationSlotInflationFactory
import androidx.wear.watchface.ComplicationSlotsManager
import androidx.wear.watchface.RenderParameters
import androidx.wear.watchface.Renderer
import androidx.wear.watchface.StatefulWatchFaceService
import androidx.wear.watchface.WatchFace
import androidx.wear.watchface.WatchFaceService
import androidx.wear.watchface.WatchFaceType
import androidx.wear.watchface.WatchState
import androidx.wear.watchface.complications.SystemDataSources
import androidx.wear.watchface.complications.data.ComplicationData
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.data.NoDataComplicationData
import androidx.wear.watchface.control.IInteractiveWatchFace
import androidx.wear.watchface.control.IPendingInteractiveWatchFace
import androidx.wear.watchface.control.InteractiveInstanceManager
import androidx.wear.watchface.control.data.CrashInfoParcel
import androidx.wear.watchface.control.data.WallpaperInteractiveWatchFaceInstanceParams
import androidx.wear.watchface.data.DeviceConfig
import androidx.wear.watchface.data.WatchUiState
import androidx.wear.watchface.style.CurrentUserStyleRepository
import androidx.wear.watchface.style.UserStyle
import androidx.wear.watchface.style.UserStyleSetting
import androidx.wear.watchface.style.UserStyleSetting.BooleanUserStyleSetting.BooleanOption
import androidx.wear.watchface.style.UserStyleSetting.DoubleRangeUserStyleSetting.DoubleRangeOption
import androidx.wear.watchface.style.UserStyleSetting.LongRangeUserStyleSetting.LongRangeOption
import androidx.wear.watchface.style.data.UserStyleWireFormat
import com.google.common.truth.Truth.assertThat
import java.time.ZonedDateTime
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert
import org.junit.Assume
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.junit.MockitoJUnit

private const val BITMAP_WIDTH = 400
private const val BITMAP_HEIGHT = 400
private const val TIMEOUT_MS = 800L

private const val INTERACTIVE_INSTANCE_ID = "InteractiveTestInstance"

class TestXmlWatchFaceService(
    testContext: Context,
    private var surfaceHolderOverride: SurfaceHolder
) : WatchFaceService() {
    init {
        attachBaseContext(testContext)
    }

    override fun getXmlWatchFaceResourceId() = R.xml.xml_watchface

    override fun getWallpaperSurfaceHolderOverride() = surfaceHolderOverride

    override fun getComplicationSlotInflationFactory(
        currentUserStyleRepository: CurrentUserStyleRepository
    ) =
        object : ComplicationSlotInflationFactory() {
            override fun getCanvasComplicationFactory(slotId: Int): CanvasComplicationFactory {
                return CanvasComplicationFactory { _, _ ->
                    object : CanvasComplication {
                        override fun render(
                            canvas: Canvas,
                            bounds: Rect,
                            zonedDateTime: ZonedDateTime,
                            renderParameters: RenderParameters,
                            slotId: Int
                        ) {}

                        override fun drawHighlight(
                            canvas: Canvas,
                            bounds: Rect,
                            boundsType: Int,
                            zonedDateTime: ZonedDateTime,
                            color: Int
                        ) {}

                        override fun getData() = NoDataComplicationData()

                        override fun loadData(
                            complicationData: ComplicationData,
                            loadDrawablesAsynchronous: Boolean
                        ) {}
                    }
                }
            }
        }

    override suspend fun createWatchFace(
        surfaceHolder: SurfaceHolder,
        watchState: WatchState,
        complicationSlotsManager: ComplicationSlotsManager,
        currentUserStyleRepository: CurrentUserStyleRepository
    ) =
        WatchFace(
            WatchFaceType.DIGITAL,
            @Suppress("deprecation")
            object :
                Renderer.CanvasRenderer(
                    surfaceHolder,
                    currentUserStyleRepository,
                    watchState,
                    CanvasType.HARDWARE,
                    16L
                ) {
                override fun render(canvas: Canvas, bounds: Rect, zonedDateTime: ZonedDateTime) {}

                override fun renderHighlightLayer(
                    canvas: Canvas,
                    bounds: Rect,
                    zonedDateTime: ZonedDateTime
                ) {}
            }
        )
}

class TestState(val value: Int)

class TestStatefulXmlWatchFaceService(
    testContext: Context,
    private var surfaceHolderOverride: SurfaceHolder
) : StatefulWatchFaceService<TestState>() {
    init {
        attachBaseContext(testContext)
    }

    override fun createExtra() = TestState(123)

    override fun getXmlWatchFaceResourceId() = R.xml.xml_watchface

    override fun getWallpaperSurfaceHolderOverride() = surfaceHolderOverride

    class TestCanvasComplicationFactory(val extra: TestState) : CanvasComplicationFactory {
        override fun create(
            watchState: WatchState,
            invalidateCallback: CanvasComplication.InvalidateCallback
        ): CanvasComplication =
            object : CanvasComplication {
                override fun render(
                    canvas: Canvas,
                    bounds: Rect,
                    zonedDateTime: ZonedDateTime,
                    renderParameters: RenderParameters,
                    slotId: Int
                ) {}

                override fun drawHighlight(
                    canvas: Canvas,
                    bounds: Rect,
                    boundsType: Int,
                    zonedDateTime: ZonedDateTime,
                    color: Int
                ) {}

                override fun getData() = NoDataComplicationData()

                override fun loadData(
                    complicationData: ComplicationData,
                    loadDrawablesAsynchronous: Boolean
                ) {}
            }
    }

    override fun getComplicationSlotInflationFactory(
        currentUserStyleRepository: CurrentUserStyleRepository,
        extra: TestState
    ) =
        object : ComplicationSlotInflationFactory() {
            override fun getCanvasComplicationFactory(slotId: Int): CanvasComplicationFactory {
                return TestCanvasComplicationFactory(extra)
            }
        }

    override suspend fun createWatchFace(
        surfaceHolder: SurfaceHolder,
        watchState: WatchState,
        complicationSlotsManager: ComplicationSlotsManager,
        currentUserStyleRepository: CurrentUserStyleRepository,
        extra: TestState
    ) =
        WatchFace(
            WatchFaceType.DIGITAL,
            @Suppress("deprecation")
            object :
                Renderer.CanvasRenderer(
                    surfaceHolder,
                    currentUserStyleRepository,
                    watchState,
                    CanvasType.HARDWARE,
                    16L
                ) {
                override fun render(canvas: Canvas, bounds: Rect, zonedDateTime: ZonedDateTime) {}

                override fun renderHighlightLayer(
                    canvas: Canvas,
                    bounds: Rect,
                    zonedDateTime: ZonedDateTime
                ) {}
            }
        )
}

@RunWith(AndroidJUnit4::class)
@MediumTest
public class XmlDefinedUserStyleSchemaAndComplicationSlotsTest {

    @get:Rule val mocks = MockitoJUnit.rule()

    @Mock private lateinit var surfaceHolder: SurfaceHolder
    @Mock private lateinit var surface: Surface

    private val bitmap = Bitmap.createBitmap(BITMAP_WIDTH, BITMAP_HEIGHT, Bitmap.Config.ARGB_8888)
    private val canvas = Canvas(bitmap)
    private var initLatch = CountDownLatch(1)
    private lateinit var interactiveWatchFaceInstance: IInteractiveWatchFace

    @Before
    public fun setUp() {
        Assume.assumeTrue("This test suite assumes API 29", Build.VERSION.SDK_INT >= 29)
    }

    @After
    public fun tearDown() {
        InteractiveInstanceManager.setParameterlessEngine(null)
        if (this::interactiveWatchFaceInstance.isInitialized) {
            interactiveWatchFaceInstance.release()
        }
    }

    private fun setPendingWallpaperInteractiveWatchFaceInstance() {
        val existingInstance =
            InteractiveInstanceManager
                .getExistingInstanceOrSetPendingWallpaperInteractiveWatchFaceInstance(
                    InteractiveInstanceManager.PendingWallpaperInteractiveWatchFaceInstance(
                        WallpaperInteractiveWatchFaceInstanceParams(
                            INTERACTIVE_INSTANCE_ID,
                            DeviceConfig(false, false, 0, 0),
                            WatchUiState(false, 0),
                            UserStyleWireFormat(emptyMap()),
                            null,
                            null,
                            null
                        ),
                        object : IPendingInteractiveWatchFace.Stub() {
                            override fun getApiVersion() = IPendingInteractiveWatchFace.API_VERSION

                            override fun onInteractiveWatchFaceCreated(
                                iInteractiveWatchFace: IInteractiveWatchFace
                            ) {
                                interactiveWatchFaceInstance = iInteractiveWatchFace
                                initLatch.countDown()
                            }

                            override fun onInteractiveWatchFaceCrashed(
                                exception: CrashInfoParcel?
                            ) {
                                Assert.fail("WatchFace crashed: $exception")
                            }
                        }
                    )
                )
        assertThat(existingInstance).isNull()
    }

    @Test
    @Suppress("Deprecation", "NewApi") // userStyleSettings
    public fun staticSchemaAndComplicationsRead() {
        val service =
            TestXmlWatchFaceService(
                ApplicationProvider.getApplicationContext<Context>(),
                surfaceHolder
            )

        Mockito.`when`(surfaceHolder.surfaceFrame)
            .thenReturn(Rect(0, 0, BITMAP_WIDTH, BITMAP_HEIGHT))
        Mockito.`when`(surfaceHolder.lockHardwareCanvas()).thenReturn(canvas)
        Mockito.`when`(surfaceHolder.surface).thenReturn(surface)
        Mockito.`when`(surface.isValid).thenReturn(false)

        setPendingWallpaperInteractiveWatchFaceInstance()

        val wrapper = service.onCreateEngine() as WatchFaceService.EngineWrapper
        assertThat(initLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS)).isTrue()

        runBlocking {
            val watchFaceImpl = wrapper.watchFaceDetails!!.deferredWatchFaceImpl.await()
            val schema = watchFaceImpl.currentUserStyleRepository.schema
            assertThat(schema.userStyleSettings.map { it.id.value })
                .containsExactly("TimeStyle", "BooleanId", "DoubleId", "LongId")

            assertThat(watchFaceImpl.complicationSlotsManager.complicationSlots.size).isEqualTo(5)

            val slotA = watchFaceImpl.complicationSlotsManager.complicationSlots[10]!!
            assertThat(slotA.boundsType).isEqualTo(ComplicationSlotBoundsType.ROUND_RECT)
            assertThat(slotA.supportedTypes)
                .containsExactly(
                    ComplicationType.RANGED_VALUE,
                    ComplicationType.SHORT_TEXT,
                    ComplicationType.SMALL_IMAGE
                )
                .inOrder()
            assertThat(slotA.defaultDataSourcePolicy.primaryDataSource).isNull()
            assertThat(slotA.defaultDataSourcePolicy.primaryDataSourceDefaultType).isNull()
            assertThat(slotA.defaultDataSourcePolicy.secondaryDataSource).isNull()
            assertThat(slotA.defaultDataSourcePolicy.secondaryDataSourceDefaultType).isNull()
            assertThat(slotA.defaultDataSourcePolicy.systemDataSourceFallback)
                .isEqualTo(SystemDataSources.DATA_SOURCE_WATCH_BATTERY)
            assertThat(slotA.defaultDataSourcePolicy.systemDataSourceFallbackDefaultType)
                .isEqualTo(ComplicationType.RANGED_VALUE)
            assertThat(
                    slotA.complicationSlotBounds.perComplicationTypeBounds[
                            ComplicationType.SHORT_TEXT]!!
                )
                .isEqualTo(RectF(0.3f, 0.7f, 0.7f, 0.9f))
            assertThat(slotA.nameResourceId).isEqualTo(R.string.complication_name_one)
            assertThat(slotA.screenReaderNameResourceId)
                .isEqualTo(R.string.complication_screen_reader_name_one)

            val slotB = watchFaceImpl.complicationSlotsManager.complicationSlots[20]!!
            assertThat(slotB.boundsType).isEqualTo(ComplicationSlotBoundsType.BACKGROUND)
            assertThat(slotB.supportedTypes)
                .containsExactly(ComplicationType.LONG_TEXT, ComplicationType.SHORT_TEXT)
                .inOrder()
            assertThat(slotB.defaultDataSourcePolicy.primaryDataSource)
                .isEqualTo(ComponentName("com.package", "com.app"))
            assertThat(slotB.defaultDataSourcePolicy.primaryDataSourceDefaultType)
                .isEqualTo(ComplicationType.SHORT_TEXT)
            assertThat(slotB.defaultDataSourcePolicy.secondaryDataSource).isNull()
            assertThat(slotB.defaultDataSourcePolicy.secondaryDataSourceDefaultType).isNull()
            assertThat(slotB.defaultDataSourcePolicy.systemDataSourceFallback)
                .isEqualTo(SystemDataSources.DATA_SOURCE_SUNRISE_SUNSET)
            assertThat(slotB.defaultDataSourcePolicy.systemDataSourceFallbackDefaultType)
                .isEqualTo(ComplicationType.LONG_TEXT)
            assertThat(
                    slotB.complicationSlotBounds.perComplicationTypeBounds[
                            ComplicationType.SHORT_TEXT]!!
                )
                .isEqualTo(RectF(0.1f, 0.2f, 0.3f, 0.4f))
            assertThat(slotB.nameResourceId).isEqualTo(R.string.complication_name_two)
            assertThat(slotB.screenReaderNameResourceId)
                .isEqualTo(R.string.complication_screen_reader_name_two)

            val slotC = watchFaceImpl.complicationSlotsManager.complicationSlots[30]!!
            assertThat(slotC.defaultDataSourcePolicy.primaryDataSource).isNull()
            assertThat(slotC.defaultDataSourcePolicy.primaryDataSourceDefaultType).isNull()
            assertThat(slotC.defaultDataSourcePolicy.secondaryDataSource).isNull()
            assertThat(slotC.defaultDataSourcePolicy.secondaryDataSourceDefaultType).isNull()
            assertThat(slotC.defaultDataSourcePolicy.systemDataSourceFallback)
                .isEqualTo(SystemDataSources.NO_DATA_SOURCE)
            assertThat(slotC.defaultDataSourcePolicy.systemDataSourceFallbackDefaultType)
                .isEqualTo(ComplicationType.NOT_CONFIGURED)

            val slotD = watchFaceImpl.complicationSlotsManager.complicationSlots[40]!!
            assertThat(slotD.supportedTypes)
                .containsExactly(
                    ComplicationType.SHORT_TEXT,
                    ComplicationType.RANGED_VALUE,
                    ComplicationType.SMALL_IMAGE
                )
                .inOrder()
            assertThat(slotD.defaultDataSourcePolicy.primaryDataSource)
                .isEqualTo(ComponentName("com.package", "com.app.example1"))
            assertThat(slotD.defaultDataSourcePolicy.primaryDataSourceDefaultType)
                .isEqualTo(ComplicationType.SHORT_TEXT)
            assertThat(slotD.defaultDataSourcePolicy.secondaryDataSource)
                .isEqualTo(ComponentName("com.package", "com.app.example2"))
            assertThat(slotD.defaultDataSourcePolicy.secondaryDataSourceDefaultType)
                .isEqualTo(ComplicationType.SMALL_IMAGE)
            assertThat(slotD.defaultDataSourcePolicy.systemDataSourceFallback)
                .isEqualTo(SystemDataSources.DATA_SOURCE_WATCH_BATTERY)
            assertThat(slotD.defaultDataSourcePolicy.systemDataSourceFallbackDefaultType)
                .isEqualTo(ComplicationType.RANGED_VALUE)

            val slotE = watchFaceImpl.complicationSlotsManager.complicationSlots[50]!!
            assertThat(slotE.supportedTypes)
                .containsExactly(ComplicationType.GOAL_PROGRESS, ComplicationType.WEIGHTED_ELEMENTS)
                .inOrder()
            assertThat(slotE.defaultDataSourcePolicy.primaryDataSource)
                .isEqualTo(ComponentName("com.package", "com.app"))
            assertThat(slotE.defaultDataSourcePolicy.primaryDataSourceDefaultType)
                .isEqualTo(ComplicationType.GOAL_PROGRESS)
            assertThat(slotE.defaultDataSourcePolicy.systemDataSourceFallback)
                .isEqualTo(SystemDataSources.DATA_SOURCE_WATCH_BATTERY)
            assertThat(slotE.defaultDataSourcePolicy.systemDataSourceFallbackDefaultType)
                .isEqualTo(ComplicationType.WEIGHTED_ELEMENTS)

            val earlyInitDetails = wrapper.watchFaceDetails!!.deferredEarlyInitDetails.await()
            val flavors = earlyInitDetails.userStyleFlavors

            assertThat(flavors.flavors.size).isEqualTo(1)
            val flavor = flavors.flavors[0]
            assertThat(flavor.id).isEqualTo("flavor1")

            val style = UserStyle(flavor.style, schema)
            assertThat(style.size).isEqualTo(4)
            assertThat(style[UserStyleSetting.Id("TimeStyle")]!!.id)
                .isEqualTo(UserStyleSetting.Option.Id("minimal"))
            assertThat((style[UserStyleSetting.Id("BooleanId")]!! as BooleanOption).value)
                .isEqualTo(false)
            assertThat((style[UserStyleSetting.Id("DoubleId")]!! as DoubleRangeOption).value)
                .isEqualTo(1.0)
            assertThat((style[UserStyleSetting.Id("LongId")]!! as LongRangeOption).value)
                .isEqualTo(2)

            val complications = flavor.complications
            assertThat(complications.size).isEqualTo(1)
            val complicationPolicy = complications[10]!!
            assertThat(complicationPolicy.primaryDataSource)
                .isEqualTo(ComponentName("com.package", "com.app"))
            assertThat(complicationPolicy.primaryDataSourceDefaultType)
                .isEqualTo(ComplicationType.SHORT_TEXT)
            assertThat(complicationPolicy.systemDataSourceFallback)
                .isEqualTo(SystemDataSources.DATA_SOURCE_DAY_AND_DATE)
            assertThat(complicationPolicy.systemDataSourceFallbackDefaultType)
                .isEqualTo(ComplicationType.SHORT_TEXT)

            var fixedString = flavor.toString()

            // remove binary data from option values
            val booleanIndex = fixedString.indexOf("BooleanId=") + "BooleanId=".length
            fixedString =
                fixedString.removeRange(booleanIndex, fixedString.indexOf(',', booleanIndex))

            val doubleIndex = fixedString.indexOf("DoubleId=") + "DoubleId=".length
            fixedString =
                fixedString.removeRange(doubleIndex, fixedString.indexOf(',', doubleIndex))

            val longIndex = fixedString.indexOf("LongId=") + "LongId=".length
            fixedString = fixedString.removeRange(longIndex, fixedString.indexOf('}', longIndex))

            assertThat(fixedString)
                .isEqualTo(
                    "UserStyleFlavor[flavor1: " +
                        "{BooleanId=, TimeStyle=minimal, DoubleId=, LongId=}, " +
                        "{10=DefaultComplicationDataSourcePolicy[" +
                        "primary(ComponentInfo{com.package/com.app}, SHORT_TEXT), " +
                        "secondary(null, null), " +
                        "system(16, SHORT_TEXT)]}]"
                )
        }
    }

    @Test
    @Suppress("Deprecation", "NewApi") // userStyleSettings
    public fun testStatefulXmlWatchFaceService() {
        val service =
            TestStatefulXmlWatchFaceService(
                ApplicationProvider.getApplicationContext<Context>(),
                surfaceHolder
            )

        Mockito.`when`(surfaceHolder.surfaceFrame)
            .thenReturn(Rect(0, 0, BITMAP_WIDTH, BITMAP_HEIGHT))
        Mockito.`when`(surfaceHolder.lockHardwareCanvas()).thenReturn(canvas)
        Mockito.`when`(surfaceHolder.surface).thenReturn(surface)
        Mockito.`when`(surface.isValid).thenReturn(false)

        setPendingWallpaperInteractiveWatchFaceInstance()

        val wrapper = service.onCreateEngine() as WatchFaceService.EngineWrapper
        assertThat(initLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS)).isTrue()

        runBlocking {
            val watchFaceImpl = wrapper.watchFaceDetails!!.deferredWatchFaceImpl.await()
            val factory =
                watchFaceImpl.complicationSlotsManager.complicationSlots[10]!!
                    .canvasComplicationFactory
                    as TestStatefulXmlWatchFaceService.TestCanvasComplicationFactory

            // Assert the extra was passed to the TestCanvasComplicationFactory.
            assertThat(factory.extra.value).isEqualTo(123)
        }
    }
}
