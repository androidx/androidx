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

@file:Suppress("NOTHING_TO_INLINE")

package androidx.graphics.shapes.testcompose

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Path
import androidx.graphics.shapes.Cubic
import androidx.graphics.shapes.MutableCubic
import androidx.graphics.shapes.RoundedPolygon

/**
 * Utility functions providing more idiomatic ways of transforming RoundedPolygons and
 * transforming shapes into a compose Path, for drawing them.
 *
 * This should in the future move into the compose library, maybe with additional API that makes
 * it easier to create, draw, and animate from Compose apps.
 *
 * This code is just here for now prior to integration into compose
 */

/**
 * Scales a shape (given as a Sequence) in place.
 * As this works in Sequences, it doesn't create the whole list at any point, only one
 * MutableCubic is (re)used.
 */
fun Sequence<MutableCubic>.scaled(scale: Float) = map {
    it.transform {
        x *= scale
        y *= scale
    }
    it
}

/**
 * Scales a shape (given as a List), creating a new List.
 */
fun List<Cubic>.scaled(scale: Float) = map {
    it.transformed {
        x *= scale
        y *= scale
    }
}

/**
 * Transforms a [RoundedPolygon] with the given [Matrix]
 */
fun RoundedPolygon.transformed(matrix: Matrix): RoundedPolygon =
    transformed {
        val transformedPoint = matrix.map(Offset(x, y))
        x = transformedPoint.x
        y = transformedPoint.y
    }

/**
 * Calculates and returns the bounds of this [RoundedPolygon] as a [Rect]
 */
fun RoundedPolygon.getBounds() = calculateBounds().let { Rect(it[0], it[1], it[2], it[3]) }

/**
 * Function used to create a Path from some Cubics.
 * Note that this takes an Iterator, so it could be used on Lists, Sequences, etc.
 */
fun Iterator<Cubic>.toPath(path: Path = Path()): Path {
    path.reset()
    var first = true
    while (hasNext()) {
        var bezier = next()
        if (first) {
            path.moveTo(bezier.anchor0X, bezier.anchor0Y)
            first = false
        }
        path.cubicTo(
            bezier.control0X, bezier.control0Y,
            bezier.control1X, bezier.control1Y,
            bezier.anchor1X, bezier.anchor1Y
        )
    }
    path.close()
    return path
}

/**
 * Transforms the Sequence into a [Path].
 */
fun Sequence<Cubic>.toPath(path: Path = Path()) = iterator().toPath(path)

internal const val DEBUG = false

internal inline fun debugLog(message: String) {
    if (DEBUG) {
        println(message)
    }
}
