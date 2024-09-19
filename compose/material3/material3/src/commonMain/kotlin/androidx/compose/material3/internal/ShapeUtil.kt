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

package androidx.compose.material3.internal

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.util.fastForEach
import androidx.graphics.shapes.Cubic
import androidx.graphics.shapes.Morph
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.TransformResult
import kotlin.math.PI
import kotlin.math.atan2

/** Transforms a [RoundedPolygon] with the given [Matrix] */
internal fun RoundedPolygon.transformed(matrix: Matrix): RoundedPolygon = transformed { x, y ->
    val transformedPoint = matrix.map(Offset(x, y))
    TransformResult(transformedPoint.x, transformedPoint.y)
}

/**
 * Gets a [Path] representation for a [RoundedPolygon] shape. Note that there is some rounding
 * happening (to the nearest thousandth), to work around rendering artifacts introduced by some
 * points being just slightly off from each other (far less than a pixel). This also allows for a
 * more optimal path, as redundant curves (usually a single point) can be detected and not added to
 * the resulting path.
 *
 * @param path a [Path] object which, if supplied, will avoid the function having to create a new
 *   [Path] object
 * @param startAngle an angle (in degrees) to rotate the [Path] to start drawing from. The rotation
 *   pivot is set to be the polygon's centerX and centerY coordinates.
 * @param repeatPath whether or not to repeat the [Path] twice before closing it. This flag is
 *   useful when the caller would like to draw parts of the path while offsetting the start and stop
 *   positions (for example, when phasing and rotating a path to simulate a motion as a Star
 *   circular progress indicator advances).
 * @param closePath whether or not to close the created [Path]
 */
internal fun RoundedPolygon.toPath(
    path: Path = Path(),
    startAngle: Int = 270,
    repeatPath: Boolean = false,
    closePath: Boolean = true,
): Path {
    pathFromCubics(
        path = path,
        startAngle = startAngle,
        repeatPath = repeatPath,
        closePath = closePath,
        cubics = cubics,
        rotationPivotX = centerX,
        rotationPivotY = centerY
    )
    return path
}

/**
 * Returns a [Path] for a [Morph].
 *
 * @param progress the [Morph]'s progress
 * @param path a [Path] to rewind and set with the new path data
 * @param startAngle an angle (in degrees) to rotate the [Path] to start drawing from
 * @param repeatPath whether or not to repeat the [Path] twice before closing it. This flag is
 *   useful when the caller would like to draw parts of the path while offsetting the start and stop
 *   positions (for example, when phasing and rotating a path to simulate a motion as a Star
 *   circular progress indicator advances).
 * @param closePath whether or not to close the created [Path]
 * @param rotationPivotX the rotation pivot on the X axis. By default it's set to 0, and that should
 *   align with [Morph] instances that were created for RoundedPolygons with zero centerX. In case
 *   the RoundedPolygon were normalized (i.e. moved to (0.5, 0.5)), or where created with a
 *   different centerX coordinated, this pivot point may need to be aligned to support a proper
 *   rotation.
 * @param rotationPivotY the rotation pivot on the Y axis. By default it's set to 0, and that should
 *   align with [Morph] instances that were created for RoundedPolygons with zero centerY. In case
 *   the RoundedPolygon were normalized (i.e. moves to (0.5, 0.5)), or where created with a
 *   different centerY coordinated, this pivot point may need to be aligned to support a proper
 *   rotation.
 */
internal fun Morph.toPath(
    progress: Float,
    path: Path = Path(),
    startAngle: Int = 270, // 12 O'clock
    repeatPath: Boolean = false,
    closePath: Boolean = true,
    rotationPivotX: Float = 0f,
    rotationPivotY: Float = 0f
): Path {
    pathFromCubics(
        path = path,
        startAngle = startAngle,
        repeatPath = repeatPath,
        closePath = closePath,
        cubics = asCubics(progress),
        rotationPivotX = rotationPivotX,
        rotationPivotY = rotationPivotY
    )
    return path
}

private fun pathFromCubics(
    path: Path,
    startAngle: Int,
    repeatPath: Boolean,
    closePath: Boolean,
    cubics: List<Cubic>,
    rotationPivotX: Float,
    rotationPivotY: Float
) {
    var first = true
    var firstCubic: Cubic? = null
    path.rewind()
    cubics.fastForEach {
        if (first) {
            path.moveTo(it.anchor0X, it.anchor0Y)
            if (startAngle != 0) {
                firstCubic = it
            }
            first = false
        }
        path.cubicTo(
            it.control0X,
            it.control0Y,
            it.control1X,
            it.control1Y,
            it.anchor1X,
            it.anchor1Y
        )
    }
    if (repeatPath) {
        var firstInRepeat = true
        cubics.fastForEach {
            if (firstInRepeat) {
                path.lineTo(it.anchor0X, it.anchor0Y)
                firstInRepeat = false
            }
            path.cubicTo(
                it.control0X,
                it.control0Y,
                it.control1X,
                it.control1Y,
                it.anchor1X,
                it.anchor1Y
            )
        }
    }

    if (closePath) path.close()

    if (startAngle != 0 && firstCubic != null) {
        val angleToFirstCubic =
            radiansToDegrees(
                atan2(
                    y = cubics[0].anchor0Y - rotationPivotY,
                    x = cubics[0].anchor0X - rotationPivotX
                )
            )
        // Rotate the Path to to start from the given angle.
        path.transform(Matrix().apply { rotateZ(-angleToFirstCubic + startAngle) })
    }
}

private fun radiansToDegrees(radians: Float): Float {
    return (radians * 180.0 / PI).toFloat()
}
