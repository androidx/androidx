/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.camera.integration.testingtestapp.ui

import androidx.camera.integration.testingtestapp.R
import androidx.camera.integration.testingtestapp.testing.HiltComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.test.filters.SdkSuppress
import androidx.test.rule.GrantPermissionRule
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Rule
import org.junit.Test

@HiltAndroidTest
@SdkSuppress(minSdkVersion = 31) // TODO: b/360115093 - Enable tests only all APIs once fixed
class CameraTest {

    @get:Rule(order = 0) var hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val cameraPermissionRule: GrantPermissionRule =
        GrantPermissionRule.grant(android.Manifest.permission.CAMERA)

    @get:Rule(order = 2) var composeTestRule = createAndroidComposeRule<HiltComponentActivity>()

    @Test
    fun test1() {

        composeTestRule.setContent { Camera() }
        composeTestRule
            .onNodeWithContentDescription(composeTestRule.activity.getString(R.string.swap_cameras))
            .assertExists()
    }
}
