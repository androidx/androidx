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

package androidx.web

import android.content.Context
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.junit.Assert.assertTrue

internal fun <T : WebContentView> WebContent.attachToActivity(
    activity: android.app.Activity,
    factory: (Context) -> T,
): T {
    val view = attach(activity, factory)
    activity.setContentView(
        view,
        android.view.ViewGroup.LayoutParams(
            android.view.ViewGroup.LayoutParams.MATCH_PARENT,
            android.view.ViewGroup.LayoutParams.MATCH_PARENT,
        ),
    )
    return view
}

internal fun WebContent.attachToActivity(activity: android.app.Activity): WebContentView {
    return attachToActivity(activity, ::WebContentView)
}

internal fun WebContent.detachFromView(view: WebContentView) {
    (view.parent as? android.view.ViewGroup)?.removeView(view)
    detach()
}

internal inline fun <reified A : android.app.Activity> ActivityScenario<A>.runOnActivityAndWait(
    timeoutSeconds: Long = 5,
    crossinline block: (activity: A, done: () -> Unit) -> Unit,
) {
    val latch = CountDownLatch(1)
    onActivity { activity -> block(activity) { latch.countDown() } }
    assertTrue(
        "Timed out after $timeoutSeconds seconds waiting for operation to finish",
        latch.await(timeoutSeconds, TimeUnit.SECONDS),
    )
}

internal inline fun WebContent.useOnMain(block: (WebContent) -> Unit) {
    try {
        block(this)
    } finally {
        InstrumentationRegistry.getInstrumentation().runOnMainSync { close() }
    }
}

internal class TestWebContentView(context: Context) : WebContentView(context) {
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

class TestActivity : android.app.Activity()

class TestActivity2 : android.app.Activity()

internal class OnPageFinishedClient(private val onPageFinished: () -> Unit) : WebViewClient() {
    override fun onPageFinished(view: WebView?, url: String?) {
        onPageFinished()
    }
}
