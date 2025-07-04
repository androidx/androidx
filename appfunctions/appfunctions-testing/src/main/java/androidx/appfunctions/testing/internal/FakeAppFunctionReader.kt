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

package androidx.appfunctions.testing.internal

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.appfunctions.AppFunctionManagerCompat
import androidx.appfunctions.AppFunctionSearchSpec
import androidx.appfunctions.internal.AppFunctionReader
import androidx.appfunctions.internal.findImpl
import androidx.appfunctions.metadata.AppFunctionMetadata
import androidx.appfunctions.metadata.AppFunctionPackageMetadata
import androidx.appfunctions.metadata.CompileTimeAppFunctionMetadata
import androidx.appfunctions.service.internal.AggregatedAppFunctionInventory
import androidx.appfunctions.service.internal.AppFunctionInventory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

@RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
internal class FakeAppFunctionReader(context: Context) : AppFunctionReader {

    private val packageToFunctionMetadataMapState:
        MutableStateFlow<Map<String, Map<String, AppFunctionStaticAndRuntimeMetadata>>>

    init {
        val packageToFunctionMetadataMap:
            MutableMap<String, MutableMap<String, AppFunctionStaticAndRuntimeMetadata>> =
            mutableMapOf()
        val compiledInventories: List<AppFunctionInventory>? =
            try {
                AggregatedAppFunctionInventory::class
                    .java
                    .findImpl(prefix = "$", suffix = "_Impl")
                    .inventories
            } catch (e: Exception) {
                Log.d("AppFunctionsTesting", "No aggregated inventory found.", e)
                null
            }
        for (inventory in compiledInventories ?: listOf()) {
            packageToFunctionMetadataMap.putIfAbsent(context.packageName, mutableMapOf())
            for ((id, staticMetadata) in inventory.functionIdToMetadataMap) {
                packageToFunctionMetadataMap
                    .getValue(context.packageName)
                    .put(
                        id,
                        AppFunctionStaticAndRuntimeMetadata(
                            staticMetadata,
                            AppFunctionRuntimeMetadata(
                                AppFunctionManagerCompat.APP_FUNCTION_STATE_DEFAULT
                            ),
                        ),
                    )
            }
        }

        packageToFunctionMetadataMapState = MutableStateFlow(packageToFunctionMetadataMap)
    }

    override fun searchAppFunctions(
        searchFunctionSpec: AppFunctionSearchSpec
    ): Flow<List<AppFunctionPackageMetadata>> =
        packageToFunctionMetadataMapState.map { packageToFunctionMetadataMap ->
            packageToFunctionMetadataMap
                .filterKeys {
                    searchFunctionSpec.packageNames == null ||
                        it in checkNotNull(searchFunctionSpec.packageNames)
                }
                .flatMap { (packageName, metadataMap) ->
                    metadataMap.values
                        .filter {
                            if (
                                searchFunctionSpec.schemaName != null &&
                                    searchFunctionSpec.schemaName != it.staticMetadata.schema?.name
                            ) {
                                return@filter false
                            }

                            if (
                                searchFunctionSpec.schemaCategory != null &&
                                    searchFunctionSpec.schemaCategory !=
                                        it.staticMetadata.schema?.category
                            ) {
                                return@filter false
                            }

                            // minSchemaVersion == 0 is treated as unset and basically will evaluate
                            // true for all objects.
                            (it.staticMetadata.schema?.version ?: 0) >=
                                searchFunctionSpec.minSchemaVersion
                        }
                        .map { metadata ->
                            AppFunctionMetadata(
                                id = metadata.staticMetadata.id,
                                packageName = packageName,
                                isEnabled = metadata.computeEffectivelyEnabled(),
                                schema = metadata.staticMetadata.schema,
                                parameters = metadata.staticMetadata.parameters,
                                response = metadata.staticMetadata.response,
                                components = metadata.staticMetadata.components,
                            )
                        }
                        .groupBy { it.packageName }
                        .map { (packageName, appFunctions) ->
                            AppFunctionPackageMetadata(packageName, appFunctions)
                        }
                }
        }

    override suspend fun getAppFunctionMetadata(
        functionId: String,
        packageName: String,
    ): AppFunctionMetadata? =
        packageToFunctionMetadataMapState.value[packageName]
            ?.get(functionId)
            ?.toAppFunctionMetadata(packageName)

    fun getAppFunctionStaticAndRuntimeMetadata(
        packageName: String,
        functionId: String,
    ): AppFunctionStaticAndRuntimeMetadata? =
        packageToFunctionMetadataMapState.value[packageName]?.get(functionId)

    fun setAppFunctionStaticAndRuntimeMetadata(
        packageName: String,
        appFunctionStaticAndRuntimeMetadata: AppFunctionStaticAndRuntimeMetadata,
    ) {
        packageToFunctionMetadataMapState.update { currentMap ->
            val functionId = appFunctionStaticAndRuntimeMetadata.staticMetadata.id
            val existingPackageMap = currentMap[packageName] ?: emptyMap()
            currentMap +
                (packageName to
                    (existingPackageMap + (functionId to appFunctionStaticAndRuntimeMetadata)))
        }
    }
}

internal data class AppFunctionRuntimeMetadata(
    @AppFunctionManagerCompat.EnabledState val enabled: Int
)

internal data class AppFunctionStaticAndRuntimeMetadata(
    val staticMetadata: CompileTimeAppFunctionMetadata,
    val runtimeMetadata: AppFunctionRuntimeMetadata,
) {
    fun toAppFunctionMetadata(packageName: String) =
        AppFunctionMetadata(
            id = staticMetadata.id,
            packageName = packageName,
            isEnabled = computeEffectivelyEnabled(),
            schema = staticMetadata.schema,
            parameters = staticMetadata.parameters,
            response = staticMetadata.response,
            components = staticMetadata.components,
        )

    fun computeEffectivelyEnabled(): Boolean =
        when (runtimeMetadata.enabled) {
            AppFunctionManagerCompat.Companion.APP_FUNCTION_STATE_ENABLED -> true
            AppFunctionManagerCompat.Companion.APP_FUNCTION_STATE_DISABLED -> false
            AppFunctionManagerCompat.Companion.APP_FUNCTION_STATE_DEFAULT ->
                staticMetadata.isEnabledByDefault

            else ->
                throw IllegalStateException(
                    "Unknown AppFunction state: ${runtimeMetadata.enabled}."
                )
        }
}
