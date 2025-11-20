/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.compose.foundation.text.contextmenu.internal

import androidx.compose.foundation.internal.checkPreconditionNotNull
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.text.contextmenu.provider.LocalTextContextMenuDropdownProvider
import androidx.compose.foundation.text.contextmenu.provider.LocalTextContextMenuToolbarProvider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.neverEqualPolicy
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned

@Composable
internal actual fun ProvideDefaultPlatformTextContextMenuProviders(
    modifier: Modifier,
    content: @Composable () -> Unit,
) {
    val dropdownDefined = LocalTextContextMenuDropdownProvider.current != null
    val toolbarDefined = LocalTextContextMenuToolbarProvider.current != null

    if (dropdownDefined && toolbarDefined) {
        // Both are already set up, just put the content in and wrap it with the modifier.
        Box(modifier, propagateMinConstraints = true) { content() }
    } else if (dropdownDefined) {
        // Dropdown is defined, so the toolbar isn't.
        ProvidePlatformTextContextMenuToolbar(modifier, content)
    } else if (toolbarDefined) {
        // Toolbar is defined, so the dropdown isn't.
        ProvideDefaultTextContextMenuDropdown(modifier, content)
    } else {
        // Neither is defined, so set up both.
        ProvideBothDefaultProviders(modifier, content)
    }
}

/** Set up both default providers, sharing what can be between the two. */
@Composable
private fun ProvideBothDefaultProviders(modifier: Modifier, content: @Composable () -> Unit) {
    var layoutCoordinates by remember {
        // onGloballyPositioned may fire with the same LayoutCoordinates containing different
        // positioning data, so always trigger read observation when this is set.
        mutableStateOf<LayoutCoordinates?>(null, policy = neverEqualPolicy())
    }

    val layoutCoordinatesBlock: () -> LayoutCoordinates = remember {
        { checkPreconditionNotNull(layoutCoordinates) }
    }

    val dropdownProvider = defaultTextContextMenuDropdown()
    val toolbarProvider = platformTextContextMenuToolbarProvider(layoutCoordinatesBlock)

    CompositionLocalProvider(
        LocalTextContextMenuToolbarProvider provides toolbarProvider,
        LocalTextContextMenuDropdownProvider provides dropdownProvider,
    ) {
        Box(
            propagateMinConstraints = true,
            modifier = modifier.onGloballyPositioned { layoutCoordinates = it },
        ) {
            content()
            dropdownProvider.ContextMenu(layoutCoordinatesBlock)
        }
    }
}
