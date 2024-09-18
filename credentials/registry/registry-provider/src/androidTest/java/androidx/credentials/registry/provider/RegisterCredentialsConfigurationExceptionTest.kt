/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.credentials.registry.provider

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
class RegisterCredentialsConfigurationExceptionTest {
    @Test
    fun construction_success() {
        val e = RegisterCredentialsConfigurationException("error msg")

        assertThat(e.type)
            .isEqualTo(
                "androidx.credentials.provider.registry.TYPE_REGISTER_CREDENTIALS_CONFIGURATION_EXCEPTION"
            )
        assertThat(e.message).isEqualTo("error msg")
    }

    @Test
    fun construction_nullMessage_success() {
        val e = RegisterCredentialsConfigurationException()

        assertThat(e.type)
            .isEqualTo(
                "androidx.credentials.provider.registry.TYPE_REGISTER_CREDENTIALS_CONFIGURATION_EXCEPTION"
            )
        assertThat(e.message).isNull()
    }
}
