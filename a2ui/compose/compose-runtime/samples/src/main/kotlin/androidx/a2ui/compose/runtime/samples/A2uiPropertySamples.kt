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
import androidx.a2ui.compose.runtime.A2uiProperty
import androidx.a2ui.compose.ui.A2uiCatalog
import androidx.a2ui.compose.ui.A2uiComponent
import androidx.annotation.Sampled
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Sampled
fun A2uiPropertyAnySample() {
    // A static property is read as sent: {"options": {"unit": "km"}} resolves to that Map, and
    // {"options": {"path": "/prefs"}} resolves to the Map {"path": "/prefs"}, not to the data at
    // /prefs.
    val optionsProperty =
        A2uiProperty.any("options", description = "Display options, passed through as sent.")
    // A dynamic property is evaluated: {"value": 42}, {"value": {"path": "/trip/distance"}} and
    // {"value": {"call": "length", ...}} all resolve to the value they denote, which keeps its
    // type.
    val valueProperty =
        A2uiProperty.dynamicAny("value", required = true, description = "The value to display.")

    val metric =
        object : A2uiComponent {
            override val name = "Metric"
            override val description = "Displays a value of any type, followed by an optional unit."
            override val properties = listOf(valueProperty, optionsProperty)

            @Composable
            override fun A2uiComponentScope.isReady(properties: A2uiComponentProperties): Boolean =
                properties.bind(valueProperty) != null

            @Composable
            override fun A2uiComponentScope.Content(
                properties: A2uiComponentProperties,
                modifier: Modifier,
            ) {
                val value: Any = properties.bind(valueProperty) ?: return
                val options = properties[optionsProperty] as? Map<*, *>
                // The component narrows the value's type as far as it needs to.
                val text =
                    when (value) {
                        is List<*> -> value.joinToString()
                        else -> value.toString()
                    }
                val unit = options?.get("unit") as? String
                BasicText(if (unit == null) text else "$text $unit", modifier)
            }
        }

    A2uiCatalog(catalogId = "https://example.com/catalog/v1", components = listOf(metric))
}

@Sampled
fun A2uiPropertyNestedObjectSample() {
    // The fields of the nested object are properties in their own right, static or dynamic.
    val nameProperty = A2uiProperty.dynamicString("name", required = true)
    val initialsProperty = A2uiProperty.string("initials")
    // The agent sends the object inline in the component's payload:
    // {"user": {"name": {"path": "/user/name"}, "initials": "AB"}}
    val userProperty =
        A2uiProperty.nestedObject(
            "user",
            properties = listOf(nameProperty, initialsProperty),
            required = true,
        )

    val avatar =
        object : A2uiComponent {
            override val name = "Avatar"
            override val description = "Shows a user's initials next to their name."
            override val properties = listOf(userProperty)

            @Composable
            override fun A2uiComponentScope.isReady(properties: A2uiComponentProperties): Boolean {
                val user = properties[userProperty] ?: return false
                return user.bind(nameProperty) != null
            }

            @Composable
            override fun A2uiComponentScope.Content(
                properties: A2uiComponentProperties,
                modifier: Modifier,
            ) {
                // The nested object is read like the component's own properties: static fields with
                // get(), dynamic fields with bind(), evaluated in this component's data context.
                val user = properties[userProperty] ?: return
                val userName = user.bind(nameProperty) ?: return
                val initials = user[initialsProperty] ?: userName.take(2)
                Row(modifier) {
                    BasicText(initials, Modifier.padding(end = 8.dp))
                    BasicText(userName)
                }
            }
        }

    A2uiCatalog(catalogId = "https://example.com/catalog/v1", components = listOf(avatar))
}

@Sampled
fun A2uiPropertyNestedObjectListSample() {
    val labelProperty = A2uiProperty.string("label", required = true)
    val valueProperty = A2uiProperty.dynamicString("value", required = true)
    // The agent sends an array of objects with these fields:
    // {"details": [{"label": "Price", "value": {"path": "/product/price"}}, {"label": ...}]}
    val detailsProperty =
        A2uiProperty.nestedObjectList(
            "details",
            properties = listOf(labelProperty, valueProperty),
            required = true,
            minItems = 1,
        )

    val details =
        object : A2uiComponent {
            override val name = "Details"
            override val description = "Lists labeled values, one per line."
            override val properties = listOf(detailsProperty)

            @Composable
            override fun A2uiComponentScope.isReady(properties: A2uiComponentProperties): Boolean {
                val details = properties[detailsProperty] ?: return false
                return details.all { detail -> detail.bind(valueProperty) != null }
            }

            @Composable
            override fun A2uiComponentScope.Content(
                properties: A2uiComponentProperties,
                modifier: Modifier,
            ) {
                // One A2uiComponentProperties per item, read like the component's own properties.
                val details = properties[detailsProperty] ?: return
                Column(modifier) {
                    for (detail in details) {
                        val label = detail[labelProperty] ?: continue
                        val value = detail.bind(valueProperty) ?: continue
                        BasicText("$label: $value")
                    }
                }
            }
        }

    A2uiCatalog(catalogId = "https://example.com/catalog/v1", components = listOf(details))
}
