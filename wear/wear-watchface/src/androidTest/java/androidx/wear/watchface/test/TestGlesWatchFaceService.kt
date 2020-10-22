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
import androidx.wear.complications.DefaultComplicationProviderPolicy
import androidx.wear.complications.SystemProviders
import androidx.wear.watchface.Complication
import androidx.wear.watchface.ComplicationsManager
import androidx.wear.watchface.MutableWatchState
import androidx.wear.watchface.WatchFace
import androidx.wear.watchface.WatchFaceHost
import androidx.wear.watchface.WatchFaceService
import androidx.wear.watchface.WatchFaceType
import androidx.wear.watchface.WatchState
import androidx.wear.watchface.samples.EXAMPLE_OPENGL_COMPLICATION_ID
import androidx.wear.watchface.samples.ExampleOpenGLRenderer
import androidx.wear.watchface.samples.R
import androidx.wear.watchface.samples.WatchFaceColorStyle
import androidx.wear.watchface.style.Layer
import androidx.wear.watchface.style.ListUserStyleCategory
import androidx.wear.watchface.style.UserStyleRepository

/** A simple OpenGL test watch face for integration tests. */
internal class TestGlesWatchFaceService(
    testContext: Context,
    private val handler: Handler,
    var mockSystemTimeMillis: Long,
    var surfacHolderOverride: SurfaceHolder?
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

        val watchFaceStyle = WatchFaceColorStyle.create(this, "white_style")
        val colorStyleCategory = ListUserStyleCategory(
            "color_style_category",
            "Colors",
            "Watchface colorization",
            icon = null,
            options = listOf(
                ListUserStyleCategory.ListOption(
                    "red_style",
                    "Red",
                    Icon.createWithResource(this, R.drawable.red_style)
                ),
                ListUserStyleCategory.ListOption(
                    "green_style",
                    "Green",
                    Icon.createWithResource(this, R.drawable.green_style)
                )
            ),
            listOf(Layer.BASE_LAYER, Layer.TOP_LAYER)
        )
        val userStyleRepository = UserStyleRepository(listOf(colorStyleCategory))
        val complicationSlots = ComplicationsManager(
            listOf(
                Complication.Builder(
                    EXAMPLE_OPENGL_COMPLICATION_ID,
                    watchFaceStyle.getComplicationDrawableRenderer(this, watchState),
                    intArrayOf(
                        ComplicationData.TYPE_RANGED_VALUE,
                        ComplicationData.TYPE_LONG_TEXT,
                        ComplicationData.TYPE_SHORT_TEXT,
                        ComplicationData.TYPE_ICON,
                        ComplicationData.TYPE_SMALL_IMAGE
                    ),
                    DefaultComplicationProviderPolicy(SystemProviders.DAY_OF_WEEK)
                ).setUnitSquareBounds(RectF(0.2f, 0.7f, 0.4f, 0.9f))
                    .setDefaultProviderType(ComplicationData.TYPE_SHORT_TEXT)
                    .build()
            ),
            userStyleRepository
        )
        val renderer = ExampleOpenGLRenderer(
            surfaceHolder,
            userStyleRepository,
            watchState,
            colorStyleCategory,
            complicationSlots[EXAMPLE_OPENGL_COMPLICATION_ID]!!
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
