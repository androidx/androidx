/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.credentials.registry.provider.internal

import android.content.Intent
import android.os.Binder
import android.os.IBinder
import androidx.credentials.registry.provider.UiDelegationFulfillmentProvider
import androidx.credentials.registry.provider.UiDelegationFulfillmentService
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
class UiDelegationFulfillmentProviderFactoryTest {

    @Before
    fun setUp() {
        NonProviderClass.instantiated = false
    }

    @Test
    fun getBestAvailableProvider_validClassName_returnsProvider() {
        val factory = UiDelegationFulfillmentProviderFactory()
        val intent =
            Intent().apply {
                putExtra(
                    UiDelegationFulfillmentProvider.EXTRA_STUB_IMPL_CLASS_NAME,
                    DummyUiDelegationFulfillmentProvider::class.java.name,
                )
            }

        val provider = factory.getBestAvailableProvider(intent)

        assertThat(provider).isInstanceOf(DummyUiDelegationFulfillmentProvider::class.java)
    }

    @Test
    fun getBestAvailableProvider_nonProviderClass_doesNotInstantiateAndReturnsNull() {
        val factory = UiDelegationFulfillmentProviderFactory()
        val intent =
            Intent().apply {
                putExtra(
                    UiDelegationFulfillmentProvider.EXTRA_STUB_IMPL_CLASS_NAME,
                    NonProviderClass::class.java.name,
                )
            }

        val provider = factory.getBestAvailableProvider(intent)

        assertThat(provider).isNull()
        assertThat(NonProviderClass.instantiated).isFalse()
    }

    @Test
    fun getBestAvailableProvider_invalidClassName_returnsNull() {
        val factory = UiDelegationFulfillmentProviderFactory()
        val intent =
            Intent().apply {
                putExtra(
                    UiDelegationFulfillmentProvider.EXTRA_STUB_IMPL_CLASS_NAME,
                    "com.example.NonExistentProviderClass",
                )
            }

        val provider = factory.getBestAvailableProvider(intent)

        assertThat(provider).isNull()
    }

    @Test
    fun getBestAvailableProvider_noClassName_returnsNull() {
        val factory = UiDelegationFulfillmentProviderFactory()
        val intent = Intent()

        val provider = factory.getBestAvailableProvider(intent)

        assertThat(provider).isNull()
    }
}

class DummyUiDelegationFulfillmentProvider : UiDelegationFulfillmentProvider {
    override fun getStubImplementation(service: UiDelegationFulfillmentService): IBinder? {
        return Binder()
    }
}

class NonProviderClass {
    init {
        instantiated = true
    }

    companion object {
        var instantiated: Boolean = false
    }
}
