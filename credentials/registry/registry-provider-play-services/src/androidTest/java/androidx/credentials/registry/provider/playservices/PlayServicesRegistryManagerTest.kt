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

package androidx.credentials.registry.provider.playservices

import androidx.credentials.registry.provider.RegisterCredentialsRequest
import androidx.credentials.registry.provider.RegistryManager
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
class PlayServicesRegistryManagerTest {
    private val context = InstrumentationRegistry.getInstrumentation().context

    private lateinit var registryManager: RegistryManager
    private lateinit var playServicesImpl: RegistryManagerProviderPlayServicesImpl

    @Before
    fun setup() {
        registryManager = RegistryManager.create(context)
        playServicesImpl = RegistryManagerProviderPlayServicesImpl(context)
    }

    @Ignore // Wait to enable when the flags fully propagate
    @Test
    fun registerCredentials_success() =
        runBlocking<Unit> {
            if (playServicesImpl.isAvailableOnDevice()) {
                val result =
                    registryManager.registerCredentials(
                        object :
                            RegisterCredentialsRequest("type", "id", ByteArray(4), ByteArray(8)) {}
                    )

                assertThat(result.type).isEqualTo("type")
            }
        }
}
