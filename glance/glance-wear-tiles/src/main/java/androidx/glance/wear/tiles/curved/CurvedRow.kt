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

package androidx.glance.wear.tiles.curved

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.glance.Emittable
import androidx.glance.EmittableWithChildren
import androidx.glance.GlanceModifier
import androidx.glance.GlanceNode
import androidx.glance.unit.ColorProvider

/**
 * A curved layout container. This container will fill itself to a circle, which fits inside its
 * parent container, and all of its children will be placed on that circle. The parameters
 * [anchorDegrees] and [anchorType] can be used to specify where to draw children within this
 * circle. Each child will then be placed, one after the other, clockwise around the circle.
 *
 * While this container can hold any composable element, only those built specifically to work
 * inside of a CurvedRow container (e.g. [CurvedScope.curvedText]) will adapt themselves to the
 * CurvedRow. Any other element wrapped in [CurvedScope.curvedComposable] will be drawn normally, at
 * a tangent to the circle or straight up depending on the value of rotateContent.
 *
 * @param modifier Modifiers for this container.
 * @param anchorDegrees The angle for the anchor in degrees, used with [anchorType] to determine
 *   where to draw children. Note that 0 degrees is the 3 o'clock position on a device, and the
 *   angle sweeps clockwise. Values do not have to be clamped to the range 0-360; values less than 0
 *   degrees will sweep anti-clockwise (i.e. -90 degrees is equivalent to 270 degrees), and
 *   values >360 will be be placed at X mod 360 degrees.
 * @param anchorType Alignment of the contents of this container relative to [anchorDegrees].
 * @param radialAlignment specifies where to lay down children that are thinner than the CurvedRow,
 *   either closer to the center (INNER), apart from the center (OUTER) or in the middle point
 *   (CENTER).
 * @param content The content of this [CurvedRow].
 */
@Composable
@Deprecated("glance-wear-tiles is deprecated and will be removed")
public fun CurvedRow(
    modifier: GlanceModifier = GlanceModifier,
    anchorDegrees: Float = 270f,
    anchorType: AnchorType = AnchorType.Center,
    radialAlignment: RadialAlignment = RadialAlignment.Center,
    content: CurvedScope.() -> Unit,
) {
    GlanceNode(
        factory = ::EmittableCurvedRow,
        update = {
            this.set(modifier) { this.modifier = it }
            this.set(anchorDegrees) { this.anchorDegrees = it }
            this.set(anchorType) { this.anchorType = it }
            this.set(radialAlignment) { this.radialAlignment = it }
        },
        content = applyCurvedScope(content),
    )
}

private fun applyCurvedScope(content: CurvedScope.() -> Unit): @Composable () -> Unit {
    val curvedChildList = mutableListOf<@Composable CurvedChildScope.() -> Unit>()
    val curvedScopeImpl =
        object : CurvedScope {
            override fun curvedComposable(rotateContent: Boolean, content: @Composable () -> Unit) {
                curvedChildList.add { CurvedChild(rotateContent, content) }
            }

            override fun curvedText(
                text: String,
                curvedModifier: GlanceCurvedModifier,
                style: CurvedTextStyle?,
            ) {
                curvedChildList.add {
                    GlanceNode(
                        factory = ::EmittableCurvedText,
                        update = {
                            this.set(text) { this.text = it }
                            this.set(curvedModifier) { this.curvedModifier = it }
                            this.set(style) { this.style = it }
                        },
                    )
                }
            }

            override fun curvedLine(color: ColorProvider, curvedModifier: GlanceCurvedModifier) {
                curvedChildList.add {
                    GlanceNode(
                        factory = ::EmittableCurvedLine,
                        update = {
                            this.set(color) { this.color = it }
                            this.set(curvedModifier) { this.curvedModifier = it }
                        },
                    )
                }
            }

            override fun curvedSpacer(curvedModifier: GlanceCurvedModifier) {
                curvedChildList.add {
                    GlanceNode(
                        factory = ::EmittableCurvedSpacer,
                        update = { this.set(curvedModifier) { this.curvedModifier = it } },
                    )
                }
            }
        }

    curvedScopeImpl.apply(content)

    return { curvedChildList.forEach { composable -> object : CurvedChildScope {}.composable() } }
}

@Composable
private fun CurvedChild(rotateContent: Boolean, content: @Composable () -> Unit) {
    GlanceNode(
        factory = ::EmittableCurvedChild,
        update = { this.set(rotateContent) { this.rotateContent = it } },
        content = content,
    )
}

internal class EmittableCurvedRow : EmittableWithChildren() {
    override var modifier: GlanceModifier = GlanceModifier

    var anchorDegrees: Float = 270f
    var anchorType: AnchorType = AnchorType.Center
    var radialAlignment: RadialAlignment = RadialAlignment.Center

    override fun copy(): Emittable =
        EmittableCurvedRow().also {
            it.modifier = modifier
            it.anchorDegrees = anchorDegrees
            it.anchorType = anchorType
            it.radialAlignment = radialAlignment
            it.children.addAll(children.map { it.copy() })
        }

    override fun toString(): String =
        "EmittableCurvedRow(modifier=$modifier, anchorDegrees=$anchorDegrees," +
            "anchorType=$anchorType, children=[\n{${childrenToString()}}\n])"
}

internal class EmittableCurvedChild : EmittableWithChildren() {
    override var modifier: GlanceModifier = GlanceModifier

    var rotateContent: Boolean = false

    override fun copy(): Emittable =
        EmittableCurvedChild().also {
            it.modifier = modifier
            it.rotateContent = rotateContent
            it.children.addAll(children.map { it.copy() })
        }

    override fun toString(): String =
        "EmittableCurvedChild(" +
            "modifier=$modifier, " +
            "rotateContent=$rotateContent" +
            "children=[\n${childrenToString()}\n]" +
            ")"
}

internal class EmittableCurvedText : Emittable {
    override var modifier: GlanceModifier = GlanceModifier

    var curvedModifier: GlanceCurvedModifier = GlanceCurvedModifier
    var text: String = ""
    var style: CurvedTextStyle? = null

    override fun copy(): Emittable =
        EmittableCurvedText().also {
            it.modifier = modifier
            it.curvedModifier = curvedModifier
            it.text = text
            it.style = style
        }

    override fun toString(): String =
        "EmittableCurvedText(" +
            "modifier=$modifier, " +
            "curvedModifier=$curvedModifier, " +
            "text=$text, " +
            "style=$style" +
            ")"
}

internal class EmittableCurvedLine : Emittable {
    override var modifier: GlanceModifier = GlanceModifier

    var color: ColorProvider = ColorProvider(Color.Transparent)
    var curvedModifier: GlanceCurvedModifier = GlanceCurvedModifier

    override fun copy(): Emittable =
        EmittableCurvedLine().also {
            it.modifier = modifier
            it.color = color
            it.curvedModifier = curvedModifier
        }

    override fun toString(): String =
        "EmittableCurvedLine(" +
            "modifier=$modifier, " +
            "color=$color, " +
            "curvedModifier=$curvedModifier" +
            ")"
}

internal class EmittableCurvedSpacer : Emittable {
    override var modifier: GlanceModifier = GlanceModifier

    var curvedModifier: GlanceCurvedModifier = GlanceCurvedModifier

    override fun copy(): Emittable =
        EmittableCurvedSpacer().also {
            it.modifier = modifier
            it.curvedModifier = curvedModifier
        }

    override fun toString(): String =
        "EmittableCurvedSpacer(" + "modifier=$modifier, " + "curvedModifier=$curvedModifier" + ")"
}

@Deprecated("glance-wear-tiles is deprecated and will be removed")
@DslMarker
public annotation class CurvedScopeMarker

@Deprecated("glance-wear-tiles is deprecated and will be removed")
@CurvedScopeMarker
public interface CurvedChildScope

@Deprecated("glance-wear-tiles is deprecated and will be removed")
@JvmDefaultWithCompatibility
/** A scope for elements which can only be contained within a [CurvedRow]. */
@CurvedScopeMarker
public interface CurvedScope {

    /**
     * Component that allows normal composable to be part of a [CurvedRow]
     *
     * @param rotateContent whether to rotate the composable at a tangent to the circle
     * @param content The content of this [curvedComposable].
     */
    public fun curvedComposable(rotateContent: Boolean = true, content: @Composable () -> Unit)

    /**
     * A text element which will draw curved text. This is only valid as a direct descendant of a
     * [CurvedRow]
     *
     * Note: The sweepAngle/thickness from curvedModifier is ignored by CurvedText, its size is
     * measured with the set text and text style
     *
     * @param text The text to render.
     * @param curvedModifier [GlanceCurvedModifier] to apply to this layout element.
     * @param style The style to use for the Text.
     */
    // TODO(b/227327952) Make CurvedText accepts sweepAngle/thickness in CurveModifier
    public fun curvedText(
        text: String,
        curvedModifier: GlanceCurvedModifier = GlanceCurvedModifier,
        style: CurvedTextStyle? = null,
    )

    /**
     * A line that can be used in a [CurvedRow] and renders as a curved bar.
     *
     * @param color The color of this line.
     * @param curvedModifier [GlanceCurvedModifier] to apply to this layout element.
     */
    public fun curvedLine(
        color: ColorProvider,
        curvedModifier: GlanceCurvedModifier = GlanceCurvedModifier,
    )

    /**
     * A simple spacer used to provide padding between adjacent elements in a [CurvedRow].
     *
     * @param curvedModifier [GlanceCurvedModifier] to apply to this layout element.
     */
    public fun curvedSpacer(curvedModifier: GlanceCurvedModifier = GlanceCurvedModifier)
}
