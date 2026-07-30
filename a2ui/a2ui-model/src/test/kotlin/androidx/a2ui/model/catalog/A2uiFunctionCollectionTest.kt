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

package androidx.a2ui.model.catalog

import androidx.a2ui.model.protocol.A2uiExecutionContext
import androidx.a2ui.model.schema.A2uiObjectSchema
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class A2uiFunctionCollectionTest {

    @Test
    fun lookupByString_returnsCorrectFunction() {
        val func1 = StubFunction("func1")
        val func2 = StubFunction("func2")

        val collection = A2uiFunctionCollection(listOf(func1, func2))

        assertThat(collection["func1"]).isEqualTo(func1)
        assertThat(collection["func2"]).isEqualTo(func2)
        assertThat(collection["unknownFunc"]).isNull()
    }

    @Test
    fun listOperations_behaveAsExpectedList() {
        val func1 = StubFunction("func1")
        val func2 = StubFunction("func2")
        val functionsList = listOf(func1, func2)

        val collection = A2uiFunctionCollection(functionsList)

        assertThat(collection).hasSize(2)
        assertThat(collection.isEmpty()).isFalse()
        assertThat(collection[0]).isEqualTo(func1)
        assertThat(collection[1]).isEqualTo(func2)
        assertThat(collection.contains(func1)).isTrue()
        assertThat(collection.containsAll(listOf(func1, func2))).isTrue()
        assertThat(collection.indexOf(func2)).isEqualTo(1)
        assertThat(collection.lastIndexOf(func1)).isEqualTo(0)
        assertThat(collection.toList()).containsExactly(func1, func2).inOrder()
        assertThat(collection.subList(0, 1)).containsExactly(func1)
        assertThat(collection).isEqualTo(functionsList)
        assertThat(collection.hashCode()).isEqualTo(functionsList.hashCode())
        assertThat(collection.toString()).isEqualTo(functionsList.toString())
    }

    @Test
    fun emptyCollection_returnsEmpty() {
        val collection = A2uiFunctionCollection()

        assertThat(collection).isEmpty()
        assertThat(collection.size).isEqualTo(0)
        assertThat(collection["unknownFunc"]).isNull()
    }

    @Test
    fun duplicateNames_throwsException() {
        val func1 = StubFunction("duplicateFunc")
        val func2 = StubFunction("duplicateFunc")

        val exception =
            assertThrows(IllegalArgumentException::class.java) {
                A2uiFunctionCollection(listOf(func1, func2))
            }
        assertThat(exception)
            .hasMessageThat()
            .contains("Duplicate function registered for name 'duplicateFunc'")
    }

    @Test
    fun equalsAndHashCode_workCorrectly() {
        val func1 = StubFunction("func1")
        val func2 = StubFunction("func2")
        val func3 = StubFunction("func3")

        val collection1 = A2uiFunctionCollection(listOf(func1, func2))
        val collection2 = A2uiFunctionCollection(listOf(func1, func2))
        val differentCollection = A2uiFunctionCollection(listOf(func1, func3))

        assertThat(collection1).isEqualTo(collection2)
        assertThat(collection1.hashCode()).isEqualTo(collection2.hashCode())
        assertThat(collection1).isNotEqualTo(differentCollection)
        assertThat(collection1.hashCode()).isNotEqualTo(differentCollection.hashCode())
    }

    @Test
    fun equalsAndHashCode_emptyCollections_workCorrectly() {
        val func1 = StubFunction("func1")
        val func2 = StubFunction("func2")
        val collection1 = A2uiFunctionCollection(listOf(func1, func2))
        val empty1 = A2uiFunctionCollection()
        val empty2 = A2uiFunctionCollection(emptyList())

        assertThat(empty1).isEqualTo(empty2)
        assertThat(empty1.hashCode()).isEqualTo(empty2.hashCode())
        assertThat(collection1).isNotEqualTo(empty1)
    }

    private class StubFunction(functionName: String) : A2uiFunction {
        override val definition =
            object : A2uiFunctionDefinition {
                override val name: String = functionName
                override val description: String = "Description for $functionName"
                override val argumentSchema = A2uiObjectSchema.INSTANCE
                override val returnType = A2uiFunctionReturnType.VOID
            }

        override fun execute(args: Map<String, Any>, executionContext: A2uiExecutionContext): Any? =
            null
    }
}
