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

package androidx.compose.remote.creation.compose.vector

import androidx.compose.remote.creation.compose.capture.NoRemoteCompose
import androidx.compose.remote.creation.compose.state.remotePath
import androidx.compose.remote.creation.compose.state.rf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class RemotePathScopeTest {

    private val testRemoteStateScope = NoRemoteCompose()

    @Test
    fun conicTo_addsConicToNode() {
        val scope = RemotePathScope()
        scope.moveTo(10f.rf, 20f.rf)
        scope.conicTo(30f.rf, 40f.rf, 50f.rf, 60f.rf, 0.707f.rf)
        scope.close()

        assertEquals(3, scope.nodes.size)
        val move = scope.nodes[0] as RemotePathNode.MoveTo
        assertEquals(10f, move.x.constantValueOrNull)
        assertEquals(20f, move.y.constantValueOrNull)

        val conic = scope.nodes[1] as RemotePathNode.ConicTo
        assertEquals(30f, conic.x1.constantValueOrNull)
        assertEquals(40f, conic.y1.constantValueOrNull)
        assertEquals(50f, conic.x2.constantValueOrNull)
        assertEquals(60f, conic.y2.constantValueOrNull)
        assertEquals(0.707f, conic.weight.constantValueOrNull)

        assertEquals(RemotePathNode.Close, scope.nodes[2])
    }

    @Test
    fun conicToRelative_addsRelativeConicToNode() {
        val scope = RemotePathScope()
        scope.moveTo(10f.rf, 20f.rf)
        scope.conicToRelative(15f.rf, 25f.rf, 35f.rf, 45f.rf, 0.5f.rf)

        assertEquals(2, scope.nodes.size)
        val move = scope.nodes[0] as RemotePathNode.MoveTo
        assertEquals(10f, move.x.constantValueOrNull)
        assertEquals(20f, move.y.constantValueOrNull)

        val conic = scope.nodes[1] as RemotePathNode.RelativeConicTo
        assertEquals(15f, conic.dx1.constantValueOrNull)
        assertEquals(25f, conic.dy1.constantValueOrNull)
        assertEquals(35f, conic.dx2.constantValueOrNull)
        assertEquals(45f, conic.dy2.constantValueOrNull)
        assertEquals(0.5f, conic.weight.constantValueOrNull)
    }

    @Test
    fun remotePath_withConicTo() {
        val path =
            testRemoteStateScope.remotePath {
                moveTo(0f.rf, 0f.rf)
                conicTo(100f.rf, 0f.rf, 100f.rf, 100f.rf, 0.70710678f.rf)
                close()
            }
        val floatArray = path.createFloatArray()
        assertTrue(floatArray.isNotEmpty())
    }
}
