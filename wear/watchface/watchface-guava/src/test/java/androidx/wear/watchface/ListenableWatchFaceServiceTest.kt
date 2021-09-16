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
import androidx.wear.watchface.style.CurrentUserStyleRepository
import androidx.wear.watchface.style.UserStyleSchema
import com.google.common.truth.Truth.assertThat
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.model.FrameworkMethod
import org.mockito.Mockito
import org.mockito.Mockito.`when`
import org.robolectric.RobolectricTestRunner
import org.robolectric.internal.bytecode.InstrumentationConfiguration
import java.time.Instant
import java.time.ZonedDateTime

private val REFERENCE_PREVIEW_TIME = Instant.ofEpochMilli(123456L)

private class TestListenableWatchFaceService : ListenableWatchFaceService() {
    override fun createWatchFaceFuture(
        surfaceHolder: SurfaceHolder,
        watchState: WatchState,
        complicationSlotsManager: ComplicationSlotsManager,
        currentUserStyleRepository: CurrentUserStyleRepository
    ): ListenableFuture<WatchFace> {
        return Futures.immediateFuture(
            WatchFace(
                WatchFaceType.DIGITAL,
                object : Renderer.CanvasRenderer(
                    surfaceHolder,
                    currentUserStyleRepository,
                    watchState,
                    CanvasType.SOFTWARE,
                    16
                ) {
                    override fun render(
                        canvas: Canvas,
                        bounds: Rect,
                        zonedDateTime: ZonedDateTime
                    ) {}

                    override fun renderHighlightLayer(
                        canvas: Canvas,
                        bounds: Rect,
                        zonedDateTime: ZonedDateTime
                    ) {}
                }
            ).apply { setOverridePreviewReferenceInstant(REFERENCE_PREVIEW_TIME) }
        )
    }

    suspend fun createWatchFaceForTest(
        surfaceHolder: SurfaceHolder,
        watchState: WatchState,
        complicationSlotsManager: ComplicationSlotsManager,
        currentUserStyleRepository: CurrentUserStyleRepository
    ): WatchFace = createWatchFace(
        surfaceHolder,
        watchState,
        complicationSlotsManager,
        currentUserStyleRepository
    )
}

@RunWith(ListenableWatchFaceServiceTestRunner::class)
public class ListenableWatchFaceServiceTest {
    @Test
    public fun overridePreviewReferenceTimeMillis() {
        val service = TestListenableWatchFaceService()
        val mockSurfaceHolder = Mockito.mock(SurfaceHolder::class.java)
        `when`(mockSurfaceHolder.surfaceFrame).thenReturn(Rect(0, 0, 100, 100))
        runBlocking {
            val currentUserStyleRepository =
                CurrentUserStyleRepository(UserStyleSchema(emptyList()))
            val complicationsSlotManager =
                ComplicationSlotsManager(emptyList(), currentUserStyleRepository)

            // Make sure the ListenableFuture<> to kotlin coroutine bridge works.
            val watchFace = service.createWatchFaceForTest(
                mockSurfaceHolder,
                MutableWatchState().asWatchState(),
                complicationsSlotManager,
                currentUserStyleRepository
            )

            // Simple check that [watchFace] looks sensible.
            assertThat(watchFace.overridePreviewReferenceInstant).isEqualTo(
                REFERENCE_PREVIEW_TIME
            )
        }
    }
}

// Without this we get test failures with an error:
// "failed to access class kotlin.jvm.internal.DefaultConstructorMarker".
public class ListenableWatchFaceServiceTestRunner(
    testClass: Class<*>
) : RobolectricTestRunner(testClass) {
    override fun createClassLoaderConfig(method: FrameworkMethod): InstrumentationConfiguration =
        InstrumentationConfiguration.Builder(
            super.createClassLoaderConfig(method)
        )
            .doNotInstrumentPackage("androidx.wear.watchface")
            .build()
}
