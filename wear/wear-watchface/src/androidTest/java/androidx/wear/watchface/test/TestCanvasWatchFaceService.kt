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

package androidx.wear.watchface.test

import android.content.Context
import android.graphics.RectF
import android.graphics.drawable.Icon
import android.os.Handler
import android.view.SurfaceHolder
import androidx.wear.complications.DefaultComplicationProviderPolicy
import androidx.wear.complications.SystemProviders
import androidx.wear.complications.data.ComplicationType
import androidx.wear.watchface.Complication
import androidx.wear.watchface.ComplicationsManager
import androidx.wear.watchface.MutableWatchState
import androidx.wear.watchface.WatchFace
import androidx.wear.watchface.WatchFaceHost
import androidx.wear.watchface.WatchFaceService
import androidx.wear.watchface.WatchFaceType
import androidx.wear.watchface.WatchState
import androidx.wear.watchface.samples.EXAMPLE_CANVAS_WATCHFACE_LEFT_COMPLICATION_ID
import androidx.wear.watchface.samples.EXAMPLE_CANVAS_WATCHFACE_RIGHT_COMPLICATION_ID
import androidx.wear.watchface.samples.ExampleCanvasRenderer
import androidx.wear.watchface.samples.LEFT_AND_RIGHT_COMPLICATIONS
import androidx.wear.watchface.samples.LEFT_COMPLICATION
import androidx.wear.watchface.samples.NO_COMPLICATIONS
import androidx.wear.watchface.samples.R
import androidx.wear.watchface.samples.RIGHT_COMPLICATION
import androidx.wear.watchface.samples.WatchFaceColorStyle
import androidx.wear.watchface.style.BooleanUserStyleSetting
import androidx.wear.watchface.style.ComplicationsUserStyleSetting
import androidx.wear.watchface.style.ComplicationsUserStyleSetting.ComplicationOverlay
import androidx.wear.watchface.style.ComplicationsUserStyleSetting.ComplicationsOption
import androidx.wear.watchface.style.DoubleRangeUserStyleSetting
import androidx.wear.watchface.style.Layer
import androidx.wear.watchface.style.ListUserStyleSetting
import androidx.wear.watchface.style.UserStyleRepository
import androidx.wear.watchface.style.UserStyleSchema

/** A simple canvas test watch face for integration tests. */
internal class TestCanvasWatchFaceService(
    testContext: Context,
    private val handler: Handler,
    var mockSystemTimeMillis: Long,
    var surfacHolderOverride: SurfaceHolder
) : WatchFaceService() {

    private val mutableWatchState = MutableWatchState().apply {
        isAmbient.value = false
    }

    init {
        attachBaseContext(testContext)
    }

    override fun createWatchFace(
        surfaceHolder: SurfaceHolder,
        watchFaceHost: WatchFaceHost,
        watchState: WatchState
    ): WatchFace {
        // Override is necessary because the watch face isn't visible in this test.
        mutableWatchState.isVisible.value = true

        val watchFaceStyle = WatchFaceColorStyle.create(this, "red_style")
        val colorStyleSetting = ListUserStyleSetting(
            "color_style_setting",
            "Colors",
            "Watchface colorization",
            icon = null,
            options = listOf(
                ListUserStyleSetting.ListOption(
                    "red_style",
                    "Red",
                    Icon.createWithResource(this, R.drawable.red_style)
                ),
                ListUserStyleSetting.ListOption(
                    "green_style",
                    "Green",
                    Icon.createWithResource(this, R.drawable.green_style)
                )
            ),
            listOf(Layer.BASE_LAYER, Layer.COMPLICATIONS, Layer.TOP_LAYER)
        )
        val drawHourPipsStyleSetting =
            BooleanUserStyleSetting(
                "draw_hour_pips_style_setting",
                "Hour Pips",
                "Whether or not hour pips should be drawn",
                null,
                true,
                listOf(Layer.BASE_LAYER)
            )
        val watchHandLengthStyleSetting =
            DoubleRangeUserStyleSetting(
                "watch_hand_length_style_setting",
                "Hand length",
                "How long the watch hands should be",
                null,
                0.25,
                1.0,
                1.0,
                listOf(Layer.TOP_LAYER)
            )
        val complicationsStyleSetting = ComplicationsUserStyleSetting(
            "complications_style_setting",
            "Complications",
            "Number and position",
            icon = null,
            complicationConfig = listOf(
                ComplicationsOption(
                    LEFT_AND_RIGHT_COMPLICATIONS,
                    "Both",
                    null,
                    listOf(
                        ComplicationOverlay.Builder(EXAMPLE_CANVAS_WATCHFACE_LEFT_COMPLICATION_ID)
                            .setEnabled(true).build(),
                        ComplicationOverlay.Builder(EXAMPLE_CANVAS_WATCHFACE_RIGHT_COMPLICATION_ID)
                            .setEnabled(true).build()
                    )
                ),
                ComplicationsOption(
                    NO_COMPLICATIONS,
                    "None",
                    null,
                    listOf(
                        ComplicationOverlay.Builder(EXAMPLE_CANVAS_WATCHFACE_LEFT_COMPLICATION_ID)
                            .setEnabled(false).build(),
                        ComplicationOverlay.Builder(EXAMPLE_CANVAS_WATCHFACE_RIGHT_COMPLICATION_ID)
                            .setEnabled(false).build()
                    )
                ),
                ComplicationsOption(
                    LEFT_COMPLICATION,
                    "Left",
                    null,
                    listOf(
                        ComplicationOverlay.Builder(EXAMPLE_CANVAS_WATCHFACE_LEFT_COMPLICATION_ID)
                            .setEnabled(true).build(),
                        ComplicationOverlay.Builder(EXAMPLE_CANVAS_WATCHFACE_RIGHT_COMPLICATION_ID)
                            .setEnabled(true).build()
                    )
                ),
                ComplicationsOption(
                    RIGHT_COMPLICATION,
                    "Right",
                    null,
                    listOf(
                        ComplicationOverlay.Builder(EXAMPLE_CANVAS_WATCHFACE_LEFT_COMPLICATION_ID)
                            .setEnabled(true).build(),
                        ComplicationOverlay.Builder(EXAMPLE_CANVAS_WATCHFACE_RIGHT_COMPLICATION_ID)
                            .setEnabled(false).build()
                    )
                )
            ),
            listOf(Layer.COMPLICATIONS)
        )
        val userStyleRepository = UserStyleRepository(
            UserStyleSchema(
                listOf(
                    colorStyleSetting,
                    drawHourPipsStyleSetting,
                    watchHandLengthStyleSetting,
                    complicationsStyleSetting
                )
            )
        )
        val complicationSlots = ComplicationsManager(
            listOf(
                Complication.Builder(
                    EXAMPLE_CANVAS_WATCHFACE_LEFT_COMPLICATION_ID,
                    watchFaceStyle.getComplicationDrawableRenderer(this, watchState),
                    listOf(
                        ComplicationType.RANGED_VALUE,
                        ComplicationType.LONG_TEXT,
                        ComplicationType.SHORT_TEXT,
                        ComplicationType.MONOCHROMATIC_IMAGE,
                        ComplicationType.SMALL_IMAGE
                    ),
                    DefaultComplicationProviderPolicy(SystemProviders.DAY_OF_WEEK)
                ).setUnitSquareBounds(RectF(0.2f, 0.4f, 0.4f, 0.6f))
                    .setDefaultProviderType(ComplicationType.SHORT_TEXT)
                    .build(),
                Complication.Builder(
                    EXAMPLE_CANVAS_WATCHFACE_RIGHT_COMPLICATION_ID,
                    watchFaceStyle.getComplicationDrawableRenderer(this, watchState),
                    listOf(
                        ComplicationType.RANGED_VALUE,
                        ComplicationType.LONG_TEXT,
                        ComplicationType.SHORT_TEXT,
                        ComplicationType.MONOCHROMATIC_IMAGE,
                        ComplicationType.SMALL_IMAGE
                    ),
                    DefaultComplicationProviderPolicy(SystemProviders.STEP_COUNT)
                ).setUnitSquareBounds(RectF(0.6f, 0.4f, 0.8f, 0.6f))
                    .setDefaultProviderType(ComplicationType.SHORT_TEXT)
                    .build()
            ),
            userStyleRepository
        )
        val renderer = ExampleCanvasRenderer(
            surfaceHolder,
            this,
            watchFaceStyle,
            userStyleRepository,
            watchState,
            colorStyleSetting,
            drawHourPipsStyleSetting,
            watchHandLengthStyleSetting,
            complicationSlots
        )

        return WatchFace.Builder(
            WatchFaceType.ANALOG,
            16,
            userStyleRepository,
            complicationSlots,
            renderer,
            watchFaceHost,
            watchState
        ).setSystemTimeProvider(object : WatchFace.SystemTimeProvider {
            override fun getSystemTimeMillis(): Long {
                return mockSystemTimeMillis
            }
        }).build()
    }

    override fun getMutableWatchState() = mutableWatchState

    override fun getHandler() = handler

    // We want full control over when frames are produced.
    override fun allowWatchFaceToAnimate() = false

    override fun getWallpaperSurfaceHolderOverride() = surfacHolderOverride
}
