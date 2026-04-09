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

import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import org.jetbrains.skia.Path as SkPath
import org.jetbrains.skia.PathDirection
import org.jetbrains.skia.PathBuilder
import org.jetbrains.skia.PathFillMode
import org.jetbrains.skia.PathOp

actual fun Path(): Path = SkiaBackedPath()

/**
 * Convert the [org.jetbrains.skia.Path] instance into a Compose-compatible Path
 */
// TODO: Multiple calls will NOT return the same instance,
//  consider to replace to `fun Path(skiaPath: org.jetbrains.skia.Path)`
fun SkPath.asComposePath(): Path = SkiaBackedPath(this).also {
    it.isSkiaPathObserved = true
}

/**
 * Obtain a reference to the underlying [org.jetbrains.skia.Path] instance.
 *
 * It throws an exception if accessed on unsupported types.
 */
fun Path.asSkiaPath(): SkPath {
    requirePrecondition(this is SkiaBackedPath) {
        "Extracting skia path reference is only supported from androidx.compose.ui.graphics.SkiaBackedPath instances but received ${this::class}"
    }
    isSkiaPathObserved = true
    synchronizeSkiaPathIfNeeded()
    return internalSkiaPath
}

/**
 * Obtain a reference to the underlying [org.jetbrains.skia.Path] instance without marking
 * that native path as externally observed.
 *
 * This is intended for internal, one-shot interop where the Skia API reads or copies the path
 * immediately. Callers must not retain or mutate the returned [org.jetbrains.skia.Path].
 *
 * It throws an exception if accessed on unsupported types.
 */
@InternalComposeUiApi
fun Path.materializeSkiaPath(): SkPath {
    requirePrecondition(this is SkiaBackedPath) {
        "Materializing skia path snapshot is only supported from androidx.compose.ui.graphics.SkiaBackedPath instances but received ${this::class}"
    }
    synchronizeSkiaPathIfNeeded()
    return internalSkiaPath
}

/**
 * Marks that a path has Compose-side mutations in its [PathBuilder] that are not yet
 * reflected in the stable native [SkPath]. Skia's public path generation IDs are always non-zero,
 * so `0` remains reserved for this local pending-state marker.
 */
private const val PendingGenerationId = 0

@OptIn(InternalComposeUiApi::class)
private class SkiaBackedPath(
    internal val internalSkiaPath: SkPath = SkPath()
) : Path {
    private var pathBuilder = PathBuilder(internalSkiaPath)
    private var materializedGenerationId = internalSkiaPath.generationId
    private var currentFillMode = internalSkiaPath.fillMode

    /**
     * Indicates if [internalSkiaPath] is externally observable.
     */
    internal var isSkiaPathObserved = false

    private inline fun mutatePath(block: PathBuilder.() -> Unit) {
        synchronizeBuilderIfNeeded()
        pathBuilder.apply(block)
        materializedGenerationId = PendingGenerationId
        if (isSkiaPathObserved) {
            synchronizeSkiaPathIfNeeded()
        }
    }

    private fun hasPendingChanges(): Boolean = materializedGenerationId == PendingGenerationId

    private fun synchronizeBuilderIfNeeded() {
        // Skia's generationId does not change when only the fill mode changes. Compare both
        // so a native/external fill type update still rebuilds the cached PathBuilder before
        // the next Compose-side mutation would otherwise overwrite internalSkiaPath with stale
        // builder state.
        if (!hasPendingChanges() &&
            (internalSkiaPath.generationId != materializedGenerationId ||
                internalSkiaPath.fillMode != currentFillMode)
        ) {
            pathBuilder.close()
            pathBuilder = PathBuilder(internalSkiaPath)
            materializedGenerationId = internalSkiaPath.generationId
            currentFillMode = internalSkiaPath.fillMode
        }
    }

    internal fun synchronizeSkiaPathIfNeeded() {
        if (hasPendingChanges()) {
            replacePath(pathBuilder.snapshot())
        } else {
            synchronizeBuilderIfNeeded()
        }
    }

    private fun replacePath(path: SkPath) {
        // Keep the same SkPath instance alive so native callers can continue mutating it.
        internalSkiaPath.swap(path)
        materializedGenerationId = internalSkiaPath.generationId
        currentFillMode = internalSkiaPath.fillMode
        pathBuilder.close()
        pathBuilder = PathBuilder(internalSkiaPath)
    }

    override var fillType: PathFillType
        get() {
            synchronizeBuilderIfNeeded()
            return currentFillMode.toComposePathFillType()
        }

        set(value) {
            val fillMode = value.toSkiaPathFillMode()
            mutatePath {
                setFillType(fillMode)
            }
            currentFillMode = fillMode
        }

    override fun moveTo(x: Float, y: Float) = mutatePath {
        moveTo(x, y)
    }

    override fun relativeMoveTo(dx: Float, dy: Float) = mutatePath {
        rMoveTo(dx, dy)
    }

    override fun lineTo(x: Float, y: Float) = mutatePath {
        lineTo(x, y)
    }

    override fun relativeLineTo(dx: Float, dy: Float) = mutatePath {
        rLineTo(dx, dy)
    }

    @Deprecated(
        "Use quadraticTo() for consistency with cubicTo()",
        replaceWith = ReplaceWith("quadraticTo(x1, y1, x2, y2)"),
        level = DeprecationLevel.WARNING,
    )
    override fun quadraticBezierTo(x1: Float, y1: Float, x2: Float, y2: Float) =
        mutatePath { quadTo(x1, y1, x2, y2) }

    override fun quadraticTo(x1: Float, y1: Float, x2: Float, y2: Float) =
        mutatePath { quadTo(x1, y1, x2, y2) }

    @Deprecated(
        "Use relativeQuadraticTo() for consistency with relativeCubicTo()",
        replaceWith = ReplaceWith("relativeQuadraticTo(dx1, dy1, dx2, dy2)"),
        level = DeprecationLevel.WARNING,
    )
    override fun relativeQuadraticBezierTo(dx1: Float, dy1: Float, dx2: Float, dy2: Float) =
        mutatePath { rQuadTo(dx1, dy1, dx2, dy2) }

    override fun relativeQuadraticTo(dx1: Float, dy1: Float, dx2: Float, dy2: Float) {
        mutatePath { rQuadTo(dx1, dy1, dx2, dy2) }
    }

    override fun cubicTo(x1: Float, y1: Float, x2: Float, y2: Float, x3: Float, y3: Float) =
        mutatePath {
            cubicTo(
                x1, y1,
                x2, y2,
                x3, y3
            )
        }

    override fun relativeCubicTo(
        dx1: Float,
        dy1: Float,
        dx2: Float,
        dy2: Float,
        dx3: Float,
        dy3: Float
    ) = mutatePath {
        rCubicTo(
            dx1, dy1,
            dx2, dy2,
            dx3, dy3
        )
    }

    override fun arcTo(
        rect: Rect,
        startAngleDegrees: Float,
        sweepAngleDegrees: Float,
        forceMoveTo: Boolean
    ) = mutatePath {
        arcTo(
            rect.left,
            rect.top,
            rect.right,
            rect.bottom,
            startAngleDegrees,
            sweepAngleDegrees,
            forceMoveTo
        )
    }

    @Deprecated(
        "Prefer usage of addRect() with a winding direction",
        replaceWith = ReplaceWith("addRect(rect)"),
        level = DeprecationLevel.HIDDEN,
    )
    override fun addRect(rect: Rect) = mutatePath {
        addRect(
            rect.left,
            rect.top,
            rect.right,
            rect.bottom,
            PathDirection.COUNTER_CLOCKWISE
        )
    }

    override fun addRect(rect: Rect, direction: Path.Direction) = mutatePath {
        addRect(
            rect.left,
            rect.top,
            rect.right,
            rect.bottom,
            direction.toSkiaPathDirection()
        )
    }

    @Deprecated(
        "Prefer usage of addOval() with a winding direction",
        replaceWith = ReplaceWith("addOval(oval)"),
        level = DeprecationLevel.HIDDEN
    )
    override fun addOval(oval: Rect) = mutatePath {
        addOval(
            oval.left,
            oval.top,
            oval.right,
            oval.bottom,
            PathDirection.COUNTER_CLOCKWISE
        )
    }

    override fun addOval(oval: Rect, direction: Path.Direction) = mutatePath {
        addOval(
            oval.left,
            oval.top,
            oval.right,
            oval.bottom,
            direction.toSkiaPathDirection()
        )
    }

    @Deprecated(
        "Prefer usage of addRoundRect() with a winding direction",
        replaceWith = ReplaceWith("addRoundRect(roundRect)"),
        level = DeprecationLevel.HIDDEN
    )
    override fun addRoundRect(roundRect: RoundRect) = mutatePath {
        addRRect(
            roundRect.left,
            roundRect.top,
            roundRect.right,
            roundRect.bottom,
            floatArrayOf(
                roundRect.topLeftCornerRadius.x,
                roundRect.topLeftCornerRadius.y,
                roundRect.topRightCornerRadius.x,
                roundRect.topRightCornerRadius.y,
                roundRect.bottomRightCornerRadius.x,
                roundRect.bottomRightCornerRadius.y,
                roundRect.bottomLeftCornerRadius.x,
                roundRect.bottomLeftCornerRadius.y
            ),
            PathDirection.COUNTER_CLOCKWISE
        )
    }

    override fun addRoundRect(roundRect: RoundRect, direction: Path.Direction) = mutatePath {
        addRRect(
            roundRect.left,
            roundRect.top,
            roundRect.right,
            roundRect.bottom,
            floatArrayOf(
                roundRect.topLeftCornerRadius.x,
                roundRect.topLeftCornerRadius.y,
                roundRect.topRightCornerRadius.x,
                roundRect.topRightCornerRadius.y,
                roundRect.bottomRightCornerRadius.x,
                roundRect.bottomRightCornerRadius.y,
                roundRect.bottomLeftCornerRadius.x,
                roundRect.bottomLeftCornerRadius.y
            ),
            direction.toSkiaPathDirection()
        )
    }

    override fun addArcRad(oval: Rect, startAngleRadians: Float, sweepAngleRadians: Float) {
        addArc(oval, degrees(startAngleRadians), degrees(sweepAngleRadians))
    }

    override fun addArc(oval: Rect, startAngleDegrees: Float, sweepAngleDegrees: Float) = mutatePath {
        addArc(
            oval.left,
            oval.top,
            oval.right,
            oval.bottom,
            startAngleDegrees,
            sweepAngleDegrees
        )
    }

    override fun addPath(path: Path, offset: Offset) =
        mutatePath { addPath(path.materializeSkiaPath(), offset.x, offset.y) }

    override fun close() = mutatePath {
        closePath()
    }

    override fun reset() {
        val fillMode = currentFillMode
        mutatePath {
            reset()
            setFillType(fillMode)
        }
    }

    override fun translate(offset: Offset) = mutatePath {
        offset(offset.x, offset.y)
    }

    override fun transform(matrix: Matrix) = mutatePath {
        transform(identityMatrix33().apply { setFrom(matrix) })
    }

    override fun getBounds(): Rect {
        synchronizeSkiaPathIfNeeded()
        val bounds = internalSkiaPath.bounds
        return Rect(
            bounds.left,
            bounds.top,
            bounds.right,
            bounds.bottom
        )
    }

    override fun op(
        path1: Path,
        path2: Path,
        operation: PathOperation
    ): Boolean = SkPath.makeCombining(
        path1.materializeSkiaPath(),
        path2.materializeSkiaPath(),
        operation.toSkiaOperation()
    )?.also {
        replacePath(it)
    } != null

    override val isConvex: Boolean
        get() {
            synchronizeSkiaPathIfNeeded()
            return internalSkiaPath.isConvex
        }

    override val isEmpty: Boolean
        get() {
            synchronizeSkiaPathIfNeeded()
            return internalSkiaPath.isEmpty
        }
}

private fun PathOperation.toSkiaOperation() = when (this) {
    PathOperation.Difference -> PathOp.DIFFERENCE
    PathOperation.Intersect -> PathOp.INTERSECT
    PathOperation.Union -> PathOp.UNION
    PathOperation.Xor -> PathOp.XOR
    PathOperation.ReverseDifference -> PathOp.REVERSE_DIFFERENCE
    else -> PathOp.XOR
}

private fun PathFillType.toSkiaPathFillMode(): PathFillMode = when (this) {
    PathFillType.EvenOdd -> PathFillMode.EVEN_ODD
    PathFillType.NonZero -> PathFillMode.WINDING
    else -> error("Unsupported PathFillType: $this")
}

private fun PathFillMode.toComposePathFillType(): PathFillType = when (this) {
    PathFillMode.WINDING -> PathFillType.NonZero
    PathFillMode.EVEN_ODD -> PathFillType.EvenOdd
    PathFillMode.INVERSE_WINDING -> PathFillType.NonZero
    PathFillMode.INVERSE_EVEN_ODD -> PathFillType.EvenOdd
}

private fun Path.Direction.toSkiaPathDirection() = when (this) {
    Path.Direction.CounterClockwise -> PathDirection.COUNTER_CLOCKWISE
    Path.Direction.Clockwise -> PathDirection.CLOCKWISE
}
