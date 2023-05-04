/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.privacysandbox.sdkruntime.client.controller

import android.os.Binder
import android.os.Bundle
import androidx.privacysandbox.sdkruntime.client.activity.LocalSdkActivityHandlerRegistry
import androidx.privacysandbox.sdkruntime.client.loader.LocalSdkProvider
import androidx.privacysandbox.sdkruntime.core.SandboxedSdkCompat
import androidx.privacysandbox.sdkruntime.core.activity.ActivityHolder
import androidx.privacysandbox.sdkruntime.core.activity.SdkSandboxActivityHandlerCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
class LocalControllerTest {

    private lateinit var locallyLoadedSdks: LocallyLoadedSdks
    private lateinit var controller: LocalController

    @Before
    fun setUp() {
        locallyLoadedSdks = LocallyLoadedSdks()
        controller = LocalController(locallyLoadedSdks)
    }

    @Test
    fun getSandboxedSdks_returnsResultsFromLocallyLoadedSdks() {
        val sandboxedSdk = SandboxedSdkCompat(Binder())
        locallyLoadedSdks.put(
            "sdk", LocallyLoadedSdks.Entry(
                sdkProvider = NoOpSdkProvider(),
                sdk = sandboxedSdk
            )
        )

        val result = controller.getSandboxedSdks()
        assertThat(result).containsExactly(sandboxedSdk)
    }

    @Test
    fun registerSdkSandboxActivityHandler_delegateToLocalSdkActivityHandlerRegistry() {
        val handler = object : SdkSandboxActivityHandlerCompat {
            override fun onActivityCreated(activityHolder: ActivityHolder) {
                // do nothing
            }
        }

        val token = controller.registerSdkSandboxActivityHandler(handler)

        val registeredHandler = LocalSdkActivityHandlerRegistry.getHandlerByToken(token)
        assertThat(registeredHandler).isSameInstanceAs(handler)
    }

    @Test
    fun unregisterSdkSandboxActivityHandler_delegateToLocalSdkActivityHandlerRegistry() {
        val handler = object : SdkSandboxActivityHandlerCompat {
            override fun onActivityCreated(activityHolder: ActivityHolder) {
                // do nothing
            }
        }

        val token = controller.registerSdkSandboxActivityHandler(handler)
        controller.unregisterSdkSandboxActivityHandler(handler)

        val registeredHandler = LocalSdkActivityHandlerRegistry.getHandlerByToken(token)
        assertThat(registeredHandler).isNull()
    }

    private class NoOpSdkProvider : LocalSdkProvider(Any()) {
        override fun onLoadSdk(params: Bundle): SandboxedSdkCompat {
            throw IllegalStateException("Unexpected call")
        }

        override fun beforeUnloadSdk() {
            throw IllegalStateException("Unexpected call")
        }
    }
}