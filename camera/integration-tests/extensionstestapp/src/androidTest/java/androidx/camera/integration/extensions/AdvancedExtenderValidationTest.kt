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

package androidx.camera.integration.extensions

import androidx.camera.camera2.Camera2Config
import androidx.camera.integration.extensions.util.CameraXExtensionsTestUtil
import androidx.camera.integration.extensions.utils.CameraIdExtensionModePair
import androidx.camera.testing.impl.CameraUtil
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/**
 * Validation test for OEMs' advanced extender implementation.
 *
 * <p>All testings are forwarded to AdvancedExtenderValidation. This is because JUnit test will
 * check all types exist in the test class and on the devices that don't support extensions, the
 * extensions interface classes doesn't exist and will cause a class-not-found exception. Accessing
 * these extensions interface classes in the AdvancedExtenderValidation can avoid this issue.
 */
@LargeTest
@RunWith(Parameterized::class)
@SdkSuppress(minSdkVersion = 28)
class AdvancedExtenderValidationTest(config: CameraIdExtensionModePair) {
    private val validation = AdvancedExtenderValidation(config.cameraId, config.extensionMode)

    companion object {
        @JvmStatic
        @get:Parameterized.Parameters(name = "config = {0}")
        val parameters: Collection<CameraIdExtensionModePair>
            get() = CameraXExtensionsTestUtil.getAllCameraIdExtensionModeCombinations()
    }

    @get:Rule
    val useCamera = CameraUtil.grantCameraPermissionAndPreTest(
        CameraUtil.PreTestCameraIdList(Camera2Config.defaultConfig())
    )

    @Before
    fun setUp() = validation.setUp()

    @After
    fun tearDown() = validation.tearDown()

    @Test
    fun getSupportedPreviewOutputResolutions_returnValidData() =
        validation.getSupportedPreviewOutputResolutions_returnValidData()

    @Test
    fun getSupportedCaptureOutputResolutions_returnValidData() =
        validation.getSupportedCaptureOutputResolutions_returnValidData()

    @Test
    fun getAvailableCaptureRequestKeys_existAfter1_3() =
        validation.getAvailableCaptureRequestKeys_existAfter1_3()

    @Test
    fun getAvailableCaptureResultKeys_existAfter1_3() =
        validation.getAvailableCaptureResultKeys_existAfter1_3()

    @Test
    fun initSession_maxSize_canConfigureSession() =
        validation.initSession_maxSize_canConfigureSession()

    @Test
    fun initSession_minSize_canConfigureSession() =
        validation.initSession_minSize_canConfigureSession()

    @Test
    fun initSession_medianSize_canConfigureSession() =
        validation.initSession_medianSize_canConfigureSession()

    @Test
    fun initSessionWithAnalysis_maxSize_canConfigureSession() =
        validation.initSessionWithAnalysis_maxSize_canConfigureSession()

    @Test
    fun initSessionWithAnalysis_minSize_canConfigureSession() =
        validation.initSessionWithAnalysis_minSize_canConfigureSession()

    @Test
    fun initSessionWithAnalysis_medianSize_canConfigureSession() =
        validation.initSessionWithAnalysis_medianSize_canConfigureSession()
}
