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

package androidx.compose.remote.creation.compose.layout

import androidx.annotation.RestrictTo
import androidx.compose.remote.creation.compose.capture.RemoteComposeCreationState
import androidx.compose.remote.creation.compose.modifier.DrawWithContentModifier
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.find
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ComposeNode
import androidx.compose.runtime.DisallowComposableCalls
import androidx.compose.runtime.Updater
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastForEachReversed

internal abstract class RemoteComposeNode {
    val children = mutableListOf<RemoteComposeNode>()
    var modifier: RemoteModifier = RemoteModifier

    abstract fun render(creationState: RemoteComposeCreationState, remoteCanvas: RemoteCanvas)

    fun renderChildren(
        creationState: RemoteComposeCreationState,
        remoteCanvas: RemoteCanvas,
        reversed: Boolean = false,
    ) {
        val drawWithContent = modifier.find<DrawWithContentModifier>()

        if (drawWithContent != null) {
            val drawWithContentScope = RemoteContentDrawScope(remoteCanvas)

            creationState.document.startCanvasOperations()
            drawWithContent.onDraw(drawWithContentScope)
            creationState.document.endCanvasOperations()
        }

        if (!reversed) {
            children.fastForEach { it.render(creationState, remoteCanvas) }
        } else {
            children.fastForEachReversed { it.render(creationState, remoteCanvas) }
        }
    }
}

internal class RemoteRootNode : RemoteComposeNode() {
    override fun render(creationState: RemoteComposeCreationState, remoteCanvas: RemoteCanvas) {
        creationState.document.root { renderChildren(creationState, remoteCanvas) }
    }
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Composable
internal inline fun <T : RemoteComposeNode> RemoteComposeNode(
    noinline factory: () -> T,
    update: @DisallowComposableCalls Updater<T>.() -> Unit,
) {
    ComposeNode<T, RemoteComposeApplier>(factory, update)
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Composable
internal inline fun <T : RemoteComposeNode> RemoteComposeNode(
    noinline factory: () -> T,
    update: @DisallowComposableCalls Updater<T>.() -> Unit,
    content: @Composable @RemoteComposable () -> Unit,
) {
    ComposeNode<T, RemoteComposeApplier>(factory, update, content)
}

internal fun shouldReverse(
    horizontalArrangement: RemoteArrangement.Horizontal,
    layoutDirection: LayoutDirection,
): Boolean =
    if (layoutDirection == LayoutDirection.Rtl) {
        when (horizontalArrangement) {
            is HorizontalArrangement -> !horizontalArrangement.isAbsolute()

            is RemoteSpacedAbsoluteHorizontalArrangement -> false

            else -> true
        }
    } else {
        false
    }
