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

package androidx.compose.ui.modifiers

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Button
import androidx.compose.material.LocalContentColor
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.preferredFrameRate
import androidx.compose.ui.test.findNodeWithTag
import androidx.compose.ui.test.runUIKitInstrumentedTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

internal class FrameRateTest {

    @Test
    fun testLowFrameRates() = runUIKitInstrumentedTest {
        val frameRates = listOf(5f, 10f, 30f, 60f)

        setContent {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(frameRates.size) { index ->
                    AlphaButton(frameRates[index])
                }
            }
        }

        val redrawer = rootRedrawer
        assertNotNull(redrawer, "redrawer is null")

        for (frameRate in frameRates) {
            val expectedFrameDuration = 1.0 / frameRate
            findNodeWithTag("${frameRate}fps").tap()
            waitUntil {
                val frameDuration = redrawer.currentTargetFrameDuration
                assertNotNull(frameDuration)
                checkEqual(expectedFrameDuration, frameDuration, 1e-5)
            }
        }
    }

    @Test
    fun testPreferredFrameRates() = runUIKitInstrumentedTest {
        val frameRates = listOf(5f, 10f, 30f, 60f, 80f, 120f)

        setContent {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(frameRates.size) { index ->
                    AlphaButton(frameRates[index])
                }
            }
        }

        val redrawer = rootRedrawer
        assertNotNull(redrawer, "redrawer is null")

        for (frameRate in frameRates) {
            findNodeWithTag("${frameRate}fps").tap()
            waitUntil {
                redrawer.preferredFramesPerSecond == frameRate.toLong()
            }
        }
    }
}

private fun checkEqual(expected: Double, actual: Double, absoluteTolerance: Double): Boolean =
    try {
        assertEquals(expected, actual, absoluteTolerance)
        true
    } catch (_: AssertionError) {
        false
    }


@Composable
private fun AlphaButton(frameRate: Float) {
    var targetAlpha by remember { mutableFloatStateOf(1f) }
    val alpha by animateFloatAsState(
        targetValue = targetAlpha,
        animationSpec = tween(durationMillis = 100)
    )

    Button(
        onClick = { targetAlpha = if (targetAlpha == 1f) 0.2f else 1f },
        modifier = Modifier.testTag("${frameRate}fps")
    ) {
        Text(
            text = "Click for $frameRate fps",
            color = LocalContentColor.current.copy(alpha = alpha), // Adjust text alpha
            modifier = Modifier.preferredFrameRate(frameRate),
        )
    }
}
