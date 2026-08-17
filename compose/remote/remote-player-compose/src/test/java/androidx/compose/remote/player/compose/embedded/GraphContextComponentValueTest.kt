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
import androidx.compose.remote.core.operations.FloatExpression
import androidx.compose.remote.core.operations.Utils
import androidx.compose.remote.core.operations.utilities.AnimatedFloatExpression
import androidx.compose.runtime.mutableFloatStateOf
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class GraphContextComponentValueTest {

    @Test
    fun graphContextResolvesComponentValueDirectlyAndReactively() {
        val state = SnapshotRemoteComposeState()
        val componentValueState = mutableFloatStateOf(100f)
        val componentValueId = 50

        val graphContext =
            GraphContext(
                realState = state,
                computedOps = mutableIntObjectMapOf(),
                timeMillis = mutableFloatStateOf(0f),
                clock = RemoteClock.SYSTEM,
                componentValues = mapOf(componentValueId to componentValueState),
            )

        assertThat(graphContext.getFloat(componentValueId)).isEqualTo(100f)

        // Mutate component size
        componentValueState.floatValue = 250f
        assertThat(graphContext.getFloat(componentValueId)).isEqualTo(250f)
    }

    @Test
    fun expressionOverComponentValueEvaluatesAndUpdatesReactively() {
        val state = SnapshotRemoteComposeState()
        val componentValueState = mutableFloatStateOf(100f)
        val componentValueId = 50
        val expressionId = 60

        // Expression: componentValueId / 2
        // RPN: [id_as_nan, 2f, DIV]
        val rpn = floatArrayOf(Utils.asNan(componentValueId), 2f, AnimatedFloatExpression.DIV)
        val exprOp = FloatExpression(expressionId, rpn, null)
        val computedOps = mutableIntObjectMapOf<Operation>().apply { put(expressionId, exprOp) }

        val graphContext =
            GraphContext(
                realState = state,
                computedOps = computedOps,
                timeMillis = mutableFloatStateOf(0f),
                clock = RemoteClock.SYSTEM,
                componentValues = mapOf(componentValueId to componentValueState),
            )

        assertThat(graphContext.getFloat(expressionId)).isEqualTo(50f)

        // Mutate component size
        componentValueState.floatValue = 200f
        assertThat(graphContext.getFloat(expressionId)).isEqualTo(100f)
    }

    @Test
    fun componentValueTakesPrecedenceOverStoreFloatOverride() {
        val state = SnapshotRemoteComposeState()
        val componentValueId = 50
        state.overrideFloat(componentValueId, 999f)

        val componentValueState = mutableFloatStateOf(100f)
        val graphContext =
            GraphContext(
                realState = state,
                computedOps = mutableIntObjectMapOf(),
                timeMillis = mutableFloatStateOf(0f),
                clock = RemoteClock.SYSTEM,
                componentValues = mapOf(componentValueId to componentValueState),
            )

        // Measured component value must take precedence over the store override
        assertThat(graphContext.getFloat(componentValueId)).isEqualTo(100f)
    }
}
