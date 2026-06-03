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

import android.app.Activity
import androidx.test.filters.LargeTest
import androidx.test.rule.GrantPermissionRule
import androidx.xr.scenecore.testapp.accessibilitytest.AccessibilityTestActivity
import androidx.xr.scenecore.testapp.activitypanel.ActivityPanel
import androidx.xr.scenecore.testapp.activitypanel.ActivityPanelActivity
import androidx.xr.scenecore.testapp.anchorspace.AnchorSpaceActivity
import androidx.xr.scenecore.testapp.fieldofviewvisibility.FieldOfViewVisibilityActivity
import androidx.xr.scenecore.testapp.fsmhsmtransition.FsmHsmTransitionActivity
import androidx.xr.scenecore.testapp.headlockedui.HeadLockedUiActivity
import androidx.xr.scenecore.testapp.hittest.HitTestActivity
import androidx.xr.scenecore.testapp.inputmoveresize.InputMoveResizeTestActivity
import androidx.xr.scenecore.testapp.memoryleak.MemoryLeakActivity
import androidx.xr.scenecore.testapp.meshentity.MeshEntityActivity
import androidx.xr.scenecore.testapp.model.GltfModelAnimationActivity
import androidx.xr.scenecore.testapp.model.GltfModelMaterialTextureActivity
import androidx.xr.scenecore.testapp.movable.MovableActivity
import androidx.xr.scenecore.testapp.panelcoordinate.PanelCoordinateActivity
import androidx.xr.scenecore.testapp.panelroundedcorner.PanelRoundedCornerActivity
import androidx.xr.scenecore.testapp.spatialaudio.SpatialAudioActivity
import androidx.xr.scenecore.testapp.spatialaudio.SpatialAudioComponentsActivity
import androidx.xr.scenecore.testapp.spatialcapabilities.SpatialCapabilitiesActivity
import androidx.xr.scenecore.testapp.standalone.StandaloneActivity
import androidx.xr.scenecore.testapp.surfacecustommesh.SurfaceEntityCustomMeshActivity
import androidx.xr.scenecore.testapp.surfaceimage.SurfaceEntityImageActivity
import androidx.xr.scenecore.testapp.surfaceinteraction.SurfaceEntityInteractionActivity
import androidx.xr.scenecore.testapp.surfaceplayback.SurfaceEntityPlaybackActivity
import androidx.xr.scenecore.testapp.visibility.VisibilityActivity
import androidx.xr.testutils.TestAppSmokeTest
import androidx.xr.testutils.XrDeviceTest
import androidx.xr.testutils.filterSupportedPermissions
import org.junit.Rule
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
@LargeTest
@XrDeviceTest
class SceneCoreTestAppSmokeTest(activityClass: Class<out Activity>) :
    TestAppSmokeTest(activityClass) {

    @get:Rule
    val permissionRule: GrantPermissionRule =
        GrantPermissionRule.grant(
            *filterSupportedPermissions(
                "android.permission.SCENE_UNDERSTANDING_COARSE",
                "android.permission.HAND_TRACKING",
                "android.permission.HEAD_TRACKING",
                "android.permission.INTERNET",
                "android.permission.READ_MEDIA_AUDIO",
                "android.permission.READ_MEDIA_IMAGES",
                "android.permission.READ_MEDIA_VIDEO",
                "android.permission.READ_EXTERNAL_STORAGE",
            )
        )

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun data(): Collection<Array<Any>> {
            return listOf(
                // TODO: b/515469049 - Fix and enable these tests.
                // arrayOf(EnvironmentActivity::class.java),
                // arrayOf(HandTrackingTest::class.java),
                // arrayOf(SceneViewerActivity::class.java),
                // arrayOf(SpatialUserActivity::class.java),
                arrayOf(AccessibilityTestActivity::class.java),
                arrayOf(ActivityPanel::class.java),
                arrayOf(ActivityPanelActivity::class.java),
                arrayOf(AnchorSpaceActivity::class.java),
                arrayOf(FieldOfViewVisibilityActivity::class.java),
                arrayOf(FsmHsmTransitionActivity::class.java),
                arrayOf(HeadLockedUiActivity::class.java),
                arrayOf(HitTestActivity::class.java),
                arrayOf(InputMoveResizeTestActivity::class.java),
                arrayOf(MainActivity::class.java),
                arrayOf(MemoryLeakActivity::class.java),
                arrayOf(MeshEntityActivity::class.java),
                arrayOf(GltfModelAnimationActivity::class.java),
                arrayOf(GltfModelMaterialTextureActivity::class.java),
                arrayOf(MovableActivity::class.java),
                arrayOf(PanelCoordinateActivity::class.java),
                arrayOf(PanelRoundedCornerActivity::class.java),
                arrayOf(SpatialAudioActivity::class.java),
                arrayOf(SpatialAudioComponentsActivity::class.java),
                arrayOf(SpatialCapabilitiesActivity::class.java),
                arrayOf(StandaloneActivity::class.java),
                arrayOf(SurfaceEntityCustomMeshActivity::class.java),
                arrayOf(SurfaceEntityImageActivity::class.java),
                arrayOf(SurfaceEntityInteractionActivity::class.java),
                arrayOf(SurfaceEntityPlaybackActivity::class.java),
                arrayOf(VisibilityActivity::class.java),
            )
        }
    }
}
