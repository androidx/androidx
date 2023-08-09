/*
 * Copyright 2022 The Android Open Source Project
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
package androidx.privacysandbox.sdkruntime.client.config

import android.content.Context
import java.io.FileNotFoundException

/**
 * Holds information about all SDKs bundled with App.
 *
 */
internal class LocalSdkConfigsHolder private constructor(
    private val configs: Map<String, LocalSdkConfig>
) {

    fun getSdkConfig(sdkName: String): LocalSdkConfig? {
        return configs[sdkName]
    }

    companion object {
        private const val SDK_TABLE_ASSET_NAME = "RuntimeEnabledSdkTable.xml"

        fun load(
            context: Context,
            sdkTableAssetName: String = SDK_TABLE_ASSET_NAME
        ): LocalSdkConfigsHolder {
            val sdkTable = loadSdkTable(context, sdkTableAssetName)

            val data = buildMap {
                for ((packageName, versionMajor, configPath) in sdkTable) {
                    context.assets.open(configPath).use { sdkConfigAsset ->
                        val sdkInfo = LocalSdkConfigParser.parse(
                            sdkConfigAsset,
                            packageName,
                            versionMajor
                        )
                        put(packageName, sdkInfo)
                    }
                }
            }

            return LocalSdkConfigsHolder(data)
        }

        private fun loadSdkTable(
            context: Context,
            sdkTableAssetName: String
        ): Set<SdkTableConfigParser.SdkTableEntry> {
            try {
                context.assets.open(sdkTableAssetName).use { sdkTableAsset ->
                    return SdkTableConfigParser.parse(sdkTableAsset)
                }
            } catch (ignored: FileNotFoundException) {
                return emptySet()
            }
        }
    }
}
