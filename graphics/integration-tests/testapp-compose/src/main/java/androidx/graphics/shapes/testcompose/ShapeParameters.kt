/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.graphics.shapes.testcompose

import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Matrix
import androidx.graphics.shapes.CornerRounding
import androidx.graphics.shapes.Feature
import androidx.graphics.shapes.FeatureSerializer
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.circle
import androidx.graphics.shapes.pill
import androidx.graphics.shapes.pillStar
import androidx.graphics.shapes.rectangle
import androidx.graphics.shapes.star
import kotlin.math.min
import kotlin.math.roundToInt

data class ShapeItem(
    val name: String,
    var shapegen: () -> RoundedPolygon,
    val shapeOutput: () -> String,
    val usesSides: Boolean = true,
    val usesWidthAndHeight: Boolean = false,
    val usesPillStarFactor: Boolean = false,
    val usesInnerRatio: Boolean = true,
    val usesRoundness: Boolean = true,
    val usesInnerParameters: Boolean = true,
)

open class ShapeParameters(
    val name: String,
    sides: Int = 5,
    innerRadius: Float = 0.5f,
    roundness: Float = 0f,
    smooth: Float = 0f,
    innerRoundness: Float = roundness,
    innerSmooth: Float = smooth,
    rotation: Float = 0f,
    width: Float = 1f,
    height: Float = 1f,
    pillStarFactor: Float = .5f,
    shapeId: ShapeId = ShapeId.Polygon,
    splitProgress: Float = 1.0f,
    customFeaturesOverlay: List<FeatureType> = listOf(),
) {
    internal val sides = mutableFloatStateOf(sides.toFloat())
    internal val innerRadius = mutableFloatStateOf(innerRadius)
    internal val roundness = mutableFloatStateOf(roundness)
    internal val smooth = mutableFloatStateOf(smooth)
    internal val innerRoundness = mutableFloatStateOf(innerRoundness)
    internal val innerSmooth = mutableFloatStateOf(innerSmooth)
    internal val rotation = mutableFloatStateOf(rotation)
    internal val width = mutableFloatStateOf(width)
    internal val height = mutableFloatStateOf(height)
    internal val pillStarFactor = mutableFloatStateOf(pillStarFactor)
    internal val splitProgress = mutableFloatStateOf(splitProgress)
    internal val customFeaturesOverlay = mutableStateOf(customFeaturesOverlay)

    internal var shapeIx by mutableIntStateOf(shapeId.ordinal)

    open val isCustom: Boolean = false

    // Primitive shapes we can draw (so far)
    internal val shapes =
        listOf(
            ShapeItem(
                "Pill",
                shapegen = {
                    RoundedPolygon.pill(
                        width = this.width.floatValue,
                        height = this.height.floatValue,
                        smoothing = this.smooth.floatValue,
                    )
                },
                shapeOutput = {
                    shapeDescription(
                        id = "Pill",
                        width = this.width.floatValue,
                        height = this.height.floatValue,
                        code =
                            "RoundedPolygon.pill(width = $this.width.floatValue, " +
                                "height = $this.height.floatValue)",
                    )
                },
                usesSides = false,
                usesInnerParameters = false,
                usesInnerRatio = false,
                usesRoundness = true,
                usesWidthAndHeight = true,
            ),
            ShapeItem(
                "PillStar",
                shapegen = {
                    RoundedPolygon.pillStar(
                        width = this.width.floatValue,
                        height = this.height.floatValue,
                        numVerticesPerRadius = this.sides.floatValue.roundToInt(),
                        innerRadiusRatio = this.innerRadius.floatValue,
                        rounding =
                            CornerRounding(this.roundness.floatValue, this.smooth.floatValue),
                        innerRounding =
                            CornerRounding(
                                this.innerRoundness.floatValue,
                                this.innerSmooth.floatValue,
                            ),
                        vertexSpacing = this.pillStarFactor.floatValue,
                    )
                },
                shapeOutput = {
                    shapeDescription(
                        id = "PillStar",
                        width = this.width.floatValue,
                        height = this.height.floatValue,
                        numVerts = this.sides.floatValue.roundToInt(),
                        innerRadius = this.innerRadius.floatValue,
                        roundness = this.roundness.floatValue,
                        smooth = this.smooth.floatValue,
                        innerRoundness = this.innerRoundness.floatValue,
                        innerSmooth = this.innerSmooth.floatValue,
                        rotation = this.rotation.floatValue,
                        code =
                            "RoundedPolygon.pillStar(width = $width, height = $height," +
                                "numVerticesPerRadius = $sides, " +
                                "innerRadius = ${innerRadius}f, " +
                                "rounding = CornerRounding(${roundness}f, ${smooth}f), " +
                                "innerRounding = CornerRounding(${innerRoundness}f, ${innerSmooth}f))",
                    )
                },
                usesWidthAndHeight = true,
                usesPillStarFactor = true,
            ),
            ShapeItem(
                "Star",
                shapegen = {
                    RoundedPolygon.star(
                        radius = 1f,
                        numVerticesPerRadius = this.sides.floatValue.roundToInt(),
                        innerRadius = this.innerRadius.floatValue,
                        rounding =
                            CornerRounding(this.roundness.floatValue, this.smooth.floatValue),
                        innerRounding =
                            CornerRounding(
                                this.innerRoundness.floatValue,
                                this.innerSmooth.floatValue,
                            ),
                    )
                },
                shapeOutput = {
                    shapeDescription(
                        id = "Star",
                        sides = this.sides.floatValue.roundToInt(),
                        innerRadius = this.innerRadius.floatValue,
                        roundness = this.roundness.floatValue,
                        smooth = this.smooth.floatValue,
                        innerRoundness = this.innerRoundness.floatValue,
                        innerSmooth = this.innerSmooth.floatValue,
                        rotation = this.rotation.floatValue,
                        code =
                            "RoundedPolygon.star(" +
                                "radius = 2f, " +
                                "numVerticesPerRadius = ${this.sides.floatValue.roundToInt()}, " +
                                "innerRadius = ${this.innerRadius.floatValue}f, " +
                                "rounding = " +
                                "CornerRounding(${this.roundness.floatValue}f," +
                                "${this.smooth.floatValue}f), " +
                                "innerRounding = CornerRounding(${this.innerRoundness.floatValue}f, " +
                                "${this.innerSmooth.floatValue}f))",
                    )
                },
            ),
            ShapeItem(
                "Polygon",
                shapegen = {
                    RoundedPolygon(
                        numVertices = this.sides.floatValue.roundToInt(),
                        rounding = CornerRounding(this.roundness.floatValue, this.smooth.floatValue),
                    )
                },
                shapeOutput = {
                    shapeDescription(
                        id = "Polygon",
                        sides = this.sides.floatValue.roundToInt(),
                        roundness = this.roundness.floatValue,
                        smooth = this.smooth.floatValue,
                        rotation = this.rotation.floatValue,
                        code =
                            "RoundedPolygon(numVertices = ${this.sides.floatValue.roundToInt()}," +
                                "rounding = CornerRounding(${this.roundness.floatValue}f, " +
                                "${this.smooth.floatValue}f))",
                    )
                },
                usesInnerRatio = false,
                usesInnerParameters = false,
            ),
            ShapeItem(
                "Triangle",
                shapegen = {
                    val points =
                        floatArrayOf(
                            radialToCartesian(1f, 270f.toRadians()).x,
                            radialToCartesian(1f, 270f.toRadians()).y,
                            radialToCartesian(1f, 30f.toRadians()).x,
                            radialToCartesian(1f, 30f.toRadians()).y,
                            radialToCartesian(this.innerRadius.floatValue, 90f.toRadians()).x,
                            radialToCartesian(this.innerRadius.floatValue, 90f.toRadians()).y,
                            radialToCartesian(1f, 150f.toRadians()).x,
                            radialToCartesian(1f, 150f.toRadians()).y,
                        )
                    RoundedPolygon(
                        points,
                        CornerRounding(this.roundness.floatValue, this.smooth.floatValue),
                        centerX = 0f,
                        centerY = 0f,
                    )
                },
                shapeOutput = {
                    shapeDescription(
                        id = "Triangle",
                        innerRadius = this.innerRadius.floatValue,
                        smooth = this.smooth.floatValue,
                        rotation = this.rotation.floatValue,
                        code =
                            "val points = floatArrayOf(" +
                                "    radialToCartesian(1f, 270f.toRadians()).x,\n" +
                                "    radialToCartesian(1f, 270f.toRadians()).y,\n" +
                                "    radialToCartesian(1f, 30f.toRadians()).x,\n" +
                                "    radialToCartesian(1f, 30f.toRadians()).y,\n" +
                                "    radialToCartesian(${this.innerRadius.floatValue}f, " +
                                "90f.toRadians()).x,\n" +
                                "    radialToCartesian(${this.innerRadius.floatValue}f, " +
                                "90f.toRadians()).y,\n" +
                                "    radialToCartesian(1f, 150f.toRadians()).x,\n" +
                                "    radialToCartesian(1f, 150f.toRadians()).y)\n" +
                                "RoundedPolygon(points, CornerRounding(" +
                                "${this.roundness.floatValue}f, ${this.smooth.floatValue}f), " +
                                "centerX = 0f, centerY = 0f)",
                    )
                },
                usesSides = false,
                usesInnerParameters = false,
            ),
            ShapeItem(
                "Blob",
                shapegen = {
                    val sx = this.innerRadius.floatValue.coerceAtLeast(0.1f)
                    val sy = this.roundness.floatValue.coerceAtLeast(0.1f)
                    RoundedPolygon(
                        vertices = floatArrayOf(-sx, -sy, sx, -sy, sx, sy, -sx, sy),
                        rounding = CornerRounding(min(sx, sy), this.smooth.floatValue),
                        centerX = 0f,
                        centerY = 0f,
                    )
                },
                shapeOutput = {
                    shapeDescription(
                        id = "Blob",
                        innerRadius = this.innerRadius.floatValue,
                        roundness = this.roundness.floatValue,
                        smooth = this.smooth.floatValue,
                        rotation = this.rotation.floatValue,
                        code =
                            "val sx = ${this.innerRadius.floatValue}f.coerceAtLeast(0.1f)\n" +
                                "val sy = ${this.roundness.floatValue}f.coerceAtLeast(.1f)\n" +
                                "val verts = floatArrayOf(-sx, -sy, sx, -sy, sx, sy, -sx, sy)\n" +
                                "RoundedPolygon(verts, rounding = CornerRounding(min(sx, sy), " +
                                "${this.smooth.floatValue}f)," +
                                "centerX = 0f, centerY = 0f)",
                    )
                },
                usesSides = false,
                usesInnerParameters = false,
            ),
            ShapeItem(
                "CornerSE",
                shapegen = {
                    RoundedPolygon(
                        squarePoints(),
                        perVertexRounding =
                            listOf(
                                CornerRounding(this.roundness.floatValue, this.smooth.floatValue),
                                CornerRounding(1f),
                                CornerRounding(1f),
                                CornerRounding(1f),
                            ),
                        centerX = 0f,
                        centerY = 0f,
                    )
                },
                shapeOutput = {
                    shapeDescription(
                        id = "cornerSE",
                        roundness = this.roundness.floatValue,
                        smooth = this.smooth.floatValue,
                        rotation = this.rotation.floatValue,
                        code =
                            "RoundedPolygon(floatArrayOf(1f, 1f, -1f, 1f, -1f, -1f, 1f, -1f), " +
                                "perVertexRounding = listOf(CornerRounding(" +
                                "${this.roundness.floatValue}f, ${this.smooth.floatValue}f), " +
                                "CornerRounding(1f), CornerRounding(1f),  CornerRounding(1f))," +
                                "centerX = 0f, centerY = 0f)",
                    )
                },
                usesSides = false,
                usesInnerRatio = false,
                usesInnerParameters = false,
            ),
            ShapeItem(
                "Circle",
                shapegen = {
                    RoundedPolygon.circle(this.sides.floatValue.roundToInt())
                        .transformed(
                            Matrix().apply {
                                scale(
                                    x = this@ShapeParameters.width.floatValue,
                                    y = this@ShapeParameters.height.floatValue,
                                )
                                rotateX(rotation)
                            }
                        )
                },
                shapeOutput = {
                    shapeDescription(
                        id = "Circle",
                        roundness = this.roundness.floatValue,
                        smooth = this.smooth.floatValue,
                        rotation = this.rotation.floatValue,
                        code = "RoundedPolygon.circle($sides)",
                    )
                },
                usesSides = true,
                usesInnerRatio = false,
                usesWidthAndHeight = true,
                usesInnerParameters = false,
            ),
            ShapeItem(
                "Rectangle",
                shapegen = {
                    RoundedPolygon.rectangle(
                        width = 4f,
                        height = 2f,
                        rounding = CornerRounding(this.roundness.floatValue, this.smooth.floatValue),
                    )
                },
                shapeOutput = {
                    shapeDescription(
                        id = "Rectangle",
                        numVerts = 4,
                        roundness = this.roundness.floatValue,
                        smooth = this.smooth.floatValue,
                        rotation = this.rotation.floatValue,
                        code =
                            "RoundedPolygon.rectangle(width = 4f, height = 2f, " +
                                "rounding = CornerRounding(" +
                                "${this.roundness.floatValue}f, ${this.smooth.floatValue}f))",
                    )
                },
                usesSides = false,
                usesInnerRatio = false,
                usesInnerParameters = false,
            ),

            /*
            TODO: Add quarty. Needs to be able to specify a rounding radius of up to 2f
            ShapeItem("Quarty", { DefaultShapes.quarty(roundness.value, smooth.value) },
            usesSides = false, usesInnerRatio = false),
            */
        )

    open fun copy() =
        ShapeParameters(
            this.name,
            this.sides.floatValue.roundToInt(),
            this.innerRadius.floatValue,
            this.roundness.floatValue,
            this.smooth.floatValue,
            this.innerRoundness.floatValue,
            this.innerSmooth.floatValue,
            this.rotation.floatValue,
            this.width.floatValue,
            this.height.floatValue,
            this.pillStarFactor.floatValue,
            ShapeId.values()[this.shapeIx],
            this.splitProgress.floatValue,
            this.customFeaturesOverlay.value.map { it },
        )

    enum class ShapeId {
        Pill,
        PillStar,
        Star,
        Polygon,
        Triangle,
        Blob,
        CornerSE,
        Circle,
        Rectangle,
    }

    fun serialized(): String =
        """
            val features: List<Feature> = FeatureSerializer.parse("${FeatureSerializer.serialize(genShape().features)}")
            val shape: RoundedPolygon = RoundedPolygon(features, centerX = <recommended to add>, centerY = <recommended to add>)
        """
            .trimIndent()

    private fun shapeDescription(
        id: String? = null,
        numVerts: Int? = null,
        sides: Int? = null,
        innerRadius: Float? = null,
        roundness: Float? = null,
        innerRoundness: Float? = null,
        smooth: Float? = null,
        innerSmooth: Float? = null,
        rotation: Float? = null,
        width: Float? = null,
        height: Float? = null,
        pillStarFactor: Float? = null,
        splitProgress: Float? = null,
        customFeaturesOverlay: List<FeatureType>? = null,
        code: String? = null,
    ): String {
        var description = "ShapeParameters:\n"
        if (id != null) description += "shapeId = $id, "
        if (numVerts != null) description += "numVertices = $numVerts, "
        if (sides != null) description += "sides = $sides, "
        if (innerRadius != null) description += "innerRadius = $innerRadius, "
        if (roundness != null) description += "roundness = $roundness, "
        if (innerRoundness != null) description += "innerRoundness = $innerRoundness, "
        if (smooth != null) description += "smoothness = $smooth, "
        if (innerSmooth != null) description += "innerSmooth = $innerSmooth, "
        if (rotation != null) description += "rotation = $rotation, "
        if (width != null) description += "width = $width, "
        if (height != null) description += "height = $height, "
        if (pillStarFactor != null) description += "pillStarFactor = $pillStarFactor, "
        if (splitProgress != null) description += "splitProgress = $splitProgress, "
        if (customFeaturesOverlay != null) {
            description +=
                "customFeaturesOverlay = ${customFeaturesOverlay.joinToString(separator = " ") { it.name }}, "
        }
        if (code != null) {
            description += "\nCode:\n$code"
        }
        return description
    }

    open fun selectedShape() = derivedStateOf { shapes[shapeIx] }

    fun genShape(autoSize: Boolean = true): RoundedPolygon {
        // TODO: b/378433883 - Improve combined overlay - split interactions

        val original = selectedShape().value.shapegen()
        val split = original.split(splitProgress.floatValue)

        if (split.features.size != customFeaturesOverlay.value.size) {
            customFeaturesOverlay.value = split.features.map { it.toFeatureType() }
        }

        val customizedSplitPolygon =
            RoundedPolygon(
                split.features.mapIndexed { index, feature ->
                    customFeaturesOverlay.value[index].apply(feature)
                }
            )

        return customizedSplitPolygon.transformed(
            Matrix().apply {
                if (autoSize) {
                    val bounds = customizedSplitPolygon.getBounds()
                    // Move the center to the origin.
                    translate(
                        x = -(bounds.left + bounds.right) / 2,
                        y = -(bounds.top + bounds.bottom) / 2,
                    )

                    // Scale to the [-1, 1] range
                    val scale = 2f / bounds.maxDimension
                    scale(x = scale, y = scale)
                }
                // Apply the needed rotation
                rotateZ(rotation.floatValue)
            }
        )
    }

    internal fun equals(other: ShapeParameters) =
        this.shapeDescription(
            sides = sides.floatValue.toInt(),
            innerRadius = innerRadius.floatValue,
            roundness = roundness.floatValue,
            smooth = smooth.floatValue,
            innerRoundness = innerRoundness.floatValue,
            innerSmooth = innerSmooth.floatValue,
            rotation = rotation.floatValue,
            width = width.floatValue,
            height = height.floatValue,
            pillStarFactor = pillStarFactor.floatValue,
            splitProgress = splitProgress.floatValue,
            customFeaturesOverlay = customFeaturesOverlay.value,
        ) ==
            other.shapeDescription(
                sides = other.sides.floatValue.toInt(),
                innerRadius = other.innerRadius.floatValue,
                roundness = other.roundness.floatValue,
                smooth = other.smooth.floatValue,
                innerRoundness = other.innerRoundness.floatValue,
                innerSmooth = other.innerSmooth.floatValue,
                rotation = other.rotation.floatValue,
                width = other.width.floatValue,
                height = other.height.floatValue,
                pillStarFactor = other.pillStarFactor.floatValue,
                splitProgress = other.splitProgress.floatValue,
                customFeaturesOverlay = other.customFeaturesOverlay.value,
            )

    private fun radialToCartesian(
        radius: Float,
        angleRadians: Float,
        center: Offset = Offset.Zero,
    ) = directionVector(angleRadians) * radius + center
}

class CustomShapeParameters(
    name: String,
    sides: Int = 5,
    innerRadius: Float = 0.5f,
    roundness: Float = 0f,
    smooth: Float = 0f,
    innerRoundness: Float = roundness,
    innerSmooth: Float = smooth,
    rotation: Float = 0f,
    width: Float = 1f,
    height: Float = 1f,
    pillStarFactor: Float = .5f,
    shapeId: ShapeId = ShapeId.Polygon,
    splitProgress: Float = 1.0f,
    customFeaturesOverlay: List<FeatureType> = listOf(),
    private val shapegen: () -> RoundedPolygon,
) :
    ShapeParameters(
        name,
        sides,
        innerRadius,
        roundness,
        smooth,
        innerRoundness,
        innerSmooth,
        rotation,
        width,
        height,
        pillStarFactor,
        shapeId,
        splitProgress,
        customFeaturesOverlay,
    ) {

    override val isCustom: Boolean = true

    override fun selectedShape() = derivedStateOf {
        ShapeItem(name = name, shapegen = shapegen, shapeOutput = { "Custom Shape: $name" })
    }

    override fun copy(): CustomShapeParameters =
        CustomShapeParameters(
            this.name,
            this.sides.floatValue.roundToInt(),
            this.innerRadius.floatValue,
            this.roundness.floatValue,
            this.smooth.floatValue,
            this.innerRoundness.floatValue,
            this.innerSmooth.floatValue,
            this.rotation.floatValue,
            this.width.floatValue,
            this.height.floatValue,
            this.pillStarFactor.floatValue,
            ShapeId.values()[this.shapeIx],
            this.splitProgress.floatValue,
            this.customFeaturesOverlay.value.map { it },
            this.shapegen,
        )
}

private fun squarePoints() = floatArrayOf(1f, 1f, -1f, 1f, -1f, -1f, 1f, -1f)

enum class FeatureType {
    IGNORABLE {
        override fun apply(feature: Feature) = Feature.buildIgnorableFeature(feature.cubics)
    },
    CONVEX_CORNER {
        override fun apply(feature: Feature) = Feature.buildConvexCorner(feature.cubics)
    },
    CONCAVE_CORNER {
        override fun apply(feature: Feature) = Feature.buildConcaveCorner(feature.cubics)
    };

    abstract fun apply(feature: Feature): Feature
}

internal fun Feature.toFeatureType(): FeatureType =
    if (isEdge) {
        FeatureType.IGNORABLE
    } else if (isConvexCorner) {
        FeatureType.CONVEX_CORNER
    } else if (isConcaveCorner) {
        FeatureType.CONCAVE_CORNER
    } else {
        FeatureType.IGNORABLE
    }

internal fun toggleFeatureType(typeToToggle: FeatureType): FeatureType {
    val currentFeatureIndex = FeatureType.values().indexOf(typeToToggle)
    val nextFeatureType =
        FeatureType.values()[(currentFeatureIndex + 1) % FeatureType.values().size]
    return nextFeatureType
}
