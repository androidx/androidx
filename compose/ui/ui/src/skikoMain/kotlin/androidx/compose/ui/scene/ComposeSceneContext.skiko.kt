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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionContext
import androidx.compose.runtime.CompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.node.LayoutNode
import androidx.compose.ui.platform.PlatformContext
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection

/**
 * Represents a static [CompositionLocal] key for a [ComposeSceneContext] in Jetpack Compose.
 *
 * @see ComposeSceneContext
 */
internal val LocalComposeSceneContext = staticCompositionLocalOf<ComposeSceneContext?> { null }

/**
 * The local [ComposeSceneContext] is typically not-null. This extension can be used in these cases.
 */
@Composable
internal fun CompositionLocal<ComposeSceneContext?>.requireCurrent(): ComposeSceneContext {
    return current ?: error("CompositionLocal LocalComposeSceneContext not provided")
}

/**
 * Interface representing the context for [ComposeScene].
 * It's used to share resources between multiple scenes and provide a way for platform interaction.
 */
@InternalComposeUiApi
interface ComposeSceneContext {
    /**
     * Represents the platform-specific context used for platform interaction in a [ComposeScene].
     */
    val platformContext: PlatformContext

    /**
     * Creates a scene layer to display content as a new [LayoutNode] tree.
     *
     * @param compositionContext The composition context for the layer.
     * @param density The density of the layer.
     * @param layoutDirection The layout direction of the layer.
     * @param focusable Indicates whether the layer is focusable.
     * @param consumePointerInputOutside Indicates whether the pointer input events outside the
     *  layer should be blocked.
     * @return The created [ComposeSceneLayer] representing the scene layer.
     *
     * @see ComposeSceneLayer
     */
    fun createLayer(
        compositionContext: CompositionContext,
        density: Density,
        layoutDirection: LayoutDirection,
        focusable: Boolean,
        consumePointerInputOutside: Boolean = focusable,
    ) : ComposeSceneLayer {
        throw IllegalStateException()
    }

    @InternalComposeUiApi
    class Empty : ComposeSceneContext {
        override val platformContext: PlatformContext = PlatformContext.Empty()
    }
}
