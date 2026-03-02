/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.compose.ui.graphics

import androidx.compose.ui.geometry.Offset
import org.jetbrains.skia.PathMeasure as SkPathMeasure

/**
 * Convert the [org.jetbrains.skia.PathMeasure] instance into a Compose-compatible PathMeasure
 */
fun SkPathMeasure.asComposePathEffect(): PathMeasure = SkiaBackedPathMeasure(this)

/**
 * Obtain a reference the underlying [org.jetbrains.skia.PathMeasure] instance.
 *
 * It throws an exception if accessed on unsupported types.
 */
fun PathMeasure.asSkiaPathMeasure(): SkPathMeasure {
    requirePrecondition(this is SkiaBackedPathMeasure) {
        "Extracting skia path measure reference is only supported from androidx.compose.ui.graphics.SkiaBackedPathMeasure instances but received ${this::class}"
    }
    return internalSkiaPathMeasure
}

internal class SkiaBackedPathMeasure(
    internal val internalSkiaPathMeasure: SkPathMeasure = SkPathMeasure()
) : PathMeasure {

    override fun setPath(path: Path?, forceClosed: Boolean) {
        internalSkiaPathMeasure.setPath(path?.asSkiaPath(), forceClosed)
    }

    override fun getSegment(
        startDistance: Float,
        stopDistance: Float,
        destination: Path,
        startWithMoveTo: Boolean
    ) = internalSkiaPathMeasure.getSegment(
        startDistance,
        stopDistance,
        destination.asSkiaPath(),
        startWithMoveTo
    )

    override val length: Float
        get() = internalSkiaPathMeasure.length

    override fun getPosition(
        distance: Float
    ): Offset {
        val result = internalSkiaPathMeasure.getPosition(distance)
        return if (result != null) {
            Offset(result.x, result.y)
        } else {
            Offset.Unspecified
        }
    }

    override fun getTangent(
        distance: Float
    ): Offset {
        val result = internalSkiaPathMeasure.getTangent(distance)
        return if (result != null) {
            Offset(result.x, result.y)
        } else {
            Offset.Unspecified
        }
    }
}

actual fun PathMeasure(): PathMeasure =
    SkiaBackedPathMeasure()
