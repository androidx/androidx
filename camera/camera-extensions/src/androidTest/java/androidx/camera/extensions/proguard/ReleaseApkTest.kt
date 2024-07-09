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

package androidx.camera.extensions.proguard

import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraExtensionCharacteristics.EXTENSION_AUTOMATIC
import android.hardware.camera2.CameraExtensionCharacteristics.EXTENSION_BOKEH
import android.hardware.camera2.CameraExtensionCharacteristics.EXTENSION_FACE_RETOUCH
import android.hardware.camera2.CameraExtensionCharacteristics.EXTENSION_HDR
import android.hardware.camera2.CameraExtensionCharacteristics.EXTENSION_NIGHT
import android.hardware.camera2.CameraManager
import android.os.Build
import android.os.Bundle
import androidx.camera.camera2.Camera2Config
import androidx.camera.camera2.pipe.integration.CameraPipeConfig
import androidx.camera.core.CameraFilter
import androidx.camera.core.CameraInfo
import androidx.camera.core.CameraSelector
import androidx.camera.core.CameraXConfig
import androidx.camera.core.impl.CameraInfoInternal
import androidx.camera.extensions.ExtensionMode.AUTO
import androidx.camera.extensions.ExtensionMode.BOKEH
import androidx.camera.extensions.ExtensionMode.FACE_RETOUCH
import androidx.camera.extensions.ExtensionMode.HDR
import androidx.camera.extensions.ExtensionMode.NIGHT
import androidx.camera.extensions.ExtensionsManager
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.testing.impl.CameraPipeConfigTestRule
import androidx.camera.testing.impl.CameraUtil
import androidx.camera.testing.impl.CameraUtil.PreTestCameraIdList
import androidx.camera.testing.impl.CoreAppTestUtil
import androidx.camera.testing.impl.LabTestRule
import androidx.camera.testing.impl.activity.RequestResultTestActivity
import androidx.camera.testing.impl.activity.RequestResultTestActivity.INTENT_EXTRA_BUNDLE
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso
import androidx.test.espresso.IdlingRegistry
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import com.google.common.truth.Truth.assertWithMessage
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@LargeTest
@RunWith(Parameterized::class)
class ReleaseApkTest(private val config: CameraXExtensionTestParams) {

    @get:Rule
    val cameraPipeConfigTestRule =
        CameraPipeConfigTestRule(
            active = config.implName == CameraPipeConfig::class.simpleName,
        )

    @get:Rule
    val cameraRule =
        CameraUtil.grantCameraPermissionAndPreTestAndPostTest(
            PreTestCameraIdList(config.cameraXConfig)
        )

    @get:Rule val labTestRule = LabTestRule()

    private val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    private lateinit var requestResultActivityScenario: ActivityScenario<RequestResultTestActivity>

    @Before
    fun setUp() {
        // Clear the device UI and check if there is no dialog or lock screen on the top of the
        // window before start the test.
        CoreAppTestUtil.prepareDeviceUI(InstrumentationRegistry.getInstrumentation())
    }

    @After
    fun tearDown(): Unit = runBlocking {
        if (::requestResultActivityScenario.isInitialized) {
            requestResultActivityScenario.close()
        }

        // Unfreeze rotation so the device can choose the orientation via its own policy. Be nice
        // to other tests :)
        device.unfreezeRotation()
        device.pressHome()
        device.waitForIdle(3000)
    }

    @LabTestRule.LabTestOnly
    @Test
    fun canLaunchReleaseVersionApk() {
        // Launches the RequestResultTestActivity to invoke startActivityForResult to the
        // extensions test app's ReleaseTestActivity.
        val startIntent =
            Intent(Intent.ACTION_MAIN).apply {
                setClassName(context.packageName, RequestResultTestActivity::class.java.name)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                // Specify the intent action string for launching the ReleaseTestActivity.
                putExtra(
                    RequestResultTestActivity.INTENT_EXTRA_INTENT_ACTION,
                    "androidx.camera.integration.extensions.release_test"
                )
                // Specifies the target impl mode, running mode, camera id and extension mode info.
                putExtra(
                    INTENT_EXTRA_BUNDLE,
                    Bundle().apply {
                        putString(
                            INTENT_EXTRA_CAMERA_IMPLEMENTATION,
                            if (config.implName == CameraPipeConfig::class.simpleName) {
                                CAMERA_PIPE_IMPLEMENTATION_OPTION
                            } else {
                                CAMERA2_IMPLEMENTATION_OPTION
                            }
                        )
                        putString(INTENT_EXTRA_RUNNING_MODE_CHECK, "release")
                        putString(INTENT_EXTRA_KEY_CAMERA_ID, config.cameraId)
                        putInt(INTENT_EXTRA_KEY_EXTENSION_MODE, config.extensionMode)
                    }
                )
            }
        val activityRef =
            RequestResultTestActivity::class
                .java
                .cast(InstrumentationRegistry.getInstrumentation().startActivitySync(startIntent))!!
        try {
            IdlingRegistry.getInstance().register(activityRef.requestResultReadyIdlingResource)

            // The {@link Espresso#onIdle()} throws timeout exception if the
            // RequestResultTestActivity
            // cannot get onActivityResult. The default timeout in espresso is 26 sec.
            Espresso.onIdle()
            assertWithMessage(activityRef.resultErrorMessage)
                .that(activityRef.resultErrorCode)
                .isEqualTo(RESULT_ERROR_NONE)
        } finally {
            IdlingRegistry.getInstance().unregister(activityRef.requestResultReadyIdlingResource)
        }
    }

    companion object {
        /** Launches the activity with the specified CameraX implementation. */
        private const val INTENT_EXTRA_CAMERA_IMPLEMENTATION = "camera_implementation"

        /** Launches the activity with the specified id of camera. */
        private const val INTENT_EXTRA_KEY_CAMERA_ID = "camera_id"

        /** Launches the activity with the specified extension mode. */
        private const val INTENT_EXTRA_KEY_EXTENSION_MODE = "extension_mode"

        /** Used to pass the running mode to check. The valid values are debug and release. */
        private const val INTENT_EXTRA_RUNNING_MODE_CHECK = "running_mode_check"

        /** Result error code - no error */
        private const val RESULT_ERROR_NONE = 0

        private const val CAMERA2_IMPLEMENTATION_OPTION: String = "camera2"
        private const val CAMERA_PIPE_IMPLEMENTATION_OPTION: String = "camera_pipe"

        private val context = ApplicationProvider.getApplicationContext<Context>()!!

        @Parameterized.Parameters(name = "config = {0}")
        @JvmStatic
        fun parameters() = getAllCameraIdExtensionModeCombinations()

        data class CameraXExtensionTestParams(
            val implName: String,
            val cameraXConfig: CameraXConfig,
            val cameraId: String,
            val extensionMode: Int,
        )

        /** Gets a list of all camera id and extension mode combinations. */
        @JvmStatic
        fun getAllCameraIdExtensionModeCombinations(
            context: Context = ApplicationProvider.getApplicationContext()
        ): List<CameraXExtensionTestParams> =
            filterOutUnavailableMode(
                context,
                CameraUtil.getBackwardCompatibleCameraIdListOrThrow().flatMap { cameraId ->
                    AVAILABLE_EXTENSION_MODES.flatMap { extensionMode ->
                        CAMERAX_CONFIGS.map { config ->
                            CameraXExtensionTestParams(
                                config.first!!,
                                config.second,
                                cameraId,
                                extensionMode
                            )
                        }
                    }
                }
            )

        @JvmStatic
        private fun filterOutUnavailableMode(
            context: Context,
            list: List<CameraXExtensionTestParams>
        ): List<CameraXExtensionTestParams> {
            var extensionsManager: ExtensionsManager? = null
            var cameraProvider: ProcessCameraProvider? = null
            try {
                cameraProvider = ProcessCameraProvider.getInstance(context)[2, TimeUnit.SECONDS]
                extensionsManager =
                    ExtensionsManager.getInstanceAsync(context, cameraProvider)[2, TimeUnit.SECONDS]

                val result: MutableList<CameraXExtensionTestParams> = mutableListOf()
                for (item in list) {
                    val cameraSelector = createCameraSelectorById(item.cameraId)
                    // For CameraX extensions, minification might cause a default ExtensionsManager
                    // instance returned. In that case, no item will be added to the testing target
                    // list. Therefore, adding the items according to Camera2Extensions support
                    // list. Then, the test will fail when ReleaseTestActivity checks whether the
                    // target extensions mode is supported or not. This can avoid the false-positive
                    // test result situation.
                    if (
                        (isCamera2ExtensionsSupported(context, item.cameraId, item.extensionMode) &&
                            !isDeviceOnlySupportedInCamera2Extensions()) ||
                            extensionsManager.isExtensionAvailable(
                                cameraSelector,
                                item.extensionMode
                            )
                    ) {
                        result.add(item)
                    }
                }
                return result
            } catch (e: Exception) {
                return list
            } finally {
                try {
                    cameraProvider?.shutdownAsync()?.get()
                    extensionsManager?.shutdown()?.get()
                } catch (_: Exception) {}
            }
        }

        @JvmStatic
        private fun createCameraSelectorById(cameraId: String) =
            CameraSelector.Builder()
                .addCameraFilter(
                    CameraFilter { cameraInfos ->
                        cameraInfos.forEach {
                            if ((it as CameraInfoInternal).cameraId.equals(cameraId)) {
                                return@CameraFilter listOf<CameraInfo>(it)
                            }
                        }

                        return@CameraFilter emptyList()
                    }
                )
                .build()

        /** Checks whether the corresponding extensions mode is supported in Camera2Extensions. */
        @JvmStatic
        private fun isCamera2ExtensionsSupported(
            context: Context,
            cameraId: String,
            cameraXExtensionMode: Int
        ): Boolean {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                return false
            }

            val correspondingMode =
                when (cameraXExtensionMode) {
                    BOKEH -> EXTENSION_BOKEH
                    HDR -> EXTENSION_HDR
                    NIGHT -> EXTENSION_NIGHT
                    FACE_RETOUCH -> EXTENSION_FACE_RETOUCH
                    AUTO -> EXTENSION_AUTOMATIC
                    else ->
                        throw IllegalArgumentException(
                            "No matching Camera2Extensions mode can be found!"
                        )
                }

            val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val characteristics = cameraManager.getCameraExtensionCharacteristics(cameraId)

            return characteristics.supportedExtensions.contains(correspondingMode)
        }

        /**
         * Some devices only support Camera2Extensions but do not support CameraXExtensions. For
         * these devices, do not force add to the testing target list.
         */
        @JvmStatic
        private fun isDeviceOnlySupportedInCamera2Extensions() =
            Build.BRAND.equals("samsung", true) && Build.MODEL.equals("sm-n975u1", true)

        @JvmStatic
        private val AVAILABLE_EXTENSION_MODES = arrayOf(BOKEH, HDR, NIGHT, FACE_RETOUCH, AUTO)

        /** A list of supported implementation options and their respective [CameraXConfig]. */
        private val CAMERAX_CONFIGS =
            listOf(
                Pair(Camera2Config::class.simpleName, Camera2Config.defaultConfig()),
                Pair(CameraPipeConfig::class.simpleName, CameraPipeConfig.defaultConfig())
            )
    }
}
