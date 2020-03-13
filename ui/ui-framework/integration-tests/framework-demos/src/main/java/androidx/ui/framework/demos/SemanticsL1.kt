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

import androidx.ui.layout.Row
import androidx.ui.unit.PxPosition
import androidx.ui.core.Text
import androidx.ui.unit.dp
import androidx.ui.core.gesture.PressGestureDetector
import androidx.ui.unit.px
import androidx.ui.layout.Column
import androidx.ui.material.Button
import androidx.ui.material.MaterialTheme
import androidx.compose.Composable
import androidx.compose.state
import androidx.ui.foundation.Box
import androidx.ui.foundation.ContentGravity
import androidx.ui.layout.Arrangement
import androidx.ui.layout.LayoutGravity
import androidx.ui.layout.LayoutHeight
import androidx.ui.layout.LayoutSize
import androidx.ui.layout.LayoutWidth

/** A [SemanticProperty] is used to store semantic information about a component.
 *
 * Note: We use an interface instead of a sealed class here to allow SemanticProperty to be
 * implemented as an Enum (Enums cannot inherit from classes, but they can implement interfaces). */
interface SemanticProperty<T> {
    val value: T

    // Returning null signifies that this property cannot be merged.
    fun merge(other: SemanticProperty<T>): SemanticProperty<T>? = null
}

// These are some example SemanticProperties provided by the framework.

/** [Label] stores a string that describes the component. */
class Label(override var value: String) : SemanticProperty<String> {
    // Labels are concatenated.
    override fun merge(other: SemanticProperty<String>) = Label("$this $other")
}

/** [Visibility] stores an enum that represents the visibility of the component. */
enum class Visibility : SemanticProperty<Visibility> {
    @Suppress("Unused")
    Undefined,
    Visible,
    @Suppress("Unused")
    Invisible;

    override val value: Visibility get() = this

    // The visibility of the parent takes precedence.
    override fun merge(other: SemanticProperty<Visibility>) = this
}

/**
 * This class provides a way to store a function that is run when the [SemanticAction] is invoked.
 */
class SemanticAction<T>(
    val phrase: String = "",
    val defaultParam: T,
    val types: Set<ActionType> = setOf(),
    val action: (ActionParam<T>) -> Unit
)

/**
 * An extension function to invoke the action.
 */
fun <T> SemanticAction<T>.invoke(
    caller: ActionCaller = ActionCaller.Unknown,
    param: T = defaultParam
) = action(ActionParam(caller, param))

/**
 * The parameter sent to every callback. In addition to the parameter value, it also provides
 * information about the framework that raised the action.
 */
class ActionParam<T>(
    @Suppress("Unused") val caller: ActionCaller = ActionCaller.Unknown,
    val value: T
)

/**
 * Frameworks that invoke the action. The developer might be interested in knowing which framework
 * invoked the action.
 */
enum class ActionCaller {
    Unknown,
    Accessibility,
    @Suppress("Unused")
    AutoFill,
    Assistant,
    PointerInput,
    @Suppress("Unused")
    KeyInput
}

/**
 * The [ActionType] is a way to provide more information about a [SemanticAction]. It can be used by
 * other frameworks to identify an action that is to be run.
 *
 * Right now we are just using this interface as a common base class.
 * */
interface ActionType

// These are some example action types provided by the framework:
@Suppress("Unused")
class Autofill : ActionType

enum class PolarityAction : ActionType { Positive, Negative }

enum class AccessibilityAction : ActionType { Primary, Secondary }

@Suppress("Unused")
enum class EditAction : ActionType { Cut, Copy, Paste, Select, SelectAll, Clear, Undo }

@Suppress("Unused")
enum class NavigationAction : ActionType { Back, Forward, Up, Down, Left, Right }

/**
 * A PressGestureDetector that uses actions instead of lambda callbacks.
 *
 * This component just wraps the GestureDetetor and allows us to use it with actions, instead of
 * lambda functions. A [SemanticAction] allows us to specify more information in addition to the
 * lambda to be executed.*/
@Suppress("Unused")
@Composable
fun PressGestureDetectorWithActions(
    onPress: SemanticAction<PxPosition> = SemanticAction(defaultParam = PxPosition.Origin) { },
    onRelease: SemanticAction<Unit> = SemanticAction(defaultParam = Unit) { },
    onCancel: SemanticAction<Unit> = SemanticAction(defaultParam = Unit) { },
    children: @Composable() () -> Unit
) {
    PressGestureDetector(
        onPress = { onPress.action(ActionParam(ActionCaller.PointerInput, it)) },
        onRelease = { onRelease.action(ActionParam(ActionCaller.PointerInput, Unit)) },
        onCancel = { onCancel.action(ActionParam(ActionCaller.PointerInput, Unit)) },
        children = children
    )
}

/**
 * This is our lowest level API for Semantics.
 *
 * This implementation is a component just wraps its child components in a frame that has buttons
 * which represent some frameworks that will trigger the semantic actions.
 */
@Suppress("Unused")
@Composable
fun Semantics(
    @Suppress("UNUSED_PARAMETER") properties: Set<SemanticProperty<out Any>> = setOf(),
    actions: Set<SemanticAction<out Any?>> = setOf(),
    children: @Composable() () -> Unit
) {
    Column(LayoutHeight.Fill) {
        MaterialTheme {
            Collapsable {
                InvokeActionsByType(actions)
                InvokeActionsByPhrase(actions)
                InvokeActionsByAssistantAction(actions)
                InvokeActionsByParameters(actions)
            }
        }
        Row(LayoutWidth.Fill, arrangement = Arrangement.Center) {
            Box(
                LayoutGravity.Center + LayoutSize(500.dp, 300.dp),
                gravity = ContentGravity.Center,
                children = children
            )
        }
    }
}

/**
 * This component adds buttons to invoke actions based on the accessibility action type.
 */
@Composable
private fun InvokeActionsByType(actions: Set<SemanticAction<out Any?>> = setOf()) {
    val primary = actions.firstOrNull { it.types.contains(AccessibilityAction.Primary) }
    val secondary =
        actions.firstOrNull { it.types.contains(AccessibilityAction.Secondary) }
    Text(text = "Accessibility Actions By Type", style = MaterialTheme.typography().h6)
    Row(LayoutWidth.Fill, arrangement = Arrangement.SpaceEvenly) {
        Button(onClick = { primary?.invoke(ActionCaller.Accessibility) }) {
            Text("Primary")
        }
        Button(onClick = { secondary?.invoke(ActionCaller.Accessibility) }) {
            Text("Secondary")
        }
    }
}

/**
 * This component adds buttons to invoke actions based on the accessibility action phrase.
 */
@Composable
private fun InvokeActionsByPhrase(actions: Set<SemanticAction<out Any?>> = setOf()) {
    Text(
        text = "Accessibility Actions By Phrase",
        style = MaterialTheme.typography().h6
    )
    Row(LayoutWidth.Fill, arrangement = Arrangement.SpaceEvenly) {
        actions.forEach {
            Button(onClick = { it.invoke(ActionCaller.Accessibility) }) {
                Text(it.phrase)
            }
        }
    }
}

/**
 * This component adds buttons to invoke actions using the assistant.
 */
@Composable
private fun InvokeActionsByAssistantAction(actions: Set<SemanticAction<out Any?>> = setOf()) {
    val positive = actions.firstOrNull { it.types.contains(PolarityAction.Positive) }
    val negative = actions.firstOrNull { it.types.contains(PolarityAction.Negative) }
    Text(text = "Assistant Actions", style = MaterialTheme.typography().h6)
    Row(LayoutWidth.Fill, arrangement = Arrangement.SpaceEvenly) {
        Button(onClick = { negative?.invoke(ActionCaller.Assistant) }) {
            Text("Negative")
        }
        Button(onClick = { positive?.invoke(ActionCaller.Assistant) }) {
            Text("Positive")
        }
    }
}

/**
 * This component adds buttons to invoke actions based on the parameter type.
 * It is a more realistic example where the framework using the action will first find out the type
 * of action before invoking it.
 */
@Composable
private fun InvokeActionsByParameters(actions: Set<SemanticAction<out Any?>> = setOf()) {
    @Suppress("UNCHECKED_CAST")
    val pxPositionAction =
        actions.firstOrNull { it.defaultParam is PxPosition } as SemanticAction<PxPosition>?
    @Suppress("UNCHECKED_CAST")
    val unitAction =
        actions.firstOrNull { it.defaultParam is Unit } as SemanticAction<Unit>?
    Text(text = "Actions using Parameters", style = MaterialTheme.typography().h6)
    Row(LayoutWidth.Fill, arrangement = Arrangement.SpaceEvenly) {
        Button(onClick = { pxPositionAction?.invoke(param = PxPosition(1.px, 1.px)) }) {
            Text("IntAction")
        }
        Button(onClick = { unitAction?.invoke(param = Unit) }) {
            Text("VoidAction")
        }
    }
}

/**
 * Enum class used by the [Collapsable] component.
 */
private enum class CollapseMode { Visible, Collapsed }

/**
 * This composable wraps its children with a container and adds a show/hide button, to hide the
 * children or make them visible.
 */
@Composable
private fun Collapsable(children: @Composable() () -> Unit) {

    val collapsedState = state { CollapseMode.Collapsed }

    Row(LayoutWidth.Fill, arrangement = Arrangement.SpaceEvenly) {
        Button(onClick = {
            collapsedState.value = when (collapsedState.value) {
                CollapseMode.Collapsed -> CollapseMode.Visible
                CollapseMode.Visible -> CollapseMode.Collapsed
            }
        }) {
            Text("Show/Hide Actions")
        }
    }

    if (collapsedState.value == CollapseMode.Visible) {
        children()
    }
}
