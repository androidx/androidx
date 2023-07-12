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
            "androidx.privacysandbox.sdkruntime.test.v1"
        )

        assertThat(result)
            .isEqualTo(
                LocalSdkConfig(
                    packageName = "androidx.privacysandbox.sdkruntime.test.v1",
                    versionMajor = 42,
                    dexPaths = listOf("RuntimeEnabledSdks/V1/classes.dex"),
                    entryPoint = "androidx.privacysandbox.sdkruntime.test.v1.CompatProvider",
                    javaResourcesRoot = "RuntimeEnabledSdks/V1/javaresources"
                )
            )
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