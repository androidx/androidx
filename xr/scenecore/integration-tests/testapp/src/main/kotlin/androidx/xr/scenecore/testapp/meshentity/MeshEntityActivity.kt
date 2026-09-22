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

package androidx.xr.scenecore.testapp.meshentity

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import androidx.xr.runtime.Session
import androidx.xr.runtime.math.BoundingBox
import androidx.xr.runtime.math.FloatSize2d
import androidx.xr.runtime.math.FloatSize3d
import androidx.xr.runtime.math.IntSize2d
import androidx.xr.runtime.math.Matrix4
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Quaternion
import androidx.xr.runtime.math.Vector3
import androidx.xr.runtime.math.Vector4
import androidx.xr.scenecore.AlphaMode
import androidx.xr.scenecore.ByteBufferRegion
import androidx.xr.scenecore.CustomMesh
import androidx.xr.scenecore.InputEvent
import androidx.xr.scenecore.InteractableComponent
import androidx.xr.scenecore.KhronosPbrMaterial
import androidx.xr.scenecore.KhronosUnlitMaterial
import androidx.xr.scenecore.Material
import androidx.xr.scenecore.MeshBuffer
import androidx.xr.scenecore.MeshEntity
import androidx.xr.scenecore.MeshSubsetTopology
import androidx.xr.scenecore.MovableComponent
import androidx.xr.scenecore.PanelEntity
import androidx.xr.scenecore.VertexAttribute
import androidx.xr.scenecore.VertexAttributeType
import androidx.xr.scenecore.VertexLayout
import androidx.xr.scenecore.scene
import androidx.xr.scenecore.testapp.R
import androidx.xr.scenecore.testapp.common.managers.SessionManager
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.materialswitch.MaterialSwitch
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlinx.coroutines.android.awaitFrame
import kotlinx.coroutines.launch

@SuppressLint("RestrictedApi", "RestrictedApiAndroidX")
class MeshEntityActivity : AppCompatActivity() {
    private var session: Session? = null
    private var material: KhronosPbrMaterial? = null
    private var material2: KhronosUnlitMaterial? = null
    private var cubeEntity: MeshEntity? = null
    private var twoSubsetsEntity: MeshEntity? = null
    private var sharedBufferBottomEntity: MeshEntity? = null
    private var sharedBufferTopEntity: MeshEntity? = null
    private var triangleStripEntity: MeshEntity? = null
    private var twoMaterialsEntity: MeshEntity? = null
    private var wigglingStickEntity: MeshEntity? = null
    private var customStridesEntity: MeshEntity? = null
    private var dynamicVertexEntity: MeshEntity? = null
    private var dynamicColorEntity: MeshEntity? = null
    private var dynamicIndexEntity: MeshEntity? = null

    private data class EntityComponents(
        val movable: MovableComponent,
        val interactable: InteractableComponent,
    )

    private val meshEntitiesAndComponents = mutableMapOf<MeshEntity, EntityComponents>()
    private val initialPoses = mutableMapOf<MeshEntity, Pose>()
    private var movableSwitch: MaterialSwitch? = null
    private var interactableSwitch: MaterialSwitch? = null

    companion object {
        /**
         * Vertices per cube: 6 faces x 4 corners, not shared so normals and colors stay per-face.
         */
        private const val CUBE_VERTEX_COUNT = 24

        /** Triangles per cube: 6 faces x 2. */
        private const val CUBE_TRIANGLE_COUNT = 12

        /** Indices per cube, at 3 corners per triangle. */
        private const val CUBE_INDEX_COUNT = CUBE_TRIANGLE_COUNT * 3

        /** Index buffers are always 32-bit. */
        private const val BYTES_PER_INDEX = 4
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // The content view must be installed before the Session is created. Creating a Session
        // registers this Activity's window as an XR "window leash", and the platform reads
        // Window.peekDecorView() without a null check when a compositor transform update
        // arrives, which crashes the process if no content view has been set yet.
        // See b/562983987.
        setContentView(R.layout.activity_mesh_entity)

        lifecycleScope.launch {
            session = SessionManager(this@MeshEntityActivity).createSession()
            if (session == null) {
                finish()
                return@launch
            }

            session!!.scene.mainPanelEntity.size = FloatSize2d(0.4f, 0.3f)
            val movableComponent = MovableComponent.createSystemMovable(session!!)
            movableComponent.size = FloatSize3d(0.4f, 0.3f, 0.1f)
            session!!.scene.mainPanelEntity.addComponent(movableComponent)

            movableSwitch = findViewById<MaterialSwitch>(R.id.movableSwitch)
            movableSwitch?.setOnCheckedChangeListener { _, isChecked ->
                meshEntitiesAndComponents.forEach { (entity, components) ->
                    if (isChecked) {
                        entity.addComponent(components.movable)
                    } else {
                        entity.removeComponent(components.movable)
                    }
                }
            }

            interactableSwitch = findViewById<MaterialSwitch>(R.id.interactableSwitch)
            interactableSwitch?.setOnCheckedChangeListener { _, isChecked ->
                meshEntitiesAndComponents.forEach { (entity, components) ->
                    if (isChecked) {
                        entity.addComponent(components.interactable)
                    } else {
                        entity.removeComponent(components.interactable)
                    }
                }
            }

            findViewById<android.widget.Button>(R.id.resetPosesButton)?.setOnClickListener {
                initialPoses.forEach { (entity, pose) -> entity.setPose(pose) }
            }

            createMeshEntities()

            val toolbar = findViewById<Toolbar>(R.id.toolbar)
            toolbar.setNavigationOnClickListener { this@MeshEntityActivity.finish() }

            findViewById<FloatingActionButton>(R.id.bottomEndFab).setOnClickListener {
                ActivityCompat.recreate(this@MeshEntityActivity)
            }
        }
    }

    private val colorSchemes =
        arrayOf(
            arrayOf(
                intArrayOf(255, 0, 0), // Front (Red)
                intArrayOf(0, 255, 0), // Back (Green)
                intArrayOf(0, 0, 255), // Top (Blue)
                intArrayOf(255, 255, 0), // Bottom (Yellow)
                intArrayOf(255, 0, 255), // Right (Magenta)
                intArrayOf(0, 255, 255), // Left (Cyan)
            ),
            arrayOf(
                intArrayOf(255, 128, 0), // Front (Orange)
                intArrayOf(0, 128, 128), // Back (Teal)
                intArrayOf(128, 0, 128), // Top (Purple)
                intArrayOf(255, 128, 128), // Bottom (Pink)
                intArrayOf(128, 255, 128), // Right (Light green)
                intArrayOf(0, 100, 0), // Left (Dark green)
            ),
        )

    private inline fun generateCubeVertices(
        centerX: Float,
        centerY: Float,
        centerZ: Float,
        size: Float,
        colorSchemeIndex: Int = 0,
        putVertex:
            (
                x: Float,
                y: Float,
                z: Float,
                nx: Float,
                ny: Float,
                nz: Float,
                r: Int,
                g: Int,
                b: Int,
                a: Int,
            ) -> Unit,
    ) {
        val h = size / 2.0f
        val minX = centerX - h
        val maxX = centerX + h
        val minY = centerY - h
        val maxY = centerY + h
        val minZ = centerZ - h
        val maxZ = centerZ + h

        val colors = colorSchemes[colorSchemeIndex]
        val cFront = colors[0]
        val cBack = colors[1]
        val cTop = colors[2]
        val cBottom = colors[3]
        val cRight = colors[4]
        val cLeft = colors[5]

        // Front face
        putVertex(minX, minY, maxZ, 0.0f, 0.0f, 1.0f, cFront[0], cFront[1], cFront[2], 255)
        putVertex(maxX, minY, maxZ, 0.0f, 0.0f, 1.0f, cFront[0], cFront[1], cFront[2], 255)
        putVertex(maxX, maxY, maxZ, 0.0f, 0.0f, 1.0f, cFront[0], cFront[1], cFront[2], 255)
        putVertex(minX, maxY, maxZ, 0.0f, 0.0f, 1.0f, cFront[0], cFront[1], cFront[2], 255)

        // Back face
        putVertex(minX, minY, minZ, 0.0f, 0.0f, -1.0f, cBack[0], cBack[1], cBack[2], 255)
        putVertex(minX, maxY, minZ, 0.0f, 0.0f, -1.0f, cBack[0], cBack[1], cBack[2], 255)
        putVertex(maxX, maxY, minZ, 0.0f, 0.0f, -1.0f, cBack[0], cBack[1], cBack[2], 255)
        putVertex(maxX, minY, minZ, 0.0f, 0.0f, -1.0f, cBack[0], cBack[1], cBack[2], 255)

        // Top face
        putVertex(minX, maxY, minZ, 0.0f, 1.0f, 0.0f, cTop[0], cTop[1], cTop[2], 255)
        putVertex(minX, maxY, maxZ, 0.0f, 1.0f, 0.0f, cTop[0], cTop[1], cTop[2], 255)
        putVertex(maxX, maxY, maxZ, 0.0f, 1.0f, 0.0f, cTop[0], cTop[1], cTop[2], 255)
        putVertex(maxX, maxY, minZ, 0.0f, 1.0f, 0.0f, cTop[0], cTop[1], cTop[2], 255)

        // Bottom face
        putVertex(minX, minY, minZ, 0.0f, -1.0f, 0.0f, cBottom[0], cBottom[1], cBottom[2], 255)
        putVertex(maxX, minY, minZ, 0.0f, -1.0f, 0.0f, cBottom[0], cBottom[1], cBottom[2], 255)
        putVertex(maxX, minY, maxZ, 0.0f, -1.0f, 0.0f, cBottom[0], cBottom[1], cBottom[2], 255)
        putVertex(minX, minY, maxZ, 0.0f, -1.0f, 0.0f, cBottom[0], cBottom[1], cBottom[2], 255)

        // Right face
        putVertex(maxX, minY, minZ, 1.0f, 0.0f, 0.0f, cRight[0], cRight[1], cRight[2], 255)
        putVertex(maxX, maxY, minZ, 1.0f, 0.0f, 0.0f, cRight[0], cRight[1], cRight[2], 255)
        putVertex(maxX, maxY, maxZ, 1.0f, 0.0f, 0.0f, cRight[0], cRight[1], cRight[2], 255)
        putVertex(maxX, minY, maxZ, 1.0f, 0.0f, 0.0f, cRight[0], cRight[1], cRight[2], 255)

        // Left face
        putVertex(minX, minY, minZ, -1.0f, 0.0f, 0.0f, cLeft[0], cLeft[1], cLeft[2], 255)
        putVertex(minX, minY, maxZ, -1.0f, 0.0f, 0.0f, cLeft[0], cLeft[1], cLeft[2], 255)
        putVertex(minX, maxY, maxZ, -1.0f, 0.0f, 0.0f, cLeft[0], cLeft[1], cLeft[2], 255)
        putVertex(minX, maxY, minZ, -1.0f, 0.0f, 0.0f, cLeft[0], cLeft[1], cLeft[2], 255)
    }

    private fun putCubeVertices(
        vertexBuffer: ByteBuffer,
        centerX: Float,
        centerY: Float,
        centerZ: Float,
        size: Float,
        colorSchemeIndex: Int = 0,
    ) {
        generateCubeVertices(centerX, centerY, centerZ, size, colorSchemeIndex) {
            x,
            y,
            z,
            nx,
            ny,
            nz,
            r,
            g,
            b,
            a ->
            vertexBuffer.putFloat(x)
            vertexBuffer.putFloat(y)
            vertexBuffer.putFloat(z)
            vertexBuffer.putFloat(nx)
            vertexBuffer.putFloat(ny)
            vertexBuffer.putFloat(nz)
            vertexBuffer.put(r.toByte())
            vertexBuffer.put(g.toByte())
            vertexBuffer.put(b.toByte())
            vertexBuffer.put(a.toByte())
        }
    }

    private fun putCubeVerticesWithCustomOffsetsAndStrides(
        buffer1: ByteBuffer,
        buffer2: ByteBuffer,
        centerX: Float,
        centerY: Float,
        centerZ: Float,
        size: Float,
        stride1: Int,
        stride2: Int,
        colorSchemeIndex: Int = 0,
    ) {
        var vIdx = 0
        generateCubeVertices(centerX, centerY, centerZ, size, colorSchemeIndex) {
            x,
            y,
            z,
            nx,
            ny,
            nz,
            r,
            g,
            b,
            a ->
            val base1 = buffer1.position() + vIdx * stride1
            buffer1.put(base1 + 4, r.toByte())
            buffer1.put(base1 + 5, g.toByte())
            buffer1.put(base1 + 6, b.toByte())
            buffer1.put(base1 + 7, a.toByte())
            buffer1.putFloat(base1 + 12, x)
            buffer1.putFloat(base1 + 16, y)
            buffer1.putFloat(base1 + 20, z)

            val base2 = buffer2.position() + vIdx * stride2
            buffer2.putFloat(base2 + 4, nx)
            buffer2.putFloat(base2 + 8, ny)
            buffer2.putFloat(base2 + 12, nz)

            vIdx++
        }
        buffer1.position(buffer1.position() + vIdx * stride1)
        buffer2.position(buffer2.position() + vIdx * stride2)
    }

    /** Writes cube positions and normals, morphed from a cube (0f) to a pyramid (1f). */
    private fun putMorphedCubeVertices(
        buffer: ByteBuffer,
        centerX: Float,
        centerY: Float,
        centerZ: Float,
        size: Float,
        morphAmount: Float,
    ) {
        generateCubeVertices(centerX, centerY, centerZ, size) { x, y, z, nx, ny, nz, _, _, _, _ ->
            val isTop = y > centerY
            val factor = if (isTop) 1f - morphAmount else 1f
            val dx = x - centerX
            val dz = z - centerZ

            val fx = centerX + dx * factor
            val fy = y
            val fz = centerZ + dz * factor

            var fnx = nx
            var fny = ny
            var fnz = nz

            if (ny in -0.5f..0.5f) {
                // Side faces lean up towards the pyramid peak.
                fnx = 2f * nx
                fny = morphAmount
                fnz = 2f * nz
                val flen = kotlin.math.sqrt(fnx * fnx + fny * fny + fnz * fnz).coerceAtLeast(1e-6f)
                fnx /= flen
                fny /= flen
                fnz /= flen
            }

            buffer.putFloat(fx)
            buffer.putFloat(fy)
            buffer.putFloat(fz)
            buffer.putFloat(fnx)
            buffer.putFloat(fny)
            buffer.putFloat(fnz)
        }
    }

    /** Writes per-vertex colors that cycle through the color wheel as [time] advances. */
    private fun putAnimatedCubeColors(buffer: ByteBuffer, time: Float) {
        for (i in 0 until CUBE_VERTEX_COUNT) {
            val r = ((kotlin.math.sin(time + i * 0.1f) + 1f) / 2f * 255).toInt().toByte()
            val g = ((kotlin.math.sin(time * 1.3f + i * 0.2f) + 1f) / 2f * 255).toInt().toByte()
            val b = ((kotlin.math.sin(time * 0.7f + i * 0.3f) + 1f) / 2f * 255).toInt().toByte()
            buffer.put(r)
            buffer.put(g)
            buffer.put(b)
            buffer.put(255.toByte())
        }
    }

    /** Writes the per-face colors of [colorSchemeIndex]; the geometry args are unused. */
    private fun putCubeColors(buffer: ByteBuffer, colorSchemeIndex: Int) {
        generateCubeVertices(0f, 0f, 0f, 0.3f, colorSchemeIndex) { _, _, _, _, _, _, r, g, b, a ->
            buffer.put(r.toByte())
            buffer.put(g.toByte())
            buffer.put(b.toByte())
            buffer.put(a.toByte())
        }
    }

    private fun putCubeIndices(indexBuffer: ByteBuffer, vertexOffset: Int) {
        val intBuffer = indexBuffer.asIntBuffer()
        fun putIndices(vararg indices: Int) {
            for (i in indices) {
                intBuffer.put(i + vertexOffset)
            }
        }
        putIndices(0, 1, 2, 0, 2, 3)
        putIndices(4, 5, 6, 4, 6, 7)
        putIndices(8, 9, 10, 8, 10, 11)
        putIndices(12, 13, 14, 12, 14, 15)
        putIndices(16, 17, 18, 16, 18, 19)
        putIndices(20, 21, 22, 20, 22, 23)
        indexBuffer.position(indexBuffer.position() + 36 * 4)
    }

    private fun putCubeIndicesStrip(indexBuffer: ByteBuffer, vertexOffset: Int) {
        val intBuffer = indexBuffer.asIntBuffer()
        fun putIndices(vararg indices: Int) {
            for (i in indices) {
                intBuffer.put(i + vertexOffset)
            }
        }
        // Strip 1: Left, Front, Right, Back
        putIndices(23, 20, 22, 21)
        putIndices(3, 0, 2, 1)
        putIndices(18, 19, 17, 16)
        putIndices(6, 7, 5, 4)
        // Connection 1
        putIndices(4, 15)
        // Strip 2: Bottom
        putIndices(15, 12, 14, 13)
        // Connection 2
        putIndices(13, 8)
        // Strip 3: Top
        putIndices(8, 9, 11, 10)
        indexBuffer.position(indexBuffer.position() + 28 * 4)
    }

    private fun createPanel(
        session: Session,
        text: String,
        pose: Pose,
        entities: List<MeshEntity> = emptyList(),
        extraContent: @Composable () -> Unit = {},
    ) {
        val inputCountState = mutableIntStateOf(0)

        entities.forEach { entity ->
            val movableComponent = MovableComponent.createSystemMovable(session, scaleInZ = false)
            val interactableComponent =
                InteractableComponent.create(session) {
                    if (it.action == InputEvent.Action.UP) {
                        inputCountState.intValue++
                    }
                }
            meshEntitiesAndComponents[entity] =
                EntityComponents(movableComponent, interactableComponent)
            if (movableSwitch?.isChecked == true) {
                entity.addComponent(movableComponent)
            }
            if (interactableSwitch?.isChecked == true) {
                entity.addComponent(interactableComponent)
            }
        }

        val composeView =
            ComposeView(this).apply {
                setViewTreeLifecycleOwner(this@MeshEntityActivity)
                setViewTreeViewModelStoreOwner(this@MeshEntityActivity)
                setViewTreeSavedStateRegistryOwner(this@MeshEntityActivity)
                setContent {
                    Surface(color = Color.White, modifier = Modifier.fillMaxSize()) {
                        Column(
                            modifier = Modifier.fillMaxSize().padding(16.dp),
                            verticalArrangement =
                                Arrangement.spacedBy(32.dp, Alignment.CenterVertically),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(
                                text = text,
                                color = Color.Black,
                                fontSize = 48.sp,
                                textAlign = TextAlign.Center,
                            )
                            if (inputCountState.intValue > 0) {
                                Text(
                                    "Input Count: ${inputCountState.intValue}",
                                    fontSize = 48.sp,
                                    color = Color.Black,
                                )
                            }
                            extraContent()
                        }
                    }
                }
            }
        val panel =
            PanelEntity.create(
                session,
                view = composeView,
                pixelDimensions = IntSize2d(1600, 800),
                name = "Panel_$text",
                pose = pose,
                parent = session.scene.activitySpace,
            )
        panel.size = FloatSize2d(0.8f, 0.4f)
    }

    private fun createMeshEntity(
        session: Session,
        mesh: CustomMesh,
        materials: List<Material>,
        pose: Pose,
        boneCount: Int = 0,
        parent: androidx.xr.scenecore.Entity? = session.scene.activitySpace,
    ): MeshEntity {
        val entity = MeshEntity.create(session, mesh, materials, boneCount, pose, parent)
        initialPoses[entity] = pose
        return entity
    }

    private fun createMeshEntities() {
        lifecycleScope.launch {
            val currentSession = session ?: return@launch

            // Define vertex layout
            val vertexLayout =
                VertexLayout.Builder()
                    .addAttribute(VertexAttribute.POSITION, VertexAttributeType.FLOAT3)
                    .addAttribute(VertexAttribute.NORMAL, VertexAttributeType.FLOAT3)
                    .addAttribute(VertexAttribute.COLOR, VertexAttributeType.UBYTE4_NORM)
                    .build()

            val stride = 28

            material = KhronosPbrMaterial.create(currentSession, AlphaMode.OPAQUE)
            material?.setMetallicFactor(0f)
            material?.setRoughnessFactor(1f)

            material2 = KhronosUnlitMaterial.create(currentSession, AlphaMode.OPAQUE)
            material2?.setBaseColorFactor(Vector4(1f, 1f, 1f, 1f))

            createTest1_Cube(currentSession, vertexLayout, stride)
            createTest2_TwoSubsets(currentSession, vertexLayout, stride)
            createTest3_SharedBuffer(currentSession, vertexLayout, stride)
            createTest4_TriangleStrip(currentSession, vertexLayout, stride)
            createTest5_TwoMaterials(currentSession, vertexLayout, stride)
            createTest6_WigglingStick(currentSession)
            createTest7_CustomStridesAndOffsets(currentSession)
            createTest8_DynamicMeshUpdates(currentSession)
        }
    }

    private fun createTest7_CustomStridesAndOffsets(currentSession: Session) {
        val stride1 = 32
        val stride2 = 24
        val vertexLayout =
            VertexLayout.Builder()
                .addAttribute(VertexAttribute.POSITION, VertexAttributeType.FLOAT3, offset = 12)
                .addAttribute(VertexAttribute.COLOR, VertexAttributeType.UBYTE4_NORM, offset = 4)
                .setStride(stride1)
                .startNextBuffer()
                .addAttribute(VertexAttribute.NORMAL, VertexAttributeType.FLOAT3, offset = 4)
                .setStride(stride2)
                .build()

        val vertexCount = 24
        val vertexBuffer1 =
            ByteBuffer.allocateDirect(vertexCount * stride1).order(ByteOrder.nativeOrder())
        val vertexBuffer2 =
            ByteBuffer.allocateDirect(vertexCount * stride2).order(ByteOrder.nativeOrder())

        putCubeVerticesWithCustomOffsetsAndStrides(
            vertexBuffer1,
            vertexBuffer2,
            0f,
            0f,
            0f,
            0.3f,
            stride1,
            stride2,
        )

        val indexSize = 36 * 4
        val indexBuffer = ByteBuffer.allocateDirect(indexSize).order(ByteOrder.nativeOrder())
        putCubeIndices(indexBuffer, 0)

        val cubeMesh =
            CustomMesh.BuilderFromMeshData(currentSession, vertexLayout)
                .addVertexData(vertexBuffer1)
                .addVertexData(vertexBuffer2)
                .setIndexData(indexBuffer)
                .setTopology(MeshSubsetTopology.TRIANGLES)
                .build()

        customStridesEntity =
            createMeshEntity(
                currentSession,
                cubeMesh,
                listOf(material!!),
                Pose(Vector3(-3f, 0f, -1.5f)),
            )
        createPanel(
            currentSession,
            "Custom Strides & Offsets: A cube with interleaved vertex data.\nBox: " +
                "[${cubeMesh.bounds.min.x},${cubeMesh.bounds.min.y},${cubeMesh.bounds.min.z}] - " +
                "[${cubeMesh.bounds.max.x},${cubeMesh.bounds.max.y},${cubeMesh.bounds.max.z}]",
            Pose(Vector3(-3f, 0.7f, -1.5f)),
            listOfNotNull(customStridesEntity),
        )
    }

    private fun createTest1_Cube(currentSession: Session, vertexLayout: VertexLayout, stride: Int) {
        val vertexCount = 24
        val vertexSize = vertexCount * stride
        val indexSize = 36 * 4

        val sharedBuffer =
            ByteBuffer.allocateDirect(vertexSize + indexSize).order(ByteOrder.nativeOrder())

        putCubeVertices(sharedBuffer, 0f, 0f, 0f, 0.3f)
        putCubeIndices(sharedBuffer, 0)

        val cubeMesh =
            CustomMesh.BuilderFromMeshData(currentSession, vertexLayout)
                .addVertexData(sharedBuffer, 0, vertexSize)
                .setIndexData(sharedBuffer, vertexSize, indexSize)
                .setTopology(MeshSubsetTopology.TRIANGLES)
                .build()
        cubeEntity =
            createMeshEntity(
                currentSession,
                cubeMesh,
                listOf(material!!),
                Pose(Vector3(-2f, 0f, -1.5f)),
                parent = null,
            )
        cubeEntity?.parent = currentSession.scene.activitySpace
        cubeEntity?.setEnabled(true)

        createPanel(
            currentSession,
            "A cube with six different colored faces.\nBox: " +
                "[${cubeMesh.bounds.min.x},${cubeMesh.bounds.min.y},${cubeMesh.bounds.min.z}] - " +
                "[${cubeMesh.bounds.max.x},${cubeMesh.bounds.max.y},${cubeMesh.bounds.max.z}]",
            Pose(Vector3(-2f, 0.7f, -1.5f)),
            listOfNotNull(cubeEntity),
        )
    }

    private fun createTest2_TwoSubsets(
        currentSession: Session,
        vertexLayout: VertexLayout,
        stride: Int,
    ) {
        val vertexCount = 48
        val vertexBuffer =
            ByteBuffer.allocateDirect(vertexCount * stride).order(ByteOrder.nativeOrder())
        putCubeVertices(vertexBuffer, 0f, -0.2f, 0f, 0.3f, colorSchemeIndex = 0)
        putCubeVertices(vertexBuffer, 0f, 0.2f, 0f, 0.3f, colorSchemeIndex = 1)

        val indexBuffer = ByteBuffer.allocateDirect(72 * 4).order(ByteOrder.nativeOrder())
        putCubeIndices(indexBuffer, 0)
        putCubeIndices(indexBuffer, 24)

        val twoSubsetsMesh =
            CustomMesh.BuilderFromMeshData(currentSession, vertexLayout)
                .addVertexData(vertexBuffer)
                .setIndexData(indexBuffer)
                .addSubset(MeshSubsetTopology.TRIANGLES, 0, 36)
                .addSubset(MeshSubsetTopology.TRIANGLES, 36, 36)
                .build()
        twoSubsetsEntity =
            createMeshEntity(
                currentSession,
                twoSubsetsMesh,
                listOf(material!!, material!!),
                Pose(Vector3(-1f, 0f, -1.5f)),
            )
        createPanel(
            currentSession,
            "Two Subsets: One mesh with two subsets rendered on top of each other.",
            Pose(Vector3(-1f, 0.7f, -1.5f)),
            listOfNotNull(twoSubsetsEntity),
        )
    }

    private fun createTest3_SharedBuffer(
        currentSession: Session,
        vertexLayout: VertexLayout,
        stride: Int,
    ) {
        val vertexCount = 48
        val vertexBuffer =
            ByteBuffer.allocateDirect(vertexCount * stride).order(ByteOrder.nativeOrder())
        putCubeVertices(vertexBuffer, 0f, -0.2f, 0f, 0.3f, colorSchemeIndex = 0)
        putCubeVertices(vertexBuffer, 0f, 0.2f, 0f, 0.3f, colorSchemeIndex = 1)

        val indexBuffer = ByteBuffer.allocateDirect(72 * 4).order(ByteOrder.nativeOrder())
        putCubeIndices(indexBuffer, 0)
        putCubeIndices(indexBuffer, 24)

        val meshBuffer =
            MeshBuffer.create(
                currentSession,
                vertexLayout,
                listOf(ByteBufferRegion(vertexBuffer, 0, vertexCount * stride)),
                ByteBufferRegion(indexBuffer, 0, 72 * 4),
            )

        val bottomCubeMesh =
            CustomMesh.BuilderFromMeshBuffer(currentSession, meshBuffer)
                .addSubset(MeshSubsetTopology.TRIANGLES, 0, 36)
                .setBounds(
                    BoundingBox.fromCenterAndHalfExtents(
                        Vector3(0f, -0.2f, 0f),
                        FloatSize3d(0.15f, 0.15f, 0.15f),
                    )
                )
                .build()
        val topCubeMesh =
            CustomMesh.BuilderFromMeshBuffer(currentSession, meshBuffer)
                .addSubset(MeshSubsetTopology.TRIANGLES, 36, 36)
                .setBounds(
                    BoundingBox.fromCenterAndHalfExtents(
                        Vector3(0f, 0.2f, 0f),
                        FloatSize3d(0.15f, 0.15f, 0.15f),
                    )
                )
                .build()
        sharedBufferBottomEntity =
            createMeshEntity(
                currentSession,
                bottomCubeMesh,
                listOf(material!!),
                Pose(Vector3(0f, 0f, -1.5f)),
            )
        sharedBufferTopEntity =
            createMeshEntity(
                currentSession,
                topCubeMesh,
                listOf(material!!),
                Pose(Vector3(0f, 0f, -1.5f)),
            )
        createPanel(
            currentSession,
            "Shared Buffer: Two meshes sharing the same buffer rendered on top of each other.",
            Pose(Vector3(0f, 0.7f, -1.5f)),
            listOfNotNull(sharedBufferBottomEntity, sharedBufferTopEntity),
        )
    }

    private fun createTest4_TriangleStrip(
        currentSession: Session,
        vertexLayout: VertexLayout,
        stride: Int,
    ) {
        val vertexCount = 24
        val vertexBuffer =
            ByteBuffer.allocateDirect(vertexCount * stride).order(ByteOrder.nativeOrder())
        putCubeVertices(vertexBuffer, 0f, 0f, 0f, 0.3f, colorSchemeIndex = 0)

        val stripIndexCount = 28
        val indexBuffer =
            ByteBuffer.allocateDirect(stripIndexCount * 4).order(ByteOrder.nativeOrder())
        putCubeIndicesStrip(indexBuffer, 0)

        val cubeMesh =
            CustomMesh.BuilderFromMeshData(currentSession, vertexLayout)
                .addVertexData(vertexBuffer)
                .setIndexData(indexBuffer)
                .setTopology(MeshSubsetTopology.TRIANGLE_STRIP)
                .build()
        triangleStripEntity =
            createMeshEntity(
                currentSession,
                cubeMesh,
                listOf(material!!),
                Pose(Vector3(1f, 0f, -1.5f)),
            )
        createPanel(
            currentSession,
            "Triangle Strip: A cube rendered with TRIANGLE_STRIP.",
            Pose(Vector3(1f, 0.7f, -1.5f)),
            listOfNotNull(triangleStripEntity),
        )
    }

    private fun createTest6_WigglingStick(currentSession: Session) {
        val vertexLayoutSkinned =
            VertexLayout.Builder()
                .addAttribute(VertexAttribute.POSITION, VertexAttributeType.FLOAT3)
                .addAttribute(VertexAttribute.NORMAL, VertexAttributeType.FLOAT3)
                .addAttribute(VertexAttribute.COLOR, VertexAttributeType.UBYTE4_NORM)
                .addAttribute(VertexAttribute.BONE_INDICES, VertexAttributeType.UBYTE4)
                .addAttribute(VertexAttribute.BONE_WEIGHTS, VertexAttributeType.UBYTE4_NORM)
                .build()

        val stride = 36
        val segments = 12
        val height = 0.9f
        val halfSize = 0.15f

        val vertexCount = segments * 4 * 4 + 8 // 4 sides per segment + top and bottom
        val indexCount = segments * 4 * 6 + 12

        val vertexBuffer =
            ByteBuffer.allocateDirect(vertexCount * stride).order(ByteOrder.nativeOrder())
        val indexBuffer = ByteBuffer.allocateDirect(indexCount * 4).order(ByteOrder.nativeOrder())

        fun getBoneInfo(y: Float): Pair<ByteArray, ByteArray> {
            val indices = ByteArray(4)
            val weights = ByteArray(4)
            if (y <= 0.15f) {
                indices[0] = 0
                weights[0] = 255.toByte()
            } else if (y <= 0.45f) {
                val t = (y - 0.15f) / 0.3f
                indices[0] = 0
                indices[1] = 1
                val w1 = (t * 255).toInt()
                val w0 = 255 - w1
                weights[0] = w0.toByte()
                weights[1] = w1.toByte()
            } else if (y <= 0.75f) {
                val t = (y - 0.45f) / 0.3f
                indices[0] = 1
                indices[1] = 2
                val w1 = (t * 255).toInt()
                val w0 = 255 - w1
                weights[0] = w0.toByte()
                weights[1] = w1.toByte()
            } else {
                indices[0] = 2
                weights[0] = 255.toByte()
            }
            return Pair(indices, weights)
        }

        fun putVertex(x: Float, y: Float, z: Float, nx: Float, ny: Float, nz: Float, c: IntArray) {
            vertexBuffer.putFloat(x)
            vertexBuffer.putFloat(y)
            vertexBuffer.putFloat(z)
            vertexBuffer.putFloat(nx)
            vertexBuffer.putFloat(ny)
            vertexBuffer.putFloat(nz)
            vertexBuffer.put(c[0].toByte())
            vertexBuffer.put(c[1].toByte())
            vertexBuffer.put(c[2].toByte())
            vertexBuffer.put(255.toByte())

            val (indices, weights) = getBoneInfo(y)
            vertexBuffer.put(indices)
            vertexBuffer.put(weights)
        }

        var vIdx = 0
        val intBuffer = indexBuffer.asIntBuffer()
        fun putQuad(
            x0: Float,
            y0: Float,
            z0: Float,
            x1: Float,
            y1: Float,
            z1: Float,
            x2: Float,
            y2: Float,
            z2: Float,
            x3: Float,
            y3: Float,
            z3: Float,
            nx: Float,
            ny: Float,
            nz: Float,
            c: IntArray,
        ) {
            putVertex(x0, y0, z0, nx, ny, nz, c)
            putVertex(x1, y1, z1, nx, ny, nz, c)
            putVertex(x2, y2, z2, nx, ny, nz, c)
            putVertex(x3, y3, z3, nx, ny, nz, c)
            intBuffer.put(vIdx + 0)
            intBuffer.put(vIdx + 1)
            intBuffer.put(vIdx + 2)
            intBuffer.put(vIdx + 0)
            intBuffer.put(vIdx + 2)
            intBuffer.put(vIdx + 3)
            vIdx += 4
        }

        val colors = colorSchemes[0]
        val cFront = colors[0]
        val cBack = colors[1]
        val cTop = colors[2]
        val cBottom = colors[3]
        val cRight = colors[4]
        val cLeft = colors[5]

        val h = halfSize

        for (i in 0 until segments) {
            val y0 = (i.toFloat() / segments) * height
            val y1 = ((i + 1).toFloat() / segments) * height

            // Front
            putQuad(-h, y0, h, h, y0, h, h, y1, h, -h, y1, h, 0f, 0f, 1f, cFront)
            // Back
            putQuad(h, y0, -h, -h, y0, -h, -h, y1, -h, h, y1, -h, 0f, 0f, -1f, cBack)
            // Right
            putQuad(h, y0, h, h, y0, -h, h, y1, -h, h, y1, h, 1f, 0f, 0f, cRight)
            // Left
            putQuad(-h, y0, -h, -h, y0, h, -h, y1, h, -h, y1, -h, -1f, 0f, 0f, cLeft)
        }

        // Top
        putQuad(-h, height, h, h, height, h, h, height, -h, -h, height, -h, 0f, 1f, 0f, cTop)
        // Bottom
        putQuad(-h, 0f, -h, h, 0f, -h, h, 0f, h, -h, 0f, h, 0f, -1f, 0f, cBottom)

        val meshBuffer =
            MeshBuffer.create(
                currentSession,
                vertexLayoutSkinned,
                listOf(ByteBufferRegion(vertexBuffer, 0, vertexCount * stride)),
                ByteBufferRegion(indexBuffer, 0, indexCount * 4),
            )

        val stickMesh =
            CustomMesh.BuilderFromMeshBuffer(currentSession, meshBuffer)
                .addSubset(MeshSubsetTopology.TRIANGLES, 0, indexCount)
                .setBounds(
                    BoundingBox.fromCenterAndHalfExtents(
                        Vector3(0f, height / 2f, 0f),
                        FloatSize3d(halfSize, height / 2f, halfSize),
                    )
                )
                .build()

        wigglingStickEntity =
            createMeshEntity(
                currentSession,
                stickMesh,
                listOf(material!!),
                Pose(Vector3(3f, -0.5f, -1.5f)),
                boneCount = 3,
            )

        createPanel(
            currentSession,
            "Wiggling Stick: Demonstrates dynamic bone transforms.",
            Pose(Vector3(3f, 0.7f, -1.5f)),
            listOfNotNull(wigglingStickEntity),
        )

        lifecycleScope.launch {
            var time = 0f
            val idScale = Vector3(1f, 1f, 1f)
            val idRot = Quaternion(0f, 0f, 0f, 1f)

            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                while (true) {
                    awaitFrame()
                    time += 0.05f

                    val m0 = Matrix4.Identity

                    val angle1 = kotlin.math.sin(time) * 0.5f
                    val q1 =
                        Quaternion(
                            0f,
                            0f,
                            kotlin.math.sin(angle1 / 2f),
                            kotlin.math.cos(angle1 / 2f),
                        )
                    val pivot1 = Vector3(0f, 0.3f, 0f)
                    val invPivot1 = Vector3(0f, -0.3f, 0f)
                    val m1 =
                        Matrix4.fromTrs(pivot1, idRot, idScale) *
                            Matrix4.fromTrs(Vector3(0f, 0f, 0f), q1, idScale) *
                            Matrix4.fromTrs(invPivot1, idRot, idScale)

                    val angle2 = kotlin.math.sin(time * 1.5f) * 0.8f
                    val q2 =
                        Quaternion(
                            0f,
                            0f,
                            kotlin.math.sin(angle2 / 2f),
                            kotlin.math.cos(angle2 / 2f),
                        )
                    val pivot2 = Vector3(0f, 0.6f, 0f)
                    val invPivot2 = Vector3(0f, -0.6f, 0f)
                    val m2 =
                        m1 *
                            Matrix4.fromTrs(pivot2, idRot, idScale) *
                            Matrix4.fromTrs(Vector3(0f, 0f, 0f), q2, idScale) *
                            Matrix4.fromTrs(invPivot2, idRot, idScale)

                    wigglingStickEntity?.setBoneTransforms(listOf(m0, m1, m2))
                }
            }
        }
    }

    private fun createTest5_TwoMaterials(
        currentSession: Session,
        vertexLayout: VertexLayout,
        stride: Int,
    ) {
        val vertexCount = 48
        val vertexBuffer =
            ByteBuffer.allocateDirect(vertexCount * stride).order(ByteOrder.nativeOrder())
        putCubeVertices(vertexBuffer, 0f, -0.2f, 0f, 0.3f, colorSchemeIndex = 0)
        putCubeVertices(vertexBuffer, 0f, 0.2f, 0f, 0.3f, colorSchemeIndex = 0)

        val indexBuffer = ByteBuffer.allocateDirect(72 * 4).order(ByteOrder.nativeOrder())
        putCubeIndices(indexBuffer, 0)
        putCubeIndices(indexBuffer, 24)

        val cubeMesh =
            CustomMesh.BuilderFromMeshData(currentSession, vertexLayout)
                .addVertexData(vertexBuffer)
                .setIndexData(indexBuffer)
                .addSubset(MeshSubsetTopology.TRIANGLES, 0, 36)
                .addSubset(MeshSubsetTopology.TRIANGLES, 36, 36)
                .build()
        twoMaterialsEntity =
            createMeshEntity(
                currentSession,
                cubeMesh,
                listOf(material!!, material2!!),
                Pose(Vector3(2f, 0f, -1.5f)),
            )
        createPanel(
            currentSession,
            "Two Materials: One mesh with two subsets using different materials.",
            Pose(Vector3(2f, 0.7f, -1.5f)),
            listOfNotNull(twoMaterialsEntity),
        ) {
            Button(
                onClick = {
                    twoMaterialsEntity?.let { entity ->
                        val materials = entity.materials
                        if (materials.size >= 2) {
                            val mat0 = materials[0]
                            val mat1 = materials[1]
                            entity.setMaterial(mat1, 0)
                            entity.setMaterial(mat0, 1)
                        }
                    }
                }
            ) {
                Text("Swap Materials", fontSize = 48.sp)
            }
        }
    }

    private fun createTest8_DynamicMeshUpdates(currentSession: Session) {
        val indexSize = CUBE_INDEX_COUNT * BYTES_PER_INDEX

        val posNormStride = 24
        val colorStride = 4
        val posNormSize = CUBE_VERTEX_COUNT * posNormStride
        val colorSize = CUBE_VERTEX_COUNT * colorStride
        val dynamicLayout =
            VertexLayout.Builder()
                .addAttribute(VertexAttribute.POSITION, VertexAttributeType.FLOAT3)
                .addAttribute(VertexAttribute.NORMAL, VertexAttributeType.FLOAT3)
                .setStride(posNormStride)
                .startNextBuffer()
                .addAttribute(VertexAttribute.COLOR, VertexAttributeType.UBYTE4_NORM)
                .setStride(colorStride)
                .build()

        val indexBuffer = ByteBuffer.allocateDirect(indexSize).order(ByteOrder.nativeOrder())
        putCubeIndices(indexBuffer, 0)

        // --- 1. Morph Cube (Updates Buffer 0, Color constant in Buffer 1) ---
        val vertexBufferMorphPosNorm =
            ByteBuffer.allocateDirect(posNormSize).order(ByteOrder.nativeOrder())
        putMorphedCubeVertices(vertexBufferMorphPosNorm, 0f, 0f, 0f, 0.3f, 0f)

        val vertexBufferMorphColor =
            ByteBuffer.allocateDirect(colorSize).order(ByteOrder.nativeOrder())
        putCubeColors(vertexBufferMorphColor, 1)

        val meshBufferMorph =
            MeshBuffer.createDynamic(
                session = currentSession,
                vertexLayout = dynamicLayout,
                vertexCount = CUBE_VERTEX_COUNT,
                indexCount = CUBE_INDEX_COUNT,
                vertexData =
                    listOf(
                        ByteBufferRegion(vertexBufferMorphPosNorm, 0, posNormSize),
                        ByteBufferRegion(vertexBufferMorphColor, 0, colorSize),
                    ),
                indexData = ByteBufferRegion(indexBuffer, 0, indexSize),
            )

        val cubeMeshMorph =
            CustomMesh.BuilderFromMeshBuffer(currentSession, meshBufferMorph)
                .addSubset(MeshSubsetTopology.TRIANGLES, 0, CUBE_INDEX_COUNT)
                .setBounds(
                    BoundingBox.fromCenterAndHalfExtents(
                        Vector3(0f, 0f, 0f),
                        FloatSize3d(0.15f, 0.15f, 0.15f),
                    )
                )
                .build()

        dynamicVertexEntity =
            createMeshEntity(
                currentSession,
                cubeMeshMorph,
                listOf(material!!),
                Pose(Vector3(4f, 0.2f, -1.5f)),
            )

        // --- 2. Color Cycle Cube (Pos/Norm constant in Buffer 0, Updates Color in Buffer 1) ---
        val vertexBufferCyclePosNorm =
            ByteBuffer.allocateDirect(posNormSize).order(ByteOrder.nativeOrder())
        putMorphedCubeVertices(vertexBufferCyclePosNorm, 0f, 0f, 0f, 0.3f, 0f)

        val vertexBufferCycleColor =
            ByteBuffer.allocateDirect(colorSize).order(ByteOrder.nativeOrder())
        putAnimatedCubeColors(vertexBufferCycleColor, 0f)

        val meshBufferCycle =
            MeshBuffer.createDynamic(
                session = currentSession,
                vertexLayout = dynamicLayout,
                vertexCount = CUBE_VERTEX_COUNT,
                indexCount = CUBE_INDEX_COUNT,
                vertexData =
                    listOf(
                        ByteBufferRegion(vertexBufferCyclePosNorm, 0, posNormSize),
                        ByteBufferRegion(vertexBufferCycleColor, 0, colorSize),
                    ),
                indexData = ByteBufferRegion(indexBuffer, 0, indexSize),
            )

        val cubeMeshCycle =
            CustomMesh.BuilderFromMeshBuffer(currentSession, meshBufferCycle)
                .addSubset(MeshSubsetTopology.TRIANGLES, 0, CUBE_INDEX_COUNT)
                .setBounds(
                    BoundingBox.fromCenterAndHalfExtents(
                        Vector3(0f, 0f, 0f),
                        FloatSize3d(0.15f, 0.15f, 0.15f),
                    )
                )
                .build()

        dynamicColorEntity =
            createMeshEntity(
                currentSession,
                cubeMeshCycle,
                listOf(material!!),
                Pose(Vector3(4f, -0.2f, -1.5f)),
            )

        // --- 3. Index Update Cube (Constant Pos/Color, Updates Index Buffer) ---
        val doubleSidedIndexCount = CUBE_INDEX_COUNT * 2
        val doubleSidedIndexSize = doubleSidedIndexCount * BYTES_PER_INDEX
        val indexBufferDoubleSided =
            ByteBuffer.allocateDirect(doubleSidedIndexSize).order(ByteOrder.nativeOrder())

        for (triangle in 0 until CUBE_TRIANGLE_COUNT) {
            val firstIndex = triangle * 3
            val v0 = indexBuffer.getInt(firstIndex * BYTES_PER_INDEX)
            val v1 = indexBuffer.getInt((firstIndex + 1) * BYTES_PER_INDEX)
            val v2 = indexBuffer.getInt((firstIndex + 2) * BYTES_PER_INDEX)

            indexBufferDoubleSided.putInt(v0)
            indexBufferDoubleSided.putInt(v1)
            indexBufferDoubleSided.putInt(v2)

            // Reversed
            indexBufferDoubleSided.putInt(v0)
            indexBufferDoubleSided.putInt(v2)
            indexBufferDoubleSided.putInt(v1)
        }

        val vertexBufferIndexPosNorm =
            ByteBuffer.allocateDirect(posNormSize).order(ByteOrder.nativeOrder())
        putMorphedCubeVertices(vertexBufferIndexPosNorm, 0f, 0f, 0f, 0.3f, 0f)

        val vertexBufferIndexColor =
            ByteBuffer.allocateDirect(colorSize).order(ByteOrder.nativeOrder())
        putCubeColors(vertexBufferIndexColor, 0)

        val meshBufferIndex =
            MeshBuffer.createDynamic(
                session = currentSession,
                vertexLayout = dynamicLayout,
                vertexCount = CUBE_VERTEX_COUNT,
                indexCount = doubleSidedIndexCount,
                vertexData =
                    listOf(
                        ByteBufferRegion(vertexBufferIndexPosNorm, 0, posNormSize),
                        ByteBufferRegion(vertexBufferIndexColor, 0, colorSize),
                    ),
                indexData = ByteBufferRegion(indexBufferDoubleSided, 0, doubleSidedIndexSize),
            )

        val cubeMeshIndex =
            CustomMesh.BuilderFromMeshBuffer(currentSession, meshBufferIndex)
                .addSubset(MeshSubsetTopology.TRIANGLES, 0, doubleSidedIndexCount)
                .setBounds(
                    BoundingBox.fromCenterAndHalfExtents(
                        Vector3(0f, 0f, 0f),
                        FloatSize3d(0.15f, 0.15f, 0.15f),
                    )
                )
                .build()

        dynamicIndexEntity =
            createMeshEntity(
                currentSession,
                cubeMeshIndex,
                listOf(material2!!),
                Pose(Vector3(4f, -0.6f, -1.5f)),
            )

        // --- Panel ---
        createPanel(
            currentSession,
            "Dynamic Meshes:\nTop: Morph to Pyramid\nMid: Color Cycle\nBot: Index Assembly",
            Pose(Vector3(4f, 0.7f, -1.5f)),
            listOfNotNull(dynamicVertexEntity, dynamicColorEntity, dynamicIndexEntity),
        )

        // --- Animation Loop ---
        lifecycleScope.launch {
            var time = 0f
            var indexTime = 0
            // Frames taken to assemble the cube one triangle at a time before restarting.
            val assemblyPeriodFrames = 150
            val updateBufferPosNorm =
                ByteBuffer.allocateDirect(posNormSize).order(ByteOrder.nativeOrder())
            val updateBufferColor =
                ByteBuffer.allocateDirect(colorSize).order(ByteOrder.nativeOrder())
            val updateBufferIndex =
                ByteBuffer.allocateDirect(doubleSidedIndexSize).order(ByteOrder.nativeOrder())

            val posNormRegion = ByteBufferRegion(updateBufferPosNorm, 0, posNormSize)
            val colorRegion = ByteBufferRegion(updateBufferColor, 0, colorSize)
            val indexRegion = ByteBufferRegion(updateBufferIndex, 0, doubleSidedIndexSize)

            val originalIndices = IntArray(CUBE_INDEX_COUNT)
            indexBuffer.position(0)
            indexBuffer.asIntBuffer().get(originalIndices)

            // Order: Top-left triangle of each face, then bottom-right triangle
            val triangleOrder = intArrayOf(0, 2, 4, 6, 8, 10, 1, 3, 5, 7, 9, 11)

            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                while (true) {
                    awaitFrame()
                    time += 0.05f

                    // 1. Morph Cube (Buffer 0)
                    updateBufferPosNorm.clear()
                    val morph = (kotlin.math.sin(time) + 1f) / 2f
                    putMorphedCubeVertices(updateBufferPosNorm, 0f, 0f, 0f, 0.3f, morph)
                    cubeMeshMorph.meshBuffer.updateVertexData(0, posNormRegion)

                    // 2. Color Cycle Cube (Buffer 1)
                    updateBufferColor.clear()
                    putAnimatedCubeColors(updateBufferColor, time)
                    cubeMeshCycle.meshBuffer.updateVertexData(1, colorRegion)

                    // 3. Index Assembly Cube
                    indexTime = (indexTime + 1) % assemblyPeriodFrames
                    // Reveal one more triangle each step, holding the full cube for one step.
                    val numTriangles =
                        (indexTime / assemblyPeriodFrames.toFloat() * (CUBE_TRIANGLE_COUNT + 1))
                            .toInt()
                            .coerceIn(0, CUBE_TRIANGLE_COUNT)
                    updateBufferIndex.clear()

                    for (i in 0 until CUBE_TRIANGLE_COUNT) {
                        if (i < numTriangles) {
                            val targetTriangle = triangleOrder[i]
                            val v0 = originalIndices[targetTriangle * 3]
                            val v1 = originalIndices[targetTriangle * 3 + 1]
                            val v2 = originalIndices[targetTriangle * 3 + 2]

                            // Front face
                            updateBufferIndex.putInt(v0)
                            updateBufferIndex.putInt(v1)
                            updateBufferIndex.putInt(v2)

                            // Back face
                            updateBufferIndex.putInt(v0)
                            updateBufferIndex.putInt(v2)
                            updateBufferIndex.putInt(v1)
                        } else {
                            // Degenerate triangle pair
                            for (j in 0 until 6) {
                                updateBufferIndex.putInt(0)
                            }
                        }
                    }
                    cubeMeshIndex.meshBuffer.updateIndexData(indexRegion)
                }
            }
        }
    }
}
