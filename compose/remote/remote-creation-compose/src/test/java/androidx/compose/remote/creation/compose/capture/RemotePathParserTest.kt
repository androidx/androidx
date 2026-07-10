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
import androidx.compose.remote.core.operations.Utils
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.remote.creation.compose.vector.RemotePathNode
import java.util.ArrayList
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class RemotePathParserTest {

    val testRemoteStateScope = NoRemoteCompose()

    @Test
    fun testRelativeReflectiveQuadTo() {
        // M 100 100
        // q -10 0 -20 0  (Control: 90 100, End: 80 100)
        // t -20 0        (Reflected Control: 80+(80-90) = 70 100. End: 80-20 = 60 100)

        val nodes =
            listOf(
                RemotePathNode.MoveTo(100f.rf, 100f.rf),
                RemotePathNode.RelativeQuadTo((-10f).rf, 0f.rf, (-20f).rf, 0f.rf),
                RemotePathNode.RelativeReflectiveQuadTo((-20f).rf, 0f.rf),
            )

        val remotePath = nodes.toRemotePath(creationState = testRemoteStateScope)
        val pathArray = remotePath.createFloatArray()

        // Expected commands:
        // MOVE (100, 100)
        // QUADRATIC (calculated absolute points)
        // QUADRATIC (calculated absolute points)

        // RemotePathBase constants: MOVE=10, QUADRATIC=12

        val ops = parsePathArray(pathArray)
        assertEquals(3, ops.size)

        // Op 1: MoveTo 100, 100
        assertEquals("MoveTo", ops[0].type)
        assertEquals(100f, ops[0].points[0], 0.01f)
        assertEquals(100f, ops[0].points[1], 0.01f)

        // Op 2: QuadTo
        // Start (implicit 100,100)
        // Control: 100-10 = 90, 100
        // End: 100-20 = 80, 100
        assertEquals("QuadTo", ops[1].type)
        assertEquals(90f, ops[1].points[0], 0.01f)
        assertEquals(100f, ops[1].points[1], 0.01f)
        assertEquals(80f, ops[1].points[2], 0.01f)
        assertEquals(100f, ops[1].points[3], 0.01f)

        // Op 3: QuadTo (from t)
        // Start (implicit 80,100)
        // Control: Reflected 80 + (80 - 90) = 70
        // End: 80 - 20 = 60
        assertEquals("QuadTo", ops[2].type)
        assertEquals(70f, ops[2].points[0], 0.01f)
        assertEquals(100f, ops[2].points[1], 0.01f)
        assertEquals(60f, ops[2].points[2], 0.01f)
        assertEquals(100f, ops[2].points[3], 0.01f)
    }

    private data class PathOp(val type: String, val points: FloatArray) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as PathOp

            if (type != other.type) return false
            if (!points.contentEquals(other.points)) return false

            return true
        }

        override fun hashCode(): Int {
            var result = type.hashCode()
            result = 31 * result + points.contentHashCode()
            return result
        }
    }

    private fun parsePathArray(array: FloatArray): List<PathOp> {
        val ops = ArrayList<PathOp>()
        var i = 0
        // The array might be padded or have a size. RemotePath.createFloatArray returns the buffer
        // which might be large.
        // We should check until DONE or look at size if possible, but createFloatArray returns just
        // the array.
        // RemotePath usually has a size, but we only have the array here.
        // We can assume it ends with DONE or 0/NaN depending on impl.
        // But let's just parse until we hit a known terminator or end of valid data.
        // Actually RemotePathBase.DONE is 16.

        while (i < array.size) {
            val id = Utils.idFromNan(array[i])
            if (id == RemotePathBase.DONE) break

            // Validate that we found a command (encoded as NaN)
            // If it's not a command (i.e. not NaN), it implies misalignment or data corruption.

            when (id) {
                RemotePathBase.MOVE -> {
                    // MoveTo: 1 command float + 2 coordinate floats (total 3)
                    ops.add(PathOp("MoveTo", floatArrayOf(array[i + 1], array[i + 2])))
                    i += 3
                }
                RemotePathBase.LINE -> {
                    // LineTo: 1 command float + 2 padding floats + 2 coordinate floats (total 5)
                    // Data is at array[i+3], array[i+4]
                    ops.add(PathOp("LineTo", floatArrayOf(array[i + 3], array[i + 4])))
                    i += 5
                }
                RemotePathBase.QUADRATIC -> {
                    // QuadTo: 1 command float + 2 padding floats + 4 coordinate floats (total 7)
                    ops.add(
                        PathOp(
                            "QuadTo",
                            floatArrayOf(array[i + 3], array[i + 4], array[i + 5], array[i + 6]),
                        )
                    )
                    i += 7
                }
                RemotePathBase.CUBIC -> {
                    // CubicTo: 1 command float + 2 padding floats + 6 coordinate floats (total 9)
                    ops.add(
                        PathOp(
                            "CubicTo",
                            floatArrayOf(
                                array[i + 3],
                                array[i + 4],
                                array[i + 5],
                                array[i + 6],
                                array[i + 7],
                                array[i + 8],
                            ),
                        )
                    )
                    i += 9
                }
                RemotePathBase.CLOSE -> {
                    // Close: 1 command float (total 1)
                    ops.add(PathOp("Close", FloatArray(0)))
                    i += 1
                }
                else -> {
                    // Unknown command or misalignment
                    i++
                }
            }
        }
        return ops
    }
}
