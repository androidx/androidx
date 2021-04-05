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
import androidx.wear.watchface.complications.rendering.ComplicationDrawable
import androidx.wear.watchface.CanvasType
import androidx.wear.watchface.Complication
import androidx.wear.watchface.CanvasComplicationDrawable
import androidx.wear.watchface.ComplicationsManager
import androidx.wear.watchface.Renderer
import androidx.wear.watchface.WatchFace
import androidx.wear.watchface.WatchFaceService
import androidx.wear.watchface.WatchFaceType
import androidx.wear.watchface.WatchState
import androidx.wear.watchface.style.Layer
import androidx.wear.watchface.style.UserStyle
import androidx.wear.watchface.style.CurrentUserStyleRepository
import androidx.wear.watchface.style.UserStyleSchema
import androidx.wear.watchface.style.UserStyleSetting
import androidx.wear.watchface.style.UserStyleSetting.ListUserStyleSetting
import androidx.wear.watchface.style.UserStyleSetting.Option

@Sampled
fun kDocCreateExampleWatchFaceService(): WatchFaceService {

    class ExampleCanvasWatchFaceService : WatchFaceService() {
        override suspend fun createWatchFace(
            surfaceHolder: SurfaceHolder,
            watchState: WatchState
        ): WatchFace {
            val userStyleRepository = CurrentUserStyleRepository(
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
                            listOf(Layer.BASE, Layer.COMPLICATIONS, Layer.COMPLICATIONS_OVERLAY)
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
                            listOf(Layer.COMPLICATIONS_OVERLAY)
                        )
                    )
                )
            )
            val complicationSlots = ComplicationsManager(
                listOf(
                    Complication.createRoundRectComplicationBuilder(
                        /*id */ 0,
                        CanvasComplicationDrawable(
                            ComplicationDrawable(this),
                            watchState
                        ),
                        listOf(
                            ComplicationType.RANGED_VALUE,
                            ComplicationType.LONG_TEXT,
                            ComplicationType.SHORT_TEXT,
                            ComplicationType.MONOCHROMATIC_IMAGE,
                            ComplicationType.SMALL_IMAGE
                        ),
                        DefaultComplicationProviderPolicy(SystemProviders.DAY_OF_WEEK),
                        ComplicationBounds(RectF(0.15625f, 0.1875f, 0.84375f, 0.3125f))
                    ).setDefaultProviderType(ComplicationType.SHORT_TEXT)
                        .build(),
                    Complication.createRoundRectComplicationBuilder(
                        /*id */ 1,
                        CanvasComplicationDrawable(
                            ComplicationDrawable(this),
                            watchState
                        ),
                        listOf(
                            ComplicationType.RANGED_VALUE,
                            ComplicationType.LONG_TEXT,
                            ComplicationType.SHORT_TEXT,
                            ComplicationType.MONOCHROMATIC_IMAGE,
                            ComplicationType.SMALL_IMAGE
                        ),
                        DefaultComplicationProviderPolicy(SystemProviders.STEP_COUNT),
                        ComplicationBounds(RectF(0.1f, 0.5625f, 0.35f, 0.8125f))
                    ).setDefaultProviderType(ComplicationType.SHORT_TEXT)
                        .build()
                ),
                userStyleRepository
            )

            val renderer = object : Renderer.CanvasRenderer(
                surfaceHolder,
                userStyleRepository,
                watchState,
                CanvasType.HARDWARE,
                /* interactiveUpdateRateMillis */ 16,
            ) {
                init {
                    userStyleRepository.addUserStyleChangeListener(
                        object : CurrentUserStyleRepository.UserStyleChangeListener {
                            override fun onUserStyleChanged(userStyle: UserStyle) {
                                // `userStyle` will contain two userStyle categories with options
                                // from the lists above. ...
                            }
                        })
                }

                override fun render(
                    canvas: Canvas,
                    bounds: Rect,
                    calendar: Calendar
                ) {
                    // ...
                }
            }

            return WatchFace(
                WatchFaceType.ANALOG,
                userStyleRepository,
                renderer,
                complicationSlots
            )
        }
    }

    return ExampleCanvasWatchFaceService()
}
