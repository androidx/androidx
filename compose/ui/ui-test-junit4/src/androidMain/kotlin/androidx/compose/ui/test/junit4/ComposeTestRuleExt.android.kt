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

package androidx.compose.ui.test.junit4

import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import androidx.test.espresso.ViewInteraction

/**
 * Scopes the Compose interaction to the View hierarchy matched by the provided Espresso
 * [ViewInteraction].
 *
 * It resolves the View from the Espresso [interaction], locates all Compose roots within that view
 * hierarchy, and creates a new, scoped SemanticsNodeInteractionsProvider.
 *
 * @sample androidx.compose.ui.test.samples.onRootWithViewInteractionBasicSample
 * @sample androidx.compose.ui.test.samples.onRootWithViewInteractionRecyclerViewSample
 * @sample androidx.compose.ui.test.samples.onRootWithViewInteractionFragmentSample
 */
fun ComposeTestRule.onRootWithViewInteraction(
    interaction: ViewInteraction
): SemanticsNodeInteractionsProvider {
    val androidComposeTestRule =
        this as? AndroidComposeTestRule<*, *>
            ?: error(
                "This implementation of ComposeTestRule does not support onRootWithViewInteraction."
            )
    return androidComposeTestRule.onRootWithViewInteraction(interaction)
}
