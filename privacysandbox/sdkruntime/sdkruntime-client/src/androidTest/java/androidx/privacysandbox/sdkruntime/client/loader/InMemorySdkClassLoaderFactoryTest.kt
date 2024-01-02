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
import androidx.privacysandbox.sdkruntime.client.TestSdkConfigs
import androidx.privacysandbox.sdkruntime.client.config.LocalSdkConfig
import androidx.privacysandbox.sdkruntime.core.LoadSdkCompatException
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
class InMemorySdkClassLoaderFactoryTest {

    private lateinit var factoryUnderTest: InMemorySdkClassLoaderFactory
    private lateinit var singleDexSdkInfo: LocalSdkConfig
    private lateinit var multipleDexSdkInfo: LocalSdkConfig

    @Before
    fun setUp() {
        factoryUnderTest = InMemorySdkClassLoaderFactory.create(
            ApplicationProvider.getApplicationContext()
        )
        singleDexSdkInfo = TestSdkConfigs.CURRENT
        assertThat(singleDexSdkInfo.dexPaths.size).isEqualTo(1)

        multipleDexSdkInfo = TestSdkConfigs.CURRENT_WITH_RESOURCES
        assertThat(multipleDexSdkInfo.dexPaths.size).isEqualTo(2)
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O_MR1)
    fun createClassLoaderFor_whenApi27AndMultipleDex_returnClassloader() {
        val classLoader = factoryUnderTest.createClassLoaderFor(
            multipleDexSdkInfo,
            javaClass.classLoader!!
        )
        val loadedEntryPointClass = classLoader.loadClass(multipleDexSdkInfo.entryPoint)
        assertThat(loadedEntryPointClass.classLoader).isEqualTo(classLoader)
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    fun createClassLoaderFor_whenApi26AndSingleDex_returnClassloader() {
        val classLoader = factoryUnderTest.createClassLoaderFor(
            singleDexSdkInfo,
            javaClass.classLoader!!
        )
        val loadedEntryPointClass = classLoader.loadClass(singleDexSdkInfo.entryPoint)
        assertThat(loadedEntryPointClass.classLoader).isEqualTo(classLoader)
    }

    @Test
    @SdkSuppress(maxSdkVersion = Build.VERSION_CODES.O)
    fun createClassLoaderFor_whenApiPre27AndMultipleDex_throwsSandboxDisabledException() {
        val ex = assertThrows(LoadSdkCompatException::class.java) {
            factoryUnderTest.createClassLoaderFor(
                multipleDexSdkInfo,
                javaClass.classLoader!!
            )
        }

        assertThat(ex.loadSdkErrorCode)
            .isEqualTo(LoadSdkCompatException.LOAD_SDK_SDK_SANDBOX_DISABLED)
        assertThat(ex)
            .hasMessageThat()
            .startsWith("Can't use InMemoryDexClassLoader")
    }

    @Test
    @SdkSuppress(maxSdkVersion = Build.VERSION_CODES.N_MR1)
    fun createClassLoaderFor_whenApiPre26AndSingleDex_throwsSandboxDisabledException() {
        val ex = assertThrows(LoadSdkCompatException::class.java) {
            factoryUnderTest.createClassLoaderFor(
                singleDexSdkInfo,
                javaClass.classLoader!!
            )
        }

        assertThat(ex.loadSdkErrorCode)
            .isEqualTo(LoadSdkCompatException.LOAD_SDK_SDK_SANDBOX_DISABLED)
        assertThat(ex)
            .hasMessageThat()
            .isEqualTo("Can't use InMemoryDexClassLoader")
    }
}
