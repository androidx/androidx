/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.compose.ui.platform

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.res.ImageVectorCache
import androidx.compose.ui.res.ResourceIdCache

/**
 * Resource caches shared by Compose hierarchies that use the same `Context`, such as the caches
 * used by `painterResource` to resolve drawable resources.
 *
 * Share one instance between hierarchies whose `Context` has the same [Configuration]. Cached
 * entries can depend on the configuration, so sharing an instance between different configurations
 * can resolve the wrong resource variant.
 *
 * @param initialConfiguration The [Configuration] of the `Context` that the hierarchies use. Later
 *   changes are reported with [onConfigurationChanged].
 */
@ExperimentalComposeUiHostApi
public class ComposeUiSharedResources(initialConfiguration: Configuration) {
    private val currentConfiguration = Configuration(initialConfiguration)
    internal val imageVectorCache: ImageVectorCache = ImageVectorCache()
    internal val resourceIdCache: ResourceIdCache = ResourceIdCache()

    /**
     * Updates the caches for [newConfiguration]. Entries that depend on a changed part of the
     * configuration are removed. Any change clears the resource id cache.
     *
     * Calling this again with an unchanged configuration has no effect.
     */
    public fun onConfigurationChanged(newConfiguration: Configuration) {
        val changedFlags = currentConfiguration.updateFrom(newConfiguration)
        if (changedFlags != 0) {
            imageVectorCache.prune(changedFlags)
            resourceIdCache.clear()
        }
    }

    /** Clears all cached resources, for example in response to `onTrimMemory`. */
    public fun clear() {
        imageVectorCache.clear()
        resourceIdCache.clear()
    }

    /** Provides [LocalImageVectorCache] and [LocalResourceIdCache] for the given [content]. */
    @Composable
    internal fun ProvideCompositionLocals(content: @Composable () -> Unit) {
        CompositionLocalProvider(
            LocalImageVectorCache provides imageVectorCache,
            LocalResourceIdCache provides resourceIdCache,
            content = content,
        )
    }
}
