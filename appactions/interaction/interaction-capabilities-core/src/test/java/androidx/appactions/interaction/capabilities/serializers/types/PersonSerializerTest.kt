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

package androidx.appactions.interaction.capabilities.serializers.types

import androidx.appactions.builtintypes.types.Person
import androidx.appactions.interaction.capabilities.serializers.stringValue
import androidx.appactions.interaction.protobuf.Struct
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class PersonSerializerTest {
    @Test
    fun returnsValidTypeNameAndClassRef() {
        assertThat(PersonSerializer().typeName).isEqualTo("Person")
        assertThat(PersonSerializer().classRef).isSameInstanceAs(Person::class.java)
    }

    @Test
    fun serializesToStructWithTypeName() {
        val person = Person.Builder().setName("Jane").build()
        assertThat(PersonSerializer().serialize(person).fieldsMap)
            .containsEntry("@type", stringValue("Person"))
        // TODO(kalindthakkar): Add more tests once serialization logic is in
    }

    @Test
    fun deserializesFromStruct() {
        val struct = Struct.newBuilder()
            .putFields("@type", stringValue("Person"))
            .build()
        assertThat(PersonSerializer().deserialize(struct)).isEqualTo(Person.Builder().build())
        // TODO(kalindthakkar): Add more tests once deserialization logic is in
    }
}
