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

package androidx.xr.scenecore.integration.tests

import android.widget.TextView
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.xr.runtime.math.IntSize2d
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Quaternion
import androidx.xr.runtime.math.Vector3
import androidx.xr.scenecore.Entity
import androidx.xr.scenecore.PanelEntity
import androidx.xr.scenecore.Space
import androidx.xr.scenecore.scene
import androidx.xr.testutils.XrDeviceTest
import androidx.xr.testutils.createXrSessionRule
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Integration tests validating spatial transformations on [PanelEntity] instances. */
@RunWith(AndroidJUnit4::class)
@LargeTest
@XrDeviceTest
class PanelTransformationTest {

    @get:Rule val sessionRule = createXrSessionRule()

    @Test
    fun panelSeparation_matchesMetricDistanceInActivitySpace() {
        sessionRule.requestFullSpace()

        sessionRule.onMainThread {
            // Create Primary Panel at (0, 0, -1) and Secondary Panel at (0, 0, 1) directly under
            // activitySpace (2.000m separation)
            val primaryPanel =
                PanelEntity.create(
                    session = session,
                    view = TextView(activity).apply { text = PRIMARY_PANEL_NAME },
                    pixelDimensions = IntSize2d(width = 400, height = 300),
                    name = PRIMARY_PANEL_NAME,
                    pose = Pose(Vector3(0f, 0f, -1f)),
                    parent = session.scene.activitySpace,
                )

            val secondaryPanel =
                PanelEntity.create(
                    session = session,
                    view = TextView(activity).apply { text = SECONDARY_PANEL_NAME },
                    pixelDimensions = IntSize2d(width = 400, height = 300),
                    name = SECONDARY_PANEL_NAME,
                    pose = Pose(Vector3(0f, 0f, 1f)),
                    parent = session.scene.activitySpace,
                )

            assertPose(primaryPanel.getPose(Space.ACTIVITY), Pose(Vector3(0f, 0f, -1f)))
            assertPose(secondaryPanel.getPose(Space.ACTIVITY), Pose(Vector3(0f, 0f, 1f)))
            val separation =
                Pose.distance(
                    primaryPanel.getPose(Space.ACTIVITY),
                    secondaryPanel.getPose(Space.ACTIVITY),
                )
            assertThat(separation).isWithin(1e-3f).of(2.0f)
        }
    }

    @Test
    fun panelRotation_rotatesInActivitySpace() {
        sessionRule.requestFullSpace()

        sessionRule.onMainThread {
            // Unrotated reference panel under activity space (Identity rotation)
            val unrotatedPanel =
                PanelEntity.create(
                    session = session,
                    view = TextView(activity).apply { text = UNROTATED_PANEL_NAME },
                    pixelDimensions = IntSize2d(width = 400, height = 300),
                    name = UNROTATED_PANEL_NAME,
                    pose = Pose(Vector3(0f, 1f, -2f), Quaternion.Identity),
                    parent = session.scene.activitySpace,
                )

            // Rotated panel with 90° Yaw around Y
            val rot90Y = Quaternion.fromAxisAngle(Vector3.Up, 90f)
            val rotatedPanel =
                PanelEntity.create(
                    session = session,
                    view = TextView(activity).apply { text = ROTATED_PANEL_NAME },
                    pixelDimensions = IntSize2d(width = 400, height = 300),
                    name = ROTATED_PANEL_NAME,
                    pose = Pose(Vector3(0f, 1f, -2f), rot90Y),
                    parent = session.scene.activitySpace,
                )

            assertPose(
                unrotatedPanel.getPose(Space.ACTIVITY),
                Pose(Vector3(0f, 1f, -2f), Quaternion.Identity),
            )
            assertPose(rotatedPanel.getPose(Space.ACTIVITY), Pose(Vector3(0f, 1f, -2f), rot90Y))
            val angleDegrees =
                Quaternion.angle(
                    unrotatedPanel.getPose(Space.ACTIVITY).rotation,
                    rotatedPanel.getPose(Space.ACTIVITY).rotation,
                )
            assertThat(angleDegrees).isWithin(1e-3f).of(90.0f)
        }
    }

    @Test
    fun hierarchicalScale_doublesChildScaleAndOffsetInActivitySpace() {
        sessionRule.requestFullSpace()

        sessionRule.onMainThread {
            // Unscaled reference panel at (0, 1, -2) (1.0x scale)
            val unscaledReferencePanel =
                PanelEntity.create(
                    session = session,
                    view = TextView(activity).apply { text = UNSCALED_REFERENCE_PANEL_NAME },
                    pixelDimensions = IntSize2d(width = 400, height = 300),
                    name = UNSCALED_REFERENCE_PANEL_NAME,
                    pose = Pose(Vector3(0f, 1f, -2f)),
                    parent = session.scene.activitySpace,
                )

            // Parent entity at the same pose, scaled by 2.0x
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
                    view = TextView(activity).apply { text = SCALED_CHILD_PANEL_NAME },
                    pixelDimensions = IntSize2d(width = 400, height = 300),
                    name = SCALED_CHILD_PANEL_NAME,
                    pose = Pose(Vector3(1.0f, 0f, 0f)),
                    parent = scaledParentEntity,
                )

            // Child panel inherits 2.0x scale, so its 1.0m local offset becomes 2.0m in
            // ActivitySpace.
            assertThat(unscaledReferencePanel.getScale(Space.ACTIVITY)).isWithin(1e-3f).of(1.0f)
            assertThat(scaledChildPanel.getScale(Space.ACTIVITY)).isWithin(1e-3f).of(2.0f)
            assertPose(
                scaledChildPanel.getPose(Space.ACTIVITY),
                Pose(Vector3(2.0f, 1f, -2f)),
                epsilon = 1e-3f,
            )
            val distance =
                Pose.distance(
                    unscaledReferencePanel.getPose(Space.ACTIVITY),
                    scaledChildPanel.getPose(Space.ACTIVITY),
                )
            assertThat(distance).isWithin(1e-3f).of(2.0f)
        }
    }

    private companion object {
        const val PRIMARY_PANEL_NAME = "Primary Panel"
        const val SECONDARY_PANEL_NAME = "Secondary Panel"
        const val UNROTATED_PANEL_NAME = "Unrotated Panel"
        const val ROTATED_PANEL_NAME = "Rotated Panel"
        const val UNSCALED_REFERENCE_PANEL_NAME = "Unscaled Reference Panel"
        const val SCALED_CHILD_PANEL_NAME = "Scaled Child Panel"
    }
}
