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
import android.graphics.Color.BLACK
import android.graphics.Color.WHITE
import android.graphics.Paint
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Process
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.ToggleButton
import androidx.privacysandbox.ui.client.SandboxedUiAdapterFactory
import androidx.privacysandbox.ui.client.view.SandboxedSdkView
import androidx.privacysandbox.ui.core.SandboxedUiAdapter
import androidx.privacysandbox.ui.core.SessionData
import androidx.privacysandbox.ui.integration.sdkproviderutils.SdkApiConstants.Companion.AUTOMATED_TEST_CALLBACK
import androidx.privacysandbox.ui.provider.AbstractSandboxedUiAdapter
import androidx.webkit.WebViewAssetLoader
import androidx.webkit.WebViewClientCompat
import java.util.concurrent.Executor
import kotlin.random.Random

class TestAdapters(private val sdkContext: Context) {
    abstract class BannerAd(automatedTestCallbackBundle: Bundle = Bundle()) :
        AbstractSandboxedUiAdapter() {
        lateinit var sessionClientExecutor: Executor
        lateinit var sessionClient: SandboxedUiAdapter.SessionClient
        lateinit var adViewWithConsumeScrollOverlay: AdViewWithConsumeScrollOverlay
        val mainLooperHandler = Handler(Looper.getMainLooper())
        val automatedTestCallbackBinder =
            automatedTestCallbackBundle.getBinder(AUTOMATED_TEST_CALLBACK)
        val automatedTestCallback: IAutomatedTestCallback? =
            automatedTestCallbackBinder?.let { IAutomatedTestCallback.Stub.asInterface(it) }
        var shouldAddAllowAppToScrollOverlay = true

        abstract fun buildAdView(sessionContext: Context, width: Int, height: Int): View?

        override fun openSession(
            context: Context,
            sessionData: SessionData,
            initialWidth: Int,
            initialHeight: Int,
            isZOrderOnTop: Boolean,
            clientExecutor: Executor,
            client: SandboxedUiAdapter.SessionClient,
        ) {
            sessionClientExecutor = clientExecutor
            sessionClient = client
            mainLooperHandler.post(
                Runnable lambda@{
                    Log.d(TAG, "Session requested")
                    var adView: View =
                        buildAdView(context, initialWidth, initialHeight) ?: return@lambda
                    adView.layoutParams = ViewGroup.LayoutParams(initialWidth, initialHeight)
                    if (shouldAddAllowAppToScrollOverlay) {
                        adViewWithConsumeScrollOverlay =
                            AdViewWithConsumeScrollOverlay(
                                context,
                                initialWidth,
                                initialHeight,
                                adView,
                            )
                        if (isZOrderOnTop) {
                            adViewWithConsumeScrollOverlay.hideOverlay()
                        }
                        adView = adViewWithConsumeScrollOverlay
                    }
                    clientExecutor.execute {
                        if (
                            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                Process.isSdkSandbox()
                        ) {
                            automatedTestCallback?.onRemoteSession()
                        }
                        client.onSessionOpened(BannerAdSession(adView))
                    }
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
                Log.i(TAG, "Z order changed to (${if (isZOrderOnTop) "z-above" else "z-below"})")
                if (isZOrderOnTop) {
                    adViewWithConsumeScrollOverlay.hideOverlay()
                } else {
                    adViewWithConsumeScrollOverlay.showOverlay()
                }
            }

            override fun notifyConfigurationChanged(configuration: Configuration) {
                Log.i(TAG, "Configuration change")
                automatedTestCallback?.onConfigurationChanged(configuration)
            }

            override fun close() {
                Log.i(TAG, "Closing session")
            }
        }

        inner class AdViewWithConsumeScrollOverlay(
            context: Context,
            initialWidth: Int,
            initialHeight: Int,
            adView: View,
        ) : FrameLayout(context) {
            private val consumeScrollOverlay: ViewGroup
            private var allowAppToScroll = true

            init {
                layoutParams = ViewGroup.LayoutParams(initialWidth, initialHeight)
                adView.layoutParams =
                    LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
                addView(adView)

                consumeScrollOverlay = createConsumeScrollOverlay()
                addView(consumeScrollOverlay)
                consumeScrollOverlay.bringToFront()
            }

            override fun onInterceptTouchEvent(motionEvent: MotionEvent): Boolean {
                if (motionEvent.action == MotionEvent.ACTION_DOWN) {
                    requestDisallowInterceptTouchEvent(!allowAppToScroll)
                }
                return super.onInterceptTouchEvent(motionEvent)
            }

            fun createConsumeScrollOverlay(): ViewGroup {
                val linearLayout = LinearLayout(context)
                linearLayout.layoutParams =
                    LayoutParams(
                        LayoutParams.WRAP_CONTENT,
                        LayoutParams.WRAP_CONTENT,
                        Gravity.BOTTOM or Gravity.RIGHT,
                    )
                linearLayout.orientation = LinearLayout.HORIZONTAL
                linearLayout.setPadding(10, 10, 10, 10)
                linearLayout.setBackgroundColor(BLACK)

                val textView = TextView(context)
                textView.text = "Allow App to scroll?"
                textView.layoutParams =
                    LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
                textView.setTextColor(WHITE)
                linearLayout.addView(textView)

                val toggleButton = ToggleButton(context)
                toggleButton.layoutParams =
                    LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
                toggleButton.isChecked = true
                toggleButton.setOnCheckedChangeListener { _, isChecked ->
                    allowAppToScroll = isChecked
                }
                linearLayout.addView(toggleButton)

                return linearLayout
            }

            fun hideOverlay() {
                mainLooperHandler.post { consumeScrollOverlay.visibility = INVISIBLE }
            }

            fun showOverlay() {
                mainLooperHandler.post { consumeScrollOverlay.visibility = VISIBLE }
            }
        }
    }

    inner class TestBannerAd(
        private val text: String,
        private val withSlowDraw: Boolean,
        automatedTestCallbackBundle: Bundle = Bundle(),
    ) : BannerAd(automatedTestCallbackBundle) {
        override fun buildAdView(sessionContext: Context, width: Int, height: Int): View? {
            return TestView(sessionContext, withSlowDraw, text, automatedTestCallback)
        }
    }

    inner class WebViewBannerAd : BannerAd() {
        private fun isAirplaneModeOn(): Boolean {
            return Settings.Global.getInt(
                sdkContext.contentResolver,
                Settings.Global.AIRPLANE_MODE_ON,
                0,
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
                "https://html5demos.com/assets/dizzy.mp4",
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

    inner class ScrollViewAd(
        automatedTestCallbackBundle: Bundle = Bundle(),
        private val appCanScroll: Boolean = true,
    ) : BannerAd(automatedTestCallbackBundle) {
        override fun buildAdView(sessionContext: Context, width: Int, height: Int): View? {
            shouldAddAllowAppToScrollOverlay = false
            val scrollView =
                ScrollView(sessionContext).apply {
                    layoutParams =
                        ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                        )
                }
            var initialScrollPositionX = 0f
            var initialScrollPositionY = 0f

            scrollView.setOnTouchListener { _, motionEvent ->
                if (motionEvent.action == MotionEvent.ACTION_DOWN) {
                    initialScrollPositionX = scrollView.scrollX.toFloat()
                    initialScrollPositionY = scrollView.scrollY.toFloat()
                    scrollView.requestDisallowInterceptTouchEvent(!appCanScroll)
                }
                if (motionEvent.action == MotionEvent.ACTION_UP) {
                    val scrollX = scrollView.scrollX.toFloat() - initialScrollPositionX
                    val scrollY = scrollView.scrollY.toFloat() - initialScrollPositionY
                    automatedTestCallback?.onGestureFinished(scrollX, scrollY)
                }
                false
            }

            val linearLayout =
                LinearLayout(sessionContext).apply {
                    orientation = LinearLayout.VERTICAL
                    layoutParams =
                        ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                        )
                }

            for (i in 1..20) {
                val randomColor =
                    Color.rgb(Random.nextInt(256), Random.nextInt(256), Random.nextInt(256))

                val textView =
                    TextView(sessionContext).apply {
                        text = "RemoteItem $i"
                        setBackgroundColor(randomColor)
                        setTextColor(WHITE)
                        textSize = 18f
                        setPadding(50, 50, 50, 50)

                        layoutParams =
                            LinearLayout.LayoutParams(
                                    LinearLayout.LayoutParams.WRAP_CONTENT,
                                    LinearLayout.LayoutParams.WRAP_CONTENT,
                                )
                                .apply { setMargins(20, 20, 20, 20) }
                    }

                linearLayout.addView(textView)
            }
            scrollView.addView(linearLayout)

            return scrollView
        }
    }

    private inner class TestView(
        context: Context,
        private val withSlowDraw: Boolean,
        private val text: String,
        private val automatedTestCallback: IAutomatedTestCallback? = null,
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
        private val paint = Paint()
        private var isFirstLayout = true

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
        }

        override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
            super.onLayout(changed, left, top, right, bottom)
            if (isFirstLayout) {
                isFirstLayout = false
            } else {
                automatedTestCallback?.onResizeOccurred(right - left, bottom - top)
            }
        }
    }

    private inner class LocalContentWebViewClient(private val assetLoader: WebViewAssetLoader) :
        WebViewClientCompat() {
        override fun shouldInterceptRequest(
            view: WebView,
            request: WebResourceRequest,
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
