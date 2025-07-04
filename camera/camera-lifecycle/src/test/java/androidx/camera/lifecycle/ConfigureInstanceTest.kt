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

package androidx.camera.lifecycle

import android.content.Context
import android.os.Build
import androidx.annotation.OptIn
import androidx.camera.core.CameraXConfig
import androidx.camera.testing.fakes.FakeAppConfig
import androidx.concurrent.futures.await
import androidx.test.core.app.ApplicationProvider
import androidx.testutils.assertThrows
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument

@RunWith(RobolectricTestRunner::class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
class ConfigureInstanceTest {
    private val context = ApplicationProvider.getApplicationContext() as Context
    private lateinit var provider: ProcessCameraProvider

    @After
    fun tearDown(): Unit = runBlocking {
        try {
            val provider = ProcessCameraProvider.getInstance(context).await()
            provider.shutdownAsync().await()
        } catch (e: IllegalStateException) {
            // ProcessCameraProvider may not be configured. Ignore.
        }
    }

    @Test
    fun canGetInstance_fromMetaData(): Unit = runBlocking {
        // Check the static invocation count for the test CameraXConfig.Provider which is
        // defined in the instrumentation test's AndroidManifest.xml. It should be incremented after
        // retrieving the ProcessCameraProvider.
        val initialInvokeCount = TestMetaDataConfigProvider.invokeCount
        provider = ProcessCameraProvider.getInstance(context).await()
        assertThat(provider).isNotNull()
        assertThat(TestMetaDataConfigProvider.invokeCount).isGreaterThan(initialInvokeCount)
    }

    @OptIn(ExperimentalCameraProviderConfiguration::class)
    @Test
    fun configuredGetInstance_doesNotUseMetaData() {
        ProcessCameraProvider.configureInstance(FakeAppConfig.create())
        runBlocking {
            // Check the static invocation count for the test CameraXConfig.Provider which is
            // defined in the instrumentation test's AndroidManifest.xml. It should NOT be
            // incremented
            // after retrieving the ProcessCameraProvider since the ProcessCameraProvider is
            // explicitly configured.
            val initialInvokeCount = TestMetaDataConfigProvider.invokeCount
            provider = ProcessCameraProvider.getInstance(context).await()
            assertThat(provider).isNotNull()
            assertThat(TestMetaDataConfigProvider.invokeCount).isEqualTo(initialInvokeCount)
        }
    }

    @OptIn(ExperimentalCameraProviderConfiguration::class)
    @Test
    fun configuredGetInstance_doesNotUseApplication() {
        ProcessCameraProvider.configureInstance(FakeAppConfig.create())
        runBlocking {
            // Wrap the context with a TestAppContextWrapper and provide a context with an
            // Application that implements CameraXConfig.Provider. Because the ProcessCameraProvider
            // is already configured, this Application should not be used.
            val testApp = TestApplication(context)
            val contextWrapper = TestAppContextWrapper(context, testApp)
            provider = ProcessCameraProvider.getInstance(contextWrapper).await()
            assertThat(provider).isNotNull()
            assertThat(testApp.providerUsed).isFalse()
        }
    }

    @Test
    fun unconfiguredGetInstance_usesApplicationProvider(): Unit = runBlocking {
        val testApp = TestApplication(context)
        val contextWrapper = TestAppContextWrapper(context, testApp)
        provider = ProcessCameraProvider.getInstance(contextWrapper).await()
        assertThat(provider).isNotNull()
        assertThat(testApp.providerUsed).isTrue()
    }

    @OptIn(ExperimentalCameraProviderConfiguration::class)
    @Test
    fun multipleConfigureInstance_throwsISE() {
        val config = FakeAppConfig.create()
        ProcessCameraProvider.configureInstance(config)
        assertThrows<IllegalStateException> { ProcessCameraProvider.configureInstance(config) }
    }

    @OptIn(ExperimentalCameraProviderConfiguration::class)
    @Test
    fun configuredGetInstance_returnsProvider() {
        ProcessCameraProvider.configureInstance(FakeAppConfig.create())
        runBlocking {
            provider = ProcessCameraProvider.getInstance(context).await()
            assertThat(provider).isNotNull()
        }
    }

    @OptIn(ExperimentalCameraProviderConfiguration::class)
    @Test
    fun configuredGetInstance_usesConfiguredExecutor() {
        var executeCalled = false
        val config =
            CameraXConfig.Builder.fromConfig(FakeAppConfig.create())
                .setCameraExecutor { runnable ->
                    run {
                        executeCalled = true
                        Dispatchers.Default.asExecutor().execute(runnable)
                    }
                }
                .build()
        ProcessCameraProvider.configureInstance(config)
        runBlocking {
            ProcessCameraProvider.getInstance(context).await()
            assertThat(executeCalled).isTrue()
        }
    }

    @Config(minSdk = 30)
    @Test
    fun configuredGetInstance_useGivenAttributionTag() = runBlocking {
        // Arrange.
        val fakeAttributionTag = "1"
        val customContext = TestAppContextWrapper(context, attributionTag = fakeAttributionTag)

        // Act.
        val provider = LifecycleCameraProvider.createInstance(customContext)

        // Assert.
        assertThat((provider as LifecycleCameraProviderImpl).context!!.attributionTag)
            .isEqualTo(fakeAttributionTag)
    }

    @Config(minSdk = 34)
    @Test
    fun configuredGetInstance_useGivenDeviceId() = runBlocking {
        // Arrange.
        val fakeDeviceId = 1
        val customContext = TestAppContextWrapper(context, deviceId = fakeDeviceId)

        // Act.
        val provider = LifecycleCameraProvider.createInstance(customContext)

        // Assert.
        assertThat((provider as LifecycleCameraProviderImpl).context!!.deviceId)
            .isEqualTo(fakeDeviceId)
    }
}
