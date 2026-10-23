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

package androidx.webkit

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.webkit.test.common.WebViewOnUiThread
import androidx.webkit.test.common.WebkitUtils
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Tests for [WebViewCompat.setGoogleAccountsCallback] and
 * [WebViewCompat.clearGoogleAccountsCallback].
 */
@MediumTest
@RunWith(AndroidJUnit4::class)
class GoogleAccountsCallbackTest {
    private lateinit var webViewOnUiThread: WebViewOnUiThread

    @Before
    fun setUp() {
        webViewOnUiThread = WebViewOnUiThread()
    }

    @After
    fun tearDown() {
        webViewOnUiThread.cleanUp()
    }

    @Test
    fun setAndClearGoogleAccountsCallback_doesNotCrash() {
        WebkitUtils.checkFeature(WebViewFeature.GOOGLE_ACCOUNTS_CALLBACK_EXPERIMENTAL_V1)
        WebkitUtils.onMainThreadSync {
            val webView = webViewOnUiThread.webViewOnCurrentThread
            WebViewCompat.setGoogleAccountsCallback(webView, "test_account_id", Runnable::run) {}
            WebViewCompat.setGoogleAccountsCallback(webView, null) {}
            WebViewCompat.clearGoogleAccountsCallback(webView)
        }
    }
}
