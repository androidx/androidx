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

package androidx.compose.remote.core

import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import java.lang.reflect.Field
import java.lang.reflect.Modifier
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class OperationsTest {

    private fun publicStaticFinalIntFields(): List<Field> =
        Operations::class.java.declaredFields.filter { field ->
            val mods = field.modifiers
            Modifier.isPublic(mods) &&
                Modifier.isStatic(mods) &&
                Modifier.isFinal(mods) &&
                field.type == Int::class.javaPrimitiveType
        }

    @Test
    fun constants_haveUniqueNames() {
        val fields = publicStaticFinalIntFields()
        assertThat(fields).isNotEmpty()

        val duplicateNames =
            fields
                .groupBy { it.name.lowercase() }
                .filterValues { it.size > 1 }
                .mapValues { entry ->
                    entry.value.map { it.name }
                }

        assertWithMessage("Duplicate constant names in Operations: $duplicateNames")
            .that(duplicateNames)
            .isEmpty()
    }

    @Test
    fun constants_haveUniqueValues() {
        val fields = publicStaticFinalIntFields()
        assertThat(fields).isNotEmpty()

        val duplicateValues =
            fields
                .groupBy { it.getInt(null) }
                .filterValues { it.size > 1 }
                .mapValues { entry -> entry.value.map { it.name } }

        assertWithMessage("Duplicate opcode values in Operations: $duplicateValues")
            .that(duplicateValues)
            .isEmpty()
    }
}
