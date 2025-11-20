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
import androidx.appfunctions.internal.AggregatedAppFunctionInventory
import androidx.appfunctions.internal.AppFunctionReader
import androidx.appfunctions.internal.findImpl
import androidx.appfunctions.metadata.AppFunctionComponentsMetadata
import androidx.appfunctions.metadata.AppFunctionMetadata
import androidx.appfunctions.metadata.AppFunctionPackageMetadata
import androidx.appfunctions.metadata.CompileTimeAppFunctionMetadata
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update

@RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
internal class FakeAppFunctionReader(context: Context) : AppFunctionReader {

    private val packageToFunctionMetadataMapState:
        MutableStateFlow<Map<String, Map<String, AppFunctionStaticAndRuntimeMetadata>>>

    private val packageToComponentsMetadataMapState:
        MutableStateFlow<Map<String, AppFunctionComponentsMetadata>>

    init {
        val packageToFunctionMetadataMap:
            MutableMap<String, MutableMap<String, AppFunctionStaticAndRuntimeMetadata>> =
            mutableMapOf()
        val packageToComponentsMetadataMap: MutableMap<String, AppFunctionComponentsMetadata> =
            mutableMapOf()

        val aggregatedAppFunctionInventory: AggregatedAppFunctionInventory? =
            try {
                AggregatedAppFunctionInventory::class.java.findImpl(prefix = "$", suffix = "_Impl")
            } catch (e: Exception) {
                Log.d("AppFunctionsTesting", "No aggregated inventory found.", e)
                null
            }

        val aggregatedFunctionIdToMetadataMap =
            aggregatedAppFunctionInventory?.functionIdToMetadataMap ?: mutableMapOf()
        packageToFunctionMetadataMap.putIfAbsent(
            context.packageName,
            aggregatedFunctionIdToMetadataMap
                .mapValues { (_, staticMetadata) ->
                    AppFunctionStaticAndRuntimeMetadata(
                        staticMetadata = staticMetadata,
                        AppFunctionRuntimeMetadata(
                            AppFunctionManagerCompat.APP_FUNCTION_STATE_DEFAULT
                        ),
                    )
                }
                .toMutableMap(),
        )

        packageToComponentsMetadataMap.putIfAbsent(
            context.packageName,
            aggregatedAppFunctionInventory?.componentsMetadata ?: AppFunctionComponentsMetadata(),
        )

        packageToFunctionMetadataMapState = MutableStateFlow(packageToFunctionMetadataMap)
        packageToComponentsMetadataMapState = MutableStateFlow(packageToComponentsMetadataMap)
    }

    override fun searchAppFunctions(
        searchFunctionSpec: AppFunctionSearchSpec
    ): Flow<List<AppFunctionPackageMetadata>> =
        packageToFunctionMetadataMapState.combine(packageToComponentsMetadataMapState) {
            packageToFunctionMetadataMap,
            packageToComponentsMetadataMap ->
            packageToFunctionMetadataMap
                .filterKeys { packageName ->
                    searchFunctionSpec.packageNames == null ||
                        packageName in checkNotNull(searchFunctionSpec.packageNames)
                }
                .mapNotNull { (packageName, metadataMap) ->
                    val appFunctions =
                        metadataMap.values
                            .filter { metadata -> matchesSchemaSpec(metadata, searchFunctionSpec) }
                            .map { metadata ->
                                AppFunctionMetadata(
                                    id = metadata.staticMetadata.id,
                                    packageName = packageName,
                                    isEnabled = metadata.computeEffectivelyEnabled(),
                                    schema = metadata.staticMetadata.schema,
                                    parameters = metadata.staticMetadata.parameters,
                                    response = metadata.staticMetadata.response,
                                    components =
                                        checkNotNull(packageToComponentsMetadataMap[packageName]),
                                )
                            }
                    if (appFunctions.isNotEmpty()) {
                        AppFunctionPackageMetadata(packageName, appFunctions)
                    } else {
                        null
                    }
                }
        }

    private fun matchesSchemaSpec(
        metadata: AppFunctionStaticAndRuntimeMetadata,
        spec: AppFunctionSearchSpec,
    ): Boolean =
        (spec.schemaName == null || spec.schemaName == metadata.staticMetadata.schema?.name) &&
            (spec.schemaCategory == null ||
                spec.schemaCategory == metadata.staticMetadata.schema?.category) &&
            // minSchemaVersion == 0 is treated as unset and basically will evaluate
            // true for all objects.
            (metadata.staticMetadata.schema?.version ?: 0) >= spec.minSchemaVersion

    override suspend fun getAppFunctionMetadata(
        functionId: String,
        packageName: String,
    ): AppFunctionMetadata? =
        packageToFunctionMetadataMapState.value[packageName]
            ?.get(functionId)
            ?.toAppFunctionMetadata(
                packageName,
                packageToComponentsMetadataMapState.value[packageName]
                    ?: AppFunctionComponentsMetadata(),
            )

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
    fun toAppFunctionMetadata(
        packageName: String,
        componentsMetadata: AppFunctionComponentsMetadata,
    ) =
        AppFunctionMetadata(
            id = staticMetadata.id,
            packageName = packageName,
            isEnabled = computeEffectivelyEnabled(),
            schema = staticMetadata.schema,
            parameters = staticMetadata.parameters,
            response = staticMetadata.response,
            components = componentsMetadata,
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
