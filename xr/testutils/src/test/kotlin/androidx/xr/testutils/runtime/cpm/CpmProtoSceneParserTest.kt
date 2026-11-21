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

package androidx.xr.testutils.runtime.cpm

import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Quaternion
import androidx.xr.runtime.math.Vector3
import androidx.xr.testutils.SnapshotXrScene
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.zip.GZIPInputStream
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class CpmProtoSceneParserTest {

    @Test
    fun parseScene_preservesSubMillimeterPoseAndQuatOrdering() {
        val expectedPos = Vector3(0.3127f, -0.1044f, -1.5008f)
        val expectedQuat = Quaternion.fromAxisAngle(Vector3(0f, 1f, 0f), 37.5f)
        val cpmBytes = buildProto {
            writeMessage(2) { // Root (id=1)
                writeVarint(1, 1L)
                writeVarint(3, 2L) // child: space-root-task-42
                writeMessage(5) { writeString(17, "Root") }
            }
            writeMessage(2) { // space-root-task-42 (id=2)
                writeVarint(1, 2L)
                writeVarint(3, 3L)
                writeVarint(4, 1L)
                writeMessage(5) { writeString(17, "space-root-task-42") }
                writeMessage(6) { writeVarint(5, 1L) }
            }
            writeMessage(2) { // precisionPanel (id=3)
                writeVarint(1, 3L)
                writeVarint(3, 4L)
                writeVarint(4, 2L)
                writeMessage(5) {
                    writeString(17, "precisionPanel")
                    writeVarint(5, 1L)
                    writeVarint(9, 17L)
                }
                writeMessage(6) {
                    writeQuat(1, expectedQuat)
                    writeVec3(2, expectedPos)
                    writeVec3(3, Vector3(1f, 1f, 1f))
                    writeVarint(5, 1L)
                }
            }
            writeMessage(2) { // #1 surface child (id=4)
                writeVarint(1, 4L)
                writeVarint(4, 3L)
                writeMessage(5) {
                    writeString(17, "#1")
                    writeVarint(22, 1L) // isAndroid2d
                    writeMessage(12) { // GraphicBufferProto
                        writeVarint(1, 400L)
                        writeVarint(2, 300L)
                    }
                    writeVec3(4, Vector3(400f, 300f, 0f)) // root2DSurfaceBoundsInPixels
                }
                writeMessage(6) {
                    writeVarint(5, 1L)
                    writeVec3(17, Vector3(0.4f, 0.3f, 0f))
                    writeFloat(18, 1000f)
                }
            }
        }

        val scene = CpmProtoSceneParser.parseScene(cpmBytes)
        val task = scene.findTaskById(42)
        assertNotNull(task)
        val panel = task.findNode { it.name == "precisionPanel" }
        assertNotNull(panel)

        assertTrue(abs(panel.position.x - expectedPos.x) < 1e-6f)
        assertTrue(abs(panel.position.y - expectedPos.y) < 1e-6f)
        assertTrue(abs(panel.position.z - expectedPos.z) < 1e-6f)
        assertTrue(abs(panel.rotation.w - expectedQuat.w) < 1e-5f)
        assertTrue(abs(panel.rotation.x - expectedQuat.x) < 1e-5f)
        assertTrue(abs(panel.rotation.y - expectedQuat.y) < 1e-5f)
        assertTrue(abs(panel.rotation.z - expectedQuat.z) < 1e-5f)
        assertEquals(17, panel.inputId)
        assertTrue(panel.isInputTarget)
        assertEquals(0.4f, panel.surfaceBoundsInMeters?.x)
        assertEquals(0.3f, panel.surfaceBoundsInMeters?.y)
        assertEquals(1000f, task.computedPixelsPerMeter)
        val stats = assertNotNull(scene.stats)
        assertEquals(4, stats.attachedNodes)
        assertEquals(120_000L, stats.bufferPixels)
    }

    @Test
    fun parseScene_decodesPackedChildrenNegativeInputIdEmbeddedLeashAndSettledAlphas() {
        val cpmBytes = buildProto {
            writeMessage(2) { // Root (id=1)
                writeVarint(1, 1L)
                writeVarint(3, 2L)
                writeMessage(5) { writeString(17, "Root") }
            }
            writeMessage(2) { // space-root-task-4 (id=2) with packed children [3, 4]
                writeVarint(1, 2L)
                writePackedVarints(3, listOf(3L, 4L))
                writeVarint(4, 1L)
                writeMessage(5) { writeString(17, "space-root-task-4") }
                writeMessage(6) { writeVarint(5, 1L) }
            }
            writeMessage(2) { // Task window leash (id=3) -> hosts window-task-4 (id=5)
                writeVarint(1, 3L)
                writeVarint(3, 5L)
                writeVarint(4, 2L)
                writeMessage(5) {
                    writeString(17, "mainWindowLeash")
                    writeVarint(9, -1L) // signed negative int32 varint
                }
                writeMessage(6) { writeVarint(5, 1L) }
            }
            writeMessage(2) { // window-task-4 (id=5) -> child 2D buffer #10 (id=6)
                writeVarint(1, 5L)
                writeVarint(3, 6L)
                writeVarint(4, 3L)
                writeMessage(5) { writeString(17, "window-task-4") }
                writeMessage(6) { writeVarint(5, 1L) }
            }
            writeMessage(2) { // #10 under window-task-4 (id=6)
                writeVarint(1, 6L)
                writeVarint(4, 5L)
                writeMessage(5) {
                    writeString(17, "#10")
                    writeVarint(22, 1L)
                }
                writeMessage(6) { writeVarint(5, 1L) }
            }
            writeMessage(2) { // Embedded panel leash (id=4) -> child #11 (id=7)
                writeVarint(1, 4L)
                writeVarint(3, 7L)
                writeVarint(4, 2L)
                writeMessage(5) {
                    writeString(17, "embeddedPanelLeash")
                    writeVarint(9, -1L)
                    writeFloat(6, 1.0f) // requestedAlpha
                }
                writeMessage(6) {
                    writeVarint(5, 1L)
                    writeFloat(6, 0.25f) // computedAlpha still fading in
                }
            }
            writeMessage(2) { // #11 under embeddedPanelLeash (id=7)
                writeVarint(1, 7L)
                writeVarint(4, 4L)
                writeMessage(5) {
                    writeString(17, "#11")
                    writeVarint(22, 1L)
                    writeMessage(52) { // XrGraphicBufferProto -> VulkanImageFdProto
                        writeMessage(2) {
                            writeVarint(1, 512L)
                            writeVarint(2, 256L)
                        }
                    }
                }
                writeMessage(6) { writeVarint(5, 1L) }
            }
        }

        val scene = CpmProtoSceneParser.parseScene(cpmBytes)
        val task = scene.findTaskById(4)
        assertNotNull(task)
        assertFalse(task.isSettled(requireFullSpace = false))

        val mainLeash = task.findNode { it.name == "mainWindowLeash" }
        assertNotNull(mainLeash)
        assertTrue(mainLeash.isTaskWindowLeash)
        assertFalse(mainLeash.isEmbeddedWindowLeash)
        assertEquals(-1, mainLeash.inputId)
        assertFalse(mainLeash.isInputTarget)

        val windowNode = task.findNode { it.name == "window-task-4" }
        assertNotNull(windowNode)
        assertFalse(windowNode.isEmbeddedWindowLeash)

        val embeddedLeash = task.findNode { it.name == "embeddedPanelLeash" }
        assertNotNull(embeddedLeash)
        assertTrue(embeddedLeash.isEmbeddedWindowLeash)

        val embeddedSurface = task.findNode { it.name == "#11" }
        assertNotNull(embeddedSurface)
        assertFalse(embeddedSurface.isEmbeddedWindowLeash)
        assertEquals(512f, embeddedSurface.bufferSize?.x)
        assertEquals(256f, embeddedSurface.bufferSize?.y)
        val stats = assertNotNull(scene.stats)
        assertEquals(7, stats.attachedNodes)
        assertEquals(131_072L, stats.bufferPixels)
    }

    @Test
    fun parseScene_decodesImpressTransformsEulerCollidersAndSubspaceWorldExtents() {
        val expectedEulerRot =
            Quaternion.fromAxisAngle(Vector3(0f, 1f, 0f), 45f) *
                Quaternion.fromAxisAngle(Vector3(0f, 0f, 1f), 15f) *
                Quaternion.fromAxisAngle(Vector3(1f, 0f, 0f), 30f)
        val cpmBytes = buildProto {
            writeMessage(2) { // Root (id=1)
                writeVarint(1, 1L)
                writeVarint(3, 2L)
                writeMessage(5) { writeString(17, "Root") }
            }
            writeMessage(2) { // space-root-task-7 (id=2)
                writeVarint(1, 2L)
                writePackedVarints(3, listOf(10L, 11L, 12L, 13L))
                writeVarint(4, 1L)
                writeMessage(5) { writeString(17, "space-root-task-7") }
                writeMessage(6) { writeVarint(5, 1L) }
            }
            writeMessage(2) { // Node with Home Space CPM reset while Impress has world pose (id=10)
                writeVarint(1, 10L)
                writeVarint(4, 2L)
                writeMessage(5) { writeString(17, "impressAuthoritativeTarget") }
                writeMessage(6) {
                    writeQuat(1, Quaternion.Identity)
                    writeVec3(2, Vector3(9f, 9f, 9f))
                    writeVec3(3, Vector3(2f, 2f, 2f))
                    writeVarint(5, 1L)
                }
            }
            writeMessage(2) { // StereoSurface subspace CPM node with RenderSubspaceProto (id=11)
                writeVarint(1, 11L)
                writeVarint(4, 2L)
                writeMessage(5) {
                    writeString(
                        17,
                        "stereo_surface_panel_entity_subspace_-42_system_side_subspace_root",
                    )
                    writeVarint(9, 17L) // Active collider inputId on SurfaceEntity subspace node
                    writeMessage(51) { // RenderSubspaceProto
                        writeVarint(1, 1L)
                        writeVarint(2, 42L)
                    }
                }
                writeMessage(6) { writeVarint(5, 1L) }
            }
            writeMessage(2) { // GLTF subspace CPM node with negative fallback suffix (id=12)
                writeVarint(1, 12L)
                writeVarint(4, 2L)
                writeMessage(5) {
                    writeString(17, "gltf_entity_subspace_-99_system_side_subspace_root")
                }
                writeMessage(6) { writeVarint(5, 1L) }
            }
            writeMessage(
                2
            ) { // Node absent from Impress falling back to CPM computed_state over local_state
                // (id=13)
                writeVarint(1, 13L)
                writeVarint(4, 2L)
                writeMessage(5) {
                    writeString(17, "computedStateFallbackTarget")
                    writeVec3(2, Vector3(1f, 1f, 1f))
                    writeVec3(3, Vector3(1f, 1f, 1f))
                }
                writeMessage(6) {
                    writeQuat(1, expectedEulerRot)
                    writeVec3(2, Vector3(9f, 9f, 9f))
                    writeVec3(3, Vector3(2f, 2f, 2f))
                    writeVarint(5, 1L)
                }
            }
        }

        val rendererBytes = buildProto {
            writeMessage(1) { // Impress Root NodeData
                writeString(1, "ImpRoot")
                writeMessage(6) {
                    writeVec3(1, Vector3(0f, 0f, 0f))
                    writeVec3(2, Vector3(1f, 1f, 1f))
                }
                writeMessage(9) {
                    writeString(1, "impressAuthoritativeChild")
                    writeVarint(4, 1L) // disabled in Impress -> isActive=false
                    writeMessage(6) {
                        writeVec3(1, Vector3(1.2345f, 2.3456f, -3.4567f))
                        writeVec3(2, Vector3(1.5f, 1.5f, 1.5f))
                        writeVec3(
                            4,
                            Vector3(30f, 45f, 15f),
                        ) // EulerFromQuatClamped Y * Z * X degrees
                    }
                    writeMessage(8) {
                        writeMessage(2) {
                            writeString(1, "type.googleapis.com/spaceflinger.NodeStateManagerState")
                            writeMessage(2) { writeVarint(1, 10L) }
                        }
                    }
                }
                writeMessage(9) {
                    writeString(
                        1,
                        "stereo_surface_panel_entity_subspace_-42_system_side_subspace_root",
                    )
                    writeMessage(6) {
                        writeVec3(1, Vector3(-0.4f, 0f, -1.5f))
                        writeVec3(2, Vector3(1f, 1f, 1f))
                    }
                    writeMessage(9) {
                        writeString(
                            1,
                            "stereo_surface_panel_entity_subspace_-42_app_side_subspace_root",
                        )
                        writeMessage(6) { writeVec3(2, Vector3(1f, 1f, 1f)) }
                        writeMessage(9) {
                            writeString(1, "entityImpressNode")
                            writeMessage(6) { writeVec3(2, Vector3(1.2f, 0.8f, 1.0f)) }
                        }
                    }
                }
                writeMessage(9) {
                    writeString(1, "gltf_entity_subspace_-99_app_side_subspace_root")
                    writeMessage(6) {
                        writeVec3(1, Vector3(0.5f, 0f, -2.0f))
                        writeVec3(2, Vector3(2f, 2f, 2f))
                    }
                    writeMessage(8) {
                        writeMessage(2) {
                            writeString(1, "type.googleapis.com/imp.BoxColliderState")
                            writeMessage(2) {
                                writeMessage(1) {
                                    writeVec3(1, Vector3(0f, 0.1f, 0f))
                                    writeVec3(2, Vector3(0.25f, 0.5f, 0.15f))
                                }
                            }
                        }
                    }
                }
            }
        }

        val scene = CpmProtoSceneParser.parseScene(cpmBytes, rendererBytes)
        val task = scene.findTaskById(7)
        assertNotNull(task)

        val impressAuthNode = task.findNode { it.name == "impressAuthoritativeTarget" }
        assertNotNull(impressAuthNode)
        assertTrue(abs(impressAuthNode.position.x - 1.2345f) < 1e-5f)
        assertTrue(abs(impressAuthNode.position.y - 2.3456f) < 1e-5f)
        assertTrue(abs(impressAuthNode.position.z - (-3.4567f)) < 1e-5f)
        assertTrue(abs(impressAuthNode.rotation.w - expectedEulerRot.w) < 1e-5f)
        assertTrue(abs(impressAuthNode.rotation.x - expectedEulerRot.x) < 1e-5f)
        assertTrue(abs(impressAuthNode.rotation.y - expectedEulerRot.y) < 1e-5f)
        assertTrue(abs(impressAuthNode.rotation.z - expectedEulerRot.z) < 1e-5f)
        assertEquals(1.5f, impressAuthNode.scale.x)
        assertEquals("false", impressAuthNode.properties["ImpNodeIsActive"])

        val fallbackNode = task.findNode { it.name == "computedStateFallbackTarget" }
        assertNotNull(fallbackNode)
        assertEquals(9f, fallbackNode.position.x, 1e-5f)
        assertEquals(9f, fallbackNode.position.y, 1e-5f)
        assertEquals(9f, fallbackNode.position.z, 1e-5f)
        assertTrue(abs(fallbackNode.rotation.w - expectedEulerRot.w) < 1e-5f)
        assertTrue(abs(fallbackNode.rotation.x - expectedEulerRot.x) < 1e-5f)
        assertTrue(abs(fallbackNode.rotation.y - expectedEulerRot.y) < 1e-5f)
        assertTrue(abs(fallbackNode.rotation.z - expectedEulerRot.z) < 1e-5f)
        assertEquals(2f, fallbackNode.scale.x, 1e-5f)
        assertEquals("true", fallbackNode.properties["ImpNodeIsActive"])
        assertNull(fallbackNode.worldExtents)

        val stereoNode = task.findNode {
            it.name == "stereo_surface_panel_entity_subspace_-42_system_side_subspace_root"
        }
        assertNotNull(stereoNode)
        val stereoExtents = assertNotNull(stereoNode.worldExtents)
        assertTrue(abs(stereoExtents.x - 1.2f) < 1e-5f)
        assertTrue(abs(stereoExtents.y - 0.8f) < 1e-5f)

        val gltfNode = task.findNode {
            it.name == "gltf_entity_subspace_-99_system_side_subspace_root"
        }
        assertNotNull(gltfNode)
        assertTrue(gltfNode.hasSubspace)
        val gltfExtents = assertNotNull(gltfNode.worldExtents)
        // worldScale=(2,2,2) * 2 * halfExtent(0.25, 0.5, 0.15) = (1.0, 2.0, 0.6)
        assertTrue(abs(gltfExtents.x - 1.0f) < 1e-5f)
        assertTrue(abs(gltfExtents.y - 2.0f) < 1e-5f)
        assertTrue(abs(gltfExtents.z - 0.6f) < 1e-5f)
    }

    @Test
    fun captureProtoSceneWithPolling_retriesTransientEmptyBinaryOutputUntilReady() {
        val validCpmBytes = buildProto {
            writeMessage(2) { // Root (id=1)
                writeVarint(1, 1L)
                writeVarint(3, 2L)
                writeMessage(5) { writeString(17, "Root") }
            }
            writeMessage(2) { // space-root-task-12 (id=2)
                writeVarint(1, 2L)
                writeVarint(3, 3L)
                writeVarint(4, 1L)
                writeMessage(5) { writeString(17, "space-root-task-12") }
                writeMessage(6) { writeVarint(5, 1L) }
            }
            writeMessage(2) { // panelLeash (id=3)
                writeVarint(1, 3L)
                writeVarint(3, 4L)
                writeVarint(4, 2L)
                writeMessage(5) { writeString(17, "panelLeash") }
                writeMessage(6) { writeVarint(5, 1L) }
            }
            writeMessage(2) { // #1 2D surface child (id=4) -> marks panelLeash as embedded leash
                writeVarint(1, 4L)
                writeVarint(4, 3L)
                writeMessage(5) {
                    writeString(17, "#1")
                    writeVarint(22, 1L)
                }
                writeMessage(6) { writeVarint(5, 1L) }
            }
        }
        val validRendererBytes = buildProto {
            writeMessage(1) { writeString(1, "ImpRoot") }
        }

        var cpmAttempts = 0
        var rendererAttempts = 0
        var textSpfCalls = 0
        var sleepCount = 0
        var nowMs = 0L

        val scene =
            captureProtoSceneWithPolling(
                includeRenderer = true,
                maxWaitMs = 1_000L,
                sleeper = { dt ->
                    sleepCount++
                    nowMs += dt
                },
                clockMs = { nowMs },
                binaryShellRunner = { command ->
                    when (command) {
                        // Poll 1: empty cpmBytes -> retries
                        // Poll 2: non-empty cpmBytes, empty rendererBytes -> retries
                        // Poll 3: both non-empty -> succeeds
                        "dumpsys spf_cpm --proto" -> {
                            cpmAttempts++
                            if (cpmAttempts == 1) ByteArray(0) else validCpmBytes
                        }
                        "dumpsys spf_cpm --proto --renderer" -> {
                            rendererAttempts++
                            if (rendererAttempts == 1) ByteArray(0) else validRendererBytes
                        }
                        else -> ByteArray(0)
                    }
                },
                textShellRunner = { command ->
                    when (command) {
                        "dumpsys spf_cpm" -> {
                            textSpfCalls++
                            ""
                        }
                        "dumpsys activity activities" -> "Task{#12 visible=true A=com.example.test}"
                        else -> ""
                    }
                },
            )

        assertTrue(scene.isProtoCapture)
        assertFalse(SnapshotXrScene(rootNodes = emptyList()).isProtoCapture)
        assertEquals(0, textSpfCalls)
        assertEquals(3, cpmAttempts)
        assertEquals(2, rendererAttempts)
        assertEquals(2, sleepCount)
        assertEquals(4, scene.stats?.attachedNodes)
        val task = assertNotNull(scene.findTaskById(12))
        assertEquals("com.example.test", task.packageName)
        assertNotNull(task.findNode { it.name == "panelLeash" && it.isEmbeddedWindowLeash })
    }

    @Test
    fun captureProtoSceneWithPolling_retriesTransientUnparseableBinaryOutputUntilReady() {
        val validCpmBytes = buildProto {
            writeMessage(2) { // Root (id=1)
                writeVarint(1, 1L)
                writeVarint(3, 2L)
                writeMessage(5) { writeString(17, "Root") }
            }
            writeMessage(2) { // space-root-task-12 (id=2)
                writeVarint(1, 2L)
                writeVarint(3, 3L)
                writeVarint(4, 1L)
                writeMessage(5) { writeString(17, "space-root-task-12") }
                writeMessage(6) { writeVarint(5, 1L) }
            }
            writeMessage(2) { // panelLeash (id=3)
                writeVarint(1, 3L)
                writeVarint(3, 4L)
                writeVarint(4, 2L)
                writeMessage(5) { writeString(17, "panelLeash") }
                writeMessage(6) { writeVarint(5, 1L) }
            }
            writeMessage(2) { // #1 2D surface child (id=4) -> marks panelLeash as embedded leash
                writeVarint(1, 4L)
                writeVarint(4, 3L)
                writeMessage(5) {
                    writeString(17, "#1")
                    writeVarint(22, 1L)
                }
                writeMessage(6) { writeVarint(5, 1L) }
            }
        }
        val validRendererBytes = buildProto {
            writeMessage(1) { writeString(1, "ImpRoot") }
        }
        val timeoutMessageBytes = "CPM scene dump timed out\n".encodeToByteArray()
        // Mid-field truncated renderer NodeData (tag 0x0A, length 8, 0 payload bytes).
        val truncatedRendererBytes = byteArrayOf(0x0A, 0x08)

        var cpmAttempts = 0
        var rendererAttempts = 0
        var textSpfCalls = 0
        var sleepCount = 0
        var nowMs = 0L

        val scene =
            captureProtoSceneWithPolling(
                includeRenderer = true,
                maxWaitMs = 1_000L,
                sleeper = { dt ->
                    sleepCount++
                    nowMs += dt
                },
                clockMs = { nowMs },
                binaryShellRunner = { command ->
                    when (command) {
                        // Poll 1: plain-text dump timeout on --proto -> parse throws -> retries
                        // Poll 2: valid cpmBytes, truncated rendererBytes -> parse throws ->
                        // retries
                        // Poll 3: both valid -> succeeds
                        "dumpsys spf_cpm --proto" -> {
                            cpmAttempts++
                            if (cpmAttempts == 1) timeoutMessageBytes else validCpmBytes
                        }
                        "dumpsys spf_cpm --proto --renderer" -> {
                            rendererAttempts++
                            if (rendererAttempts == 2) {
                                truncatedRendererBytes
                            } else {
                                validRendererBytes
                            }
                        }
                        else -> ByteArray(0)
                    }
                },
                textShellRunner = { command ->
                    when (command) {
                        "dumpsys spf_cpm" -> {
                            textSpfCalls++
                            ""
                        }
                        "dumpsys activity activities" -> "Task{#12 visible=true A=com.example.test}"
                        else -> ""
                    }
                },
            )

        assertTrue(scene.isProtoCapture)
        assertEquals(0, textSpfCalls)
        assertEquals(3, cpmAttempts)
        assertEquals(3, rendererAttempts)
        assertEquals(2, sleepCount)
        assertEquals(4, scene.stats?.attachedNodes)
        val task = assertNotNull(scene.findTaskById(12))
        assertEquals("com.example.test", task.packageName)
        assertNotNull(task.findNode { it.name == "panelLeash" && it.isEmbeddedWindowLeash })
    }

    @Test
    fun captureProtoSceneWithPolling_throwsIllegalStateExceptionWhenBinaryRemainsEmpty() {
        var textSpfCalls = 0
        var nowMs = 0L

        val emptyCpmError =
            assertFailsWith<IllegalStateException> {
                captureProtoSceneWithPolling(
                    includeRenderer = true,
                    maxWaitMs = 200L,
                    sleeper = { dt -> nowMs += dt },
                    clockMs = { nowMs },
                    binaryShellRunner = { ByteArray(0) },
                    textShellRunner = { command ->
                        if (command == "dumpsys spf_cpm") textSpfCalls++
                        ""
                    },
                )
            }
        assertTrue(emptyCpmError.message!!.contains("dumpsys spf_cpm --proto"))
        assertEquals(0, textSpfCalls)

        val nonEmptyCpmBytes = buildProto {
            writeMessage(2) {
                writeVarint(1, 1L)
                writeMessage(5) { writeString(17, "Root") }
            }
        }
        nowMs = 0L
        val emptyRendererError =
            assertFailsWith<IllegalStateException> {
                captureProtoSceneWithPolling(
                    includeRenderer = true,
                    maxWaitMs = 200L,
                    sleeper = { dt -> nowMs += dt },
                    clockMs = { nowMs },
                    binaryShellRunner = { command ->
                        if (command == "dumpsys spf_cpm --proto") nonEmptyCpmBytes else ByteArray(0)
                    },
                    textShellRunner = { command ->
                        if (command == "dumpsys spf_cpm") textSpfCalls++
                        ""
                    },
                )
            }
        assertTrue(emptyRendererError.message!!.contains("--proto --renderer"))
        assertEquals(0, textSpfCalls)
    }

    @Test
    fun captureProtoSceneWithPolling_throwsIllegalStateExceptionWhenProtoUnparseable() {
        var binaryAttempts = 0
        var textSpfCalls = 0
        var nowMs = 0L
        // Truncated length-delimited field 2 (tag 0x12, length 10, 0 payload bytes).
        val corruptProtoBytes = byteArrayOf(0x12, 0x0A)

        val error =
            assertFailsWith<IllegalStateException> {
                captureProtoSceneWithPolling(
                    includeRenderer = false,
                    maxWaitMs = 200L,
                    sleeper = { dt -> nowMs += dt },
                    clockMs = { nowMs },
                    binaryShellRunner = {
                        binaryAttempts++
                        corruptProtoBytes
                    },
                    textShellRunner = { command ->
                        if (command == "dumpsys spf_cpm") textSpfCalls++
                        ""
                    },
                )
            }
        assertTrue(error.message!!.contains("Failed to parse dumpsys spf_cpm --proto"))
        assertNotNull(error.cause)
        assertEquals(3, binaryAttempts)
        assertEquals(0, textSpfCalls)
    }

    @Test
    fun captureProtoSceneWithPolling_computesStatsFromProtoTreeWithoutTextDumpsys() {
        val cpmBytes = buildProto {
            writeMessage(2) { // Root (id=1)
                writeVarint(1, 1L)
                writeVarint(3, 2L)
                writeMessage(5) { writeString(17, "Root") }
            }
            writeMessage(2) { // space-root-task-12 (id=2)
                writeVarint(1, 2L)
                writeVarint(3, 3L)
                writeVarint(4, 1L)
                writeMessage(5) { writeString(17, "space-root-task-12") }
                writeMessage(6) { writeVarint(5, 1L) }
            }
            writeMessage(2) { // embedded panel leash (id=3)
                writeVarint(1, 3L)
                writeVarint(3, 4L)
                writeVarint(4, 2L)
                writeMessage(5) { writeString(17, "panelLeash") }
                writeMessage(6) { writeVarint(5, 1L) }
            }
            writeMessage(2) { // #1 (id=4)
                writeVarint(1, 4L)
                writeVarint(4, 3L)
                writeMessage(5) {
                    writeString(17, "#1")
                    writeVarint(22, 1L)
                    writeMessage(12) {
                        writeVarint(1, 320L)
                        writeVarint(2, 240L)
                    }
                }
                writeMessage(6) { writeVarint(5, 1L) }
            }
        }

        var defaultTextSpfCalls = 0
        val defaultScene =
            captureProtoSceneWithPolling(
                includeRenderer = false,
                binaryShellRunner = { command ->
                    if (command == "dumpsys spf_cpm --proto") cpmBytes else ByteArray(0)
                },
                textShellRunner = { command ->
                    when (command) {
                        "dumpsys spf_cpm" -> {
                            defaultTextSpfCalls++
                            ""
                        }
                        "dumpsys activity activities" -> "Task{#12 visible=true A=com.example.test}"
                        else -> ""
                    }
                },
            )
        assertEquals(0, defaultTextSpfCalls)
        val defaultStats = assertNotNull(defaultScene.stats)
        assertEquals(4, defaultStats.attachedNodes)
        assertEquals(76_800L, defaultStats.bufferPixels)
    }

    @Test
    fun protoWireReader_rejectsIntMaxLengthWithoutIntegerOverflow() {
        // Tag: field 2, wireType 2 (0x12) + varint Int.MAX_VALUE (FF FF FF FF 07)
        val malformed =
            byteArrayOf(0x12, 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0x07)
        assertFailsWith<IllegalArgumentException> {
            val reader = ProtoWireReader(malformed)
            reader.readTag()
            reader.subReader()
        }
        assertFailsWith<IllegalArgumentException> {
            val reader = ProtoWireReader(malformed)
            reader.readTag()
            reader.readBytes()
        }
        assertFailsWith<IllegalArgumentException> {
            val reader = ProtoWireReader(malformed)
            val tag = reader.readTag()
            reader.skipField(tag and 0x7)
        }
    }

    @Test
    fun readBinaryShellWithTimeout_returnsEmptyBytesOnTimeoutAndRunsCleanup() {
        var timedOutCleanup = false
        val result =
            readBinaryShellWithTimeout(
                command = "dumpsys spf_cpm --proto",
                timeoutMs = 30L,
                onTimeout = { timedOutCleanup = true },
            ) {
                CountDownLatch(1).await(500L, TimeUnit.MILLISECONDS)
                byteArrayOf(1, 2, 3)
            }

        assertTrue(timedOutCleanup)
        assertTrue(result.isEmpty())
    }

    @Test
    fun readBinaryShellWithTimeout_preservesInterruptFlagAndCancelsFuture() {
        var cleanupCalled = false
        val started = CountDownLatch(1)
        val futureCancelled = CountDownLatch(1)
        val callerThread = Thread.currentThread()
        val interrupter = Thread {
            if (started.await(1L, TimeUnit.SECONDS)) {
                CountDownLatch(1).await(20L, TimeUnit.MILLISECONDS)
                callerThread.interrupt()
            }
        }
        interrupter.start()

        val result =
            try {
                readBinaryShellWithTimeout(
                    command = "dumpsys spf_cpm --proto",
                    timeoutMs = 2_000L,
                    onTimeout = { cleanupCalled = true },
                ) {
                    started.countDown()
                    try {
                        CountDownLatch(1).await(2L, TimeUnit.SECONDS)
                    } catch (_: InterruptedException) {
                        futureCancelled.countDown()
                    }
                    byteArrayOf(1, 2, 3)
                }
            } finally {
                val wasInterrupted = Thread.interrupted()
                assertTrue(wasInterrupted, "Expected interrupt flag to be restored on caller")
                interrupter.join(1_000L)
            }

        assertTrue(cleanupCalled)
        assertTrue(result.isEmpty())
        assertTrue(futureCancelled.await(1L, TimeUnit.SECONDS))
    }

    @Test
    fun parseScene_ignoresDegenerateCylinderColliderDimensions() {
        val cpmBytes = buildProto {
            writeMessage(2) { // Root (id=1)
                writeVarint(1, 1L)
                writeVarint(3, 2L)
                writeMessage(5) { writeString(17, "Root") }
            }
            writeMessage(2) { // space-root-task-7 (id=2)
                writeVarint(1, 2L)
                writePackedVarints(3, listOf(3L, 4L, 5L))
                writeVarint(4, 1L)
                writeMessage(5) { writeString(17, "space-root-task-7") }
                writeMessage(6) { writeVarint(5, 1L) }
            }
            listOf(3L to -11, 4L to -12, 5L to -13).forEach { (id, sid) ->
                writeMessage(2) {
                    writeVarint(1, id)
                    writeVarint(4, 2L)
                    writeMessage(5) {
                        writeString(17, "gltf_entity_subspace_${sid}_system_side_subspace_root")
                        writeVarint(5, 1L)
                    }
                    writeMessage(6) { writeVarint(5, 1L) }
                }
            }
        }
        val rendererBytes = buildProto {
            writeMessage(1) {
                writeString(1, "ImpRoot")
                writeMessage(6) { writeVec3(2, Vector3(1f, 1f, 1f)) }
                fun ProtoBuilder.writeCylinderSubspace(
                    subspaceId: Int,
                    radius: Float,
                    height: Float,
                ) {
                    writeMessage(9) {
                        writeString(1, "gltf_entity_subspace_${subspaceId}_app_side_subspace_root")
                        writeMessage(6) { writeVec3(2, Vector3(1f, 1f, 1f)) }
                        writeMessage(8) {
                            writeMessage(2) {
                                writeString(1, "type.googleapis.com/imp.CylinderColliderState")
                                writeMessage(2) {
                                    writeVec3(1, Vector3.Zero)
                                    writeFloat(2, radius)
                                    writeFloat(3, height)
                                }
                            }
                        }
                    }
                }
                writeCylinderSubspace(-11, radius = 0.5f, height = 0f)
                writeCylinderSubspace(-12, radius = 0f, height = 1.0f)
                writeCylinderSubspace(-13, radius = 0.5f, height = 1.0f)
            }
        }

        val scene = CpmProtoSceneParser.parseScene(cpmBytes, rendererBytes)
        val task = assertNotNull(scene.findTaskById(7))
        val zeroHeight = assertNotNull(task.findNode { it.name.contains("subspace_-11_") })
        val zeroRadius = assertNotNull(task.findNode { it.name.contains("subspace_-12_") })
        val valid = assertNotNull(task.findNode { it.name.contains("subspace_-13_") })

        assertNull(zeroHeight.subspaceBoundsInMeters)
        assertNull(zeroHeight.worldExtents)
        assertNull(zeroRadius.subspaceBoundsInMeters)
        assertNull(zeroRadius.worldExtents)
        assertEquals(Vector3(1.0f, 1.0f, 1.0f), valid.subspaceBoundsInMeters)
        assertEquals(Vector3(1.0f, 1.0f, 1.0f), valid.worldExtents)
    }

    @Test
    fun primarySurfaceNode_ignoresActivityRecordInputSinkAndSelectsVisibleSurface() {
        val dumpsys =
            """
            SPF Nodes:
            Node Root (stateIndex=0, z=0) id=1:
            visible on both (requested-vis=show)
              Node space-root-task-42 (stateIndex=0, z=0) id=2:
              visible on both (requested-vis=show)
                Node taskWindowLeash (stateIndex=0, z=0) id=3:
                visible on both (requested-vis=show)
                  Node window-task-42 (stateIndex=0, z=0) id=4:
                  visible on both (requested-vis=show)
                    Node 128a5b2 ActivityRecordInputSink androidx.xr.test/.TestActivity#78 (stateIndex=0, z=1) id=15:
                    hidden on both (requested-vis=hide) buffer=null touchableRegion=(0 0 1536 960) isSysUi=false isAndroid2d=true canReceiveInput=false isInputSink=true input-id=78
                    Node androidx.xr.test/androidx.activity.ComponentActivity#76 (stateIndex=0, z=4) id=14:
                    visible on both (requested-vis=show) buffer=1536x960 touchableRegion=(0 0 1536 960) isSysUi=false isAndroid2d=true canReceiveInput=true isInputSink=false input-id=76
            """
                .trimIndent()

        val scene = parseSpatialScene(dumpsys)
        val task = assertNotNull(scene.findTaskById(42))
        val leash = assertNotNull(task.taskWindowLeash)
        val primarySurface = assertNotNull(leash.primarySurfaceNode)
        assertTrue(primarySurface.name.contains("ComponentActivity#76"))
        assertTrue(primarySurface.canReceiveInput)
        assertEquals(76, primarySurface.inputId)
    }

    @Test
    fun parseScene_preservesHiddenLocalVisibilityAndZeroScale() {
        val cpmBytes = buildProto {
            writeMessage(2) { // Root (id=1)
                writeVarint(1, 1L)
                writeVarint(3, 2L)
                writeMessage(5) { writeString(17, "Root") }
            }
            writeMessage(2) { // space-root-task-8 (id=2)
                writeVarint(1, 2L)
                writePackedVarints(3, listOf(3L, 4L))
                writeVarint(4, 1L)
                writeMessage(5) { writeString(17, "space-root-task-8") }
                writeMessage(6) { writeVarint(5, 1L) }
            }
            writeMessage(2) { // hiddenNode (id=3)
                writeVarint(1, 3L)
                writeVarint(4, 2L)
                writeMessage(5) {
                    writeString(17, "hiddenNode")
                    writeVarint(5, 0L)
                }
            }
            writeMessage(2) { // zeroScaleNode (id=4)
                writeVarint(1, 4L)
                writeVarint(4, 2L)
                writeMessage(5) {
                    writeString(17, "zeroScaleNode")
                    writeVarint(5, 1L)
                }
                writeMessage(6) {
                    writeVec3(3, Vector3(0f, 0f, 0f))
                    writeVarint(5, 1L)
                }
            }
        }

        val scene = CpmProtoSceneParser.parseScene(cpmBytes)
        val task = assertNotNull(scene.findTaskById(8))

        val hidden = assertNotNull(task.findNode { it.name == "hiddenNode" })
        assertFalse(hidden.isVisible)
        assertEquals("false", hidden.properties["ImpNodeIsActive"])
        assertEquals("hidden on both", hidden.stereoVisibility)

        val zeroScale = assertNotNull(task.findNode { it.name == "zeroScaleNode" })
        assertEquals(0f, zeroScale.scale.x, 1e-6f)
        assertEquals(0f, zeroScale.scale.y, 1e-6f)
        assertEquals(0f, zeroScale.scale.z, 1e-6f)
    }

    @Test
    fun parseScene_handlesCyclicParentChildIdsWithoutStackOverflow() {
        val cpmBytes = buildProto {
            writeMessage(2) { // cyclicA (id=100) <-> cyclicB (id=101)
                writeVarint(1, 100L)
                writeVarint(3, 101L)
                writeVarint(4, 101L)
                writeMessage(5) { writeString(17, "space-root-task-99") }
            }
            writeMessage(2) { // cyclicB (id=101) <-> cyclicA (id=100)
                writeVarint(1, 101L)
                writeVarint(3, 100L)
                writeVarint(4, 100L)
                writeMessage(5) {
                    writeString(17, "cyclicChild")
                    writeVarint(22, 1L)
                }
            }
        }

        val scene = CpmProtoSceneParser.parseScene(cpmBytes)
        val flattened = scene.rootNodes.flatMap { listOf(it) + it.getDescendants() }
        assertEquals(2, flattened.size)
        assertNotNull(scene.findTaskById(99))
    }

    @Test
    fun protoWireReader_rejectsOver32BitAndZeroFieldTags() {
        // 0x1_0000_0008 (33-bit varint: 88 80 80 80 10) exceeds unsigned 32-bit tag range.
        val over32BitTag =
            byteArrayOf(
                0x88.toByte(),
                0x80.toByte(),
                0x80.toByte(),
                0x80.toByte(),
                0x10,
                0x00,
            )
        assertFailsWith<IllegalArgumentException> { ProtoWireReader(over32BitTag).readTag() }

        // Field number 0 (tag=4, wireType=4 -> 0x04) is invalid in protobuf.
        val zeroFieldTag = byteArrayOf(0x04)
        assertFailsWith<IllegalArgumentException> { ProtoWireReader(zeroFieldTag).readTag() }
    }

    @Test
    fun parseScene_decodesRealSmI610ProtoAndRendererGoldenFixture() {
        val cpmBytes = gunzipBase64Chunks(REAL_CPM_PROTO_GZIP_B64)
        val rendererBytes = gunzipBase64Chunks(REAL_CPM_RENDERER_GZIP_B64)

        val scene = CpmProtoSceneParser.parseScene(cpmBytes, rendererBytes)
        assertTrue(scene.rootNodes.isNotEmpty(), "Expected non-empty rootNodes from SM-I610 dump")
        assertTrue(scene.tasks.isNotEmpty(), "Expected at least one task from SM-I610 dump")
        assertNotNull(scene.headPose, "Expected non-null headPose from SM-I610 dump")

        val allSpaces = scene.rootNodes.flatMap { listOf(it) + it.getDescendants() }
        assertTrue(allSpaces.size > 50, "Expected full compositor scene, got ${allSpaces.size}")
        for (space in allSpaces) {
            assertTrue(
                space.position.x.isFinite() &&
                    space.position.y.isFinite() &&
                    space.position.z.isFinite()
            )
            assertTrue(
                space.scale.x.isFinite() && space.scale.y.isFinite() && space.scale.z.isFinite()
            )
        }
        val subspacesWithExtents =
            scene.tasks.flatMap { it.subspaceNodes }.filter { it.worldExtents != null }
        assertEquals(
            0,
            subspacesWithExtents.size,
            "Idle SM-I610 Home capture has no active app subspaces with extents",
        )
        assertTrue(
            scene.tasks.any { it.taskWindowLeash != null },
            "Expected at least one task window leash decoded from real SM-I610 dump",
        )
    }

    @Test
    fun parseScene_decodesProto3OmittedZeroFieldsInPresentScaleAndQuaternion() {
        val rotY180 = Quaternion(0f, 1f, 0f, 0f)
        val cpmBytes = buildProto {
            writeMessage(2) { // Root (id=1)
                writeVarint(1, 1L)
                writeVarint(3, 2L)
                writeMessage(5) { writeString(17, "Root") }
            }
            writeMessage(2) { // space-root-task-15 (id=2)
                writeVarint(1, 2L)
                writePackedVarints(3, listOf(3L, 4L, 5L))
                writeVarint(4, 1L)
                writeMessage(5) { writeString(17, "space-root-task-15") }
                writeMessage(6) { writeVarint(5, 1L) }
            }
            writeMessage(2) { // CPM 180-deg quat (omitted w=0f) & partial zero scale (id=3)
                writeVarint(1, 3L)
                writeVarint(4, 2L)
                writeMessage(5) { writeString(17, "cpmZeroOmission") }
                writeMessage(6) {
                    writeQuat(1, rotY180)
                    writeVec3(3, Vector3(0f, 1.5f, 0f))
                    writeVarint(5, 1L)
                }
            }
            writeMessage(2) { // Empty 0-byte quat submessage normalizes to Identity (id=4)
                writeVarint(1, 4L)
                writeVarint(4, 2L)
                writeMessage(5) { writeString(17, "emptyQuat") }
                writeMessage(6) {
                    writeBytes(1, byteArrayOf())
                    writeVarint(5, 1L)
                }
            }
            writeMessage(2) { // Impress PreciseTransform fallback with omitted zeros (id=5)
                writeVarint(1, 5L)
                writeVarint(4, 2L)
                writeMessage(5) { writeString(17, "impressZeroOmission") }
            }
        }
        val rendererBytes = buildProto {
            writeMessage(1) {
                writeString(1, "ImpRoot")
                writeMessage(9) {
                    writeString(1, "impressZeroChild")
                    writeMessage(6) {
                        writeVec3(2, Vector3(0f, 2.5f, 0f))
                        writeQuat(3, rotY180)
                    }
                    writeMessage(8) {
                        writeMessage(2) {
                            writeString(1, "type.googleapis.com/spaceflinger.NodeStateManagerState")
                            writeMessage(2) { writeVarint(1, 5L) }
                        }
                    }
                }
            }
        }

        val scene = CpmProtoSceneParser.parseScene(cpmBytes, rendererBytes)
        val task = assertNotNull(scene.findTaskById(15))

        val cpmNode = assertNotNull(task.findNode { it.name == "cpmZeroOmission" })
        assertEquals(0f, cpmNode.rotation.w, 1e-6f)
        assertEquals(1f, cpmNode.rotation.y, 1e-6f)
        assertEquals(0f, cpmNode.scale.x, 1e-6f)
        assertEquals(1.5f, cpmNode.scale.y, 1e-6f)
        assertEquals(0f, cpmNode.scale.z, 1e-6f)

        val emptyQuatNode = assertNotNull(task.findNode { it.name == "emptyQuat" })
        assertEquals(Quaternion.Identity, emptyQuatNode.rotation)

        val impNode = assertNotNull(task.findNode { it.name == "impressZeroOmission" })
        assertEquals(0f, impNode.rotation.w, 1e-6f)
        assertEquals(1f, impNode.rotation.y, 1e-6f)
        assertEquals(0f, impNode.scale.x, 1e-6f)
        assertEquals(2.5f, impNode.scale.y, 1e-6f)
        assertEquals(0f, impNode.scale.z, 1e-6f)
    }

    @Test
    fun parseScene_ignoresDisabledImpressSubspacesAndDisabledDirectChildren() {
        val cpmBytes = buildProto {
            writeMessage(2) { // Root (id=1)
                writeVarint(1, 1L)
                writeVarint(3, 2L)
                writeMessage(5) { writeString(17, "Root") }
            }
            writeMessage(2) { // space-root-task-20 (id=2)
                writeVarint(1, 2L)
                writePackedVarints(3, listOf(10L, 11L, 12L, 13L))
                writeVarint(4, 1L)
                writeMessage(5) { writeString(17, "space-root-task-20") }
                writeMessage(6) { writeVarint(5, 1L) }
            }
            writeMessage(2) { // Disabled app-side vs active system-side fallback (id=10)
                writeVarint(1, 10L)
                writeVarint(4, 2L)
                writeMessage(5) {
                    writeString(17, "gltf_entity_subspace_-30_system_side_subspace_root")
                }
                writeMessage(6) { writeVarint(5, 1L) }
            }
            writeMessage(2) { // Standalone ancestor-disabled subspace (id=11)
                writeVarint(1, 11L)
                writeVarint(4, 2L)
                writeMessage(5) {
                    writeString(17, "gltf_entity_subspace_-31_system_side_subspace_root")
                }
                writeMessage(6) { writeVarint(5, 1L) }
            }
            writeMessage(2) { // StereoSurface with disabled first child, active second (id=12)
                writeVarint(1, 12L)
                writeVarint(4, 2L)
                writeMessage(5) {
                    writeString(
                        17,
                        "stereo_surface_panel_entity_subspace_-32_system_side_subspace_root",
                    )
                    writeMessage(51) {
                        writeVarint(1, 1L)
                        writeVarint(2, 32L)
                    }
                }
                writeMessage(6) { writeVarint(5, 1L) }
            }
            writeMessage(2) { // StereoSurface with only disabled direct child (id=13)
                writeVarint(1, 13L)
                writeVarint(4, 2L)
                writeMessage(5) {
                    writeString(
                        17,
                        "stereo_surface_panel_entity_subspace_-33_system_side_subspace_root",
                    )
                    writeMessage(51) {
                        writeVarint(1, 1L)
                        writeVarint(2, 33L)
                    }
                }
                writeMessage(6) { writeVarint(5, 1L) }
            }
        }

        val rendererBytes = buildProto {
            writeMessage(1) {
                writeString(1, "ImpRoot")
                // Active system-side root vs ancestor-disabled app-side root for subspace -30
                writeMessage(9) {
                    writeString(1, "disabledAncestor")
                    writeVarint(4, 1L) // disabled = true
                    writeMessage(9) {
                        writeString(1, "gltf_entity_subspace_-30_app_side_subspace_root")
                        writeMessage(6) { writeVec3(2, Vector3(1f, 1f, 1f)) }
                        writeMessage(8) {
                            writeMessage(2) {
                                writeString(1, "type.googleapis.com/imp.BoxColliderState")
                                writeMessage(2) {
                                    writeMessage(1) {
                                        writeVec3(2, Vector3(4.5f, 4.5f, 4.5f))
                                    }
                                }
                            }
                        }
                    }
                    // Standalone ancestor-disabled subspace -31
                    writeMessage(9) {
                        writeString(1, "gltf_entity_subspace_-31_app_side_subspace_root")
                        writeMessage(6) { writeVec3(2, Vector3(1f, 1f, 1f)) }
                        writeMessage(8) {
                            writeMessage(2) {
                                writeString(1, "type.googleapis.com/imp.BoxColliderState")
                                writeMessage(2) {
                                    writeMessage(1) {
                                        writeVec3(2, Vector3(2f, 2f, 2f))
                                    }
                                }
                            }
                        }
                    }
                }
                writeMessage(9) {
                    writeString(1, "gltf_entity_subspace_-30_system_side_subspace_root")
                    writeMessage(6) { writeVec3(2, Vector3(1f, 1f, 1f)) }
                    writeMessage(8) {
                        writeMessage(2) {
                            writeString(1, "type.googleapis.com/imp.BoxColliderState")
                            writeMessage(2) {
                                writeMessage(1) {
                                    writeVec3(2, Vector3(0.5f, 0.4f, 0.3f))
                                }
                            }
                        }
                    }
                }
                // StereoSurface -32: disabled first child (9,9,9), active second child (1.4,0.9,1)
                writeMessage(9) {
                    writeString(
                        1,
                        "stereo_surface_panel_entity_subspace_-32_system_side_subspace_root",
                    )
                    writeMessage(6) { writeVec3(2, Vector3(1f, 1f, 1f)) }
                    writeMessage(9) {
                        writeString(
                            1,
                            "stereo_surface_panel_entity_subspace_-32_app_side_subspace_root",
                        )
                        writeMessage(6) { writeVec3(2, Vector3(1f, 1f, 1f)) }
                        writeMessage(9) {
                            writeString(1, "disabledFirstChild")
                            writeVarint(4, 1L)
                            writeMessage(6) { writeVec3(2, Vector3(9f, 9f, 9f)) }
                        }
                        writeMessage(9) {
                            writeString(1, "activeStereoSurface")
                            writeMessage(6) { writeVec3(2, Vector3(1.4f, 0.9f, 1.0f)) }
                        }
                    }
                }
                // StereoSurface -33: only disabled direct child -> null extents
                writeMessage(9) {
                    writeString(
                        1,
                        "stereo_surface_panel_entity_subspace_-33_app_side_subspace_root",
                    )
                    writeMessage(6) { writeVec3(2, Vector3(1f, 1f, 1f)) }
                    writeMessage(9) {
                        writeString(1, "onlyDisabledChild")
                        writeVarint(4, 1L)
                        writeMessage(6) { writeVec3(2, Vector3(3.0f, 2.0f, 1.0f)) }
                    }
                }
            }
        }

        val scene = CpmProtoSceneParser.parseScene(cpmBytes, rendererBytes)
        val task = assertNotNull(scene.findTaskById(20))

        val fallbackActive = assertNotNull(task.findNode { it.name.contains("subspace_-30_") })
        val fallbackExtents = assertNotNull(fallbackActive.worldExtents)
        assertEquals(1.0f, fallbackExtents.x, 1e-5f)
        assertEquals(0.8f, fallbackExtents.y, 1e-5f)
        assertEquals(0.6f, fallbackExtents.z, 1e-5f)

        val standaloneDisabled = assertNotNull(task.findNode { it.name.contains("subspace_-31_") })
        assertNull(standaloneDisabled.subspaceBoundsInMeters)
        assertNull(standaloneDisabled.worldExtents)

        val stereoActive = assertNotNull(task.findNode { it.name.contains("subspace_-32_") })
        val stereoExtents = assertNotNull(stereoActive.worldExtents)
        assertEquals(1.4f, stereoExtents.x, 1e-5f)
        assertEquals(0.9f, stereoExtents.y, 1e-5f)
        assertEquals(1.0f, stereoExtents.z, 1e-5f)

        val stereoDisabledChild = assertNotNull(task.findNode { it.name.contains("subspace_-33_") })
        assertNull(stereoDisabledChild.subspaceBoundsInMeters)
        assertNull(stereoDisabledChild.worldExtents)
    }

    @Test
    fun parseScene_computesSubspaceRelativeColliderExtentsWithoutDoubleRotatingRoot() {
        val rotY45 = Quaternion.fromAxisAngle(Vector3(0f, 1f, 0f), 45f)
        val rotZ90 = Quaternion.fromAxisAngle(Vector3(0f, 0f, 1f), 90f)
        val cpmBytes = buildProto {
            writeMessage(2) { // Root (id=1)
                writeVarint(1, 1L)
                writeVarint(3, 2L)
                writeMessage(5) { writeString(17, "Root") }
            }
            writeMessage(2) { // space-root-task-21 (id=2)
                writeVarint(1, 2L)
                writePackedVarints(3, listOf(10L, 11L, 12L, 13L))
                writeVarint(4, 1L)
                writeMessage(5) { writeString(17, "space-root-task-21") }
                writeMessage(6) { writeVarint(5, 1L) }
            }
            listOf(10L to -70, 11L to -71, 12L to -72, 13L to -73).forEach { (id, sid) ->
                writeMessage(2) {
                    writeVarint(1, id)
                    writeVarint(4, 2L)
                    writeMessage(5) {
                        writeString(17, "gltf_entity_subspace_${sid}_system_side_subspace_root")
                    }
                    writeMessage(6) { writeVarint(5, 1L) }
                }
            }
        }
        val rendererBytes = buildProto {
            writeMessage(1) {
                writeString(1, "ImpRoot")
                // Root rotated 45 deg Y with unrotated collider -> entity-frame extents invariant
                writeMessage(9) {
                    writeString(1, "gltf_entity_subspace_-70_app_side_subspace_root")
                    writeMessage(6) {
                        writeVec3(2, Vector3(1f, 1f, 1f))
                        writeQuat(3, rotY45)
                    }
                    writeMessage(8) {
                        writeMessage(2) {
                            writeString(1, "type.googleapis.com/imp.BoxColliderState")
                            writeMessage(2) {
                                writeMessage(1) {
                                    writeVec3(2, Vector3(0.5f, 0.25f, 0.15f))
                                }
                            }
                        }
                    }
                }
                // Child node rotated 90 deg Z inside subspace -> swaps X and Y box extents
                writeMessage(9) {
                    writeString(1, "gltf_entity_subspace_-71_app_side_subspace_root")
                    writeMessage(6) { writeVec3(2, Vector3(1f, 1f, 1f)) }
                    writeMessage(9) {
                        writeString(1, "rotatedBoxChild")
                        writeMessage(6) {
                            writeVec3(2, Vector3(1f, 1f, 1f))
                            writeQuat(3, rotZ90)
                        }
                        writeMessage(8) {
                            writeMessage(2) {
                                writeString(1, "type.googleapis.com/imp.BoxColliderState")
                                writeMessage(2) {
                                    writeMessage(1) {
                                        writeVec3(2, Vector3(1.0f, 0.25f, 0.15f))
                                    }
                                }
                            }
                        }
                    }
                }
                // Child node rotated 90 deg Z with cylinder collider -> height (2.0) moves to X
                writeMessage(9) {
                    writeString(1, "gltf_entity_subspace_-72_app_side_subspace_root")
                    writeMessage(6) {
                        writeVec3(2, Vector3(1f, 1f, 1f))
                        writeQuat(3, rotY45)
                    }
                    writeMessage(9) {
                        writeString(1, "rotatedCylinderChild")
                        writeMessage(6) {
                            writeVec3(2, Vector3(1f, 1f, 1f))
                            writeQuat(3, rotZ90)
                        }
                        writeMessage(8) {
                            writeMessage(2) {
                                writeString(1, "type.googleapis.com/imp.CylinderColliderState")
                                writeMessage(2) {
                                    writeFloat(2, 0.4f)
                                    writeFloat(3, 2.0f)
                                }
                            }
                        }
                    }
                }
                // Child node rotated 90 deg Z under 45 deg Y root -> sphere center rotates to Y
                // without double-rotating root
                writeMessage(9) {
                    writeString(1, "gltf_entity_subspace_-73_app_side_subspace_root")
                    writeMessage(6) {
                        writeVec3(2, Vector3(1f, 1f, 1f))
                        writeQuat(3, rotY45)
                    }
                    writeMessage(9) {
                        writeString(1, "rotatedSphereChild")
                        writeMessage(6) {
                            writeVec3(2, Vector3(1f, 1f, 1f))
                            writeQuat(3, rotZ90)
                        }
                        writeMessage(8) {
                            writeMessage(2) {
                                writeString(1, "type.googleapis.com/imp.SphereColliderState")
                                writeMessage(2) {
                                    writeVec3(1, Vector3(1f, 0f, 0f))
                                    writeFloat(2, 0.25f)
                                }
                            }
                        }
                    }
                }
            }
        }

        val scene = CpmProtoSceneParser.parseScene(cpmBytes, rendererBytes)
        val task = assertNotNull(scene.findTaskById(21))

        val rotatedRoot = assertNotNull(task.findNode { it.name.contains("subspace_-70_") })
        val rootExtents = assertNotNull(rotatedRoot.worldExtents)
        assertEquals(1.0f, rootExtents.x, 1e-4f)
        assertEquals(0.5f, rootExtents.y, 1e-4f)
        assertEquals(0.3f, rootExtents.z, 1e-4f)

        val rotatedBoxChild = assertNotNull(task.findNode { it.name.contains("subspace_-71_") })
        val boxExtents = assertNotNull(rotatedBoxChild.worldExtents)
        assertEquals(0.5f, boxExtents.x, 1e-4f)
        assertEquals(2.0f, boxExtents.y, 1e-4f)
        assertEquals(0.3f, boxExtents.z, 1e-4f)

        val rotatedCylChild = assertNotNull(task.findNode { it.name.contains("subspace_-72_") })
        val cylExtents = assertNotNull(rotatedCylChild.worldExtents)
        assertEquals(2.0f, cylExtents.x, 1e-4f)
        assertEquals(0.8f, cylExtents.y, 1e-4f)
        assertEquals(0.8f, cylExtents.z, 1e-4f)

        val rotatedSphereChild = assertNotNull(task.findNode { it.name.contains("subspace_-73_") })
        val sphereExtents = assertNotNull(rotatedSphereChild.worldExtents)
        assertEquals(0.5f, sphereExtents.x, 1e-4f)
        assertEquals(0.5f, sphereExtents.y, 1e-4f)
        assertEquals(0.5f, sphereExtents.z, 1e-4f)

        val impressData = CpmProtoSceneParser.parseImpressSceneDump(rendererBytes)
        val sphereSubspace =
            assertNotNull(impressData.subspaces.firstOrNull { it.subspaceId == -73 })
        val sphereCollider = assertNotNull(sphereSubspace.colliders.singleOrNull())
        assertEquals("Sphere", sphereCollider.type)
        assertEquals(0f, sphereCollider.centerInMeters.x, 1e-4f)
        assertEquals(1f, sphereCollider.centerInMeters.y, 1e-4f)
        assertEquals(0f, sphereCollider.centerInMeters.z, 1e-4f)
    }

    @Test
    fun captureDefaultSceneWithPolling_routesProtoAndTextByFlag() {
        val cpmBytes = buildProto {
            writeMessage(2) {
                writeVarint(1, 1L)
                writeVarint(3, 2L)
                writeMessage(5) { writeString(17, "Root") }
            }
            writeMessage(2) {
                writeVarint(1, 2L)
                writeVarint(3, 3L)
                writeVarint(4, 1L)
                writeMessage(5) { writeString(17, "space-root-task-50") }
                writeMessage(6) { writeVarint(5, 1L) }
            }
            writeMessage(2) {
                writeVarint(1, 3L)
                writeVarint(4, 2L)
                writeMessage(5) {
                    writeString(17, "gltf_entity_subspace_-80_system_side_subspace_root")
                }
                writeMessage(6) { writeVarint(5, 1L) }
            }
        }
        val rendererBytes = buildProto {
            writeMessage(1) {
                writeString(1, "ImpRoot")
                writeMessage(9) {
                    writeString(1, "gltf_entity_subspace_-80_app_side_subspace_root")
                    writeMessage(6) { writeVec3(2, Vector3(1f, 1f, 1f)) }
                    writeMessage(8) {
                        writeMessage(2) {
                            writeString(1, "type.googleapis.com/imp.BoxColliderState")
                            writeMessage(2) {
                                writeMessage(1) { writeVec3(2, Vector3(0.6f, 0.3f, 0.2f)) }
                            }
                        }
                    }
                }
            }
        }
        val textDump =
            """
            SPF Nodes:
            Node Root (stateIndex=0, z=0) id=1:
              Node space-root-task-50 (stateIndex=0, z=0) id=2:
              visible on both (requested-vis=show)
                Node panelLeash (stateIndex=0, z=0) id=3:
                visible on both (requested-vis=show) isAppSpatialEmbeddedWindowLeash=true
            """
                .trimIndent()

        var binaryCalls = 0
        var textSpfCalls = 0
        val protoScene =
            captureDefaultSceneWithPolling(
                useProto = true,
                maxWaitMs = 1_000L,
                binaryShellRunner = { cmd ->
                    binaryCalls++
                    when (cmd) {
                        "dumpsys spf_cpm --proto" -> cpmBytes
                        "dumpsys spf_cpm --proto --renderer" -> rendererBytes
                        else -> ByteArray(0)
                    }
                },
                textShellRunner = { cmd ->
                    when (cmd) {
                        "dumpsys spf_cpm" -> {
                            textSpfCalls++
                            ""
                        }
                        "dumpsys activity activities" ->
                            "Task{#50 visible=true A=com.example.proto}"
                        else -> ""
                    }
                },
            )
        assertEquals(2, binaryCalls)
        assertEquals(0, textSpfCalls)
        val protoTask = assertNotNull(protoScene.findTaskById(50))
        val subspace = assertNotNull(protoTask.findNode { it.name.contains("subspace_-80_") })
        assertEquals(Vector3(1.2f, 0.6f, 0.4f), subspace.worldExtents)

        var binaryCallsWhenFalse = 0
        val textScene =
            captureDefaultSceneWithPolling(
                useProto = false,
                maxWaitMs = 1_000L,
                binaryShellRunner = {
                    binaryCallsWhenFalse++
                    ByteArray(0)
                },
                textShellRunner = { cmd ->
                    when (cmd) {
                        "dumpsys spf_cpm" -> textDump
                        "dumpsys activity activities" -> "Task{#50 visible=true A=com.example.text}"
                        else -> ""
                    }
                },
            )
        assertEquals(0, binaryCallsWhenFalse)
        assertNotNull(textScene.findTaskById(50)?.findNode { it.name == "panelLeash" })

        assertTrue(
            shouldUseProtoCaptureByDefault { key ->
                if (key == ARG_SCENE_USE_PROTO) "true" else null
            }
        )
        assertFalse(shouldUseProtoCaptureByDefault { null })
    }

    private fun gunzipBase64Chunks(chunks: List<String>): ByteArray {
        val compressed = decodeBase64(chunks.joinToString(separator = ""))
        return GZIPInputStream(ByteArrayInputStream(compressed)).use { it.readBytes() }
    }

    private fun decodeBase64(encoded: String): ByteArray {
        val out = ByteArrayOutputStream((encoded.length * 3) / 4)
        var acc = 0
        var bits = 0
        for (ch in encoded) {
            val v =
                when (ch) {
                    in 'A'..'Z' -> ch.code - 'A'.code
                    in 'a'..'z' -> ch.code - 'a'.code + 26
                    in '0'..'9' -> ch.code - '0'.code + 52
                    '+' -> 62
                    '/' -> 63
                    '=' -> break
                    else -> continue
                }
            acc = (acc shl 6) or v
            bits += 6
            if (bits >= 8) {
                bits -= 8
                out.write((acc ushr bits) and 0xFF)
            }
        }
        return out.toByteArray()
    }

    private companion object {
        private val REAL_CPM_PROTO_GZIP_B64: List<String> =
            listOf(
                "H4sIAAAAAAAC/+2deXwTZf7H50lm6EOhUEqDwyUPLSKOgpS7StukbSRZSlvTlku7LXJWocUCCrrrhoIc",
                "FRFBRFxc8AARUBEBQQTK4YIcq4sXLxeRw2tF/SHqrst6/GbSpDzPdCbtZCabmXT4IzaDbZ/Cm8/383m+",
                "z5Ewh4Eg/rfAL8BaWZptxsawLdh4tg2bwNpYlu3IXs12Zbux17Ic25PtzfZlB7A3selsJnsL+zs2l/Ww",
                "w9nR7O/ZO9gJ7J1sOVvJ3sPexz7AVgF2HmCrAbsYsEsB95ElNjGO4n/ZhJfOwss1FOXNSGgtetpBeODN",
                "sAkvnYWXHqC/8J9BlOvKSPOvfFgp+WXvE39ZL6CqAe2pqJi+BIAnALUGUOsBtRlQNYA6DKiTgDoDqK9A",
                "bAKEVDzFUoi6BDr4PrzyrdCVD38Bvi9qoaotiaLx+r7/UovEsChqm0V43e17fdP3etz3+o6FOmmhzlio",
                "Ly3UJelPrbKKf6TdVuH1sJXqswuE+oebNDK2bvz4d0uo+3q+/9H/tAP5XXw/K0UlSX7vur82O3BR+VSx",
                "8BuTrvwBTqVmUst9f4gJL9HQilGIAHdaD7DAO8vLUHnFuPEmMHoCZmt2wvM0pHHZsqhDhpJA5tZGkwKq",
                "QZtpU8eMHd+zkheXntPHTLurZz+dIwMUINPuPx9mC8hs0SMy/r86gCNzay0pwrgT1jPQgpPCIJr7xBom",
                "cRlJKZGXdrXQTK8cUz5tQkXlFGOQIyc2ly3lvIjDpNZxHw3ZmG5rl0/X/pVyrePuTmtlt01sdrj2QX/h",
                "hf83TrmoorfuWZVWLLyUUlOExzOEl5lUFYi98i2qgfhPr7ZqBFM3Q6J6goYMjmortjWycPOtka6EvL7F",
                "3VtWPq7iXh0Q+qQlNi72w2cH24QXTWtjtTUxrnrX8Azb8TOjMzqfXzGxdhwGh+o/NGyFWyuGWxMyUEnC",
                "g5ps/sHUbKJwAlduiBYr/46+E/qMuSMVOcZOL7unbPosz/ixFZXj3OVTZ0wvKCu/C42tmNJr2pgp02aU",
                "T+w1pnxcZUXZuF5jpk7tNbOy16SKKeNv7DVsTFl54JOTBw5aAiiMTUCy2TIh1gektyWqibkEhDfdhDef",
                "xujLnB1UQFrV8vPptrvTFqcLD/4npFEi0gSBrlo+NoMfRavaPzI/ewcY2BoXtDievi81pM+vb7QrJ0T6",
                "8hqiq4HfF8E3ICCM4H8CnyCFdY81JfEZa2rsQOhtGV8TI4zQu9TrBT1Ab3AT9hPYvfyvzYsYF+XBnpYa",
                "hmKRXgJZis/SMA5X0NbcAu0Z1iCtdi2YUTmBN5SFlWPG3lVWPrHncI/7NhzQ4iiMsYZiTF4pj9GwGTkB",
                "8rMeJkC6SyFVMGtaUZmLV79o5srY0yPv0TAGL7yPC0TNtuqAqOtliRpeNq3sjrLJPE+ZYyory8ZXmljp",
                "DavtNHwc7xagGO4zPchUa8eMcWUVPqR65ZrTtfoD5yUatjDn901gGg3MERrGk8Bc1gMwTMHYyrIpUUPL",
                "FmsnWPvXWu/b8+MH0QLTOhq2Cav6HFU0lxrvLL+nrLKifMr48ukoStvRr+oYGCKWHRWpTgJum9vyqHit",
                "mnYVP1WEStthbo8nz1NSMKqg0DmsxJHj9BRitIB6c0zGVJ96/3NAfWKcuoUJy/ggANOndTDFOBOeZ2Bb",
                "HKZElMB9HLbGo1VJGUsWNx6DY2ZsUVpqtcM3flm6+al+8V4La0VUDypN+N5OilpjH0ZR/TI92DTqaPGn",
                "l1KTGkOqjmWvQVJ30DARr49tuYsWHfQcO/h7jtGsgUZH5ygNbbjItVM3zyRVMT9TBE0iSUu225GTN8Qs",
                "mbql6TOcpoUMbIfTdBWycXvDVjJpJSWzW/CSWY8z49fM3XU1s1koNdOAykawuJOGV+FFsR33nR6KYkfJ",
                "ohhlMmd0dg7TkMV1rD1fFX/VdnXq54qoaePHpTDPUVBY4vA4HWZJ1C1Kn+MorWVgexylDojl/hG2ksgo",
                "Wm8gUxIlITN+PTz18mO+egj99fD+xF5CPaSoYcJL8HpomQQaPddqTE6307ADXi7bc9/qoVyyZLmMQv0z",
                "Ojh/p2FHXOA68bVyvrYJ8gtl0w5+Vhz5+TnuLEehOy+3JG+405PjGGUWTd0y9QXO1CMM7IQz1Rl15N4M",
                "W9FspqRocjJFMzhtxq+ee5pcmiSI3EvDznh57MT9Ww/lsStZHqNZ8oxO0CEaXo1rWhe+Tv6ibab8Xlkb",
                "OwBNQYG7oNCRazYm9UvS9zhJDzKwC04SQldzu8JWHWOUVEckVx0lGDN+TTwkqol9+/ZtdKI0Zk0kONxG",
                "Q4TXxC7cN3qoiVeJamLUyZvRuXmbhl1x/UriK+GD2ibGH5RNMvhRGZ6XUzTMWZLp8Jh9R70T9QNO1MMM",
                "TMKJSkZduQNhq4hQSUXsIVMRg7Fm/Mp42F8ZQ5lrBZOMOJlK8Libhsl4ZUziftRDZexCVsbolTuj83OC",
                "ht1wPbuGr5ALtK2Q8yyKutZ+ZHLcw50lWY58c1LVEFDNs2BQLWHgNThU3VE37q9hK5LNFW0qlCmSDeBm",
                "/Dr5lYo6aTFknSSQrKFhd7xOXsP9pIc6mUTWyagWPaMjxJfKa3FV66F9qZwfUqksKHQUFhX43FVBUWZJ",
                "viPXmWOWSt1CNV9UKnvgUF2Hrg1jqYzVolQ2gJvxS+WFJteAnC8qldfhpbKHLktlVIue0RE6TkMOV7Xr",
                "+VI5R9tSuUBRqWznp2aoc9SQIocn25x11TtPC3CeHmLg9ThPNyCO2xe2KtlC0blHMlVSnrToKZBNZ86V",
                "oHEXDW/AC+T13Pd6KJCdyQIZrVJndHrepWFPXMt68bVxoba1caGi2tgp0L7OynIWFLgz3TnuwlHmlKvu",
                "qVqIU/UoA3vhVN2IenKHwlYhWyqpkDfIrdRpgDfj18mvm1ydJJjcR8Mb8TrZi/uPHupksmjVTlTLntEZ",
                "OkzD3riupWi/P7LaEsr+yJy8rKEFWR6nM9cskbpFqdpC7o9MwVHqg3qHcX9knBb7IyUhi566aJ6xI8Xp",
                "dhr2wctmii73R0ah/hkdnEM07IsLXD/t930sU1Qr44e7PYVFjhxhLiIzz+HJjqpSCWQkCBiyVC7DSfoX",
                "A/vhJPVHfblNdLhKZStV+z6CMGa0O7GA704s8jc58YPAnVi0iyrqPNqWVvzBzNuFO7EO/Lg+fYYwtpDv",
                "xBJK88V1y4jep2fH1UJpvuiI2o0mBPjbaNgfr7399LXRJGr11Ojc/J2GA3DBHKj90QQrFZXeDvmOgoJC",
                "lyevaIirZIQ7NztvRIkjS1hXVhBVRdgaVXl1Jc7UMRoOxJkahAZwVWHLq61VHU3QKNrME6sjidN+Gg7C",
                "cXoCoIH6OlqgCUiW0SFax8AniEvgU9Eg7i11d5zdmcU/OJlJUGVxFYZ470J/4R6+wP1702ZNmz5+yowy",
                "4RI+ebqSB4muKRUB1iKhuY+qrwCafAlc+ViHd/ENiO0HvwLxk1mvBbuK78qQ7ae9Xm/NYT47eK48xC/i",
                "e1MBlqc/Lk6zHT92bPD/8JI0EkvfTXynP/5YGEZOmv+WNB+o39EwFTfyTwDuce0x1eCekBulbrmSZzUa",
                "b7oyFnOULHN7aXgTro038yHgR23n31YrCgFQ6GYOdztHmC0q3ZbX1Xh5/YWGN+MEDUY3ca+GzfLHK9Ep",
                "Vmz5Jdgyfmfqm1+b2tJ/gr/NNByM87cCoJt1cG+fUOb9GSHqFM3oyKxk4AoiEaShwdwbqqyW/etsG1W6",
                "KluUCEaE6LCS5RJBgKXkQQOD+v/AbdznWqHzzWtv4/a/0elt3OdaxZ9vznpb17uN2zdo+5m6DIA9DTUE",
                "UNSZdFt29mPpkTBkq+tCAK+/GfwwEjNwQ/YJDdPwELACcHO1J1ODENBZKgQE8Gzqlj/yhFGyhO2iYTou",
                "fhm85f9OW8u/VpHlp4fkOoZHld23RJXdXyta6ZOB02NH6dzX4XJbI9soUaREsd0XcWXO5UcSnE00tOPg",
                "PAlQBndWDz69hd+nR5UMGR2X5Qx8kvDoDmTndqibDm3HOyFvotijjw7RAHWR8+gCR8mpvRvlz0/S6AQd",
                "8Oe+Nzr15yfp+BM062Xq+XPfoO1nRf7c9zRUf/7poFbptuPHWkbEPa2t8+efDlosDGNROu6e/kFDB+7P",
                "n1Rzh64slRr48/ZS/lxAs6l788jTRcnSdZSGmbjoZWl/4eRGRd48Md/pEU4IduYWlrhzs4Xj8/M85tS8",
                "bmvoRgt54WQWTlM2ygzjhZMJqi6cbIAz40/TL/4tME1v8U3T8wokTNOv3xe1q1QJFt+gYTbO4kqAsvR1",
                "52RUK53R8VnLwJVEHHCibO6wSuMlrI64OUsUB4pDXRQhu4hHgqvk1L6NigePA7QLBOKB741O48HjIH4X",
                "YGtAvXjgG7T9vCge+J6qiAdptpphKWmRMHAb8XggDOMVYj3FBRo68XiwEnBLtKdUg3hwneQaHglUzbgQ",
                "adooWdr20vAWXBSHqFu9I0XXFmWrd3IcRblZLqcZEfRRV4EEU1vwurqKgUNwglzoFu5E2CJCW1WrdyTY",
                "Mn4s2PfcMjWrd4x9726DeH5EQxeO5zOAfRawzwF2LWDXAfZ5wK4H7AbA7gHsXsF+sPsAux+wBwB7EKAh",
                "+loGFHXSaHS4djPwGTxTIBd3Loj02b8+YKNq1mYF8WpdBK9GZxGTbxbXllA9mj2T92YTKytmlI9DvAIi",
                "v2UbXjb+3tvk0kaAsuLklIGDlgDKhxqQyhgEai3hbjr+KQt7ikFPWwNwgWqL5GgjkDEcsRkQxAvtZm8z",
                "X7qoHTtAwP70bK/3zIdWPlsE+ffS6KxRusd24trO6YG/5Mn72qXZ1uZdDrZ2u3RPfVqFrxGa+6NEtPqy",
                "xuR9jwvDuI9wfzy/zyrgt+bhBvktkeDX6toWIX4HpSjh9yNG4HdnjOH4fUZDfidMaEbye66ioAF+hU8J",
                "O7/nKt6V5Pc5BfxSuzND0V/atSNS/PZTwu8PUOB3Wazh+H1WQ3779u2bplB/hU+JmP6uVcTv/lD4ZVy7",
                "IsXvQCX8vhYj8PsBNBy/z2nL727l/O6OGL/rFPmHrIb4HSrBbzPX3gjxm6rIP/w5VuD38xaG43ettv4h",
                "neC3eJa9Yf+QHnZ+i2fVSPL7vKL81iC/xRL8xrj2R4pfRf7h7RYCv5viDMfv89rmN5Lf8upbG85v4ee3",
                "vPrvkvyuV6K/cQcb4hdJ8Atdb0aKX0X+4bRV4PdX2nD8vqCp/s4j81v3rVc1qL/zwp/fum9dUZ/flQzc",
                "QPTk3TzB6rbRrWnptFFrNpCLIZu7SrXeRhegNDl1QKP68EfaoPdjA3143xud9uGPtIl/P5b1JtTrw/sG",
                "bb8k6sP7nobahy8qYjJsnQ49HfaFlLIzuT42i4oeFIbRVbyNzo1L6wa12+gkyQzXNro6EW3ivffIE0bJ",
                "EvYrDfcQ6vdnoYCv0bCB4B9UK9eqUOGik1P6N07gVgM0F8i0AXQmcatB/FzJpUa+n8G+ooqUON9T3U//",
                "B5c4yfh+koZ/JuzjHsD9bFG11kgKPw00zialcU1O23SHFiWLFq9tewltW9WQtiltLvkH1da1Wo22pTZK",
                "204HtE2iRaQzbTvt0zbiIDTsZ7A/IdK20wq0LXKtoeDaJtka4rVtFaFte1VrW4mpbeHSNh2iRcmixWtb",
                "DaFtTzWkbUobj/5Bsa5nVGjbgD7KfJtE+9BYvm2VCt8WubZhaL7tKULbakzfpl9t0yFaQX3bPkLb/tKw",
                "tu0PRds6u9aq0bb+yrRNorVsLG37izpti1BLOTRt+wuhbftMbdO1tukNraDatp/QttUNZ1JlCxb8g0py",
                "rVejbYMapW1HA9omsexAZ9p2VC6T+n4G+/MibTuqLJNGaLlBcG2TXG7Aa9tqQtv2q9a2oaa2hTGT6g0t",
                "ShYtXtsOENq2puFegrLFLP5BXevaqELbBqY0SttOBbRNYkmKzrTtlJy2+X4G+3qRtp1S1kuI0FKU4Nom",
                "uRSF17Y1hLYdUK1txaa2hbGXoDe0KFm0eG07SGjb0w36NoULnfyDusH1khpt66csk0osVzJWJt2oIpNG",
                "bplScG2TXKbEa9vThLYdVK1tyNS28Pk23aFFyaL1Fg1/h0vbUAS437Q9SnmHovMXEnLzCt23CAd3uPNy",
                "S/Iduc4c8yQG3Z5wtAPfjTyfgUNxlnLQ77jdYTuJIVGJLiWJT2IISpnxz2S4sKT2TAboP5Ph/sRewpkM",
                "FNWoo9qAAc/aIkh8nYY5OIkvADSU+z89HKXQ3n+UQhSrnNHhWcfAFwi7PwzlqL1t0Z7NP4gXH9Q2NkSX",
                "1VNuUXh9qpJTg68wiktoAb3NhKsLW6Kf+ViAv9NhKrgpdhD0JsSvg1ILxPlH66BvM1ltKsB/llBXiI9p",
                "MSDdtnABjMjZWTvqYsGYFtuFYSwgvNs/aTgMTwUvAG6x9phqkAqulUoF9Vlt6mvFI88aJcvaHhrm4pqY",
                "x+eEH7TNCXsU5YSYwry8nEJ3vhkOdFtV9+BVdScN83CA8lFu+BzZSJsSebpKHA7qo2VevBJJdl6mYT7O",
                "ziaA8rhP9WDnW/ntfLSJkdGJWc3ATYSHvxXlc/tVmqNM/sE48Y3pE7Te2OlHKTmld/D1s60SWgqHv71n",
                "YU8A9LBVmNHH3urQu98cmwp/sMQ/YmW91nre/QcLesRq31zn3YkfRoV5H2yb03NkRK6t3oObd2EYJwfj",
                "huo0DW/FzfsmwM3Rnk8NzHsnKfPuh9R07JEGjJIF7E0aenAFLOAd+3+1dewHFTn2VgXOrLzcbIdnVMkw",
                "Z26Radx1W0oP4qW0hoYFOEeFyMP9K2zGvZ0SabpabNxlCTP9eyQR2k7DQhyhjQAVcBf04N9tfv8epdJk",
                "dHDWMHAjYeOLUCF3QJ1NqhFWPvwmnoovC9EdcXI2niSKd/ON2zHCW+NfrYFjWnxvdLo2x2uN/1XSyfsG",
                "bX91Nrk2x/c0dB+/Pc3W6dB3EZkYPYj5+AHp/DDuJu67+4yGRbiP3wi4au0B1cDHJ0v5eJJS085HmjNK",
                "ljPezg/HlXCE9nb+iDI7f2uRO2toSYGzsNCdO6TAtPO6LalH8JI6h4EjcI5GouHczrAt0rlKlZ2XJcz4",
                "C3R2e1VdmmJAY3dElAhG4hS+BNAIfSWCKFU3o4PDJ4KXiEQwCo1UmQi8N2fbqNLV4kvVJ2udCEii+ETQ",
                "uH2W3zLov80DicD3RqeJ4Fsm/r/NWW+zeonAN2j7LlEi8D0NNRF0OpSSbqvstCIiN2AfqUsEnQ69Igzj",
                "qgxRIhiFJ4KX1CYCSUDDlQhISpt6Iog8Z5QsZ+/QcDSuhLfxiWCetjetv6soEbQPsOPwZLlKsnLyCpwl",
                "mUWFhXm5ZjjQbXV9F6+uR2l4G47U7Wg0Nzts4YBVdNurXDgIBps57R9JmvhCeDtO04sA3cY9adWByUci",
                "k68rwRJ82sjUrYNtwoumQFVbE+Oqdw3PsB0/Mzqj8/kVE2vHYXTOnmbgi0QmKEa3cwdVTsIKiylqxIt9",
                "KkJ0Wv0bygT1AExOSQl+VEEAwlJUegnUfajDaNA3NgWWxpeyXnwbb92A7TV1saDumYo2wWCb8BIJs/Yu",
                "3iZIswkvuFm7SMNiPBS8CLjl2hOqQSjoFSwU1MPU7BhEGjlKFrn3aPh7XBVLkJogKpkPPlKUDzo7UlJG",
                "lThyc/OKcrOcw5y5hSU57uHOEo9ziNvMCDquth/h1fYVBpbgWJWi33NfhC0jtFe0L06cERoLnPH7CV/V",
                "9ROs6i5hl2XWYA6RYPZNmrcgGLMvA1TCXdZDu6GbP4k0DWk0OkfPMfBlImmMQaXcIXU+Tgosi6syRPuW",
                "Kpc0ggLGp41+jUobAIFA2uA/1GHa6BPbO3DLmShsCBed7RWFDf5ZqGHjg52VqTbhJRLO76O6sPHBzkvC",
                "MC6l4s7vBxqOwcPGy4BbqT2kGoSNPlJhIyipTT1wRB47Sha7gzS8AxfHsXzguKztEqVTigJH3DDHkNy6",
                "TeZmwNBtkT2FF9m9NByLYzQO3cH9GLYNBx0UXXUnDhhygJmNh0gStI2G43CCNgM0lvtKD3Y/0W/3o1OY",
                "jM7NkwzcTNj78WicihPMfM5ps7Ca+1/iRsKMEA3TdXL2ngCKt/MDg9r5FgnNfSSdsaD7L4ErH+vQ0A+I",
                "7QfPWOLvJ7caXBmy/a06T3/lYaim/sAn69Jsj6X/MyLTuafqTP2BT7qn88MoI6ZzP6XheNzUbwbcQu3R",
                "1MDUJ0mZeoLPpm7iI48ZJYvZcRpOwCVwIlKzMV3SxJ9TZOLbEeg0rf0GwJBu/hxeU39m4EScp0loAreF",
                "Dpeb76hEqLoHdfNG33dw2VIOQTxMEv/onPhBf+FlEGW3uqiiquXnBxcnbebSSqkpgjbNoKg1GTOpKoAV",
                "eEE6yK+wHMg3JtbMFm90sCs4idSIlpLAfw8NJ+H4bwFoIve9HqJIZ6koYm540BtA6xm4hcgkZWgSd1SV",
                "8bNfl22jatqLNzzMDHUSt1GZBNv40Kd3IzY+DGDXMugppnbjg/+NTjc+rGPi32XYGqbexod1DHqXsR8j",
                "Nj74f5RQE0rxLCTsDW0TkQXp57DrpdYIw3iU2KL6DQ3L8ISyBXBLtQdVg4RyfYMJxdwAoRveKFnejtLw",
                "TlwZ7+Kjymxto8oFRVElUcRQfo6jwGUGFd0W2gt4oV3IwLtwmiajO7m9YVvX1EmJXnVrIKiIOTP+cqbt",
                "otTgOntTXWqIzu3RBItv0HAyzuIrAN3FfaeH1NBROjVEl9IZHZ91DHyFyAxT0GS1Nxj0E46G/0l8bNL9",
                "ITqw3o3MDD6u+MTQt1Fbpd9h0MW6w5N8b3SaGN5h4i9apbZK+wZtf0e0Vdr3NNTEsHEDl26D3S5E5FCb",
                "C3WJYeOGDcIw7iIc3AUaTsETwyuAW6I9phokhusakRh8rDb1vBB52ihZ2vbTsBxXxQo+L/yk7fqkbxXl",
                "hRbZI5w5OU3okjNjxoRvRSeLV+AQTUXl3PthiwmdlYhUR3FMkMbL+Ong+Gxys4P/drOpDvWbHYAhNzsQ",
                "hL5Kw6k4oVsBquC+1EN4SPCHh2iUPaNT8xQDtxKZ4W40latRf24NNUacGR4I9RYpucyA4cRHhQGNPVXp",
                "LMBOVToL9Huq0lkgc6rSWWD/sP6pSmeBqlOVioqKImLeviVPVSoqep9Yl3KOhnfjUWEr4OZrT6cGUQFJ",
                "RQUMUfNIpUhDRslCto+GlbgGTuMTwr+1TQg/KUoIsbXgmPcl6LqQ/iSau52GMzQdVXKXwrZ94Wol0tRB",
                "OiCYdyXoCJ8tNJyO4/MqQNO4L/Tg3tsQ7t28J0FH0Kxm4KuEeZ+Bpqu87sxvj8SLhGaHaou6B3fvtZck",
                "9Elt8MqzC5b4z5uxc6xoSQxm3+fG6Ne+z42Rse9zY+wn69v3uTFq7XtE1mr8VM++E3OvZ2k4A7fvr6o5",
                "q1KWTw3sexd5+25ekaAHxihZxk7Q8B5cBO/l3fsCbdcDXVbk3js6srKcBQXuTHeOu3BUyS05eQ5hUVnT",
                "sfPGnO+/jJfW2c3gvThUM9E93Gth27/QRdEyxnrHHTUKt6jfxPDWPasGF7enKe02McytktzE0DIzapcj",
                "Ef8GDtBwJv5vYDtA96rpnGqXSZICxyc1AaE1OkWbGLidCCmz0EzubXUm8IhgAvuJOwxzQzV/A2VPT5Ln",
                "Kzmlb59GnZ3kY6zW5r/XEm3Va8PhvZbxWwHrjauXWHyDtp8WJRbf01ATC0XVpNs2buAiMhd8uS6xCHLO",
                "D2MDMRd8iYaz8MSyHXArtIdVg8TSW/IQJXlgm3qEiTx0lCx0h2l4Hy6R9/MR5ldtGxBeq6JJv2xnge86",
                "kKL8kmy3IydviNmH0G259VqxcruWgffjKP0B3cf9I2wLlZASxeparw8RBDLjL1d6sso8m1We07/S8A84",
                "p9sAul/NzZbahQs20PDQkwQK9k/ItjbhpannC7HgbSPyxR/RH7jD6izbDbxl432bKF/MD9Wq9ZRtgogR",
                "41NF8Auj4xJa8Gy9Q7NbGDQHBoIF/+a9ljoNFluY+PdaSm2T9g3afl4ULHxPQw0WqYuT04XrESIyTe2j",
                "0hcsUhc/KwzDTtzb9SUN/4gHi22Ae1h7SjUIFt0lWyFiUpt6nIg8apQsakdo+ACuiH/i44RX247IPEVx",
                "oq07N7+osCQrb1i+o9AMFHqvr/Os5Ekkf8Jh8gL0APdx2BJFV0VXWYoTRXDOjJ8pXjEzRRBU36Khl7CC",
                "OwH6E/eLHkJFB3+o0JkQmrFCjqUXGLiTYGk2QF7AHQlHsKgO1ardKBcsJDDjo8WgRkSLs75o8X1MFESL",
                "L6ImWswLHi0u0HA2ziraCbhHdJktekhlCwlYzXQRadrk08XbNKwilHEO0P6w2EWK8kU75yhnSZYjx53p",
                "qd2Vb0YMndfaRXitfZGBcwii5gJUBbgzYQsZSarOi20QNuPnjBNmzgjC63EaziV43QHQHFVTLNqf76o/",
                "RTSzhhxPmxi4g+DpQYDmAu54OLLGw0Dr816lSUtO6ZfStOLGhaiJG4uCx41vafggETd2qD3xNVxxQ/LE",
                "V2lezcQRaeDkE8ffaDiP0Mf5QuKo0jZxLFWUOGwCRfke5y1OjzM3y2kGDr0X3KWigjufAGoBQPMA90nY",
                "AkeyEtW6RipwBGPN+HnjSzNvBMH1GA0XELi+BtB8wP2mh8ZGJyxv6EoPzbghh9NGBr5G4LQQoAWAOxaO",
                "uLEkVOuWEixu1AONTxv9mlbauBg1aWNp8LTxDQ0XEmnjNcA9qsu0wcmljXq4mmEj0rwFDRvVhDo+FIaw",
                "8aKysJGZV5Sb7fCMMndkGKPcvigKGw8RQC0CqDqcYaObqrDREGvGDxvL55hhQx5XPmwsInDdBdBDOgsb",
                "utNDM2zI4cSHjV0ETg8DtChMYWOZ5mFDEjQ+bAxsWps0/h01YePFBsPGw0TY2GWosCGJqxk2Is2bfNj4",
                "Kw0XE+r4iBA2Lmu79XuLorARl+0eNszpMUOG3svsFrzMPsjARwiQlgC0GHA7whYyrlEiVp3r7fyWYcz4",
                "4WKNP1xA/5lR/ksqGnXxNZhkxPRAcPgaDZcQHL4O0COA+0oP6SExsLE7KgXO6OQ8xcDXCXIeBWgJ4Pao",
                "uxisrXD4zsVMUVB4POQLwWR3cuNMJaf0D37XdYuE5j6YjtFo5iVw5WMdpoMBsf3gMTp+JuvFw8GVIdsv",
                "12WDKw9DjQbbS1uk21IXnx8cCau2pS4abC99SBjGWOKMns9o+CgRDV4H3ELt4dQgGiRJbuDGAW3qkSDy",
                "nFGynP2NhksJHVwWhv7DNmX9h/y8PE9JVl5uttvcXWGEwrpN1H9YRgD1GEBLw9l/6K6q/9AQa1Fwy7XZ",
                "fwiC614aPkbg+gZAy0D47lIJpf8Q3XpodII2MvANgqDlAD0WppbDSs1bDpJs8Ymib9NqOVRVRUvLYVuD",
                "LYflRK54w1AtB0lczZZDpHmTbTn8P5a8qM9a0AEA",
            )
        private val REAL_CPM_RENDERER_GZIP_B64: List<String> =
            listOf(
                "H4sIAAAAAAAC/+1deWAbxdXXrCxbHsfxWMkSx4mCsAkkpo7jK3EUy7qsxB58YdkJgQahWGtbIEtCkhOc",
                "lg8XCIFwUwoJLUfL8XEUShICFHJxFQpJW6AthdKWlqsU2i9QoKWl5JuZXUmWLNmKJGMldf6YrFc7M7/3",
                "5r03772dmYU/mA6ze+wDgs9eJdMKsDQw5BUW9Xk8fS7B7nX6F/V4BiqcA95FZvaMNWAPCKqGOm6+TPYv",
                "c1ktzM2VyWSGX/3qXb0qtzB4WayaTS5lZbJD+06QsX+H9pXk0nuaLzbesA8/AWCJxb3e6fO4BwR3oMLq",
                "dTkDGou7z+kWNCvsDkHT5iEFqMqBCrVMNrxPa4ANsYD5vfYeodfldPcJvkWNPvuGdp9D8K30eQa9nYKb",
                "XNrXuQSGWdsEa46wBZFWpPzysPQPUCx6vCkLohUel8uzoZWgNAd5h2F9TN6dv2jA4+m3uxf5h/yDzkXG",
                "9eTxPsHqFQRHl8/ec67gEzuCUKaSFctKCNu0NwJYl0hjTYLdYaUUrHC6AsGGzoClcC5U5FOwKgVP/ytm",
                "bNSXKObT//iHru6pVxXCLNqjivCY3S0CxZqx66lk2j0gMSpDwAikdU6Xc2MQ3AXw5LE7oUL05nOHzjGo",
                "EnhQs4o8WJwbFrJ5Y9chg6RLBH6nYHc5+5hwkjEedNkDHgl/LuGZ2AH+cw5UWYf8AWFA02F3Cy6/ptPj",
                "CVTJ8M9z4MwOS6e12dplaeuyNbc1NpuNXe2dRKBlWj1cMq4cUuln3bXa3URWpK4VShO6n8MfZcMT2dPl",
                "AZ/d7e/1+AbK4/Q2DUJ+aeXpDeqDB6bt014CYncdTTwjxuh2DoSpxoRimZ6nxXxalI1ot5YyQ0cLUwFj",
                "DOM743orLbppsZYWAhsCC1ycCAprj51ywzzoWy+oCpVy1m85LViHxCIkp89K5ZdyBi6lkTCjH3L4PTmc",
                "s8Hpdng2lMcd7tQNVyowG9G9HN7PwQpSY5Hd7fB5nA7KXyKzhMXn+xbFwl26rJpAL1QV5L9dN13H72+t",
                "1Kkp37WNqdKi4ooA4Uiy9GQrtwG0R469cKF10NdLnmcGlFQpX9XZfGYsWtamrHMWdB8HH1/C2umAZfGm",
                "R/a4S/D7zZ4Br8dNDIeqBGroL4LDSfRokZX82hNwetyMMVIn+EpI7MVmCBExN/0B23qnf9DOrCW5/QIH",
                "Cx1On1jL7rKxR8j9mwDM9njt5w0KBJIDnhAP0kpXoFekQg+X93h8QoUIpSLci79C7Nfu9wsBf8Wo3hb1",
                "kTYQh+dDOED447L5RAvHx0a2FcAcD5nfXPahTIP2Qw7meT1OdyB054qRbDwrEazLYG1CWEf0E0R5fBTK",
                "gmg0V0awbtLhfJIFod/rCd+YEsY0Qrt8JNPWJoKsDtYkhCw8ZkFI86IgTY8a2C0RbJpkMA9wpLiHiJoY",
                "kUTaw0tHcm11IkCr4KJ4QEd1EIR4QhTEQpgvPbvB6egT6K3NESybTCRT/Jri10QiccIZYovl4i/lXud6",
                "8uy0EpgvG77FTDz9GhNuhPkRDxHMlYnlEaAavlpaTzz7jWZ8Aczq8zkdVTklNLzabNa6YHW8Njp8zgFn",
                "wLlesPbbvYLoZAY9tlpYCU+BC0dyoyfokvkraBc2EtgIPqfdtaiHXKk4kwwBfIiDxwXvl3t9wnqnsKHc",
                "3yO4iTDgVzhYEPrR7+0nvUmpCcN+bUtyOHmYX5LH4iXkKAJlBEMR0ADcAWeNgiGxVerRoP1aYtxVsEAJ",
                "3whiUCYaXDWJD4lnv5+694aSgvzr12eZ+TrDbLO66AGFiUwGJfH6aaH1xW6WqqLDPlJsbCynV1Uw/7cF",
                "zQaeFoZh5QUe1cXaTYD+cjrAv1HAQn/QK7Z5BgPEE6eqdxDEDu1ov13C+YFBn9Dh9Ar06UiW3gDgmXC6",
                "hQ266G4LDpWsWAWnt687h/zll2qjrDJOCbQL4ckjpUSatCQcNp/gdZHwgMmIKUsp08pgLSxsF39tMrkG",
                "xQiHtI/gNPZ3qPWqbOVwFhrOEqtRe1Mk1WPBgd8ZCAEn5F7OJSdALwH4PIBPgkhZj6KiJ9ihKOsaON0T",
                "wYriKNao5sJp/SOIKY4gTTUbqtaRP63EjLR5fAPMnjjmw1fvqFcVw2meIJEuj4+MPBOK6uqdDSo1LPCT",
                "4D7QTwJPr4XYFn8ZzD94oEXH37xtdoOogUz6nwVw5kgWj2DTfSA5NvnhuVCAJ43Bo34bpSlRBo3BgjAd",
                "W4ohCmdnJHNKI2r6HE8LNS207YnY9jK4YCR6sTV/Rah9yVwTs76OTBZxmSTFoV3BaqtZLbELDcyDuaxB",
                "F8uRwGyfh/5QTGyOv8fuEvBjRVGzxR8UwYdoaExTuvnin7bQLLAK6o8MjNHvJazuZM2IwGbAaONCbHUH",
                "absFlse1gUNkWIkEEDl0OR1BOZjDWjIs4S96ec4eNbtSPrjBoL7o5e/q8HYAlRL681kqN2XgOVIaEq+m",
                "M5qazWhJYQYyzSO8TsSMlvDXbms2qHUU831hzENpw8z4nAqDNUv4wUiwgSgGb0wbWHWYwaTnh03JYiZI",
                "v0ExM1YzzN+gmF/OlYSfCfitxCljfzEJWQEXxOvJ5Dk/spNiWESE4ZqnLjAQAzBv07/FPKqYUmuOnVIb",
                "jxE0ORotaI8TFksAh2jml9w938DTQttxBGhPhCWMKfsbeFpQHu9gM7xsJO62lHGLCCXNxncBmBsETwXE",
                "eASIeTgDykZDtKYMsXCU8RnJ5I0hJqtTY/JwlZpeTSSTRfB3hESYGoxTjwDv8ZC6ilSESUtvPhyESoU5",
                "jXIsisKuEIuHGIt5QFl8+PAGQxKQJRGeMO7ysbi7MSnuEsbWqCUrEYZKWZwe7oooDylHTPTErD0EYF7o",
                "b2bajlyOH5u3yRCU4+GdBL5XBw0jON0KTcnA7wrCijJ0TwE4bQTkiTd2nWnBH2nwooiYeGOSXiJEYYoU",
                "nqGkhIeynKcSRJr8Z8iyUAlKs/CIfN8TwfeJtTBpYzkfj+Ubk2e5TPZ8jZryPQI35Xv6WM4gw9lwRvOA",
                "1yf4/RoxNg+u7pDBAJwurlwQHKF1FGcnlt1YVsclufwE7r1UDlF7h6Xt9E5bd5upvbut0dJIbOEhOVSa",
                "PINuh903RLMt/MEDB/ZpNyW4FiNYM+L9YTONOfTzL96P9eW0NZaSWUuu9FcBAsfwHUCudbew8i5AomH9",
                "A4CSsYslSfaQcr/+WXZfq0/yhXcOG4UmgM+C+SYP/VGziuUCw6m2NLzYVeZQH3/aCpfHE9U+8cs7j+i9",
                "KRHSaN62eZz+ISo1oafShPntNsibPe6Aj6gJYQx7TlrGMRGo8f0cnNa4QXC5NOZBn5+wSlzj1Nvbu097",
                "RZIZji7YBpvgivXkpsdXIVYm0lnBQFS47bSaze/xuPsqHPaAvaInRG+Fg0Kx+YSAs8clhNKhNIWRhoUL",
                "ya/Q+Iu4QoOGuCqRWyvtGwVNB31FyZJBp8LaRFSy2e0dDEi1xLZVyjzV9MghSHJBiiJqQQq+B8DjLEOC",
                "CLWJ5u9HADbCqkT6oHVZVRFtnlJBl4RpZVuADDdDVehnKlQ2k90vjJAfvADOiHzA2k8TqoWigWcOGZtT",
                "DXgPB1EIahikPjGu0lqREpitBAgUKfA7AOazNoM/jxTv9LA5I+Ty4yyoaLK7HX6WJzkSKzEPzh21VI40",
                "1EGmTTqd4ss5qGwRegP0Jls9lCzUTySoZ8JC2hZdqehiF3b/uaThdFhPgL8OUajx9lD+fkXKbWcpOQTw",
                "lRzM7aTvGSRmmJJkBlR+Kg/OgRPIDvkEsiOLsONeorR0WaWotaOnj01JTh908jBBQzLTR8ZOHPcCOCvM",
                "q3QZ46wJMcYPACKTIaxhlO3JznHHEaCq0bKSpAHm2MJZ9jZPnOce4OBMaqI0JueA3U38vClZHFMWdwHI",
                "R/IrPMbWZMe4WJmrOi72OKRrnD+aDgtY+yP8Y7oI8gy4PKFVxP1DfifpIlxbSrkrgWqWxJERLTOjmYb5",
                "7q5sWBhutkVwUOUjqEsTeNOWjmmAGOofKKJelV2ngNMpvWFc5OYXxIfoYKspikhIfvd3n1nOe1TyerVH",
                "dfFyEjcNr3nUgPUwa6XP6SVsp888ccLq3fwc2Zu71UXDu+vpSyXDow14DsxusfWRp6oKxWfq+bmyN+vp",
                "M7txLlS02FpYcDuNPrfO4xhibzP6YV6LZ4PgMw0GAh43uUXzAuvMy5/gv/X0Q1r1im1X7S4pyO+4t8PE",
                "L/z9drP6m6fsMONqmN9ic4WrsUrfUW5Zzvtcp9Wrb5u1rJ5Usr65zcwb5j1kUP/Is9OMeyFsFdyDER1V",
                "vnX9bv6B97YuV++e10I72jqnycTf0bHTrF7+NqlTBae12AZCtVid9we+U8/fVndwuRq21dF+mv5zs5mX",
                "fbxJrw5cQerYIezqHxxYZyVm4FzSj5rUEcqu3s0/MmN4t/r27XftpotWZDmkzvAOqYtAqALr4pt33ljP",
                "rz534XK1vu0FCuvjT88282sO7TCrLyMFPhvmdPmcfX1MEWiFf9fc9AT/Kjx+j/qiC39LK0y/ZaeZ11kI",
                "HS82E0wVMJd0IlZhFbyvX7Wcr7jjeJ368MmvMyI+IRVkhx/Xq2X/3KWnw9Lt9UYNy3TFvOX8c2fO2K1+",
                "+8vXaC+/+Hcl4VYOGZbNzwSHZTBcjVU6/jT5Y/zsLw/Wqx//Ywftael+ul7nlb0G9a4XSaVv58TRQaIr",
                "p8CFY+lKxNYArTeRV9inwpXJ2PTwpc1FsNI33IhL3ajjG6M1dFMsDT2UgIbqRA1NVj/zJP1kOgmDGkqu",
                "eyP0c+K00zFSOydMN20jdXMiNPOskGZOjF72RujlxGnlLo6MO9NKNpUTXeyCSxOZbcXnI52p2fkHDxxo",
                "EFebVFdX6zXgDNk62Tkynyxdsbgl5flSjv6ckwYf6zcchIxvLcQNF92U3kTskhHqkrFLLtqLZI30cFEi",
                "rGTAxE7ziQeUU6xgd4plGeGjzo6yh9RvoPDoC0JOmijaBLtvhVNwOWL4+5uT9Pc7IIaN0JTMGNB14+La",
                "8ozz+B8F8LgoloVd/q5kXf45ypyQ4xw9Funy+R/lYH6rZ9AfmWtNaNcgqxY5uoU8EQ49W+k2nxqgBXL8",
                "a3CMpgQfJGrCklVTYXHiShLFsLQoCaRKEnMk0qUk70+HSOwgKjI+M7XIeK6SUxUFmTIBofFtkx8afy/a",
                "8b5CAQsYwRGe96fRnvcTvOnsd5ar7/5uY8jzrgvFxtSv3kH9akB97znE96ZOd6dtZdDp3kEdc0Cd7jnU",
                "6SYRaZNnQIiIsUxvXV/P76Re789Fr9ewhXi937iWeIyyokIDdUo7bf2hWqzOLY8ST3nH5x8uV8+5cCl1",
                "5jR/I17vo/2kjpM6pTFj7Dbz8uX8BdSLrxK9+IcW1pn44QpoUA8P3GmgbmPnKC/+ZwdcT/B9uxvq1Zee",
                "0U57evxP3zPzzl7iay4/THoi03VnOMzvDIf5sYLic8qurucPUtf7ezFc785o1/u6lk27+a9vvWC5WvbL",
                "X1LAH40TFP+r5qbl/MvU+d4qOt+y29fpIp3vzgjn++Kzbn+C53fO06nX8m8kGhR/njXvCf5F6n7/RnS/",
                "t955iok3fHIlzap+IPEx2v2u2fSslh8wv1CvvvN7a2lP15x2q5m/8U8/MqvfeJnQck1OPPU70qj4vES8",
                "zxbYlGJU7JM216UnLL42WjsPZ8XSzvcT0M4lUlx8pLrpGKmbE6aZMaLrCdHLPEkvg+F9ZzC8Hx0Qp1kr",
                "owLitOtkjIB4QjTyEY4QL2rkRETE3DEbEb/GwTyRcUdHSMwdRSHxdi44TUzFxImy88cAzormWdjf707W",
                "35+rVIZ95omKiveR2E46cETT5Rns6Z+Y6HjasRsdv5cNYeNqS0uLrdXS1i29RlyZkJEQ7P5+Gqh0+ymg",
                "DrqZ0elxm/uF8MlZoQWst9NFsk2mlM578aF/cvhRBSyOPmMpCv+kH2N0B5f6MUZ+9DmHX5DDQukYoyga",
                "J/fwogD6mKOrDU6Kd3hRGG1pZdUy6cyiuc9VNvDd3d0NmXJm0U6A/iknDtPxsc4sCpOQ+klFg+jvHN6f",
                "TTzvNuOqlFtrQHdx+GMFnBmtB1LrlNc3b9vWwNNCTQutMc6EF72uRvDTnbRt9vXBUwAem6domM+aOCYO",
                "B9Oju4kJkcM8SatC4zG5+mRAd3D4dg4eH0+fKM7SZYvDh3818AcPTMsYRboZoN1yfDacHUuRKPbUVciI",
                "7uTwZ9kwv9W4sq15BT1DrLm9LaPnq3XoDQ4/rYDqaD0dTcIxoVw96HccfkUOZ0rKNZrMydUyB/oNh3dy",
                "9FSG2FoWAbi0snKppG9P/+FuHX9Dw/v1maJvDwF0SI6dsCSWvkVQkbriCeg1Dr/pgNOkbTkt7WZjCwm1",
                "XlNChVU6heWgEhYavV52PCVNSNmdbhaO7SL3ReGnoVp5wO4/t7xmInTWwHTWIOpsQ9LU0uW8p+HD2fC4",
                "aJUdAT1Nupr0piGlQ1TVVMjk0Bq8VQHzJU0dQV3qKpoKLgU6A98NYMe66t4q+7plGmMPiamdgaFOocfj",
                "c7Ag0+p0n6uhGuy3D/gH3X0hTbZ7vVSJacKxYlErEcFg5dKldUyNL/rOWw38ebqrpWkzFaWYju4E+FUO",
                "to8HZJzfo3AukQxOFNL0GJxU6C1AdwHcD0+IaW9GkJC6uclHdwP80Zlxzu+9CkK1sbJyjc3Y1tbe3Wa2",
                "tNIzRVuaV1lsnZaVme4QnIVeJzGdEpZHW5fxSKK7WY3lO/ewE7cJphsbhvU8LdS0mJBNfOk9kjgEvpYC",
                "1tHiKz2SuDWxLJbV0xugi/NbPD3nirTMzKejztMTkdR0hhFjoeT9rOdB6n6WDf2WwxdnwRMl6z2+Qkyu",
                "33U2epmjB7Uti+d3jUkA8cNqJLP46x/7lvG0yBQ/7EcAfSjHQ7Aqll0ck6rUDaUd/YrD7+dCnu2SNnau",
                "sVktXd0dtsZmY0v7ygm3g6mwbQtAD8jxA0o4P9oQxqNlygAeawYwFfm5AqAH5fivcjhXsoDxVSAdKfbk",
                "gV4J0I1y/AwHK+OZvpjISytrgqHnsqtLQwegZITJewKgb2VhHyyLZfJiUrNWGojk+7wKoHvk+KVcWNho",
                "sZ761dq5VEz0RjQsxzcr4QmjXljEIGPKxE35eGHR+Qb6lhz/QQ6Lgq9/Ygr+5Pp130RfcvgJDpbHfQsU",
                "Dbq0sro2Uw3bLoD+JccD8KSYL4OiKUndf7sAHebwzlyYJ75o6jC2WVoy2pq50d84fJESzon9+jVIALVj",
                "hTdo96q/MXMRtWMXn1Sp52mhpsVRYMdC4GspYB0t/jvt2A/S8Brbg/6PwwflUBXxGjss7JNrwbzoLxz+",
                "EUdPmx7rPTaDW1pZtSTqRXbGfHznYYD+Icd9UBP/RTajIXWrdR76K4f/lguPs6yx2MzGlmZTJ3vPcDSE",
                "nRcBdKUcb1fCk6ItWFxippyyqbgzLEAXA3SVnJ5Fp5as2RhKMLmB5yUAbZHjn3CwKp5liw2dRJ6Vmeqg",
                "PQbQYTn2w1NiGbnY5KQeem4C6Ao5S7PRHjo6LSssnZY2s+VosHeXAnR97DRbPFqmzN2UuQvLz2aAvh2R",
                "ZouvApNr7S4D6Nqx02wxkRNjV5Opxu5RgIbjptliUpO6rbscoOvk+He5cEZzW0d3l83c3tph7DoaEm3/",
                "gy6V4zuVsDTa0MUmZMrMTaXawsJzIdospx+MLZasXDzhn1wbNwzQJfIxPxgbA3dpZXVdplq4HwP0Jf1g",
                "7IJYFi4GLanbt28BtEmOf5ENC1ubOzvb2VK2U63mTosls5eNLEZbOLxNMfo1QiwypmzblG0Li04luobD",
                "ttBbhNhyn0oHVWgTh3+dDWdITVvXWLssrTZji6WzK6OVSoXeBvgWxWiXITYhU2o1pVZh4ZmB/gywPeQx",
                "xJP9VLqYiV4HLO/Q0U6aNre3NTYfLXnW6wHaFTvvEI+WKe2ayjuE5efbAD0SkXeIrwKT65PfAND2sfMO",
                "MZGXVtZWZ6pXvhugi+PmHWJSk7pf/h2AHpbjjzioGrnFxLaihXQX+tgLfgtAZHGvd/o87gGiwMFPgeiS",
                "FFVF6rNAIXoR4FaYbT13aJ3nfMniH4mlKoQFYt3wV0hcsIBZHGu/nUg+pZGdUNINlSt9Tsdpg3ZH+Nst",
                "qR8rMyu0q8faZVxpCTP7IwiLO+x+f6CfVOvr16xmiqiRGEAIvQFAY2LmM9SI2EYED9upJd3H9/a+u5cM",
                "x41PldBvz16pp9+epR/laWmopYV2Zv6Hp9XpeR1cLVpSdgqAhdqsVnr2a7dHJV++VjzLIBeqRuOumiF+",
                "TXmfmvZE2Td8dZo+raM1Jil/uUoQ/PLB1hx4YgxmG9nXl/2a7maN0d3T7/ExMj578PsN6jtXn6djUnCj",
                "Cb+eTYbKaLV2NXW2d69ssq1ubmtsX20zmqmOWlP2Q5agbRz+TAHLRs3lY/c56Tsaf5oGP28pupnDn8vh",
                "CcGJaGyaJ3cxQx26icMHOFgbdzKKi760LpgI//3v1uqoCciYrY5bAXpcjtfDipgTUlyKUl/psAxt5fA5",
                "cHYMzRS/1cWOA6GfTU+LJVHi7Tlw9mndzWa60MzYaW6ymVvarRabqburK8P3NZ2BXmEHEiyMthFjkzPp",
                "Gyk9qVuIM9EvOfwPOdRIFmJsiifXQHwd/YzDL45hIOKCL62sDC4LsOc9Us/TIlMMxAMAfSDHg3BRLAMR",
                "l6LU7cNa9HOOHtw53WqhLjFd9R46nSWVdjvRMxx+TgHnRStTjH6OiUnWip7l8K/lkJdUKAadk6s3Xegp",
                "jp7/XhZPbyIRl1Yurg0ri46f+9xHGbNQ8H6A3pWTWa00lrJEkpG6hnSjpzn8160gzl7e95RwjtFstlit",
                "zabmluauNTTiM3Y1t63M/KOo1qN/cfjuHHjKqJ28YxJ0InHgz6j76171FsdCmucqE1bqeVqoacECkx+Y",
                "05yaCvVXS/vQ0eIrTU0lb1w+TsP64w3o3xz+jxyWBPfKjiNwk2tpzkefcfjnHFwad6dsfPilldVVktlh",
                "H5y+/76yjDE7jwD0uRxvgItj7pONT1PqNmgI/YPDb5wElS3G7jZzk6Uzg43KCrSDww/Nh0XRRiWEnX5w",
                "etf/7NijvjX7wn1pthOhdpna6mjxldqJ5L8hKeWcm7iUuL8S7eTwLXKYY3f6Nthd9FxpnCQklfJw8F8o",
                "yXMbgLnnDdodGtY2DR3hq6W7tcYj+EQ4D2dAGRG5gwda9DRjrxejz3R8xVJeBPD3Aczze/sFnxDGWF29",
                "U69dAk+Jm9Rkz0fCzIEymkHQa09LGViBkkNhVhKM1x8PCyRLPkKh0xF/Z6UkPE3oIY5+FTWrtLJ2ifhC",
                "i6jO2Xv5l09W08/Pu548Tsff1f6vjIma9gJ0SRY+M3R6aoRFXptiQv+7AO2VB9mxTGJHb292iB1/8lgz",
                "ix37ANo0Yez4HkD7guxYUiWxo7q6Wpex0rEfoEsnjB23ALQ/xI7aMDv2ZCw7ngRo84Sx41aAngyxoy6s",
                "LA1BdqwdMmQWO54C6LIJY8dtAD0VZMfSyrApDbHDveW0zGLH0wBdPmHsuB2gp0PsqAlJx6UhU3rSw7My",
                "ix3PALRlwtjxfYCekeP3ADSYSKt9BL/boSE+cnDj4yqnsOHMeMFT0GNYS1hZN/4kPblrC34A0GvpoLSu",
                "cvz5d3IpvQOg19NCac34U+vkUnonQL9NC6VLx581J5fSuwB6Ix2ULqscf0KcXErvBujNtFBaM/5cN7mU",
                "/i9Af0wLpUvHn8Yml9J7APqTHN/NwdLxyCldtoRl3Lq7FXp+7nPfl9ZxpSMkV6Q4D98H0FtyvA6qY83D",
                "oeFIMb3WjLZz+PeLYGGLfUjwaapYbl8jfb1uIpZtpnmh4yq4LKHmAoR3wQQg4Ujwm0UymcHAk2I/O2V/",
                "Pmn3yXJSGPEjeTDP6iKsMLV3dbW3imu2hu8x410KOCe4Cp+eBmg2drC1dO2rLJ0txjUZ/dLjRHQphz+R",
                "j37pMTZBNI9Ej1NJc75yMPeyveqXdZrJylcm/17jh2l4rzEfXcHRA8CjNnbEE6lUujoJfQ7wO4rQrqmu",
                "dqO1y2bstBgzWlqL0LsAP5gVd9dUBBk0sX5P6xX16o3HnqAmn1iXBLVJntJAzEYfAHwXiN6EFCVG6cjh",
                "ZqeEsxi9CfAviiTD3drc2NhikQz3DjPerIT5jc2trZbOo2ELxtUA7ZDjV7JHf3simoZp4tLYKvU3qeRf",
                "mLTgW0YLftWzhXvU/a+WJyb4GWacU+H+NQDtlONfhb+EMVpwJtfBvRag++X44TE+hRGBuLSyNvjpmUfO",
                "zmvgl139Vsakmx4H6KIsfE7sT2FEUJH6RoXrAPqhHD+jgHMlGxb56vpo8OHK0eUcvigLfi3OrBiPIrr0",
                "/IuXrtutXqJYtI/aRMPOdC9UeWXzGXvUxVeUH30OXTq2FC5CV9PlxqWRk2R8AUulrwp0EYd3KOBxUi+n",
                "Wtas7DZ2Nh4NR32Uocs4/Jl89BlucWmhonv4pevq1Usl0ZXt16dZdL8MasZ/peiegq7isCN0KNoYQpVK",
                "L19DhwHeroBFUvur2lu6Wy02k7HzaBDbE9AnAP9dDhfEEdtY1ES6ZumV2CPzzY616LkEfQbodx8jJTa2",
                "RKXSTyn6GOB9SqhkIUVXe4e0ee8yI75XCVVt7V2hD3UdBQcAY/QYhz/KhiXRMhyTDiq9V16m3DMRuR/D",
                "R2UN6g//seDoC6l/ItrbBal9julU9GOOHlk0WxLgOIKUjpg6NaAt6BEO7x3jiPDRyEuXLQutnl/SwF9+",
                "mVKXvqR6qtHGvQC9LcdueHKsaGM0LamvX21Fj3L4zRziqo38qh89ipwukk19e2cv+hOHh3NiuFPx+iuC",
                "dOu0YV/4u3sGqnAGfTqTB2IXtbRZHS2Okrnp0jR8RbMPvcXhv4XPmB1j4Cd3sXo/epPDz45xxGxs5KWV",
                "VcFkwtohTQM/97nCjDn9YAdAf5djX+wjZmOTk7qKO9EfOfzrHDgzqoOOFqO1KeXWz0EfcPif2fDEcRQ8",
                "2BtV76Y/aql6k//0PC3ojTSrN+2iljaro8V/kXqfiz7k8J/lcE5s9Q4P+uQqtwu9y+EnObg4QeVmuIlq",
                "Bw82uf++sgZeeeIHGbMFZTtAn9DjBhcmoNqMmNQVewC9x+HnFbA4mMzp6GgJ9nE05ArnoPcAvixr9NEP",
                "Y9IzzllOx9a5S8nbkr1pSLzMRR+yr7JG5QzjiFkqPanRWwA/nQVRsA+rtdnaZWxL/ZyzeejvAL8vh5p4",
                "QjaipyK2I6WaShX5T8/Tgt5I89xEu6ilzepo8V8kT8ejTwE+C86Kkqe0jbUG/R/AL4ZXwJBWu7qtLOFi",
                "7TYdBRmQk9FmDm/JirsCJg5BUybxK8tFL0BXxlgXE1fQUulqIfoC4EcUxG+POP7xKEhG8+gdgL+Qx4gJ",
                "YlMyJb9fmfweh/4C8LpwdBBPtFLpYxb6HcDDSqik3sGqZsvqjBZWLbqNwy9nj94VPQL9lIB+ZQK6HN3O",
                "4afloW24ETI0uTFrPbqFw/eMsWo7iLW0bmnopIQ3G/jGxhsyJgN1E0BPyOnL1FhhahB/6qGpDt3K4duU",
                "cHrwgKQRWcVMNQSr0Asc/iB79MFIo2iYMgdfmTlYjV6MOLwphjxNrlE4HT0/9uFNkYhLKxfXjfzKo2/u",
                "TRljGx4E6C9xD2+KJCN1C7EG/ZSjyy3QqubOrm5jC13PYWo3djam30Y8/ek9DUFdSdFGVKNvc/jfMbIJ",
                "MaighzFdMOORver8LI5aic7H5ul5WqhpMTGHMYX6q6V96GgxkcYia3519c6G8t7ek1I2Fl+kYdlFDbqB",
                "G5FfiClZqbRfi67n8BcKmNPV3t7S1dyRcoNtaC+H71fAWdHiNLKDY+IQvna0j8NPyuF0aWxGEji5BrwD",
                "7ebw/47h1UlQieWuCi8cqOcvLj89YxYp/xCgd+S4B86NZbkl/Kmb7NPQHg6+NA2qVnhcLs+GVvqYEAiQ",
                "Sn7S8n8AbE5ETo0OuzfgXC8wg9YpEKHtYweuB5sSe/sRgDODdkumZ3xmtkZFN7A+bCZ3NcZiepr3gQaW",
                "pi1hxxZJZp7eKCsQf2Q36HnfVQXiDj3JDj5v0kbfYJZSYwzeqDHh0lqPSl6vo5v66HHgDa3w1Tt03QTH",
                "3rU3b9u2l5jO/fs6FcwMa/uhJTHyzxn0B1rZpkN3wEc4SRgcQfrxjHIvBXa7QU2I0LHzyFU55O40EwLa",
                "awBsTKir9UTG+gSrVxAcTCKie1op8pgxiXohww1iTxVF4FoAamnXOvhqab1IO4UzQK72DtI/rwLUJ70L",
                "0Ie0d4PYKhCNyORxDFn8AWkSi8DigMfRaOlJnpJOsBwyzqfdLABsxA/RUbnWWFweviZWzWskeC8yESOn",
                "MeloPQuloZUu6FhLR7KkYP6DK+17y5n3TK4atJsAXJ4YUKarRMojUa5gG6hlDTxli2oJlTld+axHX9LW",
                "sq2aRCh0xQX5T75g1/Pv/GKdXv30p/1UNKurZ+j5y40GcQIulmlvBYlJS6N9wCs4rF4fgRCa/CMQdcPZ",
                "oTHUB6VFY2Q2XzV7hA5IPx0SfypWlJMbe0S1ofe9lI055gWglkq+tg8aEsFH98Baqe1Y4XQFosWrlDFL",
                "8jpUI66Lc5G8qGo+ZZl2OEFhDvVE2l7ndDk3RvdWx3q71sCEQ1XG0/EulnwfKklqWsynXBBP/qc7anVs",
                "k+4NAJoTgdDqdDvHUtxWJWB7dkc6W0yhyhnRmqC7ZmBa3cAm6MWAWhe9pbf3br0mrwyGxnKvdmViIjLC",
                "hBJwg65RIkKFLSH6uuy+PiFAnGghtqithszUGcgMePO21yglX6eaqjEVz84n+tBAT2Vj7+MNItWUzUG7",
                "vPDrBSzOMZTJqgrEjzAw80YL+AyAhY3OAY04LQo+tsGbTVf3AVgdb493h8854KQzibXf7hWCdUWofuiE",
                "PXDVenLT46sQKxN6KxihFW47rWbzezzuvoipr8JhD9grhPBXQGyDpE6PTxDctl47aX9RD+GLKhcqejwu",
                "j69KpuJMMgSKgAYsyIJvAcivGHS5xAoas8/j92tW2CVSiI2siUdKl3B+YNAndDi9AgESRYsbuiCi7VpZ",
                "u/S8+Koalay4BM6LvmtjsGwBsTkESmI94xC8gf7QM4qqbOUlOeiSHFOWUqaVnQ6wGs4eVccnIaqSwVMY",
                "NfNhaTxizPYBMu8w7PB8OL1RCNh7+gUHG1Q/+9rIOlgc01Hx2P2B1HdR/Qyg32XBQ3lwhmiZxMfFI4Kp",
                "o9KUpCOMok9dNL2hIBr/qiLJcb1VAfcBWNDe0+MadFBfsJkCJAM7C85gN/0klG21e6VGUH6JBs5d5fQF",
                "Bu2uRjqG0g/smo4TUpSxz6s0aNfBs1MXfU8QQ4UnBNHmJBiZEojiArvhvBhYT7VvsPuFJmdffyWh50So",
                "ifuMabC3V/AtRvkMektDgs1WJdRspdTsAV2CzVYn1GxVEG2wWTxOs0Rb8bhYE2yrNoG2Fktt6cZpawlp",
                "K5aoidVNa4l0r4F14yrLiA8KRaiims2F5cT5bqhSzO/tfbdBK/7H/Ek8O6aYMwNx0vgSk+bnKtP8XBV7",
                "7gOOhGRelzOgsbj7iAEQZwPxLJPQHJfOiaGmNtbEUFM7/sRAnhlnYkjxU1jYFj2vkC7D80rqWQA4HUJ2",
                "IZ0PA7dnwyzpK14NSc8nWUqAVuOXAVQQ4M6BtO16SD4d81wa0jEI/RTgc+BJMY/pH/J3O5s8AwL9QNR6",
                "Z2BobYoMzEZn4p+A2IueWWernH4n9ekDQya7z+cUfKn2mIN6cTcsMA46nB7W3SLpyKBU/IobAXpUjq8C",
                "UHmO26lxiw0m/3GYHnEUUyFTjk7HrVFwkpeJPHQ7gB6oINFEf0DaSLbZrF0LS+JZpxb6pFh9qSo6zUyK",
                "jWYW+VTB/N8WNBt4WhiGlRfQU6U30fSB/nQA58M8KgPNGmuP4CYk4JkQjTj/S7oL38lhBhXC7B7mXpLr",
                "P8jhKX0CkdOAb8jmH1zHiLRV1drsXq/N73QI4Zs+8YsQD8hJcY8cwgHCAlfw7k3yoJnABQRMq20l63rx",
                "4ugbldE3qqJvVEffqIm+URt9Y0n0jaXRN+qibywjNxZCJblhPXeIwFQT3tN8BEtK0GO/60tYloxmkDab",
                "RzxamfijVezRPPooLdR58R+tTvzRmvEencYeXU0kikwJ+PdyWDZ6hKcG+JgZYBRtbf4fOHOI/JT8AAA=",
            )
    }

    @Test
    fun parseScene_impressEulerFromQuatClamped_reconstructsThreeAxisWorldRotation() {
        val parentQuat = Quaternion.fromEulerAngles(pitch = 30f, yaw = 40f, roll = -20f)
        val expectedChildWorldQuat =
            Pose(Vector3.Zero, parentQuat).getUpVectorToUpRotation(Pose.Identity)
        val localChildQuat = parentQuat.inverse * expectedChildWorldQuat

        val northQuat =
            Quaternion.fromAxisAngle(Vector3(0f, 1f, 0f), 35f) *
                Quaternion.fromAxisAngle(Vector3(0f, 0f, 1f), 90f) *
                Quaternion.fromAxisAngle(Vector3(1f, 0f, 0f), 20f)
        val southQuat =
            Quaternion.fromAxisAngle(Vector3(0f, 1f, 0f), -25f) *
                Quaternion.fromAxisAngle(Vector3(0f, 0f, 1f), -90f) *
                Quaternion.fromAxisAngle(Vector3(1f, 0f, 0f), 15f)

        val cpmBytes = buildProto {
            writeMessage(2) { // Root (id=1)
                writeVarint(1, 1L)
                writeVarint(3, 2L)
                writeMessage(5) { writeString(17, "Root") }
            }
            writeMessage(2) { // space-root-task-7 (id=2)
                writeVarint(1, 2L)
                writePackedVarints(3, listOf(10L, 11L, 12L))
                writeVarint(4, 1L)
                writeMessage(5) { writeString(17, "space-root-task-7") }
                writeMessage(6) { writeVarint(5, 1L) }
            }
            writeMessage(2) {
                writeVarint(1, 10L)
                writeVarint(4, 2L)
                writeMessage(5) { writeString(17, "gravity_aligned_child_panel") }
                writeMessage(6) { writeVarint(5, 1L) }
            }
            writeMessage(2) {
                writeVarint(1, 11L)
                writeVarint(4, 2L)
                writeMessage(5) { writeString(17, "north_pole_node") }
                writeMessage(6) { writeVarint(5, 1L) }
            }
            writeMessage(2) {
                writeVarint(1, 12L)
                writeVarint(4, 2L)
                writeMessage(5) { writeString(17, "south_pole_node") }
                writeMessage(6) { writeVarint(5, 1L) }
            }
        }

        val rendererBytes = buildProto {
            writeMessage(1) { // ImpRoot
                writeString(1, "ImpRoot")
                writeMessage(9) {
                    writeString(1, "tilted_parent")
                    writeMessage(6) { writeVec3(4, eulerFromQuatClamped(parentQuat)) }
                    writeMessage(9) {
                        writeString(1, "gravity_aligned_child_panel")
                        writeMessage(6) { writeVec3(4, eulerFromQuatClamped(localChildQuat)) }
                        writeMessage(8) {
                            writeMessage(2) {
                                writeString(
                                    1,
                                    "type.googleapis.com/spaceflinger.NodeStateManagerState",
                                )
                                writeMessage(2) { writeVarint(1, 10L) }
                            }
                        }
                    }
                }
                writeMessage(9) {
                    writeString(1, "north_pole_node")
                    writeMessage(6) { writeVec3(4, eulerFromQuatClamped(northQuat)) }
                    writeMessage(8) {
                        writeMessage(2) {
                            writeString(
                                1,
                                "type.googleapis.com/spaceflinger.NodeStateManagerState",
                            )
                            writeMessage(2) { writeVarint(1, 11L) }
                        }
                    }
                }
                writeMessage(9) {
                    writeString(1, "south_pole_node")
                    writeMessage(6) { writeVec3(4, eulerFromQuatClamped(southQuat)) }
                    writeMessage(8) {
                        writeMessage(2) {
                            writeString(
                                1,
                                "type.googleapis.com/spaceflinger.NodeStateManagerState",
                            )
                            writeMessage(2) { writeVarint(1, 12L) }
                        }
                    }
                }
            }
        }

        val scene = CpmProtoSceneParser.parseScene(cpmBytes, rendererBytes)
        assertTrue(scene.isProtoCapture)
        val task = checkNotNull(scene.findTaskById(7))

        val childNode = checkNotNull(task.findNode { it.name == "gravity_aligned_child_panel" })
        val reconstructedUp = (childNode.rotation * Vector3.Up).toNormalized()
        assertTrue(
            Vector3.distance(reconstructedUp, Vector3.Up) < 1e-4f,
            "Expected reconstructed Up near Vector3.Up, got $reconstructedUp",
        )

        val sampleVec = Vector3(0.3f, -0.7f, 0.5f)
        val northNode = checkNotNull(task.findNode { it.name == "north_pole_node" })
        assertTrue(
            Vector3.distance(northNode.rotation * sampleVec, northQuat * sampleVec) < 1e-4f,
            "Expected north pole rotation to match $northQuat, got ${northNode.rotation}",
        )

        val southNode = checkNotNull(task.findNode { it.name == "south_pole_node" })
        assertTrue(
            Vector3.distance(southNode.rotation * sampleVec, southQuat * sampleVec) < 1e-4f,
            "Expected south pole rotation to match $southQuat, got ${southNode.rotation}",
        )
    }

    /** Matches `imp::EulerFromQuatClamped` (`third_party/impress/core/math/quat.cc`). */
    private fun eulerFromQuatClamped(q: Quaternion): Vector3 {
        val radToDeg = (180.0 / PI).toFloat()
        val singularity = 0.5f - 1e-6f
        val test = q.x * q.y + q.z * q.w
        val raw =
            when {
                test > singularity -> Vector3(x = 0f, y = 2f * atan2(q.x, q.w) * radToDeg, z = 90f)
                test < -singularity ->
                    Vector3(x = 0f, y = -2f * atan2(q.x, q.w) * radToDeg, z = -90f)
                else -> {
                    val sqx = q.x * q.x
                    val sqy = q.y * q.y
                    val sqz = q.z * q.z
                    Vector3(
                        x =
                            atan2(2f * q.x * q.w - 2f * q.y * q.z, 1f - 2f * sqx - 2f * sqz) *
                                radToDeg,
                        y =
                            atan2(2f * q.y * q.w - 2f * q.x * q.z, 1f - 2f * sqy - 2f * sqz) *
                                radToDeg,
                        z = asin((2f * test).coerceIn(-1f, 1f)) * radToDeg,
                    )
                }
            }
        fun clampNonNegative360(deg: Float): Float = ((deg % 360f) + 360f) % 360f
        return Vector3(
            clampNonNegative360(raw.x),
            clampNonNegative360(raw.y),
            clampNonNegative360(raw.z),
        )
    }

    private class ProtoBuilder {
        private val out = ByteArrayOutputStream()

        fun writeVarint(fieldNumber: Int, value: Long) {
            writeRawVarint(((fieldNumber shl 3) or 0).toLong())
            writeRawVarint(value)
        }

        fun writeFloat(fieldNumber: Int, value: Float) {
            writeRawVarint(((fieldNumber shl 3) or 5).toLong())
            val bits = value.toRawBits()
            out.write(bits and 0xFF)
            out.write((bits ushr 8) and 0xFF)
            out.write((bits ushr 16) and 0xFF)
            out.write((bits ushr 24) and 0xFF)
        }

        fun writeBytes(fieldNumber: Int, data: ByteArray) {
            writeRawVarint(((fieldNumber shl 3) or 2).toLong())
            writeRawVarint(data.size.toLong())
            out.write(data)
        }

        fun writeString(fieldNumber: Int, value: String) {
            writeBytes(fieldNumber, value.encodeToByteArray())
        }

        fun writeMessage(fieldNumber: Int, block: ProtoBuilder.() -> Unit) {
            val child = ProtoBuilder()
            child.block()
            writeBytes(fieldNumber, child.toByteArray())
        }

        fun writePackedVarints(fieldNumber: Int, values: List<Long>) {
            val packed = ProtoBuilder()
            for (v in values) {
                packed.writeRawVarint(v)
            }
            writeBytes(fieldNumber, packed.toByteArray())
        }

        fun writeVec3(fieldNumber: Int, v: Vector3) {
            writeMessage(fieldNumber) {
                if (v.x.toRawBits() != 0) writeFloat(1, v.x)
                if (v.y.toRawBits() != 0) writeFloat(2, v.y)
                if (v.z.toRawBits() != 0) writeFloat(3, v.z)
            }
        }

        fun writeQuat(fieldNumber: Int, q: Quaternion) {
            writeMessage(fieldNumber) {
                if (q.x.toRawBits() != 0) writeFloat(1, q.x)
                if (q.y.toRawBits() != 0) writeFloat(2, q.y)
                if (q.z.toRawBits() != 0) writeFloat(3, q.z)
                if (q.w.toRawBits() != 0) writeFloat(4, q.w)
            }
        }

        private fun writeRawVarint(value: Long) {
            var v = value
            while ((v and -0x80L) != 0L) {
                out.write((v.toInt() and 0x7F) or 0x80)
                v = v ushr 7
            }
            out.write(v.toInt() and 0x7F)
        }

        fun toByteArray(): ByteArray = out.toByteArray()
    }

    private fun buildProto(block: ProtoBuilder.() -> Unit): ByteArray =
        ProtoBuilder().apply(block).toByteArray()
}
