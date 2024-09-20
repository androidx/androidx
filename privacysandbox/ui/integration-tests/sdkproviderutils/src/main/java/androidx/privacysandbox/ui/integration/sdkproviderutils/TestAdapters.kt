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
import android.graphics.Path
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.privacysandbox.ui.client.SandboxedUiAdapterFactory
import androidx.privacysandbox.ui.client.view.SandboxedSdkView
import androidx.privacysandbox.ui.core.SandboxedUiAdapter
import androidx.privacysandbox.ui.provider.AbstractSandboxedUiAdapter
import androidx.webkit.WebViewAssetLoader
import androidx.webkit.WebViewClientCompat
import java.util.concurrent.Executor

class TestAdapters(private val sdkContext: Context) {
    inner class TestBannerAd(private val text: String, private val withSlowDraw: Boolean) :
        BannerAd() {
        override fun buildAdView(sessionContext: Context, width: Int, height: Int): View? {
            return TestView(sessionContext, withSlowDraw, text)
        }
    }

    abstract class BannerAd() : AbstractSandboxedUiAdapter() {
        lateinit var sessionClientExecutor: Executor
        lateinit var sessionClient: SandboxedUiAdapter.SessionClient

        abstract fun buildAdView(sessionContext: Context, width: Int, height: Int): View?

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
                        val adView: View =
                            buildAdView(context, initialWidth, initialHeight) ?: return@lambda
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

        override fun buildAdView(sessionContext: Context, width: Int, height: Int): View? {
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

    inner class VideoBannerAd(private val playerViewProvider: PlayerViewProvider) : BannerAd() {

        override fun buildAdView(sessionContext: Context, width: Int, height: Int): View? {
            return playerViewProvider.createPlayerView(
                sessionContext,
                "https://html5demos.com/assets/dizzy.mp4"
            )
        }
    }

    inner class WebViewAdFromLocalAssets : BannerAd() {
        override fun buildAdView(sessionContext: Context, width: Int, height: Int): View? {
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

    inner class OverlaidAd(private val mediateeBundle: Bundle) : BannerAd() {
        override fun buildAdView(sessionContext: Context, width: Int, height: Int): View {
            val adapter = SandboxedUiAdapterFactory.createFromCoreLibInfo(mediateeBundle)
            val linearLayout = LinearLayout(sessionContext)
            linearLayout.orientation = LinearLayout.VERTICAL
            linearLayout.layoutParams = LinearLayout.LayoutParams(width, height)
            // The SandboxedSdkView will take up 90% of the parent height, with the overlay taking
            // the other 10%
            val ssvParams =
                LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 0.9f)
            val overlayParams =
                LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 0.1f)
            val sandboxedSdkView = SandboxedSdkView(sessionContext)
            sandboxedSdkView.setAdapter(adapter)
            sandboxedSdkView.layoutParams = ssvParams
            linearLayout.addView(sandboxedSdkView)
            val textView =
                TextView(sessionContext).also {
                    it.setBackgroundColor(Color.GRAY)
                    it.text = "Mediator Overlay"
                    it.textSize = 20f
                    it.setTextColor(Color.BLACK)
                    it.layoutParams = overlayParams
                }
            linearLayout.addView(textView)
            return linearLayout
        }
    }

    private inner class TestView(
        context: Context,
        private val withSlowDraw: Boolean,
        private val text: String
    ) : View(context) {

        init {
            setOnClickListener {
                Log.i(TAG, "Click on ad detected")
                val visitUrl = Intent(Intent.ACTION_VIEW)
                visitUrl.data = Uri.parse(GOOGLE_URL)
                visitUrl.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(visitUrl)
            }
        }

        private val viewColor = Color.rgb((0..255).random(), (0..255).random(), (0..255).random())

        // Map that attaches each pointer to its path
        private val pointerIdToPathMap = mutableMapOf<Int, Path>()

        private val paint = Paint()

        override fun onTouchEvent(event: MotionEvent): Boolean {
            super.onTouchEvent(event)
            when (event.actionMasked) {
                // A new pointer is down, keep track of it using its id, and create
                // new line (path) to draw
                MotionEvent.ACTION_DOWN,
                MotionEvent.ACTION_POINTER_DOWN -> {
                    val pointerIdToAdd = event.getPointerId(event.actionIndex)
                    val pathToAdd =
                        Path().apply {
                            moveTo(event.getX(event.actionIndex), event.getY(event.actionIndex))
                        }
                    pointerIdToPathMap[pointerIdToAdd] = pathToAdd
                }

                // Update paths as the pointers are moving
                MotionEvent.ACTION_MOVE -> {
                    for (i in 0 until event.pointerCount) {
                        val path = pointerIdToPathMap[event.getPointerId(i)]
                        path?.lineTo(event.getX(i), event.getY(i))
                    }
                }

                // Stop drawing path of pointer that is now up
                MotionEvent.ACTION_UP,
                MotionEvent.ACTION_POINTER_UP -> {
                    val pointerToRemove = event.getPointerId(event.actionIndex)
                    pointerIdToPathMap.remove(pointerToRemove)
                }
                else -> return false
            }
            invalidate()
            return true
        }

        @SuppressLint("BanThreadSleep")
        override fun onDraw(canvas: Canvas) {
            // We are adding sleep to test the synchronization of the app and the sandbox view's
            // size changes.
            if (withSlowDraw) {
                Thread.sleep(500)
            }
            super.onDraw(canvas)
            paint.textSize = 50F
            canvas.drawColor(viewColor)
            canvas.drawText(text, 75F, 75F, paint)
            pointerIdToPathMap.forEach { (_, path) -> canvas.drawPath(path, paint) }
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
