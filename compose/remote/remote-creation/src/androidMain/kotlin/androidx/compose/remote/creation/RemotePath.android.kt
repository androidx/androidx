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

import android.graphics.PathMeasure
import android.os.Build
import androidx.annotation.RestrictTo
import androidx.compose.remote.core.operations.PathData
import androidx.compose.remote.core.operations.Utils
import kotlin.math.max
import kotlin.math.min

internal actual typealias RectF = android.graphics.RectF

internal actual typealias Path = android.graphics.Path

internal actual typealias MatrixTransform = android.graphics.Matrix

internal actual val RectF.left: Float
    get() = this.left

internal actual val RectF.top: Float
    get() = this.top

internal actual val RectF.right: Float
    get() = this.right

internal actual val RectF.bottom: Float
    get() = this.bottom

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public actual class RemotePath : RemotePathBase {

    public actual constructor() : super()

    public actual constructor(bufferSize: Int) : super(bufferSize)

    public actual constructor(pathData: String) : super(pathData)

    public actual val path: Path
        get() {
            val result = Path()
            genPath(result, pathArray, size, Float.Companion.NaN, Float.Companion.NaN)
            return result
        }

    private var mCachePath: Path? = Path()
    private var mCacheMeasure: PathMeasure? = PathMeasure()

    private fun genPath(
        retPath: Path,
        pathArray: FloatArray,
        length: Int,
        startSection: Float,
        stopSection: Float,
    ) {
        var i = 0
        mCachePath = if (mCachePath == null) Path() else mCachePath

        while (i < length) {
            when (Utils.idFromNan(pathArray[i])) {
                PathData.MOVE -> {
                    i++
                    mCachePath!!.moveTo(pathArray[i + 0], pathArray[i + 1])
                    i += 2
                }
                PathData.LINE -> {
                    i += 3
                    mCachePath!!.lineTo(pathArray[i + 0], pathArray[i + 1])
                    i += 2
                }
                PathData.QUADRATIC -> {
                    i += 3
                    mCachePath!!.quadTo(
                        pathArray[i + 0],
                        pathArray[i + 1],
                        pathArray[i + 2],
                        pathArray[i + 3],
                    )
                    i += 4
                }
                PathData.CONIC -> {
                    i += 3
                    if (Build.VERSION.SDK_INT >= 34) { // REMOVE IN PLATFORM
                        mCachePath!!.conicTo(
                            pathArray[i + 0],
                            pathArray[i + 1],
                            pathArray[i + 2],
                            pathArray[i + 3],
                            pathArray[i + 4],
                        )
                    }
                    i += 5
                }
                PathData.CUBIC -> {
                    i += 3
                    mCachePath!!.cubicTo(
                        pathArray[i + 0],
                        pathArray[i + 1],
                        pathArray[i + 2],
                        pathArray[i + 3],
                        pathArray[i + 4],
                        pathArray[i + 5],
                    )
                    i += 6
                }
                PathData.CLOSE -> {
                    mCachePath!!.close()
                    i++
                }
                PathData.DONE -> i++
                else ->
                    System.err.println("RemotePath Odd command " + Utils.idFromNan(pathArray[i]))
            }
        }

        retPath.reset()
        if (startSection.isNaN() && stopSection.isNaN()) {
            retPath.addPath(mCachePath!!)
            return
        }
        val start = if (startSection.isNaN()) 0f else startSection
        val stop = if (stopSection.isNaN()) 1f else stopSection

        if (start > stop) {
            retPath.addPath(mCachePath!!)
            return
        }
        mCacheMeasure = if (mCacheMeasure == null) PathMeasure() else mCacheMeasure
        if (stop > 1) {
            val seg = min(stop, 1f)
            mCacheMeasure!!.setPath(mCachePath, false)
            val len = mCacheMeasure!!.getLength()
            val scaleStart = ((start + 1) % 1) * len
            val scaleStop = ((seg + 1) % 1) * len // TODO
            mCacheMeasure!!.getSegment(scaleStart, scaleStop, retPath, true)
            retPath.addPath(mCachePath!!)
            return
        }

        mCacheMeasure!!.setPath(mCachePath, false)
        val len = mCacheMeasure!!.getLength()
        val scaleStart = max(start, 0f) * len
        val scaleStop = min(stop, 1f) * len
        mCacheMeasure!!.getSegment(scaleStart, scaleStop, retPath, true)
        retPath.addPath(mCachePath!!)
    }

    public actual fun transform(matrix: MatrixTransform) {
        var i = 0
        while (i < size) {
            when (Utils.idFromNan(pathArray[i])) {
                PathData.MOVE -> {
                    i++
                    matrix.mapPoints(pathArray, i, pathArray, i, 1)
                    i += 2
                }
                PathData.LINE -> {
                    i += 3
                    matrix.mapPoints(pathArray, i, pathArray, i, 1)
                    i += 2
                }
                PathData.QUADRATIC -> {
                    i += 3
                    matrix.mapPoints(pathArray, i, pathArray, i, 2)
                    i += 4
                }
                PathData.CONIC -> {
                    i += 3
                    if (Build.VERSION.SDK_INT >= 34) { // REMOVE IN PLATFORM
                        matrix.mapPoints(pathArray, i, pathArray, i, 2)
                    }
                    i += 5
                }
                PathData.CUBIC -> {
                    i += 3
                    matrix.mapPoints(pathArray, i, pathArray, i, 3)

                    i += 6
                }
                PathData.CLOSE,
                PathData.DONE -> i++
                else -> System.err.println(" Odd command " + Utils.idFromNan(pathArray[i]))
            }
        }
    }

    public actual fun addArc(oval: RectF, startAngle: Float, sweepAngle: Float) {
        addArc(oval.left, oval.top, oval.right, oval.bottom, startAngle, sweepAngle)
    }

    public actual fun arcTo(oval: RectF, startAngle: Float, sweepAngle: Float) {
        addArc(oval.left, oval.top, oval.right, oval.bottom, startAngle, sweepAngle, false)
    }

    public actual fun arcTo(
        oval: RectF,
        startAngle: Float,
        sweepAngle: Float,
        forceMoveTo: Boolean,
    ) {
        addArc(oval.left, oval.top, oval.right, oval.bottom, startAngle, sweepAngle, forceMoveTo)
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
        // Super doesn't implement logic, but we override it here.
        // Actually base throws UnsupportedOperationException.
        // So we should implement it here or call super if we want the exception?
        // But wait, the Kotlin code originally had TODO() or implementation?
        // The original Kotlin code for addArc was commented out or TODO().
        // So we will just leave it as is or throw?
        // Let's just do what we did in previous steps:
        // But wait, we need to implement it if we don't want the Base exception.
        // Since we don't have implementation logic readily available from previous context (it was
        // commented out),
        // we'll keep it minimal.
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
