/*
 * Copyright 2021 The Android Open Source Project
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

import android.graphics.Canvas
import android.graphics.Rect
import android.icu.util.Calendar
import android.view.SurfaceHolder
import androidx.wear.watchface.CanvasType
import androidx.wear.watchface.ComplicationsManager
import androidx.wear.watchface.ListenableWatchFaceService
import androidx.wear.watchface.MutableWatchState
import androidx.wear.watchface.Renderer
import androidx.wear.watchface.WatchFace
import androidx.wear.watchface.WatchFaceType
import androidx.wear.watchface.WatchState
import androidx.wear.watchface.style.CurrentUserStyleRepository
import androidx.wear.watchface.style.UserStyleSchema
import com.google.common.truth.Truth.assertThat
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.mockito.Mockito

private const val REFERENCE_PREVIEW_TIME = 123456L

private class FakeRenderer(
    surfaceHolder: SurfaceHolder,
    watchState: WatchState,
    currentUserStyleRepository: CurrentUserStyleRepository
) : Renderer.CanvasRenderer(
    surfaceHolder,
    currentUserStyleRepository,
    watchState,
    CanvasType.SOFTWARE,
    16
) {
    override fun render(canvas: Canvas, bounds: Rect, calendar: Calendar) {}

    override fun renderHighlightLayer(canvas: Canvas, bounds: Rect, calendar: Calendar) {}
}

private class TestAsyncListenableWatchFaceService :
    ListenableWatchFaceService() {
    override fun createWatchFaceFuture(
        surfaceHolder: SurfaceHolder,
        watchState: WatchState,
        complicationsManager: ComplicationsManager,
        currentUserStyleRepository: CurrentUserStyleRepository
    ): ListenableFuture<WatchFace> {
        val future = SettableFuture.create<WatchFace>()
        // Post a task to resolve the future.
        getUiThreadHandler().post {
            future.set(
                WatchFace(
                    WatchFaceType.DIGITAL,
                    FakeRenderer(surfaceHolder, watchState, currentUserStyleRepository)
                ).apply { setOverridePreviewReferenceTimeMillis(REFERENCE_PREVIEW_TIME) }
            )
        }
        return future
    }

    suspend fun createWatchFaceForTest(
        surfaceHolder: SurfaceHolder,
        watchState: WatchState,
        complicationsManager: ComplicationsManager,
        currentUserStyleRepository: CurrentUserStyleRepository
    ): WatchFace =
        createWatchFace(surfaceHolder, watchState, complicationsManager, currentUserStyleRepository)
}

/**
 * Illustrates that createWatchFaceFuture can be resolved in a different task posted to the main
 * looper.
 */
public class AsyncListenableWatchFaceServiceTest {

    @Test
    public fun asyncTest() {
        val service = TestAsyncListenableWatchFaceService()
        val mockSurfaceHolder = Mockito.mock(SurfaceHolder::class.java)
        Mockito.`when`(mockSurfaceHolder.surfaceFrame).thenReturn(Rect(0, 0, 100, 100))

        runBlocking {
            val currentUserStyleRepository =
                CurrentUserStyleRepository(UserStyleSchema(emptyList()))
            val complicationsManager = ComplicationsManager(emptyList(), currentUserStyleRepository)
            val watchFace = service.createWatchFaceForTest(
                mockSurfaceHolder,
                MutableWatchState().asWatchState(),
                complicationsManager,
                currentUserStyleRepository
            )

            // Simple check that [watchFace] looks sensible.
            assertThat(watchFace.overridePreviewReferenceTimeMillis).isEqualTo(
                REFERENCE_PREVIEW_TIME
            )
        }
    }
}