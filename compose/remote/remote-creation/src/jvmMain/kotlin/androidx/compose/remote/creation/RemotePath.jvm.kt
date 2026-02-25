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

package androidx.compose.remote.creation

import androidx.annotation.RestrictTo
import androidx.compose.remote.core.operations.PathData
import androidx.compose.remote.core.operations.Utils
import java.awt.geom.AffineTransform
import java.awt.geom.Path2D
import java.awt.geom.Rectangle2D

internal actual typealias RectF = Rectangle2D.Float

internal actual typealias Path = Path2D.Float

internal actual typealias MatrixTransform = AffineTransform

internal actual val RectF.left: Float
    get() = this.x

internal actual val RectF.top: Float
    get() = this.y

internal actual val RectF.right: Float
    get() = this.x + this.width

internal actual val RectF.bottom: Float
    get() = this.y + this.height

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public actual class RemotePath : RemotePathBase {

    public actual constructor(bufferSize: Int) : super(bufferSize)

    public actual constructor() : super()

    public actual constructor(pathData: String) : super(pathData)

    public actual val path: Path
        get() {
            TODO()
        }

    public actual fun transform(matrix: MatrixTransform) {
        var i = 0
        while (i < size) {
            when (Utils.idFromNan(pathArray[i])) {
                PathData.MOVE -> {
                    i++
                    matrix.transform(pathArray, i, pathArray, i, 1)
                    i += 2
                }
                PathData.LINE -> {
                    i += 3
                    matrix.transform(pathArray, i, pathArray, i, 1)
                    i += 2
                }
                PathData.QUADRATIC -> {
                    i += 3
                    matrix.transform(pathArray, i, pathArray, i, 2)
                    i += 4
                }
                PathData.CONIC -> {
                    i += 3
                    matrix.transform(pathArray, i, pathArray, i, 2)
                    i += 5
                }
                PathData.CUBIC -> {
                    i += 3
                    matrix.transform(pathArray, i, pathArray, i, 3)

                    i += 6
                }
                PathData.CLOSE,
                PathData.DONE -> i++
                else -> System.err.println(" Odd command " + Utils.idFromNan(pathArray[i]))
            }
        }
    }

    public actual fun addArc(oval: RectF, startAngle: Float, sweepAngle: Float) {
        addArc(oval.x, oval.y, oval.x + oval.width, oval.y + oval.height, startAngle, sweepAngle)
    }

    public actual fun arcTo(oval: RectF, startAngle: Float, sweepAngle: Float) {
        addArc(
            oval.x,
            oval.y,
            oval.x + oval.width,
            oval.y + oval.height,
            startAngle,
            sweepAngle,
            false,
        )
    }

    public actual fun arcTo(
        oval: RectF,
        startAngle: Float,
        sweepAngle: Float,
        forceMoveTo: Boolean,
    ) {
        addArc(
            oval.x,
            oval.y,
            oval.x + oval.width,
            oval.y + oval.height,
            startAngle,
            sweepAngle,
            forceMoveTo,
        )
    }

    public actual fun arcTo(
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        startAngle: Float,
        sweepAngle: Float,
        forceMoveTo: Boolean,
    ) {
        addArc(left, top, right, bottom, startAngle, sweepAngle, forceMoveTo)
    }

    public actual override fun addArc(
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        startAngle: Float,
        sweepAngle: Float,
        forceMoveTo: Boolean,
    ) {
        super.addArc(left, top, right, bottom, startAngle, sweepAngle, forceMoveTo)
    }

    public actual companion object {
        public actual val MOVE: Int = RemotePathBase.MOVE
        public actual val LINE: Int = RemotePathBase.LINE
        public actual val QUADRATIC: Int = RemotePathBase.QUADRATIC
        public actual val CONIC: Int = RemotePathBase.CONIC
        public actual val CUBIC: Int = RemotePathBase.CUBIC
        public actual val CLOSE: Int = RemotePathBase.CLOSE
        public actual val DONE: Int = RemotePathBase.DONE
        public actual val MOVE_NAN: Float = RemotePathBase.MOVE_NAN
        public actual val LINE_NAN: Float = RemotePathBase.LINE_NAN
        public actual val QUADRATIC_NAN: Float = RemotePathBase.QUADRATIC_NAN
        public actual val CONIC_NAN: Float = RemotePathBase.CONIC_NAN
        public actual val CUBIC_NAN: Float = RemotePathBase.CUBIC_NAN
        public actual val CLOSE_NAN: Float = RemotePathBase.CLOSE_NAN
        public actual val DONE_NAN: Float = RemotePathBase.DONE_NAN

        public actual fun createCirclePath(
            rc: RemoteComposeWriter,
            x: Float,
            y: Float,
            rad: Float,
        ): RemotePath = RemotePathBase.createCirclePath(rc, x, y, rad)
    }
}
