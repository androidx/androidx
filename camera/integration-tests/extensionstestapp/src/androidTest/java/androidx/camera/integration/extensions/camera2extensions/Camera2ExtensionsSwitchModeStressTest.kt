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

package androidx.camera.integration.extensions.camera2extensions

import android.content.Context
import android.hardware.camera2.CameraManager
import androidx.camera.camera2.Camera2Config
import androidx.camera.integration.extensions.util.Camera2ExtensionsTestUtil
import androidx.camera.integration.extensions.util.Camera2ExtensionsTestUtil.EXTENSION_NOT_FOUND
import androidx.camera.integration.extensions.util.Camera2ExtensionsTestUtil.findNextEffectMode
import androidx.camera.integration.extensions.utils.CameraIdExtensionModePair
import androidx.camera.testing.impl.CameraUtil
import androidx.camera.testing.impl.StressTestRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import kotlinx.coroutines.runBlocking
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.ClassRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@LargeTest
@RunWith(Parameterized::class)
@SdkSuppress(minSdkVersion = 31)
class Camera2ExtensionsSwitchModeStressTest(private val config: CameraIdExtensionModePair) {
    @get:Rule
    val useCamera = CameraUtil.grantCameraPermissionAndPreTest(
        CameraUtil.PreTestCameraIdList(Camera2Config.defaultConfig())
    )

    companion object {
        @ClassRule
        @JvmField
        val stressTest = StressTestRule()

        @Parameterized.Parameters(name = "cameraId = {0}, extensionMode = {1}")
        @JvmStatic
        fun parameters() = Camera2ExtensionsTestUtil.getAllCameraIdExtensionModeCombinations()
    }

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager

    @Before
    fun setUp() {
        assumeTrue(Camera2ExtensionsTestUtil.isTargetDeviceExcludedForExtensionsTest())
    }

    @Test
    fun switchModes(): Unit = runBlocking {
        val (cameraId, extensionMode) = config
        val nextMode = findNextEffectMode(context, cameraId, extensionMode)
        assumeTrue(nextMode != EXTENSION_NOT_FOUND)
        repeat(Camera2ExtensionsTestUtil.getStressTestRepeatingCount()) {
            Camera2ExtensionsTestUtil.assertCanOpenExtensionsSession(
                cameraManager,
                cameraId,
                extensionMode
            )

            Camera2ExtensionsTestUtil.assertCanOpenExtensionsSession(
                cameraManager,
                cameraId,
                nextMode
            )
        }

        // Test if preview frame can update and it can take a picture after the stress test.
        Camera2ExtensionsTestUtil.assertCanOpenExtensionsSession(
            cameraManager,
            cameraId,
            extensionMode,
            verifyOutput = true
        )
    }
}
