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

import android.content.Context
import android.graphics.Rect
import android.view.SurfaceHolder
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.wear.watchface.complications.rendering.CanvasComplicationDrawable
import androidx.wear.watchface.complications.rendering.ComplicationDrawable
import androidx.wear.watchface.guava.test.R
import androidx.wear.watchface.style.CurrentUserStyleRepository
import com.google.common.truth.Truth.assertThat
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import java.time.Instant
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

val TIME_OUT_MILLIS = 500L
private val REFERENCE_PREVIEW_TIME = Instant.ofEpochMilli(123456L)

private class TestAsyncXmlListenableWatchFaceService(
    testContext: Context
) : ListenableWatchFaceService() {

    init {
        attachBaseContext(testContext)
    }

    override fun getXmlWatchFaceResourceId() = R.xml.xml_watchface

    override fun getComplicationSlotInflationFactory() =
        object : ComplicationSlotInflationFactory() {
            override fun getCanvasComplicationFactory(
                slotId: Int
            ) = CanvasComplicationFactory { watchState, invalidateCallback ->
                CanvasComplicationDrawable(
                    ComplicationDrawable(),
                    watchState,
                    invalidateCallback
                )
            }
        }

    fun createUserStyleSchemaForTest() = createUserStyleSchema()

    fun createComplicationSlotsManagerForTest(
        currentUserStyleRepository: CurrentUserStyleRepository
    ) = createComplicationSlotsManager(currentUserStyleRepository)

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
}

@RunWith(AndroidJUnit4::class)
@MediumTest
public class AsyncXmlListenableWatchFaceServiceTest {

    @Test
    public fun asyncTest() {
        val service = TestAsyncXmlListenableWatchFaceService(
            ApplicationProvider.getApplicationContext<Context>()
        )
        val mockSurfaceHolder = Mockito.mock(SurfaceHolder::class.java)
        Mockito.`when`(mockSurfaceHolder.surfaceFrame).thenReturn(Rect(0, 0, 100, 100))

        val currentUserStyleRepository =
            CurrentUserStyleRepository(service.createUserStyleSchemaForTest())

        val complicationSlotsManager =
            service.createComplicationSlotsManagerForTest(currentUserStyleRepository)

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

        assertTrue(latch.await(TIME_OUT_MILLIS, TimeUnit.MILLISECONDS))

        val watchFace = future.get()

        // Simple check that [watchFace] looks sensible.
        assertThat(watchFace.overridePreviewReferenceInstant).isEqualTo(
            REFERENCE_PREVIEW_TIME)

        assertThat(currentUserStyleRepository.schema.toString()).isEqualTo(
            "[{TimeStyle : minimal, seconds}]"
        )

        assertThat(complicationSlotsManager.complicationSlots.size).isEqualTo(2)
    }
}
