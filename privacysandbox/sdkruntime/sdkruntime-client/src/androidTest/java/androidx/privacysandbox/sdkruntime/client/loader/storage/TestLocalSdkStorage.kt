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

package androidx.privacysandbox.sdkruntime.client.loader.storage

import android.content.Context
import androidx.privacysandbox.sdkruntime.client.config.LocalSdkConfig
import java.io.File

/**
 * Extract SDK DEX files in [rootFolder] / <packageName>.
 */
internal class TestLocalSdkStorage(
    private val context: Context,
    private val rootFolder: File
) : LocalSdkStorage {
    override fun dexFilesFor(sdkConfig: LocalSdkConfig): LocalSdkDexFiles {
        val outputFolder = File(rootFolder, sdkConfig.packageName)
        outputFolder.deleteRecursively()
        outputFolder.mkdirs()

        val fileList = buildList {
            for (index in sdkConfig.dexPaths.indices) {
                val dexFile = File(outputFolder, "$index.dex")
                dexFile.createNewFile()
                context.assets.open(sdkConfig.dexPaths[index]).use { inputStream ->
                    dexFile.outputStream().use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }
                dexFile.setReadOnly()
                add(dexFile)
            }
        }

        return LocalSdkDexFiles(fileList)
    }
}
