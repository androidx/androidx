/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.compose.foundation.text

import androidx.compose.foundation.layout.Column
import androidx.compose.ui.layout.GraphicLayerInfo
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.text.AnnotatedString
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class BasicTextGraphicsLayerTest {

    @get:Rule val rule = createComposeRule()

    @Test
    fun modifiersDoNotExposeGraphicsLayer() {
        // Something that wraps the `BasicText` is required to distinguish the root graphicsLayer.
        rule.setContent { Column { BasicText("Ok") } }
        val owners = rule.onNodeWithText("Ok").fetchGraphicsLayerOwnerViewId()
        assertThat(owners).hasSize(0)
    }

    @Test
    fun modifiersDoNotExposeGraphicsLayer_annotatedString() {
        // Something that wraps the `BasicText` is required to distinguish the root graphicsLayer.
        rule.setContent { Column { BasicText(AnnotatedString("Ok")) } }
        val owners = rule.onNodeWithText("Ok").fetchGraphicsLayerOwnerViewId()
        assertThat(owners).hasSize(0)
    }
}

private fun SemanticsNodeInteraction.fetchGraphicsLayerOwnerViewId(): List<Long> =
    fetchSemanticsNode()
        .layoutInfo
        .getModifierInfo()
        .map { it.extra }
        .filterIsInstance<GraphicLayerInfo>()
        .map { it.ownerViewId }
        .distinct()
