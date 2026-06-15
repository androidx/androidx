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

package androidx.appfunctions.internal

import android.app.appfunctions.AppFunctionManager as PlatformAppFunctionManager
import android.content.Context
import android.os.Build
import android.os.OutcomeReceiver
import androidx.annotation.RequiresApi
import androidx.appfunctions.AppFunctionSearchSpec
import androidx.appfunctions.metadata.AppFunctionMetadata
import androidx.appfunctions.metadata.AppFunctionName
import androidx.appfunctions.metadata.AppFunctionPackageMetadata
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.suspendCancellableCoroutine

/** Reads AppFunction metadata from the platform AppFunctionManager. */
@RequiresApi(Build.VERSION_CODES.CINNAMON_BUN)
internal class PlatformAppFunctionReader(
    private val context: Context,
    private val schemaAppFunctionInventory: SchemaAppFunctionInventory?,
) : AppFunctionReader {

    private val appFunctionManager: PlatformAppFunctionManager by lazy {
        context.getSystemService(PlatformAppFunctionManager::class.java)
    }

    private val appSearchReader: AppSearchAppFunctionReader by lazy {
        AppSearchAppFunctionReader(context, schemaAppFunctionInventory)
    }

    /** Searches app function metadata using the platform API. */
    override suspend fun searchAppFunctionsMetadata(
        searchFunctionSpec: AppFunctionSearchSpec
    ): List<AppFunctionMetadata> {
        val platformSpec = searchFunctionSpec.toPlatformSearchSpec()

        return suspendCancellableCoroutine { cont ->
            appFunctionManager.searchAppFunctions(
                platformSpec,
                Runnable::run,
                object :
                    OutcomeReceiver<List<android.app.appfunctions.AppFunctionMetadata>, Exception> {
                    override fun onResult(
                        result: List<android.app.appfunctions.AppFunctionMetadata>
                    ) {
                        val mappedResults =
                            result.mapNotNull {
                                AppFunctionMetadata.fromPlatformAppFunctionMetadata(
                                    it,
                                    schemaAppFunctionInventory,
                                )
                            }
                        cont.resume(mappedResults)
                    }

                    override fun onError(error: Exception) {
                        cont.resumeWithException(error)
                    }
                },
            )
        }
    }

    // TODO(b/508188326): Routing the legacy observeAppFunctions API to AppSearchAppFunctionReader
    // This should be cleaned up once the legacy observeAppFunctions API is removed.
    override fun searchAppFunctionsPackageMetadata(
        searchFunctionSpec: AppFunctionSearchSpec
    ): Flow<List<AppFunctionPackageMetadata>> =
        appSearchReader.searchAppFunctionsPackageMetadata(searchFunctionSpec)

    override suspend fun getAppFunctionMetadata(
        functionId: String,
        packageName: String,
    ): AppFunctionMetadata? {
        return searchAppFunctionsMetadata(
                AppFunctionSearchSpec(
                    packageNames = setOf(packageName),
                    functionNames = setOf(AppFunctionName(packageName, functionId)),
                )
            )
            .firstOrNull()
    }
}
