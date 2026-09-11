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

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import androidx.xr.runtime.math.Vector3
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.Timeout
import org.junit.runner.RunWith

/**
 * On-device instrumentation tests verifying [SpatialInteractionHelper], [SpatialScene], and raycast
 * injection in Android XR Home Space.
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
@XrDeviceTest
class SpatialInteractionHelperAndroidTest {

    @get:Rule val composeTestRule = createComposeRule()
    @get:Rule val globalTimeout: Timeout = Timeout.seconds(30)

    @Before
    fun setUp() {
        resetSpatialInteraction()
    }

    @After
    fun tearDown() {
        resetSpatialInteraction()
    }

    @Test
    fun spatialInteraction_selectTriggers2DComposeClickAndPointerEvents() {
        var clickCount by mutableIntStateOf(0)
        var receivedPress by mutableStateOf(false)
        var receivedRelease by mutableStateOf(false)

        composeTestRule.setContent {
            Box(
                modifier =
                    Modifier.fillMaxSize()
                        .pointerInput(Unit) {
                            awaitPointerEventScope {
                                while (true) {
                                    val event = awaitPointerEvent()
                                    if (event.type == PointerEventType.Press) {
                                        receivedPress = true
                                    } else if (event.type == PointerEventType.Release) {
                                        receivedRelease = true
                                    }
                                }
                            }
                        }
                        .clickable { clickCount++ },
                contentAlignment = Alignment.Center,
            ) {
                Button(modifier = Modifier.size(300.dp, 120.dp), onClick = { clickCount++ }) {
                    Text("2D Home Space Button")
                }
            }
        }

        composeTestRule.waitForIdle()

        val packageName = InstrumentationRegistry.getInstrumentation().targetContext.packageName
        val scene = SpatialSceneHelper.captureSpatialScene()
        val task = assertNotNull(scene.findTaskByPackageName(packageName))
        val targetNode =
            assertNotNull(
                task.taskWindowLeash ?: task.topLevelWindowLeashes.firstOrNull(),
                "Expected an active target node in the 2D Home Space scene for $packageName",
            )

        targetNode.performSpatialInteraction { select() }

        composeTestRule.waitUntil(
            conditionDescription = "Pointer press/release was not received within timeout",
            timeoutMillis = 5000L,
        ) {
            receivedPress && receivedRelease
        }

        assertTrue(receivedPress, "Expected 2D surface to receive PointerEventType.Press")
        assertTrue(receivedRelease, "Expected 2D surface to receive PointerEventType.Release")
        assertTrue(
            clickCount > 0,
            "Expected clickCount to increment after selection, got $clickCount",
        )
    }

    @Test
    fun spatialInteraction_selectQuadrantsHitsExactQuadrantCenters() {
        val quadrantClickCounts = mutableStateListOf(0, 0, 0, 0)
        val quadrantRecordedPresses = mutableStateListOf<Offset?>(null, null, null, null)
        val quadrantSizesPx =
            mutableStateListOf(IntSize.Zero, IntSize.Zero, IntSize.Zero, IntSize.Zero)

        composeTestRule.setContent {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(modifier = Modifier.fillMaxWidth().weight(1f)) {
                    // Quadrant 0: Top-Left
                    Box(
                        modifier =
                            Modifier.weight(1f)
                                .fillMaxHeight()
                                .onGloballyPositioned { coords -> quadrantSizesPx[0] = coords.size }
                                .pointerInput(Unit) {
                                    awaitPointerEventScope {
                                        while (true) {
                                            val event = awaitPointerEvent()
                                            if (event.type == PointerEventType.Press) {
                                                quadrantRecordedPresses[0] =
                                                    event.changes.firstOrNull()?.position
                                            }
                                        }
                                    }
                                }
                                .clickable { quadrantClickCounts[0]++ },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("Top-Left")
                    }

                    // Quadrant 1: Top-Right
                    Box(
                        modifier =
                            Modifier.weight(1f)
                                .fillMaxHeight()
                                .onGloballyPositioned { coords -> quadrantSizesPx[1] = coords.size }
                                .pointerInput(Unit) {
                                    awaitPointerEventScope {
                                        while (true) {
                                            val event = awaitPointerEvent()
                                            if (event.type == PointerEventType.Press) {
                                                quadrantRecordedPresses[1] =
                                                    event.changes.firstOrNull()?.position
                                            }
                                        }
                                    }
                                }
                                .clickable { quadrantClickCounts[1]++ },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("Top-Right")
                    }
                }

                Row(modifier = Modifier.fillMaxWidth().weight(1f)) {
                    // Quadrant 2: Bottom-Left
                    Box(
                        modifier =
                            Modifier.weight(1f)
                                .fillMaxHeight()
                                .onGloballyPositioned { coords -> quadrantSizesPx[2] = coords.size }
                                .pointerInput(Unit) {
                                    awaitPointerEventScope {
                                        while (true) {
                                            val event = awaitPointerEvent()
                                            if (event.type == PointerEventType.Press) {
                                                quadrantRecordedPresses[2] =
                                                    event.changes.firstOrNull()?.position
                                            }
                                        }
                                    }
                                }
                                .clickable { quadrantClickCounts[2]++ },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("Bottom-Left")
                    }

                    // Quadrant 3: Bottom-Right
                    Box(
                        modifier =
                            Modifier.weight(1f)
                                .fillMaxHeight()
                                .onGloballyPositioned { coords -> quadrantSizesPx[3] = coords.size }
                                .pointerInput(Unit) {
                                    awaitPointerEventScope {
                                        while (true) {
                                            val event = awaitPointerEvent()
                                            if (event.type == PointerEventType.Press) {
                                                quadrantRecordedPresses[3] =
                                                    event.changes.firstOrNull()?.position
                                            }
                                        }
                                    }
                                }
                                .clickable { quadrantClickCounts[3]++ },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("Bottom-Right")
                    }
                }
            }
        }

        composeTestRule.waitForIdle()

        val packageName = InstrumentationRegistry.getInstrumentation().targetContext.packageName
        val scene = SpatialSceneHelper.captureSpatialScene()
        val task = assertNotNull(scene.findTaskByPackageName(packageName))
        val targetNode =
            assertNotNull(
                task.taskWindowLeash ?: task.topLevelWindowLeashes.firstOrNull(),
                "Expected an active target node in the 2D Home Space scene",
            )

        val quadrantNames = listOf("Top-Left", "Top-Right", "Bottom-Left", "Bottom-Right")
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

        for (i in 0..3) {
            val uiObject =
                device.wait(Until.findObject(By.text(quadrantNames[i])), 5000L)
                    ?: device.findObject(By.text(quadrantNames[i]))
            assertNotNull(uiObject, "Expected UIAutomator to find ${quadrantNames[i]} node")
            val bounds = uiObject.visibleBounds

            targetNode.performSpatialInteraction {
                pointAt(bounds)
                select()
            }

            composeTestRule.waitUntil(
                conditionDescription =
                    "${quadrantNames[i]} quadrant was not clicked within timeout",
                timeoutMillis = 5000L,
            ) {
                quadrantClickCounts[i] > 0 && quadrantRecordedPresses[i] != null
            }

            val recordedPress = quadrantRecordedPresses[i]
            assertNotNull(recordedPress, "Expected recorded press position for ${quadrantNames[i]}")

            assertTrue(
                quadrantClickCounts[i] > 0,
                "Expected ${quadrantNames[i]} quadrant click count to increment",
            )
            val expectedCenterX = quadrantSizesPx[i].width / 2f
            val expectedCenterY = quadrantSizesPx[i].height / 2f
            val deltaX = recordedPress.x - expectedCenterX
            val deltaY = recordedPress.y - expectedCenterY
            val errorDistancePx = kotlin.math.sqrt(deltaX * deltaX + deltaY * deltaY)
            val maxAllowedErrorPx =
                minOf(quadrantSizesPx[i].width, quadrantSizesPx[i].height) * 0.35f

            assertTrue(
                errorDistancePx <= maxAllowedErrorPx,
                "Expected ${quadrantNames[i]} click near center ($expectedCenterX, " +
                    "$expectedCenterY) but received event at (${recordedPress.x}, " +
                    "${recordedPress.y}), delta: ($deltaX, $deltaY), error distance: " +
                    "${errorDistancePx}px (max allowed: ${maxAllowedErrorPx}px, " +
                    "size: ${quadrantSizesPx[i]})",
            )
        }
    }

    @Test
    fun spatialInteraction_selectAlignmentsHitExpectedContentRegions() {
        val recordedHits = mutableStateListOf<Offset>()
        var layoutSizePx by mutableStateOf(IntSize.Zero)

        composeTestRule.setContent {
            Box(
                modifier =
                    Modifier.fillMaxSize()
                        .onGloballyPositioned { coordinates -> layoutSizePx = coordinates.size }
                        .pointerInput(Unit) {
                            awaitPointerEventScope {
                                while (true) {
                                    val event = awaitPointerEvent()
                                    for (change in event.changes) {
                                        if (event.type == PointerEventType.Press) {
                                            recordedHits.add(change.position)
                                        }
                                    }
                                }
                            }
                        },
                contentAlignment = Alignment.Center,
            ) {
                Text("Alignment Target Surface")
            }
        }

        composeTestRule.waitForIdle()

        val packageName = InstrumentationRegistry.getInstrumentation().targetContext.packageName
        val scene = SpatialSceneHelper.captureSpatialScene()
        val task = assertNotNull(scene.findTaskByPackageName(packageName))
        val targetNode =
            assertNotNull(
                task.taskWindowLeash ?: task.topLevelWindowLeashes.firstOrNull(),
                "Expected target node for alignment verification",
            )

        // Use standard edge alignments with a small inward offset (2cm) towards the center to test
        // as close to each edge (top, bottom, start, end) as possible while staying safely inside
        // the interactive content area.
        val inwardOffsetMeters = 0.02f
        val alignmentsAndOffsetsToTest =
            listOf(
                SpatialAlignment.Center to Vector3.Zero,
                SpatialAlignment.TopCenter to Vector3(0f, -inwardOffsetMeters, 0f),
                SpatialAlignment.BottomCenter to Vector3(0f, inwardOffsetMeters, 0f),
                SpatialAlignment.CenterLeft to Vector3(inwardOffsetMeters, 0f, 0f),
                SpatialAlignment.CenterRight to Vector3(-inwardOffsetMeters, 0f, 0f),
            )

        for ((alignment, offset) in alignmentsAndOffsetsToTest) {
            val initialCount = recordedHits.size

            targetNode.performSpatialInteraction {
                pointAt(alignment = alignment, offset = offset)
                select()
            }

            composeTestRule.waitForIdle()

            composeTestRule.waitUntil(
                conditionDescription =
                    "Ray hit was not received for alignment $alignment with offset $offset " +
                        "within timeout",
                timeoutMillis = 3000L,
            ) {
                recordedHits.size > initialCount
            }
        }

        assertTrue(
            layoutSizePx.width > 0 && layoutSizePx.height > 0,
            "Expected non-zero layout size",
        )
        val extents = targetNode.worldExtents ?: Vector3.Zero

        for (i in alignmentsAndOffsetsToTest.indices) {
            val (alignment, offset) = alignmentsAndOffsetsToTest[i]
            val actualHit = recordedHits[i]

            val expectedX =
                ((alignment.x + 1.0f) * 0.5f + (if (extents.x > 0f) offset.x / extents.x else 0f)) *
                    layoutSizePx.width
            val expectedY =
                ((1.0f - alignment.y) * 0.5f - (if (extents.y > 0f) offset.y / extents.y else 0f)) *
                    layoutSizePx.height

            val deltaX = actualHit.x - expectedX
            val deltaY = actualHit.y - expectedY
            val errorDistancePx = kotlin.math.sqrt(deltaX * deltaX + deltaY * deltaY)
            val maxAllowedErrorPx = minOf(layoutSizePx.width, layoutSizePx.height) * 0.25f

            assertTrue(
                errorDistancePx <= maxAllowedErrorPx,
                "Expected hit for $alignment (offset: $offset) near ($expectedX, $expectedY) " +
                    "in window of size $layoutSizePx, but received event at (${actualHit.x}, " +
                    "${actualHit.y}), delta: ($deltaX, $deltaY), error: ${errorDistancePx}px " +
                    "(max allowed: ${maxAllowedErrorPx}px)",
            )
        }
    }

    @Test
    fun spatialInteraction_activitySpaceClicksHitSameLocationsAsNodeRelativeClicks() {
        val recordedHits = mutableStateListOf<Offset>()

        composeTestRule.setContent {
            Box(
                modifier =
                    Modifier.fillMaxSize().pointerInput(Unit) {
                        awaitPointerEventScope {
                            while (true) {
                                val event = awaitPointerEvent()
                                if (event.type == PointerEventType.Press) {
                                    for (change in event.changes) {
                                        recordedHits.add(change.position)
                                    }
                                }
                            }
                        }
                    },
                contentAlignment = Alignment.Center,
            ) {
                Text("Activity Space vs Node Relative Comparison Surface")
            }
        }

        composeTestRule.waitForIdle()

        val packageName = InstrumentationRegistry.getInstrumentation().targetContext.packageName
        val scene = SpatialSceneHelper.captureSpatialScene()
        val task = assertNotNull(scene.findTaskByPackageName(packageName))
        val targetNode =
            assertNotNull(
                task.taskWindowLeash ?: task.topLevelWindowLeashes.firstOrNull(),
                "Expected target node for click comparison",
            )

        val inwardOffsetMeters = 0.02f
        val alignmentsAndOffsetsToTest =
            listOf(
                SpatialAlignment.Center to Vector3.Zero,
                SpatialAlignment.TopCenter to Vector3(0f, -inwardOffsetMeters, 0f),
                SpatialAlignment.BottomCenter to Vector3(0f, inwardOffsetMeters, 0f),
                SpatialAlignment.CenterLeft to Vector3(inwardOffsetMeters, 0f, 0f),
                SpatialAlignment.CenterRight to Vector3(-inwardOffsetMeters, 0f, 0f),
            )

        val nodeRelativeHits = mutableListOf<Offset>()
        val activitySpaceHits = mutableListOf<Offset>()

        for ((alignment, offset) in alignmentsAndOffsetsToTest) {
            val countBeforeNodeClick = recordedHits.size

            // Phase 1: Node-relative click
            targetNode.performSpatialInteraction {
                pointAt(alignment = alignment, offset = offset)
                select()
            }

            composeTestRule.waitForIdle()
            composeTestRule.waitUntil(
                conditionDescription = "Node-relative click not received for $alignment ($offset)",
                timeoutMillis = 5000L,
            ) {
                recordedHits.size > countBeforeNodeClick
            }
            nodeRelativeHits.add(recordedHits.last())

            val countBeforeActivityClick = recordedHits.size

            // Phase 2: Activity space-based click targeting the exact same spatial location
            targetNode.activitySpaceRoot?.performSpatialInteraction {
                val activityPt = localPointOf(targetNode, offset, alignment)
                pointAt(offset = activityPt)
                select()
            }

            composeTestRule.waitForIdle()
            composeTestRule.waitUntil(
                conditionDescription = "Activity-space click not received for $alignment ($offset)",
                timeoutMillis = 5000L,
            ) {
                recordedHits.size > countBeforeActivityClick
            }
            activitySpaceHits.add(recordedHits.last())
        }

        // Phase 3: Assert that both click methods landed on the exact same UI pixel coordinates
        for (i in alignmentsAndOffsetsToTest.indices) {
            val (alignment, offset) = alignmentsAndOffsetsToTest[i]
            val nodeHit = nodeRelativeHits[i]
            val activityHit = activitySpaceHits[i]

            val deltaX = activityHit.x - nodeHit.x
            val deltaY = activityHit.y - nodeHit.y
            val errorDistancePx = kotlin.math.sqrt(deltaX * deltaX + deltaY * deltaY)

            // Hits should land at the exact same location with negligible tolerance (<= 5px)
            assertTrue(
                errorDistancePx <= 5.0f,
                "Expected activity space-based click to hit the same location as node-relative click " +
                    "for alignment $alignment (offset: $offset). " +
                    "Node hit: (${nodeHit.x}, ${nodeHit.y}), " +
                    "Activity hit: (${activityHit.x}, ${activityHit.y}), " +
                    "delta: ($deltaX, $deltaY), error: ${errorDistancePx}px (max allowed: 5.0px)",
            )
        }
    }

    @Test
    fun spatialInteraction_dragTriggersPointerMoveEventsOn2DCompose() {
        var moveEventCount by mutableIntStateOf(0)

        composeTestRule.setContent {
            Box(
                modifier =
                    Modifier.fillMaxSize()
                        .pointerInput(Unit) {
                            awaitPointerEventScope {
                                while (true) {
                                    val event = awaitPointerEvent()
                                    if (event.type == PointerEventType.Move) {
                                        moveEventCount++
                                    }
                                }
                            }
                        }
                        .clickable {},
                contentAlignment = Alignment.Center,
            ) {
                Text("2D Drag Surface")
            }
        }

        composeTestRule.waitForIdle()

        val packageName = InstrumentationRegistry.getInstrumentation().targetContext.packageName
        val scene = SpatialSceneHelper.captureSpatialScene()
        val task = assertNotNull(scene.findTaskByPackageName(packageName))
        val targetNode =
            assertNotNull(
                task.taskWindowLeash ?: task.topLevelWindowLeashes.firstOrNull(),
                "Expected an active target node in the 2D Home Space scene",
            )

        targetNode.performSpatialInteraction {
            select()
            pointAt(
                alignment = SpatialAlignment.Center,
                offset = Vector3(-0.05f, 0f, 0f),
            )
            press()
            animate(durationMs = 250L) { fraction ->
                pointAt(
                    alignment = SpatialAlignment.Center,
                    offset =
                        Vector3.lerp(
                            Vector3(-0.05f, 0f, 0f),
                            Vector3(0.05f, 0f, 0f),
                            fraction,
                        ),
                )
            }
            release()
        }

        composeTestRule.waitUntil(
            conditionDescription = "Pointer move events were not received within timeout",
            timeoutMillis = 5000L,
        ) {
            moveEventCount > 0
        }

        assertTrue(
            moveEventCount > 0,
            "Expected PointerEventType.Move events during spatial drag, got $moveEventCount",
        )
    }

    @Test
    fun spatialInteraction_moves2DWindowInHomeSpace() {
        composeTestRule.setContent {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Movable 2D Window")
            }
        }

        composeTestRule.waitForIdle()

        val packageName = InstrumentationRegistry.getInstrumentation().targetContext.packageName
        val initialScene = SpatialSceneHelper.captureSpatialScene()
        val initialTask = assertNotNull(initialScene.findTaskByPackageName(packageName))
        val windowNodeInitial =
            assertNotNull(
                initialTask.taskWindowLeash ?: initialTask.topLevelWindowLeashes.firstOrNull(),
                "Expected a window node in initial spatial scene",
            )

        val initialPosition = windowNodeInitial.position

        // Translate the 2D window using moveNodeBy
        windowNodeInitial.performSpatialInteraction {
            moveNodeBy(delta = Vector3(0.25f, 0f, 0f))
        }

        composeTestRule.waitForIdle()

        var updatedPosition = initialPosition
        composeTestRule.waitUntil(
            conditionDescription = "Window position did not change after move interaction",
            timeoutMillis = 5000L,
        ) {
            val scene = SpatialSceneHelper.captureSpatialScene()
            val task = scene.findTaskByPackageName(packageName)
            val node = task?.taskWindowLeash ?: task?.topLevelWindowLeashes?.firstOrNull()
            if (node != null) {
                updatedPosition = node.position
                (updatedPosition - initialPosition).length > 0.05f
            } else {
                false
            }
        }

        val distanceMoved = (updatedPosition - initialPosition).length

        assertTrue(
            distanceMoved > 0.05f,
            "Expected 2D window to move by at least 0.05m after performSpatialInteraction move, " +
                "but moved $distanceMoved (from $initialPosition to $updatedPosition)",
        )
    }

    @Test
    fun spatialInteraction_resizes2DWindowInHomeSpace() {
        var layoutSize by mutableStateOf(IntSize.Zero)

        composeTestRule.setContent {
            Box(
                modifier =
                    Modifier.fillMaxSize().onGloballyPositioned { coordinates ->
                        layoutSize = coordinates.size
                    },
                contentAlignment = Alignment.Center,
            ) {
                Text("Resizable 2D Window")
            }
        }

        composeTestRule.waitForIdle()

        val packageName = InstrumentationRegistry.getInstrumentation().targetContext.packageName
        val initialScene = SpatialSceneHelper.captureSpatialScene()
        val initialTask = assertNotNull(initialScene.findTaskByPackageName(packageName))
        val windowNodeInitial =
            assertNotNull(
                initialTask.taskWindowLeash ?: initialTask.topLevelWindowLeashes.firstOrNull(),
                "Expected a window node in initial spatial scene",
            )

        val initialExtents = windowNodeInitial.worldExtents ?: Vector3.Zero

        // Resize the 2D window using resizeNodeBy
        windowNodeInitial.performSpatialInteraction {
            resizeNodeBy(
                delta = Vector3(0.20f, -0.20f, 0f),
                alignment = SpatialAlignment.BottomRight,
            )
        }

        composeTestRule.waitForIdle()

        var extentsChanged = false
        composeTestRule.waitUntil(
            conditionDescription =
                "Window extents or scale did not change after resize interaction",
            timeoutMillis = 5000L,
        ) {
            val updatedScene = SpatialSceneHelper.captureSpatialScene()
            val updatedTask = updatedScene.findTaskByPackageName(packageName)
            val windowNodeUpdated =
                updatedTask?.taskWindowLeash ?: updatedTask?.topLevelWindowLeashes?.firstOrNull()
            if (windowNodeUpdated != null) {
                val updatedExtents = windowNodeUpdated.worldExtents ?: Vector3.Zero
                val updatedScale = windowNodeUpdated.scale
                extentsChanged =
                    (updatedExtents - initialExtents).length > 0.01f ||
                        (updatedScale - windowNodeInitial.scale).length > 0.01f
                extentsChanged
            } else {
                false
            }
        }

        assertTrue(
            extentsChanged,
            "Expected window extents or scale to reflect resize interaction " +
                "(initial: $initialExtents)",
        )
    }

    @Test
    fun spatialInteraction_discreteRayActionsTriggerPressAndRelease() {
        var isPressed by mutableStateOf(false)

        composeTestRule.setContent {
            Box(
                modifier =
                    Modifier.fillMaxSize().pointerInput(Unit) {
                        awaitPointerEventScope {
                            while (true) {
                                val event = awaitPointerEvent()
                                if (event.type == PointerEventType.Press) {
                                    isPressed = true
                                } else if (event.type == PointerEventType.Release) {
                                    isPressed = false
                                }
                            }
                        }
                    },
                contentAlignment = Alignment.Center,
            ) {
                Text("Discrete Ray Action Target")
            }
        }

        composeTestRule.waitForIdle()

        val packageName = InstrumentationRegistry.getInstrumentation().targetContext.packageName
        val scene = SpatialSceneHelper.captureSpatialScene()
        val task = assertNotNull(scene.findTaskByPackageName(packageName))
        val targetNode =
            assertNotNull(
                task.taskWindowLeash ?: task.topLevelWindowLeashes.firstOrNull(),
                "Expected a target node in Home Space",
            )

        targetNode.performSpatialInteraction {
            // Perform Down action (press)
            press()

            composeTestRule.waitUntil(
                conditionDescription = "Pointer press state was not set within timeout",
                timeoutMillis = 5000L,
            ) {
                isPressed
            }
            assertTrue(isPressed, "Expected 2D surface to enter pressed state after press()")

            // Perform Up action (release)
            release()

            composeTestRule.waitUntil(
                conditionDescription = "Pointer release state was not set within timeout",
                timeoutMillis = 5000L,
            ) {
                !isPressed
            }
            assertTrue(!isPressed, "Expected 2D surface to enter released state after release()")
        }
    }

    @Test
    fun spatialInteraction_multiActivityWindowArrangementWithDsl() {
        var primaryClickCount by mutableIntStateOf(0)
        composeTestRule.setContent {
            Box(
                modifier = Modifier.fillMaxSize().clickable { primaryClickCount++ },
                contentAlignment = Alignment.Center,
            ) {
                Text("Primary Activity Window")
            }
        }
        composeTestRule.waitForIdle()

        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val secondaryIntent =
            Intent(context, SecondarySpatialTestActivity::class.java).apply {
                addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_MULTIPLE_TASK or
                        Intent.FLAG_ACTIVITY_NEW_DOCUMENT
                )
            }
        val tertiaryIntent =
            Intent(context, TertiarySpatialTestActivity::class.java).apply {
                addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_MULTIPLE_TASK or
                        Intent.FLAG_ACTIVITY_NEW_DOCUMENT
                )
            }

        // 1. Move primary window to the left so it does not intercept center input
        val initialScene = SpatialSceneHelper.captureSpatialScene()
        val primaryTask = assertNotNull(initialScene.findTaskByPackageName(context.packageName))
        val primaryWindowNode =
            assertNotNull(
                primaryTask.taskWindowLeash ?: primaryTask.topLevelWindowLeashes.firstOrNull(),
                "Expected primary window node to be present",
            )
        val initialPrimaryPos = primaryWindowNode.position

        primaryWindowNode.performSpatialInteraction {
            moveNodeBy(delta = Vector3(-3.0f, 0f, 0f))
        }
        composeTestRule.waitForIdle()

        composeTestRule.waitUntil(
            conditionDescription = "Primary window did not move to the left",
            timeoutMillis = 5000L,
        ) {
            val scene = SpatialSceneHelper.captureSpatialScene()
            val task = scene.findTaskByPackageName(context.packageName)
            val node = task?.taskWindowLeash ?: task?.topLevelWindowLeashes?.firstOrNull()
            node != null && (node.position - initialPrimaryPos).length > 0.05f
        }

        // 2. Launch SecondarySpatialTestActivity in center, click it, then move it to the right
        val secondaryScenario =
            ActivityScenario.launch<SecondarySpatialTestActivity>(secondaryIntent)
        try {
            composeTestRule.waitForIdle()

            var secondaryScene: SpatialScene? = null
            composeTestRule.waitUntil(
                conditionDescription = "Secondary spatial node to appear in scene",
                timeoutMillis = 5000L,
            ) {
                val currentScene = SpatialSceneHelper.captureSpatialScene()
                if (
                    currentScene.tasks.any {
                        it.findNodesByName("SecondarySpatialTestActivity").isNotEmpty()
                    }
                ) {
                    secondaryScene = currentScene
                    true
                } else {
                    false
                }
            }
            val activeSecondaryScene =
                assertNotNull(
                    secondaryScene,
                    "Expected active scene with secondary activity window",
                )

            val secondaryTask =
                assertNotNull(
                    activeSecondaryScene.tasks.firstOrNull {
                        it.findNodesByName("SecondarySpatialTestActivity").isNotEmpty()
                    },
                    "Expected secondary task in scene",
                )
            val secondaryNode =
                secondaryTask.taskWindowLeash
                    ?: secondaryTask.findNodesByName("SecondarySpatialTestActivity").first()
            assertTrue(secondaryNode.canReceiveInput)

            // Perform spatial select on secondary activity window while in center
            composeTestRule.waitForIdle()

            composeTestRule.waitUntil(
                conditionDescription = "SecondarySpatialTestActivity to receive click",
                timeoutMillis = 10000L,
            ) {
                var clicked = false
                secondaryScenario.onActivity { activity -> clicked = activity.clickCount > 0 }
                if (!clicked) {
                    secondaryNode.performSpatialInteraction { select() }
                    secondaryScenario.onActivity { activity -> clicked = activity.clickCount > 0 }
                }
                clicked
            }

            val secondaryWindowNode =
                assertNotNull(
                    secondaryTask.taskWindowLeash
                        ?: secondaryTask.topLevelWindowLeashes.firstOrNull(),
                    "Expected secondary window node",
                )
            val initialSecondaryPos = secondaryWindowNode.position

            // Move secondary window to the right so center is clear for tertiary activity
            secondaryWindowNode.performSpatialInteraction {
                moveNodeBy(delta = Vector3(3.0f, 0f, 0f))
            }
            composeTestRule.waitForIdle()

            composeTestRule.waitUntil(
                conditionDescription = "Secondary window did not move to the right",
                timeoutMillis = 5000L,
            ) {
                val currentScene = SpatialSceneHelper.captureSpatialScene()
                val secTask =
                    currentScene.tasks.firstOrNull {
                        it.findNodesByName("SecondarySpatialTestActivity").isNotEmpty()
                    }
                val secWin =
                    secTask?.taskWindowLeash ?: secTask?.topLevelWindowLeashes?.firstOrNull()
                secWin != null && (secWin.position - initialSecondaryPos).length > 0.05f
            }

            // 3. Launch TertiarySpatialTestActivity in the center
            val tertiaryScenario =
                ActivityScenario.launch<TertiarySpatialTestActivity>(tertiaryIntent)
            try {
                composeTestRule.waitForIdle()

                var multiScene: SpatialScene? = null
                composeTestRule.waitUntil(
                    conditionDescription = "All activity windows to be present in scene",
                    timeoutMillis = 5000L,
                ) {
                    val currentScene = SpatialSceneHelper.captureSpatialScene()
                    val tasks = currentScene.findTasksByPackageName(context.packageName)
                    val hasSecondary = tasks.any {
                        it.findNodesByName("SecondarySpatialTestActivity").isNotEmpty()
                    }
                    val hasTertiary = tasks.any {
                        it.findNodesByName("TertiarySpatialTestActivity").isNotEmpty()
                    }
                    val inputTargetsCount = tasks.flatMap { it.inputTargets }.size
                    if (hasSecondary && hasTertiary && inputTargetsCount >= 3) {
                        multiScene = currentScene
                        true
                    } else {
                        false
                    }
                }
                val activeMultiScene =
                    assertNotNull(
                        multiScene,
                        "Expected multi-activity scene with concurrent windows",
                    )

                // Verify 3 concurrent app root task windows and interactive input targets
                val appTasks = activeMultiScene.findTasksByPackageName(context.packageName)
                val allInputTargets = appTasks.flatMap { it.inputTargets }
                assertTrue(
                    allInputTargets.isNotEmpty(),
                    "Expected interactive input targets across concurrent activity windows",
                )
                assertEquals(
                    3,
                    appTasks.size,
                    "Expected 3 app root task windows in scene",
                )

                // Perform spatial select on tertiary activity window (in the center)
                val tertiaryTask =
                    assertNotNull(
                        appTasks.firstOrNull {
                            it.findNodesByName("TertiarySpatialTestActivity").isNotEmpty()
                        },
                        "Expected tertiary task in scene",
                    )
                val currentTertiaryNode =
                    tertiaryTask.taskWindowLeash
                        ?: tertiaryTask.findNodesByName("TertiarySpatialTestActivity").first()
                assertTrue(currentTertiaryNode.canReceiveInput)
                composeTestRule.waitForIdle()

                composeTestRule.waitUntil(
                    conditionDescription = "TertiarySpatialTestActivity to receive click",
                    timeoutMillis = 10000L,
                ) {
                    var clicked = false
                    tertiaryScenario.onActivity { activity -> clicked = activity.clickCount > 0 }
                    if (!clicked) {
                        currentTertiaryNode.performSpatialInteraction { select() }
                        tertiaryScenario.onActivity { activity ->
                            clicked = activity.clickCount > 0
                        }
                    }
                    clicked
                }
            } finally {
                tertiaryScenario.close()
            }
        } finally {
            secondaryScenario.close()
        }
    }

    @Test
    fun spatialInteraction_resetClearsInjectedRaycast() {
        val result = resetSpatialInteraction()
        // Reset command should execute successfully without throwing
        assertNotNull(result)
    }

    @Test
    fun spatialInteractionScope_moves2DWindowWithDsl() {
        composeTestRule.setContent {
            Box(modifier = Modifier.fillMaxSize()) { Text("SpatialInteractionScope Move Test") }
        }
        composeTestRule.waitForIdle()

        val packageName = InstrumentationRegistry.getInstrumentation().targetContext.packageName
        val initialScene = SpatialSceneHelper.captureSpatialScene()
        val initialTask = assertNotNull(initialScene.findTaskByPackageName(packageName))
        val windowNodeInitial =
            assertNotNull(
                initialTask.taskWindowLeash ?: initialTask.topLevelWindowLeashes.firstOrNull(),
                "Expected a window node in initial spatial scene",
            )

        val initialPosition = windowNodeInitial.position

        // Use the performSpatialInteraction DSL to compose pointAtMoveAffordance with animate
        windowNodeInitial.performSpatialInteraction {
            pointAtMoveAffordance()
            press()
            animate(durationMs = 500L) { fraction ->
                pointAtMoveAffordance(Vector3(0.25f, 0f, 0f) * fraction)
            }
            release()
        }

        composeTestRule.waitForIdle()

        var updatedPosition = initialPosition
        composeTestRule.waitUntil(
            conditionDescription = "Window position did not change with DSL move interaction",
            timeoutMillis = 5000L,
        ) {
            val scene = SpatialSceneHelper.captureSpatialScene()
            val task = scene.findTaskByPackageName(packageName)
            val node = task?.taskWindowLeash ?: task?.topLevelWindowLeashes?.firstOrNull()
            if (node != null) {
                updatedPosition = node.position
                (updatedPosition - initialPosition).length > 0.05f
            } else {
                false
            }
        }

        val distanceMoved = (updatedPosition - initialPosition).length

        assertTrue(
            distanceMoved > 0.05f,
            "Expected 2D window to move by at least 0.05m with performSpatialInteraction DSL, " +
                "but moved $distanceMoved (from $initialPosition to $updatedPosition)",
        )
    }

    @Test
    fun spatialInteractionScope_nodeExtensionSelectAndDrag() {
        var clicked by mutableStateOf(false)
        var pointerMoveCount by mutableStateOf(0)

        composeTestRule.setContent {
            Box(
                modifier =
                    Modifier.fillMaxSize()
                        .pointerInput(Unit) {
                            awaitPointerEventScope {
                                while (true) {
                                    val event = awaitPointerEvent()
                                    if (event.type == PointerEventType.Move) {
                                        pointerMoveCount++
                                    }
                                }
                            }
                        }
                        .clickable { clicked = true },
                contentAlignment = Alignment.Center,
            ) {
                Text("Scope Node Extension Test")
            }
        }
        composeTestRule.waitForIdle()

        val packageName = InstrumentationRegistry.getInstrumentation().targetContext.packageName
        val scene = SpatialSceneHelper.captureSpatialScene()
        val task = assertNotNull(scene.findTaskByPackageName(packageName))
        val targetNode =
            assertNotNull(
                task.taskWindowLeash ?: task.topLevelWindowLeashes.firstOrNull(),
                "Expected active target node",
            )

        // Use node.performSpatialInteraction extension
        targetNode.performSpatialInteraction {
            // Select center
            select()

            // Press and move across center
            pointAt(
                alignment = SpatialAlignment.Center,
                offset = Vector3(-0.05f, 0f, 0f),
            )
            press()
            animate(durationMs = 250L) { fraction ->
                pointAt(
                    alignment = SpatialAlignment.Center,
                    offset =
                        Vector3.lerp(
                            Vector3(-0.05f, 0f, 0f),
                            Vector3(0.05f, 0f, 0f),
                            fraction,
                        ),
                )
            }
            release()
        }

        composeTestRule.waitUntil(
            conditionDescription = "Click or pointer move events were not received within timeout",
            timeoutMillis = 5000L,
        ) {
            clicked && pointerMoveCount > 0
        }

        assertTrue(clicked, "Expected select() in scope to trigger Compose click")
        assertTrue(pointerMoveCount > 0, "Expected drag in scope to trigger pointer move events")
    }
}
