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

@file:Suppress("RestrictedApiAndroidX", "PrimitiveInCollection")

package androidx.compose.remote.player.compose.embedded.components

import androidx.compose.foundation.layout.Box
import androidx.compose.remote.core.operations.layout.ClickModifierOperation
import androidx.compose.remote.core.operations.layout.MultiClickModifier
import androidx.compose.remote.core.operations.layout.managers.Custom
import androidx.compose.remote.player.compose.embedded.CustomComposablePlugin
import androidx.compose.remote.player.compose.embedded.LocalRcCustomPlugins
import androidx.compose.remote.player.compose.embedded.LocalRemoteContext
import androidx.compose.remote.player.compose.embedded.RcCustomComponent
import androidx.compose.remote.player.compose.embedded.modifier.ClickActionHandlers
import androidx.compose.remote.player.compose.embedded.modifier.rememberClickActionHandlers
import androidx.compose.remote.player.compose.embedded.state.rememberRemoteStringAsState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.util.fastFilter

/** Resolves the config name of a [Custom] component. */
@Composable
private fun rememberCustomConfig(layout: Custom): String {
    return if (layout.configId != -1) {
        rememberRemoteStringAsState(layout.configId).value
    } else {
        layout.config ?: ""
    }
}

/**
 * Returns the click handlers for [layout] if its plugin handles clicks itself (see
 * [CustomComposablePlugin.handlesClick]), otherwise `null`. When non-null, the player must not also
 * apply the component's click modifiers, and each individual handler on [ClickActionHandlers] is
 * `null` if the component has no actions for that gesture.
 */
@Composable
internal fun rememberCustomClickHandlers(layout: Custom): ClickActionHandlers? {
    val customPlugins = LocalRcCustomPlugins.current ?: return null
    val config = rememberCustomConfig(layout)
    if (!customPlugins.handlesClick(config)) return null
    val clickOps =
        layout.componentModifiers.list.fastFilter {
            it is ClickModifierOperation || it is MultiClickModifier
        }
    return rememberClickActionHandlers(clickOps)
}

/** Renders a [Custom] component by delegating to the host's [LocalRcCustomPlugins] registry. */
@Composable
internal fun RcPlayerCustom(
    layout: Custom,
    modifier: Modifier,
    clickHandlers: ClickActionHandlers? = null,
) {
    val remoteContext = LocalRemoteContext.current
    val customPlugins = LocalRcCustomPlugins.current

    val config = rememberCustomConfig(layout)

    Box(modifier = modifier) {
        val component =
            RcCustomComponent(
                config = config,
                componentId = layout.componentId,
                rawProperties = layout.properties,
                remoteContext = remoteContext,
                children = layout.childrenComponents,
                onClick = clickHandlers?.onClick,
                onDoubleClick = clickHandlers?.onDoubleClick,
                onLongClick = clickHandlers?.onLongClick,
            )
        customPlugins?.Render(component, modifier = Modifier)
    }
}
