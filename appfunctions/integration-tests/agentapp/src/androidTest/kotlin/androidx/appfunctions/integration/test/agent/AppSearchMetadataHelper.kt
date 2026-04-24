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

package androidx.appfunctions.integration.test.agent

import android.content.Context
import android.util.Log
import androidx.appsearch.app.GlobalSearchSession
import androidx.appsearch.app.SearchSpec
import androidx.appsearch.platformstorage.PlatformStorage
import androidx.concurrent.futures.await

internal object AppSearchMetadataHelper {
    private const val TAG = "AppFunctionAppSearchHelper"

    /** Returns function IDs that belong to the given [targetPackage]. */
    suspend fun collectFunctionIds(context: Context, targetPackage: String): Set<String> {
        val staticIds = searchStaticMetadataIds(context, targetPackage)
        val runtimeIds = searchRuntimeMetadataIds(context, targetPackage)
        if (staticIds.size != runtimeIds.size) {
            Log.w(
                TAG,
                "Static metadata size ${staticIds.size} doesn't matc runtime " +
                    "metadata size ${runtimeIds.size}.",
            )
            return emptySet()
        }
        return runtimeIds.toSet()
    }

    suspend fun isDynamicIndexerAvailable(
        context: Context,
        packageName: String = "androidx.appfunctions.integration.testapp",
    ): Boolean =
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
                    val packageNameProperty =
                        result.genericDocument.getPropertyString("packageName")
                    if (packageNameProperty != packageName) {
                        continue
                    }
                    return result.genericDocument.getPropertyDocument("response") != null
                }
                nextPage = searchResults.nextPageAsync.await()
            }
            throw IllegalStateException("No functions found for package $packageName")
        }

    private suspend fun searchStaticMetadataIds(
        context: Context,
        packageName: String,
    ): List<String> {
        val functionIds = mutableListOf<String>()
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
                    val packageNameProperty =
                        result.genericDocument.getPropertyString("packageName")
                    if (packageNameProperty != packageName) {
                        continue
                    }
                    functionIds.add(result.genericDocument.id)
                }
                nextPage = searchResults.nextPageAsync.await()
            }
        }
        return functionIds
    }

    private suspend fun searchRuntimeMetadataIds(
        context: Context,
        targetPackageName: String,
    ): List<String> {
        val functionIds = mutableListOf<String>()
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
                    if (packageName != targetPackageName) {
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

    private suspend fun createSearchSession(context: Context): GlobalSearchSession {
        return PlatformStorage.createGlobalSearchSessionAsync(
                PlatformStorage.GlobalSearchContext.Builder(context).build()
            )
            .await()
    }
}
