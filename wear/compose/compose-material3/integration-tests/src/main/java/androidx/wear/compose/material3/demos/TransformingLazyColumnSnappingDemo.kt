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

package androidx.wear.compose.material3.demos

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.TransformingLazyColumnDefaults
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.foundation.rotary.RotaryScrollableDefaults
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.Card
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text

@Composable
fun TransformingLazyColumnSnappingDemo() {
    val state = rememberTransformingLazyColumnState()
    val labels = rememberDefaultListOfLabels()
    AppScaffold {
        ScreenScaffold(state) { contentPadding ->
            TransformingLazyColumn(
                state = state,
                contentPadding = contentPadding,
                flingBehavior = TransformingLazyColumnDefaults.snapFlingBehavior(state),
                rotaryScrollableBehavior =
                    RotaryScrollableDefaults.snapBehavior(scrollableState = state),
            ) {
                items(labels) { label ->
                    Card(modifier = Modifier.fillMaxWidth(0.9f)) { Text(label) }
                }
            }
        }
    }
}

@Composable
private fun rememberDefaultListOfLabels(): List<String> {
    return remember {
        listOf(
            "Hi",
            "Hello World!",
            "Hello world again?",
            "More content as we add stuff",
            "This is a longer item. Here are some fun facts: Did you know that the human brain generates about 12-25 watts of power? That's enough to power a low-energy LED light bulb! The Eiffel Tower gets 15 cm taller in the summer due to thermal expansion of the iron.",
            "This is another long item. Here are some other fun facts: Honey never spoils. Archaeologists have found pots of honey in ancient Egyptian tombs that are over 3,000 years old and still perfectly edible. Honey is made by bees from the nectar of flowers. Bees collect nectar from flowers and store it in their honey stomach, where it is mixed with enzymes. The bees then regurgitate the nectar into a honeycomb, where it is stored.",
            "I don't know if this will fit now, testing",
            "And now we are really pushing it because the screen is really small",
        )
    }
}
