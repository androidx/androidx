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

package androidx.compose.ui.text

import androidx.compose.ui.graphics.Color
import com.google.common.truth.Truth.assertThat
import kotlin.test.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class LinkAnnotationTest {
    @Test
    fun copy_keepAll_linkAnnotation_url() {
        val original = LinkAnnotation.Url("url1", TextLinkStyles(), null)
        val actual = original.copy()

        assertThat(actual).isEqualTo(original)
    }

    @Test
    fun copy_changeAll_linkAnnotation_url() {
        val original = LinkAnnotation.Url("url1", TextLinkStyles(), null)
        val actual = original.copy(url = "url2", TextLinkStyles(SpanStyle(Color.Green))) {}

        assertThat(actual.url).isEqualTo("url2")
        assertThat(actual.styles).isEqualTo(TextLinkStyles(SpanStyle(Color.Green)))
        assertThat(actual.linkInteractionListener).isNotNull()
    }

    @Test
    fun copy_keepAll_linkAnnotation_clickable() {
        val original = LinkAnnotation.Clickable("tag", TextLinkStyles()) {}
        val actual = original.copy()

        assertThat(actual).isEqualTo(original)
    }

    @Test
    fun copy_changeAll_linkAnnotation_clickable() {
        val original = LinkAnnotation.Clickable("tag", TextLinkStyles()) {}
        val actual = original.copy("tag1", TextLinkStyles(SpanStyle(Color.Green)), null)

        assertThat(actual.tag).isEqualTo("tag1")
        assertThat(actual.styles).isEqualTo(TextLinkStyles(SpanStyle(Color.Green)))
        assertThat(actual.linkInteractionListener).isNull()
    }
}
