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

package com.example.androidx.webkit

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity

/** An Activity to demonstrate how to properly display fullscreen web content with WebView. */
class FullscreenActivity : AppCompatActivity() {

    /**
     * An example {@link WebChromeClient} implementation which supports showing web content in
     * fullscreen.
     */
    private class FullScreenWebChromeClient(val window: Window) : WebChromeClient() {

        // Store the View passed from onShowCustomView in a member variable, because we need to
        // access this again during onHideCustomView().
        var fullScreenView: View? = null

        // Optional: store the CustomViewCallback in a member variable in case the app needs to
        // force WebView to exit fullscreen mode.
        var customViewCallback: CustomViewCallback? = null

        override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
            // At this point, the WebView is no longer drawing the content. We should cover it up
            // with the new View.
            fullScreenView = view
            customViewCallback = callback
            addDeprecatedFullScreenFlag()
            window.addContentView(
                fullScreenView,
                FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                ),
            )
        }

        @Suppress("DEPRECATION")
        private fun addDeprecatedFullScreenFlag() {
            window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        }

        @Suppress("DEPRECATION")
        private fun clearDeprecatedFullScreenFlag() {
            window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        }

        override fun onHideCustomView() {
            // At this point, mFullScreenView is no longer drawing content. Remove this from the
            // layout to show the underlying WebView, and remove the reference to the View so it can
            // be GC'ed.
            clearDeprecatedFullScreenFlag()
            (fullScreenView?.parent as? ViewGroup)?.removeView(fullScreenView)
            fullScreenView = null
            customViewCallback = null
        }

        /* package */
        fun exitFullScreen(): Unit? = customViewCallback?.onCustomViewHidden()

        /* package */
        fun isFullScreenMode(): Boolean = fullScreenView != null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_fullscreen)
        setTitle(R.string.fullscreen_activity_title)
        setUpDemoAppActivity()

        val fullScreenClient = FullScreenWebChromeClient(window)
        val webView =
            findViewById<WebView>(R.id.webview_supports_fullscreen).apply {
                settings.javaScriptEnabled = true
                webChromeClient = fullScreenClient
                webViewClient = WebViewClient()
                loadUrl(EXAMPLE_SITE)
            }

        this@FullscreenActivity.onBackPressedDispatcher.addCallback(
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    when {
                        fullScreenClient.isFullScreenMode() -> fullScreenClient.exitFullScreen()
                        webView.canGoBack() -> webView.goBack()
                        else -> handleOnBackPressed()
                    }
                }
            }
        )
    }

    companion object {
        // Use YouTube as an example website, but any site with a video player should suffice to
        // demonstrate fullscreen usage.
        private const val EXAMPLE_SITE = "https://youtube.com/"
    }
}
