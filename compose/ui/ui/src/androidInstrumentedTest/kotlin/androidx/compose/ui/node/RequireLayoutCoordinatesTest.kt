/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.compose.ui.node

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toOffset
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
class RequireLayoutCoordinatesTest {

    @get:Rule val rule = createComposeRule()

    @Test
    fun requireLayoutCoordinates_throws_whenNotAttached() {
        lateinit var modifier: TestModifierNode
        rule.setContent { Box(Modifier.then(TestModifier { modifier = it }).size(1.dp)) }

        rule.runOnIdle {
            assertIs<IllegalStateException>(modifier.coordinatesFromInit.exceptionOrNull())
        }
    }

    @Test
    fun requireLayoutCoordinates_returnsCoordinates_whenAttached() {
        lateinit var modifier: TestModifierNode
        rule.setContent {
            Box(
                Modifier.offset(10.dp, 20.dp)
                    .requiredSize(30.dp, 40.dp)
                    .then(TestModifier { modifier = it })
            )
        }

        rule.runOnIdle {
            val coordinates = assertNotNull(modifier.requireLayoutCoordinates())
            assertThat(coordinates.isAttached).isTrue()

            with(rule.density) {
                assertThat(coordinates.positionInRoot())
                    .isEqualTo(IntOffset(10.dp.roundToPx(), 20.dp.roundToPx()).toOffset())
                assertThat(coordinates.size)
                    .isEqualTo(IntSize(30.dp.roundToPx(), 40.dp.roundToPx()))
            }
        }
    }

    @Test
    fun requireLayoutCoordinates_returnsCoordinatesFromNearestLayoutModifier() {
        lateinit var modifier: TestModifierNode
        rule.setContent {
            Layout(
                Modifier.requiredSize(10.dp).then(TestModifier { modifier = it }).requiredSize(5.dp)
            ) { _, _ ->
                layout(2.dp.roundToPx(), 2.dp.roundToPx()) {}
            }
        }

        rule.runOnIdle {
            val coordinates = assertNotNull(modifier.requireLayoutCoordinates())

            with(rule.density) {
                assertThat(coordinates.size).isEqualTo(IntSize(5.dp.roundToPx(), 5.dp.roundToPx()))
            }
        }
    }

    @Test
    fun requireLayoutCoordinates_throws_afterDetached() {
        var attachModifier by mutableStateOf(true)
        lateinit var modifier: TestModifierNode
        rule.setContent {
            Box(
                Modifier.then(if (attachModifier) TestModifier { modifier = it } else Modifier)
                    .size(1.dp)
            )
        }

        rule.waitUntil("requireLayoutCoordinates returns") {
            runCatching { modifier.requireLayoutCoordinates() }.isSuccess
        }
        attachModifier = false

        rule.runOnIdle {
            assertFailsWith<IllegalStateException> { modifier.requireLayoutCoordinates() }
        }
    }

    private data class TestModifier(val onModifier: (TestModifierNode) -> Unit) :
        ModifierNodeElement<TestModifierNode>() {
        override fun create(): TestModifierNode = TestModifierNode().also(onModifier)

        override fun update(node: TestModifierNode) {}
    }

    private class TestModifierNode : Modifier.Node() {
        val coordinatesFromInit = runCatching { requireLayoutCoordinates() }
    }
}
