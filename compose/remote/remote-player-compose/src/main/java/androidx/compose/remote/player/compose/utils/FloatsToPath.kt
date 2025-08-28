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

package androidx.compose.remote.player.compose.utils

import android.util.Log
import androidx.compose.remote.core.operations.PathData
import androidx.compose.remote.core.operations.Utils.idFromNan
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import kotlin.math.max
import kotlin.math.min

/** Utility class to convert a float array representation of a path into a Compose [Path] object. */
internal object FloatsToPath {
    private const val TAG = "FloatsToPath"

    /**
     * Converts a float array representing a path into a Path object.
     *
     * @param retPath The Path object to populate with the converted path data.
     * @param floatPath The float array representing the path.
     * @param start The starting percentage (0.0 to 1.0) of the path to include.
     * @param stop The ending percentage (0.0 to 1.0) of the path to include.
     */
    fun genPath(retPath: Path, floatPath: FloatArray, start: Float, stop: Float) {
        var i = 0
        val path = Path() // todo this should be cached for performance
        while (i < floatPath.size) {
            when (idFromNan(floatPath[i])) {
                PathData.MOVE -> {
                    i++
                    path.moveTo(floatPath[i + 0], floatPath[i + 1])
                    i += 2
                }
                PathData.LINE -> {
                    i += 3
                    path.lineTo(floatPath[i + 0], floatPath[i + 1])
                    i += 2
                }
                PathData.QUADRATIC -> {
                    i += 3
                    path.quadraticTo(
                        floatPath[i + 0],
                        floatPath[i + 1],
                        floatPath[i + 2],
                        floatPath[i + 3],
                    )
                    i += 4
                }
                PathData.CONIC -> {
                    i += 3
                    //                    if (Build.VERSION.SDK_INT >= 34) { // REMOVE IN PLATFORM
                    //                        path.conicTo(
                    //                            floatPath[i + 0],
                    //                            floatPath[i + 1],
                    //                            floatPath[i + 2],
                    //                            floatPath[i + 3],
                    //                            floatPath[i + 4]
                    //                        )
                    //                    } // REMOVE IN PLATFORM
                    i += 5
                    // TODO(b/434130226): Conic operation not available in
                    // androidx.compose.ui.graphics
                    throw UnsupportedOperationException("Conic operation not yet implemented.")
                }
                PathData.CUBIC -> {
                    i += 3
                    path.cubicTo(
                        floatPath[i + 0],
                        floatPath[i + 1],
                        floatPath[i + 2],
                        floatPath[i + 3],
                        floatPath[i + 4],
                        floatPath[i + 5],
                    )
                    i += 6
                }
                PathData.CLOSE -> {
                    path.close()
                    i++
                }
                PathData.DONE -> i++
                else -> Log.w(TAG, " Odd command " + idFromNan(floatPath[i]))
            }
        }

        retPath.reset()
        if (start > 0f || stop < 1f) {
            if (start < stop) {
                val measure: PathMeasure = PathMeasure() // todo cached
                measure.setPath(path, false)
                val len: Float = measure.length
                val scaleStart = (max(start.toDouble(), 0.0) * len).toFloat()
                val scaleStop = (min(stop.toDouble(), 1.0) * len).toFloat()
                measure.getSegment(scaleStart, scaleStop, retPath, true)
            }
        } else {
            retPath.addPath(path)
        }
    }
}
