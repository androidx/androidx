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
package androidx.privacysandbox.sdkruntime.client.loader

import android.content.Context
import android.os.Build
import androidx.privacysandbox.sdkruntime.client.TestSdkConfigs
import androidx.privacysandbox.sdkruntime.client.config.LocalSdkConfig
import androidx.privacysandbox.sdkruntime.core.LoadSdkCompatException
import androidx.privacysandbox.sdkruntime.core.Versions
import androidx.privacysandbox.sdkruntime.core.controller.SdkSandboxControllerCompat
import androidx.privacysandbox.sdkruntime.core.internal.ClientApiVersion
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import androidx.testutils.assertThrows
import com.google.common.truth.Truth.assertThat
import java.io.File
import java.lang.reflect.Proxy
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
class SdkLoaderTest {

    private lateinit var sdkLoader: SdkLoader

    private lateinit var testSdkConfig: LocalSdkConfig

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        sdkLoader =
            SdkLoader.create(
                context = context,
                controllerFactory = NoOpFactory,
            )
        testSdkConfig = TestSdkConfigs.CURRENT_WITH_RESOURCES

        // Clean extracted SDKs between tests
        val codeCacheDir = File(context.applicationInfo.dataDir, "code_cache")
        File(codeCacheDir, "RuntimeEnabledSdk").deleteRecursively()
    }

    @Test
    fun loadSdk_callVersionsHandShake() {
        val loadedSdk = sdkLoader.loadSdk(testSdkConfig)

        assertThat(loadedSdk.extractClientVersion()).isEqualTo(Versions.API_VERSION)
    }

    @Test
    fun loadSdk_withCustomVersionHandshake_performsCustomHandShake() {
        val customVersionHandshake = VersionHandshake(overrideApiVersion = Int.MAX_VALUE)
        val loadedSdk = sdkLoader.loadSdk(testSdkConfig, customVersionHandshake)

        assertThat(loadedSdk.extractClientVersion()).isEqualTo(Int.MAX_VALUE)
    }

    @Test
    fun loadSdk_forUnsupportedApiVersion_throwsLoadSdkCompatException() {
        val customVersionHandshake =
            VersionHandshake(overrideApiVersion = ClientApiVersion.MIN_SUPPORTED.apiLevel - 1)

        assertThrows(LoadSdkCompatException::class.java) {
                sdkLoader.loadSdk(testSdkConfig, customVersionHandshake)
            }
            .hasMessageThat()
            .startsWith("SDK built with unsupported version of sdkruntime-provider library")
    }

    @Test
    fun testContextClassloader() {
        val loadedSdk = sdkLoader.loadSdk(testSdkConfig)

        val classLoader = loadedSdk.extractSdkProviderClassloader()
        val sdkContext = loadedSdk.extractSdkContext()

        assertThat(sdkContext.classLoader).isSameInstanceAs(classLoader)
    }

    @Test
    fun testContextFilesDir() {
        val loadedSdk = sdkLoader.loadSdk(testSdkConfig)

        val sdkContext = loadedSdk.extractSdkContext()

        val context = ApplicationProvider.getApplicationContext<Context>()
        val expectedSdksRoot = context.getDir("RuntimeEnabledSdksData", Context.MODE_PRIVATE)
        val expectedSdkData = File(expectedSdksRoot, testSdkConfig.packageName)
        val expectedSdkFilesDir = File(expectedSdkData, "files")

        assertThat(sdkContext.filesDir).isEqualTo(expectedSdkFilesDir)
    }

    @Test
    fun testJavaResources() {
        val loadedSdk = sdkLoader.loadSdk(testSdkConfig)

        val classLoader = loadedSdk.extractSdkProviderClassloader()
        val content =
            classLoader.getResourceAsStream("test.txt").use { inputStream ->
                inputStream.bufferedReader().readLine()
            }

        assertThat(content).isEqualTo("test")
    }

    @Test
    fun testRPackageUpdate() {
        val loadedSdk = sdkLoader.loadSdk(testSdkConfig)

        val classLoader = loadedSdk.extractSdkProviderClassloader()

        val rPackageClass =
            classLoader.loadClass("androidx.privacysandbox.sdkruntime.test.RPackage")

        val packageIdField = rPackageClass.getDeclaredField("packageId")
        val value = packageIdField.get(null)

        // 42 (0x2A) -> (0x2A000000)
        assertThat(value).isEqualTo(0x2A000000)
    }

    @Test
    @SdkSuppress(maxSdkVersion = Build.VERSION_CODES.O)
    fun testLowSpace_failPreApi27() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val sdkLoaderWithLowSpaceMode =
            SdkLoader.create(
                context = context,
                controllerFactory = NoOpFactory,
                lowSpaceThreshold = Long.MAX_VALUE
            )

        assertThrows(LoadSdkCompatException::class.java) {
                sdkLoaderWithLowSpaceMode.loadSdk(testSdkConfig)
            }
            .hasMessageThat()
            .startsWith("Can't use InMemoryDexClassLoader")
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O_MR1)
    fun testLowSpace_notFailApi27() {
        val sdkLoaderWithLowSpaceMode =
            SdkLoader.create(
                context = ApplicationProvider.getApplicationContext(),
                controllerFactory = NoOpFactory,
                lowSpaceThreshold = Long.MAX_VALUE
            )

        val loadedSdk = sdkLoaderWithLowSpaceMode.loadSdk(testSdkConfig)
        val classLoader = loadedSdk.extractSdkProviderClassloader()

        val entryPointClass = classLoader.loadClass(testSdkConfig.entryPoint)
        assertThat(entryPointClass).isNotNull()
    }

    private object NoOpFactory : SdkLoader.ControllerFactory {

        val controllerImplClass = SdkSandboxControllerCompat.SandboxControllerImpl::class.java

        val noOpProxy =
            Proxy.newProxyInstance(controllerImplClass.classLoader, arrayOf(controllerImplClass)) {
                proxy,
                method,
                args ->
                throw UnsupportedOperationException(
                    "Unexpected method call (NoOp) object:$proxy, method: $method, args: $args"
                )
            } as SdkSandboxControllerCompat.SandboxControllerImpl

        override fun createControllerFor(sdkConfig: LocalSdkConfig) = noOpProxy
    }
}
