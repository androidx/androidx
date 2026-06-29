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

package androidx.compose.runtime.a2ui

import androidx.a2ui.model.protocol.A2uiDataPath
import androidx.a2ui.model.protocol.A2uiException.A2uiRuntimeException
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import kotlin.test.assertFailsWith
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class A2uiDataModelTest {

    private val dataModel = A2uiDataModel()

    @Test
    fun update_root_setsRoot() {
        dataModel.update(path("/"), mapOf("user" to mapOf("name" to "Alice", "age" to 30)))

        assertThat(dataModel[path("/")]).isInstanceOf(SnapshotStateMap::class.java)
        assertThat(dataModel[path("/user/name")]).isEqualTo("Alice")
        assertThat(dataModel[path("/user/age")]).isEqualTo(30)
    }

    @Test
    fun update_root_emptyPathString_setsRoot() {
        dataModel.update(path(""), mapOf("a" to 1))

        val root = dataModel[path("")] as Map<*, *>
        assertThat(root["a"]).isEqualTo(1)
    }

    @Test
    fun update_root_withEmptyMap_clearsRoot() {
        dataModel.update(path("/"), mapOf("user" to "Alice", "theme" to "dark"))
        assertThat(dataModel[path("/user")]).isEqualTo("Alice")

        dataModel.update(path("/"), emptyMap<String, Any>())

        val root = dataModel[path("/")] as Map<*, *>
        assertThat(root.isEmpty()).isTrue()
        assertThat(dataModel[path("/user")]).isNull()
    }

    @Test
    fun update_root_withNull_clearsRoot() {
        dataModel.update(path("/"), mapOf("user" to "Alice", "theme" to "dark"))
        assertThat(dataModel[path("/user")]).isEqualTo("Alice")

        dataModel.update(path("/"), null)

        assertThat(dataModel[path("/")]).isNull()
        assertThat(dataModel[path("/user")]).isNull()
    }

    @Test
    fun update_root_withPrimitive_throwsException() {
        val e =
            assertFailsWith<A2uiRuntimeException> {
                dataModel.update(path("/"), "primitive string")
            }

        assertThat(e).hasMessageThat().contains("must be a Map/Object or null")
        assertThat(e.context["path"]).isEqualTo("/")
    }

    @Test
    fun update_root_withList_throwsException() {
        val e =
            assertFailsWith<A2uiRuntimeException> {
                dataModel.update(path("/"), listOf("invalid", "root"))
            }

        assertThat(e).hasMessageThat().contains("must be a Map/Object or null")
        assertThat(e.context["path"]).isEqualTo("/")
    }

    @Test
    fun update_root_nestedPathAfterRootCleared_rehydratesRootMap() {
        dataModel.update(path("/"), mapOf("user" to "Alice", "theme" to "dark"))
        dataModel.update(path("/"), null)
        assertThat(dataModel[path("/")]).isNull()

        dataModel.update(path("/restored"), "success")

        assertThat(dataModel[path("/")]).isInstanceOf(SnapshotStateMap::class.java)
        assertThat(dataModel[path("/restored")]).isEqualTo("success")
    }

    @Test
    fun update_rfc6901EscapedPath_resolvesCorrectly() {
        // "~1" -> "/" and "~0" -> "~"
        dataModel.update(path("/user/first~1last~0name"), "Alice")
        val root = dataModel[path("/user")] as Map<*, *>

        assertThat(root["first/last~name"]).isEqualTo("Alice")
    }

    @Test
    fun update_invalidRfc6901EscapedPath_throwsExceptionWithProperlyEscapedPath() {
        // "array~1node~0" parses to the segment name "array/node~"
        dataModel.update(path("/container/array~1node~0"), listOf("A", "B"))

        val e =
            assertFailsWith<A2uiRuntimeException> {
                // Agent hallucinates a string index "bad/index~" for the existing array
                dataModel.update(path("/container/array~1node~0/bad~1index~0"), "Value")
            }

        // The exception path must correctly re-escape the parsed segments
        assertThat(e.context["path"]).isEqualTo("/container/array~1node~0/bad~1index~0")
    }

    @Test
    fun update_nestedMap_hydratesIntermediateMaps() {
        dataModel.update(path("/company/address/city"), "London")

        assertThat(dataModel[path("/company")]).isInstanceOf(SnapshotStateMap::class.java)
        assertThat(dataModel[path("/company/address")]).isInstanceOf(SnapshotStateMap::class.java)
        assertThat(dataModel[path("/company/address/city")]).isEqualTo("London")
    }

    @Test
    fun update_map_withNull_removesKey() {
        dataModel.update(path("/settings/theme"), "dark")
        assertThat(dataModel[path("/settings/theme")]).isEqualTo("dark")

        dataModel.update(path("/settings/theme"), null)

        val settings = dataModel[path("/settings")] as Map<*, *>
        // The map should still exist, but the key has been removed
        assertThat(settings.containsKey("theme")).isFalse()
        assertThat(dataModel[path("/settings/theme")]).isNull()
    }

    @Test
    fun update_map_withDashKey_insertsLiteralDash() {
        dataModel.update(path("/map"), mapOf("a" to 1))

        // "-" on a Map is just a standard string key
        dataModel.update(path("/map/-"), "Literal Dash")

        assertThat(dataModel[path("/map/-")]).isEqualTo("Literal Dash")
    }

    @Test
    fun update_map_withEmptyStringKey_resolvesCorrectly() {
        // "/user//name" parses to segments ["user", "", "name"]
        dataModel.update(path("/user//name"), "Hidden")

        val userMap = dataModel[path("/user")] as Map<*, *>
        val emptyKeyMap = userMap[""] as Map<*, *>

        assertThat(emptyKeyMap["name"]).isEqualTo("Hidden")
        assertThat(dataModel[path("/user//name")]).isEqualTo("Hidden")
    }

    @Test
    fun update_withExplicitNullInMap_preservesNullAsValue() {
        val payload = mapOf("name" to "Alice", "deletedAt" to null)

        // Replacing the root/node with an object that contains a literal null
        dataModel.update(path("/user"), payload)

        val userMap = dataModel[path("/user")] as SnapshotStateMap<*, *>

        // The key should exist in the map and point to null
        assertThat(userMap.containsKey("deletedAt")).isTrue()
        assertThat(userMap["deletedAt"]).isNull()
    }

    @Test
    fun update_existingMap_withNonExistentKeyAndNull_doesNothing() {
        dataModel.update(path("/map"), mapOf("a" to 1))

        // Agent hallucinates deleting a key that was never there
        dataModel.update(path("/map/b"), null)

        val map = dataModel[path("/map")] as Map<*, *>
        assertThat(map).hasSize(1)
        assertThat(map.containsKey("a")).isTrue()
        assertThat(map.containsKey("b")).isFalse()
    }

    @Test
    fun update_nonExistentPath_withNull_doesNotHydrate() {
        dataModel.update(path("/"), null)

        // Attempting to delete a deep path that doesn't exist
        dataModel.update(path("/missingParent/missingChild"), null)

        assertThat(dataModel[path("/")]).isNull()
    }

    @Test
    fun update_nestedListElement_hydratesList() {
        // Automatically creates a list because the segment "0" implies an array
        dataModel.update(path("/users/0/name"), "Bob")

        val usersList = dataModel[path("/users")]
        assertThat(usersList).isInstanceOf(SnapshotStateSparseList::class.java)
        assertThat(usersList as List<*>).hasSize(1)
        val bobMap = usersList[0] as Map<*, *>
        assertThat(bobMap["name"]).isEqualTo("Bob")
        assertThat(dataModel[path("/users/0/name")]).isEqualTo("Bob")
    }

    @Test
    fun update_nestedMapInsideListAppend_hydratesListAndMap() {
        // Append a new object with a "message" key to a previously non-existent list
        dataModel.update(path("/logs/-/message"), "System started")

        val logs = dataModel[path("/logs")] as List<*>
        assertThat(logs).hasSize(1)
        val firstLog = logs[0] as Map<*, *>
        assertThat(firstLog["message"]).isEqualTo("System started")
    }

    @Test
    fun update_list_withDash_appendsToList() {
        dataModel.update(path("/array/-"), "First")
        dataModel.update(path("/array/-"), "Second")

        val items = dataModel[path("/array")] as List<*>
        assertThat(items).hasSize(2)
        assertThat(items[0]).isEqualTo("First")
        assertThat(items[1]).isEqualTo("Second")
        assertThat(dataModel[path("/array/0")]).isEqualTo("First")
        assertThat(dataModel[path("/array/1")]).isEqualTo("Second")
    }

    @Test
    fun update_list_withSizeIndex_appendsToList() {
        // Appending at index == size is equivalent to "-"
        dataModel.update(path("/array/0"), "A") // hydrates list, size becomes 1

        dataModel.update(path("/array/1"), "B") // size becomes 2

        val items = dataModel[path("/array")] as List<*>
        assertThat(items).hasSize(2)
        assertThat(items[0]).isEqualTo("A")
        assertThat(items[1]).isEqualTo("B")
    }

    @Test
    fun update_list_withExistingIndex_overwritesValue() {
        dataModel.update(path("/array/-"), "Original")

        dataModel.update(path("/array/0"), "Overwritten")

        assertThat(dataModel[path("/array/0")]).isEqualTo("Overwritten")
    }

    @Test
    fun update_list_outOfBoundsIndex_withNull_doesNotExpandList() {
        dataModel.update(path("/array"), listOf("A", "B"))

        // Agent hallucinates deleting an element at an out-of-bounds index
        dataModel.update(path("/array/10"), null)

        val array = dataModel[path("/array")] as List<*>
        assertThat(array).hasSize(2)
        assertThat(dataModel[path("/array/2")]).isNull()
    }

    @Test
    fun update_list_outOfBoundsIndex_withDashAndNull_doesNotExpandList() {
        dataModel.update(path("/array"), listOf("A", "B"))

        dataModel.update(path("/array/-"), null)

        val array = dataModel[path("/array")] as List<*>
        assertThat(array).hasSize(2)
        assertThat(dataModel[path("/array/2")]).isNull()
    }

    @Test
    fun update_list_withNull_clearsValueAndPreservesLength() {
        dataModel.update(path("/array"), listOf("A", "B", "C"))

        // Spec: "For arrays, the value at the index is set to undefined, preserving length."
        dataModel.update(path("/array/1"), null)

        val array = dataModel[path("/array")] as List<*>

        // Assert length is preserved and elements are not shifted
        assertThat(array).hasSize(3)
        assertThat(array[0]).isEqualTo("A")
        assertThat(array[1]).isNull() // The gap
        assertThat(array[2]).isEqualTo("C")
        assertThat(dataModel[path("/array/0")]).isEqualTo("A")
        assertThat(dataModel[path("/array/1")]).isNull()
        assertThat(dataModel[path("/array/2")]).isEqualTo("C")
    }

    @Test
    fun update_list_withNegativeIndex_throwsException() {
        dataModel.update(path("/array/-"), "Valid")

        val e =
            assertFailsWith<A2uiRuntimeException> { dataModel.update(path("/array/-1"), "Invalid") }

        assertThat(e.message).contains("Invalid index")
        assertThat(e.context["path"]).isEqualTo("/array/-1")
    }

    @Test
    fun update_sparseList_outOfBoundsIndex_hydratesWithGap() {
        dataModel.update(path("/users/2/name"), "Alice")

        val list = dataModel[path("/users")] as List<*>
        assertThat(list.size).isEqualTo(3) // Indices 0, 1, 2
        assertThat(list[0]).isNull()
        assertThat(list[1]).isNull()
        val aliceMap = list[2] as Map<*, *>
        assertThat(aliceMap["name"]).isEqualTo("Alice")
        assertThat(dataModel[path("/users/0")]).isNull()
        assertThat(dataModel[path("/users/1")]).isNull()
        assertThat(dataModel[path("/users/2/name")]).isEqualTo("Alice")
    }

    @Test
    fun update_sparseList_existingList_outOfBoundsIndex_updatesWithGap() {
        dataModel.update(path("/list"), listOf("Index0"))

        dataModel.update(path("/list/2"), "Index2")

        val list = dataModel[path("/list")] as List<*>
        assertThat(list.size).isEqualTo(3)
        assertThat(list[0]).isEqualTo("Index0")
        assertThat(list[1]).isNull() // The gap
        assertThat(list[2]).isEqualTo("Index2")
        assertThat(dataModel[path("/list/0")]).isEqualTo("Index0")
        assertThat(dataModel[path("/list/1")]).isNull() // The gap
        assertThat(dataModel[path("/list/2")]).isEqualTo("Index2")
    }

    @Test
    fun update_sparseList_withHugeOutOfBoundsIndex_throwsException() {
        dataModel.update(path("/huge"), listOf("Index0"))

        // Attempting to update an index beyond MAX_ARRAY_SIZE (100_000)
        val e =
            assertFailsWith<A2uiRuntimeException> { dataModel.update(path("/huge/100000"), "Oops") }

        assertThat(e.message).contains("exceeds the maximum allowed limit")
        assertThat(e.context["path"]).isEqualTo("/huge/100000")
    }

    @Test
    fun update_sparseList_appendAtMaxCapacity_throwsException() {
        dataModel.update(path("/limit/99999"), "At limit")

        val e =
            assertFailsWith<A2uiRuntimeException> { dataModel.update(path("/limit/-"), "Overflow") }

        assertThat(e.message).contains("exceeds the maximum allowed limit")
        assertThat(e.context["path"]).isEqualTo("/limit/-")
    }

    @Test
    fun update_sparseList_withDash_appendsCorrectly() {
        dataModel.update(path("/sparse_append"), listOf("A"))
        dataModel.update(path("/sparse_append/2"), "C")

        // Now append using JSON pointer "-"
        dataModel.update(path("/sparse_append/-"), "D")

        val list = dataModel[path("/sparse_append")] as List<*>

        // Size should increment seamlessly to 4: [A, null, C, D]
        assertThat(list.size).isEqualTo(4)
        assertThat(list[3]).isEqualTo("D")
        assertThat(dataModel[path("/sparse_append/3")]).isEqualTo("D")
    }

    @Test
    fun update_sparseList_withNewDenseList_resetsList() {
        // Create a sparse list with size 4: [A, null, null, D]
        dataModel.update(path("/items"), listOf("A"))
        dataModel.update(path("/items/3"), "D")

        dataModel.update(path("/items"), listOf("X", "Y"))

        val list = dataModel[path("/items")] as List<*>
        assertThat(list).hasSize(2)
        assertThat(list[0]).isEqualTo("X")
        assertThat(list[1]).isEqualTo("Y")
        assertThat(dataModel[path("/items/3")]).isNull()
    }

    @Test
    fun update_sparseList_withEmptyMap_replacesEntirely() {
        dataModel.update(path("/data"), listOf("A"))
        dataModel.update(path("/data/5"), "F")

        dataModel.update(path("/data"), emptyMap<String, Any>())

        val data = dataModel[path("/data")] as Map<*, *>
        assertThat(data).isEmpty()
    }

    @Test
    fun update_primitive_withMap_replacesWithMap() {
        dataModel.update(path("/data"), "Just a string")
        assertThat(dataModel[path("/data")]).isEqualTo("Just a string")

        dataModel.update(path("/data/nested"), "Now a map")

        assertThat(dataModel[path("/data")]).isInstanceOf(SnapshotStateMap::class.java)
        assertThat(dataModel[path("/data/nested")]).isEqualTo("Now a map")
    }

    @Test
    fun update_map_withPrimitive_replacesWithPrimitive() {
        dataModel.update(path("/settings/theme"), mapOf("color" to "red"))
        assertThat(dataModel[path("/settings/theme")]).isInstanceOf(SnapshotStateMap::class.java)

        dataModel.update(path("/settings/theme"), "dark_mode")

        assertThat(dataModel[path("/settings/theme")]).isEqualTo("dark_mode")
    }

    @Test
    fun update_list_withPrimitive_replacesWithPrimitive() {
        dataModel.update(path("/array"), listOf("A", "B"))
        assertThat(dataModel[path("/array")]).isInstanceOf(SnapshotStateSparseList::class.java)

        dataModel.update(path("/array"), "Now I am a string")

        assertThat(dataModel[path("/array")]).isEqualTo("Now I am a string")
    }

    @Test
    fun update_list_withMap_replacesListWithMap() {
        dataModel.update(path("/data"), listOf("A", "B"))
        assertThat(dataModel[path("/data")]).isInstanceOf(SnapshotStateSparseList::class.java)

        dataModel.update(path("/data"), mapOf("key" to "value"))

        assertThat(dataModel[path("/data")]).isInstanceOf(SnapshotStateMap::class.java)
        assertThat(dataModel[path("/data/key")]).isEqualTo("value")
    }

    @Test
    fun update_primitiveInList_withMap_replacesWithMap() {
        dataModel.update(path("/array"), listOf("String"))

        dataModel.update(path("/array/0"), mapOf("key" to "value"))

        val item = dataModel[path("/array/0")]
        assertThat(item).isInstanceOf(SnapshotStateMap::class.java)
        assertThat((item as Map<*, *>)["key"]).isEqualTo("value")
    }

    @Test
    fun update_map_withNumericKey_retainsMap() {
        // Under RFC 6901, "0" is a valid map key
        dataModel.update(path("/container/key"), "Value")
        assertThat(dataModel[path("/container")]).isInstanceOf(SnapshotStateMap::class.java)

        // Attempting to index it with "0" should just add "0" to the Map.
        dataModel.update(path("/container/0"), "Array Value")

        assertThat(dataModel[path("/container")]).isInstanceOf(SnapshotStateMap::class.java)
        assertThat(dataModel[path("/container/0")]).isEqualTo("Array Value")
        assertThat(dataModel[path("/container/key")]).isEqualTo("Value")
    }

    @Test
    fun update_map_withNestedNumericKeyPath_retainsMap() {
        // Agent provides an object where the key happens to be a number string
        dataModel.update(path("/mixed"), mapOf("0" to "Zero", "name" to "Map"))

        // Agent attempts to write deeply through the "0" segment.
        dataModel.update(path("/mixed/0/deep"), "Replaced")

        assertThat(dataModel[path("/mixed")]).isInstanceOf(SnapshotStateMap::class.java)
        assertThat(dataModel[path("/mixed/name")]).isEqualTo("Map")
        assertThat(dataModel[path("/mixed/0/deep")]).isEqualTo("Replaced")
    }

    @Test
    fun update_list_withStringKey_throwsException() {
        dataModel.update(path("/mixed"), listOf("Zero"))

        val e =
            assertFailsWith<A2uiRuntimeException> {
                dataModel.update(path("/mixed/invalid_string"), "Replaced")
            }

        assertThat(e.message).contains("Invalid index")
        assertThat(e.context["path"]).isEqualTo("/mixed/invalid_string")
    }

    @Test
    fun update_sparseList_withStringKey_throwsException() {
        dataModel.update(path("/mixed"), listOf("Zero"))
        dataModel.update(path("/mixed/2"), "Two")

        val e =
            assertFailsWith<A2uiRuntimeException> {
                dataModel.update(path("/mixed/invalid_string"), "Replaced")
            }

        assertThat(e.message).contains("Invalid index")
        assertThat(e.context["path"]).isEqualTo("/mixed/invalid_string")
    }

    @Test
    fun update_list_withNestedStringKey_throwsException() {
        dataModel.update(path("/mixed"), listOf("Zero"))

        val e =
            assertFailsWith<A2uiRuntimeException> {
                dataModel.update(path("/mixed/invalid_string/some/deep/node"), "Replaced")
            }

        assertThat(e.message).contains("Invalid index")
        assertThat(e.context["path"]).isEqualTo("/mixed/invalid_string")
    }

    @Test
    fun update_complexNestedData_convertsToSnapshotStateDeeply() {
        val rawInput =
            mapOf(
                "string" to "hello",
                "list" to listOf(mapOf("nestedKey" to "nestedValue"), "simple_item"),
                "map" to mapOf("subList" to listOf(1, 2, 3)),
            )

        dataModel.update(path("/complex"), rawInput)

        val rootNode = dataModel[path("/complex")]
        assertThat(rootNode).isInstanceOf(SnapshotStateMap::class.java)

        val listNode = dataModel[path("/complex/list")]
        assertThat(listNode).isInstanceOf(SnapshotStateSparseList::class.java)

        val nestedMap = dataModel[path("/complex/list/0")]
        assertThat(nestedMap).isInstanceOf(SnapshotStateMap::class.java)
        assertThat(dataModel[path("/complex/list/0/nestedKey")]).isEqualTo("nestedValue")

        val subList = dataModel[path("/complex/map/subList")]
        assertThat(subList).isInstanceOf(SnapshotStateSparseList::class.java)
        assertThat(subList as List<*>).hasSize(3)
    }

    @Test
    fun update_deepDeletionThroughPrimitive_ignoresDeletionAndRetainsPrimitive() {
        dataModel.update(path("/config/theme"), "dark_mode")

        // Agent hallucinates a deletion inside a string primitive
        dataModel.update(path("/config/theme/colors/primary"), null)

        assertThat(dataModel[path("/config/theme")]).isEqualTo("dark_mode")
    }

    @Test
    fun update_concurrentWritesToDifferentKeys_preservesAllWritesWithoutConflict(): Unit =
        runBlocking {
            val numCoroutines = 100
            val jobs =
                List(numCoroutines) { index ->
                    // Dispatchers.Default forces execution onto background thread pools
                    launch(Dispatchers.Default) {
                        dataModel.update(path("/key_$index"), "value_$index")
                    }
                }
            jobs.joinAll()

            // Verify all writes succeeded and no SnapshotApplyConflictException was thrown
            val rootMap = dataModel[path("/")] as Map<*, *>
            assertThat(rootMap).hasSize(numCoroutines)
            for (i in 0 until numCoroutines) {
                assertThat(dataModel[path("/key_$i")]).isEqualTo("value_$i")
            }
        }

    @Test
    fun update_concurrentAppendsToList_preservesAllItems(): Unit = runBlocking {
        dataModel.update(path("/list"), emptyList<Any>())
        val numCoroutines = 100
        val jobs =
            List(numCoroutines) { index ->
                launch(Dispatchers.Default) {
                    // Concurrent appends to the same list using the "-" segment
                    dataModel.update(path("/list/-"), "item_$index")
                }
            }
        jobs.joinAll()

        val list = dataModel[path("/list")] as List<*>
        assertThat(list).hasSize(numCoroutines)
        val expectedItems = (0 until numCoroutines).map { "item_$it" }
        assertThat(list).containsExactlyElementsIn(expectedItems)
    }

    @Test
    fun update_concurrentHydrationOfDeepPaths_resolvesSafely(): Unit = runBlocking {
        val numCoroutines = 50
        val jobs =
            List(numCoroutines) { index ->
                launch(Dispatchers.Default) {
                    // Threads race to hydrate the same intermediate paths
                    dataModel.update(path("/deep/nested/path/key_$index"), "val_$index")
                }
            }
        jobs.joinAll()

        val nestedMap = dataModel[path("/deep/nested/path")] as Map<*, *>
        assertThat(nestedMap).hasSize(numCoroutines)
    }

    @Test
    fun get_nonExistentPath_returnsNull() {
        dataModel.update(path("/existing/value"), "Present")

        assertThat(dataModel[path("/non/existent")]).isNull()
        assertThat(dataModel[path("/existing/missing_key")]).isNull()
        assertThat(dataModel[path("/existing/value/deeper")]).isNull()
    }

    @Test
    fun get_outOfBoundsListIndex_returnsNull() {
        dataModel.update(path("/array"), listOf("A", "B"))

        assertThat(dataModel[path("/array/0")]).isEqualTo("A")
        assertThat(dataModel[path("/array/5")]).isNull()
        assertThat(dataModel[path("/array/-1")]).isNull()
    }

    @Test
    fun get_sparseListGapIndex_returnsNull() {
        dataModel.update(path("/array"), listOf("A"))
        dataModel.update(path("/array/3"), "D") // Creates adaptive sparse list

        assertThat(dataModel[path("/array/0")]).isEqualTo("A")
        assertThat(dataModel[path("/array/1")]).isNull() // Gap read
        assertThat(dataModel[path("/array/3")]).isEqualTo("D")
        assertThat(dataModel[path("/array/4")]).isNull() // Standard out-of-bounds
    }

    @Test
    fun get_listAppendSegment_returnsNull() {
        dataModel.update(path("/items/-"), "First")

        assertThat(dataModel[path("/items/-")]).isNull()
    }

    @Test
    fun get_sparseList_iterationYieldsCorrectElementsAndGaps() {
        dataModel.update(path("/array"), listOf("A"))
        dataModel.update(path("/array/2"), "C") // Forces sparse mode. Size becomes 3: [A, null, C]

        val array = dataModel[path("/array")] as List<*>

        // toList() forces an iteration over the AbstractMutableList iterator
        val elements = array.toList()
        assertThat(elements).hasSize(3)
        assertThat(elements[0]).isEqualTo("A")
        assertThat(elements[1]).isNull() // Gap
        assertThat(elements[2]).isEqualTo("C")
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun observe_dataModelUpdate_triggersRecomposition() =
        runComposeUiTest(effectContext = StandardTestDispatcher()) {
            val path = path("/user/name")
            dataModel.update(path, "Alice")

            setContent { BasicText(text = "Hello, ${dataModel[path]}") }

            onNodeWithText("Hello, Alice").assertIsDisplayed()

            dataModel.update(path, "Bob")
            waitForIdle()

            onNodeWithText("Hello, Bob").assertIsDisplayed()
        }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun observe_hydratedPath_triggersRecomposition() =
        runComposeUiTest(effectContext = StandardTestDispatcher()) {
            val targetPath = path("/deep/nested/title")

            setContent {
                val title = dataModel[targetPath] as? String ?: "Pending..."
                BasicText(text = title)
            }

            onNodeWithText("Pending...").assertIsDisplayed()

            dataModel.update(targetPath, "Loaded")
            waitForIdle()

            onNodeWithText("Loaded").assertIsDisplayed()
        }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun observe_parentSubtreeReplacement_triggersRecomposition() =
        runComposeUiTest(effectContext = StandardTestDispatcher()) {
            dataModel.update(path("/profile"), mapOf("user" to mapOf("name" to "Alice")))

            setContent {
                val name = dataModel[path("/profile/user/name")] as? String ?: "No Name"
                BasicText(text = name)
            }

            onNodeWithText("Alice").assertIsDisplayed()

            // Replace the entire "/profile" subtree, removing the "user" object
            dataModel.update(path("/profile"), mapOf("settings" to "dark"))
            waitForIdle()

            // The child component should reactively update to null/fallback
            onNodeWithText("No Name").assertIsDisplayed()
        }

    private fun path(pathString: String): A2uiDataPath = A2uiDataPath(pathString)
}
