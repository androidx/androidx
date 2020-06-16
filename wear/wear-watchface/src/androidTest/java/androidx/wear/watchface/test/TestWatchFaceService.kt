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
import android.support.wearable.complications.ComplicationData
import android.view.SurfaceHolder
import androidx.wear.complications.SystemProviders
import androidx.wear.watchface.Complication
import androidx.wear.watchface.ComplicationSlots
import androidx.wear.watchface.FixedBounds
import androidx.wear.watchface.SystemApi
import androidx.wear.watchface.SystemState
import androidx.wear.watchface.WatchFace
import androidx.wear.watchface.WatchFaceService
import androidx.wear.watchface.WatchFaceType
import androidx.wear.watchface.samples.EXAMPLE_CANVAS_WATCHFACE_LEFT_COMPLICATION_ID
import androidx.wear.watchface.samples.EXAMPLE_CANVAS_WATCHFACE_RIGHT_COMPLICATION_ID
import androidx.wear.watchface.samples.ExampleCanvasRenderer
import androidx.wear.watchface.samples.R
import androidx.wear.watchface.samples.WatchFaceStyle
import androidx.wear.watchfacestyle.ListViewUserStyleCategory
import androidx.wear.watchfacestyle.UserStyleManager

/** A simple test watch face for integration tests. */
internal class TestWatchFaceService(
    testContext: Context,
    private val handler: Handler,
    var mockSystemTimeMillis: Long
) : WatchFaceService() {

    init {
        attachBaseContext(testContext)
    }

    override fun createWatchFace(
        surfaceHolder: SurfaceHolder,
        systemApi: SystemApi,
        systemState: SystemState
    ): WatchFace {
        // Override is necessary because the watch face isn't visible in this test.
        systemState.onVisibilityChanged(true)

        val watchFaceStyle = WatchFaceStyle.create(this, "red_style")
        val colorStyleCategory = ListViewUserStyleCategory(
            "color_style_category",
            "Colors",
            "Watchface colorization",
            icon = null,
            options = listOf(
                ListViewUserStyleCategory.ListViewOption(
                    "red_style",
                    "Red",
                    Icon.createWithResource(this, R.drawable.red_style)
                ),
                ListViewUserStyleCategory.ListViewOption(
                    "green_style",
                    "Green",
                    Icon.createWithResource(this, R.drawable.green_style)
                )
            )
        )
        val styleManager = UserStyleManager(
            listOf(colorStyleCategory)
        )
        val complicationSlots = ComplicationSlots(
            listOf(
                Complication(
                    EXAMPLE_CANVAS_WATCHFACE_LEFT_COMPLICATION_ID,
                    FixedBounds(RectF(0.2f, 0.4f, 0.4f, 0.6f)),
                    watchFaceStyle.getComplicationDrawableRenderer(this, systemState),
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
                    EXAMPLE_CANVAS_WATCHFACE_RIGHT_COMPLICATION_ID,
                    FixedBounds(RectF(0.6f, 0.4f, 0.8f, 0.6f)),
                    watchFaceStyle.getComplicationDrawableRenderer(this, systemState),
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
        val renderer = ExampleCanvasRenderer(
            surfaceHolder,
            this,
            watchFaceStyle,
            styleManager,
            systemState,
            colorStyleCategory,
            complicationSlots
        )

        return object : WatchFace(
            WatchFaceType.ANALOG,
            /** mInteractiveUpdateRateMillis */
            16,
            styleManager,
            complicationSlots,
            renderer,
            systemApi,
            systemState
        ) {
            override fun getSystemTimeMillis() = mockSystemTimeMillis

            override fun invalidate() {
                // Do nothing. We don't want unexpected rendering.
            }
        }
    }

    override fun getHandler() = handler
}
