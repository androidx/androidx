/*
 * Copyright 2024 The Android Open Source Project
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
@file:Suppress(
    "BanConcurrentHashMap",
    "deprecation",
    "TYPEALIAS_EXPANSION_DEPRECATION",
    "RestrictedApiAndroidX",
)

package androidx.xr.arcore.testapp.helloar.rendering

import android.annotation.SuppressLint
import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.xr.arcore.AnchorCreateSuccess
import androidx.xr.arcore.Plane
import androidx.xr.arcore.PlaneLabel
import androidx.xr.arcore.TrackingState
import androidx.xr.runtime.Session
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Vector2
import androidx.xr.runtime.math.Vector4
import androidx.xr.scenecore.AlphaMode
import androidx.xr.scenecore.AnchorSpace
import androidx.xr.scenecore.CustomMesh
import androidx.xr.scenecore.KhronosUnlitMaterial
import androidx.xr.scenecore.Material
import androidx.xr.scenecore.MeshEntity
import androidx.xr.scenecore.MeshSubsetTopology
import androidx.xr.scenecore.Texture
import androidx.xr.scenecore.TextureSampler
import androidx.xr.scenecore.VertexAttribute
import androidx.xr.scenecore.VertexAttributeType
import androidx.xr.scenecore.VertexLayout
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.file.Paths
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Renders ARCore detected planes using SceneCore [MeshEntity] and [CustomMesh].
 *
 * Note on [androidx.xr.scenecore.Entity.dispose]: While [androidx.xr.scenecore.Entity.dispose] is
 * restricted and deprecated in favor of automatic JVM garbage collection (detaching with `parent =
 * null`), dynamic ARCore plane tracking replaces meshes and entities frequently as plane geometry
 * expands. Because [CustomMesh] is cleaned up via [BindingsResourceManager] and [Entity] via
 * [ReferenceCleaner], relying solely on GC creates a race condition where the underlying
 * `imp::Mesh` native resource is destroyed before the Impress node has released its borrow,
 * triggering a fatal abort (`OwnedPtr released with outstanding borrowed objects`, b/510404486).
 * Explicitly calling `dispose()` synchronously destroys the native subspace node and clears the
 * borrow prior to GC.
 */
@SuppressLint("RestrictedApiAndroidX")
internal class PlaneRenderer(val session: Session) : DefaultLifecycleObserver {

    private val _materialsMap = ConcurrentHashMap<PlaneLabel, KhronosUnlitMaterial>()
    private var _defaultMaterial: KhronosUnlitMaterial? = null
    private var _texture: Texture? = null
    private val _renderedPlanes: MutableStateFlow<List<PlaneModel>> =
        MutableStateFlow(mutableListOf())
    internal val renderedPlanes: StateFlow<Collection<PlaneModel>> = _renderedPlanes.asStateFlow()
    private lateinit var renderScope: CoroutineScope

    override fun onResume(owner: LifecycleOwner) {
        renderScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
        renderScope.launch {
            preloadMeshAndMaterials()
            Plane.subscribe(session).collect { updatePlaneModels(it) }
        }
    }

    override fun onPause(owner: LifecycleOwner) {
        renderScope.cancel()
        clearPlaneModels()
    }

    private suspend fun preloadMeshAndMaterials() {
        if (_materialsMap.isNotEmpty()) return

        val texture = Texture.create(session, Paths.get(TEXTURE_NAME))
        _texture = texture
        val sampler = TextureSampler()

        for ((label, color) in PLANE_COLORS) {
            val material = KhronosUnlitMaterial.create(session, AlphaMode.BLEND)
            material.setBaseColorTexture(texture, sampler)
            material.setBaseColorFactor(color)
            _materialsMap[label] = material
        }
        val defaultMaterial = KhronosUnlitMaterial.create(session, AlphaMode.BLEND)
        defaultMaterial.setBaseColorTexture(texture, sampler)
        defaultMaterial.setBaseColorFactor(DEFAULT_PLANE_COLOR)
        _defaultMaterial = defaultMaterial
    }

    private fun putVertex(
        buffer: ByteBuffer,
        x: Float,
        y: Float,
        z: Float,
        u: Float,
        v: Float,
    ) {
        buffer.putFloat(x)
        buffer.putFloat(y)
        buffer.putFloat(z)
        buffer.putFloat(u)
        buffer.putFloat(v)
        buffer.put(255.toByte())
        buffer.put(255.toByte())
        buffer.put(255.toByte())
        buffer.put(255.toByte())
    }

    /**
     * Creates a vertex buffer for a triangle fan mesh.
     *
     * The buffer contains N + 1 vertices, where N is the number of boundary vertices:
     * - Index 0: Center vertex at (0, 0, 0) with UV (0, 0).
     * - Indices 1..N: Perimeter vertices with matching (x, 0, z) coordinates and UVs scaled to
     *   world coordinates.
     *
     * Note: KhronosUnlitMaterial uses an unlit shader where normals are not evaluated. A single set
     * of vertices is shared by both top and bottom faces of the double-sided index buffer.
     */
    private fun createVertexBuffer(polygonVertices: List<Vector2>): ByteBuffer {
        val numBoundary = polygonVertices.size
        val vertexCount = numBoundary + 1
        val vertexBuffer =
            ByteBuffer.allocateDirect(vertexCount * VERTEX_STRIDE_BYTES)
                .order(ByteOrder.nativeOrder())

        // Center vertex (index 0)
        putVertex(vertexBuffer, 0f, 0f, 0f, 0f, 0f)
        // Perimeter vertices (indices 1..numBoundary)
        for (p in polygonVertices) {
            val u = p.x * DOTS_PER_METER
            val v = p.y * DOTS_PER_METER * EQUILATERAL_TRIANGLE_SCALE
            putVertex(vertexBuffer, p.x, 0f, p.y, u, v)
        }

        vertexBuffer.rewind()
        return vertexBuffer
    }

    /**
     * Creates a 32-bit index buffer defining a double-sided triangle fan.
     *
     * Generates 2 * N triangles (6 * N indices) connecting the center vertex to the perimeter:
     * - Top face: Counter-clockwise winding (0, current, next) viewed from above.
     * - Bottom face: Clockwise winding (0, next, current) viewed from above to face downward.
     *
     * Both faces share the same N + 1 vertices with opposite winding orders to ensure visibility
     * from both sides under backface culling.
     *
     * @param numBoundary The number of boundary vertices (N).
     */
    private fun createIndexBuffer(numBoundary: Int): ByteBuffer {
        val indexCount = numBoundary * 6
        val indexBuffer = ByteBuffer.allocateDirect(indexCount * 4).order(ByteOrder.nativeOrder())
        val intBuffer = indexBuffer.asIntBuffer()

        for (i in 0 until numBoundary) {
            val curr = 1 + i
            val next = 1 + ((i + 1) % numBoundary)
            // Top face: Counter-clockwise winding
            intBuffer.put(0)
            intBuffer.put(curr)
            intBuffer.put(next)

            // Bottom face: Clockwise winding (faces downward)
            intBuffer.put(0)
            intBuffer.put(next)
            intBuffer.put(curr)
        }

        indexBuffer.rewind()
        return indexBuffer
    }

    private fun createPlaneMesh(
        session: Session,
        polygonVertices: List<Vector2>,
    ): CustomMesh? {
        if (polygonVertices.size < 3) return null

        val vertexBuffer = createVertexBuffer(polygonVertices)
        val indexBuffer = createIndexBuffer(polygonVertices.size)

        return CustomMesh.BuilderFromMeshData(session, VERTEX_LAYOUT)
            .addVertexData(vertexBuffer)
            .setIndexData(indexBuffer)
            .setTopology(MeshSubsetTopology.TRIANGLES)
            .build()
    }

    private suspend fun updatePlaneModels(planes: Collection<Plane>) {
        val planesToRender = _renderedPlanes.value.toMutableList()
        // Create renderers for new planes.
        for (plane in planes) {
            if (_renderedPlanes.value.none { it.id == plane.hashCode() }) {
                addPlaneModel(plane, planesToRender)
            }
        }
        // Stop rendering dropped planes.
        for (renderedPlane in _renderedPlanes.value) {
            if (planes.none { it.hashCode() == renderedPlane.id }) {
                removePlaneModel(renderedPlane, planesToRender)
            }
        }
        // Emit to notify collectors that collection has been updated.
        _renderedPlanes.value = planesToRender
    }

    private suspend fun addPlaneModel(plane: Plane, planesToRender: MutableList<PlaneModel>) {
        val initialState = plane.state.value
        val initialMesh = createPlaneMesh(session, initialState.vertices) ?: return

        val anchorResult = plane.createAnchor(Pose.Identity)
        if (anchorResult !is AnchorCreateSuccess) {
            Log.w(TAG, "Failed to create plane anchor for plane ${plane.hashCode()}: $anchorResult")
            return
        }
        val anchor = anchorResult.anchor
        val anchorSpace = AnchorSpace.create(session, anchor)

        val label = initialState.label
        val material = getMaterialForLabel(label)
        var modelEntity =
            MeshEntity.create(
                session = session,
                mesh = initialMesh,
                materials = listOf(material),
                parent = anchorSpace,
            )
        modelEntity.setPose(Pose.Identity)

        val isTracking =
            initialState.trackingState == TrackingState.TRACKING && initialState.subsumedBy == null
        modelEntity.setEnabled(isTracking)
        if (isTracking) {
            modelEntity.setAlpha(1.0f)
        }

        var currentLabel = label
        var lastVertices = initialState.vertices
        val planeModel =
            PlaneModel(
                id = plane.hashCode(),
                planeType = plane.type,
                stateFlow = plane.state,
                modelEntity = modelEntity,
                renderJob = null,
                anchor = anchor,
                anchorSpace = anchorSpace,
            )

        val renderJob = renderScope.launch {
            plane.state.collect { state ->
                val tracking =
                    state.trackingState == TrackingState.TRACKING && state.subsumedBy == null
                modelEntity.setEnabled(tracking)
                if (tracking) {
                    modelEntity.setAlpha(1.0f)
                    if (state.label != currentLabel) {
                        currentLabel = state.label
                        modelEntity.setMaterial(getMaterialForLabel(currentLabel), 0)
                    }

                    if (state.vertices != lastVertices) {
                        val newMesh = createPlaneMesh(session, state.vertices)
                        if (newMesh != null) {
                            lastVertices = state.vertices
                            val newModelEntity =
                                MeshEntity.create(
                                    session = session,
                                    mesh = newMesh,
                                    materials = listOf(getMaterialForLabel(currentLabel)),
                                    parent = anchorSpace,
                                )
                            newModelEntity.setPose(Pose.Identity)
                            newModelEntity.setEnabled(tracking)
                            newModelEntity.setAlpha(1.0f)
                            val oldModelEntity = modelEntity
                            oldModelEntity.parent = null
                            // Explicitly dispose old MeshEntity to synchronously release the
                            // native Impress borrow on the underlying imp::Mesh before GC.
                            oldModelEntity.dispose()

                            modelEntity = newModelEntity
                            planeModel.modelEntity = newModelEntity
                        }
                    }
                }
            }
        }
        planeModel.renderJob = renderJob

        planesToRender.add(planeModel)
    }

    private fun getMaterialForLabel(label: PlaneLabel): Material {
        return _materialsMap[label] ?: _defaultMaterial!!
    }

    private fun removePlaneModel(planeModel: PlaneModel, planesToRender: MutableList<PlaneModel>) {
        planeModel.renderJob?.cancel()
        planeModel.modelEntity.parent = null
        planeModel.modelEntity.dispose()
        planeModel.anchorSpace.dispose()
        planeModel.anchor.detach()
        planesToRender.remove(planeModel)
    }

    private fun clearPlaneModels() {
        for (planeModel in _renderedPlanes.value) {
            planeModel.renderJob?.cancel()
            planeModel.modelEntity.parent = null
            planeModel.modelEntity.dispose()
            planeModel.anchorSpace.dispose()
            planeModel.anchor.detach()
        }
        _renderedPlanes.value = emptyList()
    }

    private companion object {
        private const val TAG = "PlaneRenderer"
        private const val TEXTURE_NAME = "textures/trigrid_mask.png"
        private const val VERTEX_STRIDE_BYTES = 24 // 3*4 (pos) + 2*4 (uv) + 4*1 (color)
        private val VERTEX_LAYOUT =
            VertexLayout.Builder()
                .addAttribute(VertexAttribute.POSITION, VertexAttributeType.FLOAT3)
                .addAttribute(VertexAttribute.UV0, VertexAttributeType.FLOAT2)
                .addAttribute(VertexAttribute.COLOR, VertexAttributeType.UBYTE4_NORM)
                .build()
        private const val DOTS_PER_METER = 10.0f
        private const val EQUILATERAL_TRIANGLE_SCALE = 0.57735026f // 1 / sqrt(3)
        private val DEFAULT_PLANE_COLOR = Vector4(1f, 0.2f, 0.2f, 0.7f)
        private val PLANE_COLORS =
            mapOf(
                PlaneLabel.WALL to Vector4(0f, 1f, 0f, .7f),
                PlaneLabel.FLOOR to Vector4(0f, 0f, 1f, .7f),
                PlaneLabel.CEILING to Vector4(1f, 1f, 0f, .7f),
                PlaneLabel.TABLE to Vector4(1f, 0f, 1f, .7f),
                PlaneLabel.UNKNOWN to Vector4(1f, 0.2f, 0.2f, .7f),
            )
    }
}
