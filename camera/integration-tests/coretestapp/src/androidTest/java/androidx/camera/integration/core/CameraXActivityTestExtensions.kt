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

package androidx.camera.integration.core

import android.view.SurfaceView
import android.view.TextureView
import android.view.View
import androidx.camera.integration.core.util.StressTestUtil.VIDEO_CAPTURE_AUTO_STOP_LENGTH_MS
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.platform.app.InstrumentationRegistry
import androidx.testutils.withActivity
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking

private const val DEFAULT_TIMEOUT_SECONDS = 30L

/** Helper to assert that a latch completes within the standard timeout. */
private fun CountDownLatch.awaitOrThrow(
    timeout: Long = DEFAULT_TIMEOUT_SECONDS,
    message: String = "Timed out waiting for latch",
) {
    assertWithMessage(message).that(await(timeout, TimeUnit.SECONDS)).isTrue()
}

/**
 * Extension to wait for a specific view to be fully interactive at the OS level. This ensures the
 * Window has focus and the view is ready to receive touch events, preventing the system from
 * swallowing clicks during lifecycle transitions.
 */
/** Tiered check: Focus is required for Clicks, but not for Preview frames. */
internal fun ActivityScenario<CameraXActivity>.waitUntilViewReady(
    viewId: Int,
    requireFocus: Boolean = true,
    timeoutMs: Long = 10000L,
) {
    val deadline = System.currentTimeMillis() + timeoutMs
    var isReady = false

    while (System.currentTimeMillis() < deadline) {
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()

        onActivity { activity ->
            val view = activity.findViewById<View>(viewId)
            val baseReady =
                view != null && view.isAttachedToWindow && view.visibility == View.VISIBLE

            // Only require focus if we intend to perform an Input event (click)
            isReady =
                if (requireFocus) {
                    baseReady && activity.hasWindowFocus() && view.isEnabled
                } else {
                    baseReady
                }
        }

        if (isReady) return
        Thread.yield()
    }

    throw TimeoutException("View $viewId ready=$isReady focus required:$requireFocus")
}

/** Waits until the viewfinder has received frames. */
internal fun ActivityScenario<CameraXActivity>.waitForViewfinderIdle() {
    // Ensure the UI thread has processed onCreate/onResume and layout passes
    waitUntilViewReady(R.id.viewFinder, requireFocus = false)

    val latch = withActivity { resetViewIdlingLatch() }
    latch.awaitOrThrow(message = "Viewfinder failed to receive frames.")

    Espresso.onView(withId(R.id.viewFinder)).check(matches(isDisplayed()))
}

/** Handles switching cameras and waiting for the new stream. */
internal fun ActivityScenario<CameraXActivity>.switchCameraAndWaitForViewfinderIdle() {
    // Ensure the UI thread has processed onCreate/onResume and layout passes
    waitUntilViewReady(R.id.direction_toggle)

    // 1. Ensure current state is stable before clicking
    waitForViewfinderIdle()

    // 2. Perform toggle
    clickView(R.id.direction_toggle)

    // 3. Wait for the new camera stream
    val latch = withActivity { resetViewIdlingLatch() }
    latch.awaitOrThrow(message = "Viewfinder failed to restart after camera switch.")
}

/** Issues capture requests and waits for them to be saved. */
internal fun ActivityScenario<CameraXActivity>.takePictureAndWaitForImageSavedIdle(
    captureRequestsCount: Int = 1
) {
    // Ensure the UI thread has processed onCreate/onResume and layout passes
    waitUntilViewReady(R.id.Picture)

    val latch = withActivity {
        cleanTakePictureErrorMessage()
        resetImageSavedIdlingLatch(captureRequestsCount)
    }

    try {
        repeat(captureRequestsCount) { clickView(R.id.Picture) }

        latch.awaitOrThrow(
            message = "Captured images failed to save within $DEFAULT_TIMEOUT_SECONDS seconds."
        )
    } finally {
        withActivity {
            val error = lastTakePictureErrorMessage
            deleteSessionImages()
            if (error != null) throw Exception("Image capture error: $error")
        }
    }
}

/** Waits until ImageAnalysis receives the required frames. */
internal fun ActivityScenario<CameraXActivity>.waitForImageAnalysisIdle() {
    val latch = withActivity { resetAnalysisIdlingLatch() }
    latch.awaitOrThrow(message = "Image analysis failed to receive required frames.")

    Espresso.onView(withId(R.id.textView)).check(matches(isDisplayed()))
}

/** Records a video and waits for it to be saved. */
internal fun ActivityScenario<CameraXActivity>.recordVideoAndWaitForVideoSavedIdle() {
    // Ensure the UI thread has processed onCreate/onResume and layout passes
    waitUntilViewReady(R.id.Video)

    val latch = withActivity {
        assertThat(videoCapture).isNotNull()
        cleanVideoRecordingErrorMessage()
        setVideoCaptureAutoStopLength(VIDEO_CAPTURE_AUTO_STOP_LENGTH_MS)
        resetVideoSavedIdlingLatch()
    }

    clickView(R.id.Video)

    try {
        latch.awaitOrThrow(timeout = 45L, message = "Video failed to record and save.")
    } finally {
        withActivity {
            val error = lastVideoRecordingErrorMessage
            deleteSessionVideos()
            if (error != null) throw Exception("Video recording error: $error")
        }
    }
}

/** Clicks a view directly on the UI thread, bypassing Espresso's touch coordinate issues. */
internal fun ActivityScenario<CameraXActivity>.clickView(viewId: Int) {
    waitUntilViewReady(viewId, requireFocus = true)
    onActivity { activity ->
        val view = activity.findViewById<View>(viewId)
        assertWithMessage("View $viewId not found").that(view).isNotNull()
        if (!view!!.performClick()) {
            throw RuntimeException("Failed to click view $viewId")
        }
    }
    InstrumentationRegistry.getInstrumentation().waitForIdleSync()
}

/** Waits until the viewfinder has received frames without using Espresso. */
internal fun ActivityScenario<CameraXActivity>.waitForViewfinderIdleDirect() {
    // Ensure the UI thread has processed onCreate/onResume and layout passes
    waitUntilViewReady(R.id.viewFinder, requireFocus = false)

    val initLatch = withActivity { initializationIdlingLatch }
    initLatch.awaitOrThrow(message = "Activity initialization failed.")

    waitForSurfaceValid()

    val latch = withActivity { resetViewIdlingLatch() }
    latch.awaitOrThrow(message = "Viewfinder failed to receive frames.")

    onActivity { activity ->
        val viewFinder = activity.findViewById<View>(R.id.viewFinder)
        assertWithMessage("Viewfinder is not displayed")
            .that(viewFinder != null && viewFinder.visibility == View.VISIBLE)
            .isTrue()
    }
}

/** Handles switching cameras and waiting for the new stream without using Espresso. */
internal fun ActivityScenario<CameraXActivity>.switchCameraAndWaitForViewfinderIdleDirect() {
    // Ensure the UI thread has processed onCreate/onResume and layout passes
    waitUntilViewReady(R.id.direction_toggle)

    // 1. Ensure current state is stable before clicking
    waitForViewfinderIdleDirect()

    // 2. Perform toggle
    clickView(R.id.direction_toggle)

    // 3. Wait for the new camera stream
    waitForSurfaceValid()

    val latch = withActivity { resetViewIdlingLatch() }
    latch.awaitOrThrow(message = "Viewfinder failed to restart after camera switch.")
}

/** Waits until the viewfinder surface is valid. */
internal fun ActivityScenario<CameraXActivity>.waitForSurfaceValid(timeoutMs: Long = 10000L) =
    runBlocking {
        val deadline = System.currentTimeMillis() + timeoutMs
        var isValid = false
        while (System.currentTimeMillis() < deadline) {
            onActivity { activity ->
                val viewFinder = activity.findViewById<View>(R.id.viewFinder)
                isValid =
                    when (viewFinder) {
                        is SurfaceView -> viewFinder.holder.surface.isValid
                        is TextureView -> viewFinder.isAvailable
                        else -> false
                    }
            }
            if (isValid) return@runBlocking
            delay(50.milliseconds)
        }
        throw TimeoutException("Surface failed to become valid.")
    }

/** Issues capture requests and waits for them to be saved without using Espresso. */
internal fun ActivityScenario<CameraXActivity>.takePictureAndWaitForImageSavedIdleDirect(
    captureRequestsCount: Int = 1
) {
    // Ensure the UI thread has processed onCreate/onResume and layout passes
    waitUntilViewReady(R.id.Picture)

    val latch = withActivity {
        cleanTakePictureErrorMessage()
        resetImageSavedIdlingLatch(captureRequestsCount)
    }

    try {
        repeat(captureRequestsCount) { clickView(R.id.Picture) }

        latch.awaitOrThrow(
            message = "Captured images failed to save within $DEFAULT_TIMEOUT_SECONDS seconds."
        )
    } finally {
        withActivity {
            val error = lastTakePictureErrorMessage
            deleteSessionImages()
            if (error != null) throw Exception("Image capture error: $error")
        }
    }
}
