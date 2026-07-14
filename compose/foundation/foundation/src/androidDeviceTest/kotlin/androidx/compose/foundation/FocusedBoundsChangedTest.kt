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

package androidx.compose.foundation

import androidx.compose.ui.Modifier
import androidx.compose.ui.node.ModifierNodeElement
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
class FocusedBoundsChangedTest {

    @Test
    fun onFocusedBoundsChanged_noOps() {
        val expected = EmptyTestModifier
        @Suppress("DEPRECATION_ERROR") val actual = EmptyTestModifier.onFocusedBoundsChanged {}
        assertThat(actual).isEqualTo(expected)
    }

    private object EmptyTestModifier : ModifierNodeElement<EmptyTestModifierNode>() {
        override fun create(): EmptyTestModifierNode = EmptyTestModifierNode()

        override fun update(node: EmptyTestModifierNode) {}

        override fun hashCode() = -1

        override fun equals(other: Any?) = other === this
    }

    private class EmptyTestModifierNode : Modifier.Node()
}
