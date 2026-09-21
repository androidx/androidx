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

package androidx.compose.remote.creation.dsl

import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.core.Operation
import androidx.compose.remote.core.RcPlatformServices
import androidx.compose.remote.core.RcProfiles
import androidx.compose.remote.core.operations.AddMesh2D
import androidx.compose.remote.core.operations.DrawMesh2D
import androidx.compose.remote.core.operations.MatrixFromMesh2D
import androidx.compose.remote.core.operations.utilities.Mesh2DGenerator
import androidx.compose.remote.creation.RemoteComposeWriter
import androidx.compose.remote.creation.profile.Profile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for the Kotlin mesh DSL.
 *
 * These check the part the DSL is actually responsible for: that an unset channel stays unset
 * rather than becoming a zero expression, that the readable enums reach the wire as the right ints,
 * and that `u` and `v` in a mesh block really are the per-vertex domain parameters and not some
 * unrelated variable.
 */
class RcMeshTest {

    // Meshes live in the experimental profiles, so a document must opt in to write them.
    private val testProfile =
        Profile(
            CoreDocument.DOCUMENT_API_LEVEL,
            RcProfiles.PROFILE_ANDROIDX or RcProfiles.PROFILE_EXPERIMENTAL,
            RcPlatformServices.None,
        ) { _, profile, _ ->
            RemoteComposeWriter(profile)
        }

    private fun operationsOf(writer: RemoteComposeWriter): List<Operation> {
        val operations = ArrayList<Operation>()
        writer.buffer.inflateFromBuffer(operations)
        return operations
    }

    @Test
    fun expressionMeshReachesTheWire() {
        val writer = RemoteComposeWriter(testProfile)
        val scope = RcScopeImpl(writer)

        val mesh =
            scope.remoteMesh2D(RcMeshLayout.Grid, uCount = 24, vCount = 16) {
                x = u * 300f
                y = v * 200f
            }
        scope.drawMesh2D(mesh)

        val operations = operationsOf(writer)
        val add = operations.filterIsInstance<AddMesh2D>().single()
        val draw = operations.filterIsInstance<DrawMesh2D>().single()

        assertTrue(add.toString(), add.toString().contains("layout=${Mesh2DGenerator.LAYOUT_GRID}"))
        assertTrue(add.toString(), add.toString().contains("u=24"))
        assertTrue(add.toString(), add.toString().contains("v=16"))
        // An untextured mesh draws its vertex colours alone.
        assertTrue(draw.toString(), draw.toString().contains("${DrawMesh2D.BLEND_COLORS_ONLY}"))
    }

    @Test
    fun layoutEnumMapsToTheWireValue() {
        assertEquals(Mesh2DGenerator.LAYOUT_GRID, RcMeshLayout.Grid.value)
        assertEquals(Mesh2DGenerator.LAYOUT_POLAR, RcMeshLayout.Polar.value)
        assertEquals(Mesh2DGenerator.LAYOUT_RING, RcMeshLayout.Ring.value)
        assertEquals(Mesh2DGenerator.LAYOUT_STRIP, RcMeshLayout.Strip.value)
        assertEquals(Mesh2DGenerator.LAYOUT_FAN, RcMeshLayout.Fan.value)
        assertEquals(Mesh2DGenerator.LAYOUT_PATH_STRIP, RcMeshLayout.PathStrip.value)
    }

    @Test
    fun matrixEnumMapsToTheWireValue() {
        assertEquals(MatrixFromMesh2D.FLAG_ORIGIN, RcMeshMatrix.Origin.value)
        assertEquals(MatrixFromMesh2D.FLAG_ROTATION, RcMeshMatrix.Rotation.value)
        assertEquals(MatrixFromMesh2D.FLAG_SCALE, RcMeshMatrix.Scale.value)
        assertEquals(MatrixFromMesh2D.FLAG_FULL, RcMeshMatrix.Full.value)
    }

    @Test
    fun meshWithNoChannelsSetIsValid() {
        // Every channel is optional: with none set the layout's default geometry is the whole
        // definition, which is what makes `layout: polar` useful without spending expressions on
        // cos and sin.
        val writer = RemoteComposeWriter(testProfile)
        val scope = RcScopeImpl(writer)

        val mesh = scope.remoteMesh2D(RcMeshLayout.Polar, uCount = 32, vCount = 4) {}
        scope.drawMesh2D(mesh)

        val add = operationsOf(writer).filterIsInstance<AddMesh2D>().single()
        assertTrue(
            add.toString(),
            add.toString().contains("layout=${Mesh2DGenerator.LAYOUT_POLAR}"),
        )
    }

    @Test
    fun literalMeshDefaultsToSequentialIndices() {
        val writer = RemoteComposeWriter(testProfile)
        val scope = RcScopeImpl(writer)

        // Three vertices and no index array means "draw them in order".
        val mesh = scope.remoteMesh2DValues(verts = floatArrayOf(0f, 0f, 10f, 0f, 0f, 10f))
        scope.drawMesh2D(mesh)

        val add = operationsOf(writer).filterIsInstance<AddMesh2D>().single()
        assertTrue(add.toString(), add.toString().contains("type=${AddMesh2D.TYPE_VALUES}"))
    }

    @Test
    fun matrixFromMeshIsWritten() {
        val writer = RemoteComposeWriter(testProfile)
        val scope = RcScopeImpl(writer)

        val ribbon =
            scope.remoteMesh2D(RcMeshLayout.PathStrip, uCount = 64, vCount = 2) {
                width = RcFloat(28f)
            }
        scope.matrixFromMesh2D(ribbon, RcFloat(0.35f), RcFloat(0.5f), RcMeshMatrix.Rotation)

        val matrix = operationsOf(writer).filterIsInstance<MatrixFromMesh2D>().single()
        assertTrue(
            matrix.toString(),
            matrix.toString().endsWith("${MatrixFromMesh2D.FLAG_ROTATION}"),
        )
    }

    @Test
    fun halfFloatMeshUsesTheCompactWireType() {
        val writer = RemoteComposeWriter(testProfile)
        val scope = RcScopeImpl(writer)

        val mesh =
            scope.remoteMesh2DValues(
                verts = floatArrayOf(0f, 0f, 16f, 0f, 0f, 16f),
                uv = floatArrayOf(0f, 0f, 1f, 0f, 0f, 1f),
                halfFloat = true,
            )
        scope.drawMesh2D(mesh)

        val add = operationsOf(writer).filterIsInstance<AddMesh2D>().single()
        assertTrue(add.toString(), add.toString().contains("type=${AddMesh2D.TYPE_F16_VALUES}"))
    }

    @Test
    fun rgbHelperAndCustomTexUvAndTexturedBlendModesReachTheWire() {
        val writer = RemoteComposeWriter(testProfile)
        val scope = RcScopeImpl(writer)

        val image = scope.createBitmap(64, 64)
        val mesh =
            scope.remoteMesh2D(RcMeshLayout.Ring, uCount = 48, vCount = 4) {
                texU = u * 2f
                texV = v * 2f
                rgb(red = u, green = v, blue = RcFloat(0.5f))
            }
        scope.drawMesh2D(mesh, image = image, blend = RcMeshBlend.ColorsOnly)
        scope.drawMesh2D(mesh, image = image, blend = RcMeshBlend.Modulate)

        val ops = operationsOf(writer)
        val add = ops.filterIsInstance<AddMesh2D>().single()
        val draws = ops.filterIsInstance<DrawMesh2D>()

        assertTrue(add.toString(), add.toString().contains("layout=${Mesh2DGenerator.LAYOUT_RING}"))
        assertEquals(2, draws.size)
        assertTrue(
            draws[0].toString(),
            draws[0].toString().contains("blend=${DrawMesh2D.BLEND_COLORS_ONLY}"),
        )
        assertTrue(
            draws[1].toString(),
            draws[1].toString().contains("blend=${DrawMesh2D.BLEND_MODULATE}"),
        )
    }

    @Test
    fun literalMeshWithGridMetadataSupportsFullMatrixExtraction() {
        val writer = RemoteComposeWriter(testProfile)
        val scope = RcScopeImpl(writer)

        val carpet =
            scope.remoteMesh2DValues(
                verts = floatArrayOf(0f, 0f, 100f, 0f, 20f, 80f, 120f, 80f),
                indices = intArrayOf(0, 1, 2, 1, 3, 2),
                uv = null,
                colors = intArrayOf(-1, -1, -1, -1),
                layout = RcMeshLayout.Grid,
                uCount = 2,
                vCount = 2,
                halfFloat = false,
            )
        scope.matrixFromMesh2D(carpet, RcFloat(0.5f), RcFloat(0.5f), RcMeshMatrix.Full)

        val ops = operationsOf(writer)
        val add = ops.filterIsInstance<AddMesh2D>().single()
        val matrix = ops.filterIsInstance<MatrixFromMesh2D>().single()

        assertTrue(add.toString(), add.toString().contains("type=${AddMesh2D.TYPE_VALUES}"))
        assertTrue(add.toString(), add.toString().contains("u=2"))
        assertTrue(add.toString(), add.toString().contains("v=2"))
        assertTrue(matrix.toString(), matrix.toString().endsWith("${MatrixFromMesh2D.FLAG_FULL}"))
    }

    @Test
    fun scopePairHelpersAndFloatMatrixOverloadReachTheWire() {
        val writer = RemoteComposeWriter(testProfile)
        val scope = RcScopeImpl(writer)

        val mesh =
            scope.remoteMesh2D(RcMeshLayout.Strip, uCount = 12, vCount = 2) {
                xy(u * 200f, v * 40f)
                texUv(u, v)
                rgba(red = u, green = v, blue = RcFloat(0.25f), alpha = RcFloat(0.9f))
            }
        scope.matrixFromMesh2D(mesh, 0.25f, 0.75f, RcMeshMatrix.Scale)

        val ops = operationsOf(writer)
        val add = ops.filterIsInstance<AddMesh2D>().single()
        val matrix = ops.filterIsInstance<MatrixFromMesh2D>().single()

        assertTrue(
            add.toString(),
            add.toString().contains("layout=${Mesh2DGenerator.LAYOUT_STRIP}"),
        )
        assertTrue(matrix.toString(), matrix.toString().contains("0.25, 0.75"))
        assertTrue(matrix.toString(), matrix.toString().endsWith("${MatrixFromMesh2D.FLAG_SCALE}"))
    }
}
