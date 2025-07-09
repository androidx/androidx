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
import androidx.compose.foundation.text.contextmenu.provider.LocalTextContextMenuToolbarProvider
import androidx.compose.foundation.text.contextmenu.provider.TextContextMenuDataProvider
import androidx.compose.foundation.text.contextmenu.provider.TextContextMenuProvider
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
internal fun ProvidePlatformTextContextMenuToolbar(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    var layoutCoordinates by remember {
        mutableStateOf<LayoutCoordinates?>(null, policy = neverEqualPolicy())
    }

    val layoutCoordinatesBlock: () -> LayoutCoordinates = remember {
        { checkPreconditionNotNull(layoutCoordinates) }
    }
    val provider = platformTextContextMenuToolbarProvider(layoutCoordinatesBlock)

    CompositionLocalProvider(LocalTextContextMenuToolbarProvider provides provider) {
        Box(
            propagateMinConstraints = true,
            modifier = modifier.onGloballyPositioned { layoutCoordinates = it },
        ) {
            content()
        }
    }
}

@Composable
internal fun platformTextContextMenuToolbarProvider(
    anchorLayoutCoordinates: () -> LayoutCoordinates
): TextContextMenuProvider {
    val provider: TextContextMenuProvider = remember { WebTextContextMenuToolbarProvider() }
    return provider
}

internal class WebTextContextMenuToolbarProvider() : TextContextMenuProvider {
    override suspend fun showTextContextMenu(dataProvider: TextContextMenuDataProvider) {
        // TODO show web text toolbar here
    }
}
