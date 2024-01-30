/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.compose.ui.scene

import androidx.compose.runtime.CompositionContext
import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.platform.PlatformContext
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection

/**
 * Interface representing the context for [ComposeScene].
 * It's used to share resources between multiple scenes and provide a way for platform interaction.
 */
@InternalComposeUiApi
interface ComposeSceneContext {
    /**
     * Represents the platform-specific context used for platform interaction in a [ComposeScene].
     */
    val platformContext: PlatformContext get() = PlatformContext.Empty

    /**
     * Creates a platform-specific layer for the [ComposeScene].
     *
     * @param density The density of the layer.
     * @param layoutDirection The layout direction of the layer.
     * @param focusable Indicates whether the layer is focusable.
     * @param compositionContext The composition context for the layer.
     * @return The created [ComposeSceneLayer] representing the platform-specific layer.
     */
    fun createPlatformLayer(
        density: Density,
        layoutDirection: LayoutDirection,
        focusable: Boolean,
        compositionContext: CompositionContext,
    ) : ComposeSceneLayer {
        throw IllegalStateException()
    }

    companion object {
        /**
         * Represents an empty implementation of [ComposeSceneContext] and used to provide
         * a default value.
         */
        val Empty = object : ComposeSceneContext {
        }
    }
}
