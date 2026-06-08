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

@file:OptIn(ExperimentalRemoteCreationApi::class)

package androidx.compose.remote.creation.dsl

import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.core.Paint3DContext
import androidx.compose.remote.core.RcPlatformServices
import androidx.compose.remote.core.RcProfiles
import androidx.compose.remote.core.operations.d3.MeshExpression3D
import androidx.compose.remote.core.operations.utilities.AnimatedFloatExpression
import androidx.compose.remote.core.operations.utilities.d3.Primitive3D
import androidx.compose.remote.creation.ExperimentalRemoteCreationApi
import androidx.compose.remote.creation.RemoteComposeWriter
import androidx.compose.remote.creation.RemoteComposeWriter3D
import androidx.compose.remote.creation.profile.Profile
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for the Kotlin 3D DSL.
 *
 * Each case compares bytes: a DSL call must write exactly what the matching [RemoteComposeWriter3D]
 * calls write, so the DSL, the JSON parser and the writer cannot drift apart.
 */
class RcScene3DScopeTest {

    // 3D ops live in the experimental profile.
    private val testProfile =
        Profile(
            CoreDocument.DOCUMENT_API_LEVEL,
            RcProfiles.PROFILE_ANDROIDX or RcProfiles.PROFILE_EXPERIMENTAL,
            RcPlatformServices.None,
        ) { _, profile, _ ->
            RemoteComposeWriter(profile)
        }

    private fun newWriter() = RemoteComposeWriter(testProfile)

    /** The bytes [write] adds to [writer]. */
    private fun bytesOf(
        writer: RemoteComposeWriter = newWriter(),
        write: (RemoteComposeWriter3D) -> Unit,
    ): ByteArray {
        val start = writer.buffer.buffer.size
        write(writer.rc3DWriter)
        val wire = writer.buffer.buffer
        return wire.buffer.copyOfRange(start, wire.size)
    }

    /** The bytes a scene3D [block] adds to a fresh writer. */
    private fun sceneBytes(block: RcScene3DScope.() -> Unit): ByteArray {
        val writer = newWriter()
        val start = writer.buffer.buffer.size
        RcScene3DScopeImpl(writer).block()
        val wire = writer.buffer.buffer
        return wire.buffer.copyOfRange(start, wire.size)
    }

    @Test
    fun pushAndPopMatchTheWriter() {
        val dsl = sceneBytes {
            pushMatrix()
            translate(1f, 2f, 3f)
            popMatrix()
            pushMatrix { scale(2f) }
        }
        val expected = bytesOf { w ->
            w.matrix3DPush()
            w.matrix3DOp(Paint3DContext.M3_TRANSLATE, floatArrayOf(1f, 2f, 3f))
            w.matrix3DPop()
            w.matrix3DPush()
            w.matrix3DOp(Paint3DContext.M3_SCALE, floatArrayOf(2f, 2f, 2f))
            w.matrix3DPop()
        }
        assertArrayEquals(expected, dsl)
    }

    @Test
    fun ambientLightIsWrittenWithTheOtherLightsOnTheNextDraw() {
        val dsl = sceneBytes {
            ambientLight(0xFF202020.toInt(), 0.25f)
            directionalLight(floatArrayOf(0f, -1f, 0f), 0xFFFFFFFF.toInt())
            ambientLight(0xFF101010.toInt(), RcFloat(0.5f))
            drawMesh(RcMesh(3), RcMeshMode.SoftwareFlat)
        }
        val expected = bytesOf { w ->
            w.setLights3D(
                intArrayOf(
                    Paint3DContext.LIGHT_AMBIENT,
                    Paint3DContext.LIGHT_DIRECTIONAL,
                    Paint3DContext.LIGHT_AMBIENT,
                ),
                intArrayOf(0xFF202020.toInt(), 0xFFFFFFFF.toInt(), 0xFF101010.toInt()),
                floatArrayOf(0f, 0f, 0f, 0.25f, 0f, -1f, 0f, 1f, 0f, 0f, 0f, 0.5f),
            )
            w.drawMesh3D(3, Paint3DContext.MODE_SOFTWARE_FLAT)
        }
        assertArrayEquals(expected, dsl)
    }

    @Test
    fun meshModeFlagsReachTheWire() {
        val dsl = sceneBytes {
            drawMesh(RcMesh(2), RcMeshMode.SoftwareSmooth or RcMeshMode.Transparent)
            drawMesh(
                RcMesh(2),
                RcMeshMode.SoftwareFlat or
                    RcMeshMode.Gpu or
                    RcMeshMode.Pbr or
                    RcMeshMode.DoubleSided,
            )
            drawMesh(RcMesh(2), RcMeshMode.CanvasFlat or RcMeshMode.FrontCw)
        }
        val expected = bytesOf { w ->
            w.drawMesh3D(2, Paint3DContext.MODE_SOFTWARE_SMOOTH or Paint3DContext.MODE_TRANSPARENT)
            w.drawMesh3D(
                2,
                Paint3DContext.MODE_SOFTWARE_FLAT or
                    Paint3DContext.MODE_GPU or
                    Paint3DContext.MODE_PBR or
                    Paint3DContext.MODE_DOUBLE_SIDED,
            )
            w.drawMesh3D(2, Paint3DContext.MODE_CANVAS_FLAT or Paint3DContext.MODE_FRONT_CW)
        }
        assertArrayEquals(expected, dsl)
    }

    @Test
    fun infinitePlaneDefaultsAndOptions() {
        val dsl = sceneBytes {
            primitiveInfinitePlane(1)
            primitiveInfinitePlane(
                2,
                y = -1f,
                spacing = 0.5f,
                lineWidth = 2f,
                lineColor = 0x80336699.toInt(),
                axis = RcPrimitive3D.AXIS_Z,
            )
        }
        val grey = 0x80 / 255f
        val expected = bytesOf { w ->
            w.meshPrimitive(
                1,
                Primitive3D.INFINITE_PLANE,
                0f,
                0,
                floatArrayOf(0f, 0f, 0f, 0f, 1f, 1f, grey, grey, grey),
            )
            w.meshPrimitive(
                2,
                Primitive3D.INFINITE_PLANE,
                0f,
                Primitive3D.AXIS_Z shl Primitive3D.FLAG_AXIS_SHIFT,
                floatArrayOf(0f, -1f, 0f, 0.5f, 2f, grey, 0x33 / 255f, 0x66 / 255f, 0x99 / 255f),
            )
        }
        assertArrayEquals(expected, dsl)
    }

    @Test
    fun axisSetsTheAxisFlagAndNativeLeavesTheBytesUnchanged() {
        val dsl = sceneBytes {
            primitiveSphere(1, 2f)
            primitiveSphere(2, 2f, axis = RcPrimitive3D.AXIS_NATIVE)
            primitivePlane(3, 2f, 3f, axis = RcPrimitive3D.AXIS_X)
            primitiveCube(4, 1f, 2f, 3f, axis = RcPrimitive3D.AXIS_Y)
        }
        val shift = Primitive3D.FLAG_AXIS_SHIFT
        val expected = bytesOf { w ->
            w.meshPrimitive(1, Primitive3D.SPHERE, 0f, 0, floatArrayOf(2f, 0f, 0f, 0f))
            w.meshPrimitive(2, Primitive3D.SPHERE, 0f, 0, floatArrayOf(2f, 0f, 0f, 0f))
            w.meshPrimitive(
                3,
                Primitive3D.PLANE,
                0f,
                (Primitive3D.UV shl Primitive3D.FLAG_UV_SHIFT) or (Primitive3D.AXIS_X shl shift),
                floatArrayOf(2f, 3f, 0f, 0f, 0f),
            )
            w.meshPrimitive(
                4,
                Primitive3D.CUBE,
                0f,
                Primitive3D.AXIS_Y shl shift,
                floatArrayOf(1f, 2f, 3f, 0f, 0f, 0f),
            )
        }
        assertArrayEquals(expected, dsl)
    }

    @Test
    fun unknownAxisOrUvModeIsRejected() {
        for (bad in intArrayOf(-1, 4)) {
            val axis =
                assertThrows(IllegalArgumentException::class.java) { RcPrimitive3D.axisFlag(bad) }
            assertTrue(
                axis.message,
                axis.message!!.contains("AXIS_NATIVE, AXIS_X, AXIS_Y or AXIS_Z"),
            )
            val uv =
                assertThrows(IllegalArgumentException::class.java) { RcPrimitive3D.uvFlag(bad) }
            assertTrue(uv.message, uv.message!!.contains("UV_NONE, UV, UV2 or UV3"))
        }
        assertThrows(IllegalArgumentException::class.java) {
            RcScene3DScopeImpl(newWriter()).primitiveSphere(1, 1f, axis = 4)
        }
    }

    @Test
    fun curvatureDensityFlagsTubes() {
        val points = floatArrayOf(0f, 0f, 0f, 1f, 0f, 0f)
        val dsl = sceneBytes {
            primitiveTube(1, 0.1f, points, curvatureDensity = true)
            primitiveProfileTube(2, points, floatArrayOf(0.2f, 0.1f), curvatureDensity = true)
        }
        val density = Primitive3D.FLAG_TUBE_CURVATURE_DENSITY
        val expected = bytesOf { w ->
            w.meshPrimitive(
                1,
                Primitive3D.TUBE,
                4f,
                density,
                floatArrayOf(0.1f, 0f, 0f, 0f, 1f, 0f, 0f),
            )
            w.meshPrimitive(
                2,
                Primitive3D.PROFILE_TUBE,
                0f,
                density,
                floatArrayOf(2f, 0f, 0f, 0f, 1f, 0f, 0f, 0.2f, 0.1f),
            )
        }
        assertArrayEquals(expected, dsl)
    }

    /**
     * See `MeshExpression3DWindingTest` for which side each surface faces with and without flip.
     */
    @Test
    fun onlyTheSphereAndAFlippedGeneralSurfaceFlipTheWinding() {
        val first: (RcFloat, RcFloat) -> RcFloat = { a, _ -> a }
        val dsl = sceneBytes {
            expressionSurface(1, -1f, 1f, -1f, 1f, uSeg = 2, vSeg = 2, height = first)
            expressionSphere(2, uSeg = 2, vSeg = 2, displacement = first)
            expressionCylinder(3, uSeg = 2, vSeg = 2, displacement = first)
            expressionGeneral(4, 0f, 1f, 0f, 1f, 2, 2, x = first, y = first, z = first)
            expressionGeneral(5, 0f, 1f, 0f, 1f, 2, 2, flip = true, x = first, y = first, z = first)
        }
        val u = floatArrayOf(AnimatedFloatExpression.VAR1)
        val none = emptyArray<FloatArray>()
        val flip = MeshExpression3D.FLAG_FLIP_WINDING
        val grid = floatArrayOf(0f, 1f, 3f, 0f, 1f, 3f)
        val expected = bytesOf { w ->
            w.meshExpression(
                1,
                MeshExpression3D.SURFACE_HEIGHT_FIELD,
                0,
                floatArrayOf(-1f, 1f, 3f, -1f, 1f, 3f, -1e6f, 1e6f),
                arrayOf(u),
                none,
                none,
            )
            w.meshExpression(
                2,
                MeshExpression3D.SURFACE_SPHERE,
                flip,
                floatArrayOf(1f, 3f, 3f),
                arrayOf(u),
                none,
                none,
            )
            w.meshExpression(
                3,
                MeshExpression3D.SURFACE_CYLINDER,
                0,
                floatArrayOf(0.8f, 1.8f, 3f, 3f),
                arrayOf(u),
                none,
                none,
            )
            w.meshExpression(
                4,
                MeshExpression3D.SURFACE_GENERAL,
                0,
                grid,
                arrayOf(u, u, u),
                none,
                none,
            )
            w.meshExpression(
                5,
                MeshExpression3D.SURFACE_GENERAL,
                flip,
                grid,
                arrayOf(u, u, u),
                none,
                none,
            )
        }
        assertArrayEquals(expected, dsl)
    }

    @Test
    fun flipNormalsAddsTheFlipNormalsFlag() {
        val first: (RcFloat, RcFloat) -> RcFloat = { a, _ -> a }
        val dsl = sceneBytes {
            expressionSurface(
                1,
                -1f,
                1f,
                -1f,
                1f,
                uSeg = 2,
                vSeg = 2,
                flipNormals = true,
                height = first,
            )
            expressionSphere(2, uSeg = 2, vSeg = 2, flipNormals = true, displacement = first)
            expressionCylinder(3, uSeg = 2, vSeg = 2, flipNormals = true, displacement = first)
            expressionGeneral(
                4,
                0f,
                1f,
                0f,
                1f,
                2,
                2,
                flip = true,
                flipNormals = true,
                x = first,
                y = first,
                z = first,
            )
        }
        val u = floatArrayOf(AnimatedFloatExpression.VAR1)
        val none = emptyArray<FloatArray>()
        val winding = MeshExpression3D.FLAG_FLIP_WINDING
        val normals = MeshExpression3D.FLAG_FLIP_NORMALS
        val expected = bytesOf { w ->
            w.meshExpression(
                1,
                MeshExpression3D.SURFACE_HEIGHT_FIELD,
                normals,
                floatArrayOf(-1f, 1f, 3f, -1f, 1f, 3f, -1e6f, 1e6f),
                arrayOf(u),
                none,
                none,
            )
            w.meshExpression(
                2,
                MeshExpression3D.SURFACE_SPHERE,
                winding or normals,
                floatArrayOf(1f, 3f, 3f),
                arrayOf(u),
                none,
                none,
            )
            w.meshExpression(
                3,
                MeshExpression3D.SURFACE_CYLINDER,
                normals,
                floatArrayOf(0.8f, 1.8f, 3f, 3f),
                arrayOf(u),
                none,
                none,
            )
            w.meshExpression(
                4,
                MeshExpression3D.SURFACE_GENERAL,
                winding or normals,
                floatArrayOf(0f, 1f, 3f, 0f, 1f, 3f),
                arrayOf(u, u, u),
                none,
                none,
            )
        }
        assertArrayEquals(expected, dsl)
    }
}
