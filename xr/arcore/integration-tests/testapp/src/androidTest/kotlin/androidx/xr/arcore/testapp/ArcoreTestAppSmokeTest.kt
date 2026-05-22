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
package androidx.xr.arcore.testapp

import android.app.Activity
import androidx.test.filters.LargeTest
import androidx.test.rule.GrantPermissionRule
import androidx.xr.arcore.testapp.capabilities.CapabilitiesActivity
import androidx.xr.arcore.testapp.depth.DepthActivity
import androidx.xr.arcore.testapp.eyetracking.EyeTrackingActivity
import androidx.xr.arcore.testapp.facetracking.FaceTrackingActivity
import androidx.xr.arcore.testapp.geospatial.GeospatialActivity
import androidx.xr.arcore.testapp.handtracking.HandTrackingActivity
import androidx.xr.arcore.testapp.helloar.HelloArObjectActivity
import androidx.xr.arcore.testapp.helloar.HelloArPlaneActivity
import androidx.xr.arcore.testapp.helloar.HelloArQrCodeActivity
import androidx.xr.arcore.testapp.persistentanchors.PersistentAnchorsActivity
import androidx.xr.testutils.TestAppSmokeTest
import androidx.xr.testutils.XrDeviceTest
import androidx.xr.testutils.filterSupportedPermissions
import org.junit.Rule
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
@LargeTest
@XrDeviceTest
class ArCoreTestAppSmokeTest(activityClass: Class<out Activity>) : TestAppSmokeTest(activityClass) {

    @get:Rule
    val permissionRule: GrantPermissionRule =
        GrantPermissionRule.grant(
            *filterSupportedPermissions(
                "android.permission.SCENE_UNDERSTANDING_COARSE",
                "android.permission.SCENE_UNDERSTANDING_FINE",
                "android.permission.HAND_TRACKING",
                "android.permission.HEAD_TRACKING",
                "android.permission.FACE_TRACKING",
                "android.permission.EYE_TRACKING_COARSE",
                "android.permission.EYE_TRACKING_FINE",
                "android.permission.INTERNET",
                "android.permission.ACCESS_COARSE_LOCATION",
                "android.permission.ACCESS_FINE_LOCATION",
                "android.permission.XR_EXPERIMENTAL_FEATURES",
            )
        )

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun data(): Collection<Array<Any>> {
            return listOf(
                // TODO b/515469379 - Re-enable when the test is fixed.
                // arrayOf(HelloArAugmentedImageActivity::class.java),
                // arrayOf(NativeDataActivity::class.java),
                arrayOf(MainActivity::class.java),
                arrayOf(CapabilitiesActivity::class.java),
                arrayOf(DepthActivity::class.java),
                arrayOf(EyeTrackingActivity::class.java),
                arrayOf(FaceTrackingActivity::class.java),
                arrayOf(GeospatialActivity::class.java),
                arrayOf(HandTrackingActivity::class.java),
                arrayOf(HelloArObjectActivity::class.java),
                arrayOf(HelloArPlaneActivity::class.java),
                arrayOf(HelloArQrCodeActivity::class.java),
                arrayOf(PersistentAnchorsActivity::class.java),
            )
        }
    }
}
