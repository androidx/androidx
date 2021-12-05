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
import androidx.wear.watchface.ComplicationSlotsManager
import androidx.wear.watchface.MutableWatchState
import androidx.wear.watchface.WatchFace
import androidx.wear.watchface.WatchFaceService
import androidx.wear.watchface.WatchState
import androidx.wear.watchface.control.data.WallpaperInteractiveWatchFaceInstanceParams
import androidx.wear.watchface.samples.ExampleCanvasAnalogWatchFaceService
import androidx.wear.watchface.style.CurrentUserStyleRepository
import java.time.ZoneId

/** A simple canvas test analog watch face for integration tests. */
internal class TestCanvasAnalogWatchFaceService(
    testContext: Context,
    private val handler: Handler,
    var mockSystemTimeMillis: Long,
    var mockZoneId: ZoneId,
    var surfaceHolderOverride: SurfaceHolder,
    var preRInitFlow: Boolean,
    var directBootParams: WallpaperInteractiveWatchFaceInstanceParams?
) : WatchFaceService() {

    private val mutableWatchState = MutableWatchState()

    // We can't subclass ExampleCanvasAnalogWatchFaceService because we want to override internal
    // methods, so instead we use composition.
    private val delegate = object : ExampleCanvasAnalogWatchFaceService() {
        init {
            attachBaseContext(testContext)
        }
    }

    init {
        attachBaseContext(testContext)
    }

    override fun createUserStyleSchema() = delegate.createUserStyleSchema()

    override fun createComplicationSlotsManager(
        currentUserStyleRepository: CurrentUserStyleRepository
    ) = delegate.createComplicationSlotsManager(currentUserStyleRepository)

    override suspend fun createWatchFace(
        surfaceHolder: SurfaceHolder,
        watchState: WatchState,
        complicationSlotsManager: ComplicationSlotsManager,
        currentUserStyleRepository: CurrentUserStyleRepository
    ): WatchFace {
        // Override is necessary because the watch face isn't visible in this test.
        mutableWatchState.isVisible.value = true
        return delegate.createWatchFace(
            surfaceHolder,
            watchState,
            complicationSlotsManager,
            currentUserStyleRepository
        ).setSystemTimeProvider(object : WatchFace.SystemTimeProvider {
            override fun getSystemTimeMillis() = mockSystemTimeMillis

            override fun getSystemTimeZoneId() = mockZoneId
        })
    }

    override fun getMutableWatchState() = mutableWatchState

    override fun getUiThreadHandlerImpl() = handler

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

    override fun readComplicationDataCacheByteArray(
        context: Context,
        fileName: String
    ): ByteArray? = null

    override fun writeComplicationDataCacheByteArray(
        context: Context,
        fileName: String,
        byteArray: ByteArray
    ) {}
}
