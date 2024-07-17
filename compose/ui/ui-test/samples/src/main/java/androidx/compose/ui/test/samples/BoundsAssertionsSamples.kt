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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.MouseInjectionScope
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.click
import androidx.compose.ui.test.getPartialBoundsOfLinks
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.text.LinkAnnotation

@Sampled
fun touchInputOnFirstSpecificLinkInText() {
    // Example of clicking on a link
    val firstLinkBounds =
        composeTestRule
            .onNodeWithText("YOUR_TEXT_WITH_LINK")
            .getPartialBoundsOfLinks { (it.item as? LinkAnnotation.Url)?.url == "YOUR_URL" }
            .first()

    composeTestRule.onNodeWithText("YOUR_TEXT_WITH_LINK").performTouchInput {
        click(firstLinkBounds.center)
    }
}

@OptIn(ExperimentalTestApi::class)
@Sampled
fun hoverAnyFirstLinkInText() {
    // Example of a convenience function to hover over the first link
    fun SemanticsNodeInteraction.performMouseInputOnFirstLink(
        block: MouseInjectionScope.(offsetInLink: Offset) -> Unit
    ): SemanticsNodeInteraction {
        val linkBounds = getPartialBoundsOfLinks().firstOrNull() ?: return this
        return this.performMouseInput { block(linkBounds.center) }
    }

    composeTestRule.onNodeWithText("YOUR_TEXT_WITH_LINK").performMouseInputOnFirstLink {
        moveTo(it)
    }
}
