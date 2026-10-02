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
package androidx.appfunctions.testing.internal

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.appfunctions.AppFunctionService
import androidx.appfunctions.internal.AppFunctionInventory
import androidx.appfunctions.metadata.AppFunctionComponentsMetadata
import androidx.appfunctions.metadata.CompileTimeAppFunctionMetadata
import androidx.appfunctions.testing.internal.AppFunctionServiceUtils.getEnabledAppFunctionServiceClasses
import androidx.appfunctions.testing.internal.AppFunctionServiceUtils.resolveInventory

/**
 * Aggregates inventories across enabled [AppFunctionService] components for testing.
 *
 * Scans the application context for active [AppFunctionService]s, fetches their corresponding
 * inventories and loads their AppFunctions and Components metadata.
 */
@RequiresApi(Build.VERSION_CODES.BAKLAVA)
internal class FakeAppFunctionInventory(private val context: Context) : AppFunctionInventory {

    // TODO(b/570443019): handle synchronization of these 3 properties.
    @Volatile
    private var functionIdToServiceClass: Map<String, Class<AppFunctionService>> = emptyMap()

    @Volatile
    override var functionIdToMetadataMap: Map<String, CompileTimeAppFunctionMetadata> = emptyMap()
        private set

    @Volatile
    override var componentsMetadata: AppFunctionComponentsMetadata = AppFunctionComponentsMetadata()
        private set

    /**
     * Rescans enabled [AppFunctionService]s and updates the aggregated metadata and service
     * mapping.
     */
    // TODO(b/567966316): Cache service states to avoid re-resolving all inventories on every query.
    fun reloadInventories() {
        val inventoriesToServiceClass = getInventoriesForEnabledServices()
        val newFunctionIdToServiceClass = mutableMapOf<String, Class<AppFunctionService>>()

        for ((inventory, serviceClass) in inventoriesToServiceClass) {
            for (functionId in inventory.functionIdToMetadataMap.keys) {
                newFunctionIdToServiceClass[functionId] = serviceClass
            }
        }

        val inventories = inventoriesToServiceClass.keys

        val newFunctionIdToMetadataMap =
            if (inventories.isEmpty()) {
                emptyMap()
            } else {
                inventories.map(AppFunctionInventory::functionIdToMetadataMap).reduce { acc, map ->
                    acc + map
                }
            }

        val newComponentsMetadata =
            if (inventories.isEmpty()) {
                AppFunctionComponentsMetadata()
            } else {
                val dataTypes =
                    inventories
                        .map { it.componentsMetadata.dataTypes }
                        .reduce { acc, map -> acc + map }
                AppFunctionComponentsMetadata(dataTypes)
            }

        functionIdToServiceClass = newFunctionIdToServiceClass
        functionIdToMetadataMap = newFunctionIdToMetadataMap
        componentsMetadata = newComponentsMetadata
    }

    private fun getInventoriesForEnabledServices():
        Map<AppFunctionInventory, Class<AppFunctionService>> {
        val serviceClasses = getEnabledAppFunctionServiceClasses(context)
        return serviceClasses
            .mapNotNull { serviceClass ->
                val inventory = resolveInventory(serviceClass) ?: return@mapNotNull null
                check(AppFunctionService::class.java.isAssignableFrom(serviceClass)) {
                    "Service class ${serviceClass.name} is not a subclass of AppFunctionService"
                }
                @Suppress("UNCHECKED_CAST")
                val appFunctionServiceClass = serviceClass as Class<AppFunctionService>
                inventory to appFunctionServiceClass
            }
            .toMap()
    }

    /**
     * Returns the [AppFunctionService] class containing the function for [functionIdentifier].
     *
     * @param functionIdentifier the identifier of the function to look up
     * @return the corresponding [AppFunctionService] class, or `null` if not found
     */
    fun getServiceClassForFunction(functionIdentifier: String): Class<AppFunctionService>? {
        return functionIdToServiceClass[functionIdentifier]
    }
}
