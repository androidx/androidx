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

package androidx.appfunctions.testing.internal

import android.os.Build
import androidx.appfunctions.AppFunctionService
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

// TODO(b/570443019): Add tests for concurrent access
@RunWith(RobolectricTestRunner::class)
@Config(minSdk = Build.VERSION_CODES.BAKLAVA)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.BAKLAVA)
class FakeAppFunctionInventoryTest {
    private val targetContext = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun getServiceClassForFunction_returnsAppFunctionServiceClass_whenValidFunctionIdentifier() {
        val inventory = FakeAppFunctionInventory(targetContext)
        inventory.reloadInventories()

        val serviceClass =
            inventory.getServiceClassForFunction(
                "androidx.appfunctions.testing.BaseTestAppFunctionService#createNote"
            )

        val expectedServiceClass =
            Class.forName("androidx.appfunctions.testing.TestAppFunctionService")
        assertThat(AppFunctionService::class.java.isAssignableFrom(serviceClass!!)).isTrue()
        assertThat(serviceClass).isEqualTo(expectedServiceClass)
    }

    @Test
    fun getServiceClassForFunction_returnsNull_whenFunctionIdentifierNotFound() {
        val inventory = FakeAppFunctionInventory(targetContext)
        inventory.reloadInventories()

        val serviceClass = inventory.getServiceClassForFunction("invalid#function")

        assertThat(serviceClass).isNull()
    }
}
