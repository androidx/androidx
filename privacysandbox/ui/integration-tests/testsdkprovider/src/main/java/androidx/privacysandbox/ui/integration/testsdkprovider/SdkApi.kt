/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.privacysandbox.ui.integration.testsdkprovider

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import androidx.privacysandbox.ui.core.SandboxedUiAdapter
import androidx.privacysandbox.ui.integration.testaidl.ISdkApi
import androidx.privacysandbox.ui.provider.toCoreLibInfo
import java.util.concurrent.Executor

class SdkApi(sdkContext: Context) : ISdkApi.Stub() {
    private var mContext: Context? = null

    init {
        mContext = sdkContext
    }

    override fun loadAd(isWebView: Boolean): Bundle {
        return BannerAd(isWebView).toCoreLibInfo(mContext!!)
    }

    private inner class BannerAd(private val isWebView: Boolean) : SandboxedUiAdapter {
        override fun openSession(
            context: Context,
            initialWidth: Int,
            initialHeight: Int,
            isZOrderOnTop: Boolean,
            clientExecutor: Executor,
            client: SandboxedUiAdapter.SessionClient
        ) {
            Log.d(TAG, "Session requested")
            lateinit var adView: View
            if (isWebView) {
                val webView = WebView(context)
                webView.loadUrl(AD_URL)
                webView.layoutParams = ViewGroup.LayoutParams(
                    initialWidth, initialHeight
                )
                adView = webView
            } else {
                adView = TestView(context)
            }
            clientExecutor.execute {
                client.onSessionOpened(BannerAdSession(adView))
            }
        }

        private inner class BannerAdSession(private val adView: View) : SandboxedUiAdapter.Session {
            override val view: View
                get() = adView

            override fun notifyResized(width: Int, height: Int) {
                Log.i(TAG, "Resized $width $height")
                view.layoutParams = ViewGroup.LayoutParams(width, height)
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

    private inner class TestView(context: Context) : View(context) {
        override fun onDraw(canvas: Canvas?) {
            super.onDraw(canvas)

            val paint = Paint()
            paint.textSize = 50F
            canvas!!.drawColor(
                Color.rgb((0..255).random(), (0..255).random(), (0..255).random())
            )
            canvas.drawText("Hey", 75F, 75F, paint)

            setOnClickListener {
                Log.i(TAG, "Click on ad detected")
                val visitUrl = Intent(Intent.ACTION_VIEW)
                visitUrl.data = Uri.parse(AD_URL)
                visitUrl.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                mContext!!.startActivity(visitUrl)
            }
        }

        override fun onConfigurationChanged(newConfig: Configuration?) {
            Log.i(TAG, "View notification - configuration of the app has changed")
        }
    }

    companion object {
        private const val TAG = "TestSandboxSdk"
        private const val AD_URL = "http://www.google.com/"
    }
}
