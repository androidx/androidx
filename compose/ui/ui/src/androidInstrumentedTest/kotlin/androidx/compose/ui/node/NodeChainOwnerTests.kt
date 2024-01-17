/*
 * Copyright 2022 The Android Open Source Project
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

@file:OptIn(ExperimentalComposeUiApi::class)
package androidx.compose.ui.node

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasurePolicy
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test

class NodeChainOwnerTests {

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun getModifierNode_returnsLayers_whenGraphicsLayerIsTail() {
        rule.setContent {
            // box gets the root graphics layer applied to it
            Box {
                // test this, with no graphicsLayers added externally
                Layout(
                    modifier = Modifier.testTag("tag").graphicsLayer(),
                    EmptyMeasurePolicy())
            }
        }

        val modifierInfo = rule.onNodeWithTag("tag")
            .fetchSemanticsNode()
            .layoutInfo
            .getModifierInfo()
        assertThat(modifierInfo.mapNotNull { it.extra }).hasSize(1)
    }

    @Test
    fun getModifierNode_returnsLayersWhenHead_noOtherLayout() {
        rule.setContent {
            // box gets the root graphics layer applied to it
            Box {
                // test this, with no graphicsLayers added externally
                Layout(
                    modifier = Modifier.graphicsLayer().semantics {}.testTag("tag"),
                    EmptyMeasurePolicy())
            }
        }

        val modifierInfo = rule.onNodeWithTag("tag")
            .fetchSemanticsNode()
            .layoutInfo
            .getModifierInfo()
        assertThat(modifierInfo.mapNotNull { it.extra }).hasSize(2)
    }

    @Test
    fun getModifierNode_returnsLayersWhenHead_whenTailLayoutLayout() {
        rule.setContent {
            // box gets the root graphics layer applied to it
            Box {
                // test this, with no graphicsLayers added externally
                Layout(
                    modifier = Modifier.graphicsLayer().size(30.dp).testTag("tag"),
                    EmptyMeasurePolicy())
            }
        }

        val modifierInfo = rule.onNodeWithTag("tag")
            .fetchSemanticsNode()
            .layoutInfo
            .getModifierInfo()
        assertThat(modifierInfo.mapNotNull { it.extra }).hasSize(1)
    }
}

class EmptyMeasurePolicy : MeasurePolicy {
    override fun MeasureScope.measure(
        measurables: List<Measurable>,
        constraints: Constraints
    ): MeasureResult {
        return layout(constraints.maxWidth, constraints.maxHeight, placementBlock = {})
    }
}
