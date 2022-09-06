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

import androidx.camera.integration.extensions.CameraExtensionsActivity
import androidx.camera.integration.extensions.R
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
internal fun ActivityScenario<CameraExtensionsActivity>.waitForPreviewIdle() {
    val idlingResource = withActivity {
        resetPreviewViewStreamingStateIdlingResource()
        previewViewStreamingStateIdlingResource
    }
    try {
        IdlingRegistry.getInstance().register(idlingResource)
        // Waits for the previewViewStreamingStateIdlingResource becoming idle
        Espresso.onView(ViewMatchers.withId(R.id.viewFinder))
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
    } finally { // Always releases the idling resource, in case of timeout exceptions.
        IdlingRegistry.getInstance().unregister(idlingResource)
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
    }
}