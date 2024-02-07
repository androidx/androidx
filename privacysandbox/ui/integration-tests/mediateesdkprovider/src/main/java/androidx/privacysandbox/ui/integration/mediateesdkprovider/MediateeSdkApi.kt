/*
 * Copyright (C) 2024 The Android Open Source Project
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

package androidx.privacysandbox.ui.integration.mediateesdkprovider

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import android.view.View
import androidx.privacysandbox.ui.core.SandboxedUiAdapter
import androidx.privacysandbox.ui.integration.testaidl.IMediateeSdkApi
import androidx.privacysandbox.ui.provider.toCoreLibInfo
import java.util.concurrent.Executor

class MediateeSdkApi(val sdkContext: Context) : IMediateeSdkApi.Stub() {
    private val handler = Handler(Looper.getMainLooper())

    override fun loadTestAdWithWaitInsideOnDraw(count: Int): Bundle {
        var bannerAd = BannerAd(count)
        return bannerAd.toCoreLibInfo(sdkContext)
    }

    // TODO(b/321830843) : Move logic to a helper file
    private inner class BannerAd(
        private val count: Int
    ) : SandboxedUiAdapter {
        lateinit var sessionClientExecutor: Executor
        lateinit var sessionClient: SandboxedUiAdapter.SessionClient
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
            handler.post(Runnable lambda@{
                Log.d(TAG, "Session requested")
                var adView = TestView(context, count)
                clientExecutor.execute {
                    client.onSessionOpened(BannerAdSession(adView))
                }
            })
        }

        private inner class BannerAdSession(
            private val adView: View
        ) : SandboxedUiAdapter.Session {
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

    // TODO(b/321830843) : Move logic to a helper file
    private inner class TestView(
        context: Context,
        private val count: Int
    ) : View(context) {

        @SuppressLint("BanThreadSleep")
        override fun onDraw(canvas: Canvas) {
            // We are adding sleep to test the synchronization of the app and the sandbox view's
            // size changes.
            Thread.sleep(500)
            super.onDraw(canvas)

            val paint = Paint()
            paint.textSize = 50F
            canvas.drawColor(
                Color.rgb((0..255).random(), (0..255).random(), (0..255).random())
            )

            val text = "Mediated Ad #$count"
            canvas.drawText(text, 75F, 75F, paint)

            setOnClickListener {
                Log.i(TAG, "Click on ad detected")
                val visitUrl = Intent(Intent.ACTION_VIEW)
                visitUrl.data = Uri.parse(AD_URL)
                visitUrl.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                sdkContext.startActivity(visitUrl)
            }
        }

        override fun onConfigurationChanged(newConfig: Configuration?) {
            Log.i(TAG, "View notification - configuration of the app has changed")
        }
    }

    companion object {
        private const val TAG = "TestSandboxSdk"
        private const val AD_URL = "https://www.google.com/"
    }
}
