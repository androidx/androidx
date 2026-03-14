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

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.webkit.JavascriptInterface
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.FrameLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.DialogFragment
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import androidx.webkit.WebViewRenderProcess
import androidx.webkit.WebViewRenderProcessClient
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/** An activity demonstrating the behaviour of renderer APIs. */
class RendererTerminationActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var status: TextView
    private lateinit var terminateButton: Button
    private lateinit var blockButton: Button
    private lateinit var blockTransientButton: Button
    private lateinit var unblockButton: Button
    private lateinit var blocker: JSBlocker
    private val rendererClientUsageEnabled =
        WebViewFeature.isFeatureSupported(WebViewFeature.WEB_VIEW_RENDERER_CLIENT_BASIC_USAGE)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_renderer_termination)
        setTitle(R.string.renderer_termination_activity_title)
        setUpDemoAppActivity()

        webView = findViewById(R.id.renderer_termination_webview)
        status = findViewById(R.id.renderer_termination_status)
        terminateButton = findViewById(R.id.renderer_termination_terminate_button)
        blockButton = findViewById(R.id.renderer_termination_block_button)
        blockTransientButton = findViewById(R.id.renderer_termination_block_transient_button)
        unblockButton = findViewById(R.id.renderer_termination_unblock_button)

        recreateWebView()

        if (!rendererClientUsageEnabled) {
            status.text = API_NOT_AVAILABLE
        }

        terminateButton.setOnClickListener(this::terminateWebViewRenderer)
        blockButton.setOnClickListener(this::blockWebViewRenderer)
        blockTransientButton.setOnClickListener { blockWebViewRenderer(10000) }
        unblockButton.setOnClickListener(this::unblockWebViewRenderer)
    }

    private fun recreateWebView() {
        val layout = webView.parent as FrameLayout
        val params = webView.layoutParams
        layout.removeView(webView)
        webView.destroy()
        webView = WebView(this)
        webView.layoutParams = params
        layout.addView(webView)

        webView.webViewClient = RendererTerminationWebViewClient()
        webView.settings.javaScriptEnabled = true
        if (this::blocker.isInitialized) {
            blocker.unblock()
        }
        blocker = JSBlocker()
        webView.addJavascriptInterface(blocker, INTERFACE_NAME)
        status.text = STARTED_TEXT

        webView.loadUrl(EXAMPLE_SITE)
        if (
            WebViewFeature.isFeatureSupported(WebViewFeature.WEB_VIEW_RENDERER_CLIENT_BASIC_USAGE)
        ) {

            WebViewCompat.setWebViewRenderProcessClient(
                webView,
                RendererUnresponsiveWebViewClient(),
            )
        }
    }

    private fun terminateWebViewRenderer(v: View) {
        if (rendererClientUsageEnabled) {
            WebViewCompat.getWebViewRenderProcess(webView)?.terminate()
        }
    }

    private fun blockWebViewRenderer(v: View) {
        blocker.beginBlocking()
    }

    @Suppress("DEPRECATION")
    private fun blockWebViewRenderer(duration: Long) {
        blocker.beginBlocking()
        Handler().postDelayed(blocker::unblock, duration)
    }

    private fun unblockWebViewRenderer(v: View) {
        blocker.unblock()
    }

    private fun evaluateBeginBlockingJavascript() {
        webView.evaluateJavascript("$INTERFACE_NAME.block();", null)
    }

    private fun updateButtonState(isBlocked: Boolean) {
        blockButton.isEnabled = !isBlocked
        blockTransientButton.isEnabled = !isBlocked
        unblockButton.isEnabled = isBlocked
        terminateButton.isEnabled = rendererClientUsageEnabled
    }

    private inner class RendererTerminationWebViewClient : WebViewClient() {
        override fun onRenderProcessGone(
            view: WebView?,
            detail: RenderProcessGoneDetail?,
        ): Boolean {
            webView.destroy()
            blocker.unblock()

            RendererTerminatedDialogFragment()
                .newInstance()
                .show(supportFragmentManager, DIALOG_UNRESPONSIVE_TEXT)
            status.text = TERMINATED_TEXT
            return true
        }
    }

    private inner class RendererUnresponsiveWebViewClient : WebViewRenderProcessClient() {
        override fun onRenderProcessUnresponsive(view: WebView, renderer: WebViewRenderProcess?) {
            status.text = UNRESPONSIVE_TEXT
            var dialog =
                supportFragmentManager.findFragmentByTag(UNRESPONSIVE_TEXT) as DialogFragment?
            if (dialog == null) {
                dialog = RendererUnresponsiveDialogFragment().newInstance()
                dialog.show(supportFragmentManager, UNRESPONSIVE_TEXT)
            }
        }

        override fun onRenderProcessResponsive(view: WebView, renderer: WebViewRenderProcess?) {
            status.text = RESPONSIVE_TEXT
            val dialog =
                supportFragmentManager.findFragmentByTag(DIALOG_UNRESPONSIVE_TEXT)
                    as DialogFragment?
            dialog?.dismiss()
        }
    }

    /**
     * Injected JavaScript interface that provides a blocking call.
     *
     * Uses a lock to wait until the notify call arrives, which blocks the renderer.
     */
    private inner class JSBlocker {

        private val lock = ReentrantLock()
        private val condition = lock.newCondition()
        var blocked = false

        init {
            updateButtonState(false)
        }

        fun unblock() {
            lock.withLock {
                blocked = false
                condition.signal()
                updateButtonState(false)
            }
        }

        fun beginBlocking() {
            lock.withLock {
                blocked = true
                evaluateBeginBlockingJavascript()
                updateButtonState(true)
            }
        }

        @Suppress("UNUSED") // used from JavaScript
        @JavascriptInterface
        fun block() {
            lock.withLock {
                while (blocked) {
                    condition.await()
                }
            }
        }
    }

    /** A renderer terminated {@link DialogFragment}. */
    class RendererTerminatedDialogFragment : DialogFragment() {
        /** Creates a new RendererTerminatedDialogFragment instance. */
        fun newInstance(): RendererTerminatedDialogFragment {
            return RendererTerminatedDialogFragment().apply { isCancelable = false }
        }

        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
            val v =
                LayoutInflater.from(activity).inflate(R.layout.fragment_renderer_terminated, null)
            return AlertDialog.Builder(activity!!)
                .setTitle(RENDERER_TERMINATED_TITLE)
                .setView(v)
                .setPositiveButton(android.R.string.ok) { _: DialogInterface, _: Int ->
                    (activity as RendererTerminationActivity).recreateWebView()
                }
                .create()
        }
    }

    /** A renderer unresponsive {@link DialogFragment}. */
    class RendererUnresponsiveDialogFragment : DialogFragment() {
        /** Creates a new RendererUnresponsiveDialogFragment instance. */
        fun newInstance(): RendererUnresponsiveDialogFragment {
            return RendererUnresponsiveDialogFragment().apply { isCancelable = false }
        }

        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
            val v =
                LayoutInflater.from(activity).inflate(R.layout.fragment_renderer_terminated, null)
            return AlertDialog.Builder(activity!!)
                .setTitle(RENDERER_UNRESPONSIVE_TITLE)
                .setView(v)
                .setNegativeButton(android.R.string.ok) { _: DialogInterface, _: Int ->
                    (activity as RendererTerminationActivity).terminateWebViewRenderer(v)
                }
                .setPositiveButton(WAIT_TEXT) { _, _ -> }
                .create()
        }
    }

    companion object {
        const val EXAMPLE_SITE = "http://www.wikipedia.org/wiki/Cat"
        const val TERMINATED_TEXT = "terminated"
        const val UNRESPONSIVE_TEXT = "unresponsive"
        const val DIALOG_UNRESPONSIVE_TEXT = "dialog-unresponsive"
        const val RESPONSIVE_TEXT = "responsive"
        const val STARTED_TEXT = "started"
        const val API_NOT_AVAILABLE = "API not available"
        const val WAIT_TEXT = "Wait"
        const val RENDERER_TERMINATED_TITLE = "Renderer Terminated"
        const val RENDERER_UNRESPONSIVE_TITLE = "Renderer Unresponsive"
        const val INTERFACE_NAME = "__blocker__"
    }
}
