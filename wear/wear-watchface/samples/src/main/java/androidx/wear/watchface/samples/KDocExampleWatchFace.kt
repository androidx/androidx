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
import android.support.wearable.complications.ComplicationData
import android.view.SurfaceHolder
import androidx.annotation.Sampled
import androidx.wear.complications.SystemProviders
import androidx.wear.complications.rendering.ComplicationDrawable
import androidx.wear.watchface.CanvasRenderer
import androidx.wear.watchface.CanvasType
import androidx.wear.watchface.Complication
import androidx.wear.watchface.ComplicationDrawableRenderer
import androidx.wear.watchface.ComplicationSlots
import androidx.wear.watchface.FixedBounds
import androidx.wear.watchface.SystemApi
import androidx.wear.watchface.SystemState
import androidx.wear.watchface.WatchFace
import androidx.wear.watchface.WatchFaceService
import androidx.wear.watchface.WatchFaceType
import androidx.wear.watchfacestyle.ListViewUserStyleCategory
import androidx.wear.watchfacestyle.UserStyleCategory
import androidx.wear.watchfacestyle.UserStyleManager

@Sampled
fun kDocCreateExampleWatchFaceService(): WatchFaceService {

    class ExampleCanvasWatchFaceService : WatchFaceService() {
        override fun createWatchFace(
            surfaceHolder: SurfaceHolder,
            systemApi: SystemApi,
            systemState: SystemState
        ): WatchFace {
            val styleManager = UserStyleManager(
                listOf(
                    ListViewUserStyleCategory(
                        "color_style_category",
                        "Colors",
                        "Watchface colorization",
                        icon = null,
                        options = listOf(
                            ListViewUserStyleCategory.ListViewOption(
                                "red_style",
                                "Red",
                                icon = null
                            ),
                            ListViewUserStyleCategory.ListViewOption(
                                "green_style",
                                "Green",
                                icon = null
                            ),
                            ListViewUserStyleCategory.ListViewOption(
                                "bluestyle",
                                "Blue",
                                icon = null
                            )
                        )
                    ),
                    ListViewUserStyleCategory(
                        "hand_style_category",
                        "Hand Style",
                        "Hand visual look",
                        icon = null,
                        options = listOf(
                            ListViewUserStyleCategory.ListViewOption(
                                "classic_style", "Classic", icon = null
                            ),
                            ListViewUserStyleCategory.ListViewOption(
                                "modern_style", "Modern", icon = null
                            ),
                            ListViewUserStyleCategory.ListViewOption(
                                "gothic_style",
                                "Gothic",
                                icon = null
                            )
                        )
                    )
                )
            )
            val complicationSlots = ComplicationSlots(
                listOf(
                    Complication(
                        /*id */ 0,
                        FixedBounds(RectF(0.15625f, 0.1875f, 0.84375f, 0.3125f)),
                        ComplicationDrawableRenderer(
                            ComplicationDrawable(this),
                            systemState
                        ),
                        intArrayOf(
                            ComplicationData.TYPE_RANGED_VALUE,
                            ComplicationData.TYPE_LONG_TEXT,
                            ComplicationData.TYPE_SHORT_TEXT,
                            ComplicationData.TYPE_ICON,
                            ComplicationData.TYPE_SMALL_IMAGE
                        ),
                        Complication.DefaultComplicationProvider(SystemProviders.DAY_OF_WEEK),
                        ComplicationData.TYPE_SHORT_TEXT
                    ),
                    Complication(
                        /*id */ 1,
                        FixedBounds(
                            RectF(0.1f, 0.5625f, 0.35f, 0.8125f)
                        ),
                        ComplicationDrawableRenderer(
                            ComplicationDrawable(this),
                            systemState
                        ),
                        intArrayOf(
                            ComplicationData.TYPE_RANGED_VALUE,
                            ComplicationData.TYPE_LONG_TEXT,
                            ComplicationData.TYPE_SHORT_TEXT,
                            ComplicationData.TYPE_ICON,
                            ComplicationData.TYPE_SMALL_IMAGE
                        ),
                        Complication.DefaultComplicationProvider(SystemProviders.STEP_COUNT),
                        ComplicationData.TYPE_SHORT_TEXT
                    )
                )
            )

            val renderer = object : CanvasRenderer(
                surfaceHolder,
                styleManager,
                systemState,
                CanvasType.HARDWARE
            ) {
                init {
                    styleManager.addUserStyleListener(object : UserStyleManager.UserStyleListener {
                        override fun onUserStyleChanged(
                            userStyle: Map<UserStyleCategory, UserStyleCategory.Option>
                        ) {
                            // `userStyle` will contain two userStyle categories with options from
                            // the lists above. ...
                        }
                    })
                }

                override fun onDraw(
                    canvas: Canvas,
                    bounds: Rect,
                    calendar: Calendar
                ) {
                    // ...
                }
            }

            return object : WatchFace(
                WatchFaceType.ANALOG,
                /* interactiveUpdateRateMillis */ 16,
                styleManager,
                complicationSlots,
                renderer,
                systemApi,
                systemState
            ) {}
        }
    }

    return ExampleCanvasWatchFaceService()
}