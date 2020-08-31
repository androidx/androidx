/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.camera.extensions

import androidx.test.filters.SmallTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.testutils.assertThrows
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Assume.assumeTrue

import org.junit.Test
import java.util.concurrent.TimeUnit

@SmallTest
class ExtensionsManagerTest {

    private val context = InstrumentationRegistry.getInstrumentation().context

    @After
    fun teardown() {
        ExtensionsManager.deinit().get()
    }

    @Test
    fun canGetExtensionAfterInit() {
        val availabilityFuture = ExtensionsManager.init(context)

        val availability = availabilityFuture.get(5000, TimeUnit.MILLISECONDS)

        assumeTrue(
            availability == ExtensionsManager.ExtensionsAvailability
                .LIBRARY_AVAILABLE
        )

        assertThat(ExtensionsManager.getExtensions(context)).isNotNull()
    }

    @Test
    fun exceptionThrownIfNotInit() {
        assertThrows<IllegalStateException> {
            ExtensionsManager.getExtensions(context)
        }
    }
}
