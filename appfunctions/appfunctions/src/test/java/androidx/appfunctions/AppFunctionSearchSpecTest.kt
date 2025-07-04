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

package androidx.appfunctions

import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Test

class AppFunctionSearchSpecTest {

    @Test
    fun constructor_minSchemaVersionNegative_throws() {
        val exception =
            assertThrows(IllegalArgumentException::class.java) {
                AppFunctionSearchSpec(minSchemaVersion = -1)
            }
        assertThat(exception)
            .hasMessageThat()
            .isEqualTo("The minimum schema version must be a non-negative integer.")
    }

    @Test
    fun constructor_minSchemaVersionPositive_doesNotThrow() {
        val spec = AppFunctionSearchSpec(minSchemaVersion = 1)
        assertThat(spec.minSchemaVersion).isEqualTo(1)
    }
}
