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

package androidx.compose.foundation.text.contextmenu.builder

import android.content.res.Resources
import androidx.compose.foundation.text.contextmenu.data.TextContextMenuComponent
import androidx.compose.foundation.text.contextmenu.data.TextContextMenuData
import androidx.compose.foundation.text.contextmenu.data.TextContextMenuItem
import androidx.compose.foundation.text.contextmenu.data.TextContextMenuSeparator
import androidx.compose.foundation.text.contextmenu.data.TextContextMenuSession
import com.google.common.truth.Correspondence
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class TextContextMenuBuilderScopeTest {
    @Test
    fun whenNoFunctionsCalled_emptyDataReturned() {
        val actualData = createAndRunScope { /* Nothing */ }

        actualData.assertHasComponents(
            // None
        )
    }

    @Test
    fun whenOnlySeparators_emptyDataReturned() {
        val actualData = createAndRunScope { repeat(3) { separator() } }

        actualData.assertHasComponents(
            // None
        )
    }

    @Test
    fun whenItem_itemReturnedAndDrawableAndOnClickMatches() {
        var clicked = false
        val expectedInt = 14
        val actualData = createAndRunScope {
            intItem(0, leadingIcon = expectedInt) { clicked = true }
        }

        actualData.assertHasComponents(Component.Item(0))
        val item = actualData.components.single() as TextContextMenuItem

        assertThat(clicked).isFalse()
        item.onClick(FakeSession)
        assertThat(clicked).isTrue()

        assertThat(item.leadingIcon).isEqualTo(expectedInt)
    }

    @Test
    fun whenItemsWithSeparatorsBetween_separatorsCombined() {
        val actualData = createAndRunScope {
            intItem(0)
            separator()
            separator()
            intItem(1)
        }

        actualData.assertHasComponents(Component.Item(0), Component.Separator, Component.Item(1))
    }

    @Test
    fun whenHeadingSeparator_separatorRemoved() {
        val actualData = createAndRunScope {
            separator()
            intItem(0)
        }

        actualData.assertHasComponents(Component.Item(0))
    }

    @Test
    fun whenTrailingSeparator_separatorRemoved() {
        val actualData = createAndRunScope {
            intItem(0)
            separator()
        }

        actualData.assertHasComponents(Component.Item(0))
    }

    @Test
    fun whenOnlyItems_itemsReturned() {
        val actualData = createAndRunScope { repeat(3) { intItem(it) } }
        actualData.assertHasComponents(Component.Item(0), Component.Item(1), Component.Item(2))
    }

    @Test
    fun whenItemSurroundedBySeparators_separatorsTrimmed() {
        val actualData = createAndRunScope {
            separator()
            intItem(0)
            separator()
        }
        actualData.assertHasComponents(Component.Item(0))
    }

    @Test
    fun whenThreeGroupsWithTwoItemsEach_separatorsCombined() {
        val actualData = createAndRunScope {
            intItem(0)
            intItem(1)
            separator()
            separator()
            intItem(2)
            intItem(3)
            separator()
            separator()
            intItem(4)
            intItem(5)
        }

        actualData.assertHasComponents(
            Component.Item(0),
            Component.Item(1),
            Component.Separator,
            Component.Item(2),
            Component.Item(3),
            Component.Separator,
            Component.Item(4),
            Component.Item(5),
        )
    }

    @Test
    fun whenExcessiveSeparators_separatorsCombined() {
        val actualData = createAndRunScope {
            repeat(3) { i ->
                repeat(10) { separator() }
                intItem(i)
                repeat(10) { separator() }
            }
        }

        actualData.assertHasComponents(
            Component.Item(0),
            Component.Separator,
            Component.Item(1),
            Component.Separator,
            Component.Item(2),
        )
    }

    @Test
    fun whenFiltering_filtersWork() {
        val actualData =
            createAndRunScope(filter = { (it.key as Int) % 2 == 0 }) { repeat(4) { intItem(it) } }

        actualData.assertHasComponents(Component.Item(0), Component.Item(2))
    }

    @Test
    fun whenFiltering_separatorsStillCombinedAfterFiltersApplied() {
        val actualData =
            createAndRunScope(filter = { (it.key as Int) % 2 == 0 }) {
                repeat(4) {
                    separator()
                    intItem(it)
                    separator()
                }
            }

        actualData.assertHasComponents(Component.Item(0), Component.Separator, Component.Item(2))
    }

    @Test
    fun whenFiltering_cannotFilterSeparators() {
        val actualData =
            createAndRunScope(filter = { it !== TextContextMenuSeparator }) {
                intItem(0)
                separator()
                intItem(1)
            }

        actualData.assertHasComponents(Component.Item(0), Component.Separator, Component.Item(1))
    }

    @Test
    fun whenFiltering_filterCannotReceiveSeparator() {
        val actualData =
            createAndRunScope(
                filter = {
                    assertThat(it).isNotInstanceOf(TextContextMenuSeparator::class.java)
                    it !is TextContextMenuSeparator
                }
            ) {
                repeat(2) {
                    separator()
                    intItem(it)
                    separator()
                }
            }

        actualData.assertHasComponents(Component.Item(0), Component.Separator, Component.Item(1))
    }

    @Test
    fun whenFilteringAll_emptyResult() {
        val actualData =
            createAndRunScope(filter = { false }) {
                repeat(4) {
                    separator()
                    intItem(it)
                    separator()
                }
            }

        actualData.assertHasComponents(
            // None
        )
    }
}

private fun TextContextMenuData.assertHasComponents(vararg components: Component) {
    assertThat(this.components).isNotNull()
    assertThat(this.components)
        .comparingElementsUsing(ComponentCorrespondence)
        .containsExactlyElementsIn(components)
        .inOrder()
}

private fun createAndRunScope(
    filter: ((TextContextMenuComponent) -> Boolean)? = null,
    scope: TextContextMenuBuilderScope.() -> Unit,
): TextContextMenuData =
    TextContextMenuBuilderScope()
        .apply {
            scope()
            filter?.let(::addFilter)
        }
        .build()
        .also {
            assertThat(it).isNotNull()
            assertThat(it.components).isNotNull()
        }

private fun TextContextMenuBuilderScope.intItem(
    intValue: Int,
    leadingIcon: Int = Resources.ID_NULL,
    onClick: TextContextMenuSession.() -> Unit = {},
) {
    item(key = intValue, label = intValue.toString(), leadingIcon = leadingIcon, onClick = onClick)
}

private val FakeSession: TextContextMenuSession =
    object : TextContextMenuSession {
        override fun close() {
            // Nothing
        }
    }

private sealed interface Component {
    data object Separator : Component

    data class Item(val intValue: Int) : Component
}

private val ComponentCorrespondence =
    Correspondence.from<TextContextMenuComponent, Component>(
        /* predicate = */ { actual, expected ->
            when (expected) {
                null -> actual == null
                is Component.Separator -> actual === TextContextMenuSeparator
                is Component.Item -> actual?.key as? Int == expected.intValue
            }
        },
        /* description = */ "equals",
    )
