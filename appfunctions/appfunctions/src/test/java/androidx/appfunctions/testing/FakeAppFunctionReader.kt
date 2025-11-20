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

package androidx.appfunctions.testing

import androidx.appfunctions.AppFunctionFunctionNotFoundException
import androidx.appfunctions.AppFunctionSearchSpec
import androidx.appfunctions.internal.AppFunctionReader
import androidx.appfunctions.metadata.AppFunctionMetadata

class FakeAppFunctionReader : AppFunctionReader {

    private val appFunctionMetadataMap = mutableMapOf<Pair<String, String>, AppFunctionMetadata>()

    fun addAppFunctionMetadata(metadata: AppFunctionMetadata) {
        appFunctionMetadataMap[Pair(metadata.id, metadata.packageName)] = metadata
    }

    fun clear() {
        appFunctionMetadataMap.clear()
    }

    override fun searchAppFunctions(searchFunctionSpec: AppFunctionSearchSpec) =
        TODO("Not yet implemented")

    override suspend fun getAppFunctionMetadata(
        functionId: String,
        packageName: String,
    ): AppFunctionMetadata? {
        val key = Pair(functionId, packageName)
        if (!appFunctionMetadataMap.containsKey(key)) throw AppFunctionFunctionNotFoundException()
        return appFunctionMetadataMap[key]
    }
}
