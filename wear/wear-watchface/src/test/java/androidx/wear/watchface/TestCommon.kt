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

import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Rect
import android.os.Handler
import android.support.wearable.complications.ComplicationData
import android.support.wearable.complications.ComplicationText
import android.support.wearable.watchface.IWatchFaceService
import android.support.wearable.watchface.WatchFaceStyle
import android.support.wearable.watchface.accessibility.ContentDescriptionLabel
import android.view.SurfaceHolder
import androidx.test.core.app.ApplicationProvider
import androidx.wear.complications.data.toApiComplicationData
import androidx.wear.watchface.control.data.WallpaperInteractiveWatchFaceInstanceParams
import androidx.wear.watchface.style.CurrentUserStyleRepository
import androidx.wear.watchface.style.UserStyleSchema
import org.junit.runners.model.FrameworkMethod
import org.robolectric.RobolectricTestRunner
import org.robolectric.internal.bytecode.InstrumentationConfiguration
import java.time.ZoneId
import java.time.ZonedDateTime

internal class TestWatchFaceService(
    @WatchFaceType private val watchFaceType: Int,
    private val complicationSlots: List<ComplicationSlot>,
    private val rendererFactory: (
        surfaceHolder: SurfaceHolder,
        currentUserStyleRepository: CurrentUserStyleRepository,
        watchState: WatchState,
    ) -> TestRenderer,
    private val userStyleSchema: UserStyleSchema,
    private val watchState: MutableWatchState,
    private val handler: Handler,
    private val tapListener: WatchFace.TapListener?,
    private val preAndroidR: Boolean,
    private val directBootParams: WallpaperInteractiveWatchFaceInstanceParams?
) : WatchFaceService() {
    /** The ids of the [ComplicationSlot]s that have been tapped. */
    val tappedComplicationSlotIds: List<Int>
        get() = mutableTappedComplicationIds
    var complicationSelected: Int? = null
    var mockSystemTimeMillis = 0L
    var mockZoneId: ZoneId = ZoneId.of("UTC")
    var renderer: TestRenderer? = null

    /** A mutable list of the ids of the complicationSlots that have been tapped. */
    private val mutableTappedComplicationIds: MutableList<Int> = ArrayList()

    fun reset() {
        clearTappedState()
        complicationSelected = null
        renderer?.lastOnDrawZonedDateTime = null
        mockSystemTimeMillis = 0L
    }

    fun clearTappedState() {
        mutableTappedComplicationIds.clear()
    }

    init {
        attachBaseContext(ApplicationProvider.getApplicationContext())
    }

    override fun createUserStyleSchema() = userStyleSchema

    override fun createComplicationSlotsManager(
        currentUserStyleRepository: CurrentUserStyleRepository
    ): ComplicationSlotsManager {
        val complicationSlotsManager =
            ComplicationSlotsManager(complicationSlots, currentUserStyleRepository)
        complicationSlotsManager.addTapListener(
            object : ComplicationSlotsManager.TapCallback {
                override fun onComplicationSlotTapped(complicationSlotId: Int) {
                    mutableTappedComplicationIds.add(complicationSlotId)
                }
            }
        )
        return complicationSlotsManager
    }

    override suspend fun createWatchFace(
        surfaceHolder: SurfaceHolder,
        watchState: WatchState,
        complicationSlotsManager: ComplicationSlotsManager,
        currentUserStyleRepository: CurrentUserStyleRepository
    ): WatchFace {
        renderer = rendererFactory(surfaceHolder, currentUserStyleRepository, watchState)
        return WatchFace(watchFaceType, renderer!!)
            .setSystemTimeProvider(object : WatchFace.SystemTimeProvider {
                override fun getSystemTimeMillis() = mockSystemTimeMillis

                override fun getSystemTimeZoneId() = mockZoneId
            }).setTapListener(tapListener)
    }

    override fun getUiThreadHandlerImpl() = handler

    // To make unit tests simpler and non-flaky we run background tasks and ui tasks on the same
    // handler.
    override fun getBackgroundThreadHandlerImpl() = handler

    override fun getMutableWatchState() = watchState

    fun setIsVisible(isVisible: Boolean) {
        watchState.isVisible.value = isVisible
    }

    override fun readDirectBootPrefs(
        context: Context,
        fileName: String
    ) = directBootParams

    override fun writeDirectBootPrefs(
        context: Context,
        fileName: String,
        prefs: WallpaperInteractiveWatchFaceInstanceParams
    ) {
    }

    override fun expectPreRInitFlow() = preAndroidR
}

/**
 * IWatchFaceService.Stub implementation that redirects all method calls to a mock so they can be
 * verified. (Using a Spy on the actual stub doesn't work).
 */
public class WatchFaceServiceStub(private val iWatchFaceService: IWatchFaceService) :
    IWatchFaceService.Stub() {
    override fun setStyle(style: WatchFaceStyle) {
        iWatchFaceService.setStyle(style)
    }

    override fun getApiVersion(): Int = iWatchFaceService.apiVersion

    override fun setActiveComplications(ids: IntArray, updateAll: Boolean) {
        iWatchFaceService.setActiveComplications(ids, updateAll)
    }

    override fun setDefaultComplicationProvider(
        watchFaceComplicationId: Int,
        provider: ComponentName,
        type: Int
    ) {
        iWatchFaceService.setDefaultComplicationProvider(
            watchFaceComplicationId, provider, type
        )
    }

    override fun setDefaultSystemComplicationProvider(
        watchFaceComplicationId: Int,
        systemProvider: Int,
        type: Int
    ) {
        iWatchFaceService.setDefaultSystemComplicationProvider(
            watchFaceComplicationId, systemProvider, type
        )
    }

    override fun setContentDescriptionLabels(labels: Array<ContentDescriptionLabel>) {
        iWatchFaceService.setContentDescriptionLabels(labels)
    }

    override fun reserved5() {
        iWatchFaceService.reserved5()
    }

    override fun setDefaultComplicationProviderWithFallbacks(
        watchFaceComplicationId: Int,
        providers: List<ComponentName>,
        fallbackSystemProvider: Int,
        type: Int
    ) {
        iWatchFaceService.setDefaultComplicationProviderWithFallbacks(
            watchFaceComplicationId, providers, fallbackSystemProvider, type
        )
    }

    override fun reserved8() {
        iWatchFaceService.reserved8()
    }
}

public open class TestRenderer(
    surfaceHolder: SurfaceHolder,
    currentUserStyleRepository: CurrentUserStyleRepository,
    watchState: WatchState,
    interactiveFrameRateMs: Long
) : Renderer.CanvasRenderer(
    surfaceHolder,
    currentUserStyleRepository,
    watchState,
    CanvasType.HARDWARE,
    interactiveFrameRateMs
) {
    public var lastOnDrawZonedDateTime: ZonedDateTime? = null
    public var lastRenderParameters: RenderParameters = RenderParameters.DEFAULT_INTERACTIVE

    override fun render(
        canvas: Canvas,
        bounds: Rect,
        zonedDateTime: ZonedDateTime
    ) {
        lastOnDrawZonedDateTime = zonedDateTime
        lastRenderParameters = renderParameters
    }

    override fun renderHighlightLayer(canvas: Canvas, bounds: Rect, zonedDateTime: ZonedDateTime) {
    }
}

public open class TestRendererWithShouldAnimate(
    surfaceHolder: SurfaceHolder,
    currentUserStyleRepository: CurrentUserStyleRepository,
    watchState: WatchState,
    interactiveFrameRateMs: Long,
    public var animate: Boolean = true
) : TestRenderer(
    surfaceHolder,
    currentUserStyleRepository,
    watchState,
    interactiveFrameRateMs
) {
    override fun shouldAnimate(): Boolean = animate
}

public fun createComplicationData(): androidx.wear.complications.data.ComplicationData =
    ComplicationData.Builder(ComplicationData.TYPE_SHORT_TEXT)
        .setShortText(ComplicationText.plainText("Test Text"))
        .setTapAction(
            PendingIntent.getActivity(
                ApplicationProvider.getApplicationContext(), 0,
                Intent("Fake intent"), 0
            )
        ).build().toApiComplicationData()

/**
 * We need to prevent roboloetric from instrumenting our classes or things break...
 */
public class WatchFaceTestRunner(testClass: Class<*>) : RobolectricTestRunner(testClass) {
    override fun createClassLoaderConfig(method: FrameworkMethod): InstrumentationConfiguration =
        InstrumentationConfiguration.Builder(super.createClassLoaderConfig(method))
            .doNotInstrumentPackage("android.support.wearable.watchface")
            .doNotInstrumentPackage("androidx.wear.complicationSlots")
            .doNotInstrumentPackage("androidx.wear.utility")
            .doNotInstrumentPackage("androidx.wear.watchface")
            .doNotInstrumentPackage("androidx.wear.watchface.ui")
            .doNotInstrumentPackage("androidx.wear.watchfacestyle")
            .build()
}
