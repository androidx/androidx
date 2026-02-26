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

package androidx.compose.foundation.layout.samples

import androidx.annotation.Sampled
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalFlexBoxApi
import androidx.compose.foundation.layout.FlexAlignContent
import androidx.compose.foundation.layout.FlexAlignItems
import androidx.compose.foundation.layout.FlexAlignSelf
import androidx.compose.foundation.layout.FlexBasis
import androidx.compose.foundation.layout.FlexBox
import androidx.compose.foundation.layout.FlexBoxConfig
import androidx.compose.foundation.layout.FlexConfig
import androidx.compose.foundation.layout.FlexDirection
import androidx.compose.foundation.layout.FlexJustifyContent
import androidx.compose.foundation.layout.FlexWrap
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.AlignmentLine
import androidx.compose.ui.layout.FirstBaseline
import androidx.compose.ui.layout.LastBaseline
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Sampled
@Composable
@OptIn(ExperimentalFlexBoxApi::class)
fun SimpleFlexBox() {
    // FlexBox defaults to a Row-like layout (FlexDirection.Row).
    // The children will be laid out horizontally.
    FlexBox(
        modifier = Modifier.fillMaxWidth(),
        config = {
            direction(
                if (constraints.maxWidth < 400.dp.roundToPx()) FlexDirection.Column
                else FlexDirection.Row
            )
        },
    ) {
        // This child has a fixed size and will not flex.
        Box(
            modifier = Modifier.size(80.dp).background(Color.Magenta),
            contentAlignment = Alignment.Center,
        ) {
            Text("Fixed")
        }
        // This child has a grow factor of 1. It will take up 1/3 of the remaining space.
        Box(
            modifier = Modifier.height(80.dp).flex { grow(1f) }.background(Color.Yellow),
            contentAlignment = Alignment.Center,
        ) {
            Text("Grow = 1")
        }
        // This child has a growth factor of 2. It will take up 2/3 of the remaining space.
        Box(
            modifier = Modifier.height(80.dp).flex { grow(2f) }.background(Color.Green),
            contentAlignment = Alignment.Center,
        ) {
            Text("Grow = 2")
        }
    }
}

@OptIn(ExperimentalFlexBoxApi::class)
@Sampled
@Composable
fun FlexBoxConfigReusableSample() {
    // Define reusable config - can be a top-level constant
    val RowWrapConfig = FlexBoxConfig {
        direction(FlexDirection.Row)
        wrap(FlexWrap.Wrap)
        gap(8.dp)
    }

    FlexBox(config = RowWrapConfig) {
        repeat(6) { Box(Modifier.size(80.dp).background(Color.Blue)) }
    }
}

@OptIn(ExperimentalFlexBoxApi::class)
@Sampled
@Composable
fun FlexBoxConfigResponsiveSample() {
    val ResponsiveFlexBoxConfig = FlexBoxConfig {
        direction(FlexDirection.Row)
        wrap(if (constraints.maxWidth < 600.dp.roundToPx()) FlexWrap.Wrap else FlexWrap.NoWrap)
    }
    FlexBox(config = ResponsiveFlexBoxConfig) {
        repeat(4) { Box(Modifier.size(100.dp).background(Color.Blue)) }
    }
}

@OptIn(ExperimentalFlexBoxApi::class)
@Sampled
@Composable
fun FlexConfigScopeSample() {

    FlexBox {
        Box(
            Modifier.flex {
                grow(1f) // Grow to fill space
                shrink(0f) // Don't shrink below basis
                basis(100.dp) // Start at 100dp
                alignSelf(FlexAlignSelf.Center) // Center this item
            }
        )
    }
}

@OptIn(ExperimentalFlexBoxApi::class)
@Sampled
@Composable
fun FlexBoxConstraintsSample() {
    FlexBox(
        config = {
            direction(FlexDirection.Row)
            // Switch to wrapping on narrow screens
            wrap(if (constraints.maxWidth < 400.dp.roundToPx()) FlexWrap.Wrap else FlexWrap.NoWrap)
            // Adjust gap based on available space
            gap(if (constraints.maxWidth > 800.dp.roundToPx()) 16.dp else 8.dp)
        }
    ) {
        repeat(4) { Box(Modifier.size(80.dp).background(Color.Blue)) }
    }
}

@OptIn(ExperimentalFlexBoxApi::class)
@Sampled
@Composable
fun FlexBoxDirectionSample() {
    // Can be extracted to top-level FlexBoxConfig constants
    val HorizontalConfig = FlexBoxConfig { direction(FlexDirection.Row) }
    val VerticalConfig = FlexBoxConfig { direction(FlexDirection.Column) }

    // Horizontal layout
    FlexBox(config = HorizontalConfig) {
        Box(Modifier.size(50.dp).background(Color.Red))
        Box(Modifier.size(50.dp).background(Color.Green))
        Box(Modifier.size(50.dp).background(Color.Blue))
    }
}

@OptIn(ExperimentalFlexBoxApi::class)
@Sampled
@Composable
fun FlexBoxWrapSample() {
    // Can be extracted to a top-level FlexBoxConfig
    val WrappingRow = FlexBoxConfig {
        direction(FlexDirection.Row)
        wrap(FlexWrap.Wrap)
    }

    // Items wrap to next line when they don't fit
    FlexBox(modifier = Modifier.width(250.dp), config = WrappingRow) {
        repeat(10) { Box(Modifier.size(60.dp).background(Color.Blue)) }
    }
}

@OptIn(ExperimentalFlexBoxApi::class)
@Sampled
@Composable
fun FlexBoxJustifyContentSample() {
    // Can be extracted to a top-level FlexBoxConfig
    val SpaceBetweenRow = FlexBoxConfig {
        direction(FlexDirection.Row)
        justifyContent(FlexJustifyContent.SpaceBetween)
    }

    FlexBox(modifier = Modifier.fillMaxWidth(), config = SpaceBetweenRow) {
        Text("Left")
        Text("Right")
    }
}

@OptIn(ExperimentalFlexBoxApi::class)
@Sampled
@Composable
fun FlexBoxAlignItemsSample() {
    // Can be extracted to a top-level FlexBoxConfig
    val CenteredRow = FlexBoxConfig {
        direction(FlexDirection.Row)
        alignItems(FlexAlignItems.Center)
    }

    FlexBox(modifier = Modifier.height(200.dp), config = CenteredRow) {
        Text("Centered")
        Box(Modifier.size(50.dp).background(Color.Blue))
    }
}

@OptIn(ExperimentalFlexBoxApi::class)
@Sampled
@Composable
fun FlexBoxAlignItemsBaselineSample() {
    // Can be extracted to a top-level FlexBoxConfig
    val BaselineRow = FlexBoxConfig {
        direction(FlexDirection.Row)
        alignItems(FirstBaseline)
    }

    FlexBox(config = BaselineRow) {
        Text("Small", fontSize = 12.sp)
        Text("Large", fontSize = 24.sp)
    }
}

@OptIn(ExperimentalFlexBoxApi::class)
@Sampled
@Composable
fun FlexBoxAlignItemsCustomBaselineSample() {
    FlexBox(
        config = {
            direction(FlexDirection.Row)
            alignItems { measured ->
                // Custom baseline: 10px below the first baseline
                val baseline = measured[FirstBaseline]
                if (baseline != AlignmentLine.Unspecified) baseline + 10
                else measured.measuredHeight
            }
        }
    ) {
        Text("Custom", fontSize = 14.sp)
        Text("Baseline", fontSize = 20.sp)
    }
}

@OptIn(ExperimentalFlexBoxApi::class)
@Sampled
@Composable
fun FlexBoxAlignContentSample() {
    // Can be extracted to a top-level FlexBoxConfig
    val SpaceAroundWrap = FlexBoxConfig {
        direction(FlexDirection.Row)
        wrap(FlexWrap.Wrap)
        alignContent(FlexAlignContent.SpaceAround)
    }

    FlexBox(modifier = Modifier.fillMaxSize(), config = SpaceAroundWrap) {
        repeat(20) { Box(Modifier.size(80.dp).background(Color.Blue)) }
    }
}

@OptIn(ExperimentalFlexBoxApi::class)
@Sampled
@Composable
fun FlexBoxRowGapSample() {
    // Can be extracted to a top-level FlexBoxConfig
    val WrapWithRowGap = FlexBoxConfig {
        direction(FlexDirection.Row)
        wrap(FlexWrap.Wrap)
        rowGap(16.dp)
    }

    FlexBox(modifier = Modifier.width(200.dp), config = WrapWithRowGap) {
        repeat(6) { Box(Modifier.size(60.dp).background(Color.Blue)) }
    }
}

@OptIn(ExperimentalFlexBoxApi::class)
@Sampled
@Composable
fun FlexBoxColumnGapSample() {
    // Can be extracted to a top-level FlexBoxConfig
    val RowWithColumnGap = FlexBoxConfig {
        direction(FlexDirection.Row)
        columnGap(12.dp)
    }

    FlexBox(config = RowWithColumnGap) {
        Box(Modifier.size(50.dp).background(Color.Red))
        Box(Modifier.size(50.dp).background(Color.Green))
        Box(Modifier.size(50.dp).background(Color.Blue))
    }
}

@OptIn(ExperimentalFlexBoxApi::class)
@Sampled
@Composable
fun FlexBoxGapSample() {
    // Can be extracted to a top-level FlexBoxConfig
    val WrapWithUniformGap = FlexBoxConfig {
        direction(FlexDirection.Row)
        wrap(FlexWrap.Wrap)
        gap(8.dp)
    }

    FlexBox(modifier = Modifier.width(200.dp), config = WrapWithUniformGap) {
        repeat(6) { Box(Modifier.size(60.dp).background(Color.Blue)) }
    }
}

@OptIn(ExperimentalFlexBoxApi::class)
@Sampled
@Composable
fun FlexBoxGapDifferentSample() {
    // Can be extracted to a top-level FlexBoxConfig
    val WrapWithDifferentGaps = FlexBoxConfig {
        direction(FlexDirection.Row)
        wrap(FlexWrap.Wrap)
        gap(row = 16.dp, column = 8.dp)
    }

    FlexBox(modifier = Modifier.width(200.dp), config = WrapWithDifferentGaps) {
        repeat(6) { Box(Modifier.size(60.dp).background(Color.Blue)) }
    }
}

@OptIn(ExperimentalFlexBoxApi::class)
@Sampled
@Composable
fun FlexBoxScopeSample() {
    // FlexConfig can be defined as top-level constants for reuse
    val NoShrink = FlexConfig { shrink(0f) }

    FlexBox {
        // Basic item without flex configuration
        Box(Modifier.size(50.dp).background(Color.Red))

        // Item with inline flex configuration
        Box(Modifier.size(50.dp).background(Color.Green).flex { grow(1f) })

        // Item with reusable FlexConfig
        Box(Modifier.size(50.dp).background(Color.Blue).flex(NoShrink))
    }
}

@OptIn(ExperimentalFlexBoxApi::class)
@Sampled
@Composable
fun FlexModifierWithConfigSample() {
    // Define reusable FlexConfig - can be a top-level constant
    val GrowAndCenter = FlexConfig {
        grow(1f)
        alignSelf(FlexAlignSelf.Center)
    }

    FlexBox(modifier = Modifier.fillMaxWidth().height(100.dp)) {
        Box(Modifier.size(50.dp).background(Color.Red))
        Box(Modifier.background(Color.Green).flex(GrowAndCenter))
        Box(Modifier.size(50.dp).background(Color.Blue))
    }
}

@OptIn(ExperimentalFlexBoxApi::class)
@Sampled
@Composable
fun FlexModifierWithLambdaSample() {
    FlexBox(modifier = Modifier.fillMaxWidth()) {
        Box(
            Modifier.height(50.dp).background(Color.Blue).flex {
                grow(1f)
                shrink(0f)
                alignSelf(FlexAlignSelf.Center)
            }
        )
    }
}

@OptIn(ExperimentalFlexBoxApi::class)
@Sampled
@Composable
fun FlexBasisDpSample() {
    val FixedBasis = FlexConfig { basis(FlexBasis.Dp(100.dp)) }
    // Or use the shorthand
    val FixedBasisShorthand = FlexConfig { basis(100.dp) }

    FlexBox(modifier = Modifier.fillMaxWidth()) {
        Box(Modifier.height(50.dp).background(Color.Blue).flex(FixedBasis))
        Box(Modifier.height(50.dp).background(Color.Green).flex(FixedBasisShorthand))
    }
}

@OptIn(ExperimentalFlexBoxApi::class)
@Sampled
@Composable
fun FlexBasisPercentSample() {
    val HalfWidth = FlexConfig { basis(FlexBasis.Percent(0.5f)) }
    // Or use the shorthand
    val HalfWidthShorthand = FlexConfig { basis(0.5f) }

    FlexBox(modifier = Modifier.width(400.dp)) {
        Box(Modifier.height(50.dp).background(Color.Blue).flex(HalfWidth))
        Box(Modifier.height(50.dp).background(Color.Green).flex(HalfWidthShorthand))
    }
}

@OptIn(ExperimentalFlexBoxApi::class)
@Sampled
@Composable
fun FlexConfigSample() {
    // Define reusable FlexConfig - can be a top-level constant
    val FlexibleCentered = FlexConfig {
        grow(1f)
        shrink(0f)
        basis(100.dp)
        alignSelf(FlexAlignSelf.Center)
    }

    FlexBox(modifier = Modifier.fillMaxWidth().height(100.dp)) {
        Box(Modifier.background(Color.Blue).flex(FlexibleCentered))
    }
}

@OptIn(ExperimentalFlexBoxApi::class)
@Sampled
@Composable
fun FlexAlignSelfSample() {
    // Can be extracted to a top-level FlexBoxConfig
    val StartAligned = FlexBoxConfig {
        direction(FlexDirection.Row)
        alignItems(FlexAlignItems.Start)
    }

    // FlexConfig can be defined as top-level constants
    val CenterSelf = FlexConfig { alignSelf(FlexAlignSelf.Center) }

    FlexBox(modifier = Modifier.fillMaxWidth().height(100.dp), config = StartAligned) {
        Box(Modifier.size(50.dp).background(Color.Red)) // Aligned to start
        Box(Modifier.size(50.dp).background(Color.Green).flex(CenterSelf)) // Centered
        Box(Modifier.size(50.dp).background(Color.Blue)) // Aligned to start
    }
}

@OptIn(ExperimentalFlexBoxApi::class)
@Sampled
@Composable
fun FlexAlignSelfBaselineSample() {
    // FlexConfig can be defined as top-level constants
    val BaselineAligned = FlexConfig { alignSelf(LastBaseline) }

    FlexBox {
        Text("Normal alignment")
        Text("Baseline aligned", modifier = Modifier.flex(BaselineAligned))
    }
}

@OptIn(ExperimentalFlexBoxApi::class)
@Sampled
@Composable
fun FlexAlignSelfCustomBaselineSample() {
    FlexBox {
        Box(
            Modifier.size(50.dp).background(Color.Blue).flex {
                alignSelf { measured ->
                    val baseline = measured[FirstBaseline]
                    if (baseline != AlignmentLine.Unspecified) baseline + 5
                    else measured.measuredHeight
                }
            }
        )
    }
}

@OptIn(ExperimentalFlexBoxApi::class)
@Sampled
@Composable
fun FlexOrderSample() {
    // FlexConfig can be defined as top-level constants
    val First = FlexConfig { order(-1) }
    val Second = FlexConfig { order(1) }
    val Third = FlexConfig { order(2) }

    FlexBox {
        Box(Modifier.size(50.dp).background(Color.Red).flex(Third)) // Displayed last
        Box(Modifier.size(50.dp).background(Color.Green)) // order = 0, displayed second
        Box(Modifier.size(50.dp).background(Color.Blue).flex(Second)) // Displayed third
        Box(Modifier.size(50.dp).background(Color.Yellow).flex(First)) // Displayed first
    }
    // Visual order: Yellow, Green, Blue, Red
}

@OptIn(ExperimentalFlexBoxApi::class)
@Sampled
@Composable
fun FlexGrowSample() {
    // FlexConfig can be defined as top-level constants
    val Grow1 = FlexConfig { grow(1f) }
    val Grow2 = FlexConfig { grow(2f) }

    FlexBox(modifier = Modifier.fillMaxWidth()) {
        // Free space distributed 1:2
        Box(Modifier.height(50.dp).background(Color.Red).flex(Grow1)) // Gets 1/3
        Box(Modifier.height(50.dp).background(Color.Blue).flex(Grow2)) // Gets 2/3
    }
}

@OptIn(ExperimentalFlexBoxApi::class)
@Sampled
@Composable
fun FlexShrinkSample() {
    // FlexConfig can be defined as top-level constants
    val NoShrink = FlexConfig { shrink(0f) }
    val CanShrink = FlexConfig { shrink(1f) }

    FlexBox(modifier = Modifier.width(150.dp)) {
        // Both items want 100dp but only 150dp available
        Box(
            Modifier.width(100.dp).height(50.dp).background(Color.Red).flex(NoShrink)
        ) // Keeps 100dp
        Box(
            Modifier.width(100.dp).height(50.dp).background(Color.Blue).flex(CanShrink)
        ) // Shrinks to 50dp
    }
}

@OptIn(ExperimentalFlexBoxApi::class)
@Sampled
@Composable
fun FlexBasisSample() {
    // FlexConfig can be defined as top-level constants
    val FixedBasisGrow = FlexConfig {
        basis(FlexBasis.Dp(100.dp))
        grow(1f)
    }

    val PercentBasis = FlexConfig { basis(FlexBasis.Percent(0.5f)) }

    FlexBox(modifier = Modifier.fillMaxWidth()) {
        Box(Modifier.height(50.dp).background(Color.Red).flex(FixedBasisGrow))
        Box(Modifier.height(50.dp).background(Color.Blue).flex(PercentBasis))
    }
}
