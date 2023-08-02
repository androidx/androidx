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
import android.os.Build
import android.os.Build.VERSION_CODES.JELLY_BEAN_MR2
import android.os.Environment
import android.os.StatFs
import android.util.Log
import androidx.annotation.DoNotInline
import androidx.annotation.RequiresApi
import androidx.privacysandbox.sdkruntime.client.config.LocalSdkConfig
import java.io.File

/**
 * Caching implementation of [LocalSdkStorage].
 *
 * Extract SDK DEX files into folder provided by [LocalSdkFolderProvider].
 * Not extracting new files if available space lower than lowSpaceThreshold.
 * Return result only if all files successfully extracted.
 */
internal class CachedLocalSdkStorage private constructor(
    private val context: Context,
    private val rootFolderProvider: LocalSdkFolderProvider,
    private val lowSpaceThreshold: Long
) : LocalSdkStorage {

    /**
     * Return SDK DEX files from folder provided by [LocalSdkFolderProvider].
     * Extract missing files from assets if available space bigger than lowSpaceThreshold.
     *
     * @param sdkConfig sdk config
     * @return [LocalSdkDexFiles] if all SDK DEX files available in SDK folder
     * or null if something missing and couldn't be extracted because of
     * available space lower than lowSpaceThreshold
     */
    override fun dexFilesFor(sdkConfig: LocalSdkConfig): LocalSdkDexFiles? {
        val disableExtracting = availableBytes() < lowSpaceThreshold
        val targetFolder = rootFolderProvider.dexFolderFor(sdkConfig)

        try {
            val files = buildList {
                for (index in sdkConfig.dexPaths.indices) {
                    val assetName = sdkConfig.dexPaths[index]
                    val outputFileName = "$index.dex"
                    val outputDexFile = File(targetFolder, outputFileName)

                    if (!outputDexFile.exists()) {
                        if (disableExtracting) {
                            Log.i(LOG_TAG, "Can't extract $assetName because of low space")
                            return null
                        }
                        extractAssetToFile(assetName, outputDexFile)
                    }

                    add(outputDexFile)
                }
            }
            return LocalSdkDexFiles(files)
        } catch (ex: Exception) {
            Log.e(
                LOG_TAG,
                "Failed to extract ${sdkConfig.packageName}, deleting $targetFolder.",
                ex
            )

            if (!targetFolder.deleteRecursively()) {
                Log.e(
                    LOG_TAG,
                    "Failed to delete $targetFolder during cleanup.",
                    ex
                )
            }

            throw ex
        }
    }

    private fun extractAssetToFile(
        assetName: String,
        outputFile: File,
    ) {
        outputFile.createNewFile()
        context.assets.open(assetName).use { fromStream ->
            outputFile.outputStream().use { toStream ->
                fromStream.copyTo(toStream)
            }
        }
        outputFile.setReadOnly()
    }

    private fun availableBytes(): Long {
        val dataDirectory = Environment.getDataDirectory()
        val statFs = StatFs(dataDirectory.path)
        if (Build.VERSION.SDK_INT >= JELLY_BEAN_MR2) {
            return Api18Impl.availableBytes(statFs)
        }

        @Suppress("DEPRECATION")
        val blockSize = statFs.blockSize.toLong()

        @Suppress("DEPRECATION")
        val availableBlocks = statFs.availableBlocks.toLong()

        return availableBlocks * blockSize
    }

    @RequiresApi(JELLY_BEAN_MR2)
    private object Api18Impl {
        @DoNotInline
        fun availableBytes(statFs: StatFs) = statFs.availableBytes
    }

    companion object {

        const val LOG_TAG = "CachedLocalSdkStorage"

        /**
         * Create CachedLocalSdkStorage.
         *
         * @param context Application context
         * @param lowSpaceThreshold Minimal available space in bytes required to proceed with
         * extracting new SDK Dex files.
         */
        fun create(
            context: Context,
            lowSpaceThreshold: Long
        ): CachedLocalSdkStorage {
            val localSdkFolderProvider = LocalSdkFolderProvider.create(context)
            return CachedLocalSdkStorage(context, localSdkFolderProvider, lowSpaceThreshold)
        }
    }
}
