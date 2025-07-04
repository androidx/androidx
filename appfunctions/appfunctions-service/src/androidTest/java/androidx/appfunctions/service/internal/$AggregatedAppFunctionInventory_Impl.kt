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

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.appfunctions.core.AppFunctionMetadataTestHelper
import androidx.appfunctions.metadata.AppFunctionParameterMetadata
import androidx.appfunctions.metadata.AppFunctionPrimitiveTypeMetadata
import androidx.appfunctions.metadata.AppFunctionPrimitiveTypeMetadata.Companion.TYPE_LONG
import androidx.appfunctions.metadata.AppFunctionPrimitiveTypeMetadata.Companion.TYPE_STRING
import androidx.appfunctions.metadata.AppFunctionPrimitiveTypeMetadata.Companion.TYPE_UNIT
import androidx.appfunctions.metadata.AppFunctionResponseMetadata
import androidx.appfunctions.metadata.CompileTimeAppFunctionMetadata

/** Test implementation for [androidx.appfunctions.AppFunctionManagerCompatTest] */
@RequiresApi(Build.VERSION_CODES.S)
class `$AggregatedAppFunctionInventory_Impl` : AggregatedAppFunctionInventory() {
    override val inventories: List<AppFunctionInventory>
        get() = listOf(InternalAppFunctionInventory())

    private class InternalAppFunctionInventory : AppFunctionInventory {
        override val functionIdToMetadataMap: Map<String, CompileTimeAppFunctionMetadata>
            get() =
                mapOf(
                    AppFunctionMetadataTestHelper.FunctionIds.NO_SCHEMA_EXECUTION_SUCCEED to
                        CompileTimeAppFunctionMetadata(
                            id =
                                AppFunctionMetadataTestHelper.FunctionIds
                                    .NO_SCHEMA_EXECUTION_SUCCEED,
                            isEnabledByDefault = true,
                            schema = null,
                            parameters = listOf(),
                            response =
                                AppFunctionResponseMetadata(
                                    valueType =
                                        AppFunctionPrimitiveTypeMetadata(
                                            type = TYPE_STRING,
                                            isNullable = false,
                                        )
                                ),
                        ),
                    AppFunctionMetadataTestHelper.FunctionIds.NO_SCHEMA_EXECUTION_FAIL to
                        CompileTimeAppFunctionMetadata(
                            id = AppFunctionMetadataTestHelper.FunctionIds.NO_SCHEMA_EXECUTION_FAIL,
                            isEnabledByDefault = true,
                            schema = null,
                            parameters =
                                listOf(
                                    AppFunctionParameterMetadata(
                                        name = "arg1",
                                        isRequired = true,
                                        dataType =
                                            AppFunctionPrimitiveTypeMetadata(
                                                type = TYPE_LONG,
                                                isNullable = false,
                                            ),
                                    )
                                ),
                            response =
                                AppFunctionResponseMetadata(
                                    valueType =
                                        AppFunctionPrimitiveTypeMetadata(
                                            type = TYPE_UNIT,
                                            isNullable = false,
                                        )
                                ),
                        ),
                )
    }
}
