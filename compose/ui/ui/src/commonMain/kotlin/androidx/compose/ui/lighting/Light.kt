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

package androidx.compose.ui.lighting

import androidx.annotation.FloatRange
import androidx.compose.runtime.Stable
import androidx.compose.runtime.annotation.RememberInComposition
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.neverEqualPolicy
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.colorspace.ColorSpaces
import androidx.compose.ui.internal.requirePrecondition
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.findRootCoordinates
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.isSpecified
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Connects a composable marked with Modifier.lightAnchor to the lights positioned relative to it
 * with [LightPosition.Anchored].
 *
 * @see LightPosition.Anchored
 */
@Stable
public class LightAnchorState @RememberInComposition public constructor() {
    /**
     * Placement of the anchored composable, or `null` while it is not attached or not yet placed.
     */
    internal var coordinates: LayoutCoordinates? by mutableStateOf(null, neverEqualPolicy())
}

/**
 * Where a [Light] sits, and the coordinate space that position is measured in.
 *
 * All three variants resolve to a point in the lit composable's own space; they differ only in what
 * [offset] is measured from. Use [Local] for a light that belongs to the composable it lights,
 * [Global] for one placed against the root composable, and [Anchored] for one that should follow
 * another composable.
 *
 * @see Light
 */
public abstract class LightPosition internal constructor() {

    /**
     * Position of the light within the coordinate space this describes, with `x` increasing to the
     * right and `y` increasing downwards.
     */
    public abstract val offset: DpOffset

    /**
     * A position measured from the top-left corner of the composable being lit.
     *
     * The light is part of that composable and moves with it, so the shading does not change as the
     * composable is scrolled or repositioned. This is the cheapest option, as no coordinate
     * conversion is required.
     *
     * @param offset position of the light relative to the lit composable's top-left corner.
     */
    public class Local(override val offset: DpOffset) : LightPosition() {
        override fun equals(other: Any?): Boolean =
            this === other || (other is Local && offset == other.offset)

        override fun hashCode(): Int = offset.hashCode()

        override fun toString(): String = "LightPosition.Local($offset)"
    }

    /**
     * A position measured from the top-left corner of the root of the composition that hosts the
     * lit composable, the same space as positionInRoot. A Popup or Dialog has its own root.
     *
     * The light is fixed in the root composable, so moving or scrolling the lit composable moves it
     * through the light and the shading changes accordingly. Use this for a single light shared by
     * several composables that should agree on where it is.
     *
     * @param offset position of the light relative to the root's top-left corner.
     */
    public class Global(override val offset: DpOffset) : LightPosition() {
        override fun equals(other: Any?): Boolean =
            this === other || (other is Global && offset == other.offset)

        override fun hashCode(): Int = offset.hashCode()

        override fun toString(): String = "LightPosition.Global($offset)"
    }

    /**
     * A position measured from the composable that [state] is attached to with
     * Modifier.lightAnchor. The anchor must be in the same composition hierarchy as the lit
     * composable. An anchor in a different Popup, Dialog or ComposeView contributes no light.
     *
     * The light follows that composable wherever it is laid out, without either composable needing
     * to know about the other. Until the anchored composable has been placed the position cannot be
     * resolved and the light contributes nothing.
     *
     * @param state the anchor to position against. Two anchored positions are equal only when they
     *   share the same state instance.
     * @param offset position of the light relative to the anchored composable's top-left corner.
     */
    public class Anchored(
        public val state: LightAnchorState,
        override val offset: DpOffset = DpOffset.Zero,
    ) : LightPosition() {
        override fun equals(other: Any?): Boolean =
            this === other || (other is Anchored && state === other.state && offset == other.offset)

        override fun hashCode(): Int = 31 * state.hashCode() + offset.hashCode()

        override fun toString(): String = "LightPosition.Anchored(state=$state, offset=$offset)"
    }
}

/**
 * Represents a source of light.
 *
 * Choose [DirectionalLight] for light arriving from infinitely far away, [PointLight] for a bulb at
 * a fixed spot, and [SpotLight] for a bulb that only lights a cone.
 */
@Stable
public abstract class Light internal constructor() {
    /**
     * Color emitted by this light.
     *
     * The color's alpha channel scales the emitted light alongside [intensity], so a fully
     * transparent color (alpha of `0`, such as [Color.Transparent]) emits no light.
     */
    public abstract val color: Color

    /**
     * Multiplier applied to the strength of this light.
     *
     * This scales the light's contribution alongside [color]'s alpha channel (`color.alpha *
     * intensity`). A light with an intensity of `0` contributes nothing, `1` applies [color] at its
     * base strength, values between `0` and `1` make the light more faint, and values greater than
     * `1` overdrive the light, brightening its reflection towards white.
     */
    public abstract val intensity: Float

    /**
     * Where the light sits. Position-less lights such as [DirectionalLight] report an unspecified
     * local position.
     */
    public open val position: LightPosition
        get() = LightPosition.Local(DpOffset.Unspecified)

    /**
     * Returns a copy of this light whose position is expressed in [layoutCoordinates]' local space.
     *
     * If the position cannot be resolved yet the returned light has an unspecified position, and
     * callers are expected to treat it as not contributing any light.
     */
    internal fun Density.inLocalSpace(layoutCoordinates: LayoutCoordinates): Light {
        return when (val position = position) {
            // Already expressed relative to the composable being lit.
            is LightPosition.Local -> this@Light
            is LightPosition.Anchored -> {

                val anchor = position.state.coordinates
                if (
                    anchor == null ||
                        !anchor.isAttached ||
                        !layoutCoordinates.isAttached ||
                        !position.offset.isSpecified ||
                        anchor.findRootCoordinates() !==
                            layoutCoordinates.findRootCoordinates() // Not in the same hierarchy
                ) {
                    unresolved()
                } else {
                    localCopy(layoutCoordinates.localPositionOf(anchor, toPx(position.offset)))
                }
            }
            is LightPosition.Global -> {
                val rootCoordinates = layoutCoordinates.findRootCoordinates()
                if (
                    !rootCoordinates.isAttached ||
                        !layoutCoordinates.isAttached ||
                        !position.offset.isSpecified
                ) {
                    unresolved()
                } else {
                    localCopy(
                        layoutCoordinates.localPositionOf(rootCoordinates, toPx(position.offset))
                    )
                }
            }

            else -> {
                // Should never happen
                unresolved()
            }
        }
    }

    private fun Density.localCopy(offset: Offset): Light =
        copy(LightPosition.Local(DpOffset(offset.x.toDp(), offset.y.toDp())))

    private fun unresolved(): Light = copy(LightPosition.Local(DpOffset.Unspecified))

    /**
     * Returns a copy of this light at [position], or this light itself if it has no position (see
     * [DirectionalLight]).
     */
    internal abstract fun copy(position: LightPosition): Light

    /**
     * Packs this light's uniform values at [index] into the provided shader uniform arrays.
     *
     * Callers must resolve this light's position via [inLocalSpace] before invoking this function.
     */
    internal abstract fun Density.packUniforms(
        index: Int,
        lightTypes: IntArray,
        lightColors: FloatArray,
        lightPositions: FloatArray,
        lightDirections: FloatArray,
        lightFalloffRadii: FloatArray,
        lightCutoffCosines: FloatArray,
        lightIntensities: FloatArray,
    )

    internal fun packColorAndIntensity(
        index: Int,
        lightColors: FloatArray,
        lightIntensities: FloatArray,
    ) {
        val colorOffset = index * LightColorComponents
        val linearColor = color.convert(ColorSpaces.LinearExtendedSrgb)
        lightColors[colorOffset] = linearColor.red
        lightColors[colorOffset + 1] = linearColor.green
        lightColors[colorOffset + 2] = linearColor.blue
        lightColors[colorOffset + 3] = linearColor.alpha
        lightIntensities[index] = intensity
    }

    /**
     * Writes [position] and [elevationPx] into [lightPositions], returning `false` if the position
     * could not be resolved yet (for example because the light's anchor has not been placed).
     */
    internal fun Density.packPosition(
        lightPositions: FloatArray,
        vectorOffset: Int,
        elevationPx: Float,
    ): Boolean {
        val pos = position
        requirePrecondition(pos is LightPosition.Local) {
            "Lights must be resolved into local space before they are uploaded, but $pos was found"
        }
        val offset = pos.offset
        if (!offset.isSpecified) return false

        lightPositions[vectorOffset] = offset.x.toPx()
        lightPositions[vectorOffset + 1] = offset.y.toPx()
        lightPositions[vectorOffset + 2] = elevationPx
        return true
    }
}

internal const val LightTypeDirectional: Int = 0
internal const val LightTypePoint: Int = 1
internal const val LightTypeSpot: Int = 2

internal const val LightColorComponents: Int = 4
internal const val LightVectorComponents: Int = 3

private fun Density.toPx(offset: DpOffset) = Offset(offset.x.toPx(), offset.y.toPx())

private fun Float.toRadians(): Float = this * (PI.toFloat() / 180f)

private fun calculateTargetOffsetFromAngles(
    elevation: Dp,
    planeAngleDegrees: Float,
    elevationDegrees: Float,
): DpOffset {
    requirePrecondition(elevationDegrees > 0f && elevationDegrees <= 90f) {
        "elevationDegrees must be in (0, 90], but was $elevationDegrees"
    }
    requirePrecondition(elevation.isSpecified) { "elevation must be specified" }
    val elevationRadians = elevationDegrees.toRadians()
    val planeRadians = planeAngleDegrees.toRadians()
    val reach = elevation * (cos(elevationRadians) / sin(elevationRadians))
    return DpOffset(x = reach * -cos(planeRadians), y = reach * -sin(planeRadians))
}

/**
 * A light that is infinitely far away, such as the sun.
 *
 * The light is described by the vector `(directionX, directionY, directionZ)`, which points from
 * the lit surface *towards* the light. It says where the light is coming from rather than where it
 * is shining. Only the ratio between the three components matters, as the vector is normalized
 * before use.
 *
 * [directionZ] is normally positive, which places the light in front of the surface on the viewer's
 * side. A light level with or behind the surface leaves it unlit.
 *
 * @param color color emitted by this light.
 * @param intensity multiplier applied to [color]. A light with an intensity of `0` contributes
 *   nothing.
 * @param directionX the `x` component of the vector pointing towards the light. Positive values are
 *   to the right.
 * @param directionY the `y` component of the vector pointing towards the light. Positive values are
 *   downwards.
 * @param directionZ the `z` component of the vector pointing towards the light. Positive values are
 *   towards the viewer. Larger values light the surface more evenly, as the light moves towards
 *   being head on.
 */
@Stable
public class DirectionalLight(
    override val color: Color,
    public val directionX: Float,
    public val directionY: Float,
    public val directionZ: Float,
    @FloatRange(from = 0.0) override val intensity: Float,
) : Light() {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DirectionalLight) return false
        if (color != other.color) return false
        if (directionX != other.directionX) return false
        if (directionY != other.directionY) return false
        if (intensity != other.intensity) return false
        if (directionZ != other.directionZ) return false
        return true
    }

    override fun hashCode(): Int {
        var result = color.hashCode()
        result = 31 * result + directionX.hashCode()
        result = 31 * result + directionY.hashCode()
        result = 31 * result + directionZ.hashCode()
        result = 31 * result + intensity.hashCode()
        return result
    }

    override fun toString(): String =
        "DirectionalLight(color=$color, directionX=$directionX, directionY=$directionY, directionZ=$directionZ, intensity=$intensity)"

    // A directional light has no position, so there is nothing to re-express in another space.
    override fun copy(position: LightPosition): Light = this

    override fun Density.packUniforms(
        index: Int,
        lightTypes: IntArray,
        lightColors: FloatArray,
        lightPositions: FloatArray,
        lightDirections: FloatArray,
        lightFalloffRadii: FloatArray,
        lightCutoffCosines: FloatArray,
        lightIntensities: FloatArray,
    ) {
        packColorAndIntensity(index, lightColors, lightIntensities)
        lightTypes[index] = LightTypeDirectional
        val vectorOffset = index * LightVectorComponents
        lightDirections[vectorOffset] = directionX
        lightDirections[vectorOffset + 1] = directionY
        lightDirections[vectorOffset + 2] = directionZ
    }

    public companion object {
        /**
         * Creates a [DirectionalLight] from angles instead of a direction vector.
         *
         * @param color color emitted by this light.
         * @param planeAngleDegrees where the light comes from within the plane of the surface, in
         *   degrees clockwise from the right: `0` is from the right, `90` from below, `180` from
         *   the left and `270` from above.
         * @param elevationDegrees how far the light is raised above the surface, in degrees: `90`
         *   shines head on from the viewer, and `0` is level with the surface, which leaves it
         *   unlit.
         * @param intensity multiplier applied to [color]. A light with an intensity of `0`
         *   contributes nothing.
         */
        public fun fromAngles(
            color: Color,
            planeAngleDegrees: Float,
            elevationDegrees: Float,
            @FloatRange(from = 0.0) intensity: Float,
        ): DirectionalLight =
            DirectionalLight(
                color = color,
                intensity = intensity,
                directionX = cos(elevationDegrees.toRadians()) * cos(planeAngleDegrees.toRadians()),
                directionY = cos(elevationDegrees.toRadians()) * sin(planeAngleDegrees.toRadians()),
                directionZ = sin(elevationDegrees.toRadians()),
            )
    }
}

/**
 * A light that radiates equally in every direction from a single point in front of the surface,
 * like a bare bulb.
 *
 * [elevation] is measured from the plane the surface rests on. A surface with a thickness (see
 * [SurfaceGeometrySpec]) rises towards the light, so the light's height above it is `elevation -
 * thickness`. Keep [elevation] larger than the thickness so the light stays in front of the
 * surface.
 *
 * Brightness falls away with distance and reaches zero at [falloffRadius]. The distance is measured
 * in three dimensions, so it includes the light's height above the surface: a light raised
 * `falloffRadius` or more above the surface cannot reach it at all, and lights nothing. Keep
 * [falloffRadius] comfortably larger than `elevation - thickness`.
 *
 * @param color color emitted by this light.
 * @param position where the light sits in the plane of the surface.
 * @param elevation distance of the light in front of the plane the surface rests on, towards the
 *   viewer. The light's height above the surface itself is `elevation - thickness`.
 * @param falloffRadius distance at which the light has fully faded out. A radius of zero or less
 *   disables falloff entirely, so the light reaches the whole surface at full strength.
 * @param intensity multiplier applied to [color]. A light with an intensity of `0` contributes
 *   nothing.
 * @throws IllegalArgumentException if [elevation] or [falloffRadius] is [Dp.Unspecified].
 */
@Stable
public class PointLight(
    override val color: Color,
    override val position: LightPosition,
    public val elevation: Dp,
    public val falloffRadius: Dp,
    @FloatRange(from = 0.0) override val intensity: Float,
) : Light() {

    init {
        requirePrecondition(elevation.isSpecified) { "elevation must be specified" }
        requirePrecondition(falloffRadius.isSpecified) { "falloffRadius must be specified" }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PointLight) return false
        if (color != other.color) return false
        if (position != other.position) return false
        if (elevation != other.elevation) return false
        if (falloffRadius != other.falloffRadius) return false
        if (intensity != other.intensity) return false
        return true
    }

    override fun hashCode(): Int {
        var result = color.hashCode()
        result = 31 * result + position.hashCode()
        result = 31 * result + elevation.hashCode()
        result = 31 * result + falloffRadius.hashCode()
        result = 31 * result + intensity.hashCode()
        return result
    }

    override fun toString(): String =
        "PointLight(color=$color, position=$position, elevation=$elevation, " +
            "falloffRadius=$falloffRadius, intensity=$intensity)"

    override fun copy(position: LightPosition): Light =
        PointLight(
            color = color,
            position = position,
            elevation = elevation,
            falloffRadius = falloffRadius,
            intensity = intensity,
        )

    override fun Density.packUniforms(
        index: Int,
        lightTypes: IntArray,
        lightColors: FloatArray,
        lightPositions: FloatArray,
        lightDirections: FloatArray,
        lightFalloffRadii: FloatArray,
        lightCutoffCosines: FloatArray,
        lightIntensities: FloatArray,
    ) {
        packColorAndIntensity(index, lightColors, lightIntensities)
        lightTypes[index] = LightTypePoint
        lightFalloffRadii[index] = falloffRadius.toPx()
        val vectorOffset = index * LightVectorComponents
        if (!packPosition(lightPositions, vectorOffset, elevation.toPx())) {
            lightIntensities[index] = 0f
        }
    }
}

/**
 * A light that sits in front of the surface like a [PointLight], but emits only within a cone, so
 * it lights a bounded pool of the surface rather than all of it.
 *
 * The cone axis runs from the light to the point on the surface at [targetOffset] from [position],
 * and [coneAngleDegrees] opens the cone out around that axis. The cone edge is hard: a pixel is
 * either inside the cone and fully lit or outside it and not lit at all, with no soft penumbra.
 *
 * As with a [PointLight], [elevation] is measured from the plane the surface rests on, so the light
 * is `elevation - thickness` above a surface with a thickness (see [SurfaceGeometrySpec]). The
 * point the cone aims at is on the surface. Brightness within the cone falls away with distance
 * exactly as it does for a [PointLight]. The [falloffRadius] must be comfortably larger than
 * `elevation - thickness` for the light to reach the surface.
 *
 * @param color color emitted by this light.
 * @param position where the light sits in the plane of the surface.
 * @param targetOffset the in-plane offset from [position] to the point on the surface that the cone
 *   aims at. Together with [elevation] this defines the cone axis, which points from the light onto
 *   the surface. [DpOffset.Zero] aims at the point directly beneath the light, so the cone meets
 *   the surface head on.
 * @param elevation distance of the light in front of the plane the surface rests on, towards the
 *   viewer. The light's height above the surface itself is `elevation - thickness`.
 * @param falloffRadius distance at which the light has fully faded out. A radius of zero or less
 *   disables falloff entirely.
 * @param coneAngleDegrees half angle of the cone, in degrees. This is measured from the axis to the
 *   cone edge, so the full width of the cone is twice this value. Pixels outside the cone are not
 *   lit.
 * @param intensity multiplier applied to [color]. A light with an intensity of `0` contributes
 *   nothing.
 * @throws IllegalArgumentException if [targetOffset] is [DpOffset.Unspecified], or if [elevation]
 *   or [falloffRadius] is [Dp.Unspecified].
 */
@Stable
public class SpotLight(
    override val color: Color,
    override val position: LightPosition,
    public val targetOffset: DpOffset,
    public val elevation: Dp,
    public val falloffRadius: Dp,
    @FloatRange(from = 0.0, to = 180.0) public val coneAngleDegrees: Float,
    @FloatRange(from = 0.0) override val intensity: Float,
) : Light() {

    init {
        requirePrecondition(targetOffset.isSpecified) { "targetOffset must be specified" }
        requirePrecondition(elevation.isSpecified) { "elevation must be specified" }
        requirePrecondition(falloffRadius.isSpecified) { "falloffRadius must be specified" }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SpotLight) return false
        if (color != other.color) return false
        if (position != other.position) return false
        if (targetOffset != other.targetOffset) return false
        if (elevation != other.elevation) return false
        if (falloffRadius != other.falloffRadius) return false
        if (coneAngleDegrees != other.coneAngleDegrees) return false
        if (intensity != other.intensity) return false
        return true
    }

    override fun hashCode(): Int {
        var result = color.hashCode()
        result = 31 * result + position.hashCode()
        result = 31 * result + targetOffset.hashCode()
        result = 31 * result + elevation.hashCode()
        result = 31 * result + falloffRadius.hashCode()
        result = 31 * result + coneAngleDegrees.hashCode()
        result = 31 * result + intensity.hashCode()
        return result
    }

    override fun toString(): String =
        "SpotLight(color=$color, position=$position, targetOffset=$targetOffset, " +
            "elevation=$elevation, falloffRadius=$falloffRadius, " +
            "coneAngleDegrees=$coneAngleDegrees, intensity=$intensity)"

    override fun copy(position: LightPosition): Light =
        SpotLight(
            color = color,
            position = position,
            targetOffset = targetOffset,
            elevation = elevation,
            falloffRadius = falloffRadius,
            coneAngleDegrees = coneAngleDegrees,
            intensity = intensity,
        )

    override fun Density.packUniforms(
        index: Int,
        lightTypes: IntArray,
        lightColors: FloatArray,
        lightPositions: FloatArray,
        lightDirections: FloatArray,
        lightFalloffRadii: FloatArray,
        lightCutoffCosines: FloatArray,
        lightIntensities: FloatArray,
    ) {
        packColorAndIntensity(index, lightColors, lightIntensities)
        lightTypes[index] = LightTypeSpot
        lightFalloffRadii[index] = falloffRadius.toPx()
        val elevationPx = elevation.toPx()
        val vectorOffset = index * LightVectorComponents
        lightDirections[vectorOffset] = targetOffset.x.toPx()
        lightDirections[vectorOffset + 1] = targetOffset.y.toPx()
        lightDirections[vectorOffset + 2] = -elevationPx
        lightCutoffCosines[index] = cos(coneAngleDegrees.toRadians())
        if (!packPosition(lightPositions, vectorOffset, elevationPx)) {
            lightIntensities[index] = 0f
        }
    }

    public companion object {
        /**
         * Creates a [SpotLight] whose cone axis is given by angles instead of a [targetOffset].
         *
         * The angles decide where the light aims at from its [position]. They are measured as if
         * the surface had no thickness, so on a thicker surface the light arrives slightly lower
         * than [elevationDegrees].
         *
         * @param color color emitted by this light.
         * @param position where the light sits in the plane of the surface.
         * @param planeAngleDegrees where the light comes from within the plane of the surface, as
         *   seen from the point it aims at, in degrees clockwise from the right: `0` is from the
         *   right, `90` from below, `180` from the left and `270` from top.
         * @param elevationDegrees angle between the cone axis and the surface, in degrees: `90`
         *   aims at the point directly beneath the light, and smaller values tilt the cone further
         *   across the surface. Must be in `(0, 90]`, as an axis level with the surface or behind
         *   it never reaches the surface.
         * @param elevation distance of the light in front of the plane the surface rests on,
         *   towards the viewer. The light's height above the surface itself is `elevation -
         *   thickness`.
         * @param falloffRadius distance at which the light has fully faded out. A radius of zero or
         *   less disables falloff entirely.
         * @param coneAngleDegrees half angle of the cone, in degrees. This is measured from the
         *   axis to the cone edge, so the full width of the cone is twice this value. Pixels
         *   outside the cone are not lit.
         * @param intensity multiplier applied to [color]. A light with an intensity of `0`
         *   contributes nothing.
         * @throws IllegalArgumentException if [elevationDegrees] is not in `(0, 90]`, or if
         *   [elevation] or [falloffRadius] is [Dp.Unspecified].
         */
        public fun fromAngles(
            color: Color,
            position: LightPosition,
            planeAngleDegrees: Float,
            @FloatRange(from = 0.0, to = 90.0, fromInclusive = false) elevationDegrees: Float,
            elevation: Dp,
            falloffRadius: Dp,
            @FloatRange(from = 0.0, to = 180.0) coneAngleDegrees: Float,
            @FloatRange(from = 0.0) intensity: Float,
        ): SpotLight =
            SpotLight(
                color = color,
                position = position,
                targetOffset =
                    calculateTargetOffsetFromAngles(elevation, planeAngleDegrees, elevationDegrees),
                elevation = elevation,
                falloffRadius = falloffRadius,
                coneAngleDegrees = coneAngleDegrees,
                intensity = intensity,
            )
    }
}
