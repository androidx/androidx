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

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.appfunctions.metadata.AppFunctionMetadata

@RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
internal object AppFunctionMetadataUtils {

    /**
     * Finds [AppFunctionMetadata] for the given [functionIdentifier], return nulls if metadata is
     * not found.
     *
     * This function first tries to find the metadata from [Dependencies.appFunctionInventory], if
     * no inventory is present it queries AppSearch to find the metadata using
     * [AppSearchAppFunctionReader].
     */
    suspend fun getAppFunctionMetadata(
        context: Context,
        functionIdentifier: String,
    ): AppFunctionMetadata? {
        val inventory = Dependencies.appFunctionInventory
        if (inventory != null) {
            val compileTimeAppFunctionMetadata =
                inventory.functionIdToMetadataMap[functionIdentifier] ?: return null
            return AppFunctionMetadata(
                id = compileTimeAppFunctionMetadata.id,
                packageName = context.packageName,
                isEnabled = compileTimeAppFunctionMetadata.isEnabledByDefault,
                schema = compileTimeAppFunctionMetadata.schema,
                parameters = compileTimeAppFunctionMetadata.parameters,
                response = compileTimeAppFunctionMetadata.response,
                components = inventory.componentsMetadata,
                description = compileTimeAppFunctionMetadata.description,
            )
        }

        return AppSearchAppFunctionReader(
                context,
                schemaAppFunctionInventory = Dependencies.schemaAppFunctionInventory,
            )
            .getAppFunctionMetadata(functionIdentifier, context.packageName)
    }
}
