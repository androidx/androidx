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

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import androidx.xr.runtime.Session
import androidx.xr.runtime.SessionCreateSuccess
import androidx.xr.runtime.math.FloatSize2d
import androidx.xr.runtime.math.IntSize2d
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Vector3
import androidx.xr.scenecore.AlphaMode
import androidx.xr.scenecore.ByteBufferRegion
import androidx.xr.scenecore.CustomMesh
import androidx.xr.scenecore.ExperimentalCustomMeshApi
import androidx.xr.scenecore.KhronosPbrMaterial
import androidx.xr.scenecore.MeshBuffer
import androidx.xr.scenecore.MeshEntity
import androidx.xr.scenecore.MeshSubset
import androidx.xr.scenecore.MeshSubsetTopology
import androidx.xr.scenecore.MovableComponent
import androidx.xr.scenecore.PanelEntity
import androidx.xr.scenecore.VertexAttribute
import androidx.xr.scenecore.VertexAttributeDescriptor
import androidx.xr.scenecore.VertexAttributeType
import androidx.xr.scenecore.VertexLayout
import androidx.xr.scenecore.scene
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalCustomMeshApi::class)
class MeshEntityActivity : ComponentActivity() {
    private var session: Session? = null
    private var material: KhronosPbrMaterial? = null
    private var material2: KhronosPbrMaterial? = null
    private var cubeEntity: MeshEntity? = null
    private var twoSubsetsEntity: MeshEntity? = null
    private var sharedBufferBottomEntity: MeshEntity? = null
    private var sharedBufferTopEntity: MeshEntity? = null
    private var triangleStripEntity: MeshEntity? = null
    private var twoMaterialsEntity: MeshEntity? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val sessionResult = Session.create(this)
        if (sessionResult !is SessionCreateSuccess) {
            finish()
            return
        }
        session = sessionResult.session
        session!!.scene.keyEntity = session!!.scene.mainPanelEntity
        session!!
            .scene
            .mainPanelEntity
            .addComponent(MovableComponent.createSystemMovable(session!!))
        session!!.scene.mainPanelEntity.setEnabled(false)

        createMeshEntities()

        setContent { MeshEntityUI() }
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

    private fun putCubeVertices(
        vertexBuffer: ByteBuffer,
        centerX: Float,
        centerY: Float,
        centerZ: Float,
        size: Float,
        colorSchemeIndex: Int = 0,
    ) {
        val h = size / 2.0f
        fun putVertex(
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
        ) {
            vertexBuffer.putFloat(x + centerX)
            vertexBuffer.putFloat(y + centerY)
            vertexBuffer.putFloat(z + centerZ)
            vertexBuffer.putFloat(nx)
            vertexBuffer.putFloat(ny)
            vertexBuffer.putFloat(nz)
            vertexBuffer.put(r.toByte())
            vertexBuffer.put(g.toByte())
            vertexBuffer.put(b.toByte())
            vertexBuffer.put(a.toByte())
        }

        val colors = colorSchemes[colorSchemeIndex]
        val cFront = colors[0]
        val cBack = colors[1]
        val cTop = colors[2]
        val cBottom = colors[3]
        val cRight = colors[4]
        val cLeft = colors[5]

        // Front face
        putVertex(-h, -h, h, 0.0f, 0.0f, 1.0f, cFront[0], cFront[1], cFront[2], 255)
        putVertex(h, -h, h, 0.0f, 0.0f, 1.0f, cFront[0], cFront[1], cFront[2], 255)
        putVertex(h, h, h, 0.0f, 0.0f, 1.0f, cFront[0], cFront[1], cFront[2], 255)
        putVertex(-h, h, h, 0.0f, 0.0f, 1.0f, cFront[0], cFront[1], cFront[2], 255)

        // Back face
        putVertex(-h, -h, -h, 0.0f, 0.0f, -1.0f, cBack[0], cBack[1], cBack[2], 255)
        putVertex(-h, h, -h, 0.0f, 0.0f, -1.0f, cBack[0], cBack[1], cBack[2], 255)
        putVertex(h, h, -h, 0.0f, 0.0f, -1.0f, cBack[0], cBack[1], cBack[2], 255)
        putVertex(h, -h, -h, 0.0f, 0.0f, -1.0f, cBack[0], cBack[1], cBack[2], 255)

        // Top face
        putVertex(-h, h, -h, 0.0f, 1.0f, 0.0f, cTop[0], cTop[1], cTop[2], 255)
        putVertex(-h, h, h, 0.0f, 1.0f, 0.0f, cTop[0], cTop[1], cTop[2], 255)
        putVertex(h, h, h, 0.0f, 1.0f, 0.0f, cTop[0], cTop[1], cTop[2], 255)
        putVertex(h, h, -h, 0.0f, 1.0f, 0.0f, cTop[0], cTop[1], cTop[2], 255)

        // Bottom face
        putVertex(-h, -h, -h, 0.0f, -1.0f, 0.0f, cBottom[0], cBottom[1], cBottom[2], 255)
        putVertex(h, -h, -h, 0.0f, -1.0f, 0.0f, cBottom[0], cBottom[1], cBottom[2], 255)
        putVertex(h, -h, h, 0.0f, -1.0f, 0.0f, cBottom[0], cBottom[1], cBottom[2], 255)
        putVertex(-h, -h, h, 0.0f, -1.0f, 0.0f, cBottom[0], cBottom[1], cBottom[2], 255)

        // Right face
        putVertex(h, -h, -h, 1.0f, 0.0f, 0.0f, cRight[0], cRight[1], cRight[2], 255)
        putVertex(h, h, -h, 1.0f, 0.0f, 0.0f, cRight[0], cRight[1], cRight[2], 255)
        putVertex(h, h, h, 1.0f, 0.0f, 0.0f, cRight[0], cRight[1], cRight[2], 255)
        putVertex(h, -h, h, 1.0f, 0.0f, 0.0f, cRight[0], cRight[1], cRight[2], 255)

        // Left face
        putVertex(-h, -h, -h, -1.0f, 0.0f, 0.0f, cLeft[0], cLeft[1], cLeft[2], 255)
        putVertex(-h, -h, h, -1.0f, 0.0f, 0.0f, cLeft[0], cLeft[1], cLeft[2], 255)
        putVertex(-h, h, h, -1.0f, 0.0f, 0.0f, cLeft[0], cLeft[1], cLeft[2], 255)
        putVertex(-h, h, -h, -1.0f, 0.0f, 0.0f, cLeft[0], cLeft[1], cLeft[2], 255)
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

    private fun createPanel(session: Session, text: String, pose: Pose) {
        val composeView =
            ComposeView(this).apply {
                setViewTreeLifecycleOwner(this@MeshEntityActivity)
                setViewTreeViewModelStoreOwner(this@MeshEntityActivity)
                setViewTreeSavedStateRegistryOwner(this@MeshEntityActivity)
                setContent {
                    Surface(color = Color.White, modifier = Modifier.fillMaxSize()) {
                        Column(
                            modifier = Modifier.fillMaxSize().padding(16.dp),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(
                                text = text,
                                color = Color.Black,
                                fontSize = 48.sp,
                                textAlign = TextAlign.Center,
                            )
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
            )
        panel.size = FloatSize2d(0.8f, 0.4f)
    }

    private fun createMeshEntities() {
        CoroutineScope(Dispatchers.Main).launch {
            val currentSession = session ?: return@launch

            // Define vertex layout
            val vertexLayout =
                VertexLayout(
                    listOf(
                        VertexAttributeDescriptor(
                            VertexAttribute.POSITION,
                            VertexAttributeType.FLOAT3,
                            0,
                        ),
                        VertexAttributeDescriptor(
                            VertexAttribute.NORMAL,
                            VertexAttributeType.FLOAT3,
                            0,
                        ),
                        VertexAttributeDescriptor(
                            VertexAttribute.COLOR,
                            VertexAttributeType.UBYTE4_NORM,
                            0,
                        ),
                    )
                )

            val stride = 28

            material = KhronosPbrMaterial.create(currentSession, AlphaMode.OPAQUE)
            material?.setMetallicFactor(0f)
            material?.setRoughnessFactor(1f)

            material2 = KhronosPbrMaterial.create(currentSession, AlphaMode.OPAQUE)
            material2?.setMetallicFactor(1f)
            material2?.setRoughnessFactor(1f)

            createTest1_Cube(currentSession, vertexLayout, stride)
            createTest2_TwoSubsets(currentSession, vertexLayout, stride)
            createTest3_SharedBuffer(currentSession, vertexLayout, stride)
            createTest4_TriangleStrip(currentSession, vertexLayout, stride)
            createTest5_TwoMaterials(currentSession, vertexLayout, stride)
        }
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
            CustomMesh.Builder(currentSession)
                .setVertexLayout(vertexLayout)
                .addVertexBufferData(ByteBufferRegion(sharedBuffer, 0, vertexSize))
                .setIndexData(ByteBufferRegion(sharedBuffer, vertexSize, indexSize))
                .setSingleSubsetTopology(MeshSubsetTopology.TRIANGLES)
                .build()
        cubeEntity =
            MeshEntity.create(
                currentSession,
                cubeMesh,
                listOf(material!!),
                pose = Pose(Vector3(-2f, 0f, -1.5f)),
            )
        createPanel(
            currentSession,
            "A cube with six different colored faces.\nBox: " +
                "[${cubeMesh.bounds.min.x},${cubeMesh.bounds.min.y},${cubeMesh.bounds.min.z}] - " +
                "[${cubeMesh.bounds.max.x},${cubeMesh.bounds.max.y},${cubeMesh.bounds.max.z}]",
            Pose(Vector3(-2f, 0.7f, -1.5f)),
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
            CustomMesh.Builder(currentSession)
                .setVertexLayout(vertexLayout)
                .addVertexBufferData(ByteBufferRegion(vertexBuffer, 0, vertexCount * stride))
                .setIndexData(ByteBufferRegion(indexBuffer, 0, 72 * 4))
                .addSubset(MeshSubset(MeshSubsetTopology.TRIANGLES, 0, 36))
                .addSubset(MeshSubset(MeshSubsetTopology.TRIANGLES, 36, 36))
                .build()
        twoSubsetsEntity =
            MeshEntity.create(
                currentSession,
                twoSubsetsMesh,
                listOf(material!!, material!!),
                pose = Pose(Vector3(-1f, 0f, -1.5f)),
            )
        createPanel(
            currentSession,
            "Two Subsets: One mesh with two subsets rendered on top of each other.",
            Pose(Vector3(-1f, 0.7f, -1.5f)),
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
            CustomMesh.Builder(currentSession)
                .setMeshBuffer(meshBuffer)
                .addSubset(MeshSubset(MeshSubsetTopology.TRIANGLES, 0, 36))
                .build()
        val topCubeMesh =
            CustomMesh.Builder(currentSession)
                .setMeshBuffer(meshBuffer)
                .addSubset(MeshSubset(MeshSubsetTopology.TRIANGLES, 36, 36))
                .build()
        sharedBufferBottomEntity =
            MeshEntity.create(
                currentSession,
                bottomCubeMesh,
                listOf(material!!),
                pose = Pose(Vector3(0f, 0f, -1.5f)),
            )
        sharedBufferTopEntity =
            MeshEntity.create(
                currentSession,
                topCubeMesh,
                listOf(material!!),
                pose = Pose(Vector3(0f, 0f, -1.5f)),
            )
        createPanel(
            currentSession,
            "Shared Buffer: Two meshes sharing the same buffer rendered on top of each other.",
            Pose(Vector3(0f, 0.7f, -1.5f)),
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
            CustomMesh.Builder(currentSession)
                .setVertexLayout(vertexLayout)
                .addVertexBufferData(ByteBufferRegion(vertexBuffer, 0, vertexCount * stride))
                .setIndexData(ByteBufferRegion(indexBuffer, 0, stripIndexCount * 4))
                .setSingleSubsetTopology(MeshSubsetTopology.TRIANGLE_STRIP)
                .build()
        triangleStripEntity =
            MeshEntity.create(
                currentSession,
                cubeMesh,
                listOf(material!!),
                pose = Pose(Vector3(1f, 0f, -1.5f)),
            )
        createPanel(
            currentSession,
            "Triangle Strip: A cube rendered with TRIANGLE_STRIP.",
            Pose(Vector3(1f, 0.7f, -1.5f)),
        )
    }

    private fun createSwapMaterialsPanel(session: Session, pose: Pose) {
        val composeView =
            ComposeView(this).apply {
                setViewTreeLifecycleOwner(this@MeshEntityActivity)
                setViewTreeViewModelStoreOwner(this@MeshEntityActivity)
                setViewTreeSavedStateRegistryOwner(this@MeshEntityActivity)
                setContent {
                    Surface(color = Color.White, modifier = Modifier.fillMaxSize()) {
                        Column(
                            modifier = Modifier.fillMaxSize().padding(16.dp),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally,
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
                }
            }
        val panel =
            PanelEntity.create(
                session,
                view = composeView,
                pixelDimensions = IntSize2d(1600, 400),
                name = "Panel_SwapMaterials",
                pose = pose,
            )
        panel.size = FloatSize2d(0.8f, 0.2f)
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
            CustomMesh.Builder(currentSession)
                .setVertexLayout(vertexLayout)
                .addVertexBufferData(ByteBufferRegion(vertexBuffer, 0, vertexCount * stride))
                .setIndexData(ByteBufferRegion(indexBuffer, 0, 72 * 4))
                .addSubset(MeshSubset(MeshSubsetTopology.TRIANGLES, 0, 36))
                .addSubset(MeshSubset(MeshSubsetTopology.TRIANGLES, 36, 36))
                .build()
        twoMaterialsEntity =
            MeshEntity.create(
                currentSession,
                cubeMesh,
                listOf(material!!, material2!!),
                pose = Pose(Vector3(2f, 0f, -1.5f)),
            )
        createPanel(
            currentSession,
            "Two Materials: One mesh with two subsets using different materials.",
            Pose(Vector3(2f, 0.7f, -1.5f)),
        )
        createSwapMaterialsPanel(currentSession, Pose(Vector3(2f, -0.7f, -1.5f)))
    }

    @Composable
    fun MeshEntityUI() {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = "Mesh Entity Test", color = Color.White)
        }
    }
}
