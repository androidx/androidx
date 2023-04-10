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

package androidx.wear.watchface

import android.graphics.Canvas
import android.graphics.Rect
import android.view.SurfaceHolder
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.wear.watchface.style.CurrentUserStyleRepository
import androidx.wear.watchface.style.UserStyleSchema
import com.google.common.truth.Truth.assertThat
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import java.time.Instant
import java.time.ZonedDateTime
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

private val REFERENCE_PREVIEW_TIME = Instant.ofEpochMilli(123456L)

@Suppress("Deprecation")
internal class FakeRenderer(
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
    override fun render(canvas: Canvas, bounds: Rect, zonedDateTime: ZonedDateTime) {}

    override fun renderHighlightLayer(canvas: Canvas, bounds: Rect, zonedDateTime: ZonedDateTime) {}
}

private class TestAsyncListenableWatchFaceService :
    ListenableWatchFaceService() {
    override fun createWatchFaceFuture(
        surfaceHolder: SurfaceHolder,
        watchState: WatchState,
        complicationSlotsManager: ComplicationSlotsManager,
        currentUserStyleRepository: CurrentUserStyleRepository
    ): ListenableFuture<WatchFace> {
        val future = SettableFuture.create<WatchFace>()
        // Post a task to resolve the future.
        getUiThreadHandler().post {
            future.set(
                WatchFace(
                    WatchFaceType.DIGITAL,
                    FakeRenderer(surfaceHolder, watchState, currentUserStyleRepository)
                ).apply { setOverridePreviewReferenceInstant(REFERENCE_PREVIEW_TIME) }
            )
        }
        return future
    }

    fun createWatchFaceFutureForTest(
        surfaceHolder: SurfaceHolder,
        watchState: WatchState,
        complicationSlotsManager: ComplicationSlotsManager,
        currentUserStyleRepository: CurrentUserStyleRepository
    ) = createWatchFaceFuture(
        surfaceHolder,
        watchState,
        complicationSlotsManager,
        currentUserStyleRepository
    )
}

/**
 * Illustrates that createWatchFaceFuture can be resolved in a different task posted to the main
 * looper.
 */
@RunWith(AndroidJUnit4::class)
@MediumTest
public class AsyncListenableWatchFaceServiceTest {

    @Test
    public fun asyncTest() {
        val service = TestAsyncListenableWatchFaceService()
        val mockSurfaceHolder = Mockito.mock(SurfaceHolder::class.java)
        Mockito.`when`(mockSurfaceHolder.surfaceFrame).thenReturn(Rect(0, 0, 100, 100))

        val currentUserStyleRepository =
            CurrentUserStyleRepository(UserStyleSchema(emptyList()))
        val complicationSlotsManager =
            ComplicationSlotsManager(emptyList(), currentUserStyleRepository)
        val future = service.createWatchFaceFutureForTest(
            mockSurfaceHolder,
            MutableWatchState().asWatchState(),
            complicationSlotsManager,
            currentUserStyleRepository
        )

        val latch = CountDownLatch(1)
        future.addListener(
            {
                latch.countDown()
            },
            { runnable -> runnable.run() }
        )

        Assert.assertTrue(latch.await(TIME_OUT_MILLIS, TimeUnit.MILLISECONDS))

        val watchFace = future.get()

        // Simple check that [watchFace] looks sensible.
        assertThat(watchFace.overridePreviewReferenceInstant).isEqualTo(
            REFERENCE_PREVIEW_TIME
        )
    }
}
