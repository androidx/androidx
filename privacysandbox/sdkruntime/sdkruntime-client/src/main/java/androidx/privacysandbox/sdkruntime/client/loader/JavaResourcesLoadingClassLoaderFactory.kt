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

import androidx.privacysandbox.sdkruntime.client.config.LocalSdkConfig
import java.io.File
import java.io.IOException
import java.net.URL
import java.util.Enumeration

/**
 * Delegate java resources related calls to app classloader.
 *
 * Classloaders normally delegate calls to parent classloader first, that's why this factory
 * creates classloader that will work with java resources and pass it as parent to
 * [codeClassLoaderFactory] thus overrides java resources for all classes loaded down the line.
 *
 * Add [LocalSdkConfig.javaResourcesRoot] as prefix to resource names before delegating calls,
 * thus allowing isolating java resources for different local sdks.
 */
internal class JavaResourcesLoadingClassLoaderFactory(
    private val appClassloader: ClassLoader,
    private val codeClassLoaderFactory: SdkLoader.ClassLoaderFactory
) : SdkLoader.ClassLoaderFactory {
    override fun createClassLoaderFor(
        sdkConfig: LocalSdkConfig,
        parent: ClassLoader
    ): ClassLoader {
        val javaResourcesLoadingClassLoader = createJavaResourcesLoadingClassLoader(
            sdkConfig,
            parent
        )
        return codeClassLoaderFactory.createClassLoaderFor(
            sdkConfig,
            parent = javaResourcesLoadingClassLoader
        )
    }

    private fun createJavaResourcesLoadingClassLoader(
        sdkConfig: LocalSdkConfig,
        parent: ClassLoader
    ): ClassLoader {
        return if (sdkConfig.javaResourcesRoot == null) {
            parent
        } else {
            JavaResourcesLoadingClassLoader(
                parent,
                appClassloader,
                File(ASSETS_DIR, sdkConfig.javaResourcesRoot)
            )
        }
    }

    private class JavaResourcesLoadingClassLoader constructor(
        parent: ClassLoader,
        private val appClassloader: ClassLoader,
        private val javaResourcePrefix: File
    ) : ClassLoader(parent) {
        override fun findResource(name: String): URL? {
            return appClassloader.getResource(File(javaResourcePrefix, name).path)
        }

        @Throws(IOException::class)
        override fun findResources(name: String): Enumeration<URL> {
            return appClassloader.getResources(File(javaResourcePrefix, name).path)
        }
    }

    companion object {
        const val ASSETS_DIR = "assets/"
    }
}
