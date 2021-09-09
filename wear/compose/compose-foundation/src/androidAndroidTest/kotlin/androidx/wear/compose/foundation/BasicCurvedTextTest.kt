/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.wear.compose.foundation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Assert.assertEquals
import androidx.compose.ui.unit.sp
import org.junit.Rule
import org.junit.Test

class BasicCurvedTextTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun modifying_curved_text_forces_curved_row_remeasure() {
        val counters = Counters()
        val text = mutableStateOf("Initial")
        rule.setContent {
            SpyCurvedRow(counters) {
                BasicCurvedText(
                    text = text.value,
                    style = CurvedTextStyle(fontSize = 14.sp)
                )
            }
        }

        rule.runOnIdle {
            counters.reset()
            text.value = "New Value"
        }

        rule.runOnIdle {
            assertEquals(1, counters.layoutsCount)
            assertEquals(1, counters.measuresCount)
        }
    }
}

internal data class Counters(
    var measuresCount: Int = 0,
    var layoutsCount: Int = 0
) {
    fun reset() {
        measuresCount = 0
        layoutsCount = 0
    }
}

@Composable
internal fun SpyCurvedRow(
    counters: Counters,
    content: @Composable CurvedRowScope.() -> Unit
) {
    Layout(
        content = {
            CurvedRowScopeInstance.content()
        }
    ) { measurables, constraints ->
        counters.measuresCount++
        // Ensure we ask BasicCurvedText for it's intrinsic measures, as CurvedRow does,
        // so we subscribe to get notified of changes
        measurables.sumOf {
            it.maxIntrinsicWidth(constraints.maxHeight)
        }

        val placeables = measurables.map { it.measure(constraints) }
        layout(constraints.maxWidth, constraints.maxHeight) {
            counters.layoutsCount++
            placeables.forEach {
                it.placeRelative(0, 0)
            }
        }
    }
}
