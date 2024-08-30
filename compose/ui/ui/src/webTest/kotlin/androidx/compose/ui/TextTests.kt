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

package androidx.compose.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.ui.layout.FirstBaseline
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import kotlin.math.abs
import kotlin.test.Test
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.test.runTest

class TextTests : OnCanvasTests {

    companion object {
        private fun assertApproximatelyEqual(
            expected: Float,
            actual: Float,
            tolerance: Float = 1f
        ) {
            if (abs(expected - actual) > tolerance) {
                throw AssertionError("Expected $expected but got $actual. Difference is more than the allowed delta $tolerance")
            }
        }
    }

    @Test
    // https://github.com/JetBrains/compose-multiplatform/issues/4078
    fun baselineShouldBeNotZero() = runTest {
        val headingOnPositioned = Channel<Float>(10)
        val subtitleOnPositioned = Channel<Float>(10)

        composableContent {
            val density = LocalDensity.current.density
            Row {
                Text(
                    "Heading",
                    modifier = Modifier.alignByBaseline()
                        .onGloballyPositioned {
                            headingOnPositioned.sendFromScope(it[FirstBaseline] / density)
                        },
                    style = MaterialTheme.typography.h4
                )
                Text(
                    " — Subtitle",
                    modifier = Modifier.alignByBaseline()
                        .onGloballyPositioned {
                            subtitleOnPositioned.sendFromScope(it[FirstBaseline] / density)
                        },
                    style = MaterialTheme.typography.subtitle1
                )
            }
        }

        val headingAlignment = headingOnPositioned.receive()
        val subtitleAlignment = subtitleOnPositioned.receive()

        assertApproximatelyEqual(29f, headingAlignment)
        assertApproximatelyEqual(19f, subtitleAlignment)
    }
}