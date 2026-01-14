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

package androidx.compose.remote.creation.compose.layout

import androidx.annotation.RestrictTo
import androidx.compose.remote.core.operations.DrawTextOnCircle
import androidx.compose.remote.creation.RemotePath
import androidx.compose.remote.creation.compose.capture.RecordingCanvas
import androidx.compose.remote.creation.compose.capture.RemoteComposeCreationState
import androidx.compose.remote.creation.compose.state.RemoteBitmap
import androidx.compose.remote.creation.compose.state.RemoteFloat
import androidx.compose.remote.creation.compose.state.RemotePaint
import androidx.compose.remote.creation.compose.state.RemoteString
import androidx.compose.ui.graphics.Matrix

/**
 * A wrapper around [RecordingCanvas] that provides overloads for remote types and avoids platform
 * types in its public API where possible.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class RemoteCanvas(
    /** The underlying [RecordingCanvas] being wrapped. */
    public val internalCanvas: RecordingCanvas
) {
    /** The [RemoteComposeCreationState] associated with the document being drawn into. */
    public val creationState: RemoteComposeCreationState
        get() = internalCanvas.creationState

    /** Saves the current canvas state. */
    public fun save() {
        internalCanvas.save()
    }

    /** Restores the previous canvas state. */
    public fun restore() {
        internalCanvas.restore()
    }

    /**
     * Translates the canvas by [dx] and [dy].
     *
     * @param dx The translation along the X axis.
     * @param dy The translation along the Y axis.
     */
    public fun translate(dx: RemoteFloat, dy: RemoteFloat) {
        internalCanvas.translate(dx, dy)
    }

    /**
     * Scales the canvas by [sx] and [sy].
     *
     * @param sx The scale factor along the X axis.
     * @param sy The scale factor along the Y axis.
     */
    public fun scale(sx: RemoteFloat, sy: RemoteFloat) {
        internalCanvas.scale(sx, sy)
    }

    /**
     * Scales the canvas by [sx] and [sy] around the pivot point ([px], [py]).
     *
     * @param sx The scale factor along the X axis.
     * @param sy The scale factor along the Y axis.
     * @param pivot The pivot point around which to scale.
     */
    public fun scale(sx: RemoteFloat, sy: RemoteFloat, pivot: RemoteOffset) {
        internalCanvas.scale(sx, sy, pivot.x, pivot.y)
    }

    /**
     * Rotates the canvas by [degrees].
     *
     * @param degrees The angle of rotation in degrees.
     */
    public fun rotate(degrees: RemoteFloat) {
        internalCanvas.rotate(degrees)
    }

    /**
     * Applies a transformation [matrix] to the canvas.
     *
     * @param matrix The [Matrix] to concatenate with the current canvas transformation.
     */
    public fun transform(matrix: Matrix) {
        internalCanvas.concat(
            android.graphics.Matrix().apply {
                matrix.values.let { v ->
                    setValues(floatArrayOf(v[0], v[1], v[2], v[3], v[4], v[5], v[6], v[7], v[8]))
                }
            }
        )
    }

    /**
     * Draws a rectangle from ([left], [top]) to ([right], [bottom]) using the specified [paint].
     */
    public fun drawRect(
        left: RemoteFloat,
        top: RemoteFloat,
        right: RemoteFloat,
        bottom: RemoteFloat,
        paint: RemotePaint,
    ) {
        internalCanvas.drawRect(left, top, right, bottom, paint)
    }

    /**
     * Draws a rounded rectangle from ([left], [top]) to ([right], [bottom]) with the specified [rx]
     * , [ry], and [paint].
     */
    public fun drawRoundRect(
        left: RemoteFloat,
        top: RemoteFloat,
        right: RemoteFloat,
        bottom: RemoteFloat,
        rx: RemoteFloat,
        ry: RemoteFloat,
        paint: RemotePaint,
    ) {
        internalCanvas.drawRoundRect(left, top, right, bottom, rx, ry, paint)
    }

    /** Draws a path using the specified [paint]. */
    public fun drawPath(path: android.graphics.Path, paint: RemotePaint) {
        internalCanvas.drawPath(path, paint)
    }

    /** Draws a remote path using the specified [paint]. */
    public fun drawPath(path: RemotePath, paint: RemotePaint) {
        internalCanvas.drawRPath(path, paint)
    }

    /** Draws a bitmap at ([left], [top]) using the specified [paint]. */
    public fun drawBitmap(
        bitmap: RemoteBitmap,
        left: RemoteFloat,
        top: RemoteFloat,
        paint: RemotePaint,
    ) {
        internalCanvas.drawBitmap(bitmap, left, top, paint)
    }

    /** Draws a bitmap scaled to the destination rectangle. */
    public fun drawScaledBitmap(
        bitmap: RemoteBitmap,
        srcLeft: RemoteFloat,
        srcTop: RemoteFloat,
        srcRight: RemoteFloat,
        srcBottom: RemoteFloat,
        dstLeft: RemoteFloat,
        dstTop: RemoteFloat,
        dstRight: RemoteFloat,
        dstBottom: RemoteFloat,
        scaleType: Int,
        scaleFactor: RemoteFloat,
        contentDescription: String?,
    ) {
        internalCanvas.drawScaledBitmap(
            bitmap,
            srcLeft,
            srcTop,
            srcRight,
            srcBottom,
            dstLeft,
            dstTop,
            dstRight,
            dstBottom,
            scaleType,
            scaleFactor,
            contentDescription,
        )
    }

    public fun drawTextOnCircle(
        text: RemoteString,
        centerX: RemoteFloat,
        centerY: RemoteFloat,
        radius: RemoteFloat,
        startAngle: RemoteFloat,
        warpRadiusOffset: RemoteFloat,
        alignment: DrawTextOnCircle.Alignment,
        placement: DrawTextOnCircle.Placement,
        paint: RemotePaint,
    ) {
        internalCanvas.drawTextOnCircle(
            text,
            centerX,
            centerY,
            radius,
            startAngle,
            warpRadiusOffset,
            alignment,
            placement,
            paint,
        )
    }
}

/** Returns the width of the component as a [RemoteFloat]. */
@get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public val RemoteCanvas.componentWidth: RemoteFloat
    get() = remoteComponentWidth(creationState)

/** Returns the height of the component as a [RemoteFloat]. */
@get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public val RemoteCanvas.componentHeight: RemoteFloat
    get() = remoteComponentHeight(creationState)
