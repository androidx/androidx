/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.ui.framework.demos

import androidx.compose.Composable
import androidx.ui.core.Layout
import androidx.ui.unit.ipx

/**
 * This is a level 3 API, where the user uses the [SemanticActionBuilder] to build the action.
 * This component provides default values for all the parameters to the builder, the developer has
 * to just supply the callback lambda.
 */
@Suppress("Unused")
@Composable
fun ClickInteraction(
    click: SemanticActionBuilder<Unit>.() -> Unit,
    children: @Composable() () -> Unit
) {
    val clickAction = SemanticActionBuilder(phrase = "Click", defaultParam = Unit)
        .apply(click)
        .also {
            if (it.types.none { type -> type is AccessibilityAction }) {
                it.types += AccessibilityAction.Primary
            }
        }.build()

    val press = PressGestureDetectorWithActions(onRelease = clickAction)

    Semantics(actions = setOf(clickAction)) {
        // TODO(b/150706555): This layout is temporary and should be removed once Semantics
        //  is implemented with modifiers.
        Layout(children, press) { measurables, constraints, _ ->
            check(measurables.size == 1) {
                "Draggable temporarily assumes that it has exactly 1 child."
            }
            measurables.first().measure(constraints)
                .let { layout(it.width, it.height) { it.place(0.ipx, 0.ipx) } }
        }
    }
}

/**
 * Builder to create a semantic action.
 */
class SemanticActionBuilder<T>(
    var phrase: String,
    var defaultParam: T,
    var types: Set<ActionType> = setOf(),
    var action: (ActionParam<T>) -> Unit = {}
) {
    fun build() = SemanticAction(phrase, defaultParam, types, action)
}
