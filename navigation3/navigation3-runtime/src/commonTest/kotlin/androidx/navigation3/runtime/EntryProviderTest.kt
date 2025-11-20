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

package androidx.navigation3.runtime

import androidx.kruth.assertThat
import kotlin.test.Test
import kotlin.test.fail

class EntryProviderTest {

    @Test
    fun entryProvider_withUniqueInitializers_returnsEntries() {
        val provider = entryProvider {
            entry("first", "first") {}
            entry("second", "second") {}
        }

        val entry1 = provider.invoke("first")
        val entry2 = provider.invoke("second")

        assertThat(entry1.contentKey).isEqualTo("first")
        assertThat(entry2.contentKey).isEqualTo("second")
    }

    @Test
    fun entryProvider_withDuplicatedInitializers_throwsException() {
        try {
            entryProvider {
                entry("first") {}
                entry("first") {}
            }
            fail("Expected `IllegalArgumentException` but no exception has been throw.")
        } catch (e: IllegalArgumentException) {
            assertThat(e)
                .hasMessageThat()
                .isEqualTo("An `entry` with the key `key` has already been added: first.")
        }
    }

    @Test
    fun entryProvider_noInitializers_getsInvalidEntry() {
        val provider = entryProvider<Any> {}
        try {
            provider.invoke("something")
            fail("Expected `IllegalStateException` but no exception has been throw.")
        } catch (e: IllegalStateException) {
            assertThat(e).hasMessageThat().isEqualTo("Unknown screen something")
        }
    }
}
