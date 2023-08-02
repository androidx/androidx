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

package androidx.privacysandbox.sdkruntime.client.loader

import android.util.Log
import androidx.privacysandbox.sdkruntime.client.config.LocalSdkConfig
import androidx.privacysandbox.sdkruntime.client.loader.storage.LocalSdkStorage
import androidx.privacysandbox.sdkruntime.client.loader.storage.toClassPathString
import dalvik.system.BaseDexClassLoader
import java.io.File

/**
 * Loading SDK using BaseDexClassLoader.
 * Using [LocalSdkStorage] to get SDK DEX files,
 * if no files available - delegating to fallback.
 */
internal class FileClassLoaderFactory(
    private val localSdkStorage: LocalSdkStorage,
    private val fallback: SdkLoader.ClassLoaderFactory,
) : SdkLoader.ClassLoaderFactory {

    override fun createClassLoaderFor(
        sdkConfig: LocalSdkConfig,
        parent: ClassLoader
    ): ClassLoader {
        return tryCreateBaseDexClassLoaderFor(sdkConfig, parent)
            ?: fallback.createClassLoaderFor(
                sdkConfig,
                parent
            )
    }

    private fun tryCreateBaseDexClassLoaderFor(
        sdkConfig: LocalSdkConfig,
        parent: ClassLoader
    ): ClassLoader? {
        try {
            val dexFiles = localSdkStorage.dexFilesFor(sdkConfig)
            if (dexFiles == null) {
                Log.w(
                    LOG_TAG,
                    "Can't use BaseDexClassLoader for ${sdkConfig.packageName} - no dexFiles"
                )
                return null
            }

            val optimizedDirectory = File(dexFiles.files[0].parentFile, "DexOpt")
            if (!optimizedDirectory.exists()) {
                optimizedDirectory.mkdirs()
            }

            return BaseDexClassLoader(
                dexFiles.toClassPathString(),
                optimizedDirectory,
                /* librarySearchPath = */ null,
                parent
            )
        } catch (ex: Exception) {
            Log.e(
                LOG_TAG,
                "Failed to use BaseDexClassLoader for ${sdkConfig.packageName} - exception",
                ex
            )
            return null
        }
    }

    companion object {
        const val LOG_TAG =
            "FileClassLoaderFactory"
    }
}
