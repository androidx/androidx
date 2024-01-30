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
import android.content.pm.PackageManager
import android.os.Build
import android.os.Build.VERSION_CODES.LOLLIPOP
import android.os.Build.VERSION_CODES.TIRAMISU
import androidx.annotation.DoNotInline
import androidx.annotation.RequiresApi
import androidx.privacysandbox.sdkruntime.client.config.LocalSdkConfig
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File

/**
 * Create folders for Local SDKs in ([Context.getCodeCacheDir] / RuntimeEnabledSdk / <packageName>)
 * Store Application update time ([android.content.pm.PackageInfo.lastUpdateTime]) in
 * ([Context.getCodeCacheDir] / RuntimeEnabledSdk / Folder.version) file.
 * Remove SDK Folders if Application was updated after folders were created.
 */
internal class LocalSdkFolderProvider private constructor(
    private val sdkRootFolder: File
) {

    /**
     * Return folder on storage that should be used for storing SDK DEX files.
     */
    fun dexFolderFor(sdkConfig: LocalSdkConfig): File {
        val sdkDexFolder = File(sdkRootFolder, sdkConfig.packageName)
        if (!sdkDexFolder.exists()) {
            sdkDexFolder.mkdirs()
        }
        return sdkDexFolder
    }

    companion object {

        private const val SDK_ROOT_FOLDER = "RuntimeEnabledSdk"
        private const val CODE_CACHE_FOLDER = "code_cache"
        private const val VERSION_FILE_NAME = "Folder.version"

        /**
         * Create LocalSdkFolderProvider.
         *
         * Check if current root folder created in same app installation
         * and remove folder content if not.
         */
        fun create(context: Context): LocalSdkFolderProvider {
            val sdkRootFolder = createSdkRootFolder(context)
            return LocalSdkFolderProvider(sdkRootFolder)
        }

        private fun createSdkRootFolder(context: Context): File {
            val rootFolder = getRootFolderPath(context)
            val versionFile = File(rootFolder, VERSION_FILE_NAME)

            val sdkRootFolderVersion = readVersion(versionFile)
            val lastUpdateTime = appLastUpdateTime(context)

            if (lastUpdateTime != sdkRootFolderVersion) {
                if (rootFolder.exists()) {
                    rootFolder.deleteRecursively()
                }
                rootFolder.mkdirs()
                versionFile.createNewFile()

                versionFile.outputStream().use { outputStream ->
                    DataOutputStream(outputStream).use { dataStream ->
                        dataStream.writeLong(lastUpdateTime)
                    }
                }
            }

            return rootFolder
        }

        private fun getRootFolderPath(context: Context): File {
            if (Build.VERSION.SDK_INT >= LOLLIPOP) {
                return File(Api21Impl.codeCacheDir(context), SDK_ROOT_FOLDER)
            }

            // Emulate code cache folder
            val dataDir = context.applicationInfo.dataDir
            val codeCacheDir = File(dataDir, CODE_CACHE_FOLDER)
            codeCacheDir.mkdir()

            return File(codeCacheDir, SDK_ROOT_FOLDER)
        }

        private fun appLastUpdateTime(context: Context): Long {
            if (Build.VERSION.SDK_INT >= TIRAMISU) {
                return Api33Impl.getLastUpdateTime(context)
            }

            @Suppress("DEPRECATION")
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            return packageInfo.lastUpdateTime
        }

        private fun readVersion(versionFile: File): Long? {
            if (!versionFile.exists()) {
                return null
            }
            try {
                versionFile.inputStream().use { inputStream ->
                    DataInputStream(inputStream).use { dataStream ->
                        return dataStream.readLong()
                    }
                }
            } catch (e: Exception) {
                // Failed to parse or IOException, treat as no version file exists.
                return null
            }
        }
    }

    @RequiresApi(LOLLIPOP)
    private object Api21Impl {
        @DoNotInline
        fun codeCacheDir(context: Context): File =
            context.codeCacheDir
    }

    @RequiresApi(TIRAMISU)
    private object Api33Impl {
        @DoNotInline
        fun getLastUpdateTime(context: Context): Long =
            context.packageManager.getPackageInfo(
                context.packageName,
                PackageManager.PackageInfoFlags.of(0)
            ).lastUpdateTime
    }
}
