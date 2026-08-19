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
import androidx.compose.remote.creation.compose.layout.RemoteOffset
import androidx.compose.remote.creation.compose.layout.RemoteSize
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

    @Test
    fun remoteOffsetOverloads_addExpectedNodes() {
        val scope = RemotePathScope()
        scope.moveTo(RemoteOffset(10f.rf, 20f.rf))
        scope.lineTo(RemoteOffset(30f.rf, 40f.rf))
        scope.quadTo(RemoteOffset(50f.rf, 60f.rf), RemoteOffset(70f.rf, 80f.rf))
        scope.curveTo(
            RemoteOffset(90f.rf, 100f.rf),
            RemoteOffset(110f.rf, 120f.rf),
            RemoteOffset(130f.rf, 140f.rf),
        )
        scope.conicTo(RemoteOffset(150f.rf, 160f.rf), RemoteOffset(170f.rf, 180f.rf), 0.5f.rf)
        scope.arcTo(RemoteOffset(0f.rf, 0f.rf), RemoteSize(100f.rf, 100f.rf), 0f.rf, 90f.rf)
        scope.close()

        assertEquals(7, scope.nodes.size)
        assertTrue(scope.nodes[0] is RemotePathNode.MoveTo)
        assertTrue(scope.nodes[1] is RemotePathNode.LineTo)
        assertTrue(scope.nodes[2] is RemotePathNode.QuadTo)
        assertTrue(scope.nodes[3] is RemotePathNode.CurveTo)
        assertTrue(scope.nodes[4] is RemotePathNode.ConicTo)
        assertTrue(scope.nodes[5] is RemotePathNode.AddArc)
        assertEquals(RemotePathNode.Close, scope.nodes[6])
    }

    @Test
    fun addRect_addsFiveNodes() {
        val scope = RemotePathScope()
        scope.addRect(RemoteOffset(10f.rf, 20f.rf), RemoteSize(100f.rf, 50f.rf))

        assertEquals(5, scope.nodes.size)
        val move = scope.nodes[0] as RemotePathNode.MoveTo
        assertEquals(10f, move.x.constantValueOrNull)
        assertEquals(20f, move.y.constantValueOrNull)

        val line1 = scope.nodes[1] as RemotePathNode.LineTo
        assertEquals(110f, line1.x.constantValueOrNull)
        assertEquals(20f, line1.y.constantValueOrNull)

        val line2 = scope.nodes[2] as RemotePathNode.LineTo
        assertEquals(110f, line2.x.constantValueOrNull)
        assertEquals(70f, line2.y.constantValueOrNull)

        val line3 = scope.nodes[3] as RemotePathNode.LineTo
        assertEquals(10f, line3.x.constantValueOrNull)
        assertEquals(70f, line3.y.constantValueOrNull)

        assertEquals(RemotePathNode.Close, scope.nodes[4])
    }

    @Test
    fun addArc_addsArcWithForceMoveTo() {
        val scope = RemotePathScope()
        scope.addArc(RemoteOffset(10f.rf, 20f.rf), RemoteSize(100f.rf, 50f.rf), 0f.rf, 90f.rf)

        assertEquals(1, scope.nodes.size)
        val arc = scope.nodes[0] as RemotePathNode.AddArc
        assertEquals(10f, arc.left.constantValueOrNull)
        assertEquals(20f, arc.top.constantValueOrNull)
        assertEquals(110f, arc.right.constantValueOrNull)
        assertEquals(70f, arc.bottom.constantValueOrNull)
        assertEquals(0f, arc.startAngle.constantValueOrNull)
        assertEquals(90f, arc.sweepAngle.constantValueOrNull)
        assertTrue(arc.forceMoveTo)
    }

    @Test
    fun addOval_addsArcAndClose() {
        val scope = RemotePathScope()
        scope.addOval(RemoteOffset(0f.rf, 0f.rf), RemoteSize(100f.rf, 100f.rf))

        assertEquals(2, scope.nodes.size)
        val arc = scope.nodes[0] as RemotePathNode.AddArc
        assertEquals(0f, arc.left.constantValueOrNull)
        assertEquals(0f, arc.top.constantValueOrNull)
        assertEquals(100f, arc.right.constantValueOrNull)
        assertEquals(100f, arc.bottom.constantValueOrNull)
        assertEquals(0f, arc.startAngle.constantValueOrNull)
        assertEquals(360f, arc.sweepAngle.constantValueOrNull)
        assertTrue(arc.forceMoveTo)

        assertEquals(RemotePathNode.Close, scope.nodes[1])
    }

    @Test
    fun addCircle_addsOval() {
        val scope = RemotePathScope()
        scope.addCircle(RemoteOffset(50f.rf, 50f.rf), 25f.rf)

        assertEquals(2, scope.nodes.size)
        val arc = scope.nodes[0] as RemotePathNode.AddArc
        assertEquals(25f, arc.left.constantValueOrNull)
        assertEquals(25f, arc.top.constantValueOrNull)
        assertEquals(75f, arc.right.constantValueOrNull)
        assertEquals(75f, arc.bottom.constantValueOrNull)
        assertEquals(RemotePathNode.Close, scope.nodes[1])
    }

    @Test
    fun addRoundRect_addsArcsAndLines() {
        val scope = RemotePathScope()
        scope.addRoundRect(
            RemoteOffset(0f.rf, 0f.rf),
            RemoteSize(100f.rf, 100f.rf),
            RemoteOffset(10f.rf, 10f.rf),
        )

        // 1 MoveTo + (1 LineTo + 1 ArcTo) * 4 + 1 Close = 10 nodes
        assertEquals(10, scope.nodes.size)
        assertTrue(scope.nodes[0] is RemotePathNode.MoveTo)
        assertEquals(RemotePathNode.Close, scope.nodes[9])
    }

    @Test
    fun addPath_combinesNodes() {
        val path1 = RemotePathScope()
        path1.moveTo(0f.rf, 0f.rf)
        path1.lineTo(10f.rf, 10f.rf)

        val path2 = RemotePathScope()
        path2.moveTo(20f.rf, 20f.rf)
        path2.lineTo(30f.rf, 30f.rf)

        path1.addPath(path2)
        assertEquals(4, path1.nodes.size)
    }
}
