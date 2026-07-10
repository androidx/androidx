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
package androidx.xr.compose.testapp

import android.app.Activity
import androidx.test.filters.LargeTest
import androidx.test.rule.GrantPermissionRule
import androidx.xr.compose.testapp.animation.Animation
import androidx.xr.compose.testapp.animation.SampleAnimations
import androidx.xr.compose.testapp.curvedlayout.CurvedLayout
import androidx.xr.compose.testapp.depthstacking.DepthStacking
import androidx.xr.compose.testapp.fragments.FragmentCompatibilityActivity
import androidx.xr.compose.testapp.gravityaligned.GravityAlignedActivity
import androidx.xr.compose.testapp.lifecycle.OpenCloseActivity
import androidx.xr.compose.testapp.lifecycle.OpenCloseChildActivity
import androidx.xr.compose.testapp.lifecycle.ResizeActivity
import androidx.xr.compose.testapp.mediaplayer.MediaPlayerActivity
import androidx.xr.compose.testapp.modechange.ModeChange
import androidx.xr.compose.testapp.movablescalable.MovableScalable
import androidx.xr.compose.testapp.panelembeddedsubspace.PanelEmbeddedSubspace
import androidx.xr.compose.testapp.panelvolume.PanelVolume
import androidx.xr.compose.testapp.performance.LayoutPerformance
import androidx.xr.compose.testapp.pose.Pose
import androidx.xr.compose.testapp.rotation.Rotation
import androidx.xr.compose.testapp.rtlawareness.RtlAwareSubspaceModifierActivity
import androidx.xr.compose.testapp.spacemodechange.SpaceModeActivity
import androidx.xr.compose.testapp.spatialalignmentusage.SpatialAlignmentUsageActivity
import androidx.xr.compose.testapp.spatialarrangementusage.SpatialArrangementUsageActivity
import androidx.xr.compose.testapp.spatialcompose.SpatialCompose
import androidx.xr.compose.testapp.spatialcompose.SpatialComposeStateTest
import androidx.xr.compose.testapp.spatialcompose.SpatialComposeWindowManager
import androidx.xr.compose.testapp.spatialelevation.SpatialElevation
import androidx.xr.compose.testapp.spatialgltfmodel.SpatialGltfModelActivity
import androidx.xr.compose.testapp.spatialpanel.SpatialPanelActivity
import androidx.xr.testutils.TestAppSmokeTest
import androidx.xr.testutils.XrDeviceTest
import androidx.xr.testutils.filterSupportedPermissions
import org.junit.Rule
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
@LargeTest
@XrDeviceTest
class ComposeTestAppSmokeTest(activityClass: Class<out Activity>) :
    TestAppSmokeTest(activityClass) {

    @get:Rule
    val permissionRule: GrantPermissionRule =
        GrantPermissionRule.grant(
            *filterSupportedPermissions(
                "android.permission.SCENE_UNDERSTANDING_COARSE",
                "android.permission.HAND_TRACKING",
                "android.permission.HEAD_TRACKING",
                "android.permission.INTERNET",
                "android.permission.READ_EXTERNAL_STORAGE",
                "android.permission.READ_MEDIA_VIDEO",
                "android.permission.POST_NOTIFICATIONS",
            )
        )

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun data(): Collection<Array<Any>> {
            return listOf(
                // TODO: b/513351407 - Fix and enable these tests.
                // arrayOf(AccessibilityActivity::class.java),
                // arrayOf(AnchorFollowingSubspaceActivity::class.java),
                // arrayOf(AnotherActivity::class.java),
                // arrayOf(FocusStealerActivity::class.java),
                // arrayOf(FollowingSubspaceActivity::class.java),
                // arrayOf(FSMFocusChangeActivity::class.java),
                // arrayOf(HSMFocusChangeActivity::class.java),
                // arrayOf(MovableActivity::class.java),
                // arrayOf(RotateToLookAtUserActivity::class.java),
                // arrayOf(RuntimeSessionActivity::class.java),
                // arrayOf(ResizablePanel::class.java),
                // arrayOf(SpatialComposeVideoPlayer::class.java),
                // arrayOf(SplitEngine::class.java),
                // arrayOf(VideoPlayerActivity::class.java),
                arrayOf(Animation::class.java),
                arrayOf(CurvedLayout::class.java),
                arrayOf(DepthStacking::class.java),
                arrayOf(FragmentCompatibilityActivity::class.java),
                arrayOf(GravityAlignedActivity::class.java),
                arrayOf(LayoutPerformance::class.java),
                arrayOf(MainActivity::class.java),
                arrayOf(MediaPlayerActivity::class.java),
                arrayOf(ModeChange::class.java),
                arrayOf(MovableScalable::class.java),
                arrayOf(OpenCloseActivity::class.java),
                arrayOf(OpenCloseChildActivity::class.java),
                arrayOf(PanelEmbeddedSubspace::class.java),
                arrayOf(PanelVolume::class.java),
                arrayOf(Pose::class.java),
                arrayOf(ResizeActivity::class.java),
                arrayOf(Rotation::class.java),
                arrayOf(RtlAwareSubspaceModifierActivity::class.java),
                arrayOf(SampleAnimations::class.java),
                arrayOf(SpaceModeActivity::class.java),
                arrayOf(SpatialAlignmentUsageActivity::class.java),
                arrayOf(SpatialArrangementUsageActivity::class.java),
                arrayOf(SpatialCompose::class.java),
                arrayOf(SpatialComposeStateTest::class.java),
                arrayOf(SpatialComposeWindowManager::class.java),
                arrayOf(SpatialElevation::class.java),
                arrayOf(SpatialGltfModelActivity::class.java),
                arrayOf(SpatialPanelActivity::class.java),
            )
        }
    }
}
