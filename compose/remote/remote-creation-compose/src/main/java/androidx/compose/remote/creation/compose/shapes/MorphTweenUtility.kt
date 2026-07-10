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

package androidx.compose.remote.creation.compose.shapes

import androidx.compose.remote.core.RcPlatformServices.RcPathArrayCreator
import androidx.compose.remote.core.operations.PathData
import androidx.compose.remote.creation.RemoteComposeWriter
import androidx.graphics.shapes.Cubic
import androidx.graphics.shapes.Morph
import androidx.graphics.shapes.RoundedPolygon

internal object MorphTweenUtility {
    fun emitMorphAsTweens(
        writer: RemoteComposeWriter,
        p1: RoundedPolygon,
        p2: RoundedPolygon,
        progress: Float,
    ) {
        val morph = Morph(p1, p2)
        val cubics1 = morph.asCubics(0f)
        val cubics2 = morph.asCubics(1f)

        val pathData1 = cubicsToPathData(cubics1)
        val pathData2 = cubicsToPathData(cubics2)

        val id1 =
            writer.addPathData(
                object : RcPathArrayCreator {
                    override fun createFloatArray(): FloatArray = pathData1
                }
            )
        val id2 =
            writer.addPathData(
                object : RcPathArrayCreator {
                    override fun createFloatArray(): FloatArray = pathData2
                }
            )

        val tweenId = writer.pathTween(id1, id2, progress)
        writer.buffer.addDrawPath(tweenId)
    }

    fun cubicsToPathData(cubics: List<Cubic>): FloatArray {
        if (cubics.isEmpty()) return floatArrayOf()
        // Path format: MOVE (3) + CUBIC (9 * N) + CLOSE (1)
        val data = FloatArray(3 + cubics.size * 9 + 1)
        var i = 0
        val first = cubics[0]
        data[i++] = PathData.MOVE_NAN
        data[i++] = first.anchor0X
        data[i++] = first.anchor0Y

        for (j in cubics.indices) {
            val cubic = cubics[j]
            data[i++] = PathData.CUBIC_NAN
            data[i++] = 0f // padding
            data[i++] = 0f // padding
            data[i++] = cubic.control0X
            data[i++] = cubic.control0Y
            data[i++] = cubic.control1X
            data[i++] = cubic.control1Y
            data[i++] = cubic.anchor1X
            data[i++] = cubic.anchor1Y
        }
        data[i++] = PathData.CLOSE_NAN
        return data
    }
}
