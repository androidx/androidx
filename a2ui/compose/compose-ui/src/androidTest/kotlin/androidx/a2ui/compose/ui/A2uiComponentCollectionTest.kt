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

package androidx.a2ui.compose.ui

import androidx.a2ui.compose.runtime.A2uiComponentProperties
import androidx.a2ui.compose.runtime.A2uiComponentScope
import androidx.a2ui.compose.runtime.A2uiProperty
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class A2uiComponentCollectionTest {

    @Test
    fun lookupByString_returnsCorrectComponent() {
        val comp1 = StubComponent("Component1")
        val comp2 = StubComponent("Component2")

        val collection = A2uiComponentCollection(listOf(comp1, comp2))

        assertThat(collection["Component1"]).isEqualTo(comp1)
        assertThat(collection["Component2"]).isEqualTo(comp2)
        assertThat(collection["UnknownComponent"]).isNull()
    }

    @Test
    fun listOperations_behaveAsExpectedList() {
        val comp1 = StubComponent("Component1")
        val comp2 = StubComponent("Component2")
        val componentsList = listOf(comp1, comp2)

        val collection = A2uiComponentCollection(componentsList)

        assertThat(collection).hasSize(2)
        assertThat(collection.isEmpty()).isFalse()
        assertThat(collection[0]).isEqualTo(comp1)
        assertThat(collection[1]).isEqualTo(comp2)
        assertThat(collection.contains(comp1)).isTrue()
        assertThat(collection.containsAll(listOf(comp1, comp2))).isTrue()
        assertThat(collection.indexOf(comp2)).isEqualTo(1)
        assertThat(collection.lastIndexOf(comp1)).isEqualTo(0)
        assertThat(collection.toList()).containsExactly(comp1, comp2).inOrder()
        assertThat(collection.subList(0, 1)).containsExactly(comp1)
        assertThat(collection).isEqualTo(componentsList)
        assertThat(collection.hashCode()).isEqualTo(componentsList.hashCode())
        assertThat(collection.toString()).isEqualTo(componentsList.toString())
    }

    @Test
    fun emptyCollection_returnsEmpty() {
        val collection = A2uiComponentCollection()

        assertThat(collection).isEmpty()
        assertThat(collection.size).isEqualTo(0)
        assertThat(collection["UnknownComponent"]).isNull()
    }

    @Test
    fun duplicateNames_throwsException() {
        val comp1 = StubComponent("DuplicateComponent")
        val comp2 = StubComponent("DuplicateComponent")

        val exception =
            assertThrows(IllegalArgumentException::class.java) {
                A2uiComponentCollection(listOf(comp1, comp2))
            }
        assertThat(exception)
            .hasMessageThat()
            .contains("Duplicate component registered for name 'DuplicateComponent'")
    }

    @Test
    fun equalsAndHashCode_workCorrectly() {
        val comp1 = StubComponent("Component1")
        val comp2 = StubComponent("Component2")
        val comp3 = StubComponent("Component3")

        val collection1 = A2uiComponentCollection(listOf(comp1, comp2))
        val collection2 = A2uiComponentCollection(listOf(comp1, comp2))
        val differentCollection = A2uiComponentCollection(listOf(comp1, comp3))

        assertThat(collection1).isEqualTo(collection2)
        assertThat(collection1.hashCode()).isEqualTo(collection2.hashCode())
        assertThat(collection1).isNotEqualTo(differentCollection)
        assertThat(collection1.hashCode()).isNotEqualTo(differentCollection.hashCode())
    }

    @Test
    fun equalsAndHashCode_emptyCollections_workCorrectly() {
        val comp1 = StubComponent("Component1")
        val comp2 = StubComponent("Component2")
        val collection1 = A2uiComponentCollection(listOf(comp1, comp2))
        val empty1 = A2uiComponentCollection()
        val empty2 = A2uiComponentCollection(emptyList())

        assertThat(empty1).isEqualTo(empty2)
        assertThat(empty1.hashCode()).isEqualTo(empty2.hashCode())
        assertThat(collection1).isNotEqualTo(empty1)
    }

    private class StubComponent(
        override val name: String,
        override val description: String = "Description for $name",
        override val properties: List<A2uiProperty<*>> = emptyList(),
    ) : A2uiComponent {
        @Composable
        override fun A2uiComponentScope.Content(
            properties: A2uiComponentProperties,
            modifier: Modifier,
        ) {}
    }
}
