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

package androidx.wear.compose.integration.demos

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.AnimationVector2D
import androidx.compose.animation.core.DeferredTargetAnimation
import androidx.compose.animation.core.ExperimentalAnimatableApi
import androidx.compose.animation.core.VectorConverter
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.paint
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ApproachLayoutModifierNode
import androidx.compose.ui.layout.ApproachMeasureScope
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.LookaheadScope
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.round
import androidx.compose.ui.unit.sp
import androidx.wear.compose.foundation.AnchorType
import androidx.wear.compose.foundation.CurvedAlignment
import androidx.wear.compose.foundation.CurvedDirection
import androidx.wear.compose.foundation.CurvedLayout
import androidx.wear.compose.foundation.CurvedModifier
import androidx.wear.compose.foundation.CurvedScope
import androidx.wear.compose.foundation.CurvedTextStyle
import androidx.wear.compose.foundation.angularSize
import androidx.wear.compose.foundation.angularSizeDp
import androidx.wear.compose.foundation.background
import androidx.wear.compose.foundation.basicCurvedText
import androidx.wear.compose.foundation.curvedBox
import androidx.wear.compose.foundation.curvedColumn
import androidx.wear.compose.foundation.curvedComposable
import androidx.wear.compose.foundation.curvedRow
import androidx.wear.compose.foundation.padding
import androidx.wear.compose.foundation.sizeIn
import androidx.wear.compose.foundation.weight
import androidx.wear.compose.material.CompactButton
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.ToggleButton
import androidx.wear.compose.material.curvedText

@Composable
fun WarpedTextDemo() {
    val warpings =
        listOf(
            CurvedTextStyle.WarpOffset.None to "0",
            CurvedTextStyle.WarpOffset.Descent to "D",
            CurvedTextStyle.WarpOffset.Baseline to "B",
            CurvedTextStyle.WarpOffset.HalfOpticalHeight to "H",
            CurvedTextStyle.WarpOffset.HalfAscent to "A/2",
            CurvedTextStyle.WarpOffset.Ascent to "A",
        )

    var warpIx by remember { mutableIntStateOf(0) }

    val style =
        CurvedTextStyle(
            fontSize = 20.sp,
            color = Color.White,
            letterSpacing = 0.em,
            warpOffset = warpings[warpIx].first,
        )

    var arabicIx by remember { mutableIntStateOf(0) }
    val arabicTexts =
        listOf(
            "مرحبا 👋 بالعالم 🌏!",
            "مرحبا 🧑‍🚀",
            "🧑‍🚀 مرحبا",
            "الاجتماع بعد 5 دقائق",
            "الوصول إلى العمل بعد 25 دقيقة",
            "الوصول بعد دقيقتين",
            "مؤشر جودة الهواء (AQI) غير صحي • 182",
            "بعد 10 دقائق • يوغا",
            "موعد مع ليلى كمال • الساعة ‎5:05",
            "الوقت المقدر للوصول: ‎8:28 مساءً",
            "كوردانو راسل",
            "جارٍ التنقّل...",
            "جارٍ تحضير الاتجاهات...",
            "نهاية التنبيهات!",
        )

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CurvedLayout(Modifier.padding(2.dp), radialAlignment = CurvedAlignment.Radial.Center) {
            curvedComposable { Box(Modifier.size(10.dp).background(Color.Red)) }
            curvedText(
                "Huge",
                style = style.copy(fontSize = 50.sp),
                modifier = CurvedModifier.background(Color.DarkGray),
            )
            curvedComposable { Box(Modifier.size(15.dp).background(Color.Green)) }
            curvedColumn {
                curvedText(
                    arabicTexts[arabicIx],
                    style = style,
                    modifier = CurvedModifier.background(Color.Gray),
                )
                curvedText(
                    "\uD83D\uDE02 Text \uD83D\uDC4D",
                    style = style,
                    modifier = CurvedModifier.background(Color.DarkGray),
                )
            }
            curvedComposable { Box(Modifier.size(20.dp).background(Color.Blue)) }
        }
        CurvedLayout(
            Modifier.padding(5.dp),
            angularDirection = CurvedDirection.Angular.Reversed,
            anchor = 90f,
        ) {
            curvedText("Other", style = style, modifier = CurvedModifier.background(Color.Gray))
            curvedComposable { Box(Modifier.size(15.dp).background(Color.Green)) }
            curvedText("CCw", style = style, modifier = CurvedModifier.background(Color.Gray))
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CompactButton(onClick = { warpIx = warpIx.inc() % warpings.size }) {
                Text(warpings[warpIx].second, color = Color.Black, fontSize = 12.sp)
            }
            CompactButton(onClick = { arabicIx = arabicIx.inc() % arabicTexts.size }) {
                Text("Txt")
            }
        }
    }
}

@OptIn(ExperimentalAnimatableApi::class)
@Composable
fun LookaheadDemo() {
    /**
     * Creates a custom implementation of ApproachLayoutModifierNode to approach the placement of
     * the layout using an animation.
     */
    class AnimatedPlacementModifierNode(var lookaheadScope: LookaheadScope) :
        ApproachLayoutModifierNode, Modifier.Node() {
        // Creates an offset animation, the target of which will be known during placement.
        val offsetAnimation: DeferredTargetAnimation<IntOffset, AnimationVector2D> =
            DeferredTargetAnimation(IntOffset.VectorConverter)

        override fun isMeasurementApproachInProgress(lookaheadSize: IntSize): Boolean {
            // Since we only animate the placement here, we can consider measurement approach
            // complete.
            return false
        }

        // Returns true when the offset animation is in progress, false otherwise.
        override fun Placeable.PlacementScope.isPlacementApproachInProgress(
            lookaheadCoordinates: LayoutCoordinates
        ): Boolean {
            val target =
                with(lookaheadScope) {
                    lookaheadScopeCoordinates.localLookaheadPositionOf(lookaheadCoordinates).round()
                }
            offsetAnimation.updateTarget(target, coroutineScope)
            return !offsetAnimation.isIdle
        }

        override fun ApproachMeasureScope.approachMeasure(
            measurable: Measurable,
            constraints: Constraints,
        ): MeasureResult {
            val placeable = measurable.measure(constraints)
            return layout(placeable.width, placeable.height) {
                val coordinates = coordinates
                if (coordinates != null) {
                    // Calculates the target offset within the lookaheadScope
                    val target =
                        with(lookaheadScope) {
                            lookaheadScopeCoordinates.localLookaheadPositionOf(coordinates).round()
                        }

                    // Uses the target offset to start an offset animation
                    val animatedOffset = offsetAnimation.updateTarget(target, coroutineScope)
                    // Calculates the *current* offset within the given LookaheadScope
                    val placementOffset =
                        with(lookaheadScope) {
                            lookaheadScopeCoordinates
                                .localPositionOf(coordinates, Offset.Zero)
                                .round()
                        }
                    // Calculates the delta between animated position in scope and current
                    // position in scope, and places the child at the delta offset. This puts
                    // the child layout at the animated position.
                    val (x, y) = animatedOffset - placementOffset
                    placeable.place(x, y)
                } else {
                    placeable.place(0, 0)
                }
            }
        }
    }

    // Creates a custom node element for the AnimatedPlacementModifierNode above.
    data class AnimatePlacementNodeElement(val lookaheadScope: LookaheadScope) :
        ModifierNodeElement<AnimatedPlacementModifierNode>() {

        override fun update(node: AnimatedPlacementModifierNode) {
            node.lookaheadScope = lookaheadScope
        }

        override fun create(): AnimatedPlacementModifierNode {
            return AnimatedPlacementModifierNode(lookaheadScope)
        }

        override fun InspectorInfo.inspectableProperties() {
            name = "AnimatePlacementNodeElement"
        }
    }

    @Suppress("PrimitiveInCollection")
    val colors = listOf(Color(0xffff6f69), Color(0xffffcc5c), Color(0xff264653), Color(0xff2a9d84))

    var isInColumn by remember { mutableStateOf(true) }
    val isPhone = LocalConfiguration.current.screenHeightDp > 600
    CurvedLayout(Modifier.padding(top = if (isPhone) 120.dp else 0.dp)) {
        curvedComposable(modifier = CurvedModifier.background(Color.DarkGray)) {
            LookaheadScope {
                // Creates movable content containing 4 boxes. They will be put either in a [Row] or
                // in a
                // [Column] depending on the state
                val items = remember {
                    movableContentOf {
                        colors.forEach { color ->
                            Box(
                                Modifier.padding(3.dp)
                                    .size(15.dp, 15.dp)
                                    .then(AnimatePlacementNodeElement(this))
                                    .background(color, RoundedCornerShape(20))
                            )
                        }
                    }
                }

                Box(modifier = Modifier.size(85.dp).clickable { isInColumn = !isInColumn }) {
                    // As the items get moved between Column and Row, their positions in
                    // LookaheadScope
                    // will change. The `animatePlacementInScope` modifier created above will
                    // observe that final position change via `localLookaheadPositionOf`, and create
                    // a position animation.
                    if (isInColumn) {
                        Column(Modifier.fillMaxSize()) { items() }
                    } else {
                        Row { items() }
                    }
                }
            }
        }
    }
}

@Composable
fun CurvedWorldDemo() {
    CurvedLayout(modifier = Modifier.fillMaxSize()) {
        curvedComposable { Box(modifier = Modifier.size(20.dp).background(Color.Red)) }
        curvedComposable {
            Column(
                modifier = Modifier.background(Color.Gray).padding(3.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "A",
                    color = Color.Black,
                    fontSize = 16.sp,
                    modifier = Modifier.background(Color.Blue),
                )
                Row {
                    Text(
                        text = "B",
                        color = Color.Black,
                        fontSize = 16.sp,
                        modifier = Modifier.background(Color.Green).padding(2.dp),
                    )
                    Text(
                        text = "C",
                        color = Color.Black,
                        fontSize = 16.sp,
                        modifier = Modifier.background(Color.Red),
                    )
                }
            }
        }
        curvedComposable { Box(modifier = Modifier.size(20.dp).background(Color.Red)) }
    }
    CurvedLayout(
        anchor = 90F,
        anchorType = AnchorType.Start,
        angularDirection = CurvedDirection.Angular.Reversed,
    ) {
        curvedComposable {
            Text(
                text = "Start",
                color = Color.Black,
                fontSize = 30.sp,
                modifier = Modifier.background(Color.White).padding(horizontal = 10.dp),
            )
        }
    }
    CurvedLayout(
        anchor = 90F,
        anchorType = AnchorType.End,
        angularDirection = CurvedDirection.Angular.Reversed,
    ) {
        curvedComposable {
            Text(
                text = "End",
                color = Color.Black,
                fontSize = 30.sp,
                modifier = Modifier.background(Color.White).padding(horizontal = 10.dp),
            )
        }
    }
    CurvedLayout(
        modifier = Modifier.padding(50.dp),
        anchor = 90f,
        anchorType = AnchorType.Center,
        angularDirection = CurvedDirection.Angular.Reversed,
    ) {
        listOf("A", "B", "C").forEach {
            curvedComposable {
                Text(
                    text = "$it",
                    color = Color.Black,
                    fontSize = 30.sp,
                    modifier = Modifier.background(Color.White).padding(horizontal = 10.dp),
                )
            }
        }
    }
}

private fun CurvedScope.SeparatorBlock() {
    curvedComposable(radialAlignment = CurvedAlignment.Radial.Outer) {
        Box(modifier = Modifier.size(10.dp, 40.dp).background(Color.Gray))
    }
}

private fun CurvedScope.RgbBlocks() {
    curvedComposable(radialAlignment = CurvedAlignment.Radial.Outer) {
        Box(modifier = Modifier.size(20.dp).background(Color.Red))
    }
    curvedComposable(radialAlignment = CurvedAlignment.Radial.Center) {
        Box(modifier = Modifier.size(20.dp).background(Color.Green))
    }
    curvedComposable(radialAlignment = CurvedAlignment.Radial.Inner) {
        Box(modifier = Modifier.size(20.dp).background(Color.Blue))
    }
}

@Composable
fun CurvedRowAlignmentDemo() {
    CurvedLayout(modifier = Modifier.fillMaxSize()) {
        SeparatorBlock()
        RgbBlocks()
        SeparatorBlock()
        (0..10).forEach {
            curvedComposable(radialAlignment = CurvedAlignment.Radial.Custom(it / 10.0f)) {
                Box(modifier = Modifier.size(10.dp).background(Color.White))
            }
        }
        SeparatorBlock()
    }
    CurvedLayout(anchor = 90f, angularDirection = CurvedDirection.Angular.Reversed) {
        SeparatorBlock()
        RgbBlocks()
        SeparatorBlock()
    }
}

@Composable
fun BasicCurvedTextDemo() {
    CurvedLayout(modifier = Modifier.fillMaxSize().background(Color.White)) {
        SeparatorBlock()
        basicCurvedText(
            "Curved Text",
            CurvedTextStyle(fontSize = 18.sp),
            // TODO: Re-add when we implement alignment modifiers.
            // modifier = Modifier.radialAlignment(RadialAlignment.Outer)
        )
        SeparatorBlock()
        basicCurvedText(
            "And More",
            CurvedTextStyle(fontSize = 24.sp),
            angularDirection = CurvedDirection.Angular.Reversed,
            modifier = CurvedModifier.padding(angular = 5.dp),
            // TODO: Re-add when we implement alignment modifiers.
            // modifier = Modifier.radialAlignment(RadialAlignment.Inner)
        )
        SeparatorBlock()
    }
}

@Composable
fun CurvedEllipsis() {
    CurvedLayout {
        curvedRow(modifier = CurvedModifier.sizeIn(maxSweepDegrees = 90f)) {
            curvedText(
                "This text too long to actually fit in the provided space",
                modifier = CurvedModifier.weight(1f),
                overflow = TextOverflow.Ellipsis,
            )
            curvedText("10:00")
        }
    }
}

@Composable
fun CurvedLayoutDirection() {
    var layoutDirection by remember { mutableStateOf(false) }
    val actualLayoutDirection = if (layoutDirection) LayoutDirection.Rtl else LayoutDirection.Ltr
    CompositionLocalProvider(LocalLayoutDirection provides actualLayoutDirection) {
        Box {
            Row(modifier = Modifier.align(Alignment.Center)) {
                Text("LayoutDirection: ")
                ToggleButton(
                    checked = layoutDirection,
                    onCheckedChange = { layoutDirection = !layoutDirection },
                ) {
                    Text(if (layoutDirection) "Rtl" else "Ltr")
                }
            }
            repeat(2) { topDown ->
                CurvedLayout(
                    anchor = listOf(270f, 90f)[topDown],
                    angularDirection =
                        listOf(CurvedDirection.Angular.Normal, CurvedDirection.Angular.Reversed)[
                            topDown],
                ) {
                    curvedRow(CurvedModifier.background(Color.White)) {
                        basicCurvedText(
                            "Before",
                            CurvedTextStyle(fontSize = 24.sp),
                            modifier = CurvedModifier.padding(angular = 5.dp),
                        )
                        curvedColumn { repeat(3) { basicCurvedText("#$it") } }
                        curvedRow {
                            curvedComposable {
                                Text(
                                    "after",
                                    modifier = Modifier.padding(4.dp),
                                    color = Color.Black,
                                )
                            }
                            basicCurvedText(
                                "end",
                                modifier = CurvedModifier.padding(angular = 4.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CurvedBoxDemo() {
    CurvedLayout(modifier = Modifier.fillMaxSize(), anchor = 90f) {
        curvedBox(
            modifier = CurvedModifier.background(Color.Red),
            radialAlignment = CurvedAlignment.Radial.Inner,
            angularAlignment = CurvedAlignment.Angular.End,
        ) {
            curvedComposable {
                Box(modifier = Modifier.width(60.dp).height(40.dp).background(Color.Green))
            }
            curvedComposable { WhiteCircle() }
        }
    }
    CurvedLayout(modifier = Modifier.fillMaxSize(), anchor = 180f) {
        curvedBox(modifier = CurvedModifier.background(Color.Red)) {
            curvedComposable { Box(modifier = Modifier.size(60.dp).background(Color.Green)) }
            curvedComposable { WhiteCircle() }
        }
    }
    CurvedLayout(modifier = Modifier.fillMaxSize()) {
        curvedBox(
            modifier = CurvedModifier.background(Color.Red),
            radialAlignment = CurvedAlignment.Radial.Outer,
            angularAlignment = CurvedAlignment.Angular.Start,
        ) {
            curvedComposable {
                Box(modifier = Modifier.width(40.dp).height(60.dp).background(Color.Green))
            }
            curvedComposable { WhiteCircle() }
        }
    }
}

@Composable
private fun SampleIcon(
    @DrawableRes id: Int,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    background: Color = Color.Black,
) {
    Box(
        modifier
            .size(40.dp)
            .border(2.dp, Color.White, CircleShape)
            .clip(CircleShape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .background(background, CircleShape)
            .padding(3.dp)
            .paint(painterResource(id), contentScale = ContentScale.Fit)
    )
}

@Composable
fun CurvedIconsDemo() {
    Box(Modifier.fillMaxSize().padding(10.dp), contentAlignment = Alignment.Center) {
        CurvedLayout(
            modifier = Modifier.fillMaxSize(),
            anchor = 90f,
            angularDirection = CurvedDirection.Angular.CounterClockwise,
        ) {
            curvedComposable(rotationLocked = true) { Text("Foo", color = Color.White) }
            listOf(R.drawable.icon_skip_previous, R.drawable.icon_play, R.drawable.icon_skip_next)
                .forEach {
                    curvedComposable(
                        modifier = CurvedModifier.angularSize(40f),
                        rotationLocked = true,
                    ) {
                        SampleIcon(it)
                    }
                }
            curvedComposable(rotationLocked = true) { Text("Bar", color = Color.White) }
        }
    }
}

@Composable
private fun WhiteCircle() {
    Box(modifier = Modifier.size(30.dp).clip(CircleShape).background(Color.White))
}

@Composable
fun CurvedSpacingEmDemo() {
    val style = CurvedTextStyle(MaterialTheme.typography.body1)
    repeat(2) {
        CurvedLayout(
            anchor = if (it == 0) 270f else 90f,
            angularDirection =
                if (it == 0) CurvedDirection.Angular.Clockwise
                else CurvedDirection.Angular.CounterClockwise,
            modifier = Modifier.size(300.dp),
        ) {
            listOf(-0.1f, 0f, 0.05f, 0.1f, 0.15f).forEachIndexed { ix, spacing ->
                if (ix > 0) {
                    curvedBox(modifier = CurvedModifier.angularSizeDp(10.dp)) {}
                }
                basicCurvedText(
                    "| $spacing em |",
                    style =
                        style.copy(
                            letterSpacing = spacing.em,
                            letterSpacingCounterClockwise = spacing.em,
                        ),
                    modifier =
                        CurvedModifier.background(if (ix % 2 == 0) Color.DarkGray else Color.Gray),
                )
            }
        }
    }
}

@Composable
fun CurvedSpacingSpDemo() {
    val style = CurvedTextStyle(MaterialTheme.typography.body1)
    repeat(2) {
        CurvedLayout(
            anchor = if (it == 0) 270f else 90f,
            angularDirection =
                if (it == 0) CurvedDirection.Angular.Clockwise
                else CurvedDirection.Angular.CounterClockwise,
            modifier = Modifier.size(300.dp),
        ) {
            listOf(-1f, 0f, 1f, 2f).forEachIndexed { ix, spacing ->
                if (ix > 0) {
                    curvedBox(modifier = CurvedModifier.angularSizeDp(10.dp)) {}
                }
                basicCurvedText(
                    "| $spacing sp |",
                    style =
                        style.copy(
                            letterSpacing = spacing.sp,
                            letterSpacingCounterClockwise = spacing.sp,
                        ),
                    modifier =
                        CurvedModifier.background(if (ix % 2 == 0) Color.DarkGray else Color.Gray),
                )
            }
        }
    }
}
