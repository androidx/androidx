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

package androidx.compose.ui.viewinterop

import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import org.w3c.dom.HTMLElement

actual typealias InteropView = Any

/**
 * Compose an [HTMLElement] of class [T] into the UI hierarchy.
 *
 * In the current implementation, the HTML element will overlay the canvas area according to the given size
 * (specified by [modifier]). The HTML element will intercept the input events in that area,
 * and Compose will not see those events.
 *
 * @param factory The block creating the [T] to be composed.
 *
 * NOTE: [T] shouldn't be leaked outside and will be managed by Compose runtime efficiently.
 * Remembering [T] externally and passing it to be returned from [factory] can (and probably will)
 * lead to hilarious bugs in case [onReset] is not `null`.
 * @param modifier The modifier to be applied to the layout.
 * @param update A callback to be invoked every time the state it reads changes.
 * Invoked once initially and then every time the state it reads changes.
 * @param onRelease A callback invoked as a signal that the [T] has exited the
 * composition forever. Use it to release resources and stop jobs associated with [T].
 * @param onReset If not null, this callback is invoked when this composable node is
 * reused in the composition instead of being recreated. Use it to reset the state of [T] to
 * some blank state. This is a function that will be executed instead of [factory] if the node
 * containing [T] was reused. If null, [T] will not be reused, a new instance of [T] will be created
 * using [factory] every time this function enters the composition.
 */
@ExperimentalComposeUiApi
@Composable
fun <T : HTMLElement> WebElementView(
    factory: () -> T,
    modifier: Modifier = Modifier,
    update: (T) -> Unit = NoOp,
    onRelease: (T) -> Unit = NoOp,
    onReset: ((T) -> Unit)? = null,
) {
    InternalWebElementView(
        factory = factory,
        modifier = modifier,
        update = update,
        onRelease = onRelease,
        onReset = onReset,
    )
}

internal actual class InteropViewGroup(val htmlElement: HTMLElement)

@Composable
internal fun <T : HTMLElement> InternalWebElementView(
    factory: () -> T,
    modifier: Modifier,
    update: (T) -> Unit,
    onRelease: (T) -> Unit,
    onReset: ((T) -> Unit)?,
) {
    val interopContainer = LocalInteropContainer.current

    InteropView(
        factory = { compositeKeyHash ->
            WebInteropViewHolder(
                factory,
                interopContainer,
                compositeKeyHash
            )
        },
        modifier,
        onReset,
        onRelease,
        update = {
            update(it)
        }
    )
}
