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
import android.icu.util.Calendar
import android.view.SurfaceHolder
import androidx.annotation.Sampled
import androidx.wear.complications.ComplicationBounds
import androidx.wear.complications.DefaultComplicationProviderPolicy
import androidx.wear.complications.SystemProviders
import androidx.wear.complications.data.ComplicationType
import androidx.wear.watchface.CanvasComplicationFactory
import androidx.wear.watchface.CanvasType
import androidx.wear.watchface.Complication
import androidx.wear.watchface.ComplicationsManager
import androidx.wear.watchface.Renderer
import androidx.wear.watchface.WatchFace
import androidx.wear.watchface.WatchFaceService
import androidx.wear.watchface.WatchFaceType
import androidx.wear.watchface.WatchState
import androidx.wear.watchface.complications.rendering.CanvasComplicationDrawable
import androidx.wear.watchface.complications.rendering.ComplicationDrawable
import androidx.wear.watchface.style.CurrentUserStyleRepository
import androidx.wear.watchface.style.UserStyle
import androidx.wear.watchface.style.UserStyleSchema
import androidx.wear.watchface.style.UserStyleSetting
import androidx.wear.watchface.style.UserStyleSetting.ListUserStyleSetting
import androidx.wear.watchface.style.UserStyleSetting.Option
import androidx.wear.watchface.style.WatchFaceLayer

@Sampled
fun kDocCreateExampleWatchFaceService(): WatchFaceService {
    class ExampleCanvasWatchFaceService : WatchFaceService() {
        override fun createUserStyleSchema() =
            UserStyleSchema(
                listOf(
                    ListUserStyleSetting(
                        UserStyleSetting.Id("color_style_setting"),
                        "Colors",
                        "Watchface colorization",
                        icon = null,
                        options = listOf(
                            ListUserStyleSetting.ListOption(
                                Option.Id("red_style"),
                                "Red",
                                icon = null
                            ),
                            ListUserStyleSetting.ListOption(
                                Option.Id("green_style"),
                                "Green",
                                icon = null
                            ),
                            ListUserStyleSetting.ListOption(
                                Option.Id("bluestyle"),
                                "Blue",
                                icon = null
                            )
                        ),
                        listOf(
                            WatchFaceLayer.BASE,
                            WatchFaceLayer.COMPLICATIONS,
                            WatchFaceLayer.COMPLICATIONS_OVERLAY
                        )
                    ),
                    ListUserStyleSetting(
                        UserStyleSetting.Id("hand_style_setting"),
                        "Hand Style",
                        "Hand visual look",
                        icon = null,
                        options = listOf(
                            ListUserStyleSetting.ListOption(
                                Option.Id("classic_style"), "Classic", icon = null
                            ),
                            ListUserStyleSetting.ListOption(
                                Option.Id("modern_style"), "Modern", icon = null
                            ),
                            ListUserStyleSetting.ListOption(
                                Option.Id("gothic_style"),
                                "Gothic",
                                icon = null
                            )
                        ),
                        listOf(WatchFaceLayer.COMPLICATIONS_OVERLAY)
                    )
                )
            )

        override fun createComplicationsManager(
            currentUserStyleRepository: CurrentUserStyleRepository
        ): ComplicationsManager {
            val canvasComplicationFactory =
                CanvasComplicationFactory { watchState, listener ->
                    CanvasComplicationDrawable(ComplicationDrawable(this), watchState, listener)
                }
            return ComplicationsManager(
                listOf(
                    Complication.createRoundRectComplicationBuilder(
                        /*id */ 0,
                        canvasComplicationFactory,
                        listOf(
                            ComplicationType.RANGED_VALUE,
                            ComplicationType.LONG_TEXT,
                            ComplicationType.SHORT_TEXT,
                            ComplicationType.MONOCHROMATIC_IMAGE,
                            ComplicationType.SMALL_IMAGE
                        ),
                        DefaultComplicationProviderPolicy(SystemProviders.PROVIDER_DAY_OF_WEEK),
                        ComplicationBounds(RectF(0.15625f, 0.1875f, 0.84375f, 0.3125f))
                    ).setDefaultProviderType(ComplicationType.SHORT_TEXT)
                        .build(),
                    Complication.createRoundRectComplicationBuilder(
                        /*id */ 1,
                        canvasComplicationFactory,
                        listOf(
                            ComplicationType.RANGED_VALUE,
                            ComplicationType.LONG_TEXT,
                            ComplicationType.SHORT_TEXT,
                            ComplicationType.MONOCHROMATIC_IMAGE,
                            ComplicationType.SMALL_IMAGE
                        ),
                        DefaultComplicationProviderPolicy(SystemProviders.PROVIDER_STEP_COUNT),
                        ComplicationBounds(RectF(0.1f, 0.5625f, 0.35f, 0.8125f))
                    ).setDefaultProviderType(ComplicationType.SHORT_TEXT)
                        .build()
                ),
                currentUserStyleRepository
            )
        }

        override suspend fun createWatchFace(
            surfaceHolder: SurfaceHolder,
            watchState: WatchState,
            complicationsManager: ComplicationsManager,
            currentUserStyleRepository: CurrentUserStyleRepository
        ) = WatchFace(
            WatchFaceType.ANALOG,
            object : Renderer.CanvasRenderer(
                surfaceHolder,
                currentUserStyleRepository,
                watchState,
                CanvasType.HARDWARE,
                /* interactiveUpdateRateMillis */ 16,
            ) {
                init {
                    currentUserStyleRepository.addUserStyleChangeListener(
                        object : CurrentUserStyleRepository.UserStyleChangeListener {
                            override fun onUserStyleChanged(userStyle: UserStyle) {
                                // `userStyle` will contain two userStyle categories with options
                                // from the lists above. ...
                            }
                        }
                    )
                }

                override fun render(
                    canvas: Canvas,
                    bounds: Rect,
                    calendar: Calendar
                ) {
                    // ...
                }

                override fun renderHighlightLayer(
                    canvas: Canvas,
                    bounds: Rect,
                    calendar: Calendar
                ) {
                    canvas.drawColor(renderParameters.highlightLayer!!.backgroundTint)

                    // ...
                }
            }
        )
    }

    return ExampleCanvasWatchFaceService()
}
