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

package androidx.compose.foundation.text.contextmenu.provider

import androidx.compose.foundation.MutatorMutex
import androidx.compose.foundation.internal.checkPreconditionNotNull
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.text.contextmenu.data.TextContextMenuSession
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocal
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.neverEqualPolicy
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import kotlinx.coroutines.channels.Channel

/**
 * Creates a layout with both the [content] and [contextMenu]. [content] is always shown. The
 * [providableCompositionLocal] is set such that calling
 * [showTextContextMenu][TextContextMenuProvider.showTextContextMenu] on its
 * [current][CompositionLocal.current] value will show the [contextMenu]. Without that, it shows
 * only the [content].
 *
 * @param providableCompositionLocal The composition local to provide to. This will generally be
 *   either [LocalTextContextMenuDropdownProvider] or [LocalTextContextMenuToolbarProvider].
 * @param contextMenu The composable that is shown in a layout with the [content] when the
 *   [providableCompositionLocal]'s [current][CompositionLocal.current] is called with
 *   [showTextContextMenu][TextContextMenuProvider.showTextContextMenu].
 * @param content The content where the [providableCompositionLocal] will be set with a provider
 *   that shows the [contextMenu].
 */
// TODO(grantapher) Consider making public.
@Composable
internal fun ProvideBasicTextContextMenu(
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
    ProvideBasicTextContextMenu(Modifier, providableCompositionLocal, contextMenu, content)
}

@Composable
internal fun ProvideBasicTextContextMenu(
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

    val provider = basicTextContextMenuProvider(contextMenu)
    CompositionLocalProvider(providableCompositionLocal provides provider) {
        Box(
            propagateMinConstraints = true,
            modifier = modifier.onGloballyPositioned { layoutCoordinates = it },
        ) {
            content()
            provider.ContextMenu { checkPreconditionNotNull(layoutCoordinates) }
        }
    }
}

@Composable
internal fun basicTextContextMenuProvider(
    contextMenu:
        @Composable
        (
            session: TextContextMenuSession,
            dataProvider: TextContextMenuDataProvider,
            anchorLayoutCoordinates: () -> LayoutCoordinates,
        ) -> Unit
): BasicTextContextMenuProvider {
    val provider = remember(contextMenu) { BasicTextContextMenuProvider(contextMenu) }
    DisposableEffect(provider) { onDispose { provider.cancel() } }
    return provider
}

internal class BasicTextContextMenuProvider(
    private val contextMenuBlock:
        @Composable
        (
            session: TextContextMenuSession,
            dataProvider: TextContextMenuDataProvider,
            anchorLayoutCoordinates: () -> LayoutCoordinates,
        ) -> Unit
) : TextContextMenuProvider {
    private val mutatorMutex = MutatorMutex()
    private var session: SessionImpl? by mutableStateOf(null)

    override suspend fun showTextContextMenu(dataProvider: TextContextMenuDataProvider) {
        val localSession = SessionImpl(dataProvider)
        mutatorMutex.mutate {
            try {
                session = localSession
                localSession.awaitClose()
            } finally {
                session = null
            }
        }
    }

    @Composable
    fun ContextMenu(anchorLayoutCoordinates: () -> LayoutCoordinates) {
        val session = session ?: return
        contextMenuBlock(session, session.dataProvider, anchorLayoutCoordinates)
    }

    fun cancel() {
        session?.close()
    }

    private inner class SessionImpl(val dataProvider: TextContextMenuDataProvider) :
        TextContextMenuSession {
        private val channel = Channel<Unit>()

        override fun close() {
            channel.trySend(Unit)
        }

        suspend fun awaitClose() {
            channel.receive()
        }
    }
}
