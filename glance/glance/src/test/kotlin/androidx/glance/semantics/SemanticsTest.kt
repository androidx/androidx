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

package androidx.glance.semantics

import androidx.compose.ui.unit.dp
import androidx.glance.GlanceModifier
import androidx.glance.findModifier
import androidx.glance.layout.size
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class SemanticsTest {
    @Test
    fun contentDescription() {
        val modifiers = GlanceModifier.semantics { contentDescription = "test_description" }

        val semanticsModifier = checkNotNull(modifiers.findModifier<SemanticsModifier>())
        assertThat(
            semanticsModifier.configuration.getOrNull(SemanticsProperties.ContentDescription)
                ?.joinToString()
        ).isEqualTo("test_description")
    }

    @Test
    fun noContentDescription() {
        val modifiers = GlanceModifier.semantics { testTag = "test" }

        val semanticsModifier = checkNotNull(modifiers.findModifier<SemanticsModifier>())
        assertThat(
            semanticsModifier.configuration.getOrNull(SemanticsProperties.ContentDescription)
        ).isNull()
    }

    @Test
    fun testTag() {
        val modifiers = GlanceModifier.semantics { testTag = "test_tag" }

        val semanticsModifier = checkNotNull(modifiers.findModifier<SemanticsModifier>())
        assertThat(
            semanticsModifier.configuration.getOrNull(SemanticsProperties.TestTag)
        ).isEqualTo("test_tag")
    }

    @Test
    fun noTestTag() {
        val modifiers = GlanceModifier.semantics { contentDescription = "desc" }

        val semanticsModifier = checkNotNull(modifiers.findModifier<SemanticsModifier>())
        assertThat(
            semanticsModifier.configuration.getOrNull(SemanticsProperties.TestTag)
        ).isNull()
    }

    @Test
    fun noSemantics() {
        val modifiers = GlanceModifier.size(10.dp)

        assertThat(modifiers.findModifier<SemanticsModifier>()).isNull()
    }
}
