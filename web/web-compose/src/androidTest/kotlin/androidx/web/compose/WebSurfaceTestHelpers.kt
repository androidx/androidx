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

package androidx.web.compose

import android.content.Context
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.test.platform.app.InstrumentationRegistry
import androidx.web.WebContentView
import androidx.web.compose.WebSurfaceTest.Companion.TIMEOUT_MS
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue

internal fun WebContentView.loadHtml(composeTestRule: ComposeContentTestRule, html: String) {
    val latch = CountDownLatch(1)
    composeTestRule.runOnIdle {
        settings.javaScriptEnabled = true
        webViewClient = OnPageFinishedClient {
            postVisualStateCallback(
                0L,
                object : WebView.VisualStateCallback() {
                    override fun onComplete(requestId: Long) {
                        latch.countDown()
                    }
                },
            )
        }
        loadDataWithBaseURL("https://example.com/", html, "text/html", "utf-8", null)
    }
    assertTrue(latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS))
}

internal fun WebContentView.evaluateJs(script: String): String {
    val queue = ArrayBlockingQueue<String>(1)
    InstrumentationRegistry.getInstrumentation().runOnMainSync {
        evaluateJavascript(script) { queue.add(it ?: "null") }
    }
    val result = queue.poll(TIMEOUT_MS, TimeUnit.MILLISECONDS)
    assertNotNull(result)
    return result!!
}

internal class TestWebViewBridge(context: Context) : WebViewBridge(context) {
    var onCreateInputConnectionCallback: (() -> Unit)? = null
    var inputConnection: InputConnection? = null

    override fun onCreateInputConnection(outAttrs: EditorInfo): InputConnection? {
        val ic = super.onCreateInputConnection(outAttrs)
        inputConnection = ic
        if (ic != null) {
            onCreateInputConnectionCallback?.invoke()
        }
        return ic
    }
}

internal class OnPageFinishedClient(private val onPageFinished: () -> Unit) : WebViewClient() {
    override fun onPageFinished(view: WebView?, url: String?) {
        onPageFinished()
    }
}
