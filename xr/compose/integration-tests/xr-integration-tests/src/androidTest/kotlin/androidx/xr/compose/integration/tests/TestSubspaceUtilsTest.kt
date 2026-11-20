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

package androidx.xr.compose.integration.tests

import android.graphics.Rect
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Quaternion
import androidx.xr.runtime.math.Vector3
import androidx.xr.testutils.SnapshotXrSpace
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
class TestSubspaceUtilsTest {

    @Test
    fun projectedBounds_unionAndCovers_computeExpectedExtents() {
        val left = ProjectedBounds(minX = -0.4f, minY = -0.2f, maxX = -0.1f, maxY = 0.3f)
        val right = ProjectedBounds(minX = 0.0f, minY = -0.3f, maxX = 0.5f, maxY = 0.2f)

        val combined = left.union(right)

        assertThat(combined.minX).isWithin(1e-5f).of(-0.4f)
        assertThat(combined.minY).isWithin(1e-5f).of(-0.3f)
        assertThat(combined.maxX).isWithin(1e-5f).of(0.5f)
        assertThat(combined.maxY).isWithin(1e-5f).of(0.3f)
        assertThat(combined.covers(left)).isTrue()
        assertThat(combined.covers(right)).isTrue()
        assertThat(left.covers(combined)).isFalse()
    }

    @Test
    fun projectVolume_returnsNullForNonPositiveWidthOrHeight() {
        val projection =
            SubspaceProjection(
                headPose = Pose.Identity,
                rootPosition = Vector3(0f, 0f, -2f),
                rootRotation = Quaternion.Identity,
                rootScale = Vector3.One,
                pixelsPerMeter = 1000f,
            )

        assertThat(projection.projectVolume(0, 100, 0, Pose.Identity)).isNull()
        assertThat(projection.projectVolume(100, 0, 0, Pose.Identity)).isNull()
    }

    @Test
    fun projectVolume_projectsScaledCenteredVolumeInCameraSpace() {
        val projection =
            SubspaceProjection(
                headPose = Pose.Identity,
                rootPosition = Vector3(0f, 0f, -3f),
                rootRotation = Quaternion.Identity,
                rootScale = Vector3(2f, 0.5f, 1f),
                pixelsPerMeter = 1000f,
            )

        // Scaled extents: width=2m (halfW=1.0), height=0.25m (halfH=0.125), depth=2m (halfD=1.0).
        // Front corners sit at z=-2m (depth=2m) -> max |x|/depth = 0.5, max |y|/depth = 0.0625.
        val bounds =
            projection.projectVolume(
                widthPx = 1000,
                heightPx = 500,
                depthPx = 2000,
                poseInRoot = Pose.Identity,
            )

        assertThat(bounds).isNotNull()
        assertThat(bounds!!.minX).isWithin(1e-5f).of(-0.5f)
        assertThat(bounds.maxX).isWithin(1e-5f).of(0.5f)
        assertThat(bounds.minY).isWithin(1e-5f).of(-0.0625f)
        assertThat(bounds.maxY).isWithin(1e-5f).of(0.0625f)
    }

    @Test
    fun projectVolume_appliesPoseInRootTranslationAndRotation() {
        val projection =
            SubspaceProjection(
                headPose = Pose.Identity,
                rootPosition = Vector3(0f, 0f, -2f),
                rootRotation = Quaternion.Identity,
                rootScale = Vector3.One,
                pixelsPerMeter = 1000f,
            )

        // 90 deg rotation around +Z swaps halfW=0.5m and halfH=0.25m -> 0.25m x 0.5m around
        // (1.0f, -0.5f, -2.0f), divided by depth=2m.
        val bounds =
            projection.projectVolume(
                widthPx = 1000,
                heightPx = 500,
                depthPx = 0,
                poseInRoot =
                    Pose(
                        translation = Vector3(1000f, -500f, 0f),
                        rotation = Quaternion.fromAxisAngle(Vector3(0f, 0f, 1f), 90f),
                    ),
            )

        assertThat(bounds).isNotNull()
        assertThat(bounds!!.minX).isWithin(1e-5f).of(0.375f)
        assertThat(bounds.maxX).isWithin(1e-5f).of(0.625f)
        assertThat(bounds.minY).isWithin(1e-5f).of(-0.5f)
        assertThat(bounds.maxY).isWithin(1e-5f).of(0.0f)
    }

    @Test
    fun fromSnapshot_usesSubspaceRootWhenOffsetAndFallsBackAtOrigin() {
        val offsetRoot =
            SnapshotXrSpace(
                name = "SubspaceRoot",
                position = Vector3(0.5f, -0.2f, -3f),
                scale = Vector3(1.5f, 1.5f, 1.5f),
                computedPixelsPerMeter = 2000f,
            )
        val offsetProjection = SubspaceProjection.fromSnapshot(offsetRoot)
        assertThat(offsetProjection.rootPosition).isEqualTo(Vector3(0.5f, -0.2f, -3f))
        assertThat(offsetProjection.rootScale).isEqualTo(Vector3(1.5f, 1.5f, 1.5f))
        assertThat(offsetProjection.pixelsPerMeter).isEqualTo(2000f)

        val originRoot = SnapshotXrSpace(name = "SubspaceRoot", position = Vector3.Zero)
        val fallbackProjection = SubspaceProjection.fromSnapshot(originRoot)
        assertThat(fallbackProjection.rootPosition.z).isWithin(1e-5f).of(-1.75f)
        assertThat(fallbackProjection.pixelsPerMeter).isEqualTo(1600f)
    }

    @Test
    fun mapProjectedBoundsToBitmapRect_mapsSubRegionWithVerticalFlip() {
        val allBounds = ProjectedBounds(minX = -0.5f, minY = -0.5f, maxX = 0.5f, maxY = 0.5f)
        // Top-left quadrant in camera +Y-up space: x in [-0.5, 0.0], y in [0.0, 0.5]
        val targetBounds = ProjectedBounds(minX = -0.5f, minY = 0.0f, maxX = 0.0f, maxY = 0.5f)
        val overallBounds = Rect(100, 200, 500, 400) // 400x200 foreground box

        val mapped =
            mapProjectedBoundsToBitmapRect(
                allBounds = allBounds,
                targetBounds = targetBounds,
                overallBounds = overallBounds,
                bitmapWidth = 800,
                bitmapHeight = 600,
            )

        assertThat(mapped).isEqualTo(Rect(100, 200, 300, 300))
    }

    @Test
    fun mapProjectedBoundsToBitmapRect_returnsOverallBoundsWhenCoveringAllOrDegenerate() {
        val allBounds = ProjectedBounds(minX = -0.2f, minY = -0.2f, maxX = 0.2f, maxY = 0.2f)
        val overallBounds = Rect(10, 20, 110, 120)

        assertThat(
                mapProjectedBoundsToBitmapRect(
                    allBounds = allBounds,
                    targetBounds = allBounds,
                    overallBounds = overallBounds,
                    bitmapWidth = 200,
                    bitmapHeight = 200,
                )
            )
            .isEqualTo(overallBounds)

        val degenerate = ProjectedBounds(minX = 0f, minY = 0f, maxX = 0f, maxY = 0.2f)
        assertThat(
                mapProjectedBoundsToBitmapRect(
                    allBounds = degenerate,
                    targetBounds = degenerate,
                    overallBounds = overallBounds,
                    bitmapWidth = 200,
                    bitmapHeight = 200,
                )
            )
            .isEqualTo(overallBounds)
    }

    @Test
    fun mapProjectedBoundsToBitmapRect_clampsOutOfRangeBoundsToBitmapDimensions() {
        val allBounds = ProjectedBounds(minX = 0f, minY = 0f, maxX = 1f, maxY = 1f)
        val clippedTarget = ProjectedBounds(minX = -0.25f, minY = -0.25f, maxX = 1.5f, maxY = 0.5f)
        val mapped =
            mapProjectedBoundsToBitmapRect(
                allBounds = allBounds,
                targetBounds = clippedTarget,
                overallBounds = Rect(50, 50, 250, 250),
                bitmapWidth = 200,
                bitmapHeight = 200,
            )

        assertThat(mapped).isEqualTo(Rect(50, 150, 200, 200))
    }
}
