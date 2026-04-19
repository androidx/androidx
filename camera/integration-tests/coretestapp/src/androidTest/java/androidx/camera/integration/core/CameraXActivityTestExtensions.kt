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

import android.view.View
import androidx.camera.integration.core.util.StressTestUtil.VIDEO_CAPTURE_AUTO_STOP_LENGTH_MS
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso
import androidx.test.espresso.PerformException
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.util.HumanReadables
import androidx.test.platform.app.InstrumentationRegistry
import androidx.testutils.withActivity
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import org.hamcrest.Matcher

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

/**
 * A custom ViewAction that forces a click on the view directly, bypassing physical touch coordinate
 * issues.
 */
fun forceClick(): ViewAction {
    return object : ViewAction {
        override fun getConstraints(): Matcher<View> {
            return isAssignableFrom(View::class.java)
        }

        override fun getDescription(): String {
            return "force click the view directly"
        }

        override fun perform(uiController: UiController, view: View) {
            uiController.loopMainThreadUntilIdle()
            if (!view.performClick()) {
                throw PerformException.Builder()
                    .withActionDescription(this.description)
                    .withViewDescription(HumanReadables.describe(view))
                    .withCause(RuntimeException("View.performClick() returned false"))
                    .build()
            }
            uiController.loopMainThreadUntilIdle()
        }
    }
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
    Espresso.onView(withId(R.id.direction_toggle)).perform(forceClick())

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
        Espresso.onView(withId(R.id.Picture)).apply {
            repeat(captureRequestsCount) { perform(forceClick()) }
        }

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

    Espresso.onView(withId(R.id.Video)).perform(forceClick())

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
