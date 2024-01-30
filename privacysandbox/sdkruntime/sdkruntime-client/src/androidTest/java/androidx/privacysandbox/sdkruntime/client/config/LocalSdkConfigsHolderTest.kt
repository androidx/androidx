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
package androidx.privacysandbox.sdkruntime.client.config

import androidx.privacysandbox.sdkruntime.client.TestSdkConfigs
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
class LocalSdkConfigsHolderTest {

    @Test
    fun load_whenSdkTableNotExists_doesNotThrowException() {
        val configHolder = LocalSdkConfigsHolder.load(
            ApplicationProvider.getApplicationContext(),
            sdkTableAssetName = "not-exists"
        )
        val result = configHolder.getSdkConfig("sdk")
        assertThat(result).isNull()
    }

    @Test
    fun getSdkConfig_whenSdkExists_returnSdkInfo() {
        val configHolder = LocalSdkConfigsHolder.load(
            ApplicationProvider.getApplicationContext()
        )

        val result = configHolder.getSdkConfig(
            TestSdkConfigs.CURRENT.packageName
        )

        val expectedConfig = LocalSdkConfig(
            packageName = "androidx.privacysandbox.sdkruntime.testsdk.current",
            versionMajor = 42,
            dexPaths = listOf("test-sdks/current/classes.dex"),
            entryPoint = "androidx.privacysandbox.sdkruntime.testsdk.current.CompatProvider",
        )

        assertThat(result).isEqualTo(expectedConfig)
    }

    @Test
    fun getSdkConfig_whenSdkNotExists_returnNull() {
        val configHolder = LocalSdkConfigsHolder.load(
            ApplicationProvider.getApplicationContext()
        )

        val result = configHolder.getSdkConfig("not-exists")

        assertThat(result).isNull()
    }
}
