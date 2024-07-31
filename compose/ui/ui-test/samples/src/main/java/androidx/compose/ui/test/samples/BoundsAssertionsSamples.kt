/*
 * Copyright 2024 The Android Open Source Project
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

import androidx.annotation.Sampled
import androidx.compose.ui.test.getFirstLinkBounds
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performFirstLinkClick
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.text.LinkAnnotation

@Sampled
fun touchInputOnFirstSpecificLinkInText() {
    // Example of clicking on a link in test
    composeTestRule.onNodeWithText("YOUR_TEXT_WITH_LINK").performFirstLinkClick {
        (it.item as? LinkAnnotation.Url)?.url == "YOUR_URL"
    }
}

@Sampled
fun hoverFirstLinkInText() {
    // Example of hovering over the first link in test
    val firstLinkBounds =
        composeTestRule.onNodeWithText("YOUR_TEXT_WITH_LINK").getFirstLinkBounds {
            (it.item as? LinkAnnotation.Url)?.url == "YOUR_URL"
        }

    composeTestRule.onNodeWithText("YOUR_TEXT_WITH_LINK").performMouseInput {
        moveTo(firstLinkBounds!!.center)
    }
}
