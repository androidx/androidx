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
import android.os.Bundle
import androidx.privacysandbox.sdkruntime.core.LoadSdkCompatException.Companion.toLoadCompatSdkException
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = 34)
class LoadSdkCompatExceptionTest {
    @Test
    fun toLoadSdkException_returnLoadSdkException() {
        val loadSdkCompatException = LoadSdkCompatException(RuntimeException(), Bundle())

        val loadSdkException = loadSdkCompatException.toLoadSdkException()

        assertThat(loadSdkException.cause).isSameInstanceAs(loadSdkCompatException.cause)
        assertThat(loadSdkException.extraInformation)
            .isSameInstanceAs(loadSdkCompatException.extraInformation)
        assertThat(loadSdkException.loadSdkErrorCode)
            .isEqualTo(loadSdkCompatException.loadSdkErrorCode)
    }

    @Test
    fun toLoadCompatSdkException_returnLoadCompatSdkException() {
        val loadSdkException = LoadSdkException(RuntimeException(), Bundle())

        val loadCompatSdkException = toLoadCompatSdkException(loadSdkException)

        assertThat(loadCompatSdkException.cause).isSameInstanceAs(loadSdkException.cause)
        assertThat(loadCompatSdkException.extraInformation)
            .isSameInstanceAs(loadSdkException.extraInformation)
        assertThat(loadCompatSdkException.loadSdkErrorCode)
            .isEqualTo(loadSdkException.loadSdkErrorCode)
    }
}
