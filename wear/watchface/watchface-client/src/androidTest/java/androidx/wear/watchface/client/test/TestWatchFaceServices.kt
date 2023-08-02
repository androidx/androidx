/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.wear.watchface.client.test

import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Rect
import android.graphics.RectF
import android.view.SurfaceHolder
import androidx.annotation.Px
import androidx.wear.watchface.BoundingArc
import androidx.wear.watchface.CanvasComplication
import androidx.wear.watchface.CanvasType
import androidx.wear.watchface.ComplicationSlot
import androidx.wear.watchface.ComplicationSlotsManager
import androidx.wear.watchface.ComplicationTapFilter
import androidx.wear.watchface.RenderParameters
import androidx.wear.watchface.Renderer
import androidx.wear.watchface.WatchFace
import androidx.wear.watchface.WatchFaceRuntimeService
import androidx.wear.watchface.WatchFaceService
import androidx.wear.watchface.WatchFaceType
import androidx.wear.watchface.WatchState
import androidx.wear.watchface.complications.ComplicationSlotBounds
import androidx.wear.watchface.complications.DefaultComplicationDataSourcePolicy
import androidx.wear.watchface.complications.SystemDataSources
import androidx.wear.watchface.complications.data.ComplicationData
import androidx.wear.watchface.complications.data.ComplicationExperimental
import androidx.wear.watchface.complications.data.ComplicationText
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.data.NoDataComplicationData
import androidx.wear.watchface.complications.data.PlainComplicationText
import androidx.wear.watchface.complications.data.ShortTextComplicationData
import androidx.wear.watchface.samples.ExampleCanvasAnalogWatchFaceService
import androidx.wear.watchface.samples.ExampleCanvasAnalogWatchFaceService.Companion.COMPLICATIONS_STYLE_SETTING
import androidx.wear.watchface.samples.ExampleCanvasAnalogWatchFaceService.Companion.LEFT_COMPLICATION
import androidx.wear.watchface.samples.ExampleCanvasAnalogWatchFaceService.Companion.NO_COMPLICATIONS
import androidx.wear.watchface.samples.ExampleOpenGLBackgroundInitWatchFaceService
import androidx.wear.watchface.samples.R
import androidx.wear.watchface.style.CurrentUserStyleRepository
import androidx.wear.watchface.style.UserStyleFlavors
import androidx.wear.watchface.style.UserStyleSchema
import androidx.wear.watchface.style.UserStyleSetting
import androidx.wear.watchface.style.WatchFaceLayer
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.concurrent.CountDownLatch
import kotlinx.coroutines.CompletableDeferred

internal class TestLifeCycleWatchFaceService : WatchFaceService() {
    companion object {
        val lifeCycleEvents = ArrayList<String>()
    }

    override fun onCreate() {
        super.onCreate()
        lifeCycleEvents.add("WatchFaceService.onCreate")
    }

    override fun onDestroy() {
        super.onDestroy()
        lifeCycleEvents.add("WatchFaceService.onDestroy")
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
                Renderer.GlesRenderer(surfaceHolder, currentUserStyleRepository, watchState, 16) {
                init {
                    lifeCycleEvents.add("Renderer.constructed")
                }

                override fun onDestroy() {
                    super.onDestroy()
                    lifeCycleEvents.add("Renderer.onDestroy")
                }

                override fun render(zonedDateTime: ZonedDateTime) {}

                override fun renderHighlightLayer(zonedDateTime: ZonedDateTime) {}
            }
        )
}

internal class TestExampleCanvasAnalogWatchFaceService(
    testContext: Context,
    private var surfaceHolderOverride: SurfaceHolder
) : ExampleCanvasAnalogWatchFaceService() {
    internal lateinit var watchFace: WatchFace

    init {
        attachBaseContext(testContext)
    }

    override fun getWallpaperSurfaceHolderOverride() = surfaceHolderOverride

    override suspend fun createWatchFace(
        surfaceHolder: SurfaceHolder,
        watchState: WatchState,
        complicationSlotsManager: ComplicationSlotsManager,
        currentUserStyleRepository: CurrentUserStyleRepository
    ): WatchFace {
        watchFace =
            super.createWatchFace(
                surfaceHolder,
                watchState,
                complicationSlotsManager,
                currentUserStyleRepository
            )
        return watchFace
    }

    companion object {
        var systemTimeMillis = 1000000000L
    }

    override fun getSystemTimeProvider() =
        object : SystemTimeProvider {
            override fun getSystemTimeMillis() = systemTimeMillis

            override fun getSystemTimeZoneId() = ZoneId.of("UTC")
        }
}

internal class TestExampleOpenGLBackgroundInitWatchFaceService(
    testContext: Context,
    private var surfaceHolderOverride: SurfaceHolder
) : ExampleOpenGLBackgroundInitWatchFaceService() {
    internal lateinit var watchFace: WatchFace

    init {
        attachBaseContext(testContext)
    }

    override fun getWallpaperSurfaceHolderOverride() = surfaceHolderOverride

    override suspend fun createWatchFace(
        surfaceHolder: SurfaceHolder,
        watchState: WatchState,
        complicationSlotsManager: ComplicationSlotsManager,
        currentUserStyleRepository: CurrentUserStyleRepository
    ): WatchFace {
        watchFace =
            super.createWatchFace(
                surfaceHolder,
                watchState,
                complicationSlotsManager,
                currentUserStyleRepository
            )
        return watchFace
    }
}

internal open class TestCrashingWatchFaceService : WatchFaceService() {

    companion object {
        const val COMPLICATION_ID = 123
    }

    @OptIn(ComplicationExperimental::class)
    override fun createComplicationSlotsManager(
        currentUserStyleRepository: CurrentUserStyleRepository
    ): ComplicationSlotsManager {
        return ComplicationSlotsManager(
            listOf(
                ComplicationSlot.createRoundRectComplicationSlotBuilder(
                        COMPLICATION_ID,
                        { _, _ -> throw Exception("Deliberately crashing") },
                        listOf(ComplicationType.LONG_TEXT),
                        DefaultComplicationDataSourcePolicy(
                            SystemDataSources.DATA_SOURCE_SUNRISE_SUNSET,
                            ComplicationType.LONG_TEXT
                        ),
                        ComplicationSlotBounds(RectF(0.1f, 0.1f, 0.4f, 0.4f))
                    )
                    .build()
            ),
            currentUserStyleRepository
        )
    }

    override suspend fun createWatchFace(
        surfaceHolder: SurfaceHolder,
        watchState: WatchState,
        complicationSlotsManager: ComplicationSlotsManager,
        currentUserStyleRepository: CurrentUserStyleRepository
    ): WatchFace {
        throw Exception("Deliberately crashing")
    }
}

@Suppress("Deprecation")
internal class TestWatchfaceOverlayStyleWatchFaceService(
    testContext: Context,
    private var surfaceHolderOverride: SurfaceHolder,
    private var watchFaceOverlayStyle: WatchFace.OverlayStyle
) : WatchFaceService() {

    init {
        attachBaseContext(testContext)
    }

    override fun getWallpaperSurfaceHolderOverride() = surfaceHolderOverride

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
                        16
                    ) {
                    override fun render(
                        canvas: Canvas,
                        bounds: Rect,
                        zonedDateTime: ZonedDateTime
                    ) {
                        // Actually rendering something isn't required.
                    }

                    override fun renderHighlightLayer(
                        canvas: Canvas,
                        bounds: Rect,
                        zonedDateTime: ZonedDateTime
                    ) {
                        // Actually rendering something isn't required.
                    }
                }
            )
            .setOverlayStyle(watchFaceOverlayStyle)
}

@Suppress("Deprecation")
internal class TestWatchFaceRuntimeService(
    testContext: Context,
    private var surfaceHolderOverride: SurfaceHolder
) : WatchFaceRuntimeService() {

    lateinit var lastResourceOnlyWatchFacePackageName: String
    val lastResourceOnlyWatchFacePackageNameLatch = CountDownLatch(1)

    init {
        attachBaseContext(testContext)
    }

    override fun getWallpaperSurfaceHolderOverride() = surfaceHolderOverride

    override fun createUserStyleSchema(resourceOnlyWatchFacePackageName: String) =
        UserStyleSchema(emptyList())

    override fun createComplicationSlotsManager(
        currentUserStyleRepository: CurrentUserStyleRepository,
        resourceOnlyWatchFacePackageName: String
    ) = ComplicationSlotsManager(emptyList(), currentUserStyleRepository)

    override fun createUserStyleFlavors(
        currentUserStyleRepository: CurrentUserStyleRepository,
        complicationSlotsManager: ComplicationSlotsManager,
        resourceOnlyWatchFacePackageName: String
    ) = UserStyleFlavors()

    override suspend fun createWatchFace(
        surfaceHolder: SurfaceHolder,
        watchState: WatchState,
        complicationSlotsManager: ComplicationSlotsManager,
        currentUserStyleRepository: CurrentUserStyleRepository,
        resourceOnlyWatchFacePackageName: String
    ): WatchFace {
        lastResourceOnlyWatchFacePackageName = resourceOnlyWatchFacePackageName
        lastResourceOnlyWatchFacePackageNameLatch.countDown()

        return WatchFace(
            WatchFaceType.DIGITAL,
            @Suppress("deprecation")
            object :
                Renderer.CanvasRenderer(
                    surfaceHolder,
                    currentUserStyleRepository,
                    watchState,
                    CanvasType.HARDWARE,
                    16
                ) {
                override fun render(
                    canvas: Canvas,
                    bounds: Rect,
                    zonedDateTime: ZonedDateTime
                ) {
                    // Actually rendering something isn't required.
                }

                override fun renderHighlightLayer(
                    canvas: Canvas,
                    bounds: Rect,
                    zonedDateTime: ZonedDateTime
                ) {
                    // Actually rendering something isn't required.
                }
            }
        )
    }
}

internal class TestAsyncCanvasRenderInitWatchFaceService(
    testContext: Context,
    private var surfaceHolderOverride: SurfaceHolder,
    private var initCompletableDeferred: CompletableDeferred<Unit>
) : WatchFaceService() {

    init {
        attachBaseContext(testContext)
    }

    override fun getWallpaperSurfaceHolderOverride() = surfaceHolderOverride

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
                    16
                ) {
                override suspend fun init() {
                    initCompletableDeferred.await()
                }

                override fun render(canvas: Canvas, bounds: Rect, zonedDateTime: ZonedDateTime) {
                    // Actually rendering something isn't required.
                }

                override fun renderHighlightLayer(
                    canvas: Canvas,
                    bounds: Rect,
                    zonedDateTime: ZonedDateTime
                ) {
                    TODO("Not yet implemented")
                }
            }
        )

    override fun getSystemTimeProvider() =
        object : SystemTimeProvider {
            override fun getSystemTimeMillis() = 123456789L

            override fun getSystemTimeZoneId() = ZoneId.of("UTC")
        }
}

internal class TestAsyncGlesRenderInitWatchFaceService(
    testContext: Context,
    private var surfaceHolderOverride: SurfaceHolder,
    private var onUiThreadGlSurfaceCreatedCompletableDeferred: CompletableDeferred<Unit>,
    private var onBackgroundThreadGlContextCreatedCompletableDeferred: CompletableDeferred<Unit>
) : WatchFaceService() {
    internal lateinit var watchFace: WatchFace

    init {
        attachBaseContext(testContext)
    }

    override fun getWallpaperSurfaceHolderOverride() = surfaceHolderOverride

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
                Renderer.GlesRenderer(surfaceHolder, currentUserStyleRepository, watchState, 16) {
                override suspend fun onUiThreadGlSurfaceCreated(width: Int, height: Int) {
                    onUiThreadGlSurfaceCreatedCompletableDeferred.await()
                }

                override suspend fun onBackgroundThreadGlContextCreated() {
                    onBackgroundThreadGlContextCreatedCompletableDeferred.await()
                }

                override fun render(zonedDateTime: ZonedDateTime) {
                    // GLES rendering is complicated and not strictly necessary for our test.
                }

                override fun renderHighlightLayer(zonedDateTime: ZonedDateTime) {
                    TODO("Not yet implemented")
                }
            }
        )
}

internal class TestComplicationProviderDefaultsWatchFaceService(
    testContext: Context,
    private var surfaceHolderOverride: SurfaceHolder
) : WatchFaceService() {

    init {
        attachBaseContext(testContext)
    }

    override fun getWallpaperSurfaceHolderOverride() = surfaceHolderOverride

    @OptIn(ComplicationExperimental::class)
    override fun createComplicationSlotsManager(
        currentUserStyleRepository: CurrentUserStyleRepository
    ): ComplicationSlotsManager {
        return ComplicationSlotsManager(
            listOf(
                ComplicationSlot.createRoundRectComplicationSlotBuilder(
                        123,
                        { _, _ ->
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
                        },
                        listOf(
                            ComplicationType.PHOTO_IMAGE,
                            ComplicationType.LONG_TEXT,
                            ComplicationType.SHORT_TEXT
                        ),
                        DefaultComplicationDataSourcePolicy(
                            ComponentName("com.package1", "com.app1"),
                            ComplicationType.PHOTO_IMAGE,
                            ComponentName("com.package2", "com.app2"),
                            ComplicationType.LONG_TEXT,
                            SystemDataSources.DATA_SOURCE_STEP_COUNT,
                            ComplicationType.SHORT_TEXT
                        ),
                        ComplicationSlotBounds(RectF(0.1f, 0.2f, 0.3f, 0.4f))
                    )
                    .build()
            ),
            currentUserStyleRepository
        )
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
                    16
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

internal class TestEdgeComplicationWatchFaceService(
    testContext: Context,
    private var surfaceHolderOverride: SurfaceHolder
) : WatchFaceService() {

    init {
        attachBaseContext(testContext)
    }

    override fun getWallpaperSurfaceHolderOverride() = surfaceHolderOverride

    @OptIn(ComplicationExperimental::class)
    override fun createComplicationSlotsManager(
        currentUserStyleRepository: CurrentUserStyleRepository
    ): ComplicationSlotsManager {
        return ComplicationSlotsManager(
            listOf(
                ComplicationSlot.createEdgeComplicationSlotBuilder(
                        123,
                        { _, _ ->
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
                        },
                        listOf(
                            ComplicationType.PHOTO_IMAGE,
                            ComplicationType.LONG_TEXT,
                            ComplicationType.SHORT_TEXT
                        ),
                        DefaultComplicationDataSourcePolicy(
                            ComponentName("com.package1", "com.app1"),
                            ComplicationType.PHOTO_IMAGE,
                            ComponentName("com.package2", "com.app2"),
                            ComplicationType.LONG_TEXT,
                            SystemDataSources.DATA_SOURCE_STEP_COUNT,
                            ComplicationType.SHORT_TEXT
                        ),
                        ComplicationSlotBounds(RectF(0f, 0f, 1f, 1f)),
                        BoundingArc(45f, 90f, 0.1f)
                    )
                    .build()
            ),
            currentUserStyleRepository
        )
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
                    16
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

internal class TestWatchFaceServiceWithPreviewImageUpdateRequest(
    testContext: Context,
    private var surfaceHolderOverride: SurfaceHolder,
) : WatchFaceService() {
    val rendererInitializedLatch = CountDownLatch(1)

    init {
        attachBaseContext(testContext)
    }

    override fun getWallpaperSurfaceHolderOverride() = surfaceHolderOverride

    @Suppress("deprecation") private lateinit var renderer: Renderer.CanvasRenderer

    fun triggerPreviewImageUpdateRequest() {
        renderer.sendPreviewImageNeedsUpdateRequest()
    }

    override suspend fun createWatchFace(
        surfaceHolder: SurfaceHolder,
        watchState: WatchState,
        complicationSlotsManager: ComplicationSlotsManager,
        currentUserStyleRepository: CurrentUserStyleRepository
    ): WatchFace {
        @Suppress("deprecation")
        renderer =
            object :
                Renderer.CanvasRenderer(
                    surfaceHolder,
                    currentUserStyleRepository,
                    watchState,
                    CanvasType.HARDWARE,
                    16
                ) {
                override suspend fun init() {
                    rendererInitializedLatch.countDown()
                }

                override fun render(canvas: Canvas, bounds: Rect, zonedDateTime: ZonedDateTime) {}

                override fun renderHighlightLayer(
                    canvas: Canvas,
                    bounds: Rect,
                    zonedDateTime: ZonedDateTime
                ) {}
            }
        return WatchFace(WatchFaceType.DIGITAL, renderer)
    }
}

internal class TestComplicationStyleUpdateWatchFaceService(
    testContext: Context,
    private var surfaceHolderOverride: SurfaceHolder
) : WatchFaceService() {

    init {
        attachBaseContext(testContext)
    }

    @Suppress("deprecation")
    private val complicationsStyleSetting =
        UserStyleSetting.ComplicationSlotsUserStyleSetting(
            UserStyleSetting.Id(COMPLICATIONS_STYLE_SETTING),
            resources,
            R.string.watchface_complications_setting,
            R.string.watchface_complications_setting_description,
            icon = null,
            complicationConfig =
                listOf(
                    UserStyleSetting.ComplicationSlotsUserStyleSetting.ComplicationSlotsOption(
                        UserStyleSetting.Option.Id(NO_COMPLICATIONS),
                        resources,
                        R.string.watchface_complications_setting_none,
                        null,
                        listOf(
                            UserStyleSetting.ComplicationSlotsUserStyleSetting
                                .ComplicationSlotOverlay(123, enabled = false)
                        )
                    ),
                    UserStyleSetting.ComplicationSlotsUserStyleSetting.ComplicationSlotsOption(
                        UserStyleSetting.Option.Id(LEFT_COMPLICATION),
                        resources,
                        R.string.watchface_complications_setting_left,
                        null,
                        listOf(
                            UserStyleSetting.ComplicationSlotsUserStyleSetting
                                .ComplicationSlotOverlay(
                                    123,
                                    enabled = true,
                                    nameResourceId = R.string.left_complication_screen_name,
                                    screenReaderNameResourceId =
                                        R.string.left_complication_screen_reader_name
                                )
                        )
                    )
                ),
            listOf(WatchFaceLayer.COMPLICATIONS)
        )

    override fun createUserStyleSchema(): UserStyleSchema =
        UserStyleSchema(listOf(complicationsStyleSetting))

    override fun getWallpaperSurfaceHolderOverride() = surfaceHolderOverride

    @OptIn(ComplicationExperimental::class)
    override fun createComplicationSlotsManager(
        currentUserStyleRepository: CurrentUserStyleRepository
    ): ComplicationSlotsManager {
        return ComplicationSlotsManager(
            listOf(
                ComplicationSlot.createRoundRectComplicationSlotBuilder(
                        123,
                        { _, _ ->
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
                        },
                        listOf(
                            ComplicationType.PHOTO_IMAGE,
                            ComplicationType.LONG_TEXT,
                            ComplicationType.SHORT_TEXT
                        ),
                        DefaultComplicationDataSourcePolicy(
                            ComponentName("com.package1", "com.app1"),
                            ComplicationType.PHOTO_IMAGE,
                            ComponentName("com.package2", "com.app2"),
                            ComplicationType.LONG_TEXT,
                            SystemDataSources.DATA_SOURCE_STEP_COUNT,
                            ComplicationType.SHORT_TEXT
                        ),
                        ComplicationSlotBounds(RectF(0.1f, 0.2f, 0.3f, 0.4f))
                    )
                    .build()
            ),
            currentUserStyleRepository
        )
    }

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
                    CanvasType.HARDWARE,
                    16
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

internal class TestCustomTapFilterWatchFaceService(
    testContext: Context,
    private var surfaceHolderOverride: SurfaceHolder
) : WatchFaceService() {

    init {
        attachBaseContext(testContext)
    }

    override fun getWallpaperSurfaceHolderOverride() = surfaceHolderOverride

    @OptIn(ComplicationExperimental::class)
    override fun createComplicationSlotsManager(
        currentUserStyleRepository: CurrentUserStyleRepository
    ): ComplicationSlotsManager {
        return ComplicationSlotsManager(
            listOf(
                ComplicationSlot.createEdgeComplicationSlotBuilder(
                        123,
                        { _, _ ->
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
                        },
                        listOf(
                            ComplicationType.PHOTO_IMAGE,
                            ComplicationType.LONG_TEXT,
                            ComplicationType.SHORT_TEXT
                        ),
                        DefaultComplicationDataSourcePolicy(
                            ComponentName("com.package1", "com.app1"),
                            ComplicationType.PHOTO_IMAGE,
                            ComponentName("com.package2", "com.app2"),
                            ComplicationType.LONG_TEXT,
                            SystemDataSources.DATA_SOURCE_STEP_COUNT,
                            ComplicationType.SHORT_TEXT
                        ),
                        ComplicationSlotBounds(RectF(0f, 0f, 1f, 1f)),
                        object : ComplicationTapFilter {
                            override fun hitTest(
                                complicationSlot: ComplicationSlot,
                                screenBounds: Rect,
                                @Px x: Int,
                                @Px y: Int,
                                includeMargins: Boolean
                            ): Boolean = (x % 2 == 0) && (y % 2 == 0)
                        }
                    )
                    .build()
            ),
            currentUserStyleRepository
        )
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
                    16
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

internal object TestServicesHelpers {
    fun createTestComplications(context: Context) =
        mapOf(
            ExampleCanvasAnalogWatchFaceService.EXAMPLE_CANVAS_WATCHFACE_LEFT_COMPLICATION_ID to
                ShortTextComplicationData.Builder(
                        PlainComplicationText.Builder("ID").build(),
                        ComplicationText.EMPTY
                    )
                    .setTitle(PlainComplicationText.Builder("Left").build())
                    .setTapAction(
                        PendingIntent.getActivity(
                            context,
                            0,
                            Intent("left"),
                            PendingIntent.FLAG_IMMUTABLE
                        )
                    )
                    .build(),
            ExampleCanvasAnalogWatchFaceService.EXAMPLE_CANVAS_WATCHFACE_RIGHT_COMPLICATION_ID to
                ShortTextComplicationData.Builder(
                        PlainComplicationText.Builder("ID").build(),
                        ComplicationText.EMPTY
                    )
                    .setTitle(PlainComplicationText.Builder("Right").build())
                    .setTapAction(
                        PendingIntent.getActivity(
                            context,
                            0,
                            Intent("right"),
                            PendingIntent.FLAG_IMMUTABLE
                        )
                    )
                    .build()
        )

    inline fun <reified T> componentOf(): ComponentName {
        return ComponentName(T::class.java.`package`?.name!!, T::class.java.name)
    }
}
