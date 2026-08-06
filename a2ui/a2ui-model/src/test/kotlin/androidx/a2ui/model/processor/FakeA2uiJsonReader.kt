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

package androidx.a2ui.model.processor

import com.google.gson.JsonObject
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import java.io.StringReader

/** A programmatic builder-based fake JSON streaming reader wrapping GSON's JsonReader. */
internal open class FakeA2uiJsonReader(private val jsonString: String) : A2uiJsonReader {
    private var delegate: JsonReader = JsonReader(StringReader(jsonString))
    private var isClosed = false

    constructor(
        jsonObject: JsonObject
    ) : this(
        jsonObject
            .apply {
                if (!has("version")) {
                    addProperty("version", "v0.9")
                }
            }
            .toString()
    )

    private fun ensureOpened() {
        if (isClosed) {
            delegate = JsonReader(StringReader(jsonString))
            isClosed = false
        }
    }

    override fun peek(): A2uiJsonToken {
        ensureOpened()
        return delegate.peek().toA2uiJsonToken()
    }

    override fun beginObject() {
        ensureOpened()
        delegate.beginObject()
    }

    override fun endObject() = delegate.endObject()

    override fun beginArray() {
        ensureOpened()
        delegate.beginArray()
    }

    override fun endArray() = delegate.endArray()

    override fun hasNext(): Boolean {
        ensureOpened()
        return delegate.hasNext()
    }

    override fun nextName(): String {
        ensureOpened()
        return delegate.nextName()
    }

    override fun nextString(): String {
        ensureOpened()
        return delegate.nextString()
    }

    override fun nextBoolean(): Boolean {
        ensureOpened()
        return delegate.nextBoolean()
    }

    override fun nextDouble(): Double {
        ensureOpened()
        return delegate.nextDouble()
    }

    override fun nextInt(): Int {
        ensureOpened()
        return delegate.nextInt()
    }

    override fun nextLong(): Long {
        ensureOpened()
        return delegate.nextLong()
    }

    override fun nextNull() {
        ensureOpened()
        delegate.nextNull()
    }

    override fun skipValue() {
        ensureOpened()
        delegate.skipValue()
    }

    override fun close() {
        delegate.close()
        isClosed = true
    }

    private fun JsonToken.toA2uiJsonToken(): A2uiJsonToken {
        return when (this) {
            JsonToken.BEGIN_ARRAY -> A2uiJsonToken.BEGIN_ARRAY
            JsonToken.END_ARRAY -> A2uiJsonToken.END_ARRAY
            JsonToken.BEGIN_OBJECT -> A2uiJsonToken.BEGIN_OBJECT
            JsonToken.END_OBJECT -> A2uiJsonToken.END_OBJECT
            JsonToken.NAME -> A2uiJsonToken.NAME
            JsonToken.STRING -> A2uiJsonToken.STRING
            JsonToken.NUMBER -> A2uiJsonToken.NUMBER
            JsonToken.BOOLEAN -> A2uiJsonToken.BOOLEAN
            JsonToken.NULL -> A2uiJsonToken.NULL
            JsonToken.END_DOCUMENT -> A2uiJsonToken.END_DOCUMENT
        }
    }
}
