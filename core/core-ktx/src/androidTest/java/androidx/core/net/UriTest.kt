/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.core.net

import android.net.Uri
import androidx.test.filters.SmallTest
import androidx.testutils.assertThrows
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Test

@SmallTest
class UriTest {
    @Test
    fun uriFromString() {
        val string = "https://test.example.com/foo?bar#baz"
        assertEquals(Uri.parse(string), string.toUri())
    }

    @Test
    fun uriFromFile() {
        val file = File("/path/to/my/file")
        assertEquals(Uri.fromFile(file), file.toUri())
    }

    @Test
    fun fileFromUri() {
        val file = File("/path/to/my/file")
        assertEquals(file, Uri.fromFile(file).toFile())
    }

    @Test
    fun fileFromNonFileUri() {
        val uri = Uri.parse("https://example.com/path/to/my/file")
        assertThrows<IllegalArgumentException> { uri.toFile() }
            .hasMessageThat()
            .isEqualTo("Uri lacks 'file' scheme: $uri")
    }

    @Test
    fun fileFromUriWithNullPath() {
        val uri = Uri.Builder().scheme("file").authority("example.com").path(null).build()
        assertThrows<IllegalArgumentException> { uri.toFile() }
            .hasMessageThat()
            .isEqualTo("Uri path is null: $uri")
    }
}
