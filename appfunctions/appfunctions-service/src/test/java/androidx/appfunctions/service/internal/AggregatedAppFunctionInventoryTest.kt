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

package androidx.appfunctions.service.internal

import androidx.appfunctions.metadata.AppFunctionComponentsMetadata
import androidx.appfunctions.metadata.AppFunctionPrimitiveTypeMetadata
import androidx.appfunctions.metadata.AppFunctionResponseMetadata
import androidx.appfunctions.metadata.CompileTimeAppFunctionMetadata
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class AggregatedAppFunctionInventoryTest {

    @Test
    fun testEmpty() {
        val aggregatedInventory =
            object : AggregatedAppFunctionInventory() {
                override val inventories: List<AppFunctionInventory> = emptyList()
            }

        assertThat(aggregatedInventory.functionIdToMetadataMap).hasSize(0)
    }

    @Test
    fun testUniqueInventories() {
        val aggregatedInventory =
            object : AggregatedAppFunctionInventory() {
                override val inventories: List<AppFunctionInventory> =
                    listOf(Inventory1(), Inventory2())
            }

        assertThat(aggregatedInventory.functionIdToMetadataMap).hasSize(2)
        assertThat(aggregatedInventory.functionIdToMetadataMap.keys)
            .containsExactly(
                "androix.appfunctions.internal#test1",
                "androix.appfunctions.internal#test2",
            )
    }

    @Test
    fun testDuplicatedInventories() {
        val aggregatedInventory =
            object : AggregatedAppFunctionInventory() {
                override val inventories: List<AppFunctionInventory> =
                    listOf(Inventory1(), Inventory1())
            }

        assertThat(aggregatedInventory.functionIdToMetadataMap).hasSize(1)
        assertThat(aggregatedInventory.functionIdToMetadataMap.keys)
            .containsExactly("androix.appfunctions.internal#test1")
    }

    private class Inventory1 : AppFunctionInventory {
        override val functionIdToMetadataMap: Map<String, CompileTimeAppFunctionMetadata> =
            mapOf(
                "androix.appfunctions.internal#test1" to
                    CompileTimeAppFunctionMetadata(
                        id = "androix.appfunctions.internal#test1",
                        isEnabledByDefault = false,
                        schema = null,
                        parameters = emptyList(),
                        response =
                            AppFunctionResponseMetadata(
                                valueType =
                                    AppFunctionPrimitiveTypeMetadata(
                                        type = AppFunctionPrimitiveTypeMetadata.TYPE_UNIT,
                                        isNullable = false,
                                    )
                            ),
                        components = AppFunctionComponentsMetadata(),
                    )
            )
    }

    private class Inventory2 : AppFunctionInventory {
        override val functionIdToMetadataMap: Map<String, CompileTimeAppFunctionMetadata> =
            mapOf(
                "androix.appfunctions.internal#test2" to
                    CompileTimeAppFunctionMetadata(
                        id = "androix.appfunctions.internal#test2",
                        isEnabledByDefault = false,
                        schema = null,
                        parameters = emptyList(),
                        response =
                            AppFunctionResponseMetadata(
                                valueType =
                                    AppFunctionPrimitiveTypeMetadata(
                                        type = AppFunctionPrimitiveTypeMetadata.TYPE_UNIT,
                                        isNullable = false,
                                    )
                            ),
                        components = AppFunctionComponentsMetadata(),
                    )
            )
    }
}
