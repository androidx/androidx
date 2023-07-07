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

import android.content.Context
import android.content.res.AssetManager
import android.os.Build
import androidx.annotation.DoNotInline
import androidx.annotation.RequiresApi
import androidx.privacysandbox.sdkruntime.client.config.LocalSdkConfig
import androidx.privacysandbox.sdkruntime.core.LoadSdkCompatException
import dalvik.system.InMemoryDexClassLoader
import java.nio.ByteBuffer
import java.nio.channels.Channels

/**
 * Loading SDK in memory on API 27+
 * Also support single DEX SDKs on API 26.
 */
internal abstract class InMemorySdkClassLoaderFactory : SdkLoader.ClassLoaderFactory {

    @RequiresApi(Build.VERSION_CODES.O_MR1)
    private class Api27Impl(
        private val assetLoader: AssetLoader
    ) : InMemorySdkClassLoaderFactory() {

        @DoNotInline
        override fun createClassLoaderFor(
            sdkConfig: LocalSdkConfig,
            parent: ClassLoader
        ): ClassLoader {
            try {
                val buffers = arrayOfNulls<ByteBuffer>(sdkConfig.dexPaths.size)
                for (i in sdkConfig.dexPaths.indices) {
                    buffers[i] = assetLoader.load(sdkConfig.dexPaths[i])
                }
                return InMemoryDexClassLoader(buffers, parent)
            } catch (ex: Exception) {
                throw LoadSdkCompatException(
                    LoadSdkCompatException.LOAD_SDK_INTERNAL_ERROR,
                    "Failed to instantiate classloader",
                    ex
                )
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private class Api26Impl(
        private val assetLoader: AssetLoader
    ) : InMemorySdkClassLoaderFactory() {

        @DoNotInline
        override fun createClassLoaderFor(
            sdkConfig: LocalSdkConfig,
            parent: ClassLoader
        ): ClassLoader {
            if (sdkConfig.dexPaths.size != 1) {
                throw LoadSdkCompatException(
                    LoadSdkCompatException.LOAD_SDK_SDK_SANDBOX_DISABLED,
                    "Can't use InMemoryDexClassLoader - API 26 supports only single DEX"
                )
            }
            try {
                val byteBuffer = assetLoader.load(sdkConfig.dexPaths[0])
                return InMemoryDexClassLoader(byteBuffer, parent)
            } catch (ex: Exception) {
                throw LoadSdkCompatException(
                    LoadSdkCompatException.LOAD_SDK_INTERNAL_ERROR,
                    "Failed to instantiate classloader",
                    ex
                )
            }
        }
    }

    private class FailImpl : InMemorySdkClassLoaderFactory() {
        @DoNotInline
        override fun createClassLoaderFor(
            sdkConfig: LocalSdkConfig,
            parent: ClassLoader
        ): ClassLoader {
            throw LoadSdkCompatException(
                LoadSdkCompatException.LOAD_SDK_SDK_SANDBOX_DISABLED,
                "Can't use InMemoryDexClassLoader"
            )
        }
    }

    private class AssetLoader(
        private val assetManager: AssetManager
    ) {
        fun load(assetName: String): ByteBuffer {
            return assetManager.open(assetName).use { inputStream ->
                val byteBuffer = ByteBuffer.allocate(inputStream.available())
                Channels.newChannel(inputStream).read(byteBuffer)
                byteBuffer.flip()
                byteBuffer
            }
        }
    }

    companion object {
        fun create(context: Context): InMemorySdkClassLoaderFactory {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                Api27Impl(AssetLoader(context.assets))
            } else if (Build.VERSION.SDK_INT == Build.VERSION_CODES.O) {
                Api26Impl(AssetLoader(context.assets))
            } else {
                FailImpl()
            }
        }
    }
}
