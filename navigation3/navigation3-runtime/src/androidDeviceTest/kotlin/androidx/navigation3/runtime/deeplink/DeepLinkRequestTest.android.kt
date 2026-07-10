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

import android.content.Intent
import android.os.Bundle
import androidx.core.net.toUri
import androidx.kruth.assertThat
import kotlin.test.Test

class DeepLinkRequestTestAndroid {
    @Test
    fun testFromIntentUri() {
        val uri = "navigation3.test.com/test".toUri()
        val action = "Test.Action"
        val intent = Intent()
        intent.data = uri
        intent.action = action
        val request = DeepLinkRequest(intent)
        assertThat(request.uri.toString()).isEqualTo(uri.toString())
        assertThat(request.extras).isNotEmpty()
        assertThat(request.extras[DeepLinkRequest.ActionExtrasKey]).isEqualTo(action)
    }

    @Test
    fun testFromIntentMimeType() {
        val mimeType = "image/png"
        val action = "Test.Action"
        val intent = Intent()
        intent.type = mimeType
        intent.action = action
        val request = DeepLinkRequest(intent)
        assertThat(request.extras).isNotEmpty()
        assertThat(request.extras[DeepLinkRequest.Companion.MimeTypeExtrasKey]).isEqualTo(mimeType)
        assertThat(request.extras[DeepLinkRequest.ActionExtrasKey]).isEqualTo(action)
    }

    @Test
    fun testWithIntentAndExtras() {
        val intentMimeType = "image/png"
        val intent = Intent()
        intent.type = intentMimeType
        val extrasIntKey = "intKey"
        val extrasInt = 1
        val request = DeepLinkRequest(intent, mapOf(extrasIntKey to extrasInt))
        assertThat(request.extras).isNotEmpty()
        assertThat(request.extras[DeepLinkRequest.Companion.MimeTypeExtrasKey])
            .isEqualTo(intentMimeType)
        assertThat(request.extras[extrasIntKey]).isEqualTo(extrasInt)
    }

    @Test
    fun testExtrasTakesPrecedenceOverIntentMimeType() {
        val intentMimeType = "image/png"
        val intent = Intent()
        intent.type = intentMimeType
        val extrasMimeType = "image/jpg"
        val request = DeepLinkRequest(intent, DeepLinkRequest.mimeTypeExtra(extrasMimeType))
        assertThat(request.extras).isNotEmpty()
        assertThat(request.extras[DeepLinkRequest.Companion.MimeTypeExtrasKey])
            .isEqualTo(extrasMimeType)
    }

    @Test
    fun testExtrasIncludesIntentExtra() {
        val intent = Intent()

        val key = "key"
        val intentBundle = Bundle()
        intentBundle.putInt(key, 123)
        intent.putExtras(intentBundle)

        val request = DeepLinkRequest(intent)

        assertThat(request.extras).isNotEmpty()
        assertThat(request.extras[key]).isEqualTo(123)
    }

    @Test
    fun testExtrasTakesPrecedenceOverIntentExtras() {
        val intent = Intent()

        val key = "key"
        val intentBundle = Bundle()
        intentBundle.putInt(key, 123)
        intent.putExtras(intentBundle)

        val request = DeepLinkRequest(intent, mapOf(key to 456))

        assertThat(request.extras).isNotEmpty()
        assertThat(request.extras[key]).isEqualTo(456)
    }
}
