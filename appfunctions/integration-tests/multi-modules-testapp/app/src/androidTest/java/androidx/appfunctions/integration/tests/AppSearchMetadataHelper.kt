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

package androidx.appfunctions.integration.tests

import android.content.Context
import androidx.appsearch.app.GlobalSearchSession
import androidx.appsearch.app.SearchSpec
import androidx.appsearch.platformstorage.PlatformStorage
import androidx.concurrent.futures.await

internal object AppSearchMetadataHelper {
    /** Returns function IDs that belong to the given context's package. */
    suspend fun collectSelfFunctionIds(context: Context): Set<String> {
        val functionIds = mutableSetOf<String>()
        createSearchSession(context).use { session ->
            val searchResults =
                session.search(
                    "",
                    SearchSpec.Builder()
                        .addFilterNamespaces("app_functions_runtime")
                        .addFilterPackageNames("android")
                        .addFilterSchemas("AppFunctionRuntimeMetadata")
                        .build(),
                )
            var nextPage = searchResults.nextPageAsync.await()
            while (nextPage.isNotEmpty()) {
                for (result in nextPage) {
                    val packageName = result.genericDocument.getPropertyString("packageName")
                    if (packageName != context.packageName) {
                        continue
                    }
                    val functionId = result.genericDocument.getPropertyString("functionId")
                    functionIds.add(checkNotNull(functionId))
                }
                nextPage = searchResults.nextPageAsync.await()
            }
        }
        return functionIds
    }

    suspend fun isDynamicIndexerAvailable(context: Context): Boolean =
        createSearchSession(context).use { session ->
            val searchResults =
                session.search(
                    "",
                    SearchSpec.Builder()
                        .addFilterNamespaces("app_functions")
                        .addFilterPackageNames("android")
                        .addFilterSchemas("AppFunctionStaticMetadata")
                        .build(),
                )
            var nextPage = searchResults.nextPageAsync.await()
            while (nextPage.isNotEmpty()) {
                for (result in nextPage) {
                    val packageName = result.genericDocument.getPropertyString("packageName")
                    if (packageName != context.packageName) {
                        continue
                    }
                    return result.genericDocument.getPropertyDocument("response") != null
                }
                nextPage = searchResults.nextPageAsync.await()
            }
            throw IllegalStateException("No functions found for package ${context.packageName}")
        }

    private suspend fun createSearchSession(context: Context): GlobalSearchSession {
        return PlatformStorage.createGlobalSearchSessionAsync(
                PlatformStorage.GlobalSearchContext.Builder(context).build()
            )
            .await()
    }
}
