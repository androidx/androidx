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

import androidx.annotation.RequiresApi
import androidx.camera.integration.extensions.Camera2ExtensionsActivity
import androidx.camera.integration.extensions.R
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.testutils.withActivity

/**
 * Waits until the capture session has been configured and its idling resource has become idle.
 */
@RequiresApi(31)
internal fun ActivityScenario<Camera2ExtensionsActivity>.waitForCaptureSessionConfiguredIdle() {
    val idlingResource = withActivity {
        getCaptureSessionConfiguredIdlingResource()
    }
    try {
        IdlingRegistry.getInstance().register(idlingResource)
        // Waits for the CaptureSessionConfiguredIdlingResource becoming idle
        Espresso.onIdle()
    } finally { // Always releases the idling resource, in case of timeout exceptions.
        IdlingRegistry.getInstance().unregister(idlingResource)
    }
}

/**
 * Waits until the preview has received frames and its idling resource has become idle.
 */
@RequiresApi(31)
internal fun ActivityScenario<Camera2ExtensionsActivity>.waitForPreviewIdle() {
    val idlingResource = withActivity {
        resetPreviewIdlingResource()
        getPreviewIdlingResource()
    }
    try {
        IdlingRegistry.getInstance().register(idlingResource)
        // Waits for the PreviewIdlingResource becoming idle
        Espresso.onView(ViewMatchers.withId(R.id.viewFinder))
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
    } finally { // Always releases the idling resource, in case of timeout exceptions.
        IdlingRegistry.getInstance().unregister(idlingResource)
    }
}

/**
 * Waits until captured image has been saved and its idling resource has become idle.
 */
@RequiresApi(31)
internal fun ActivityScenario<Camera2ExtensionsActivity>.waitForImageSavedIdle() {
    val idlingResource = withActivity {
        getImageSavedIdlingResource()
    }
    try {
        IdlingRegistry.getInstance().register(idlingResource)
        // Performs click action and waits for the ImageSavedIdlingResource becoming idle
        Espresso.onView(ViewMatchers.withId(R.id.Picture)).perform(ViewActions.click())
    } finally { // Always releases the idling resource, in case of timeout exceptions.
        IdlingRegistry.getInstance().unregister(idlingResource)
        withActivity { deleteSessionImages() }
    }
}
