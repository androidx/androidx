/*
 * Copyright 2022 The Android Open Source Project
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
import android.graphics.Canvas
import android.graphics.Rect
import android.os.Build
import android.view.SurfaceHolder
import androidx.annotation.RequiresApi
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.wear.watchface.client.DeviceConfig
import androidx.wear.watchface.client.WatchUiState
import androidx.wear.watchface.style.CurrentUserStyleRepository
import com.google.common.truth.Truth.assertThat
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.async
import org.junit.Test
import org.junit.runner.RunWith

public class TestSharedAssets : Renderer.SharedAssets {
    override fun onDestroy() {}
}

internal class TestAsyncCanvasRenderWithSharedAssetsTestWatchFaceService(
    testContext: Context,
    private var surfaceHolderOverride: SurfaceHolder,
    private var initFuture: ListenableFuture<Unit>,
    private var sharedAssetsFuture: ListenableFuture<TestSharedAssets>
) : WatchFaceService() {

    val lock = Any()
    val initFutureLatch = CountDownLatch(1)
    val sharedAssetsFutureLatch = CountDownLatch(1)
    var hasRendered = false
    lateinit var sharedAssetsPassedToRenderer: Renderer.SharedAssets

    init {
        attachBaseContext(testContext)
    }

    override fun forceIsVisibleForTesting() = true

    override fun getWallpaperSurfaceHolderOverride() = surfaceHolderOverride

    override suspend fun createWatchFace(
        surfaceHolder: SurfaceHolder,
        watchState: WatchState,
        complicationSlotsManager: ComplicationSlotsManager,
        currentUserStyleRepository: CurrentUserStyleRepository
    ) =
        WatchFace(
            WatchFaceType.DIGITAL,
            object :
                ListenableCanvasRenderer2<TestSharedAssets>(
                    surfaceHolder,
                    currentUserStyleRepository,
                    watchState,
                    CanvasType.HARDWARE,
                    16
                ) {
                override fun initFuture(): ListenableFuture<Unit> {
                    initFutureLatch.countDown()
                    return initFuture
                }

                override fun createSharedAssetsFuture(): ListenableFuture<TestSharedAssets> {
                    sharedAssetsFutureLatch.countDown()
                    return sharedAssetsFuture
                }

                override fun render(
                    canvas: Canvas,
                    bounds: Rect,
                    zonedDateTime: ZonedDateTime,
                    sharedAssets: TestSharedAssets
                ) {
                    // Actually rendering something isn't required.
                    synchronized(lock) {
                        hasRendered = true
                        sharedAssetsPassedToRenderer = sharedAssets
                    }
                }

                override fun renderHighlightLayer(
                    canvas: Canvas,
                    bounds: Rect,
                    zonedDateTime: ZonedDateTime,
                    sharedAssets: TestSharedAssets
                ) {
                    // NOP
                }
            }
        )

    override fun getSystemTimeProvider() =
        object : SystemTimeProvider {
            override fun getSystemTimeMillis() = 123456789L

            override fun getSystemTimeZoneId() = ZoneId.of("UTC")
        }
}

@MediumTest
@RequiresApi(Build.VERSION_CODES.O_MR1)
@RunWith(AndroidJUnit4::class)
public class AsyncListenableCanvasRenderer2Test : WatchFaceControlClientServiceTest() {
    @Test
    public fun asyncTest() {
        val testSharedAssets = TestSharedAssets()
        val initFuture = SettableFuture.create<Unit>()
        val sharedAssetsFuture = SettableFuture.create<TestSharedAssets>()
        val watchFaceService =
            TestAsyncCanvasRenderWithSharedAssetsTestWatchFaceService(
                context,
                surfaceHolder,
                initFuture,
                sharedAssetsFuture
            )
        val controlClient = createWatchFaceControlClientService()

        val deferredClient =
            handlerCoroutineScope.async {
                @Suppress("deprecation")
                controlClient.getOrCreateInteractiveWatchFaceClient(
                    "testId",
                    DeviceConfig(false, false, 0, 0),
                    WatchUiState(false, 0),
                    null,
                    emptyMap()
                )
            }

        handler.post { watchFaceService.onCreateEngine() }

        val client = awaitWithTimeout(deferredClient)

        try {
            assertThat(
                    watchFaceService.sharedAssetsFutureLatch.await(
                        TIMEOUT_MILLIS,
                        TimeUnit.MILLISECONDS
                    )
                )
                .isTrue()
            sharedAssetsFuture.set(testSharedAssets)

            assertThat(
                    watchFaceService.initFutureLatch.await(TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)
                )
                .isTrue()

            synchronized(watchFaceService.lock) {
                assertThat(watchFaceService.hasRendered).isFalse()
            }

            initFuture.set(Unit)

            assertThat(renderLatch.await(TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)).isTrue()
            assertThat(watchFaceService.sharedAssetsPassedToRenderer).isEqualTo(testSharedAssets)
        } finally {
            // Make sure we don't deadlock in case of a timeout which aborts the test mid way
            // leaving this future incomplete.
            initFuture.set(Unit)
            sharedAssetsFuture.set(testSharedAssets)
            client.close()
        }
    }
}
