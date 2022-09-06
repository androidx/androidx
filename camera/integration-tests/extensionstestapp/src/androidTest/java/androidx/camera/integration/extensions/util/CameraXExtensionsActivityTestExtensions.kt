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

package androidx.camera.integration.extensions.util

import android.util.Log
import androidx.camera.integration.extensions.CameraExtensionsActivity
import androidx.camera.integration.extensions.R
import androidx.camera.integration.extensions.util.CameraXExtensionsTestUtil.relaunchCameraExtensionsActivity
import androidx.camera.view.PreviewView
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.testutils.withActivity

/**
 * Waits until the initialization idling resource has become idle.
 */
internal fun ActivityScenario<CameraExtensionsActivity>.waitForInitializationIdle() {
    val idlingResource = withActivity {
        initializationIdlingResource
    }
    try {
        IdlingRegistry.getInstance().register(idlingResource)
        // Waits for the initializationIdlingResource becoming idle
        Espresso.onIdle()
    } finally { // Always releases the idling resource, in case of timeout exceptions.
        IdlingRegistry.getInstance().unregister(idlingResource)
    }
}

/**
 * Waits until the PreviewView has become STREAMING state and its idling resource has become idle.
 */
internal fun ActivityScenario<CameraExtensionsActivity>.waitForPreviewViewStreaming() {
    val idlingResource = withActivity {
        previewViewStreamingStateIdlingResource
    }
    try {
        IdlingRegistry.getInstance().register(idlingResource)
        // Waits for the previewViewStreamingStateIdlingResource becoming idle
        Espresso.onView(ViewMatchers.withId(R.id.viewFinder))
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
    } finally { // Always releases the idling resource, in case of timeout exceptions.
        IdlingRegistry.getInstance().unregister(idlingResource)
        var streamState: PreviewView.StreamState? = null
        withActivity { streamState = currentStreamState }
        Log.d(
            "CameraExtensionsActivity",
            "waitForPreviewIdle end in StreamState: ${streamState?.name}"
        )
    }
}

/**
 * Waits until the PreviewView has become IDLE state and its idling resource has become idle.
 */
internal fun ActivityScenario<CameraExtensionsActivity>.waitForPreviewViewIdle() {
    val idlingResource = withActivity {
        previewViewIdleStateIdlingResource
    }
    try {
        IdlingRegistry.getInstance().register(idlingResource)
        // Waits for the previewViewIdleStateIdlingResource becoming idle
        Espresso.onIdle()
    } finally { // Always releases the idling resource, in case of timeout exceptions.
        IdlingRegistry.getInstance().unregister(idlingResource)
        var streamState: PreviewView.StreamState? = null
        withActivity { streamState = currentStreamState }
        Log.d(
            "CameraExtensionsActivity",
            "waitForPreviewViewIdle end in StreamState: ${streamState?.name}"
        )
    }
}

/**
 * Waits until captured image has been saved and its idling resource has become idle.
 */
internal fun ActivityScenario<CameraExtensionsActivity>.takePictureAndWaitForImageSavedIdle() {
    val idlingResource = withActivity {
        takePictureIdlingResource
    }
    try {
        IdlingRegistry.getInstance().register(idlingResource)
        // Performs click action and waits for the takePictureIdlingResource becoming idle
        Espresso.onView(ViewMatchers.withId(R.id.Picture)).perform(ViewActions.click())
    } finally { // Always releases the idling resource, in case of timeout exceptions.
        IdlingRegistry.getInstance().unregister(idlingResource)

        withActivity {
            // Idling resource will also become idle when an error occurs. Checks the last error
            // message and throw an Exception to make the test failed if the error message is not
            // null.
            if (lastTakePictureErrorMessage != null) {
                throw Exception(lastTakePictureErrorMessage)
            }
        }
    }
}

/**
 * Pauses and resumes the activity. Returns the new activityScenario because the original activity
 * might be destroyed and new one is created.
 */
internal fun ActivityScenario<CameraExtensionsActivity>.pauseAndResumeActivity(
    cameraId: String,
    extensionMode: Int
): ActivityScenario<CameraExtensionsActivity> {
    withActivity { resetPreviewViewIdleStateIdlingResource() }
    moveToState(Lifecycle.State.CREATED)
    waitForPreviewViewIdle()
    withActivity { resetPreviewViewStreamingStateIdlingResource() }
    // The original activity might be destroyed when re-launch the activity. Re-retrieve the
    // returned activityScenario from relaunchCameraExtensionsActivity() to run the following test
    // steps.
    return relaunchCameraExtensionsActivity(cameraId, extensionMode)
}