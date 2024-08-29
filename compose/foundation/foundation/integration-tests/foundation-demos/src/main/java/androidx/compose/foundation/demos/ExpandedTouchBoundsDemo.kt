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

package androidx.compose.foundation.demos

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.SuspendingPointerInputModifierNode
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.DelegatingNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.PointerInputModifierNode
import androidx.compose.ui.node.TouchBoundsExpansion
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.node.requireDensity
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.ViewConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastAny
import kotlin.random.Random

private val padding = 50.dp

@Composable
fun ExpandedTouchBoundsDemo() {
    LazyColumn(horizontalAlignment = Alignment.CenterHorizontally) {
        item {
            Text(
                "When hit at overlapped expanded touch bounds, the pointer input is shared. \n" +
                    "When directly hit at a Box that's overlapped with expanded touch bounds, " +
                    " only the Box get the event."
            )
            Column(modifier = Modifier.padding(vertical = padding)) {
                Box(modifier = Modifier.size(100.dp, 50.dp).touchDetector())
                Spacer(modifier = Modifier.height(padding / 2))
                Box(modifier = Modifier.size(100.dp, 50.dp).touchDetector(padding))
                Spacer(modifier = Modifier.height(padding / 2))
                Box(modifier = Modifier.size(100.dp, 50.dp).touchDetector(padding))
            }
        }

        item {
            Text(
                "When hit at overlapped expanded touch bound, the pointer events can be" +
                    " shared with cousins."
            )
            Column(modifier = Modifier.padding(vertical = padding)) {
                Box(modifier = Modifier.size(100.dp, 50.dp).touchDetector(padding))
                Spacer(modifier = Modifier.height(padding / 2))
                Column {
                    Box(modifier = Modifier.size(100.dp, 50.dp).touchDetector(padding)) {
                        Text(text = "Cousin", modifier = Modifier.align(Alignment.Center))
                    }
                }
            }
        }

        item {
            Text(
                "When the expanded touch bounds overlapped with sibling's child, the " +
                    "sibling and its child get the event."
            )
            Column(modifier = Modifier.padding(vertical = padding)) {
                Box(modifier = Modifier.size(100.dp, 50.dp).touchDetector(padding))
                Spacer(modifier = Modifier.height(padding))
                Box(modifier = Modifier.size(100.dp, 50.dp).touchDetector(padding)) {
                    Box(
                        modifier =
                            Modifier.size(50.dp, 50.dp)
                                .offset(x = 25.dp, y = (-50).dp)
                                .touchDetector()
                    )
                }
            }
        }

        item {
            Text(
                "When the parent has pointer input modifier, the child can still receive" +
                    " event in the expanded touch bounds."
            )
            Box(modifier = Modifier.size(300.dp, 250.dp).touchDetector()) {
                Box(
                    modifier =
                        Modifier.size(100.dp, 50.dp).align(Alignment.Center).touchDetector(padding)
                )
            }
        }

        item {
            Text(
                "When the expanded touch bounds overlapped with a sibling's minimum touch " +
                    "bounds, the node with expanded touch bounds gets the event.\n" +
                    "Note: the minimum touch bounds is drawn in dotted line."
            )
            Column(modifier = Modifier.padding(vertical = padding)) {
                Box(modifier = Modifier.size(100.dp, 50.dp).touchDetector(padding)) {}
                Spacer(modifier = Modifier.height(padding))
                CompositionLocalProvider(
                    LocalViewConfiguration provides
                        object : ViewConfiguration by LocalViewConfiguration.current {
                            override val minimumTouchTargetSize = DpSize(200.dp, 150.dp)
                        }
                ) {
                    Box(modifier = Modifier.size(100.dp, 50.dp).touchDetector())
                }
            }
        }

        item {
            Text(
                "Parent can't intercept out of bounds child events if the child is hit in" +
                    "expanded touch bounds"
            )
            Box(modifier = Modifier.padding(padding)) {
                Box(
                    modifier =
                        Modifier.size(300.dp, 150.dp)
                            .touchDetector(interceptOutOfBoundsChildEvents = true)
                ) {
                    Box(
                        modifier =
                            Modifier.size(200.dp, 100.dp)
                                .touchDetector(interceptOutOfBoundsChildEvents = true)
                    ) {
                        Box(modifier = Modifier.size(100.dp, 50.dp).touchDetector(padding))
                    }
                }
            }
        }
    }
}

/**
 * A touch detector whose touch bounds was expanded by [touchBoundsExpansion] [Dp]s in each
 * direction.
 */
fun Modifier.touchDetector(touchBoundsExpansion: Dp): Modifier =
    this then
        TouchDetectorWithExpandedBoundsElement(
            interceptOutOfBoundsChildEvents = false,
            touchBoundsExpansionDp = touchBoundsExpansion
        )

fun Modifier.touchDetector(interceptOutOfBoundsChildEvents: Boolean = false): Modifier =
    this then TouchDetectorWithExpandedBoundsElement(interceptOutOfBoundsChildEvents, 0.dp)

internal data class TouchDetectorWithExpandedBoundsElement(
    val interceptOutOfBoundsChildEvents: Boolean,
    val touchBoundsExpansionDp: Dp
) : ModifierNodeElement<TouchDetectorWithExpandedBoundsNode>() {
    override fun create(): TouchDetectorWithExpandedBoundsNode {
        return TouchDetectorWithExpandedBoundsNode(
            nextColor(),
            interceptOutOfBoundsChildEvents,
            touchBoundsExpansionDp
        )
    }

    override fun update(node: TouchDetectorWithExpandedBoundsNode) {
        node.interceptOutOfBoundsChildEvents = interceptOutOfBoundsChildEvents
        node.touchBoundsExpansionDp = touchBoundsExpansionDp
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "TouchDetectorWithExpandedBounds"
        properties["interceptOutOfBoundsChildEvents"] = interceptOutOfBoundsChildEvents
        properties["touchBoundsExpansionDp"] = touchBoundsExpansionDp
    }
}

internal class TouchDetectorWithExpandedBoundsNode(
    val background: Color = Color.Cyan,
    var interceptOutOfBoundsChildEvents: Boolean = false,
    var touchBoundsExpansionDp: Dp = 0.dp
) :
    DelegatingNode(),
    PointerInputModifierNode,
    DrawModifierNode,
    CompositionLocalConsumerModifierNode {
    private var color = background

    override val touchBoundsExpansion: TouchBoundsExpansion
        get() =
            if (touchBoundsExpansionDp.value <= 0) {
                TouchBoundsExpansion.None
            } else {
                val touchBoundsExpansionPx =
                    with(requireDensity()) { touchBoundsExpansionDp.toPx().toInt() }
                TouchBoundsExpansion.Absolute(
                    touchBoundsExpansionPx,
                    touchBoundsExpansionPx,
                    touchBoundsExpansionPx,
                    touchBoundsExpansionPx
                )
            }

    private val pointerInputNode =
        delegate(
            SuspendingPointerInputModifierNode {
                awaitEachGesture {
                    awaitFirstDown()
                    color = background.highlight()
                    invalidateDraw()
                    while (true) {
                        val event = awaitPointerEvent()
                        if (event.changes.fastAny { it.changedToUp() }) {
                            color = background
                            invalidateDraw()
                            break
                        }
                    }
                }
            }
        )

    private val minimumTouchTargetSize: DpSize
        get() = currentValueOf(LocalViewConfiguration).minimumTouchTargetSize

    override fun interceptOutOfBoundsChildEvents(): Boolean {
        return interceptOutOfBoundsChildEvents
    }

    override fun onPointerEvent(
        pointerEvent: PointerEvent,
        pass: PointerEventPass,
        bounds: IntSize
    ) {
        pointerInputNode.onPointerEvent(pointerEvent, pass, bounds)
    }

    override fun onCancelPointerInput() {
        pointerInputNode.onCancelPointerInput()
    }

    override fun ContentDrawScope.draw() {
        drawRect(color)
        // Draw the expanded touch bounds if touchBoundsExpansion is not None.
        if (touchBoundsExpansion != TouchBoundsExpansion.None) {
            val touchBoundsExpansionPx = touchBoundsExpansionDp.toPx()
            drawRect(
                background.copy(alpha = 0.75f),
                topLeft = Offset(-touchBoundsExpansionPx, -touchBoundsExpansionPx),
                size =
                    Size(
                        width = size.width + touchBoundsExpansionPx * 2,
                        height = size.height + touchBoundsExpansionPx * 2
                    ),
                style = Stroke(width = 2f)
            )
        }

        // Draw the minimal touch target bounds if it's larger than the touch bounds of this
        // modifier.
        val minTouchTargetWidth = minimumTouchTargetSize.width.toPx()
        val minTouchTargetHeight = minimumTouchTargetSize.height.toPx()
        if (size.width < minTouchTargetWidth || size.height < minTouchTargetHeight) {
            drawRect(
                background.copy(alpha = 0.75f),
                topLeft =
                    Offset(
                        (size.width - minTouchTargetWidth) / 2,
                        (size.height - minTouchTargetHeight) / 2
                    ),
                size = Size(width = minTouchTargetWidth, height = minTouchTargetHeight),
                style =
                    Stroke(width = 2f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(5f, 5f)))
            )
        }
        drawContent()
    }
}

private val random = Random(0)

fun nextColor(): Color {
    return Color(random.nextInt() or 0xFF000000.toInt())
}

fun Color.highlight(): Color {
    return lerp(this, Color.Black, 0.5f)
}
