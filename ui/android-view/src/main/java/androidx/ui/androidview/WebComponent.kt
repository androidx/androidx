/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.ui.androidview

import android.print.PrintDocumentAdapter
import android.util.Log
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.webkit.WebView
import android.widget.FrameLayout
import com.google.r4a.Composable
import com.google.r4a.Model
import com.google.r4a.adapters.setLayoutHeight
import com.google.r4a.adapters.setLayoutWidth
import com.google.r4a.composer

class WebContext {

    companion object {
        val debug = true
    }

    public fun createPrintDocumentAdapter(documentName: String): PrintDocumentAdapter {
        return webView.createPrintDocumentAdapter(documentName)
    }

    internal var webView: WebView = WebView(composer.composer.context)
}

private fun FrameLayout.setComposeCallback(composeCallback: (FrameLayout)->Unit) {
    composeCallback(this)
}

@Composable
fun WebComponent(url: String, webContext: WebContext) {
    if (WebContext.debug) {
        Log.d("WebComponent", "WebComponent compose " + url)
    }

    <FrameLayout
        layoutWidth=MATCH_PARENT
        layoutHeight=MATCH_PARENT
        composeCallback={
            if (WebContext.debug) {
                Log.d("WebComponent", "WebComponent composeCallback")
            }

            val webView = webContext.webView
            val parent = webView.parent
            if (parent != it) {
                if (parent is ViewGroup) {
                    parent.removeView(webView)
                }
                it.addView(webView, ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT))
            }

            if (!url.equals(webView.url)) {
                if (WebContext.debug) {
                    Log.d("WebComponent", "WebComponent load url")
                }
                webView.loadUrl(url)
            }
        } />
}
