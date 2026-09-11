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

package androidx.xr.testutils

import android.graphics.Rect
import androidx.compose.animation.core.EaseIn
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Ray
import androidx.xr.runtime.math.Vector3
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class SpatialInteractionHelperTest {

    @Test
    fun enums_matchExpectedValues() {
        assertEquals(0, SpatialRayAction.Up.value)
        assertEquals(1, SpatialRayAction.Down.value)
        assertEquals(0, SpatialDeviceType.Unknown.value)
        assertEquals(1, SpatialDeviceType.Controller.value)
        assertEquals(2, SpatialDeviceType.Eyes.value)
        assertEquals(3, SpatialDeviceType.Hands.value)
        assertEquals(4, SpatialDeviceType.Head.value)
        assertEquals(0, SpatialPointerType.Left.value)
        assertEquals(1, SpatialPointerType.Right.value)
    }

    @Test
    fun composeEasing_transformsProperly() {
        assertEquals(0.0f, LinearEasing.transform(0.0f), 1e-4f)
        assertEquals(0.5f, LinearEasing.transform(0.5f), 1e-4f)
        assertEquals(1.0f, LinearEasing.transform(1.0f), 1e-4f)

        assertEquals(0.0f, FastOutSlowInEasing.transform(0.0f), 1e-4f)
        assertEquals(1.0f, FastOutSlowInEasing.transform(1.0f), 1e-4f)
        assertTrue(FastOutSlowInEasing.transform(0.5f) > 0.5f)

        assertEquals(0.0f, EaseIn.transform(0.0f), 1e-4f)
        assertEquals(1.0f, EaseIn.transform(1.0f), 1e-4f)
        assertTrue(EaseIn.transform(0.5f) < 0.5f)
    }

    @Test
    fun ray_instantiationAndProperties() {
        val ray = Ray(Vector3(1f, 2f, 3f), Vector3(0f, 0f, -1f))
        assertEquals(Vector3(1f, 2f, 3f), ray.origin)
        assertEquals(Vector3(0f, 0f, -1f), ray.direction)

        val copy = Ray(ray)
        assertEquals(ray, copy)
        assertEquals(ray.hashCode(), copy.hashCode())
    }

    @Test
    fun spatialInteractionScopeImpl_tracksTargetAndSynthesizesRays() {
        val node =
            SpatialNode(
                name = "TestWindow",
                attributes =
                    mapOf(
                        "ImpNodeWorldPosition" to floatArrayOf(0.0f, 1.447f, -1.743f),
                        "ImpNodeWorldRotation" to floatArrayOf(1.0f, 0.0f, 0.0f, 0.0f),
                        "ImpNodeWorldScale" to floatArrayOf(1.0f, 1.0f, 1.0f),
                        "root2DSurfaceBoundsInMeters" to floatArrayOf(1.0f, 0.9f),
                    ),
            )

        val scope =
            SpatialInteractionScopeImpl(
                deviceType = SpatialDeviceType.Controller,
                pointerType = SpatialPointerType.Left,
                node = node,
            )

        scope.pointAt(
            alignment = SpatialAlignment.BottomCenter,
            offset = Vector3(0f, -0.05f, 0f),
        )
        assertEquals(Vector3(0.0f, -0.5f, 0.0f), scope.currentTarget)
        assertEquals(Vector3(0.0f, 0.947f, -1.743f), scope.currentWorldTarget)
        assertFalse(scope.isPressed)

        scope.press()
        assertTrue(scope.isPressed)

        scope.movePointerBy(Vector3(0.25f, 0.0f, 0.0f))
        assertEquals(Vector3(0.25f, -0.5f, 0.0f), scope.currentTarget)
        assertEquals(Vector3(0.25f, 0.947f, -1.743f), scope.currentWorldTarget)
        assertTrue(scope.isPressed)

        scope.release()
        assertFalse(scope.isPressed)
    }

    @Test
    fun spatialInteractionScopeImpl_withIdentityNode_transformsCoordinates() {
        val scope =
            SpatialInteractionScopeImpl(
                deviceType = SpatialDeviceType.Controller,
                pointerType = SpatialPointerType.Left,
                node = SpatialNode(name = "IdentityNode"),
            )
        scope.moveOriginTo(Vector3(0f, 0f, 0.5f))

        assertEquals(Pose.Identity, scope.node.pose)
        assertEquals(Vector3(0f, 0f, 0.5f), scope.currentOrigin)
        assertEquals(Vector3(0f, 0f, 0.5f), scope.currentWorldOrigin)

        scope.pointAt(offset = Vector3(1f, 2f, 3f))
        assertEquals(Vector3(1f, 2f, 3f), scope.currentTarget)
        assertEquals(Vector3(1f, 2f, 3f), scope.currentWorldTarget)

        scope.movePointerBy(Vector3(0.5f, -0.2f, 0f))
        assertEquals(Vector3(1.5f, 1.8f, 3f), scope.currentTarget)
        assertEquals(Vector3(1.5f, 1.8f, 3f), scope.currentWorldTarget)
    }

    @Test
    fun spatialInteractionScopeImpl_withRotatedNode_transformsCoordinates() {
        // Node at (1.0f, 1.5f, -2.0f), rotated 90 degrees around Y axis:
        // [w = 0.7071068f, x = 0f, y = 0.7071068f, z = 0f]
        val taskRootNode =
            SpatialNode(
                name = "space-root-task-42",
                attributes =
                    mapOf(
                        "ImpNodeWorldPosition" to floatArrayOf(1.0f, 1.5f, -2.0f),
                        "ImpNodeWorldRotation" to floatArrayOf(0.7071068f, 0.0f, 0.7071068f, 0.0f),
                        "ImpNodeWorldScale" to floatArrayOf(1.0f, 1.0f, 1.0f),
                    ),
            )

        val scope =
            SpatialInteractionScopeImpl(
                deviceType = SpatialDeviceType.Controller,
                pointerType = SpatialPointerType.Right,
                node = taskRootNode,
            )
        scope.moveOriginTo(Vector3(0f, 0f, 1f))

        // Local space origin: (0, 0, 1)
        assertEquals(0.0f, scope.currentOrigin.x, 1e-4f)
        assertEquals(0.0f, scope.currentOrigin.y, 1e-4f)
        assertEquals(1.0f, scope.currentOrigin.z, 1e-4f)
        // In 90-degree Y rotation, local +Z rotates to world +X, so world origin is:
        // (1.0 + 1.0, 1.5, -2.0) = (2.0, 1.5, -2.0)
        assertEquals(2.0f, scope.currentWorldOrigin.x, 1e-4f)
        assertEquals(1.5f, scope.currentWorldOrigin.y, 1e-4f)
        assertEquals(-2.0f, scope.currentWorldOrigin.z, 1e-4f)

        // Point at node center (0, 0, 0)
        scope.pointAt(offset = Vector3(0f, 0f, 0f))
        assertEquals(0.0f, scope.currentTarget.x, 1e-4f)
        assertEquals(0.0f, scope.currentTarget.y, 1e-4f)
        assertEquals(0.0f, scope.currentTarget.z, 1e-4f)
        assertEquals(1.0f, scope.currentWorldTarget.x, 1e-4f)
        assertEquals(1.5f, scope.currentWorldTarget.y, 1e-4f)
        assertEquals(-2.0f, scope.currentWorldTarget.z, 1e-4f)

        // Press down
        scope.press()
        assertTrue(scope.isPressed)

        // Move 0.5m right in node space (+X in node space rotates to -Z in world space)
        scope.movePointerBy(Vector3(0.5f, 0f, 0f))
        assertEquals(0.5f, scope.currentTarget.x, 1e-4f)
        assertEquals(0.0f, scope.currentTarget.y, 1e-4f)
        assertEquals(0.0f, scope.currentTarget.z, 1e-4f)

        assertEquals(1.0f, scope.currentWorldTarget.x, 1e-4f)
        assertEquals(1.5f, scope.currentWorldTarget.y, 1e-4f)
        assertEquals(-2.5f, scope.currentWorldTarget.z, 1e-4f)

        // Fixed origin remains stationary while aiming/dragging
        assertEquals(2.0f, scope.currentWorldOrigin.x, 1e-4f)
        assertEquals(1.5f, scope.currentWorldOrigin.y, 1e-4f)
        assertEquals(-2.0f, scope.currentWorldOrigin.z, 1e-4f)

        // Calling moveOriginBy explicitly displaces the origin in node space (+X translates to
        // -Z in world)
        scope.moveOriginBy(Vector3(0.5f, 0f, 0f))
        assertEquals(0.5f, scope.currentOrigin.x, 1e-4f)
        assertEquals(0.0f, scope.currentOrigin.y, 1e-4f)
        assertEquals(1.0f, scope.currentOrigin.z, 1e-4f)
        assertEquals(2.0f, scope.currentWorldOrigin.x, 1e-4f)
        assertEquals(1.5f, scope.currentWorldOrigin.y, 1e-4f)
        assertEquals(-2.5f, scope.currentWorldOrigin.z, 1e-4f)

        // Release
        scope.release()
        assertFalse(scope.isPressed)
    }

    @Test
    fun spatialInteractionScopeImpl_withScaledNode_transformsCoordinates() {
        // Node at (1.0f, 1.5f, -2.0f), rotated 90 degrees around Y axis,
        // and scaled uniformly by 0.5:
        // [w = 0.7071068f, x = 0f, y = 0.7071068f, z = 0f]
        val taskRootNode =
            SpatialNode(
                name = "space-root-task-42",
                attributes =
                    mapOf(
                        "ImpNodeWorldPosition" to floatArrayOf(1.0f, 1.5f, -2.0f),
                        "ImpNodeWorldRotation" to floatArrayOf(0.7071068f, 0.0f, 0.7071068f, 0.0f),
                        "ImpNodeWorldScale" to floatArrayOf(0.5f, 0.5f, 0.5f),
                    ),
            )

        val scope =
            SpatialInteractionScopeImpl(
                deviceType = SpatialDeviceType.Controller,
                pointerType = SpatialPointerType.Right,
                node = taskRootNode,
            )
        scope.moveOriginTo(Vector3(0f, 0f, 1f))

        // Node scale: (0.5, 0.5, 0.5)
        assertEquals(0.5f, scope.node.scale.x, 1e-4f)
        assertEquals(0.5f, scope.node.scale.y, 1e-4f)
        assertEquals(0.5f, scope.node.scale.z, 1e-4f)

        // Node local space origin: (0, 0, 1)
        assertEquals(0.0f, scope.currentOrigin.x, 1e-4f)
        assertEquals(0.0f, scope.currentOrigin.y, 1e-4f)
        assertEquals(1.0f, scope.currentOrigin.z, 1e-4f)

        // In 90-degree Y rotation, local +Z scaled by 0.5 rotates to world +X scaled by 0.5:
        // (1.0 + 0.5, 1.5, -2.0) = (1.5, 1.5, -2.0)
        assertEquals(1.5f, scope.currentWorldOrigin.x, 1e-4f)
        assertEquals(1.5f, scope.currentWorldOrigin.y, 1e-4f)
        assertEquals(-2.0f, scope.currentWorldOrigin.z, 1e-4f)

        // Point at node center (0, 0, 0)
        scope.pointAt(offset = Vector3(0f, 0f, 0f))
        assertEquals(0.0f, scope.currentTarget.x, 1e-4f)
        assertEquals(0.0f, scope.currentTarget.y, 1e-4f)
        assertEquals(0.0f, scope.currentTarget.z, 1e-4f)
        assertEquals(1.0f, scope.currentWorldTarget.x, 1e-4f)
        assertEquals(1.5f, scope.currentWorldTarget.y, 1e-4f)
        assertEquals(-2.0f, scope.currentWorldTarget.z, 1e-4f)

        // Move 1.0m right in node space (+X in node space rotates to -Z in world space,
        // scaled by 0.5)
        scope.movePointerBy(Vector3(1.0f, 0f, 0f))
        assertEquals(1.0f, scope.currentTarget.x, 1e-4f)
        assertEquals(0.0f, scope.currentTarget.y, 1e-4f)
        assertEquals(0.0f, scope.currentTarget.z, 1e-4f)

        // World displacement is 1.0 * 0.5 = 0.5 along -Z
        assertEquals(1.0f, scope.currentWorldTarget.x, 1e-4f)
        assertEquals(1.5f, scope.currentWorldTarget.y, 1e-4f)
        assertEquals(-2.5f, scope.currentWorldTarget.z, 1e-4f)

        // Verify localPointOf with scaled root node:
        // A child node located in world space at (1.0, 1.5, -2.5) should map back to local space
        // (1.0, 0, 0)
        val childNode =
            SpatialNode(
                name = "window-task-42",
                attributes =
                    mapOf(
                        "ImpNodeWorldPosition" to floatArrayOf(1.0f, 1.5f, -2.5f),
                        "ImpNodeWorldRotation" to floatArrayOf(0.7071068f, 0.0f, 0.7071068f, 0.0f),
                        "ImpNodeWorldScale" to floatArrayOf(0.5f, 0.5f, 0.5f),
                    ),
            )
        taskRootNode.addChild(childNode)
        val calculatedLocalPoint = scope.localPointOf(childNode)
        assertEquals(1.0f, calculatedLocalPoint.x, 1e-4f)
        assertEquals(0.0f, calculatedLocalPoint.y, 1e-4f)
        assertEquals(0.0f, calculatedLocalPoint.z, 1e-4f)
    }

    @Test
    fun spatialInteractionScope_animate_passesInterpolatedFractionsWithEasing() {
        val receivedFractions = mutableListOf<Float>()
        val scope =
            SpatialInteractionScopeImpl(
                deviceType = SpatialDeviceType.Controller,
                pointerType = SpatialPointerType.Left,
                node = SpatialNode(name = "IdentityNode"),
            )
        scope.moveOriginTo(Vector3(0f, 0f, 1f))

        val start = Vector3(0f, 0f, 0f)
        val end = Vector3(1f, 0f, 0f)
        scope.pointAt(offset = start)
        assertEquals(start, scope.currentTarget)

        scope.animate(durationMs = 200L, easing = EaseIn) { fraction ->
            receivedFractions.add(fraction)
            pointAt(offset = Vector3.lerp(start, end, fraction))
        }

        // Multiple steps dispatched during animation, starting at 0.0f and ending at 1.0f
        assertTrue(receivedFractions.size >= 3)
        assertEquals(0.0f, receivedFractions.first(), 1e-4f)
        assertEquals(1.0f, receivedFractions.last(), 1e-4f)
        val halfProgress = 0.5f
        assertTrue(EaseIn.transform(halfProgress) < halfProgress)

        assertEquals(end, scope.currentTarget)
    }

    @Test
    fun spatialInteractionScope_animate_scalesStepsBeyondFourForLongDurations() {
        val receivedFractions = mutableListOf<Float>()
        val scope =
            SpatialInteractionScopeImpl(
                deviceType = SpatialDeviceType.Controller,
                pointerType = SpatialPointerType.Left,
                node = SpatialNode(name = "IdentityNode"),
            )

        scope.animate(durationMs = 600L) { fraction ->
            receivedFractions.add(fraction)
        }

        // 600ms / 100ms = 6 intervals -> 7 frames (0.0 .. 1.0 inclusive)
        assertEquals(7, receivedFractions.size)
        assertEquals(0.0f, receivedFractions.first(), 1e-4f)
        assertEquals(1.0f, receivedFractions.last(), 1e-4f)
    }

    @Test
    fun spatialInteractionScope_animate_zeroDurationDispatchesFullProgression() {
        val receivedFractions = mutableListOf<Float>()
        val scope =
            SpatialInteractionScopeImpl(
                deviceType = SpatialDeviceType.Controller,
                pointerType = SpatialPointerType.Left,
                node = SpatialNode(name = "IdentityNode"),
            )

        scope.animate(durationMs = 0L) { fraction ->
            receivedFractions.add(fraction)
        }

        assertEquals(listOf(1.0f), receivedFractions)
    }

    @Test
    fun spatialInteractionScope_localPointOf_crossNodeClicksHitSameLocations_identitySpace() {
        val taskRootNode =
            SpatialNode(
                name = "space-root-task-1",
                attributes =
                    mapOf(
                        "ImpNodeWorldPosition" to floatArrayOf(0.0f, 1.5f, -1.5f),
                        "ImpNodeWorldRotation" to floatArrayOf(1.0f, 0.0f, 0.0f, 0.0f),
                        "ImpNodeWorldScale" to floatArrayOf(1.0f, 1.0f, 1.0f),
                    ),
            )

        val windowNode =
            SpatialNode(
                name = "window-task-1",
                attributes =
                    mapOf(
                        "ImpNodeWorldPosition" to floatArrayOf(0.0f, 1.5f, -1.5f),
                        "ImpNodeWorldRotation" to floatArrayOf(1.0f, 0.0f, 0.0f, 0.0f),
                        "ImpNodeWorldScale" to floatArrayOf(1.0f, 1.0f, 1.0f),
                        "root2DSurfaceBoundsInMeters" to floatArrayOf(1.0f, 0.8f),
                    ),
            )
        taskRootNode.addChild(windowNode)

        val testCases =
            listOf(
                SpatialAlignment.Center to Vector3.Zero,
                SpatialAlignment.TopCenter to Vector3(0f, -0.02f, 0f),
                SpatialAlignment.BottomCenter to Vector3(0f, 0.02f, 0f),
                SpatialAlignment.CenterLeft to Vector3(0.02f, 0f, 0f),
                SpatialAlignment.CenterRight to Vector3(-0.02f, 0f, 0f),
                SpatialAlignment.TopLeft to Vector3.Zero,
                SpatialAlignment.BottomRight to Vector3.Zero,
                SpatialAlignment.Center to Vector3(0.08f, -0.04f, 0f),
            )

        for ((alignment, offset) in testCases) {
            val scopeNode =
                SpatialInteractionScopeImpl(
                    deviceType = SpatialDeviceType.Controller,
                    pointerType = SpatialPointerType.Left,
                    node = windowNode,
                )
            scopeNode.pointAt(alignment = alignment, offset = offset)

            val scopeTask =
                SpatialInteractionScopeImpl(
                    deviceType = SpatialDeviceType.Controller,
                    pointerType = SpatialPointerType.Left,
                    node = taskRootNode,
                )
            val taskPoint = scopeTask.localPointOf(windowNode, offset, alignment)
            scopeTask.pointAt(offset = taskPoint)

            // Both methods must produce the exact same world target
            assertEquals(
                scopeNode.currentWorldTarget.x,
                scopeTask.currentWorldTarget.x,
                1e-4f,
                "World target X mismatch for alignment $alignment, offset $offset",
            )
            assertEquals(
                scopeNode.currentWorldTarget.y,
                scopeTask.currentWorldTarget.y,
                1e-4f,
                "World target Y mismatch for alignment $alignment, offset $offset",
            )
            assertEquals(
                scopeNode.currentWorldTarget.z,
                scopeTask.currentWorldTarget.z,
                1e-4f,
                "World target Z mismatch for alignment $alignment, offset $offset",
            )
        }
    }

    @Test
    fun spatialInteractionScope_localPointOf_withWindowBounds() {
        val taskRootNode =
            SpatialNode(
                name = "space-root-task-1",
                attributes =
                    mapOf(
                        "ImpNodeWorldPosition" to floatArrayOf(0.0f, 1.5f, -1.5f),
                        "ImpNodeWorldRotation" to floatArrayOf(1.0f, 0.0f, 0.0f, 0.0f),
                        "ImpNodeWorldScale" to floatArrayOf(1.0f, 1.0f, 1.0f),
                    ),
            )

        val windowNode =
            SpatialNode(
                name = "window-task-1",
                attributes =
                    mapOf(
                        "ImpNodeWorldPosition" to floatArrayOf(0.0f, 1.5f, -1.5f),
                        "ImpNodeWorldRotation" to floatArrayOf(1.0f, 0.0f, 0.0f, 0.0f),
                        "ImpNodeWorldScale" to floatArrayOf(1.0f, 1.0f, 1.0f),
                        "root2DSurfaceBoundsInMeters" to floatArrayOf(1.0f, 0.5f),
                        "root2DSurfaceBoundsInPixels" to floatArrayOf(1000.0f, 500.0f),
                    ),
            )
        taskRootNode.addChild(windowNode)

        val scope =
            SpatialInteractionScopeImpl(
                deviceType = SpatialDeviceType.Controller,
                pointerType = SpatialPointerType.Left,
                node = taskRootNode,
            )

        // Center pixel bounds: rect from (250, 125) to (750, 375) -> center is (500, 250)
        val centerBounds =
            Rect().apply {
                left = 250
                top = 125
                right = 750
                bottom = 375
            }
        val centerPt = scope.localPointOf(windowNode, centerBounds)
        assertEquals(0.0f, centerPt.x, 1e-4f)
        assertEquals(0.0f, centerPt.y, 1e-4f)
        assertEquals(0.0f, centerPt.z, 1e-4f)

        // TopLeft pixel bounds: rect from (0, 0) to (500, 250) -> center is (250, 125)
        val topLeftQuadrant =
            Rect().apply {
                left = 0
                top = 0
                right = 500
                bottom = 250
            }
        val topLeftPt = scope.localPointOf(windowNode, topLeftQuadrant)
        assertEquals(-0.25f, topLeftPt.x, 1e-4f)
        assertEquals(0.125f, topLeftPt.y, 1e-4f)
        assertEquals(0.0f, topLeftPt.z, 1e-4f)
    }
}
