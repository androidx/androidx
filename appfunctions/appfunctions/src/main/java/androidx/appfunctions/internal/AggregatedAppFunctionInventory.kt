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
import androidx.appfunctions.metadata.AppFunctionComponentsMetadata
import androidx.appfunctions.metadata.CompileTimeAppFunctionMetadata

/**
 * An [AppFunctionInventory] that aggregates the function metadata from multiple
 * [AppFunctionInventory] instances.
 *
 * AppFunction compiler will automatically generate the implementation of this class to access all
 * generated [androidx.appfunctions.metadata.CompileTimeAppFunctionMetadata] exposed by the
 * application.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public abstract class AggregatedAppFunctionInventory : AppFunctionInventory {

    /** The list of [AppFunctionInventory] instances that contribute to this aggregate. */
    public abstract val inventories: List<AppFunctionInventory>

    final override val functionIdToMetadataMap:
        Map<String, CompileTimeAppFunctionMetadata> by lazy {
        // Empty collection can't be reduced
        if (inventories.isEmpty()) return@lazy emptyMap()
        inventories.map(AppFunctionInventory::functionIdToMetadataMap).reduce { acc, map ->
            acc + map
        }
    }

    final override val componentsMetadata: AppFunctionComponentsMetadata by lazy {
        // Empty collection can't be reduced
        if (inventories.isEmpty()) return@lazy AppFunctionComponentsMetadata()
        val dataTypes =
            inventories.map { it.componentsMetadata.dataTypes }.reduce { acc, map -> acc + map }
        AppFunctionComponentsMetadata(dataTypes)
    }
}
