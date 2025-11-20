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

package androidx.xr.arcore.apps.whitebox.mobile.samplerender.renderers

import android.opengl.Matrix
import androidx.collection.MutableObjectIntMap
import androidx.xr.arcore.Plane
import androidx.xr.arcore.apps.whitebox.mobile.samplerender.IndexBuffer
import androidx.xr.arcore.apps.whitebox.mobile.samplerender.Mesh
import androidx.xr.arcore.apps.whitebox.mobile.samplerender.SampleRender
import androidx.xr.arcore.apps.whitebox.mobile.samplerender.Shader
import androidx.xr.arcore.apps.whitebox.mobile.samplerender.Shader.BlendFactor
import androidx.xr.arcore.apps.whitebox.mobile.samplerender.Texture
import androidx.xr.arcore.apps.whitebox.mobile.samplerender.VertexBuffer
import androidx.xr.runtime.TrackingState
import androidx.xr.runtime.math.Matrix4
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Vector3
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.IntBuffer

/** Renders the detected AR planes. */
class PlaneRenderer(render: SampleRender) {

    private val mesh: Mesh
    private val indexBufferObject: IndexBuffer
    private val vertexBufferObject: VertexBuffer
    private val shader: Shader

    private var vertexBuffer: FloatBuffer =
        ByteBuffer.allocateDirect(INITIAL_VERTEX_BUFFER_SIZE_BYTES)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
    private var indexBuffer: IntBuffer =
        ByteBuffer.allocateDirect(INITIAL_INDEX_BUFFER_SIZE_BYTES)
            .order(ByteOrder.nativeOrder())
            .asIntBuffer()

    // Temporary lists/matrices allocated here to reduce number of allocations for each frame.
    private val modelMatrix = FloatArray(16)
    private val modelViewMatrix = FloatArray(16)
    private val modelViewProjectionMatrix = FloatArray(16)
    private val planeAngleUvMatrix = FloatArray(4) // 2x2 rotation matrix applied to uv coords.

    private val planeIndexMap = MutableObjectIntMap<Plane>()

    /**
     * Allocates and initializes OpenGL resources needed by the plane renderer. Must be called
     * during a [SampleRender.Renderer] callback, typically in
     * [SampleRender.Renderer.onSurfaceCreated].
     */
    init {
        val texture =
            Texture.createFromAsset(
                render,
                TEXTURE_NAME,
                Texture.WrapMode.REPEAT,
                Texture.ColorFormat.LINEAR,
            )
        shader =
            Shader.createFromAssets(
                    render,
                    VERTEX_SHADER_NAME,
                    FRAGMENT_SHADER_NAME,
                    defines = null,
                )
                .setTexture("u_Texture", texture)
                .setVec4("u_gridControl", GRID_CONTROL)
                .setBlend(
                    BlendFactor.DST_ALPHA, // RGB (src)
                    BlendFactor.ONE, // RGB (dest)
                    BlendFactor.ZERO, // ALPHA (src)
                    BlendFactor.ONE_MINUS_SRC_ALPHA, // ALPHA (dest)
                )
                .setDepthWrite(false)

        indexBufferObject = IndexBuffer(render, entries = null)
        vertexBufferObject = VertexBuffer(COORDS_PER_VERTEX, entries = null)
        val vertexBuffers = arrayOf(vertexBufferObject)
        mesh = Mesh(render, Mesh.PrimitiveMode.TRIANGLE_STRIP, indexBufferObject, vertexBuffers)
    }

    /** Updates the plane model transform matrix and extents. */
    private fun updatePlaneParameters(
        planeMatrix: FloatArray,
        extentX: Float,
        extentZ: Float,
        boundary: FloatBuffer?,
    ) {
        System.arraycopy(planeMatrix, 0, modelMatrix, 0, 16)
        if (boundary == null) {
            vertexBuffer.limit(0)
            indexBuffer.limit(0)
            return
        }

        // Generate a new set of vertices and a corresponding triangle strip index set so that
        // the plane boundary polygon has a fading edge. This is done by making a copy of the
        // boundary polygon vertices and scaling it down around center to push it inwards. Then
        // the index buffer is setup accordingly.
        boundary.rewind()
        val boundaryVertices = boundary.limit() / 2
        val numVertices: Int = boundaryVertices * VERTS_PER_BOUNDARY_VERT
        // drawn as GL_TRIANGLE_STRIP with 3n-2 triangles (n-2 for fill, 2n for perimeter).
        val numIndices: Int = boundaryVertices * INDICES_PER_BOUNDARY_VERT

        if (vertexBuffer.capacity() < numVertices * COORDS_PER_VERTEX) {
            var size = vertexBuffer.capacity()
            while (size < numVertices * COORDS_PER_VERTEX) {
                size *= 2
            }
            vertexBuffer =
                ByteBuffer.allocateDirect(BYTES_PER_FLOAT * size)
                    .order(ByteOrder.nativeOrder())
                    .asFloatBuffer()
        }
        vertexBuffer.rewind()
        vertexBuffer.limit(numVertices * COORDS_PER_VERTEX)

        if (indexBuffer.capacity() < numIndices) {
            var size = indexBuffer.capacity()
            while (size < numIndices) {
                size *= 2
            }
            indexBuffer =
                ByteBuffer.allocateDirect(BYTES_PER_INT * size)
                    .order(ByteOrder.nativeOrder())
                    .asIntBuffer()
        }
        indexBuffer.rewind()
        indexBuffer.limit(numIndices)

        // Note: when either dimension of the bounding box is smaller than 2*FADE_RADIUS_M we
        // generate a bunch of 0-area triangles. These don't get rendered though so it works
        // out ok.
        val xScale = Math.max((extentX - 2 * FADE_RADIUS_M) / extentX, 0.0f)
        val zScale = Math.max((extentZ - 2 * FADE_RADIUS_M) / extentZ, 0.0f)

        while (boundary.hasRemaining()) {
            val x = boundary.get()
            val z = boundary.get()
            vertexBuffer.put(x)
            vertexBuffer.put(z)
            vertexBuffer.put(0.0f)
            vertexBuffer.put(x * xScale)
            vertexBuffer.put(z * zScale)
            vertexBuffer.put(1.0f)
        }

        // step 1, perimeter
        indexBuffer.put(((boundaryVertices - 1) * 2))
        for (i in 0 until boundaryVertices) {
            indexBuffer.put((i * 2))
            indexBuffer.put((i * 2 + 1))
        }
        indexBuffer.put(1)
        // This leaves us on the interior edge of the perimeter between the inset vertices
        // for boundary verts n-1 and 0.

        // step 2, interior:
        for (i in 1 until boundaryVertices / 2) {
            indexBuffer.put(((boundaryVertices - 1 - i) * 2 + 1))
            indexBuffer.put((i * 2 + 1))
        }
        if (boundaryVertices % 2 != 0) {
            indexBuffer.put(((boundaryVertices / 2) * 2 + 1))
        }
    }

    /**
     * Draws the collection of tracked planes, with closer planes hiding more distant ones.
     *
     * @param allPlanes The collection of planes to draw.
     * @param cameraPose The pose of the camera.
     * @param cameraProjection The projection matrix.
     */
    fun drawPlanes(
        render: SampleRender,
        allPlanes: Collection<Plane>,
        cameraPose: Pose,
        cameraProjection: FloatArray,
    ) {
        val sortedPlanes = mutableListOf<SortablePlane>()

        for (plane in allPlanes) {
            if (
                plane.state.value.trackingState != TrackingState.TRACKING ||
                    plane.state.value.subsumedBy != null
            ) {
                continue
            }

            val distance = calculateDistanceToPlane(plane.state.value.centerPose, cameraPose)
            if (distance < 0) { // Plane is back-facing.
                continue
            }
            sortedPlanes.add(SortablePlane(distance, plane))
        }
        // Sort planes by distance from camera, farthest first.
        sortedPlanes.sortByDescending { it.distance }

        val viewMatrix = Matrix4.fromPose(cameraPose.inverse).data

        for (sortedPlane in sortedPlanes) {
            val plane = sortedPlane.plane
            // Create the matrix from the plane's pose.
            val planeMatrix = Matrix4.fromPose(plane.state.value.centerPose).data

            // Get transformed Y axis of plane's coordinate ssystem.
            val normalVector = plane.state.value.centerPose.rotation * Vector3.Up

            updatePlaneParameters(
                planeMatrix,
                plane.state.value.extents.width,
                plane.state.value.extents.height,
                plane.state.value.vertices.let { vertices ->
                    val buffer =
                        ByteBuffer.allocateDirect(vertices.size * 2 * 4)
                            .order(ByteOrder.nativeOrder())
                            .asFloatBuffer()
                    for (vertex in vertices) {
                        buffer.put(vertex.x)
                        buffer.put(vertex.y)
                    }
                    buffer.rewind()
                    buffer
                },
            )

            // Get plane index. Keep a map to assign same indices to same planes.
            val planeIndex = planeIndexMap.getOrPut(plane) { planeIndexMap.size }

            // Each plane will have its own angle offset from others, to make them easier to
            // distinguish. Compute a 2x2 rotation matrix from the angle.
            val angleRadians = planeIndex * 0.144f
            val uScale = DOTS_PER_METER
            val vScale = DOTS_PER_METER * EQUILATERAL_TRIANGLE_SCALE
            planeAngleUvMatrix[0] = +(Math.cos(angleRadians.toDouble()) * uScale).toFloat()
            planeAngleUvMatrix[1] = -(Math.sin(angleRadians.toDouble()) * vScale).toFloat()
            planeAngleUvMatrix[2] = +(Math.sin(angleRadians.toDouble()) * uScale).toFloat()
            planeAngleUvMatrix[3] = +(Math.cos(angleRadians.toDouble()) * vScale).toFloat()

            // Build the ModelView and ModelViewProjection matrices
            // for calculating cube position and light.
            Matrix.multiplyMM(modelViewMatrix, 0, viewMatrix, 0, modelMatrix, 0)
            Matrix.multiplyMM(modelViewProjectionMatrix, 0, cameraProjection, 0, modelViewMatrix, 0)

            // Populate the shader uniforms for this frame.
            shader.setMat4("u_Model", modelMatrix)
            shader.setMat4("u_ModelViewProjection", modelViewProjectionMatrix)
            shader.setMat2("u_PlaneUvMatrix", planeAngleUvMatrix)
            shader.setVec3(
                "u_Normal",
                FloatArray(3).apply {
                    this[0] = normalVector.x
                    this[1] = normalVector.y
                    this[2] = normalVector.z
                },
            )

            // Set the position of the plane
            vertexBufferObject.set(vertexBuffer)
            indexBufferObject.set(indexBuffer)
            render.draw(mesh, shader)
        }
    }

    private data class SortablePlane(val distance: Float, val plane: Plane)

    companion object {
        private const val TAG = "PlaneRenderer"

        // Shader names.
        private const val VERTEX_SHADER_NAME = "shaders/plane.vert"
        private const val FRAGMENT_SHADER_NAME = "shaders/plane.frag"
        private const val TEXTURE_NAME = "textures/trigrid.png"

        private const val BYTES_PER_FLOAT = Float.SIZE_BYTES
        private const val BYTES_PER_INT = Int.SIZE_BYTES
        private const val COORDS_PER_VERTEX = 3 // x, z, alpha

        private const val VERTS_PER_BOUNDARY_VERT = 2
        private const val INDICES_PER_BOUNDARY_VERT = 3
        private const val INITIAL_BUFFER_BOUNDARY_VERTS = 64

        private const val INITIAL_VERTEX_BUFFER_SIZE_BYTES =
            BYTES_PER_FLOAT *
                COORDS_PER_VERTEX *
                VERTS_PER_BOUNDARY_VERT *
                INITIAL_BUFFER_BOUNDARY_VERTS

        private const val INITIAL_INDEX_BUFFER_SIZE_BYTES =
            BYTES_PER_INT *
                INDICES_PER_BOUNDARY_VERT *
                INDICES_PER_BOUNDARY_VERT *
                INITIAL_BUFFER_BOUNDARY_VERTS

        private const val FADE_RADIUS_M = 0.25f
        private const val DOTS_PER_METER = 10.0f
        private const val EQUILATERAL_TRIANGLE_SCALE = 0.57735026f // 1 / sqrt(3)

        // Using the "signed distance field" approach to render sharp lines and circles.
        // {dotThreshold, lineThreshold, lineFadeSpeed, occlusionScale}
        // dotThreshold/lineThreshold: red/green intensity above which dots/lines are present
        // lineFadeShrink:  lines will fade in between alpha = 1-(1/lineFadeShrink) and 1.0
        // occlusionShrink: occluded planes will fade out between alpha = 0 and 1/occlusionShrink
        private val GRID_CONTROL = floatArrayOf(0.2f, 0.4f, 2.0f, 1.5f)

        // Calculate the normal distance to plane from cameraPose, the given planePose should have y
        // axis parallel to plane's normal, for example plane's center pose or hit test pose.
        fun calculateDistanceToPlane(planePose: Pose, cameraPose: Pose): Float {
            val cameraX = cameraPose.translation.x
            val cameraY = cameraPose.translation.y
            val cameraZ = cameraPose.translation.z
            // Get transformed Y axis of plane's coordinate system.
            val normal = planePose.rotation * Vector3.Up
            // Compute dot product of plane's normal with vector from camera to plane center.
            return (cameraX - planePose.translation.x) * normal.x +
                (cameraY - planePose.translation.y) * normal.y +
                (cameraZ - planePose.translation.z) * normal.z
        }
    }
}
