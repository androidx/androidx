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

package androidx.credentials.providerevents.service

import android.content.Intent
import android.os.Binder
import android.os.IBinder
import androidx.credentials.providerevents.DeviceSetupProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
class DeviceSetupServiceTest {

    @Test
    fun onBind_nullIntent_returnsNull() {
        val service = object : DeviceSetupService() {}
        val binder = service.onBind(null)
        assertThat(binder).isNull()
    }

    @Test
    fun onBind_noProviderFound_returnsNull() {
        val service = object : DeviceSetupService() {}
        val intent = Intent()
        // No class name added to the intent
        val binder = service.onBind(intent)
        assertThat(binder).isNull()
    }

    @Test
    fun onBind_validProvider_returnsBinder() {
        val service = object : DeviceSetupService() {}
        val intent =
            Intent().apply {
                putExtra(
                    DeviceSetupProvider.DEVICE_SETUP_PROVIDER_KEY,
                    DummyDeviceSetupProvider::class.java.name,
                )
            }
        val binder = service.onBind(intent)
        assertThat(binder).isNotNull()
    }

    // Dummy implementation of DummyDeviceSetupProvider for testing
    class DummyDeviceSetupProvider : DeviceSetupProvider {
        override fun getStubImplementation(service: DeviceSetupService): IBinder? {
            return Binder()
        }
    }
}
