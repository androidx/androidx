/*
 * Copyright (C) 2025 The Android Open Source Project
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

package com.android.compose.modifiers

import androidx.compose.ui.Modifier
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.ObserverModifierNode
import androidx.compose.ui.node.SemanticsModifierNode
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.node.invalidateSemantics
import androidx.compose.ui.node.observeReads
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.compose.ui.semantics.stateDescription
import java.text.NumberFormat
import java.util.Locale

/**
 * Replaces the default state description in a slider to read out the percentage. The value returned
 * by the lambda should be in [0, 1] to correspond to a percentage in the slider.
 *
 * The modifier will merge the semantics with that of its descendants
 */
fun Modifier.sliderPercentage(percentage: () -> Float) =
    this then SliderPercentageElement(percentage)

private data class SliderPercentageElement(val percentage: () -> Float) :
    ModifierNodeElement<SliderPercentageNode>() {
    override fun create(): SliderPercentageNode {
        return SliderPercentageNode(percentage)
    }

    override fun update(node: SliderPercentageNode) {
        node.percentage = percentage
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "sliderPercentage"
        properties["percentage"] = percentage()
    }
}

private class SliderPercentageNode(var percentage: () -> Float) :
    Modifier.Node(),
    SemanticsModifierNode,
    CompositionLocalConsumerModifierNode,
    ObserverModifierNode {

    override val shouldMergeDescendantSemantics: Boolean
        get() = true

    private var locale: Locale? = null

    override fun SemanticsPropertyReceiver.applySemantics() {
        val percentInstance =
            locale?.let { NumberFormat.getPercentInstance(it) } ?: NumberFormat.getPercentInstance()
        this.stateDescription = percentInstance.format(percentage())
    }

    override fun onAttach() {
        onObservedReadsChanged()
    }

    override fun onDetach() {
        locale = null
    }

    override fun onObservedReadsChanged() {
        observeReads {
            val oldLocale = locale
            locale = currentValueOf(LocalConfiguration).locales.get(0)
            if (locale != oldLocale) {
                invalidateSemantics()
            }
        }
    }
}
