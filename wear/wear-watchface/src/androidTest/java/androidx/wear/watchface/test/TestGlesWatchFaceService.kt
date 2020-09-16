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
import android.graphics.drawable.Icon
import android.os.Handler
import android.view.SurfaceHolder
import androidx.wear.watchface.ComplicationsManager
import androidx.wear.watchface.MutableWatchState
import androidx.wear.watchface.NoInvalidateWatchFaceHostApi
import androidx.wear.watchface.WatchFace
import androidx.wear.watchface.WatchFaceHost
import androidx.wear.watchface.WatchFaceService
import androidx.wear.watchface.WatchFaceType
import androidx.wear.watchface.WatchState
import androidx.wear.watchface.samples.ExampleOpenGLRenderer
import androidx.wear.watchface.samples.R
import androidx.wear.watchface.style.ListUserStyleCategory
import androidx.wear.watchface.style.UserStyleCategory
import androidx.wear.watchface.style.UserStyleRepository

/** A simple OpenGL test watch face for integration tests. */
internal class TestGlesWatchFaceService(
    testContext: Context,
    private val handler: Handler,
    var mockSystemTimeMillis: Long
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
            UserStyleCategory.LAYER_WATCH_FACE_BASE or UserStyleCategory.LAYER_WATCH_FACE_UPPER
        )
        val userStyleRepository = UserStyleRepository(listOf(colorStyleCategory))
        val complicationSlots = ComplicationsManager(emptyList())
        val renderer = ExampleOpenGLRenderer(
            surfaceHolder,
            userStyleRepository,
            watchState,
            colorStyleCategory
        )

        return WatchFace.Builder(
            WatchFaceType.ANALOG,
            16,
            userStyleRepository,
            complicationSlots,
            renderer,
            // We want full control over when frames are produced.
            NoInvalidateWatchFaceHostApi.create(watchFaceHost),
            watchState
        ).setSystemTimeProvider(object : WatchFace.SystemTimeProvider {
            override fun getSystemTimeMillis(): Long {
                return mockSystemTimeMillis
            }
        }).build()
    }

    override fun getMutableWatchState() = mutableWatchState

    override fun getHandler() = handler
}
