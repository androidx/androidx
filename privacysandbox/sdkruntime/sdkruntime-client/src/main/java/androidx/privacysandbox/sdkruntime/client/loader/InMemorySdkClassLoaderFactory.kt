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
 */
internal abstract class InMemorySdkClassLoaderFactory : SdkLoader.ClassLoaderFactory {

    @RequiresApi(Build.VERSION_CODES.O_MR1)
    internal class InMemoryImpl(
        private val assetManager: AssetManager
    ) : InMemorySdkClassLoaderFactory() {

        @DoNotInline
        override fun createClassLoaderFor(
            sdkConfig: LocalSdkConfig,
            parent: ClassLoader
        ): ClassLoader {
            try {
                val buffers = arrayOfNulls<ByteBuffer>(sdkConfig.dexPaths.size)
                for (i in sdkConfig.dexPaths.indices) {
                    assetManager.open(sdkConfig.dexPaths[i]).use { inputStream ->
                        val byteBuffer = ByteBuffer.allocate(inputStream.available())
                        Channels.newChannel(inputStream).read(byteBuffer)
                        byteBuffer.flip()
                        buffers[i] = byteBuffer
                    }
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

    internal class FailImpl : InMemorySdkClassLoaderFactory() {
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

    companion object {
        fun create(context: Context): InMemorySdkClassLoaderFactory {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                InMemoryImpl(context.assets)
            } else {
                FailImpl()
            }
        }
    }
}