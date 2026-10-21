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

package androidx.compose.remote.creation.compose.text

import androidx.compose.remote.creation.compose.text.RemoteTypeface.Companion.create
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class RemoteTypefaceTest {

    @Test
    fun create_withNullFontName_returnsNamedWithDefault() {
        val remote = create(null, RemoteTypeface.Style.Normal)
        assertThat(remote).isInstanceOf(RemoteTypeface.Named::class.java)
        remote as RemoteTypeface.Named
        assertThat(remote.name).isEqualTo("default")
        assertThat(remote.weight).isEqualTo(400)
        assertThat(remote.isItalic).isFalse()
    }

    @Test
    fun create_withFontName_returnsNamedWithFontName() {
        val remote = create("my-custom-font", RemoteTypeface.Style.Normal)
        assertThat(remote).isInstanceOf(RemoteTypeface.Named::class.java)
        remote as RemoteTypeface.Named
        assertThat(remote.name).isEqualTo("my-custom-font")
        assertThat(remote.weight).isEqualTo(400)
        assertThat(remote.isItalic).isFalse()
    }

    @Test
    fun named_equalsAndHashCode() {
        val remote1 = RemoteTypeface.Named("my-font", 700, true)
        val remote2 = RemoteTypeface.Named("my-font", 700, true)
        val remote3 = RemoteTypeface.Named("my-font", 400, true)
        val remote4 = RemoteTypeface.Named("other-font", 700, true)

        assertThat(remote1).isEqualTo(remote2)
        assertThat(remote1.hashCode()).isEqualTo(remote2.hashCode())

        assertThat(remote1).isNotEqualTo(remote3)
        assertThat(remote1).isNotEqualTo(remote4)
    }
}
