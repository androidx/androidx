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
import androidx.camera.view.PreviewView
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.testutils.withActivity

/** Waits until the initialization idling resource has become idle. */
internal fun ActivityScenario<CameraExtensionsActivity>.waitForInitializationIdle() {
    val idlingResource = withActivity { initializationIdlingResource }
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
    val idlingResource = withActivity { previewViewStreamingStateIdlingResource }
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
            "waitForPreviewIdle end in StreamState: ${streamState?.name}",
        )
    }
}

/** Waits until the PreviewView has become IDLE state and its idling resource has become idle. */
internal fun ActivityScenario<CameraExtensionsActivity>.waitForPreviewViewIdle() {
    val idlingResource = withActivity {
        // If PreviewView never left IDLE before the activity stopped, PreviewStreamStateObserver
        // deduplicates IDLE -> IDLE without re-emitting; release the idling resource directly.
        if (
            currentStreamState == PreviewView.StreamState.IDLE &&
                !previewViewIdleStateIdlingResource.isIdleNow
        ) {
            previewViewIdleStateIdlingResource.decrement()
        }
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
            "waitForPreviewViewIdle end in StreamState: ${streamState?.name}",
        )
    }
}

/**
 * Waits until captured image has been saved and its idling resource has become idle.
 *
 * @param waitForPostview Whether to also wait for the postview idling resource when postview is
 *   supported. Defaults to `false` because vendor extensions may omit `onPostviewBitmapAvailable`
 *   under rapid lifecycle or camera/mode-switch stress.
 */
internal fun ActivityScenario<CameraExtensionsActivity>.takePictureAndWaitForImageSavedIdle(
    waitForPostview: Boolean = false
) {
    val idlingResources = withActivity {
        if (waitForPostview) {
            arrayOf(takePictureIdlingResource, postviewIdlingResource)
        } else {
            arrayOf(takePictureIdlingResource)
        }
    }
    try {
        IdlingRegistry.getInstance().register(*idlingResources)
        // Performs click action and waits for the registered idling resources becoming idle
        Espresso.onView(ViewMatchers.withId(R.id.Picture)).perform(ViewActions.click())
    } finally { // Always releases the idling resources, in case of timeout exceptions.
        val nonIdleResources = idlingResources.filterNot { it.isIdleNow }
        if (nonIdleResources.isNotEmpty()) {
            Log.e(
                "CameraExtensionsActivity",
                "Failed to wait for idling resources to become idle: " +
                    nonIdleResources.joinToString { "${it.name} (isIdleNow=false)" },
            )
        }
        IdlingRegistry.getInstance().unregister(*idlingResources)

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
