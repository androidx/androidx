/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.privacysandbox.sdkruntime.core.controller

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import java.lang.reflect.Proxy
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
class LegacySdkSandboxControllerTest {

    @Suppress("DEPRECATION") // Test for deprecated component
    @Test
    fun injectLocalImpl_setBackendInSdkSandboxControllerBackendHolder() {
        // Using proxy instead of mock to support tests on devices before API28.
        // Other tests in project uses mockito inline to mock final classes and since require API28+
        val controllerImplClass = SdkSandboxControllerCompat.SandboxControllerImpl::class.java
        val noOpProxy =
            Proxy.newProxyInstance(controllerImplClass.classLoader, arrayOf(controllerImplClass)) {
                proxy,
                method,
                args ->
                throw UnsupportedOperationException(
                    "Unexpected method call (NoOp) object:$proxy, method: $method, args: $args"
                )
            } as SdkSandboxControllerCompat.SandboxControllerImpl

        assertThat(SdkSandboxControllerBackendHolder.LOCAL_BACKEND).isNull()
        SdkSandboxControllerCompat.injectLocalImpl(noOpProxy)
        assertThat(SdkSandboxControllerBackendHolder.LOCAL_BACKEND).isNotNull()

        SdkSandboxControllerBackendHolder.resetLocalBackend()
        assertThat(SdkSandboxControllerBackendHolder.LOCAL_BACKEND).isNull()
    }
}
