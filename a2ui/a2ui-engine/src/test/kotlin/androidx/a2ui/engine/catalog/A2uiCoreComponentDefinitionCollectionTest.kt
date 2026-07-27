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

package androidx.a2ui.engine.catalog

import androidx.a2ui.model.schema.A2uiObjectSchema
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class A2uiCoreComponentDefinitionCollectionTest {

    @Test
    fun lookupByString_returnsCorrectDefinition() {
        val def1 = StubComponentDefinition("Component1")
        val def2 = StubComponentDefinition("Component2")

        val collection = A2uiCoreComponentDefinitionCollection(listOf(def1, def2))

        assertThat(collection["Component1"]).isEqualTo(def1)
        assertThat(collection["Component2"]).isEqualTo(def2)
        assertThat(collection["UnknownComponent"]).isNull()
    }

    @Test
    fun listOperations_behaveAsExpectedList() {
        val def1 = StubComponentDefinition("Component1")
        val def2 = StubComponentDefinition("Component2")
        val definitionsList = listOf(def1, def2)

        val collection = A2uiCoreComponentDefinitionCollection(definitionsList)

        assertThat(collection).hasSize(2)
        assertThat(collection.isEmpty()).isFalse()
        assertThat(collection[0]).isEqualTo(def1)
        assertThat(collection[1]).isEqualTo(def2)
        assertThat(collection.contains(def1)).isTrue()
        assertThat(collection.containsAll(listOf(def1, def2))).isTrue()
        assertThat(collection.indexOf(def2)).isEqualTo(1)
        assertThat(collection.lastIndexOf(def1)).isEqualTo(0)
        assertThat(collection.toList()).containsExactly(def1, def2).inOrder()
        assertThat(collection.subList(0, 1)).containsExactly(def1)
        assertThat(collection).isEqualTo(definitionsList)
        assertThat(collection.hashCode()).isEqualTo(definitionsList.hashCode())
        assertThat(collection.toString()).isEqualTo(definitionsList.toString())
    }

    @Test
    fun emptyCollection_returnsEmpty() {
        val collection = A2uiCoreComponentDefinitionCollection()

        assertThat(collection).isEmpty()
        assertThat(collection.size).isEqualTo(0)
        assertThat(collection["UnknownComponent"]).isNull()
    }

    @Test
    fun duplicateNames_throwsException() {
        val def1 = StubComponentDefinition("DuplicateComponent")
        val def2 = StubComponentDefinition("DuplicateComponent")

        val exception =
            assertThrows(IllegalArgumentException::class.java) {
                A2uiCoreComponentDefinitionCollection(listOf(def1, def2))
            }

        assertThat(exception)
            .hasMessageThat()
            .contains("Duplicate component definition registered for name 'DuplicateComponent'")
    }

    @Test
    fun equalsAndHashCode_workCorrectly() {
        val def1 = StubComponentDefinition("Component1")
        val def2 = StubComponentDefinition("Component2")
        val def3 = StubComponentDefinition("Component3")

        val collection1 = A2uiCoreComponentDefinitionCollection(listOf(def1, def2))
        val collection2 = A2uiCoreComponentDefinitionCollection(listOf(def1, def2))
        val differentCollection = A2uiCoreComponentDefinitionCollection(listOf(def1, def3))

        assertThat(collection1).isEqualTo(collection2)
        assertThat(collection1.hashCode()).isEqualTo(collection2.hashCode())
        assertThat(collection1).isNotEqualTo(differentCollection)
        assertThat(collection1.hashCode()).isNotEqualTo(differentCollection.hashCode())
    }

    @Test
    fun equalsAndHashCode_emptyCollections_workCorrectly() {
        val def1 = StubComponentDefinition("Component1")
        val def2 = StubComponentDefinition("Component2")
        val collection1 = A2uiCoreComponentDefinitionCollection(listOf(def1, def2))
        val empty1 = A2uiCoreComponentDefinitionCollection()
        val empty2 = A2uiCoreComponentDefinitionCollection(emptyList())

        assertThat(empty1).isEqualTo(empty2)
        assertThat(empty1.hashCode()).isEqualTo(empty2.hashCode())
        assertThat(collection1).isNotEqualTo(empty1)
    }

    private class StubComponentDefinition(
        override val name: String,
        override val description: String = "Description for $name",
        override val propertySchema: A2uiObjectSchema = A2uiObjectSchema.INSTANCE,
    ) : A2uiCoreComponentDefinition
}
