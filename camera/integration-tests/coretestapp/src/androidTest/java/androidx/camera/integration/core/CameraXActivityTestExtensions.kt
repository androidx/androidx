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

import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.testutils.withActivity

/**
 * Waits until the viewfinder has received frames and its idling resource has become idle.
 */
internal fun ActivityScenario<CameraXActivity>.waitForViewfinderIdle() {
    val idlingResource = withActivity { viewIdlingResource }
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
 * Waits until an image has been saved and its idling resource has become idle.
 */
internal fun ActivityScenario<CameraXActivity>.takePictureAndWaitForImageSavedIdle() {
    val idlingResource = withActivity { imageSavedIdlingResource }
    try {
        IdlingRegistry.getInstance().register(idlingResource)
        // Perform click to take a picture.
        Espresso.onView(ViewMatchers.withId(R.id.Picture)).perform(click())
    } finally { // Always release the idling resource, in case of timeout exceptions.
        IdlingRegistry.getInstance().unregister(idlingResource)
        withActivity { deleteSessionImages() }
    }
}