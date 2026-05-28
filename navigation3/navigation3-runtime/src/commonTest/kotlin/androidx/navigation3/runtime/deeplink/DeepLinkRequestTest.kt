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

package androidx.navigation3.runtime.deeplink

import androidx.kruth.assertThat
import androidx.navigation3.runtime.IgnoreAndroidHostTestTarget
import kotlin.test.Test

@IgnoreAndroidHostTestTarget
class DeepLinkRequestTest {

    @Test
    fun fromUri() {
        val string = "navigation3.test.com/test"
        val uri = DeepLinkUri(string)
        val request = DeepLinkRequest.fromUri(uri)

        assertThat(request.uri).isEqualTo(uri)
        assertThat(request.mimeType).isNull()
        assertThat(request.action).isNull()
    }

    @Test
    fun fromMimeType() {
        val mimeType = "image/png"
        val request = DeepLinkRequest.fromMimeType(mimeType)

        assertThat(request.uri).isNull()
        assertThat(request.mimeType).isEqualTo(mimeType)
        assertThat(request.action).isNull()
    }

    @Test
    fun fromAction() {
        val action = "action"
        val request = DeepLinkRequest.fromAction(action)

        assertThat(request.uri).isNull()
        assertThat(request.mimeType).isNull()
        assertThat(request.action).isEqualTo(action)
    }
}
