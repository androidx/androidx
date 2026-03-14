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
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.remote.creation.compose.vector.RemotePathNode
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class RemotePathParserRealDataTest {

    val testRemoteStateScope = NoRemoteCompose()

    @Test
    fun testRealMapData() {
        val nodes =
            listOf(
                RemotePathNode.MoveTo(570.41f.rf, 838.17f.rf),
                RemotePathNode.LineTo(360f.rf, 764.37f.rf),
                RemotePathNode.LineTo(173.52f.rf, 836.85f.rf),
                RemotePathNode.RelativeQuadTo((-11.43f).rf, 4.48f.rf, (-22.25f).rf, 2.86f.rf),
                RemotePathNode.RelativeReflectiveQuadTo((-19.77f).rf, (-7.34f).rf),
                RemotePathNode.RelativeReflectiveQuadTo((-14.29f).rf, (-15.41f).rf),
                RemotePathNode.RelativeReflectiveQuadTo((-5.34f).rf, (-22.13f).rf),
                RemotePathNode.VerticalTo(230f.rf),
                RemotePathNode.RelativeQuadTo(0f.rf, (-15.63f).rf, 9.05f.rf, (-27.78f).rf),
                RemotePathNode.RelativeReflectiveQuadTo(24.45f.rf, (-17.87f).rf),
                RemotePathNode.RelativeLineTo(185.04f.rf, (-62.52f).rf),
                RemotePathNode.RelativeQuadTo(6.96f.rf, (-2.24f).rf, 14.29f.rf, (-3.48f).rf),
                RemotePathNode.ReflectiveQuadTo(360f.rf, 117.11f.rf),
                RemotePathNode.RelativeReflectiveQuadTo(15.29f.rf, 1.12f.rf),
                RemotePathNode.RelativeReflectiveQuadTo(14.29f.rf, 3.6f.rf),
                RemotePathNode.LineTo(600f.rf, 195.63f.rf),
                RemotePathNode.RelativeLineTo(186.48f.rf, (-72.48f).rf),
                RemotePathNode.RelativeQuadTo(11.43f.rf, (-4.48f).rf, 22.25f.rf, (-2.86f).rf),
                RemotePathNode.RelativeReflectiveQuadTo(19.77f.rf, 7.34f.rf),
                RemotePathNode.RelativeReflectiveQuadTo(14.29f.rf, 15.41f.rf),
                RemotePathNode.RelativeReflectiveQuadTo(5.34f.rf, 22.13f.rf),
                RemotePathNode.VerticalTo(730f.rf),
                RemotePathNode.RelativeQuadTo(0f.rf, 15.63f.rf, (-9.05f).rf, 27.78f.rf),
                RemotePathNode.RelativeReflectiveQuadTo((-24.45f).rf, 17.87f.rf),
                RemotePathNode.LineTo(629.59f.rf, 838.17f.rf),
                RemotePathNode.RelativeQuadTo((-6.96f).rf, 2.24f.rf, (-14.29f).rf, 3.48f.rf),
                RemotePathNode.ReflectiveQuadTo(600f.rf, 842.89f.rf),
                RemotePathNode.RelativeReflectiveQuadTo((-15.29f).rf, (-1.12f).rf),
                RemotePathNode.RelativeReflectiveQuadTo((-14.29f).rf, (-3.6f).rf),
                RemotePathNode.Close,
                RemotePathNode.MoveTo(560f.rf, 738.65f.rf),
                RemotePathNode.VerticalTo(277.35f.rf),
                RemotePathNode.RelativeLineTo((-160f).rf, (-56f).rf),
                RemotePathNode.RelativeVerticalTo(461.3f.rf),
                RemotePathNode.RelativeLineTo(160f.rf, 56f.rf),
                RemotePathNode.Close,
                RemotePathNode.MoveTo(640f.rf, 738.65f.rf),
                RemotePathNode.RelativeLineTo(117.13f.rf, (-38.8f).rf),
                RemotePathNode.VerticalTo(232.3f.rf),
                RemotePathNode.LineTo(640f.rf, 277.35f.rf),
                RemotePathNode.VerticalTo(738.65f.rf),
                RemotePathNode.RelativeMoveTo(80f.rf, 0f.rf),
                RemotePathNode.RelativeLineTo(117.13f.rf, (-38.8f).rf),
                RemotePathNode.VerticalTo(232.3f.rf),
                RemotePathNode.LineTo(640f.rf, 277.35f.rf),
                RemotePathNode.RelativeVerticalTo(461.3f.rf),
                RemotePathNode.Close,
                RemotePathNode.MoveTo(202.87f.rf, 727.7f.rf),
                RemotePathNode.LineTo(320f.rf, 682.65f.rf),
                RemotePathNode.VerticalTo(221.35f.rf),
                RemotePathNode.RelativeLineTo((-117.13f).rf, 38.8f.rf),
                RemotePathNode.VerticalTo(727.7f.rf),
                RemotePathNode.Close,
                RemotePathNode.MoveTo(640f.rf, 277.35f.rf),
                RemotePathNode.RelativeVerticalTo(461.3f.rf),
                RemotePathNode.VerticalTo(277.35f.rf),
                // The following commands replicate the exact sequence including the 'Zm' (Close,
                // RelativeMoveTo)
                // pattern which triggered the rMoveTo bug.
                RemotePathNode.Close,
                RemotePathNode.RelativeMoveTo((-320f).rf, (-56f).rf),
                RemotePathNode.RelativeVerticalTo(461.3f.rf),
                RemotePathNode.VerticalTo(221.35f.rf),
                RemotePathNode.Close,
            )

        val remotePath = nodes.toRemotePath(creationState = testRemoteStateScope)

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
