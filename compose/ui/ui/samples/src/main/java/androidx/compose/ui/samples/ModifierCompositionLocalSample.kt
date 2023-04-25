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

package androidx.compose.ui.samples

import androidx.annotation.Sampled
import androidx.compose.foundation.layout.Box
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.platform.InspectorInfo

@Sampled
@Composable
fun CompositionLocalConsumingModifierSample() {
    val LocalBackgroundColor = compositionLocalOf { Color.White }
    class BackgroundColor : Modifier.Node(), DrawModifierNode,
        CompositionLocalConsumerModifierNode {
        override fun ContentDrawScope.draw() {
            val backgroundColor = currentValueOf(LocalBackgroundColor)
            drawRect(backgroundColor)
            drawContent()
        }
    }
    val BackgroundColorModifierElement = object : ModifierNodeElement<BackgroundColor>() {
        override fun create() = BackgroundColor()
        override fun update(node: BackgroundColor) {}
        override fun hashCode() = System.identityHashCode(this)
        override fun equals(other: Any?) = (other === this)
        override fun InspectorInfo.inspectableProperties() {
            name = "backgroundColor"
        }
    }
    fun Modifier.backgroundColor() = this then BackgroundColorModifierElement
    Box(Modifier.backgroundColor()) {
        Text("Hello, world!")
    }
}