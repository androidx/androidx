// Copyright 2023 The Android Open Source Project
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
package androidx.appactions.interaction.capabilities.serializers.types

import androidx.appactions.builtintypes.types.Person
import androidx.appactions.interaction.capabilities.serializers.stringValue
import androidx.appactions.interaction.protobuf.Struct
import java.lang.Class
import kotlin.String
import kotlin.collections.List
import kotlin.collections.emptyList

/** Serializes [Person] to and from a [Struct] i.e. JSON obj. */
public class PersonSerializer : BuiltInTypeSerializer<Person> {
    public override val typeName: String = "Person"

    public override val classRef: Class<Person> = Person::class.java

    public override fun serialize(instance: Person): Struct =
        Struct.newBuilder().putFields("@type", stringValue("Person")).build()

    public override fun deserialize(jsonObj: Struct): Person = Person.Builder().build()

    public override fun <CanonicalValue> getCanonicalValues(
        cls: Class<CanonicalValue>
    ): List<CanonicalValue> = emptyList()
}
