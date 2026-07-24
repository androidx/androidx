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

package androidx.a2ui.compose.runtime

import androidx.a2ui.model.processor.A2uiJsonToken
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import java.io.IOException
import kotlin.test.assertFailsWith
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class A2uiJsonReaderImplTest {

    @Test
    fun nextString_parsesStringCorrectly() {
        A2uiJsonReaderImpl("""["hello world"]""").use { reader ->
            reader.beginArray()
            assertThat(reader.nextString()).isEqualTo("hello world")
            reader.endArray()
        }
    }

    @Test
    fun nextBoolean_parsesBooleanCorrectly() {
        A2uiJsonReaderImpl("[true, false]").use { reader ->
            reader.beginArray()
            assertThat(reader.nextBoolean()).isTrue()
            assertThat(reader.nextBoolean()).isFalse()
            reader.endArray()
        }
    }

    @Test
    fun nextDouble_parsesDoubleCorrectly() {
        A2uiJsonReaderImpl("[3.1415926535, -3.1415926535]").use { reader ->
            reader.beginArray()
            assertThat(reader.nextDouble()).isEqualTo(3.1415926535)
            assertThat(reader.nextDouble()).isEqualTo(-3.1415926535)
            reader.endArray()
        }
    }

    @Test
    fun nextInt_parsesIntCorrectly() {
        A2uiJsonReaderImpl("[42, -42]").use { reader ->
            reader.beginArray()
            assertThat(reader.nextInt()).isEqualTo(42)
            assertThat(reader.nextInt()).isEqualTo(-42)
            reader.endArray()
        }
    }

    @Test
    fun nextLong_parsesLongCorrectly() {
        A2uiJsonReaderImpl("[999999999999999]").use { reader ->
            reader.beginArray()
            assertThat(reader.nextLong()).isEqualTo(999999999999999L)
            reader.endArray()
        }
    }

    @Test
    fun nextName_parsesNameCorrectly() {
        A2uiJsonReaderImpl("""{"testKey": "val"}""").use { reader ->
            reader.beginObject()
            assertThat(reader.nextName()).isEqualTo("testKey")
        }
    }

    @Test
    fun nextNull_parsesNullCorrectly() {
        A2uiJsonReaderImpl("[null]").use { reader ->
            reader.beginArray()
            assertThat(reader.peek()).isEqualTo(A2uiJsonToken.NULL)
            reader.nextNull()
            reader.endArray()
            assertThat(reader.peek()).isEqualTo(A2uiJsonToken.END_DOCUMENT)
        }
    }

    @Test
    fun beginObject_and_endObject_navigatesObjectScopeCorrectly() {
        A2uiJsonReaderImpl("{}").use { reader ->
            assertThat(reader.peek()).isEqualTo(A2uiJsonToken.BEGIN_OBJECT)
            reader.beginObject()

            assertThat(reader.hasNext()).isFalse()
            assertThat(reader.peek()).isEqualTo(A2uiJsonToken.END_OBJECT)
            reader.endObject()

            assertThat(reader.peek()).isEqualTo(A2uiJsonToken.END_DOCUMENT)
        }
    }

    @Test
    fun beginArray_and_endArray_navigatesArrayScopeCorrectly() {
        A2uiJsonReaderImpl("[]").use { reader ->
            assertThat(reader.peek()).isEqualTo(A2uiJsonToken.BEGIN_ARRAY)
            reader.beginArray()

            assertThat(reader.hasNext()).isFalse()
            assertThat(reader.peek()).isEqualTo(A2uiJsonToken.END_ARRAY)
            reader.endArray()

            assertThat(reader.peek()).isEqualTo(A2uiJsonToken.END_DOCUMENT)
        }
    }

    @Test
    fun peek_mapsAllTokensCorrectly() {
        // A JSON string that forces the reader to traverse every token type.
        A2uiJsonReaderImpl("""{"a": [1, "two", true, null]}""").use { reader ->
            assertThat(reader.peek()).isEqualTo(A2uiJsonToken.BEGIN_OBJECT)
            reader.beginObject()

            assertThat(reader.peek()).isEqualTo(A2uiJsonToken.NAME)
            assertThat(reader.nextName()).isEqualTo("a")

            assertThat(reader.peek()).isEqualTo(A2uiJsonToken.BEGIN_ARRAY)
            reader.beginArray()

            assertThat(reader.peek()).isEqualTo(A2uiJsonToken.NUMBER)
            assertThat(reader.nextInt()).isEqualTo(1)

            assertThat(reader.peek()).isEqualTo(A2uiJsonToken.STRING)
            assertThat(reader.nextString()).isEqualTo("two")

            assertThat(reader.peek()).isEqualTo(A2uiJsonToken.BOOLEAN)
            assertThat(reader.nextBoolean()).isTrue()

            assertThat(reader.peek()).isEqualTo(A2uiJsonToken.NULL)
            reader.nextNull()

            assertThat(reader.peek()).isEqualTo(A2uiJsonToken.END_ARRAY)
            reader.endArray()

            assertThat(reader.peek()).isEqualTo(A2uiJsonToken.END_OBJECT)
            reader.endObject()

            assertThat(reader.peek()).isEqualTo(A2uiJsonToken.END_DOCUMENT)
        }
    }

    @Test
    fun hasNext_correctlyIdentifiesEndOfObjectAndArray() {
        A2uiJsonReaderImpl("""{"key": [1]}""").use { reader ->
            reader.beginObject()
            assertThat(reader.hasNext()).isTrue()
            reader.nextName()

            reader.beginArray()
            assertThat(reader.hasNext()).isTrue()
            reader.nextInt()

            // After reading the only element, hasNext() should be false
            assertThat(reader.hasNext()).isFalse()
            reader.endArray()

            // After reading the only key-value pair, hasNext() should be false
            assertThat(reader.hasNext()).isFalse()
            reader.endObject()
        }
    }

    @Test
    fun hasNext_arrayTraversal_iteratesCorrectly() {
        A2uiJsonReaderImpl("""["first", "second", "third"]""").use { reader ->
            reader.beginArray()

            assertThat(reader.hasNext()).isTrue()
            assertThat(reader.nextString()).isEqualTo("first")

            assertThat(reader.hasNext()).isTrue()
            assertThat(reader.nextString()).isEqualTo("second")

            assertThat(reader.hasNext()).isTrue()
            assertThat(reader.nextString()).isEqualTo("third")

            assertThat(reader.hasNext()).isFalse()
            reader.endArray()
        }
    }

    @Test
    fun skipValue_skipsDeeplyNestedStructures() {
        val json = """[ 1, {"a": [1, 2, {"b": 3}]}, "last" ]"""
        A2uiJsonReaderImpl(json).use { reader ->
            reader.beginArray()
            assertThat(reader.nextInt()).isEqualTo(1)

            // Calling skipValue here should skip over the entire nested object and its array
            reader.skipValue()

            // Verify we are at the next top-level item
            assertThat(reader.peek()).isEqualTo(A2uiJsonToken.STRING)
            assertThat(reader.nextString()).isEqualTo("last")

            reader.endArray()
        }
    }

    @Test
    fun nextInt_onBoolean_throwsIllegalStateException() {
        A2uiJsonReaderImpl("[true]").use { reader ->
            reader.beginArray()
            assertFailsWith<IllegalStateException> { reader.nextInt() }
        }
    }

    @Test
    fun beginObject_onBoolean_throwsIllegalStateException() {
        val json = """{"a": true}"""
        A2uiJsonReaderImpl(json).use { reader ->
            reader.beginObject()
            reader.nextName()

            assertFailsWith<IllegalStateException> { reader.beginObject() }

            // Ensure the stream wasn't broken by the exception and we can continue reading
            assertThat(reader.nextBoolean()).isTrue()
            reader.endObject()
        }
    }

    @Test
    fun close_preventsFurtherReading() {
        val reader = A2uiJsonReaderImpl("[123]")

        reader.close()

        assertFailsWith<IllegalStateException> { reader.peek() }
    }

    @Test
    fun malformedJson_throwsExceptionOnRead() {
        val malformedJson = """{"missingEndQuote: """
        A2uiJsonReaderImpl(malformedJson).use { reader ->
            reader.beginObject()

            assertFailsWith<IOException> { reader.nextName() }
        }
    }
}
