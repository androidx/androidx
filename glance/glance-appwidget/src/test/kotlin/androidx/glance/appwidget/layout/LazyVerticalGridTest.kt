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

package androidx.glance.appwidget.layout

import android.annotation.TargetApi
import androidx.glance.appwidget.lazy.EmittableLazyVerticalGrid
import androidx.glance.appwidget.lazy.EmittableLazyVerticalGridListItem
import androidx.glance.appwidget.lazy.LazyVerticalGrid
import androidx.glance.appwidget.lazy.GridCells
import androidx.glance.appwidget.lazy.ReservedItemIdRangeEnd
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.lazy.itemsIndexed
import androidx.glance.appwidget.runTestingComposition
import androidx.glance.layout.EmittableRow
import androidx.glance.layout.Row
import androidx.glance.text.EmittableText
import androidx.glance.text.Text
import androidx.compose.ui.unit.dp
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import kotlin.test.assertIs

@OptIn(ExperimentalCoroutinesApi::class)
class LazyVerticalGridTest {
    private lateinit var fakeCoroutineScope: TestScope

    @Before
    fun setUp() {
        fakeCoroutineScope = TestScope()
    }

    @Test
    @TargetApi(31)
    fun emptyLazyVerticalGrid_addsLazyVerticalGridToTree() = fakeCoroutineScope.runTest {
        val root = runTestingComposition {
            LazyVerticalGrid(gridCells = GridCells.Adaptive(minSize = 100.dp)) { }
        }

        val verticalGrid = assertIs<EmittableLazyVerticalGrid>(root.children.single())
        assertThat(verticalGrid.children).isEmpty()
    }

    @Test
    @TargetApi(31)
    fun items_createsListItemsEachWithChild() = fakeCoroutineScope.runTest {
        val root = runTestingComposition {
            LazyVerticalGrid(gridCells = GridCells.Adaptive(minSize = 100.dp)) {
                items(2, { it * 2L }) { index -> Text("Item $index") }
                item(4L) { Text("Item 2") }
                item(6L) { Text("Item 3") }
            }
        }

        val verticalGrid = assertIs<EmittableLazyVerticalGrid>(root.children.single())
        val listItems = assertAre<EmittableLazyVerticalGridListItem>(verticalGrid.children)
        assertThat(listItems).hasSize(4)
        assertThat(verticalGrid.getTextAtChild(0)).isEqualTo("Item 0")
        assertThat(verticalGrid.getTextAtChild(1)).isEqualTo("Item 1")
        assertThat(verticalGrid.getTextAtChild(2)).isEqualTo("Item 2")
        assertThat(verticalGrid.getTextAtChild(3)).isEqualTo("Item 3")
    }

    @Test
    @TargetApi(31)
    fun item_multipleChildren_createsListItemWithChildren() = fakeCoroutineScope.runTest {
        val root = runTestingComposition {
            LazyVerticalGrid(gridCells = GridCells.Adaptive(minSize = 100.dp)) {
                item {
                    Text("First")
                    Row { Text("Second") }
                }
            }
        }

        val verticalGrid = assertIs<EmittableLazyVerticalGrid>(root.children.single())
        val listItem = assertIs<EmittableLazyVerticalGridListItem>(verticalGrid.children.single())
        assertThat(listItem.children).hasSize(2)
        assertIs<EmittableText>(listItem.children[0])
        assertIs<EmittableRow>(listItem.children[1])
    }

    @Test
    @TargetApi(31)
    fun items_withItemId_addsChildrenWithIds() = fakeCoroutineScope.runTest {
        val root = runTestingComposition {
            LazyVerticalGrid(gridCells = GridCells.Adaptive(minSize = 100.dp)) {
                items(2, { it * 2L }) { index -> Text("Item $index") }
                item(4L) { Text("Item 2") }
                item(6L) { Text("Item 3") }
            }
        }

        val verticalGrid = assertIs<EmittableLazyVerticalGrid>(root.children.single())
        val listItems = assertAre<EmittableLazyVerticalGridListItem>(verticalGrid.children)
        assertThat(listItems[0].itemId).isEqualTo(0L)
        assertThat(listItems[1].itemId).isEqualTo(2L)
        assertThat(listItems[2].itemId).isEqualTo(4L)
        assertThat(listItems[3].itemId).isEqualTo(6L)
    }

    @Test
    @TargetApi(31)
    fun items_withoutItemId_addsItemsWithConsecutiveReservedIds() =
        fakeCoroutineScope.runTest {
            val root = runTestingComposition {
                LazyVerticalGrid(gridCells = GridCells.Adaptive(minSize = 100.dp)) {
                    items(2) { index -> Text("Item $index") }
                    item { Text("Item 2") }
                }
            }

            val verticalGrid = assertIs<EmittableLazyVerticalGrid>(root.children.single())
            val listItems = assertAre<EmittableLazyVerticalGridListItem>(verticalGrid.children)
            assertThat(listItems[0].itemId).isEqualTo(ReservedItemIdRangeEnd)
            assertThat(listItems[1].itemId).isEqualTo(ReservedItemIdRangeEnd - 1)
            assertThat(listItems[2].itemId).isEqualTo(ReservedItemIdRangeEnd - 2)
        }

    @Test
    @TargetApi(31)
    fun items_someWithItemIds_addsChildrenWithIds() = fakeCoroutineScope.runTest {
        val root = runTestingComposition {
            LazyVerticalGrid(gridCells = GridCells.Adaptive(minSize = 100.dp)) {
                items(1, { 5L }) { Text("Item 0") }
                item { Text("Item 1") }
                items(1) { Text("Item 2") }
                item(6L) { Text("Item 3") }
            }
        }

        val verticalGrid = assertIs<EmittableLazyVerticalGrid>(root.children.single())
        val listItems = assertAre<EmittableLazyVerticalGridListItem>(verticalGrid.children)
        assertThat(listItems[0].itemId).isEqualTo(5L)
        assertThat(listItems[1].itemId).isEqualTo(ReservedItemIdRangeEnd)
        assertThat(listItems[2].itemId).isEqualTo(ReservedItemIdRangeEnd - 1)
        assertThat(listItems[3].itemId).isEqualTo(6L)
    }

    @Test
    @TargetApi(31)
    fun items_listItemsWithoutItemIds_addsChildren() = fakeCoroutineScope.runTest {
        val root = runTestingComposition {
            LazyVerticalGrid(gridCells = GridCells.Adaptive(minSize = 100.dp)) {
                items(people) { person ->
                    Text(person.name)
                }
            }
        }

        val verticalGrid = assertIs<EmittableLazyVerticalGrid>(root.children.single())
        val listItems = assertAre<EmittableLazyVerticalGridListItem>(verticalGrid.children)
        assertThat(listItems[0].itemId).isEqualTo(ReservedItemIdRangeEnd)
        assertThat(listItems[1].itemId).isEqualTo(ReservedItemIdRangeEnd - 1)
        assertThat(verticalGrid.getTextAtChild(0)).isEqualTo("Alice")
        assertThat(verticalGrid.getTextAtChild(1)).isEqualTo("Bob")
    }

    @Test
    @TargetApi(31)
    fun items_listItemsWithItemIds_addsChildren() = fakeCoroutineScope.runTest {
        val root = runTestingComposition {
            LazyVerticalGrid(gridCells = GridCells.Adaptive(minSize = 100.dp)) {
                items(people, itemId = { person -> person.userId }) { person ->
                    Text(person.name)
                }
            }
        }

        val verticalGrid = assertIs<EmittableLazyVerticalGrid>(root.children.single())
        val listItems = assertAre<EmittableLazyVerticalGridListItem>(verticalGrid.children)
        assertThat(listItems[0].itemId).isEqualTo(101)
        assertThat(listItems[1].itemId).isEqualTo(202)
        assertThat(verticalGrid.getTextAtChild(0)).isEqualTo("Alice")
        assertThat(verticalGrid.getTextAtChild(1)).isEqualTo("Bob")
    }

    @Test
    @TargetApi(31)
    fun itemsIndexed_listItems_addsChildren() = fakeCoroutineScope.runTest {
        val root = runTestingComposition {
            LazyVerticalGrid(gridCells = GridCells.Adaptive(minSize = 100.dp)) {
                itemsIndexed(people) { index, person ->
                    Text("${index + 1} - ${person.name}")
                }
            }
        }

        val verticalGrid = assertIs<EmittableLazyVerticalGrid>(root.children.single())
        assertThat(verticalGrid.getTextAtChild(0)).isEqualTo("1 - Alice")
        assertThat(verticalGrid.getTextAtChild(1)).isEqualTo("2 - Bob")
    }

    @Test
    @TargetApi(31)
    fun items_arrayItemsWithoutItemIds_addsChildren() = fakeCoroutineScope.runTest {
        val root = runTestingComposition {
            LazyVerticalGrid(gridCells = GridCells.Adaptive(minSize = 100.dp)) {
                items(people.toTypedArray()) { person ->
                    Text(person.name)
                }
            }
        }

        val verticalGrid = assertIs<EmittableLazyVerticalGrid>(root.children.single())
        val listItems = assertAre<EmittableLazyVerticalGridListItem>(verticalGrid.children)
        assertThat(listItems[0].itemId).isEqualTo(ReservedItemIdRangeEnd)
        assertThat(listItems[1].itemId).isEqualTo(ReservedItemIdRangeEnd - 1)
        assertThat(verticalGrid.getTextAtChild(0)).isEqualTo("Alice")
        assertThat(verticalGrid.getTextAtChild(1)).isEqualTo("Bob")
    }

    @Test
    @TargetApi(31)
    fun items_arrayItemsWithItemIds_addsChildren() = fakeCoroutineScope.runTest {
        val root = runTestingComposition {
            LazyVerticalGrid(gridCells = GridCells.Adaptive(minSize = 100.dp)) {
                items(people.toTypedArray(), itemId = { person -> person.userId }) { person ->
                    Text(person.name)
                }
            }
        }

        val verticalGrid = assertIs<EmittableLazyVerticalGrid>(root.children.single())
        val listItems = assertAre<EmittableLazyVerticalGridListItem>(verticalGrid.children)
        assertThat(listItems[0].itemId).isEqualTo(101)
        assertThat(listItems[1].itemId).isEqualTo(202)
        assertThat(verticalGrid.getTextAtChild(0)).isEqualTo("Alice")
        assertThat(verticalGrid.getTextAtChild(1)).isEqualTo("Bob")
    }

    @Test
    @TargetApi(31)
    fun itemsIndexed_arrayItems_addsChildren() = fakeCoroutineScope.runTest {
        val root = runTestingComposition {
            LazyVerticalGrid(gridCells = GridCells.Adaptive(minSize = 100.dp)) {
                itemsIndexed(people.toTypedArray()) { index, person ->
                    Text("${index + 1} - ${person.name}")
                }
            }
        }

        val verticalGrid = assertIs<EmittableLazyVerticalGrid>(root.children.single())
        assertThat(verticalGrid.getTextAtChild(0)).isEqualTo("1 - Alice")
        assertThat(verticalGrid.getTextAtChild(1)).isEqualTo("2 - Bob")
    }

    private fun EmittableLazyVerticalGrid.getTextAtChild(index: Int): String =
        assertIs<EmittableText>((children[index] as
            EmittableLazyVerticalGridListItem).children.first()).text

    private companion object {
        data class Person(val name: String, val userId: Long)
        val people = listOf(Person("Alice", userId = 101), Person("Bob", userId = 202))
    }
}

private inline fun <reified T> assertAre(items: Iterable<Any>) =
    items.map { assertIs<T>(it) }.toList()
