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

package androidx.xr.scenecore.testapp

import android.widget.TextView
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import androidx.xr.runtime.math.IntSize2d
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Quaternion
import androidx.xr.runtime.math.Vector3
import androidx.xr.runtime.testing.math.assertPose
import androidx.xr.scenecore.Entity
import androidx.xr.scenecore.PanelEntity
import androidx.xr.scenecore.Space
import androidx.xr.scenecore.scene
import androidx.xr.testutils.XrDeviceTest
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Assume
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Integration tests validating spatial transformations on [PanelEntity] instances and verifying the
 * underlying platform (system CPM compositor) state.
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
@XrDeviceTest
class PanelTransformationTest {

    private val uiDevice: UiDevice =
        UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

    @Test
    fun panelSeparation_matchesPhysicalMetricDistanceInSystem() =
        runTestWithSession { activity, session ->
            // Create Primary Panel at (0, 0, -1) and Secondary Panel at (0, 0, 1) directly under
            // activitySpace (2.000m separation)
            val primaryPanel =
                PanelEntity.create(
                    session = session,
                    view = TextView(activity).apply { text = "Primary Panel" },
                    pixelDimensions = IntSize2d(width = 400, height = 300),
                    name = "Primary Panel",
                    pose = Pose(Vector3(0f, 0f, -1f)),
                    parent = session.scene.activitySpace,
                )

            val secondaryPanel =
                PanelEntity.create(
                    session = session,
                    view = TextView(activity).apply { text = "Secondary Panel" },
                    pixelDimensions = IntSize2d(width = 400, height = 300),
                    name = "Secondary Panel",
                    pose = Pose(Vector3(0f, 0f, 1f)),
                    parent = session.scene.activitySpace,
                )

            // Runtime Implementation Verification
            assertPose(primaryPanel.getPose(Space.ACTIVITY), Pose(Vector3(0f, 0f, -1f)))
            assertPose(secondaryPanel.getPose(Space.ACTIVITY), Pose(Vector3(0f, 0f, 1f)))
            val runtimeSeparation =
                Pose.distance(
                    primaryPanel.getPose(Space.ACTIVITY),
                    secondaryPanel.getPose(Space.ACTIVITY),
                )
            assertThat(runtimeSeparation).isWithin(1e-3f).of(2.0f)

            // Underlying Platform Verification: System compositor committed panel nodes.
            // World space in the system compositor is guaranteed to be 1:1 unit scale where 1.0 = 1
            // meter.
            Assume.assumeTrue(
                "Skipping system compositor check on non-headset target",
                SystemInspector.isAvailable(uiDevice),
            )

            // Await system nodes once ActivitySpace's unscaling transaction has settled to 1.0x
            val (systemPrimary, systemSecondary) =
                SystemInspector.awaitNodes(uiDevice, "Primary Panel", "Secondary Panel")

            // Assert unscaled physical distance in system compositor world space is exactly 2.000m.
            val physicalDistanceMeters =
                Vector3.distance(systemPrimary.worldPosition, systemSecondary.worldPosition)
            assertWithMessage("Physical Euclidean distance between panels in system compositor")
                .that(physicalDistanceMeters)
                .isWithin(0.001f) // Sub-millimeter precision
                .of(2.000f)
        }

    @Test
    fun panelRotation_rotatesInSystem() = runTestWithSession { activity, session ->
        // Unrotated reference panel under activity space (Identity rotation)
        val unrotatedPanel =
            PanelEntity.create(
                session = session,
                view = TextView(activity).apply { text = "Unrotated Panel" },
                pixelDimensions = IntSize2d(width = 400, height = 300),
                name = "Unrotated Panel",
                pose = Pose(Vector3(0f, 1f, -2f), Quaternion.Identity),
                parent = session.scene.activitySpace,
            )

        // Rotated panel with 90° Yaw around Y
        val rot90Y = Quaternion.fromAxisAngle(Vector3.Up, 90f)
        val rotatedPanel =
            PanelEntity.create(
                session = session,
                view = TextView(activity).apply { text = "Rotated Panel" },
                pixelDimensions = IntSize2d(width = 400, height = 300),
                name = "Rotated Panel",
                pose = Pose(Vector3(0f, 1f, -2f), rot90Y),
                parent = session.scene.activitySpace,
            )

        // Runtime Implementation Verification
        assertPose(
            unrotatedPanel.getPose(Space.ACTIVITY),
            Pose(Vector3(0f, 1f, -2f), Quaternion.Identity),
        )
        assertPose(rotatedPanel.getPose(Space.ACTIVITY), Pose(Vector3(0f, 1f, -2f), rot90Y))
        val runtimeAngleDegrees =
            Quaternion.angle(
                unrotatedPanel.getPose(Space.ACTIVITY).rotation,
                rotatedPanel.getPose(Space.ACTIVITY).rotation,
            )
        assertThat(runtimeAngleDegrees).isWithin(1e-3f).of(90.0f)

        // Underlying Platform Verification: The system compositor committed the rotated node in
        // world space.
        // World space in the system compositor is guaranteed to be 1:1 unit scale where 1.0 = 1
        // meter.
        Assume.assumeTrue(
            "Skipping system compositor check on non-headset target",
            SystemInspector.isAvailable(uiDevice),
        )

        val (systemUnrotated, systemRotated) =
            SystemInspector.awaitNodes(uiDevice, "Unrotated Panel", "Rotated Panel")

        // Verify system compositor node angular difference is 90° (0.5° tolerance accounts for
        // 3-decimal
        // dumpsys float formatting)
        val angleDegrees =
            Quaternion.angle(systemUnrotated.worldRotation, systemRotated.worldRotation)
        assertThat(angleDegrees).isWithin(0.5f).of(90.0f)
    }

    @Test
    fun hierarchicalScale_doublesPhysicalDistanceAndNodeScale() =
        runTestWithSession { activity, session ->
            // Unscaled reference panel at origin (1.0x scale)
            val unscaledReferencePanel =
                PanelEntity.create(
                    session = session,
                    view = TextView(activity).apply { text = "Unscaled Reference Panel" },
                    pixelDimensions = IntSize2d(width = 400, height = 300),
                    name = "Unscaled Reference Panel",
                    pose = Pose(Vector3(0f, 1f, -2f)),
                    parent = session.scene.activitySpace,
                )

            // Parent entity scaled by 2.0x
            val scaledParentEntity =
                Entity.create(
                        session = session,
                        pose = Pose(Vector3(0f, 1f, -2f)),
                        parent = session.scene.activitySpace,
                    )
                    .apply { setScale(2.0f) }

            // Child panel with 1.0m local offset under scaled parent
            val scaledChildPanel =
                PanelEntity.create(
                    session = session,
                    view = TextView(activity).apply { text = "Scaled Child Panel" },
                    pixelDimensions = IntSize2d(width = 400, height = 300),
                    name = "Scaled Child Panel",
                    pose = Pose(Vector3(1.0f, 0f, 0f)),
                    parent = scaledParentEntity,
                )

            // Runtime Implementation Verification: child panel inherits 2.0x scale and 2.0m
            // ActivitySpace translation
            assertThat(unscaledReferencePanel.getScale(Space.ACTIVITY)).isWithin(1e-3f).of(1.0f)
            assertThat(scaledChildPanel.getScale(Space.ACTIVITY)).isWithin(1e-3f).of(2.0f)
            assertThat(scaledChildPanel.getPose(Space.ACTIVITY).translation.x)
                .isWithin(1e-3f)
                .of(2.0f)

            // Underlying Platform Verification: Fetch system compositor nodes.
            // World space in the system compositor is guaranteed to be 1:1 unit scale where 1.0 = 1
            // meter.
            Assume.assumeTrue(
                "Skipping system compositor check on non-headset target",
                SystemInspector.isAvailable(uiDevice),
            )

            // Await system nodes once ActivitySpace settles to 1.0x and the child inherits 2.0x
            val (systemUnscaledPanel, systemScaledPanel) =
                SystemInspector.awaitNodes(
                    uiDevice,
                    "Unscaled Reference Panel",
                    "Scaled Child Panel",
                )

            // Assert the system compositor committed 1.0x and 2.0x scale in world space
            assertThat(systemUnscaledPanel.worldScale.x).isWithin(1e-3f).of(1.0f)
            assertThat(systemScaledPanel.worldScale.x).isWithin(1e-3f).of(2.0f)

            // Assert physical distance in system compositor world space doubled from 1.0m to 2.0m
            val physicalDistanceMeters =
                Vector3.distance(systemUnscaledPanel.worldPosition, systemScaledPanel.worldPosition)
            assertWithMessage("Physical Euclidean distance between panels in system compositor")
                .that(physicalDistanceMeters)
                .isWithin(0.001f)
                .of(2.000f)
        }
}
