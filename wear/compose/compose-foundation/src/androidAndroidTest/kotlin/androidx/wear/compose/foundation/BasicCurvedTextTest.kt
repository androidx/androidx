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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import androidx.compose.ui.unit.sp
import androidx.test.filters.FlakyTest
import org.junit.Rule
import org.junit.Test

class BasicCurvedTextTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    @FlakyTest(bugId = 227338558)
    fun modifying_curved_text_forces_curved_row_remeasure() {
        val counters = Counters()
        val text = mutableStateOf("Initial")
        rule.setContent {
            CurvedLayout {
                wrappedBasicCurvedText(
                    counters = counters,
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
            // TODO(b/219885899): Investigate why we need the extra passes.
            assertEquals(Counters(2, 2, 3), counters)
        }
    }
}

internal data class Counters(
    var measuresCount: Int = 0,
    var layoutsCount: Int = 0,
    var drawCount: Int = 0,
) {
    fun reset() {
        measuresCount = 0
        layoutsCount = 0
        drawCount = 0
    }
}

// TODO: Implement using a CurvedModifier
internal fun CurvedScope.wrappedBasicCurvedText(
    counters: Counters,
    text: String,
    style: CurvedTextStyle,
    clockwise: Boolean = true,
    contentArcPadding: ArcPaddingValues = ArcPaddingValues(0.dp)
) = add(CurvedChildWrapper(CurvedTextChild(text, clockwise, contentArcPadding) { style }, counters))

internal class CurvedChildWrapper(
    val wrapped: CurvedChild,
    val counters: Counters
) : CurvedChild() {
    @Composable
    override fun SubComposition() { wrapped.SubComposition() }

    override fun MeasureScope.initializeMeasure(
        measurables: List<Measurable>,
        index: Int
    ): Int = with(wrapped) {
        counters.measuresCount++
        initializeMeasure(measurables, index)
    }

    override fun doEstimateThickness(maxRadius: Float) = wrapped.estimateThickness(maxRadius)

    override fun doRadialPosition(
        parentOuterRadius: Float,
        parentThickness: Float,
    ) = wrapped.radialPosition(
        parentOuterRadius,
        parentThickness,
    )

    override fun doAngularPosition(
        parentStartAngleRadians: Float,
        parentSweepRadians: Float,
        centerOffset: Offset
    ) = wrapped.angularPosition(
        parentStartAngleRadians,
        parentSweepRadians,
        centerOffset
    )

    override fun (Placeable.PlacementScope).placeIfNeeded() = with(wrapped) {
        counters.layoutsCount++
        placeIfNeeded()
    }

    override fun DrawScope.draw() = with(wrapped) {
        counters.drawCount++
        draw()
    }
}
