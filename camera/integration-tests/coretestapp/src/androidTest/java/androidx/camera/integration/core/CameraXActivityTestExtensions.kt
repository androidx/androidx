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

import androidx.camera.integration.core.util.StressTestUtil.VIDEO_CAPTURE_AUTO_STOP_LENGTH_MS
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.testutils.withActivity
import com.google.common.truth.Truth.assertThat

/**
 * Waits until the viewfinder has received frames and its idling resource has become idle.
 */
internal fun ActivityScenario<CameraXActivity>.waitForViewfinderIdle() {
    val idlingResource = withActivity {
        resetViewIdlingResource()
        viewIdlingResource
    }
    try {
        IdlingRegistry.getInstance().register(idlingResource)
        // Check the activity launched and Preview displays frames.
        Espresso.onView(ViewMatchers.withId(R.id.viewFinder))
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
    } finally { // Always release the idling resource, in case of timeout exceptions.
        IdlingRegistry.getInstance().unregister(idlingResource)
    }
}
/**
 * Waits until the viewfinder has received frames and its idling resource has become idle.
 */
internal fun ActivityScenario<CameraXActivity>.switchCameraAndWaitForViewfinderIdle() {
    val idlingResource = withActivity {
        resetViewIdlingResource()
        viewIdlingResource
    }
    try {
        IdlingRegistry.getInstance().register(idlingResource)
        Espresso.onView(ViewMatchers.withId(R.id.direction_toggle)).perform(click())
    } finally { // Always release the idling resource, in case of timeout exceptions.
        IdlingRegistry.getInstance().unregister(idlingResource)
    }
}

/**
 * Waits until an image has been saved and its idling resource has become idle.
 *
 * @param captureRequestsCount the capture requests count to issue to continuously take pictures
 * without waiting for the previous capture requests to be done.
 */
internal fun ActivityScenario<CameraXActivity>.takePictureAndWaitForImageSavedIdle(
    captureRequestsCount: Int = 1
) {
    val idlingResource = withActivity {
        cleanTakePictureErrorMessage()
        imageSavedIdlingResource
    }
    try {
        // Perform click to take a picture.
        Espresso.onView(ViewMatchers.withId(R.id.Picture)).apply {
            repeat(captureRequestsCount) {
                perform(click())
            }
        }
        // Registers the idling resource and wait for it being idle after performing the click
        // operations. So that the click operations can be performed continuously without wait for
        // previous capture results.
        IdlingRegistry.getInstance().register(idlingResource)
        Espresso.onIdle()
    } finally { // Always release the idling resource, in case of timeout exceptions.
        IdlingRegistry.getInstance().unregister(idlingResource)
        withActivity {
            // Idling resource will also become idle when an error occurs. Checks the last error
            // message and throw an Exception to make the test failed if the error message is not
            // null.
            if (lastTakePictureErrorMessage != null) {
                throw Exception(lastTakePictureErrorMessage)
            } else {
                deleteSessionImages()
            }
        }
    }
}

/**
 * Waits until the imageAnalysis has received the required number of images and its idling resource
 * has become idle.
 */
internal fun ActivityScenario<CameraXActivity>.waitForImageAnalysisIdle() {
    val idlingResource = withActivity {
        resetAnalysisIdlingResource()
        analysisIdlingResource
    }
    try {
        IdlingRegistry.getInstance().register(idlingResource)
        // Check the activity launched and the image analysis info is displayed on the text view.
        Espresso.onView(ViewMatchers.withId(R.id.textView))
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
    } finally { // Always release the idling resource, in case of timeout exceptions.
        IdlingRegistry.getInstance().unregister(idlingResource)
    }
}

/**
 * Waits until a video has been saved and its idling resource has become idle.
 */
internal fun ActivityScenario<CameraXActivity>.recordVideoAndWaitForVideoSavedIdle() {
    val idlingResource = withActivity {
        // Make sure that the test target use case is not null
        assertThat(videoCapture).isNotNull()
        setVideoCaptureAutoStopLength(VIDEO_CAPTURE_AUTO_STOP_LENGTH_MS)
        videoSavedIdlingResource
    }
    try {
        IdlingRegistry.getInstance().register(idlingResource)
        // Perform click to record a video.
        Espresso.onView(ViewMatchers.withId(R.id.Video)).perform(click())
    } finally { // Always release the idling resource, in case of timeout exceptions.
        IdlingRegistry.getInstance().unregister(idlingResource)
        withActivity { deleteSessionVideos() }
    }
}
