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

import androidx.privacysandbox.sdkruntime.client.config.LocalSdkConfig
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
class JavaResourcesLoadingClassLoaderFactoryTest {

    private lateinit var appClassloader: ClassLoader
    private lateinit var factoryUnderTest: JavaResourcesLoadingClassLoaderFactory
    private lateinit var testSdkConfig: LocalSdkConfig

    @Before
    fun setUp() {
        appClassloader = javaClass.classLoader!!
        factoryUnderTest = JavaResourcesLoadingClassLoaderFactory(
            appClassloader
        )
        testSdkConfig = LocalSdkConfig(
            listOf("RuntimeEnabledSdks/V1/classes.dex"),
            "RuntimeEnabledSdks/V1/javaresources",
            "androidx.privacysandbox.sdkruntime.test.v1.CompatProvider"
        )
    }

    @Test
    fun getResource_delegateToAppClassloaderWithPrefix() {
        val classLoader = factoryUnderTest.loadSdk(testSdkConfig, appClassloader.parent!!)
        val resource = classLoader.getResource("test.txt")

        val appResource = appClassloader.getResource(
            "assets/RuntimeEnabledSdks/V1/javaresources/test.txt"
        )
        assertThat(resource).isNotNull()
        assertThat(resource).isEqualTo(appResource)
    }

    @Test
    fun getResource_whenAppResource_returnNull() {
        val classLoader = factoryUnderTest.loadSdk(testSdkConfig, appClassloader.parent!!)

        val resource = classLoader.getResource("assets/RuntimeEnabledSdkTable.xml")
        val appResource = appClassloader.getResource("assets/RuntimeEnabledSdkTable.xml")

        assertThat(appResource).isNotNull()
        assertThat(resource).isNull()
    }

    @Test
    fun getResources_delegateToAppClassloaderWithPrefix() {
        val classLoader = factoryUnderTest.loadSdk(testSdkConfig, appClassloader.parent!!)

        val resources = classLoader.getResources("test.txt")
        assertThat(resources.hasMoreElements()).isTrue()
        val resource = resources.nextElement()
        assertThat(resources.hasMoreElements()).isFalse()

        val appResources = appClassloader.getResources(
            "assets/RuntimeEnabledSdks/V1/javaresources/test.txt"
        )
        assertThat(appResources.hasMoreElements()).isTrue()
        val appResource = appResources.nextElement()
        assertThat(appResources.hasMoreElements()).isFalse()

        assertThat(resource).isEqualTo(appResource)
    }

    @Test
    fun getResources_whenAppResource_returnEmpty() {
        val classLoader = factoryUnderTest.loadSdk(testSdkConfig, appClassloader.parent!!)

        val resources = classLoader.getResources("assets/RuntimeEnabledSdkTable.xml")
        val appResources = appClassloader.getResources("assets/RuntimeEnabledSdkTable.xml")

        assertThat(appResources.hasMoreElements()).isTrue()
        assertThat(resources.hasMoreElements()).isFalse()
    }
}