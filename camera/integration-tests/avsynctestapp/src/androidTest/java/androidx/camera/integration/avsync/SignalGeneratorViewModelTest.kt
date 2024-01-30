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

package androidx.camera.integration.avsync

import android.content.Context
import android.os.Build
import androidx.camera.camera2.Camera2Config
import androidx.camera.core.CameraSelector
import androidx.camera.testing.impl.CameraUtil
import androidx.camera.testing.impl.CameraXUtil
import androidx.camera.testing.impl.fakes.FakeLifecycleOwner
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.rule.GrantPermissionRule
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.After
import org.junit.Assume
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class SignalGeneratorViewModelTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private lateinit var viewModel: SignalGeneratorViewModel
    private lateinit var lifecycleOwner: FakeLifecycleOwner
    private val fakeViewModelStoreOwner = object : ViewModelStoreOwner {
        private val vmStore = ViewModelStore()

        override val viewModelStore = vmStore

        fun clear() {
            vmStore.clear()
        }
    }

    @get:Rule
    val useCamera = CameraUtil.grantCameraPermissionAndPreTest(
        CameraUtil.PreTestCameraIdList(Camera2Config.defaultConfig())
    )

    @get:Rule
    val grantPermissionRule: GrantPermissionRule = GrantPermissionRule.grant(
        android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
        android.Manifest.permission.RECORD_AUDIO
    )

    @Before
    fun setUp(): Unit = runBlocking {
        // Skip for b/168175357, b/233661493
        Assume.assumeFalse(
            "Skip tests for Cuttlefish MediaCodec issues",
            Build.MODEL.contains("Cuttlefish") &&
                (Build.VERSION.SDK_INT == 29 || Build.VERSION.SDK_INT == 33)
        )

        val viewModelProvider = ViewModelProvider(fakeViewModelStoreOwner)
        viewModel = viewModelProvider[SignalGeneratorViewModel::class.java]

        withContext(Dispatchers.Main) {
            lifecycleOwner = FakeLifecycleOwner()
            lifecycleOwner.startAndResume()
        }
    }

    @After
    fun tearDown(): Unit = runBlocking {
        fakeViewModelStoreOwner.clear()

        if (::lifecycleOwner.isInitialized) {
            withContext(Dispatchers.Main) {
                lifecycleOwner.pauseAndStop()
                lifecycleOwner.destroy()
            }
        }

        // Ensure all cameras are released for the next test
        CameraXUtil.shutdown()[10, TimeUnit.SECONDS]
    }

    @Test
    fun initialRecorder_canMakeRecorderReady(): Unit = runBlocking {
        Assume.assumeTrue(CameraUtil.hasCameraWithLensFacing(CameraSelector.LENS_FACING_FRONT))

        viewModel.initialRecorder(context, lifecycleOwner)

        assertThat(viewModel.isRecorderReady).isTrue()
    }

    @Test
    fun initialSignalGenerator_canMakeGeneratorReady(): Unit = runBlocking {
        val beepFrequency = 1500
        viewModel.initialSignalGenerator(context, beepFrequency, true)

        assertThat(viewModel.isGeneratorReady).isTrue()
    }

    @Test
    fun startSignalGeneration_canMakeActiveFlagChangePeriodically(): Unit = runBlocking {
        // Arrange.
        val beepFrequency = 1500
        val latch = CountDownLatch(5)

        // Act.
        viewModel.initialSignalGenerator(context, beepFrequency, true)
        viewModel.startSignalGeneration()
        countActiveFlagChangeBlocking(latch)

        // Verify.
        assertThat(latch.count).isEqualTo(0)
    }

    @Test
    fun stopSignalGeneration_canMakeActiveFlagStopChanging(): Unit = runBlocking {
        // Arrange.
        val beepFrequency = 1500
        val latch = CountDownLatch(5)

        // Act.
        viewModel.initialSignalGenerator(context, beepFrequency, true)
        viewModel.startSignalGeneration()
        viewModel.stopSignalGeneration()
        countActiveFlagChangeBlocking(latch)

        // Verify.
        assertThat(latch.count).isNotEqualTo(0)
    }

    @Test
    fun startAndStopRecording_canWorkCorrectlyAfterRecorderReady(): Unit = runBlocking {
        Assume.assumeTrue(CameraUtil.hasCameraWithLensFacing(CameraSelector.LENS_FACING_FRONT))

        // Arrange.
        viewModel.initialRecorder(context, lifecycleOwner)

        assertThat(viewModel.isRecorderReady).isTrue()

        // Act. and Verify.
        viewModel.startRecording(context)
        assertThat(viewModel.isRecording).isTrue()

        viewModel.stopRecording()
        assertThat(viewModel.isRecording).isFalse()
    }

    private fun countActiveFlagChangeBlocking(latch: CountDownLatch, timeoutSec: Long = 5L) {
        var preFlag = false
        val endTimeMillis = System.currentTimeMillis() + timeoutSec * 1000
        while (endTimeMillis > System.currentTimeMillis()) {
            if (viewModel.isActivePeriod != preFlag) {
                preFlag = viewModel.isActivePeriod
                latch.countDown()
            }
        }
    }
}
