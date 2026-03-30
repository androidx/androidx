/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.compose.ui.res

import java.io.InputStream

/**
 * Open [InputStream] from a resource stored in resources for the application, calls the [block]
 * callback giving it a InputStream and closes stream once the processing is
 * complete.
 *
 * @param resourcePath  path of resource
 * @return object that was returned by [block]
 *
 * @throws IllegalArgumentException if there is no [resourcePath] in resources
 */
@Suppress("DEPRECATION")
@Deprecated("Migrate to the Compose resources library. See https://www.jetbrains.com/help/kotlin-multiplatform-dev/compose-images-resources.html")
inline fun <T> useResource(
    resourcePath: String,
    block: (InputStream) -> T
): T = openResource(resourcePath).use(block)

/**
 * Open [InputStream] from a resource stored in resources for the application.
 *
 * @param resourcePath  path of resource
 *
 * @throws IllegalArgumentException if there is no [resourcePath] in resources
 */
@PublishedApi
@Suppress("DEPRECATION")
@Deprecated("Migrate to the Compose resources library. See https://www.jetbrains.com/help/kotlin-multiplatform-dev/compose-images-resources.html")
internal fun openResource(
    resourcePath: String,
): InputStream = ClassLoaderResourceLoader.load(resourcePath)

/**
 * Resource loader based on JVM current context class loader.
 */
private object ClassLoaderResourceLoader {
    fun load(resourcePath: String): InputStream {
        // TODO(https://youtrack.jetbrains.com/issue/CMP-6099): probably we shouldn't use
        //  contextClassLoader here, as it is not defined in threads created by non-JVM
        val contextClassLoader = Thread.currentThread().contextClassLoader!!
        val resource = contextClassLoader.getResourceAsStream(resourcePath)
            ?: this::class.java.getResourceAsStream(resourcePath)
        return requireNotNull(resource) { "Resource $resourcePath not found" }
    }
}
