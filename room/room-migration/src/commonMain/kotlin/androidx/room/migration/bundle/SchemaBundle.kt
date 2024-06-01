/*
 * Copyright 2024 The Android Open Source Project
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

@file:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)

package androidx.room.migration.bundle

import androidx.annotation.RestrictTo
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.ClassDiscriminatorMode
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonContentPolymorphicSerializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.modules.SerializersModule

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
expect class SchemaBundle(formatVersion: Int, database: DatabaseBundle) :
    SchemaEquality<SchemaBundle> {

    val formatVersion: Int
    val database: DatabaseBundle

    override fun isSchemaEqual(other: SchemaBundle): Boolean
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) const val SCHEMA_LATEST_FORMAT_VERSION = 1

@OptIn(ExperimentalSerializationApi::class) // due to prettyPrintIndex
internal val json = Json {
    // The schema files are meant to be human readable and are checked-in into repositories.
    prettyPrint = true
    // Keep index to 2 spaces as that is what we used before kotlinx-serialization
    prettyPrintIndent = "  "
    // Don't output class discriminator as that would encode library class names into JSON file
    // making implementation details harder to refactor. When reading, we use a content inspector
    // that will perform polymorphic deserialization.
    classDiscriminatorMode = ClassDiscriminatorMode.NONE
    serializersModule = SerializersModule {
        polymorphicDefaultDeserializer(BaseEntityBundle::class) { EntitySerializer }
    }
}

private object EntitySerializer :
    JsonContentPolymorphicSerializer<BaseEntityBundle>(baseClass = BaseEntityBundle::class) {
    override fun selectDeserializer(
        element: JsonElement
    ): DeserializationStrategy<BaseEntityBundle> =
        when {
            "ftsVersion" in element.jsonObject -> FtsEntityBundle.serializer()
            else -> EntityBundle.serializer()
        }
}
