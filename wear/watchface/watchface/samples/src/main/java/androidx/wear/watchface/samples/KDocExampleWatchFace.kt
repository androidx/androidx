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

@file:Suppress("unused")

package androidx.wear.watchface.samples

import android.graphics.Canvas
import android.graphics.Rect
import android.graphics.RectF
import android.view.SurfaceHolder
import androidx.annotation.Sampled
import androidx.wear.watchface.CanvasComplicationFactory
import androidx.wear.watchface.CanvasType
import androidx.wear.watchface.ComplicationSlot
import androidx.wear.watchface.ComplicationSlotsManager
import androidx.wear.watchface.Renderer
import androidx.wear.watchface.WatchFace
import androidx.wear.watchface.WatchFaceService
import androidx.wear.watchface.WatchFaceType
import androidx.wear.watchface.WatchState
import androidx.wear.watchface.complications.ComplicationSlotBounds
import androidx.wear.watchface.complications.DefaultComplicationDataSourcePolicy
import androidx.wear.watchface.complications.SystemDataSources
import androidx.wear.watchface.complications.data.ComplicationExperimental
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.rendering.CanvasComplicationDrawable
import androidx.wear.watchface.complications.rendering.ComplicationDrawable
import androidx.wear.watchface.style.CurrentUserStyleRepository
import androidx.wear.watchface.style.UserStyleSchema
import androidx.wear.watchface.style.UserStyleSetting
import androidx.wear.watchface.style.UserStyleSetting.ListUserStyleSetting
import androidx.wear.watchface.style.UserStyleSetting.Option
import androidx.wear.watchface.style.WatchFaceLayer
import java.time.ZonedDateTime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private const val COLOR_STYLE_SETTING = "color_style_setting"
private const val RED_STYLE = "red_style"
private const val GREEN_STYLE = "green_style"
private const val BLUE_STYLE = "blue_style"

private const val HAND_STYLE_SETTING = "hand_style_setting"
private const val CLASSIC_STYLE = "classic_style"
private const val MODERN_STYLE = "modern_style"
private const val GOTHIC_STYLE = "gothic_style"

@Sampled
fun kDocCreateExampleWatchFaceService(): WatchFaceService {
    class ExampleCanvasWatchFaceService : WatchFaceService() {
        override fun createUserStyleSchema() =
            UserStyleSchema(
                listOf(
                    ListUserStyleSetting.Builder(
                            UserStyleSetting.Id(COLOR_STYLE_SETTING),
                            options =
                                listOf(
                                    ListUserStyleSetting.ListOption.Builder(
                                            Option.Id(RED_STYLE),
                                            resources,
                                            R.string.colors_style_red,
                                            R.string.colors_style_red_screen_reader
                                        )
                                        .build(),
                                    ListUserStyleSetting.ListOption.Builder(
                                            Option.Id(GREEN_STYLE),
                                            resources,
                                            R.string.colors_style_green,
                                            R.string.colors_style_green_screen_reader
                                        )
                                        .build(),
                                    ListUserStyleSetting.ListOption.Builder(
                                            Option.Id(BLUE_STYLE),
                                            resources,
                                            R.string.colors_style_blue,
                                            R.string.colors_style_blue_screen_reader
                                        )
                                        .build()
                                ),
                            listOf(
                                WatchFaceLayer.BASE,
                                WatchFaceLayer.COMPLICATIONS,
                                WatchFaceLayer.COMPLICATIONS_OVERLAY
                            ),
                            resources,
                            R.string.colors_style_setting,
                            R.string.colors_style_setting_description
                        )
                        .build(),
                    ListUserStyleSetting.Builder(
                            UserStyleSetting.Id(HAND_STYLE_SETTING),
                            options =
                                listOf(
                                    ListUserStyleSetting.ListOption.Builder(
                                            Option.Id(CLASSIC_STYLE),
                                            resources,
                                            R.string.hand_style_classic,
                                            R.string.hand_style_classic_screen_reader
                                        )
                                        .build(),
                                    ListUserStyleSetting.ListOption.Builder(
                                            Option.Id(MODERN_STYLE),
                                            resources,
                                            R.string.hand_style_modern,
                                            R.string.hand_style_modern_screen_reader
                                        )
                                        .build(),
                                    ListUserStyleSetting.ListOption.Builder(
                                            Option.Id(GOTHIC_STYLE),
                                            resources,
                                            R.string.hand_style_gothic,
                                            R.string.hand_style_gothic_screen_reader
                                        )
                                        .build()
                                ),
                            listOf(WatchFaceLayer.COMPLICATIONS_OVERLAY),
                            resources,
                            R.string.hand_style_setting,
                            R.string.hand_style_setting_description
                        )
                        .build()
                )
            )

        @ComplicationExperimental
        override fun createComplicationSlotsManager(
            currentUserStyleRepository: CurrentUserStyleRepository
        ): ComplicationSlotsManager {
            val canvasComplicationFactory = CanvasComplicationFactory { watchState, listener ->
                CanvasComplicationDrawable(ComplicationDrawable(this), watchState, listener)
            }
            return ComplicationSlotsManager(
                listOf(
                    ComplicationSlot.createRoundRectComplicationSlotBuilder(
                            /*id */ 0,
                            canvasComplicationFactory,
                            listOf(
                                ComplicationType.RANGED_VALUE,
                                ComplicationType.LONG_TEXT,
                                ComplicationType.SHORT_TEXT,
                                ComplicationType.MONOCHROMATIC_IMAGE,
                                ComplicationType.SMALL_IMAGE
                            ),
                            DefaultComplicationDataSourcePolicy(
                                SystemDataSources.DATA_SOURCE_DAY_OF_WEEK,
                                ComplicationType.SHORT_TEXT
                            ),
                            ComplicationSlotBounds(RectF(0.15625f, 0.1875f, 0.84375f, 0.3125f))
                        )
                        .build(),
                    ComplicationSlot.createRoundRectComplicationSlotBuilder(
                            /*id */ 1,
                            canvasComplicationFactory,
                            listOf(
                                ComplicationType.RANGED_VALUE,
                                ComplicationType.LONG_TEXT,
                                ComplicationType.SHORT_TEXT,
                                ComplicationType.MONOCHROMATIC_IMAGE,
                                ComplicationType.SMALL_IMAGE
                            ),
                            DefaultComplicationDataSourcePolicy(
                                SystemDataSources.DATA_SOURCE_STEP_COUNT,
                                ComplicationType.SHORT_TEXT
                            ),
                            ComplicationSlotBounds(RectF(0.1f, 0.5625f, 0.35f, 0.8125f))
                        )
                        .build()
                ),
                currentUserStyleRepository
            )
        }

        inner class MySharedAssets : Renderer.SharedAssets {
            override fun onDestroy() {}
        }

        override suspend fun createWatchFace(
            surfaceHolder: SurfaceHolder,
            watchState: WatchState,
            complicationSlotsManager: ComplicationSlotsManager,
            currentUserStyleRepository: CurrentUserStyleRepository
        ) =
            WatchFace(
                WatchFaceType.ANALOG,
                object :
                    Renderer.CanvasRenderer2<MySharedAssets>(
                        surfaceHolder,
                        currentUserStyleRepository,
                        watchState,
                        CanvasType.HARDWARE,
                        interactiveDrawModeUpdateDelayMillis = 16,
                        clearWithBackgroundTintBeforeRenderingHighlightLayer = true
                    ) {
                    init {
                        // Listen for user style changes.
                        CoroutineScope(Dispatchers.Main.immediate).launch {
                            currentUserStyleRepository.userStyle.collect {
                                // `userStyle` will contain two userStyle categories with options
                                // from the lists above. ..
                            }
                        }
                    }

                    override fun render(
                        canvas: Canvas,
                        bounds: Rect,
                        zonedDateTime: ZonedDateTime,
                        sharedAssets: MySharedAssets
                    ) {
                        // ...
                    }

                    override fun renderHighlightLayer(
                        canvas: Canvas,
                        bounds: Rect,
                        zonedDateTime: ZonedDateTime,
                        sharedAssets: MySharedAssets
                    ) {
                        canvas.drawColor(renderParameters.highlightLayer!!.backgroundTint)

                        // ...
                    }

                    override suspend fun createSharedAssets(): MySharedAssets {
                        // Insert resource loading here.
                        return MySharedAssets()
                    }
                }
            )
    }

    return ExampleCanvasWatchFaceService()
}
