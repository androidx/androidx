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

import androidx.credentials.CredentialManagerCallback
import androidx.credentials.registry.provider.RegisterCredentialsException
import androidx.credentials.registry.provider.RegisterCredentialsRequest
import androidx.credentials.registry.provider.RegisterCredentialsResponse
import androidx.credentials.registry.provider.RegistryManager
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@MediumTest
class PlayServicesRegistryManagerTest {
    private val context = InstrumentationRegistry.getInstrumentation().context
    private val executor =
        object : Executor {
            private val innerExecutor = Executors.newSingleThreadExecutor()
            @Volatile var executedHere = false

            override fun execute(command: Runnable) {
                executedHere = true
                innerExecutor.execute(command)
            }
        }

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
            if (playServicesImpl.isAvailable()) {
                val result =
                    registryManager.registerCredentials(
                        object :
                            RegisterCredentialsRequest(
                                "type",
                                "id",
                                ByteArray(4),
                                ByteArray(8),
                                intentAction = "com.example.ACTION_GET_CRED",
                            ) {}
                    )

                assertThat(result.type).isEqualTo("type")
            }
        }

    @Ignore // Wait to enable when the flags fully propagate
    @Test
    fun registerCredentialsAsync_success() = runBlocking {
        if (playServicesImpl.isAvailable()) {
            var resultType = ""
            registryManager.registerCredentialsAsync(
                object :
                    RegisterCredentialsRequest(
                        "type",
                        "id",
                        ByteArray(4),
                        ByteArray(8),
                        intentAction = "com.example.ACTION_GET_CRED",
                    ) {},
                null,
                executor,
                object :
                    CredentialManagerCallback<
                        RegisterCredentialsResponse,
                        RegisterCredentialsException,
                    > {
                    override fun onResult(result: RegisterCredentialsResponse) {
                        resultType = result.type
                    }

                    override fun onError(e: RegisterCredentialsException) {
                        throw e
                    }
                },
            )
            for (i in 0 until 20) {
                if (executor.executedHere) break
                delay(100)
            }
            assertThat(resultType).isEqualTo("type")
            assertThat(executor.executedHere).isTrue()
        }
    }
}
