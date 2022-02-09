/*
 * Copyright (C) 2017 The Android Open Source Project
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

package androidx.room.migration.bundle

import androidx.annotation.RestrictTo

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.TypeAdapter
import com.google.gson.TypeAdapterFactory
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter

import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.io.UnsupportedEncodingException
import kotlin.jvm.Throws

/**
 * Data class that holds the information about a database schema export.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public open class SchemaBundle(
    @SerializedName("formatVersion")
    public open val formatVersion: Int,
    @SerializedName("database")
    public open val database: DatabaseBundle
) : SchemaEquality<SchemaBundle> {
    public companion object {
        private const val CHARSET = "UTF-8"
        public const val LATEST_FORMAT: Int = 1
        private val GSON: Gson = GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .registerTypeAdapterFactory(
                EntityTypeAdapterFactory()
            )
            .create()

        /**
         * @hide
         */
        @Throws(UnsupportedEncodingException::class)
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
        @JvmStatic
        public fun deserialize(fis: InputStream): SchemaBundle {
            InputStreamReader(fis, CHARSET).use { inputStream ->
                return GSON.fromJson(inputStream, SchemaBundle::class.javaObjectType)
                    ?: throw IllegalStateException("Invalid schema file")
            }
        }

        /**
         * @hide
         */
        @Throws(IOException::class)
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
        @JvmStatic
        public fun serialize(bundle: SchemaBundle, file: File) {
            val fos = FileOutputStream(file, false)
            OutputStreamWriter(fos, CHARSET).use { outputStreamWriter ->
                GSON.toJson(bundle, outputStreamWriter)
            }
        }
    }

    override fun isSchemaEqual(other: SchemaBundle): Boolean {
        return SchemaEqualityUtil.checkSchemaEquality(database, other.database) &&
            formatVersion == other.formatVersion
    }

    private open class EntityTypeAdapterFactory : TypeAdapterFactory {
        @Suppress("UNCHECKED_CAST")
        override fun <T> create(gson: Gson, type: TypeToken<T>): TypeAdapter<T>? {
            if (!EntityBundle::class.java.isAssignableFrom(type.rawType)) {
                return null
            }
            val jsonElementAdapter = gson.getAdapter(
                JsonElement::class.java
            )
            val entityBundleAdapter = gson.getDelegateAdapter(
                this,
                TypeToken.get(EntityBundle::class.java)
            )
            val ftsEntityBundleAdapter = gson.getDelegateAdapter(
                this,
                TypeToken.get(FtsEntityBundle::class.java)
            )
            return EntityTypeAdapter(
                jsonElementAdapter, entityBundleAdapter, ftsEntityBundleAdapter
            ) as TypeAdapter<T>
        }

        private class EntityTypeAdapter(
            val jsonElementAdapter: TypeAdapter<JsonElement>,
            val entityBundleAdapter: TypeAdapter<EntityBundle>,
            val ftsEntityBundleAdapter: TypeAdapter<FtsEntityBundle>
        ) : TypeAdapter<EntityBundle>() {
            @Throws(IOException::class)
            override fun write(out: JsonWriter?, value: EntityBundle?) {
                if (value is FtsEntityBundle) {
                    ftsEntityBundleAdapter.write(out, value)
                } else {
                    entityBundleAdapter.write(out, value)
                }
            }

            override fun read(input: JsonReader?): EntityBundle {
                val jsonObject: JsonObject = jsonElementAdapter.read(input).asJsonObject
                return if (jsonObject.has("ftsVersion")) {
                    ftsEntityBundleAdapter.fromJsonTree(jsonObject)
                } else {
                    entityBundleAdapter.fromJsonTree(jsonObject)
                }
            }
        }
    }
}
