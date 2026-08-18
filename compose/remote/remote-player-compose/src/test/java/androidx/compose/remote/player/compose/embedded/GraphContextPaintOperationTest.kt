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

package androidx.compose.remote.player.compose.embedded

import androidx.collection.mutableIntObjectMapOf
import androidx.compose.remote.core.Operation
import androidx.compose.remote.core.RemoteClock
import androidx.compose.remote.core.operations.ColorAttribute
import androidx.compose.runtime.mutableFloatStateOf
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class GraphContextPaintOperationTest {

    @Test
    fun colorAttributeEvaluatesThroughPaintContextInGraphContext() {
        val state = SnapshotRemoteComposeState()
        val sourceColorId = 1
        // ARGB: Alpha=255, Red=255, Green=128, Blue=0
        state.overrideColor(sourceColorId, 0xFFFF8000.toInt())

        val redOp = ColorAttribute(10, sourceColorId, ColorAttribute.COLOR_RED)
        val greenOp = ColorAttribute(11, sourceColorId, ColorAttribute.COLOR_GREEN)
        val blueOp = ColorAttribute(12, sourceColorId, ColorAttribute.COLOR_BLUE)
        val alphaOp = ColorAttribute(13, sourceColorId, ColorAttribute.COLOR_ALPHA)

        val computedOps =
            mutableIntObjectMapOf<Operation>().apply {
                put(10, redOp)
                put(11, greenOp)
                put(12, blueOp)
                put(13, alphaOp)
            }

        val graphContext =
            GraphContext(
                realState = state,
                computedOps = computedOps,
                timeMillis = mutableFloatStateOf(0f),
                clock = RemoteClock.SYSTEM,
            )

        assertThat(graphContext.getFloat(10)).isWithin(0.01f).of(1.0f)
        assertThat(graphContext.getFloat(11)).isWithin(0.01f).of(128f / 255f)
        assertThat(graphContext.getFloat(12)).isWithin(0.01f).of(0.0f)
        assertThat(graphContext.getFloat(13)).isWithin(0.01f).of(1.0f)
    }
}
