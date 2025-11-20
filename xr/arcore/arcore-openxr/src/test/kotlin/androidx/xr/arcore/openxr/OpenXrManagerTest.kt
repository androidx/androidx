/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.xr.arcore.openxr

import androidx.activity.ComponentActivity
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.xr.runtime.Config
import androidx.xr.runtime.manifest.HAND_TRACKING
import com.google.common.truth.Truth.assertThat
import kotlin.test.assertFailsWith
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.controller.ActivityController

@RunWith(AndroidJUnit4::class)
public class OpenXrManagerTest {

    private lateinit var activityController: ActivityController<ComponentActivity>
    private lateinit var activity: ComponentActivity

    private lateinit var underTest: OpenXrManager
    private lateinit var perceptionManager: OpenXrPerceptionManager
    private lateinit var timeSource: OpenXrTimeSource

    @Before
    fun setUp() {
        activityController = Robolectric.buildActivity(ComponentActivity::class.java)
        activity = activityController.get()

        timeSource = OpenXrTimeSource()
        perceptionManager = OpenXrPerceptionManager(timeSource)
        underTest = OpenXrManager(activity, perceptionManager, timeSource)
    }

    @Test
    fun configure_handTrackingEnabledWithoutPermission_throwsSecurityException() {
        check(underTest.config.handTracking == Config.HandTrackingMode.DISABLED)

        assertFailsWith<SecurityException> {
            underTest.configure(Config(handTracking = Config.HandTrackingMode.BOTH))
        }
    }

    @Test
    fun configure_handTrackingEnabled_addsHandToUpdatables() {
        shadowOf(activity).grantPermissions(HAND_TRACKING)
        check(underTest.config.handTracking == Config.HandTrackingMode.DISABLED)
        check(perceptionManager.xrResources.updatables.isEmpty())

        underTest.configure(Config(handTracking = Config.HandTrackingMode.BOTH))

        assertThat(perceptionManager.xrResources.updatables)
            .containsExactly(
                perceptionManager.xrResources.leftHand,
                perceptionManager.xrResources.rightHand,
            )
    }

    @Test
    fun configure_handTrackingDisabled_removesHandsFromUpdatables() {
        shadowOf(activity).grantPermissions(HAND_TRACKING)
        underTest.configure(Config(handTracking = Config.HandTrackingMode.BOTH))
        check(
            perceptionManager.xrResources.updatables.containsAll(
                listOf(
                    perceptionManager.xrResources.leftHand,
                    perceptionManager.xrResources.rightHand,
                )
            )
        )

        underTest.configure(Config(handTracking = Config.HandTrackingMode.DISABLED))

        assertThat(perceptionManager.xrResources.updatables)
            .doesNotContain(perceptionManager.xrResources.leftHand)
        assertThat(perceptionManager.xrResources.updatables)
            .doesNotContain(perceptionManager.xrResources.rightHand)
    }
}
