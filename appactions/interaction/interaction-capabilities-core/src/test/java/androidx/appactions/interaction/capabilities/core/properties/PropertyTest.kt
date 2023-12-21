/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.appactions.interaction.capabilities.core.properties

import androidx.appactions.interaction.capabilities.core.properties.Property.Companion.unsupported
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class PropertyTest {

    @Test
    fun noArgConstructor_reasonableDefaultValues() {
        val prop: Property<StringValue?> = Property()

        assertThat(prop.isSupported).isTrue()
        assertThat(prop.isRequiredForExecution).isFalse()
        assertThat(prop.shouldMatchPossibleValues).isFalse()
        assertThat(prop.possibleValues).isEmpty()
    }

    @Test
    fun fullConstructor_returnsAllValues() {
        val prop = Property(
            listOf(StringValue("test")),
            isRequiredForExecution = true,
            shouldMatchPossibleValues = true,
        )

        assertThat(prop.isSupported).isTrue()
        assertThat(prop.isRequiredForExecution).isTrue()
        assertThat(prop.shouldMatchPossibleValues).isTrue()
        assertThat(prop.possibleValues).containsExactly(StringValue("test"))
    }

    @Test
    fun supplierConstructor_returnsMostRecentPossibleValues() {
        val mutableValues = ArrayList<StringValue>()
        val prop = Property({ mutableValues })

        assertThat(prop.shouldMatchPossibleValues).isFalse()
        assertThat(prop.possibleValues).isEmpty()

        // Mutate list
        mutableValues.add(StringValue("test"))

        assertThat(prop.possibleValues).containsExactly(StringValue("test"))
    }

    @Test
    fun staticUnsupportedMethod_returnsSensibleValues() {
        val prop = unsupported<StringValue>()

        assertThat(prop.isSupported).isFalse()
        assertThat(prop.isRequiredForExecution).isFalse()
        assertThat(prop.shouldMatchPossibleValues).isFalse()
        assertThat(prop.possibleValues).isEmpty()
    }
}
