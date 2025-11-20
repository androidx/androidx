/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.camera.integration.core

import android.Manifest
import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.os.Handler
import android.os.HandlerThread
import androidx.camera.camera2.Camera2Config
import androidx.camera.camera2.pipe.integration.CameraPipeConfig
import androidx.camera.core.CameraX
import androidx.camera.core.CameraXConfig
import androidx.camera.integration.core.CameraXActivity.BIND_IMAGE_CAPTURE
import androidx.camera.integration.core.CameraXActivity.BIND_PREVIEW
import androidx.camera.integration.core.CameraXActivity.INTENT_EXTRA_CAMERA_ID
import androidx.camera.integration.core.CameraXActivity.INTENT_EXTRA_USE_CASE_COMBINATION
import androidx.camera.integration.core.util.StressTestUtil
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.testing.impl.CameraPipeConfigTestRule
import androidx.camera.testing.impl.CameraUtil
import androidx.camera.testing.impl.CameraUtil.PreTestCameraIdList
import androidx.camera.testing.impl.CoreAppTestUtil
import androidx.camera.testing.impl.LabTestRule
import androidx.camera.testing.impl.activity.Camera2TestActivity
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.idling.CountingIdlingResource
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import androidx.test.uiautomator.UiDevice
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.After
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/** Tests for [CameraX] which varies use case combinations to run. */
@LargeTest
@RunWith(Parameterized::class)
class CameraDisconnectTest(
    private val testName: String,
    private val lensFacing: Int,
    private val implName: String,
    private val cameraConfig: CameraXConfig,
) {

    @get:Rule
    val cameraPipeConfigTestRule =
        CameraPipeConfigTestRule(active = implName == CameraPipeConfig::class.simpleName)

    @get:Rule
    val cameraRule =
        CameraUtil.grantCameraPermissionAndPreTestAndPostTest(PreTestCameraIdList(cameraConfig))

    @get:Rule
    val permissionRule: GrantPermissionRule =
        GrantPermissionRule.grant(
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.RECORD_AUDIO,
        )

    @get:Rule val labTestRule = LabTestRule()

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun data() =
            mutableListOf<Array<Any?>>().apply {
                CameraUtil.getAvailableCameraSelectors().forEach { selector ->
                    val lensFacing = selector.lensFacing
                    add(
                        arrayOf(
                            "config=${Camera2Config::class.simpleName} lensFacing={$lensFacing}",
                            lensFacing,
                            Camera2Config::class.simpleName,
                            Camera2Config.defaultConfig(),
                        )
                    )
                    add(
                        arrayOf(
                            "config=${CameraPipeConfig::class.simpleName} lensFacing={$lensFacing}",
                            lensFacing,
                            CameraPipeConfig::class.simpleName,
                            CameraPipeConfig.defaultConfig(),
                        )
                    )
                }
            }
    }

    private val context: Context = ApplicationProvider.getApplicationContext()
    private lateinit var cameraXActivityScenario: ActivityScenario<CameraXActivity>
    private lateinit var cameraProvider: ProcessCameraProvider
    private lateinit var backgroundCameraHandlerThread: HandlerThread
    private val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    private lateinit var cameraId: String

    @Before
    fun setUp() {
        assumeTrue(CameraUtil.hasCameraWithLensFacing(lensFacing))
        device.setOrientationNatural()
        CoreAppTestUtil.assumeCompatibleDevice()
        CoreAppTestUtil.assumeCanTestCameraDisconnect()
        ProcessCameraProvider.configureInstance(cameraConfig)
        cameraProvider = ProcessCameraProvider.getInstance(context)[10, TimeUnit.SECONDS]
        // Clear the device UI and check if there is no dialog or lock screen on the top of the
        // window before start the test.
        CoreAppTestUtil.prepareDeviceUI(InstrumentationRegistry.getInstrumentation())
        cameraId = CameraUtil.getCameraIdWithLensFacing(lensFacing)!!
    }

    @After
    fun tearDown(): Unit = runBlocking {
        if (::cameraXActivityScenario.isInitialized) {
            cameraXActivityScenario.close()
        }

        if (::cameraProvider.isInitialized) {
            withContext(Dispatchers.Main) {
                cameraProvider.shutdownAsync()[10000, TimeUnit.MILLISECONDS]
            }
        }

        if (::backgroundCameraHandlerThread.isInitialized) {
            backgroundCameraHandlerThread.quitSafely()
        }

        // Unfreeze rotation so the device can choose the orientation via its own policy. Be nice
        // to other tests :)
        device.unfreezeRotation()
        device.pressHome()
        device.waitForIdle(StressTestUtil.HOME_TIMEOUT_MS)
    }

    private fun launchAndAwaitCamera2Activity(cameraId: String) {
        val intent =
            Intent(context, Camera2TestActivity::class.java).apply {
                putExtra(Camera2TestActivity.EXTRA_CAMERA_ID, cameraId)
            }

        var completionIdlingResource: CountingIdlingResource? = null
        var wasCamera2PreviewReady = false
        try {
            ActivityScenario.launch<Camera2TestActivity>(intent).use { scenario ->
                var previewStartedIdlingResource: CountingIdlingResource? = null
                scenario.onActivity { activity ->
                    completionIdlingResource = activity.completionIdlingResource
                    previewStartedIdlingResource = activity.previewStartedIdlingResource
                    IdlingRegistry.getInstance().register(completionIdlingResource)
                }
                Espresso.onIdle() // Wait for the completionIdlingResource to become idle.
                wasCamera2PreviewReady = previewStartedIdlingResource!!.isIdleNow
            }
        } finally {
            completionIdlingResource?.let { IdlingRegistry.getInstance().unregister(it) }
        }

        assumeTrue(
            "Camera2TestActivity failed to start preview, skipping recovery test.",
            wasCamera2PreviewReady,
        )
    }

    /**
     * This test simulate the case that an app has two activities with camera function and the
     * second activity is not implemented by CameraX. The camera function should work when any of
     * the activity is in foreground.
     *
     * In camera-camera2, the camera is restarted by ON_START event. The camera-camera2 impl doesn't
     * handle the onCameraAccessPrioritiesChanged event and won't block the second activity to open
     * the camera.
     *
     * In camera-pipe, onCameraAccessPrioritiesChanged is handled to force restart the camera.
     * ActiveResumeMode isn't referred to skip the restart flow. The second activity can't open the
     * camera successfully. The test is skipped now (b/341920670).
     */
    @LabTestRule.LabTestOnly
    @Test
    fun canRecovered_afterSecondCamera2ImplementationActivityIsClosed() {
        // Launch CameraX activity
        cameraXActivityScenario = launchCameraXActivity(cameraId)
        with(cameraXActivityScenario) {
            use {
                // Wait for preview to become active
                waitForViewfinderIdle()

                // Launch Camera2 test activity to disconnect CameraX, and wait for it to succeed.
                launchAndAwaitCamera2Activity(cameraId)

                // Wait for CameraXActivity's preview to become active again.
                waitForViewfinderIdle()
            }
        }
    }

    /**
     * This test simulate the case that the CameraXActivity lifecycle state doesn't change but
     * camera is disconnected by some unknown reasons.
     *
     * CameraX can receive the onCameraAvailable callback to reopen the camera.
     *
     * In camera-camera2, the camera can also be restarted by the flow: Camera2CameraImpl ->
     * onDisconnected -> tryForceOpenCameraDevice when ActiveResumeMode is enabled.
     */
    @LabTestRule.LabTestOnly
    @Test
    fun canRecovered_afterReceivingCameraOnDisconnectedEvent() {
        // Launch CameraX activity
        cameraXActivityScenario = launchCameraXActivity(cameraId)
        with(cameraXActivityScenario) {
            use {
                // Wait for preview to become active
                waitForViewfinderIdle()

                openCamera(cameraId)

                // Wait for CameraXActivity's preview to become active after Camera2TestActivity is
                // closed.
                waitForViewfinderIdle()
            }
        }
    }

    private fun openCamera(cameraId: String): Unit = runBlocking {
        val cameraLock = Any()
        val cameraOpenCountDownLatch = CountDownLatch(1)
        val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        var cameraDevice: CameraDevice? = null

        backgroundCameraHandlerThread = HandlerThread("CameraBackground")
        backgroundCameraHandlerThread.start()
        val backgroundCameraHandler = Handler(backgroundCameraHandlerThread.getLooper())
        cameraManager.openCamera(
            cameraId,
            object : CameraDevice.StateCallback() {
                override fun onOpened(camera: CameraDevice) {
                    synchronized(cameraLock) { cameraDevice = camera }
                    cameraOpenCountDownLatch.countDown()
                }

                override fun onDisconnected(camera: CameraDevice) {
                    camera.close()
                    synchronized(cameraLock) { cameraDevice = null }
                }

                override fun onError(camera: CameraDevice, error: Int) {
                    camera.close()
                    synchronized(cameraLock) { cameraDevice = null }
                }
            },
            backgroundCameraHandler,
        )

        assertThat(cameraOpenCountDownLatch.await(1000, TimeUnit.MILLISECONDS)).isTrue()

        // Closes the camera so that CameraX can receive the onCameraAvailable callback to reopen
        // the camera.
        synchronized(cameraLock) { cameraDevice?.close() }
    }

    private fun launchCameraXActivity(cameraId: String): ActivityScenario<CameraXActivity> {
        val coreTestAppPackage = "androidx.camera.integration.core"
        val intent =
            ApplicationProvider.getApplicationContext<Context>()
                .packageManager
                .getLaunchIntentForPackage(coreTestAppPackage)!!
                .apply {
                    putExtra(INTENT_EXTRA_CAMERA_ID, cameraId)
                    putExtra(INTENT_EXTRA_USE_CASE_COMBINATION, BIND_PREVIEW or BIND_IMAGE_CAPTURE)
                    setClassName(coreTestAppPackage, CameraXActivity::class.java.name)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }

        val activityScenario: ActivityScenario<CameraXActivity> = ActivityScenario.launch(intent)

        return activityScenario
    }
}
