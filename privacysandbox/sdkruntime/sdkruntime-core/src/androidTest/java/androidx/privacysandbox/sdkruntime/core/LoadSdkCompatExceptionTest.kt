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
package androidx.privacysandbox.sdkruntime.core

import android.app.sdksandbox.LoadSdkException
import android.os.Build.VERSION_CODES.TIRAMISU
import android.os.Bundle
import android.os.ext.SdkExtensions.AD_SERVICES
import androidx.annotation.RequiresExtension
import androidx.privacysandbox.sdkruntime.core.LoadSdkCompatException.Companion.toLoadCompatSdkException
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
// TODO(b/262577044) Remove RequiresExtension after extensions support in @SdkSuppress
@RequiresExtension(extension = AD_SERVICES, version = 4)
@SdkSuppress(minSdkVersion = TIRAMISU)
class LoadSdkCompatExceptionTest {

    @Before
    fun setUp() {
        assumeTrue("Requires Sandbox API available", isSandboxApiAvailable())
    }

    @Test
    fun toLoadSdkException_returnLoadSdkException() {
        val loadSdkCompatException = LoadSdkCompatException(RuntimeException(), Bundle())

        val loadSdkException = loadSdkCompatException.toLoadSdkException()

        assertThat(loadSdkException.cause)
            .isSameInstanceAs(loadSdkCompatException.cause)
        assertThat(loadSdkException.extraInformation)
            .isSameInstanceAs(loadSdkCompatException.extraInformation)
        assertThat(loadSdkException.loadSdkErrorCode)
            .isEqualTo(loadSdkCompatException.loadSdkErrorCode)
    }

    @Test
    fun toLoadCompatSdkException_returnLoadCompatSdkException() {
        val loadSdkException = LoadSdkException(
            RuntimeException(),
            Bundle()
        )

        val loadCompatSdkException = toLoadCompatSdkException(loadSdkException)

        assertThat(loadCompatSdkException.cause)
            .isSameInstanceAs(loadSdkException.cause)
        assertThat(loadCompatSdkException.extraInformation)
            .isSameInstanceAs(loadSdkException.extraInformation)
        assertThat(loadCompatSdkException.loadSdkErrorCode)
            .isEqualTo(loadSdkException.loadSdkErrorCode)
    }

    private fun isSandboxApiAvailable() =
        AdServicesInfo.isAtLeastV4()
}
