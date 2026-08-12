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
    fun testIntentUri() {
        val uri = "navigation3.test.com/test".toUri()
        val action = "Test.Action"
        val intent = Intent()
        intent.data = uri
        intent.action = action
        val request = DeepLinkRequest(intent)
        assertThat(request.uri.toString()).isEqualTo(uri.toString())
        assertThat(request.extras.isNotEmpty()).isTrue()
        assertThat(request.extras[DeepLinkRequest.ActionExtrasKey]).isEqualTo(action)
    }

    @Test
    fun testIntentMimeType() {
        val mimeType = "image/png"
        val intent = Intent()
        intent.type = mimeType
        val request = DeepLinkRequest(intent)
        assertThat(request.extras.isNotEmpty()).isTrue()
        assertThat(request.extras[DeepLinkRequest.Companion.MimeTypeExtrasKey]).isEqualTo(mimeType)
    }

    @Test
    fun testIntentAction() {
        val action = "Test.Action"
        val intent = Intent()
        intent.action = action
        val request = DeepLinkRequest(intent)
        assertThat(request.extras.isNotEmpty()).isTrue()
        assertThat(request.extras[DeepLinkRequest.ActionExtrasKey]).isEqualTo(action)
    }

    @Test
    fun testWithIntentAndExtras() {
        val intentMimeType = "image/png"
        val intent = Intent()
        intent.type = intentMimeType
        val extrasIntKey = object : RequestExtrasKey<Int> {}
        val extrasInt = 1
        val request = DeepLinkRequest(intent, requestExtras { put(extrasIntKey, extrasInt) })
        assertThat(request.extras.isNotEmpty()).isTrue()
        assertThat(request.extras[DeepLinkRequest.Companion.MimeTypeExtrasKey])
            .isEqualTo(intentMimeType)
        assertThat(request.extras[extrasIntKey]).isEqualTo(extrasInt)
    }

    @Test
    fun testExtrasTakesPrecedenceOverIntentMimeType() {
        val intent = Intent()
        intent.type = "image/png"
        val extrasMimeType = "image/jpg"
        val request = DeepLinkRequest(intent, DeepLinkRequest.mimeTypeExtra(extrasMimeType))

        assertThat(request.extras.isNotEmpty()).isTrue()
        assertThat(request.extras[DeepLinkRequest.Companion.MimeTypeExtrasKey])
            .isEqualTo(extrasMimeType)
    }

    @Test
    fun testExtrasTakesPrecedenceOverIntentAction() {
        val intent = Intent()
        intent.action = "Intent.Action"
        val extrasAction = "Extras.Action"
        val request = DeepLinkRequest(intent, DeepLinkRequest.actionExtra(extrasAction))

        assertThat(request.extras.isNotEmpty()).isTrue()
        assertThat(request.extras[DeepLinkRequest.Companion.ActionExtrasKey])
            .isEqualTo(extrasAction)
    }

    @Test
    fun testExtrasIncludesIntentExtra() {
        val intent = Intent()

        val key = "key"
        val intentBundle = Bundle()
        intentBundle.putInt(key, 123)
        intent.putExtras(intentBundle)

        val request = DeepLinkRequest(intent)

        assertThat(request.extras.isNotEmpty()).isTrue()
        assertThat(request.extras[DeepLinkRequest.IntentExtrasKey]?.getInt(key)).isEqualTo(123)
    }
}
