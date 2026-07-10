/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.camera.integration.uiwidgets.compose

import android.os.Build
import androidx.camera.integration.uiwidgets.compose.ui.navigation.ComposeCameraScreen
import androidx.camera.integration.uiwidgets.compose.ui.screen.imagecapture.DEFAULT_LENS_FACING
import androidx.camera.testing.impl.CameraUtil
import androidx.camera.testing.impl.LabTestRule
import androidx.camera.view.PreviewView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ActivityScenario
import androidx.test.filters.LargeTest
import androidx.test.rule.GrantPermissionRule
import androidx.testutils.RepeatRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.Assume
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@LargeTest
class ComposeCameraAppTest {
    // Provide permissions to app via ComposeCameraActivity
    @get:Rule
    val permissionRule: GrantPermissionRule =
        GrantPermissionRule.grant(*ComposeCameraActivity.REQUIRED_PERMISSIONS)

    @OptIn(ExperimentalCoroutinesApi::class) // b/457970052
    @get:Rule
    val androidComposeTestRule =
        createAndroidComposeRule<ComposeCameraActivity>(StandardTestDispatcher())

    @get:Rule val labTest: LabTestRule = LabTestRule()

    @get:Rule val repeatRule = RepeatRule()

    @Before
    fun setup() {
        // Skip test for b/168175357
        Assume.assumeFalse(
            "Cuttlefish has MediaCodec dequeInput/Output buffer fails issue. Unable to test.",
            Build.MODEL.contains("Cuttlefish") && Build.VERSION.SDK_INT == 29,
        )
        Assume.assumeTrue(CameraUtil.hasCameraWithLensFacing(DEFAULT_LENS_FACING))

        // Recreate the activity as it might terminate in other tests
        androidComposeTestRule.activityRule.scenario.recreate()
    }

    // Activity launch will render ImageCaptureScreen
    // Ensure that ImageCapture screen's PreviewView is streaming properly
    @Test
    @RepeatRule.Repeat(times = 10)
    fun testPreviewViewStreamStateOnActivityLaunch() {
        assertExpectedScreenAndStreamState(androidComposeTestRule.activityRule.scenario)
    }

    // Navigating from ImageCapture to VideoCapture screen
    // Ensure that VideoCapture screen's PreviewView is streaming properly
    @Test
    @LabTestRule.LabTestOnly
    @RepeatRule.Repeat(times = 10)
    fun testPreviewViewStreamStateOnNavigation() {

        // Get VideoCapture Navigation Tab (Node)
        val node =
            androidComposeTestRule.onNode(
                SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Tab)
                    .and(
                        SemanticsMatcher.expectValue(
                            SemanticsProperties.ContentDescription,
                            listOf("VideoCapture"),
                        )
                    )
            )

        // Set expected ComposeCameraScreen and StreamState for testing
        androidComposeTestRule.activityRule.scenario.onActivity {
            it.setUpExpectedScreenAndStreamState(
                ComposeCameraScreen.VideoCapture,
                PreviewView.StreamState.STREAMING,
            )
        }

        // Ensure that Tab is selected after we click on it
        node.performClick().assertIsSelected()

        // Assert VideoCapture's PreviewView is streaming
        assertExpectedScreenAndStreamState(androidComposeTestRule.activityRule.scenario)
    }

    // Asserts that the StreamState in the ComposeCameraScreen reaches
    // expectedState within a reasonable timeout
    private fun assertExpectedScreenAndStreamState(
        scenario: ActivityScenario<ComposeCameraActivity>
    ) =
        androidComposeTestRule.waitUntil(timeoutMillis = LATCH_TIMEOUT) {
            var reached = false
            scenario.onActivity { activity -> reached = activity.isExpectedStateReached() }
            reached
        }

    companion object {
        private const val TAG = "ComposeCameraAppTest"
        private const val LATCH_TIMEOUT: Long = 5000
    }
}
