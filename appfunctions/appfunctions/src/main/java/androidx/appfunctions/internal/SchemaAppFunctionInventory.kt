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

import androidx.annotation.RestrictTo
import androidx.appfunctions.metadata.AppFunctionSchemaMetadata
import androidx.appfunctions.metadata.CompileTimeAppFunctionMetadata

/**
 * An inventory that provides the function metadata to look up based on
 * [androidx.appfunctions.metadata.AppFunctionSchemaMetadata].
 *
 * When enabled, the AppFunction compiler will automatically generate the implementation of this
 * class to access statically generated metadata from the interfaces annotated with
 * [androidx.appfunctions.AppFunctionSchemaDefinition].
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public abstract class SchemaAppFunctionInventory : AppFunctionInventory {
    /**
     * The map of [androidx.appfunctions.metadata.AppFunctionSchemaMetadata] to
     * [CompileTimeAppFunctionMetadata].
     */
    public val schemaFunctionsMap:
        Map<AppFunctionSchemaMetadata, CompileTimeAppFunctionMetadata> by lazy {
        buildMap {
            for (appFunction in functionIdToMetadataMap.values) {
                val schemaMetadata = appFunction.schema ?: continue
                this[schemaMetadata] = appFunction
            }
        }
    }
}
