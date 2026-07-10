/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.compose.ui.test.samples

import android.view.View
import androidx.annotation.Sampled
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.junit4.onRootWithViewInteraction
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.matcher.ViewMatchers.hasDescendant
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import org.hamcrest.core.AllOf.allOf

private val header_id = View.generateViewId()
private val recycler_item_root_id = View.generateViewId()
private val detail_fragment_container_id = View.generateViewId()

@Sampled
fun onRootWithViewInteractionBasicSample() {
    // Select the "Header" View container
    val headerInteraction = onView(withId(header_id))

    // Scope the Compose interaction to only the Header
    composeTestRule
        .onRootWithViewInteraction(headerInteraction)
        .onNodeWithContentDescription("Settings")
        .performClick()
}

@Sampled
fun onRootWithViewInteractionRecyclerViewSample() {
    // Select the specific View row containing "Item #5"
    val specificRowInteraction =
        onView(allOf(withId(recycler_item_root_id), hasDescendant(withText("Item #5"))))

    // Scope interaction to that specific row View
    composeTestRule
        .onRootWithViewInteraction(specificRowInteraction)
        .onNodeWithTag("fav_icon")
        .assertIsDisplayed()
        .performClick()
}

@Sampled
fun onRootWithViewInteractionFragmentSample() {
    // Select the container for the Detail Fragment
    val detailContainerInteraction = onView(withId(detail_fragment_container_id))

    // Assert that the submit button exists/is enabled only in the detail fragment
    composeTestRule
        .onRootWithViewInteraction(detailContainerInteraction)
        .onNodeWithText("Submit")
        .assertIsEnabled()
}
