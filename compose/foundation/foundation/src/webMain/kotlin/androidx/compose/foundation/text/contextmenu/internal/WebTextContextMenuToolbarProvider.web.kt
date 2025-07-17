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

import androidx.compose.foundation.contextmenu.ContextMenuPopupPositionProvider
import androidx.compose.foundation.contextmenu.computeContextMenuColors
import androidx.compose.foundation.internal.checkPreconditionNotNull
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.text.WebContextMenuToolbarPopup
import androidx.compose.foundation.text.WebContextMenuToolbarSpec
import androidx.compose.foundation.text.contextmenu.data.TextContextMenuItemWithComposableLeadingIcon
import androidx.compose.foundation.text.contextmenu.data.TextContextMenuSession
import androidx.compose.foundation.text.contextmenu.provider.BasicTextContextMenuProvider
import androidx.compose.foundation.text.contextmenu.provider.LocalTextContextMenuToolbarProvider
import androidx.compose.foundation.text.contextmenu.provider.TextContextMenuDataProvider
import androidx.compose.foundation.text.contextmenu.provider.TextContextMenuProvider
import androidx.compose.foundation.text.contextmenu.provider.basicTextContextMenuProvider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.neverEqualPolicy
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.round
import androidx.compose.ui.util.fastForEach

@Composable
internal fun ProvideDefaultTextContextMenuToolbar(
    modifier: Modifier,
    content: @Composable () -> Unit
) {
    ProvidePlatformTextContextMenuToolbar(
        modifier = modifier,
        providableCompositionLocal = LocalTextContextMenuToolbarProvider,
        contextMenu = { session, dataProvider, anchorLayoutCoordinates ->
            OpenContextMenuToolbar(session, dataProvider, anchorLayoutCoordinates)
        },
        content = content
    )
}

@Composable
internal fun ProvidePlatformTextContextMenuToolbar(
    modifier: Modifier,
    providableCompositionLocal: ProvidableCompositionLocal<TextContextMenuProvider?>,
    contextMenu:
    @Composable
        (
        session: TextContextMenuSession,
        dataProvider: TextContextMenuDataProvider,
        anchorLayoutCoordinates: () -> LayoutCoordinates,
    ) -> Unit,
    content: @Composable () -> Unit,
) {
    var layoutCoordinates: LayoutCoordinates? by remember {
        mutableStateOf(null, neverEqualPolicy())
    }

    val layoutCoordinatesBlock: () -> LayoutCoordinates = remember {
        { checkPreconditionNotNull(layoutCoordinates) }
    }

    val provider = basicTextContextMenuProvider(contextMenu)
    CompositionLocalProvider(providableCompositionLocal provides provider) {
        Box(
            propagateMinConstraints = true,
            modifier = modifier.onGloballyPositioned { layoutCoordinates = it },
        ) {
            content()
            provider.ContextMenu(layoutCoordinatesBlock)
        }
    }
}

@Composable
internal fun defaultTextContextMenuToolbar(): BasicTextContextMenuProvider =
    basicTextContextMenuProvider { session, dataProvider, anchorLayoutCoordinates ->
        OpenContextMenuToolbar(session, dataProvider, anchorLayoutCoordinates)
    }

@Composable
private fun OpenContextMenuToolbar(
    session: TextContextMenuSession,
    dataProvider: TextContextMenuDataProvider,
    anchorLayoutCoordinates: () -> LayoutCoordinates,
) {
    val toolbarOffset = with(LocalDensity.current) {
        (WebContextMenuToolbarSpec.ContainerHeight + 16.dp).roundToPx()
    }.let { Offset(0f, it.toFloat()) }

    val popupPositionProvider = remember(dataProvider) {
        ContextMenuPopupPositionProvider(
            anchorPositionBlock = {
                val anchorLayoutCoordinates = anchorLayoutCoordinates()
                val localBoundingBox = dataProvider.position(anchorLayoutCoordinates)
                localBoundingBox.minus(toolbarOffset).round()
            }
        )
    }

    val data by remember(dataProvider) { derivedStateOf(dataProvider::data) }

    WebContextMenuToolbarPopup(
        popupPositionProvider = popupPositionProvider,
        onDismiss = { /*don't hide the toolbar on click outside*/ },
        colors = computeContextMenuColors(),
        contextMenuBuilderBlock = {
            data.components.fastForEach { component ->
                when (component) {
                    is TextContextMenuItemWithComposableLeadingIcon -> {
                        item(
                            label = { component.label },
                            enabled = component.enabled,
                            onClick = { component.onClick(session) },
                            leadingIcon = component.leadingIcon
                        )
                    }
                }
            }
        }
    )
}
