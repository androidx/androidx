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

package androidx.privacysandbox.ui.integration.sdkproviderutils

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.net.Uri
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import androidx.privacysandbox.ui.core.SandboxedUiAdapter
import androidx.privacysandbox.ui.provider.AbstractSandboxedUiAdapter
import androidx.webkit.WebViewAssetLoader
import androidx.webkit.WebViewClientCompat
import java.util.concurrent.Executor

class TestAdapters(private val sdkContext: Context) {
    inner class TestBannerAd(private val text: String, private val withSlowDraw: Boolean) :
        BannerAd() {
        override fun buildAdView(sessionContext: Context): View {
            return TestView(sessionContext, withSlowDraw, text)
        }
    }

    abstract class BannerAd() : AbstractSandboxedUiAdapter() {
        lateinit var sessionClientExecutor: Executor
        lateinit var sessionClient: SandboxedUiAdapter.SessionClient

        abstract fun buildAdView(sessionContext: Context): View?

        override fun openSession(
            context: Context,
            windowInputToken: IBinder,
            initialWidth: Int,
            initialHeight: Int,
            isZOrderOnTop: Boolean,
            clientExecutor: Executor,
            client: SandboxedUiAdapter.SessionClient,
        ) {
            sessionClientExecutor = clientExecutor
            sessionClient = client
            Handler(Looper.getMainLooper())
                .post(
                    Runnable lambda@{
                        Log.d(TAG, "Session requested")
                        val adView: View = buildAdView(context) ?: return@lambda
                        adView.layoutParams = ViewGroup.LayoutParams(initialWidth, initialHeight)
                        clientExecutor.execute { client.onSessionOpened(BannerAdSession(adView)) }
                    }
                )
        }

        private inner class BannerAdSession(private val adView: View) : AbstractSession() {
            override val view: View
                get() = adView

            override fun notifyResized(width: Int, height: Int) {
                Log.i(TAG, "Resized $width $height")
                view.layoutParams.width = width
                view.layoutParams.height = height
            }

            override fun notifyZOrderChanged(isZOrderOnTop: Boolean) {
                Log.i(TAG, "Z order changed")
            }

            override fun notifyConfigurationChanged(configuration: Configuration) {
                Log.i(TAG, "Configuration change")
            }

            override fun close() {
                Log.i(TAG, "Closing session")
            }
        }
    }

    inner class WebViewBannerAd : BannerAd() {
        private fun isAirplaneModeOn(): Boolean {
            return Settings.Global.getInt(
                sdkContext.contentResolver,
                Settings.Global.AIRPLANE_MODE_ON,
                0
            ) != 0
        }

        override fun buildAdView(sessionContext: Context): View? {
            if (isAirplaneModeOn()) {
                sessionClientExecutor.execute {
                    sessionClient.onSessionError(Throwable("Cannot load WebView in airplane mode."))
                }
                return null
            }
            val webView = WebView(sessionContext)
            customizeWebViewSettings(webView.settings)
            webView.loadUrl(GOOGLE_URL)
            return webView
        }
    }

    inner class WebViewAdFromLocalAssets : BannerAd() {
        override fun buildAdView(sessionContext: Context): View {
            val webView = WebView(sessionContext)
            val assetLoader =
                WebViewAssetLoader.Builder()
                    .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(sdkContext))
                    .addPathHandler("/res/", WebViewAssetLoader.ResourcesPathHandler(sdkContext))
                    .build()
            webView.webViewClient = LocalContentWebViewClient(assetLoader)
            customizeWebViewSettings(webView.settings)
            webView.loadUrl(LOCAL_WEB_VIEW_URL)
            return webView
        }
    }

    private inner class TestView(
        context: Context,
        private val withSlowDraw: Boolean,
        private val text: String
    ) : View(context) {

        private val viewColor = Color.rgb((0..255).random(), (0..255).random(), (0..255).random())

        @SuppressLint("BanThreadSleep")
        override fun onDraw(canvas: Canvas) {
            // We are adding sleep to test the synchronization of the app and the sandbox view's
            // size changes.
            if (withSlowDraw) {
                Thread.sleep(500)
            }
            super.onDraw(canvas)

            val paint = Paint()
            paint.textSize = 50F

            canvas.drawColor(viewColor)
            canvas.drawText(text, 75F, 75F, paint)

            setOnClickListener {
                Log.i(TAG, "Click on ad detected")
                val visitUrl = Intent(Intent.ACTION_VIEW)
                visitUrl.data = Uri.parse(GOOGLE_URL)
                visitUrl.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(visitUrl)
            }
        }
    }

    private inner class LocalContentWebViewClient(private val assetLoader: WebViewAssetLoader) :
        WebViewClientCompat() {
        override fun shouldInterceptRequest(
            view: WebView,
            request: WebResourceRequest
        ): WebResourceResponse? {
            return assetLoader.shouldInterceptRequest(request.url)
        }

        @Deprecated("Deprecated in Java")
        override fun shouldInterceptRequest(view: WebView, url: String): WebResourceResponse? {
            return assetLoader.shouldInterceptRequest(Uri.parse(url))
        }
    }

    private fun customizeWebViewSettings(settings: WebSettings) {
        settings.javaScriptEnabled = true
        settings.setGeolocationEnabled(true)
        settings.setSupportZoom(true)
        settings.databaseEnabled = true
        settings.domStorageEnabled = true
        settings.allowFileAccess = true
        settings.allowContentAccess = true

        // Default layout behavior for webbrowser in android.
        settings.useWideViewPort = true
        settings.loadWithOverviewMode = true
        settings.layoutAlgorithm = WebSettings.LayoutAlgorithm.TEXT_AUTOSIZING
    }

    companion object {
        private const val TAG = "TestSandboxSdk"
        private const val GOOGLE_URL = "https://www.google.com/"
        private const val LOCAL_WEB_VIEW_URL =
            "https://appassets.androidplatform.net/assets/www/webview-test.html"
    }
}
