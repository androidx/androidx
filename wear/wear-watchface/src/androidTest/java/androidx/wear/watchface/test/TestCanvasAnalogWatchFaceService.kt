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
import androidx.wear.watchface.control.data.WallpaperInteractiveWatchFaceInstanceParams
import androidx.wear.watchface.samples.createExampleCanvasAnalogWatchFaceBuilder

/** A simple canvas test analog watch face for integration tests. */
internal class TestCanvasAnalogWatchFaceService(
    testContext: Context,
    private val handler: Handler,
    var mockSystemTimeMillis: Long,
    var surfaceHolderOverride: SurfaceHolder,
    var preRInitFlow: Boolean,
    var directBootParams: WallpaperInteractiveWatchFaceInstanceParams?
) : WatchFaceService() {

    private val mutableWatchState = MutableWatchState()

    init {
        attachBaseContext(testContext)
    }

    override suspend fun createWatchFace(
        surfaceHolder: SurfaceHolder,
        watchState: WatchState
    ): WatchFace {
        // Override is necessary because the watch face isn't visible in this test.
        mutableWatchState.isVisible.value = true
        return createExampleCanvasAnalogWatchFaceBuilder(
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

    override fun getWallpaperSurfaceHolderOverride() = surfaceHolderOverride

    override fun expectPreRInitFlow() = preRInitFlow

    override fun readDirectBootPrefs(
        context: Context,
        fileName: String
    ) = directBootParams

    override fun writeDirectBootPrefs(
        context: Context,
        fileName: String,
        prefs: WallpaperInteractiveWatchFaceInstanceParams
    ) {}
}
