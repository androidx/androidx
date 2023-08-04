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

package androidx.compose.ui.window

import androidx.compose.runtime.*
import androidx.compose.ui.LocalComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerInputEvent
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.MeasurePolicy
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.SkiaBasedOwner
import androidx.compose.ui.platform.setContent
import androidx.compose.ui.requireCurrent
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.round

/**
 * Adding [content] as root layout to separate [androidx.compose.ui.node.Owner].
 */
@Composable
internal fun RootLayout(
    modifier: Modifier,
    focusable: Boolean,
    onOutsidePointerEvent: ((PointerInputEvent) -> Unit)? = null,
    content: @Composable (SkiaBasedOwner) -> Unit
) {
    /*
     * Keep empty layout as workaround to trigger layout after remove dialog.
     * Required to properly update mouse hover state.
     */
    EmptyLayout()

    val scene = LocalComposeScene.requireCurrent()
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current
    val parentComposition = rememberCompositionContext()
    val (owner, composition) = remember {
        val owner = SkiaBasedOwner(
            scene = scene,
            platform = scene.platform,
            pointerPositionUpdater = scene.pointerPositionUpdater,
            coroutineContext = parentComposition.effectCoroutineContext,
            initDensity = density,
            initLayoutDirection = layoutDirection,
            focusable = focusable,
            onOutsidePointerEvent = onOutsidePointerEvent,
            modifier = modifier
        )
        scene.attach(owner)
        owner to owner.setContent(parent = parentComposition) {
            content(owner)
        }
    }
    DisposableEffect(Unit) {
        onDispose {
            scene.detach(owner)
            composition.dispose()
            owner.dispose()
        }
    }
    SideEffect {
        owner.density = density
        owner.layoutDirection = layoutDirection
    }
}

@Composable
internal fun EmptyLayout(
    modifier: Modifier = Modifier
) = Layout(
    content = {},
    modifier = modifier,
    measurePolicy = { _, _ ->
        layout(0, 0) {}
    }
)
