/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.wear.compose.material3.macrobenchmark.common

import android.os.SystemClock
import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text

/* Benchmark was created as a part of b/441701460 investigation to make sure that TLC can handle
 * scrolling without jank through a 2k characters long AnnotatedString with a lot of styles.
 */
val TransformingLazyColumnHeavyItemBenchmark =
    object : MacrobenchmarkScreen {
        override val content: @Composable (BoxScope.() -> Unit)
            get() = {
                val annotatedText = remember { createAnnotatedText() }
                val state = rememberTransformingLazyColumnState()
                AppScaffold {
                    ScreenScaffold(state) { contentPadding ->
                        TransformingLazyColumn(
                            state = state,
                            contentPadding = contentPadding,
                            modifier =
                                Modifier.semantics { contentDescription = CONTENT_DESCRIPTION },
                        ) {
                            item(key = "Main body") {
                                Text(text = annotatedText, modifier = Modifier.animateItem())
                            }
                        }
                    }
                }
            }

        override val exercise: MacrobenchmarkScope.() -> Unit
            get() = {
                val swipeStartY = device.displayHeight * 9 / 10 // scroll up
                val swipeEndY = device.displayHeight * 1 / 10
                val midX = device.displayWidth / 2
                repeat(5) {
                    repeat(2) {
                        device.swipe(midX, swipeStartY, midX, swipeEndY, 5)
                        device.waitForIdle()
                        SystemClock.sleep(200)
                    }
                    repeat(2) {
                        device.swipe(midX, swipeEndY, midX, swipeStartY, 5)
                        device.waitForIdle()
                        SystemClock.sleep(200)
                    }
                }
            }
    }

fun createAnnotatedText(): AnnotatedString {
    return buildAnnotatedString {
        val fullText =
            "In the quiet of the library, the scent of aged paper and old leather filled the air, a familiar comfort for Elara. She traced the spine of a book, a collection of forgotten tales, its cover worn smooth by countless hands before hers. Outside, the city hummed with a restless energy—a symphony of car horns, distant sirens, and the muffled chatter of passersby. But here, within these walls, time seemed to slow, each tick of the grandfather clock a deliberate, measured beat against the silence. She opened the book to a random page, her eyes falling upon a passage that spoke of a lone traveler on a moonlit path, guided only by the glimmer of stars. It was a simple story, yet it held a profound weight, a reminder that even in the darkest of nights, a light could always be found. Elara felt a kindred spirit with that traveler, each of them on a journey, seeking a different kind of truth. The world outside could wait; for now, she was content to be lost in the stories of those who had come before, finding her own path one page at a time. In the quiet of the library, the scent of aged paper and old leather filled the air, a familiar comfort for Elara. She traced the spine of a book, a collection of forgotten tales, its cover worn smooth by countless hands before hers. Outside, the city hummed with a restless energy—a symphony of car horns, distant sirens, and the muffled chatter of passersby. But here, within these walls, time seemed to slow, each tick of the grandfather clock a deliberate, measured beat against the silence. She opened the book to a random page, her eyes falling upon a passage that spoke of a lone traveler on a moonlit path, guided only by the glimmer of stars. It was a simple story, yet it held a profound weight, a reminder that even in the darkest of nights, a light could always be found. Elara felt a kindred spirit with that traveler, each of them on a journey, seeking a different kind of truth. The world outside could wait; for now, she was content to be lost in the stories of those who had come before, finding her"
        append(fullText)
        // Add 2 styles to each character
        for (c in 0..fullText.length - 1) {
            addStyle(SpanStyle(fontWeight = FontWeight.Bold), c, c + 1)
            addStyle(SpanStyle(fontSize = 24.sp), c, c + 1)
        }
        toAnnotatedString()
    }
}
