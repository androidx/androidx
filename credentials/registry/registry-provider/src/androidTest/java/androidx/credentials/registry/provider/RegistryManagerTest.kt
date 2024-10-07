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
import androidx.test.platform.app.InstrumentationRegistry
import androidx.testutils.assertThrows
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
class RegistryManagerTest {
    private val context = InstrumentationRegistry.getInstrumentation().context

    private lateinit var registryManager: RegistryManager

    @Before
    fun setup() {
        registryManager = RegistryManager.create(context)
    }

    @Test
    fun registerCredentials_noOptionalModule_throws() =
        runBlocking<Unit> {
            assertThrows<RegisterCredentialsConfigurationException> {
                registryManager.registerCredentials(
                    object : RegisterCredentialsRequest("type", "id", ByteArray(4), ByteArray(8)) {}
                )
            }
        }

    @Test
    fun constant() {
        assertThat(RegistryManager.ACTION_GET_CREDENTIAL)
            .isEqualTo("androidx.credentials.registry.provider.action.GET_CREDENTIAL")
    }
}
