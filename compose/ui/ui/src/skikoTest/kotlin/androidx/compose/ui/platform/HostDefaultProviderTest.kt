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

package androidx.compose.ui.platform

import androidx.compose.runtime.HostDefaultKey
import androidx.compose.runtime.compositionLocalWithHostDefaultOf
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runSkikoComposeUiTest
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import kotlin.test.Test
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull

@OptIn(ExperimentalTestApi::class)
class HostDefaultProviderTest {

    @Test
    fun testHostDefaultProviderGetLocalViewModelStoreOwner() = runSkikoComposeUiTest {
        var owner: ViewModelStoreOwner? = null
        setContent {
            owner = LocalViewModelStoreOwner.current
        }

        assertNotNull(owner)
        assertIs<ViewModelStoreOwner>(owner)
    }

    interface TestRegistry
    val TestRegistryKey = object : HostDefaultKey<TestRegistry?> {}

    @Test
    fun testHostDefaultProviderNull() = runSkikoComposeUiTest {
        val LocalTestRegistry = compositionLocalWithHostDefaultOf(TestRegistryKey)

        var registry: TestRegistry? = null
        setContent {
            registry = LocalTestRegistry.current
        }

        assertNull(registry)
    }
}