/*
 * Copyright 2024 The Android Open Source Project
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import platform.UIKit.UIView

/**
 * Compose a [UIView] of class [T] into the UI hierarchy.
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
 * @property properties The properties configuring the behavior of [T]. Default value is
 * [UIKitInteropProperties.Default]
 *
 * @see UIKitInteropProperties
 */
@Composable
fun <T : UIView> UIKitView(
    factory: () -> T,
    modifier: Modifier = Modifier,
    update: (T) -> Unit = NoOp,
    onRelease: (T) -> Unit = NoOp,
    onReset: ((T) -> Unit)? = null,
    properties: UIKitInteropProperties = UIKitInteropProperties.Default,
) {
    val interopContainer = LocalInteropContainer.current

    InteropView(
        factory = { compositeKeyHash ->
            UIKitInteropViewHolder(
                factory,
                interopContainer,
                properties,
                compositeKeyHash,
            )
        },
        modifier,
        onReset,
        onRelease,
        update = {
            update(it)

            val holder = interopContainer.holderOfView(it) as? UIKitInteropViewHolder<*>
            holder?.properties = properties
        }
    )
}