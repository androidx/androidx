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
import android.os.Handler
import android.view.SurfaceHolder
import androidx.wear.watchface.MutableWatchState
import androidx.wear.watchface.WatchFace
import androidx.wear.watchface.WatchFaceService
import androidx.wear.watchface.WatchState
import androidx.wear.watchface.samples.createExampleOpenGLWatchFaceBuilder

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
        watchState: WatchState
    ): WatchFace {
        // Override is necessary because the watch face isn't visible in this test.
        mutableWatchState.isVisible.value = true
        return createExampleOpenGLWatchFaceBuilder(
            this,
            surfaceHolder,
            watchState
        ).setSystemTimeProvider(object : WatchFace.SystemTimeProvider {
            override fun getSystemTimeMillis(): Long {
                return mockSystemTimeMillis
            }
        })
    }

    override fun getMutableWatchState() = mutableWatchState

    override fun getHandler() = handler

    // We want full control over when frames are produced.
    override fun allowWatchFaceToAnimate() = false

    override fun getWallpaperSurfaceHolderOverride() = surfacHolderOverride
}
