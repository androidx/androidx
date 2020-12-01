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
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Rect
import android.icu.util.Calendar
import android.os.Handler
import android.support.wearable.complications.ComplicationData
import android.support.wearable.complications.ComplicationText
import android.support.wearable.watchface.IWatchFaceService
import android.support.wearable.watchface.WatchFaceStyle
import android.support.wearable.watchface.accessibility.ContentDescriptionLabel
import android.view.SurfaceHolder
import androidx.test.core.app.ApplicationProvider
import androidx.wear.complications.data.IdAndComplicationData
import androidx.wear.watchface.style.UserStyle
import androidx.wear.watchface.style.UserStyleRepository
import org.junit.runners.model.FrameworkMethod
import org.robolectric.RobolectricTestRunner
import org.robolectric.internal.bytecode.InstrumentationConfiguration

internal class TestWatchFaceService(
    @WatchFaceType private val watchFaceType: Int,
    private val complicationsManager: ComplicationsManager,
    private val renderer: TestRenderer,
    private val userStyleRepository: UserStyleRepository,
    private val watchState: MutableWatchState,
    private val handler: Handler,
    private val tapListener: WatchFace.TapListener?
) : WatchFaceService() {
    var complicationSingleTapped: Int? = null
    var complicationDoubleTapped: Int? = null
    var complicationSelected: Int? = null
    var mockSystemTimeMillis = 0L
    var lastUserStyle: UserStyle? = null

    init {
        userStyleRepository.addUserStyleListener(
            object : UserStyleRepository.UserStyleListener {
                override fun onUserStyleChanged(userStyle: UserStyle) {
                    lastUserStyle = userStyle
                }
            }
        )

        complicationsManager.addTapListener(
            object : ComplicationsManager.TapCallback {
                override fun onComplicationSingleTapped(complicationId: Int) {
                    complicationSingleTapped = complicationId
                }

                override fun onComplicationDoubleTapped(complicationId: Int) {
                    complicationDoubleTapped = complicationId
                }
            })
    }

    fun reset() {
        clearTappedState()
        complicationSelected = null
        renderer.lastOnDrawCalendar = null
        mockSystemTimeMillis = 0L
    }

    fun clearTappedState() {
        complicationSingleTapped = null
        complicationDoubleTapped = null
    }

    init {
        attachBaseContext(ApplicationProvider.getApplicationContext())
    }

    override fun createWatchFace(
        surfaceHolder: SurfaceHolder,
        watchState: WatchState
    ) = WatchFace(
        watchFaceType,
        userStyleRepository,
        complicationsManager,
        renderer
    ).setSystemTimeProvider(object : WatchFace.SystemTimeProvider {
        override fun getSystemTimeMillis(): Long {
            return mockSystemTimeMillis
        }
    }).setTapListener(tapListener)

    override fun getHandler() = handler

    override fun getMutableWatchState() = watchState
}

/**
 * IWatchFaceService.Stub implementation that redirects all method calls to a mock so they can be
 * verified. (Using a Spy on the actual stub doesn't work).
 */
class WatchFaceServiceStub(private val iWatchFaceService: IWatchFaceService) :
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
}

open class TestRenderer(
    surfaceHolder: SurfaceHolder,
    userStyleRepository: UserStyleRepository,
    watchState: WatchState,
    interactiveFrameRateMs: Long
) : Renderer.CanvasRenderer(
    surfaceHolder,
    userStyleRepository,
    watchState,
    CanvasType.HARDWARE,
    interactiveFrameRateMs
) {
    var lastOnDrawCalendar: Calendar? = null
    var lastRenderParamaters = RenderParameters.DEFAULT_INTERACTIVE

    override fun render(
        canvas: Canvas,
        bounds: Rect,
        calendar: Calendar
    ) {
        lastOnDrawCalendar = calendar
        lastRenderParamaters = renderParameters
    }
}

fun createIdAndComplicationData(id: Int) =
    IdAndComplicationData(
        id,
        ComplicationData.Builder(ComplicationData.TYPE_SHORT_TEXT)
            .setShortText(ComplicationText.plainText("Test Text"))
            .setTapAction(
                PendingIntent.getActivity(
                    ApplicationProvider.getApplicationContext(), 0,
                    Intent("Fake intent"), 0
                )
            ).build()
    )

/**
 * We need to prevent roboloetric from instrumenting our classes or things break...
 */
class WatchFaceTestRunner(testClass: Class<*>) : RobolectricTestRunner(testClass) {
    override fun createClassLoaderConfig(method: FrameworkMethod): InstrumentationConfiguration =
        InstrumentationConfiguration.Builder(super.createClassLoaderConfig(method))
            .doNotInstrumentPackage("android.support.wearable.watchface")
            .doNotInstrumentPackage("androidx.wear.complications")
            .doNotInstrumentPackage("androidx.wear.watchface")
            .doNotInstrumentPackage("androidx.wear.watchface.ui")
            .doNotInstrumentPackage("androidx.wear.watchfacestyle")
            .build()
}
