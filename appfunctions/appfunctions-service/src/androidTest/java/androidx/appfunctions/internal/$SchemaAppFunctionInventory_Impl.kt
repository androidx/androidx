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

package androidx.appfunctions.internal

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.appfunctions.core.AppFunctionMetadataTestHelper
import androidx.appfunctions.metadata.AppFunctionMetadata
import androidx.appfunctions.metadata.CompileTimeAppFunctionMetadata

/** Test implementation of [SchemaAppFunctionInventory]. */
@RequiresApi(Build.VERSION_CODES.S)
class `$SchemaAppFunctionInventory_Impl` : SchemaAppFunctionInventory() {
    override val functionIdToMetadataMap: Map<String, CompileTimeAppFunctionMetadata>
        get() =
            mapOf(
                "CreateNote" to
                    AppFunctionMetadataTestHelper.FunctionMetadata.ADDITIONAL_LEGACY_CREATE_NOTE
                        .toStaticCompileTimeMetadata(),
                "NotePrint" to
                    AppFunctionMetadataTestHelper.FunctionMetadata.NOTES_SCHEMA_PRINT
                        .toStaticCompileTimeMetadata(),
                "MediaPrint1" to
                    AppFunctionMetadataTestHelper.FunctionMetadata.MEDIA_SCHEMA_PRINT
                        .toStaticCompileTimeMetadata(),
                "MediaPrint2" to
                    AppFunctionMetadataTestHelper.FunctionMetadata.MEDIA_SCHEMA2_PRINT
                        .toStaticCompileTimeMetadata(),
            )

    private fun AppFunctionMetadata.toStaticCompileTimeMetadata(): CompileTimeAppFunctionMetadata {
        return CompileTimeAppFunctionMetadata(
            id = this.id,
            isEnabledByDefault = this.isEnabled,
            schema = this.schema,
            parameters = this.parameters,
            response = this.response,
            components = this.components,
        )
    }
}
