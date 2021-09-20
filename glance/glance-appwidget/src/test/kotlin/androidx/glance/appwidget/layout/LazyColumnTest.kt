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

package androidx.glance.appwidget.layout

import androidx.glance.GlanceInternalApi
import androidx.glance.appwidget.runTestingComposition
import androidx.glance.layout.EmittableRow
import androidx.glance.layout.EmittableText
import androidx.glance.layout.Row
import androidx.glance.layout.Text
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineScope
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Before
import org.junit.Test
import kotlin.test.assertIs

@OptIn(GlanceInternalApi::class, ExperimentalCoroutinesApi::class)
class LazyColumnTest {
    private lateinit var fakeCoroutineScope: TestCoroutineScope

    @Before
    fun setUp() {
        fakeCoroutineScope = TestCoroutineScope()
    }

    @Test
    fun emptyLazyColumn_addsLazyColumnToTree() = fakeCoroutineScope.runBlockingTest {
        val root = runTestingComposition {
            LazyColumn { }
        }

        val column = assertIs<EmittableLazyColumn>(root.children.single())
        assertThat(column.children).isEmpty()
    }

    @Test
    fun items_createsListItemsEachWithChild() = fakeCoroutineScope.runBlockingTest {
        val root = runTestingComposition {
            LazyColumn {
                items(2, { it * 2L }) { index -> Text("Item $index") }
                item(4L) { Text("Item 2") }
                item(6L) { Text("Item 3") }
            }
        }

        val column = assertIs<EmittableLazyColumn>(root.children.single())
        val listItems = assertAre<EmittableLazyListItem>(column.children)
        assertThat(listItems).hasSize(4)
        val text0 = assertIs<EmittableText>(listItems[0].children.first())
        assertThat(text0.text).isEqualTo("Item 0")
        val text1 = assertIs<EmittableText>(listItems[1].children.first())
        assertThat(text1.text).isEqualTo("Item 1")
        val text2 = assertIs<EmittableText>(listItems[2].children.first())
        assertThat(text2.text).isEqualTo("Item 2")
        val text3 = assertIs<EmittableText>(listItems[3].children.first())
        assertThat(text3.text).isEqualTo("Item 3")
    }

    @Test
    fun item_multipleChildren_createsListItemWithChildren() = fakeCoroutineScope.runBlockingTest {
        val root = runTestingComposition {
            LazyColumn {
                item {
                    Text("First")
                    Row { Text("Second") }
                }
            }
        }

        val column = assertIs<EmittableLazyColumn>(root.children.single())
        val listItem = assertIs<EmittableLazyListItem>(column.children.single())
        assertThat(listItem.children).hasSize(2)
        assertIs<EmittableText>(listItem.children[0])
        assertIs<EmittableRow>(listItem.children[1])
    }

    @Test
    fun items_withItemId_addsChildrenWithIds() = fakeCoroutineScope.runBlockingTest {
        val root = runTestingComposition {
            LazyColumn {
                items(2, { it * 2L }) { index -> Text("Item $index") }
                item(4L) { Text("Item 2") }
                item(6L) { Text("Item 3") }
            }
        }

        val column = assertIs<EmittableLazyColumn>(root.children.single())
        val listItems = assertAre<EmittableLazyListItem>(column.children)
        assertThat(listItems[0].itemId).isEqualTo(0L)
        assertThat(listItems[1].itemId).isEqualTo(2L)
        assertThat(listItems[2].itemId).isEqualTo(4L)
        assertThat(listItems[3].itemId).isEqualTo(6L)
    }

    @Test
    fun items_withoutItemId_addsItemsWithConsecutiveReservedIds() =
        fakeCoroutineScope.runBlockingTest {
            val root = runTestingComposition {
                LazyColumn {
                    items(2) { index -> Text("Item $index") }
                    item { Text("Item 2") }
                }
            }

            val column = assertIs<EmittableLazyColumn>(root.children.single())
            val listItems = assertAre<EmittableLazyListItem>(column.children)
            assertThat(listItems[0].itemId).isEqualTo(ReservedItemIdRangeEnd)
            assertThat(listItems[1].itemId).isEqualTo(ReservedItemIdRangeEnd - 1)
            assertThat(listItems[2].itemId).isEqualTo(ReservedItemIdRangeEnd - 2)
        }

    @Test
    fun items_someWithItemIds_addsChildrenWithIds() = fakeCoroutineScope.runBlockingTest {
        val root = runTestingComposition {
            LazyColumn {
                items(1, { 5L }) { Text("Item 0") }
                item { Text("Item 1") }
                items(1) { Text("Item 2") }
                item(6L) { Text("Item 3") }
            }
        }

        val column = assertIs<EmittableLazyColumn>(root.children.single())
        val listItems = assertAre<EmittableLazyListItem>(column.children)
        assertThat(listItems[0].itemId).isEqualTo(5L)
        assertThat(listItems[1].itemId).isEqualTo(ReservedItemIdRangeEnd)
        assertThat(listItems[2].itemId).isEqualTo(ReservedItemIdRangeEnd - 1)
        assertThat(listItems[3].itemId).isEqualTo(6L)
    }
}

private inline fun <reified T> assertAre(items: Iterable<Any>) =
    items.map { assertIs<T>(it) }.toList()
