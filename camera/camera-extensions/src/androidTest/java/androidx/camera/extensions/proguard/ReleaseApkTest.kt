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
import android.util.Log
import androidx.camera.camera2.Camera2Config
import androidx.camera.core.CameraFilter
import androidx.camera.core.CameraInfo
import androidx.camera.core.CameraSelector
import androidx.camera.core.impl.CameraInfoInternal
import androidx.camera.core.impl.utils.executor.CameraXExecutors.directExecutor
import androidx.camera.extensions.ExtensionMode.AUTO
import androidx.camera.extensions.ExtensionMode.BOKEH
import androidx.camera.extensions.ExtensionMode.FACE_RETOUCH
import androidx.camera.extensions.ExtensionMode.HDR
import androidx.camera.extensions.ExtensionMode.NIGHT
import androidx.camera.extensions.ExtensionsManager
import androidx.camera.extensions.internal.compat.quirk.DeviceQuirks
import androidx.camera.extensions.internal.compat.quirk.ExtensionDisabledQuirk
import androidx.camera.extensions.util.ExtensionsTestUtil
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.testing.impl.CameraUtil
import androidx.camera.testing.impl.CameraUtil.PreTestCameraIdList
import androidx.camera.testing.impl.LabTestRule
import androidx.camera.testing.impl.RequireForegroundRule
import androidx.camera.testing.impl.activity.RequestResultTestActivity
import androidx.camera.testing.impl.activity.RequestResultTestActivity.INTENT_EXTRA_BUNDLE
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso
import androidx.test.espresso.IdlingRegistry
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertWithMessage
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@LargeTest
@RunWith(Parameterized::class)
class ReleaseApkTest(private val config: CameraXExtensionTestParams) {

    @get:Rule(order = 0) val labTestRule = LabTestRule()

    @get:Rule(order = 1)
    val requireForegroundRule = RequireForegroundRule { assumeTrue(CameraUtil.deviceHasCamera()) }

    @get:Rule(order = 2)
    val cameraRule =
        CameraUtil.grantCameraPermissionAndPreTestAndPostTest(
            PreTestCameraIdList(Camera2Config.defaultConfig())
        )

    private lateinit var requestResultActivityScenario: ActivityScenario<RequestResultTestActivity>

    @After
    fun tearDown(): Unit = runBlocking {
        if (::requestResultActivityScenario.isInitialized) {
            requestResultActivityScenario.close()
        }
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
                    "androidx.camera.integration.extensions.release_test",
                )
                // Specifies the target impl mode, running mode, camera id and extension mode info.
                putExtra(
                    INTENT_EXTRA_BUNDLE,
                    Bundle().apply {
                        putString(INTENT_EXTRA_RUNNING_MODE_CHECK, "release")
                        putString(INTENT_EXTRA_KEY_CAMERA_ID, config.cameraId)
                        putInt(INTENT_EXTRA_KEY_EXTENSION_MODE, config.extensionMode)
                    },
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
        /** Launches the activity with the specified id of camera. */
        private const val INTENT_EXTRA_KEY_CAMERA_ID = "camera_id"

        /** Launches the activity with the specified extension mode. */
        private const val INTENT_EXTRA_KEY_EXTENSION_MODE = "extension_mode"

        /** Used to pass the running mode to check. The valid values are debug and release. */
        private const val INTENT_EXTRA_RUNNING_MODE_CHECK = "running_mode_check"

        /** Result error code - no error */
        private const val RESULT_ERROR_NONE = 0

        private val context = ApplicationProvider.getApplicationContext<Context>()!!

        @Parameterized.Parameters(name = "config = {0}")
        @JvmStatic
        fun parameters() =
            if (LabTestRule.isInLabTest()) {
                getAllCameraIdExtensionModeCombinations()
            } else listOf()

        data class CameraXExtensionTestParams(val cameraId: String, val extensionMode: Int)

        /** Gets a list of all camera id and extension mode combinations. */
        @JvmStatic
        fun getAllCameraIdExtensionModeCombinations(
            context: Context = ApplicationProvider.getApplicationContext()
        ): List<CameraXExtensionTestParams> =
            filterOutUnavailableMode(
                context,
                CameraUtil.getBackwardCompatibleCameraIdListOrThrow().flatMap { cameraId ->
                    AVAILABLE_EXTENSION_MODES.map { extensionMode ->
                        CameraXExtensionTestParams(cameraId, extensionMode)
                    }
                },
            )

        @JvmStatic
        private fun filterOutUnavailableMode(
            context: Context,
            list: List<CameraXExtensionTestParams>,
        ): List<CameraXExtensionTestParams> {
            var extensionsManager: ExtensionsManager? = null
            var cameraProvider: ProcessCameraProvider? = null
            val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
            try {
                cameraProvider =
                    cameraProviderFuture[CAMERA_PROVIDER_TIMEOUT_SECONDS, TimeUnit.SECONDS]
                extensionsManager = runBlocking {
                    ExtensionsManager.getInstance(context, cameraProvider!!)
                }

                val result: MutableList<CameraXExtensionTestParams> = mutableListOf()
                for (item in list) {
                    // Skips the extension modes that are intentionally disabled by CameraX on the
                    // device. Camera2Extensions might still report them as supported, but
                    // ExtensionsManager will report them as unavailable by design.
                    if (isExtensionDisabledByQuirk(item.cameraId, item.extensionMode)) {
                        continue
                    }
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
                            isExtensionAvailableInOemImpl(
                                extensionsManager,
                                cameraSelector,
                                item.extensionMode,
                            )
                    ) {
                        result.add(item)
                    }
                }
                return result
            } catch (e: Exception) {
                // ExtensionsManager can't be retrieved, e.g. CameraX initialization takes too long
                // on low-end devices. Falls back to the Camera2Extensions support list instead of
                // returning all combinations, which would otherwise make all the tests fail with
                // misleading "Mode N is not supported!" errors on the devices that don't support
                // any extensions.
                Log.w(TAG, "Failed to filter out unavailable extension modes.", e)
                // The devices that only support Camera2Extensions can't rely on the
                // Camera2Extensions support list. See isDeviceOnlySupportedInCamera2Extensions().
                if (isDeviceOnlySupportedInCamera2Extensions()) {
                    return emptyList()
                }
                return list.filter {
                    !isExtensionDisabledByQuirk(it.cameraId, it.extensionMode) &&
                        runCatching {
                                isCamera2ExtensionsSupported(context, it.cameraId, it.extensionMode)
                            }
                            .getOrDefault(false)
                }
            } finally {
                try {
                    val provider = cameraProvider
                    if (provider != null) {
                        provider.shutdownAsync().get()
                    } else {
                        // Shuts down the camera provider once its initialization completes, so
                        // that it won't be leaked to the tests when getInstance() timed out.
                        cameraProviderFuture.addListener(
                            { runCatching { cameraProviderFuture.get().shutdownAsync() } },
                            directExecutor(),
                        )
                    }
                    extensionsManager?.shutdown()?.get()
                } catch (_: Exception) {}
            }
        }

        /** Checks whether the extension mode is disabled by [ExtensionDisabledQuirk]. */
        @JvmStatic
        private fun isExtensionDisabledByQuirk(cameraId: String, extensionMode: Int): Boolean =
            DeviceQuirks.get(ExtensionDisabledQuirk::class.java)
                ?.shouldDisableExtension(cameraId, extensionMode) == true

        @JvmStatic
        private fun isExtensionAvailableInOemImpl(
            extensionsManager: ExtensionsManager,
            cameraSelector: CameraSelector,
            extensionMode: Int,
        ) =
            ExtensionsTestUtil.isExtensionAvailable(
                extensionsManager,
                cameraSelector,
                extensionMode,
            )

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
            cameraXExtensionMode: Int,
        ): Boolean {
            // CameraX only enables Camera2Extensions on API level 33 or above. See
            // ExtensionsInfo#getVendorExtender().
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
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

        private const val TAG = "ReleaseApkTest"

        /** The timeout to retrieve the [ProcessCameraProvider] instance. */
        private const val CAMERA_PROVIDER_TIMEOUT_SECONDS = 10L

        @JvmStatic
        private val AVAILABLE_EXTENSION_MODES = arrayOf(BOKEH, HDR, NIGHT, FACE_RETOUCH, AUTO)
    }
}
