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

package androidx.compose.remote.creation.compose.capture

import androidx.compose.remote.core.RemotePathBase
import androidx.compose.ui.graphics.vector.PathNode
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class RemotePathParserRealDataTest {

    @Test
    fun testRealMapData() {
        val nodes =
            listOf(
                PathNode.MoveTo(570.41f, 838.17f),
                PathNode.LineTo(360f, 764.37f),
                PathNode.LineTo(173.52f, 836.85f),
                PathNode.RelativeQuadTo(-11.43f, 4.48f, -22.25f, 2.86f),
                PathNode.RelativeReflectiveQuadTo(-19.77f, -7.34f),
                PathNode.RelativeReflectiveQuadTo(-14.29f, -15.41f),
                PathNode.RelativeReflectiveQuadTo(-5.34f, -22.13f),
                PathNode.VerticalTo(230f),
                PathNode.RelativeQuadTo(0f, -15.63f, 9.05f, -27.78f),
                PathNode.RelativeReflectiveQuadTo(24.45f, -17.87f),
                PathNode.RelativeLineTo(185.04f, -62.52f),
                PathNode.RelativeQuadTo(6.96f, -2.24f, 14.29f, -3.48f),
                PathNode.ReflectiveQuadTo(360f, 117.11f),
                PathNode.RelativeReflectiveQuadTo(15.29f, 1.12f),
                PathNode.RelativeReflectiveQuadTo(14.29f, 3.6f),
                PathNode.LineTo(600f, 195.63f),
                PathNode.RelativeLineTo(186.48f, -72.48f),
                PathNode.RelativeQuadTo(11.43f, -4.48f, 22.25f, -2.86f),
                PathNode.RelativeReflectiveQuadTo(19.77f, 7.34f),
                PathNode.RelativeReflectiveQuadTo(14.29f, 15.41f),
                PathNode.RelativeReflectiveQuadTo(5.34f, 22.13f),
                PathNode.VerticalTo(730f),
                PathNode.RelativeQuadTo(0f, 15.63f, -9.05f, 27.78f),
                PathNode.RelativeReflectiveQuadTo(-24.45f, 17.87f),
                PathNode.LineTo(629.59f, 838.17f),
                PathNode.RelativeQuadTo(-6.96f, 2.24f, -14.29f, 3.48f),
                PathNode.ReflectiveQuadTo(600f, 842.89f),
                PathNode.RelativeReflectiveQuadTo(-15.29f, -1.12f),
                PathNode.RelativeReflectiveQuadTo(-14.29f, -3.6f),
                PathNode.Close,
                PathNode.MoveTo(560f, 738.65f),
                PathNode.VerticalTo(277.35f),
                PathNode.RelativeLineTo(-160f, -56f),
                PathNode.RelativeVerticalTo(461.3f),
                PathNode.RelativeLineTo(160f, 56f),
                PathNode.Close,
                PathNode.MoveTo(640f, 738.65f),
                PathNode.RelativeLineTo(117.13f, -38.8f),
                PathNode.VerticalTo(232.3f),
                PathNode.LineTo(640f, 277.35f),
                PathNode.VerticalTo(738.65f),
                PathNode.RelativeMoveTo(80f, 0f),
                PathNode.RelativeLineTo(117.13f, -38.8f),
                PathNode.VerticalTo(232.3f),
                PathNode.LineTo(640f, 277.35f),
                PathNode.RelativeVerticalTo(461.3f),
                PathNode.Close,
                PathNode.MoveTo(202.87f, 727.7f),
                PathNode.LineTo(320f, 682.65f),
                PathNode.VerticalTo(221.35f),
                PathNode.RelativeLineTo(-117.13f, 38.8f),
                PathNode.VerticalTo(727.7f),
                PathNode.Close,
                PathNode.MoveTo(640f, 277.35f),
                PathNode.RelativeVerticalTo(461.3f),
                PathNode.VerticalTo(277.35f),
                // The following commands replicate the exact sequence including the 'Zm' (Close,
                // RelativeMoveTo)
                // pattern which triggered the rMoveTo bug.
                PathNode.Close,
                PathNode.RelativeMoveTo(-320f, -56f),
                PathNode.RelativeVerticalTo(461.3f),
                PathNode.VerticalTo(221.35f),
                PathNode.Close,
            )

        val remotePath = nodes.toRemotePath()

        // Create float array and verify its integrity.
        val pathString = remotePath.toString()
        println("Path String: $pathString")

        val pathArray = remotePath.createFloatArray()
        verifyPathOpCodes(pathArray)
    }

    private fun verifyPathOpCodes(array: FloatArray) {
        var i = 0
        // RemotePathBase loops until mSize, but we have array. Array might be larger?
        // createFloatArray() uses Arrays.copyOf(mPath, mSize) so it should be exact size.

        println("Array size: ${array.size}")

        while (i < array.size) {
            if (array[i].isNaN()) {
                val id = androidx.compose.remote.core.operations.Utils.idFromNan(array[i])
                if (id == RemotePathBase.DONE) break

                when (id) {
                    RemotePathBase.MOVE -> {
                        println("MOVE at $i")
                        // MoveTo is size 3: 1 command + 2 coords.
                        i += 3
                    }
                    RemotePathBase.LINE -> {
                        println("LINE at $i")
                        // LineTo is size 5: 1 command + 2 padding + 2 coords.
                        i += 5
                    }
                    RemotePathBase.QUADRATIC -> {
                        println("QUADRATIC at $i")
                        // QuadTo is size 7: 1 command + 2 padding + 4 coords.
                        i += 7
                    }
                    RemotePathBase.CONIC -> {
                        println("CONIC at $i")
                        // ConicTo is size 8: 1 command + 2 padding + 5 coords.
                        i += 8
                    }
                    RemotePathBase.CUBIC -> {
                        println("CUBIC at $i")
                        // CubicTo is size 9: 1 command + 2 padding + 6 coords.
                        i += 9
                    }
                    RemotePathBase.CLOSE -> {
                        println("CLOSE at $i")
                        // Close is size 1: 1 command.
                        i += 1
                    }
                    else -> {
                        fail("Odd command $id at index $i. Raw: ${array[i]}")
                    }
                }
            } else {
                fail("Expected command (NaN) at index $i but got ${array[i]}")
            }
        }
    }
}
