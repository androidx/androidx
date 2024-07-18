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

import androidx.appactions.builtintypes.types.AbstractPerson
import androidx.appactions.builtintypes.types.Person
import androidx.appactions.builtintypes.types.Thing
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class BuiltInTypeSerializerRegistryTest {

    object AllTestSerializers {
        @JvmStatic fun getPersonSerializer() = PersonSerializer()

        fun someFunThatShouldBeIgnored() {}

        @JvmStatic fun otherFunThatShouldBeIgnored(@Suppress("UNUSED_PARAMETER") int: Int) {}
    }

    object MoreTestSerializers {
        @JvmStatic fun getThingSerializer() = ThingSerializer()
    }

    class MyPerson internal constructor(person: Person) :
        AbstractPerson<MyPerson, MyPerson.Builder>(person) {
        protected override val selfTypeName = "MyPerson"
        protected override val additionalProperties: Map<String, Any?>
            get() = emptyMap()

        protected override fun toBuilderWithAdditionalPropertiesOnly() = Builder()

        class Builder : AbstractPerson.Builder<Builder, MyPerson>() {
            protected override val selfTypeName = "MyPerson.Builder"
            override val additionalProperties: Map<String, Any?>
                get() = emptyMap()

            protected override fun buildFromPerson(person: Person) = MyPerson(person)
        }
    }

    @Test
    fun getSerializer() {
        val registry =
            BuiltInTypeSerializerRegistry(
                serializerRegistryClassNames =
                    listOf(
                        AllTestSerializers::class.java.canonicalName!!,
                        MoreTestSerializers::class.java.canonicalName!!),
                getClassOrNull = { canonicalName ->
                    when (canonicalName) {
                        AllTestSerializers::class.java.canonicalName ->
                            AllTestSerializers::class.java
                        MoreTestSerializers::class.java.canonicalName ->
                            MoreTestSerializers::class.java
                        else -> null
                    }
                })

        assertThat(registry.getSerializer(Person.Builder().build()))
            .isInstanceOf(PersonSerializer::class.java)
        assertThat(registry.getSerializer(Thing.Builder().build()))
            .isInstanceOf(ThingSerializer::class.java)
        // No serializer for this but should go up the parent chain and realize this is a Person
        assertThat(registry.getSerializer(MyPerson.Builder().build()))
            .isInstanceOf(PersonSerializer::class.java)
    }
}
