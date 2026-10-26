/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.a2ui.compose.runtime.samples

import androidx.a2ui.compose.runtime.A2uiComponentProperties
import androidx.a2ui.compose.runtime.A2uiComponentScope
import androidx.a2ui.compose.runtime.A2uiComponentState
import androidx.a2ui.compose.runtime.A2uiProperty
import androidx.a2ui.compose.runtime.ProvideActionInterceptor
import androidx.a2ui.compose.ui.A2uiCatalog
import androidx.a2ui.compose.ui.A2uiComponent
import androidx.a2ui.model.protocol.A2uiException.A2uiRuntimeException
import androidx.annotation.Sampled
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Sampled
fun A2uiComponentScopeSample() {
    // The properties describe the component to the agent and are the typed keys to its payload.
    val valueProperty =
        A2uiProperty.dynamicNumber("value", required = true, description = "The current value.")
    val stepProperty =
        A2uiProperty.number("step", defaultValue = 1, description = "The amount added per tap.")
    val onChangeProperty =
        A2uiProperty.action("onChange", description = "Dispatched after the value changes.")

    val stepper =
        object : A2uiComponent {
            override val name = "Stepper"
            override val description = "Shows a number with buttons to decrement and increment it."
            override val properties = listOf(valueProperty, stepProperty, onChangeProperty)

            // The scope is the receiver of both isReady and Content. bind() evaluates a dynamic
            // property against the data model and recomposes the caller when that data changes;
            // while it returns null for a required property, the component stays in its loading
            // state.
            @Composable
            override fun A2uiComponentScope.isReady(properties: A2uiComponentProperties): Boolean =
                properties.bind(valueProperty) != null

            @Composable
            override fun A2uiComponentScope.Content(
                properties: A2uiComponentProperties,
                modifier: Modifier,
            ) {
                // isReady keeps this content out of the composition until "value" resolves, and
                // moves the component back to its loading state if the data is removed later.
                // Content that is still animating out may recompose once without the data, so
                // return early rather than assert.
                val value = properties.bind(valueProperty)?.toDouble() ?: return
                // Static properties are read directly; the scope is not involved.
                val step = properties[stepProperty]?.toDouble() ?: 1.0
                val onChange = properties[onChangeProperty]
                // Non-null only if the agent bound "value" to a data path the user may write to.
                val setValue = properties.bindUpdater(valueProperty)

                if (step <= 0) {
                    // A payload that is well-formed but makes no sense: report it once per value.
                    // The renderer then shows this component as failed until the agent corrects it.
                    SideEffect(step) {
                        reportError(A2uiRuntimeException("'step' must be positive but was $step."))
                    }
                }

                fun changeBy(delta: Double) {
                    setValue?.invoke(value + delta)
                    // The action payload is sent to the agent as is, with its bindings evaluated.
                    if (onChange != null) dispatchAction(onChange)
                }

                Row(modifier) {
                    BasicText(
                        "-",
                        Modifier.clickable(enabled = setValue != null) { changeBy(-step) },
                    )
                    BasicText(value.toString(), Modifier.padding(horizontal = 8.dp))
                    BasicText(
                        "+",
                        Modifier.clickable(enabled = setValue != null) { changeBy(step) },
                    )
                }
            }
        }

    A2uiCatalog(catalogId = "https://example.com/catalog/v1", components = listOf(stepper))
}

@Sampled
fun A2uiObserveComponentStateSample() {
    // The agent fills each slot with the ID of another component of the surface.
    val labelProperty = A2uiProperty.componentId("label", required = true)
    val contentProperty = A2uiProperty.componentId("content", required = true)

    val labeled =
        object : A2uiComponent {
            override val name = "Labeled"
            override val description = "Places a label component above a content component."
            override val properties = listOf(labelProperty, contentProperty)

            @Composable
            override fun A2uiComponentScope.Content(
                properties: A2uiComponentProperties,
                modifier: Modifier,
            ) {
                val labelId = properties[labelProperty] ?: return
                val contentId = properties[contentProperty] ?: return
                Column(modifier) {
                    Child(labelId)
                    Child(contentId)
                }
            }

            // Observing a child's state subscribes this component to the child's definition and
            // data. The child renders in this component's data context, so its relative bindings
            // resolve against the same data as this component's own.
            @Composable
            private fun A2uiComponentScope.Child(id: String) {
                when (val state = observeA2uiComponentState(id)) {
                    is A2uiComponentState.Loading -> BasicText("…")
                    is A2uiComponentState.Error -> BasicText("Could not render $id")
                    is A2uiComponentState.Success -> A2uiComponent(state.component)
                }
            }
        }

    A2uiCatalog(catalogId = "https://example.com/catalog/v1", components = listOf(labeled))
}

@Sampled
fun A2uiDispatchActionSample() {
    val labelProperty = A2uiProperty.dynamicString("label", required = true)
    // The agent decides what a tap does: {"event": {"name": "select", "context": {...}}} sends an
    // event to the agent, {"call": "openUrl", "args": {...}} runs a function on the device.
    val onTapProperty = A2uiProperty.action("onTap", required = true)

    val chip =
        object : A2uiComponent {
            override val name = "Chip"
            override val description = "A tappable label."
            override val properties = listOf(labelProperty, onTapProperty)

            @Composable
            override fun A2uiComponentScope.isReady(properties: A2uiComponentProperties): Boolean =
                properties.bind(labelProperty) != null

            @Composable
            override fun A2uiComponentScope.Content(
                properties: A2uiComponentProperties,
                modifier: Modifier,
            ) {
                val label = properties.bind(labelProperty) ?: return
                val onTap = properties[onTapProperty] ?: return
                // The payload is passed on as sent; the renderer evaluates its data bindings when
                // the action is dispatched, so an event carries the data model's current values.
                BasicText(label, modifier.clickable { dispatchAction(onTap) })
            }
        }

    A2uiCatalog(catalogId = "https://example.com/catalog/v1", components = listOf(chip))
}

@Sampled
fun A2uiReportErrorSample() {
    val colorProperty =
        A2uiProperty.dynamicString(
            "color",
            required = true,
            description = "A color in hexadecimal notation, such as #FF8800.",
        )

    val swatch =
        object : A2uiComponent {
            override val name = "Swatch"
            override val description = "A square filled with a color."
            override val properties = listOf(colorProperty)

            @Composable
            override fun A2uiComponentScope.isReady(properties: A2uiComponentProperties): Boolean =
                properties.bind(colorProperty) != null

            @Composable
            override fun A2uiComponentScope.Content(
                properties: A2uiComponentProperties,
                modifier: Modifier,
            ) {
                val hex = properties.bind(colorProperty) ?: return
                // bind() guarantees a String, not a valid color; checking that is up to the
                // component.
                val rgb = hex.removePrefix("#").takeIf { it.length == 6 }?.toLongOrNull(radix = 16)
                if (rgb == null) {
                    // Report once per invalid value. The renderer then shows this component as
                    // failed, through its parent, until the agent sends a corrected definition; a
                    // neutral color covers the frame in between.
                    SideEffect(hex) {
                        reportError(A2uiRuntimeException("'color' is not a hex color: '$hex'."))
                    }
                }
                val color = if (rgb == null) Color.Gray else Color(0xFF000000 or rgb)
                Box(modifier.size(24.dp).background(color))
            }
        }

    A2uiCatalog(catalogId = "https://example.com/catalog/v1", components = listOf(swatch))
}

@Sampled
fun A2uiBindSample() {
    // The agent may send a literal, "Hello", a data binding, {"path": "/user/name"}, or a function
    // call; bind() evaluates all three to a String.
    val textProperty = A2uiProperty.dynamicString("text", required = true)

    val label =
        object : A2uiComponent {
            override val name = "Label"
            override val description = "Displays a line of text."
            override val properties = listOf(textProperty)

            // Not ready until the bound data has arrived; the parent shows a placeholder meanwhile.
            @Composable
            override fun A2uiComponentScope.isReady(properties: A2uiComponentProperties): Boolean =
                properties.bind(textProperty) != null

            @Composable
            override fun A2uiComponentScope.Content(
                properties: A2uiComponentProperties,
                modifier: Modifier,
            ) {
                // Recomposes when the data at the bound path changes. If the data is removed,
                // isReady moves the component back to its loading state; only content that is still
                // animating out can see null here, so return early rather than assert.
                val text = properties.bind(textProperty) ?: return
                BasicText(text, modifier)
            }
        }

    A2uiCatalog(catalogId = "https://example.com/catalog/v1", components = listOf(label))
}

@Sampled
fun A2uiBindUpdaterSample() {
    val labelProperty = A2uiProperty.dynamicString("label", required = true)
    // Two-way binding needs a data path, {"checked": {"path": "/form/subscribed"}}: the toggle
    // reads the current value with bind() and writes the user's changes back with the updater.
    val checkedProperty = A2uiProperty.dynamicBoolean("checked", required = true)

    val toggle =
        object : A2uiComponent {
            override val name = "Toggle"
            override val description = "A labeled switch whose state is stored in the data model."
            override val properties = listOf(labelProperty, checkedProperty)

            @Composable
            override fun A2uiComponentScope.isReady(properties: A2uiComponentProperties): Boolean =
                properties.bind(labelProperty) != null

            @Composable
            override fun A2uiComponentScope.Content(
                properties: A2uiComponentProperties,
                modifier: Modifier,
            ) {
                val label = properties.bind(labelProperty) ?: return
                // Null until the agent or the user writes to the path, so start switched off.
                val checked = properties.bind(checkedProperty) ?: false
                // Null if the agent sent a literal or a function call instead of a path: the user
                // cannot change a value that cannot be stored, so the toggle becomes read-only.
                val setChecked = properties.bindUpdater(checkedProperty)
                Row(
                    modifier.toggleable(
                        value = checked,
                        enabled = setChecked != null,
                        onValueChange = { setChecked?.invoke(it) },
                    )
                ) {
                    BasicText(if (checked) "On" else "Off", Modifier.padding(end = 8.dp))
                    BasicText(label)
                }
            }
        }

    A2uiCatalog(catalogId = "https://example.com/catalog/v1", components = listOf(toggle))
}

@Sampled
fun A2uiProvideActionInterceptorSample() {
    val headerProperty = A2uiProperty.componentId("header", required = true)
    val contentProperty = A2uiProperty.componentId("content", required = true)

    val expander =
        object : A2uiComponent {
            override val name = "Expander"
            override val description =
                "Shows its content below its header while expanded. A \"toggle\" event from the " +
                    "header expands or collapses it."
            override val properties = listOf(headerProperty, contentProperty)

            @Composable
            override fun A2uiComponentScope.Content(
                properties: A2uiComponentProperties,
                modifier: Modifier,
            ) {
                val headerId = properties[headerProperty] ?: return
                val contentId = properties[contentProperty] ?: return
                var expanded by remember { mutableStateOf(false) }
                Column(modifier) {
                    // Actions dispatched by the header, or by any component nested in it, reach
                    // this interceptor first. Consuming the "toggle" event keeps it local to this
                    // component; other actions go on to outer interceptors and then to the agent.
                    ProvideActionInterceptor(
                        onIntercept = { payload ->
                            val event = payload["event"] as? Map<*, *>
                            if (event?.get("name") == "toggle") {
                                expanded = !expanded
                                true
                            } else {
                                false
                            }
                        }
                    ) {
                        Child(headerId)
                    }
                    if (expanded) Child(contentId)
                }
            }

            @Composable
            private fun A2uiComponentScope.Child(id: String) {
                when (val state = observeA2uiComponentState(id)) {
                    is A2uiComponentState.Loading -> BasicText("…")
                    is A2uiComponentState.Error -> BasicText("Could not render $id")
                    is A2uiComponentState.Success -> A2uiComponent(state.component)
                }
            }
        }

    A2uiCatalog(catalogId = "https://example.com/catalog/v1", components = listOf(expander))
}
