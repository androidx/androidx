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

import androidx.activity.ComponentActivity
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.xr.runtime.Session
import androidx.xr.runtime.SessionCreateSuccess
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Quaternion
import androidx.xr.runtime.math.Vector3
import androidx.xr.scenecore.Entity
import androidx.xr.scenecore.Space
import androidx.xr.scenecore.scene
import androidx.xr.testutils.XrDeviceTest
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
class PositionalTest {
    @get:Rule val activityScenarioRule = ActivityScenarioRule(ComponentActivity::class.java)

    @Test
    @XrDeviceTest
    fun getPose_inActivitySpace_composesParentTransforms() {
        activityScenarioRule.scenario.onActivity { activity ->
            // 1. Create the XR Session.
            val sessionResult = runBlocking { Session.create(context = activity) }
            check(sessionResult is SessionCreateSuccess) { "Failed to create XR session" }
            val session = sessionResult.session

            // 2. Create the root Sun entity attached to ActivitySpace.
            val sunPos = Vector3(-0.5f, 0.5f, -1f)
            val sunRot = Quaternion.fromEulerAngles(-10f, 20f, 30f)
            val sunScale = 0.8f
            val sunEntity =
                Entity.create(
                    session = session,
                    pose = Pose(sunPos, sunRot),
                    parent = session.scene.activitySpace,
                )
            sunEntity.setScale(sunScale)

            // 3. Create the Planet entity attached to the Sun.
            val planetPos = Vector3(-1f, 2f, -0.5f)
            val planetRot = Quaternion.fromEulerAngles(20f, -30f, 40f)
            val planetScale = 0.6f
            val planetEntity =
                Entity.create(
                    session = session,
                    pose = Pose(planetPos, planetRot),
                    parent = sunEntity,
                )
            planetEntity.setScale(planetScale)

            // 4. Create the Moon entity attached to the Planet.
            val moonPos = Vector3(-1.5f, 2f, -0.5f)
            val moonRot = Quaternion.fromEulerAngles(30f, 40f, -50f)
            val moonScale = 0.4f
            val moonEntity =
                Entity.create(
                    session = session,
                    pose = Pose(moonPos, moonRot),
                    parent = planetEntity,
                )
            moonEntity.setScale(moonScale)

            // 5. Verify the computed world pose in ActivitySpace matches the expected composition.
            val sunWorldPose = Pose(sunPos, sunRot)
            assertThat(sunEntity.getPose(Space.ACTIVITY)).isEqualTo(sunWorldPose)

            val planetWorldPose = sunWorldPose.compose(Pose(planetPos * sunScale, planetRot))
            assertThat(planetEntity.getPose(Space.ACTIVITY)).isEqualTo(planetWorldPose)

            val moonWorldPose =
                planetWorldPose.compose(Pose(moonPos * (sunScale * planetScale), moonRot))
            assertThat(moonEntity.getPose(Space.ACTIVITY)).isEqualTo(moonWorldPose)
        }
    }
}
