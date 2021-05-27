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
import androidx.wear.watchface.ComplicationsManager
import androidx.wear.watchface.MutableWatchState
import androidx.wear.watchface.WatchFace
import androidx.wear.watchface.WatchFaceService
import androidx.wear.watchface.WatchState
import androidx.wear.watchface.control.data.WallpaperInteractiveWatchFaceInstanceParams
import androidx.wear.watchface.samples.ExampleOpenGLWatchFaceService
import androidx.wear.watchface.style.CurrentUserStyleRepository

/** A simple OpenGL test watch face for integration tests. */
internal class TestGlesWatchFaceService(
    testContext: Context,
    private val handler: Handler,
    var mockSystemTimeMillis: Long,
    var surfacHolderOverride: SurfaceHolder?,
    var directBootParams: WallpaperInteractiveWatchFaceInstanceParams?
) : WatchFaceService() {

    private val mutableWatchState = MutableWatchState()

    // We can't subclass ExampleOpenGLWatchFaceService because we want to override internal methods,
    // so instead we use composition.
    private val delegate = object : ExampleOpenGLWatchFaceService() {
        init {
            attachBaseContext(testContext)
        }
    }

    init {
        attachBaseContext(testContext)
    }

    override fun createUserStyleSchema() = delegate.createUserStyleSchema()

    override fun createComplicationsManager(
        currentUserStyleRepository: CurrentUserStyleRepository
    ) = delegate.createComplicationsManager(currentUserStyleRepository)

    override suspend fun createWatchFace(
        surfaceHolder: SurfaceHolder,
        watchState: WatchState,
        complicationsManager: ComplicationsManager,
        currentUserStyleRepository: CurrentUserStyleRepository
    ): WatchFace {
        // Override is necessary because the watch face isn't visible in this test.
        mutableWatchState.isVisible.value = true
        return delegate.createWatchFace(
            surfaceHolder,
            watchState,
            complicationsManager,
            currentUserStyleRepository
        ).setSystemTimeProvider(object : WatchFace.SystemTimeProvider {
            override fun getSystemTimeMillis(): Long {
                return mockSystemTimeMillis
            }
        })
    }

    override fun getMutableWatchState() = mutableWatchState

    override fun getUiThreadHandlerImpl() = handler

    // We want full control over when frames are produced.
    override fun allowWatchFaceToAnimate() = false

    override fun getWallpaperSurfaceHolderOverride() = surfacHolderOverride

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
