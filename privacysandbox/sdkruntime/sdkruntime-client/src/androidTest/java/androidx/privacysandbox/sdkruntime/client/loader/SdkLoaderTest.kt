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

import android.os.Build
import androidx.privacysandbox.sdkruntime.client.config.LocalSdkConfig
import androidx.privacysandbox.sdkruntime.client.config.ResourceRemappingConfig
import androidx.privacysandbox.sdkruntime.core.Versions
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.O_MR1)
class SdkLoaderTest {

    private lateinit var sdkLoader: SdkLoader

    private lateinit var testSdkConfig: LocalSdkConfig

    @Before
    fun setUp() {
        sdkLoader = SdkLoader.create(
            ApplicationProvider.getApplicationContext()
        )
        testSdkConfig = LocalSdkConfig(
            dexPaths = listOf(
                "RuntimeEnabledSdks/V1/classes.dex",
                "RuntimeEnabledSdks/RPackage.dex"
            ),
            entryPoint = "androidx.privacysandbox.sdkruntime.test.v1.CompatProvider",
            javaResourcesRoot = "RuntimeEnabledSdks/V1/javaresources",
            resourceRemapping = ResourceRemappingConfig(
                rPackageClassName = "androidx.privacysandbox.sdkruntime.test.RPackage",
                packageId = 42
            )
        )
    }

    @Test
    fun loadSdk_callVersionsHandShake() {
        val loadedSdk = sdkLoader.loadSdk(testSdkConfig)

        assertThat(loadedSdk.extractClientVersion())
            .isEqualTo(Versions.API_VERSION)
    }

    @Test
    fun testContextClassloader() {
        val loadedSdk = sdkLoader.loadSdk(testSdkConfig)

        val classLoader = loadedSdk.extractSdkProviderClassloader()
        val sdkContext = loadedSdk.extractSdkContext()

        assertThat(sdkContext.classLoader)
            .isSameInstanceAs(classLoader)
    }

    @Test
    fun testJavaResources() {
        val loadedSdk = sdkLoader.loadSdk(testSdkConfig)

        val classLoader = loadedSdk.extractSdkProviderClassloader()
        val content = classLoader.getResourceAsStream("test.txt").use { inputStream ->
            inputStream.bufferedReader().readLine()
        }

        assertThat(content)
            .isEqualTo("test")
    }

    @Test
    fun testRPackageUpdate() {
        val loadedSdk = sdkLoader.loadSdk(testSdkConfig)

        val classLoader = loadedSdk.extractSdkProviderClassloader()

        val rPackageClass =
            classLoader.loadClass("androidx.privacysandbox.sdkruntime.test.RPackage")

        val packageIdField = rPackageClass.getDeclaredField("packageId")
        val value = packageIdField.get(null)

        assertThat(value).isEqualTo(42)
    }
}