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

package androidx.binarycompatibilityvalidator

import com.google.common.truth.Truth.assertThat
import kotlin.test.Test
import org.jetbrains.kotlin.library.abi.AbiClass
import org.jetbrains.kotlin.library.abi.AbiFunction
import org.jetbrains.kotlin.library.abi.ExperimentalLibraryAbiReader

@OptIn(ExperimentalLibraryAbiReader::class)
class FictionalFunctionAbiBuilderTest {

    @Test
    fun createsAnAbiWithExpectedInterfaces() {
        val abi = FictionalFunctionAbiBuilder.build()
        val expectedInterfaces =
            listOf("kotlin/Function") + (1..22).map { num -> "kotlin/Function$num" }
        assertThat(
                abi.topLevelDeclarations.declarations.filterIsInstance<AbiClass>().map {
                    it.qualifiedName.toString()
                }
            )
            .containsExactly(*expectedInterfaces.toTypedArray())
    }

    @Test
    fun interfacesHaveTheCorrectInvokeMethods() {
        val abi = FictionalFunctionAbiBuilder.build()
        (1..22).map { num ->
            val interfaceToCheck =
                abi.topLevelDeclarations.declarations.filterIsInstance<AbiClass>().find {
                    it.qualifiedName.toString() == "kotlin/Function$num"
                }
            assertThat(interfaceToCheck).isNotNull()
            val invoke = interfaceToCheck!!.declarations.filterIsInstance<AbiFunction>().single()
            assertThat(invoke.valueParameters).hasSize(num)
        }
    }
}
