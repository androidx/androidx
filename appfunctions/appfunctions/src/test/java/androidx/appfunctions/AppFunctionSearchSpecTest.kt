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

import androidx.appfunctions.metadata.AppFunctionName
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

    @Test
    fun constructor_packageNamesEmpty_throws() {
        val exception =
            assertThrows(IllegalArgumentException::class.java) {
                AppFunctionSearchSpec(packageNames = emptySet())
            }
        assertThat(exception)
            .hasMessageThat()
            .isEqualTo("Cannot filter by empty set of package names.")
    }

    @Test
    fun constructor_packageNamesNotEmpty_doesNotThrow() {
        val spec = AppFunctionSearchSpec(packageNames = setOf("com.example.app"))
        assertThat(spec.packageNames).containsExactly("com.example.app")
    }

    @Test
    fun constructor_functionNamesEmpty_throws() {
        val exception =
            assertThrows(IllegalArgumentException::class.java) {
                AppFunctionSearchSpec(functionNames = emptySet())
            }
        assertThat(exception)
            .hasMessageThat()
            .isEqualTo("Cannot filter by empty set of function names.")
    }

    @Test
    fun constructor_functionNamesNotEmpty_doesNotThrow() {
        val spec =
            AppFunctionSearchSpec(
                functionNames = setOf(AppFunctionName("com.example.app", "functionName"))
            )
        assertThat(spec.functionNames)
            .containsExactly(AppFunctionName("com.example.app", "functionName"))
    }
}
