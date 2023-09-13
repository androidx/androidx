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

package androidx.privacysandbox.ui.integration.testapp

import android.os.Bundle
import android.os.ext.SdkExtensions
import android.util.Log
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.RequiresExtension
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.marginBottom
import androidx.core.view.marginLeft
import androidx.core.view.marginRight
import androidx.core.view.marginTop
import androidx.privacysandbox.sdkruntime.client.SdkSandboxManagerCompat
import androidx.privacysandbox.sdkruntime.core.LoadSdkCompatException
import androidx.privacysandbox.sdkruntime.core.SandboxedSdkCompat
import androidx.privacysandbox.ui.client.SandboxedUiAdapterFactory
import androidx.privacysandbox.ui.client.view.SandboxedSdkUiSessionState
import androidx.privacysandbox.ui.client.view.SandboxedSdkUiSessionStateChangedListener
import androidx.privacysandbox.ui.client.view.SandboxedSdkView
import androidx.privacysandbox.ui.integration.testaidl.ISdkApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private lateinit var mSdkSandboxManager: SdkSandboxManagerCompat

    private var mSdkLoaded = false

    private lateinit var mSandboxedSdkView1: SandboxedSdkView
    private lateinit var mSandboxedSdkView2: SandboxedSdkView
    private lateinit var mSandboxedSdkView3: SandboxedSdkView
    private lateinit var mNewAdButton: Button
    private lateinit var mResizeButton: Button

    // TODO(b/257429573): Remove this line once fixed.
    @RequiresExtension(extension = SdkExtensions.AD_SERVICES, version = 5)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mSdkSandboxManager = SdkSandboxManagerCompat.from(applicationContext)

        if (!mSdkLoaded) {
            Log.i(TAG, "Loading SDK")
            CoroutineScope(Dispatchers.Default).launch {
                try {
                    val loadedSdk = mSdkSandboxManager.loadSdk(SDK_NAME, Bundle())
                    onLoadedSdk(loadedSdk)
                } catch (e: LoadSdkCompatException) {
                    Log.i(TAG, "loadSdk failed with errorCode: " + e.loadSdkErrorCode +
                        " and errorMsg: " + e.message)
                }
            }
        }
    }
    private fun onLoadedSdk(sandboxedSdk: SandboxedSdkCompat) {
        Log.i(TAG, "Loaded successfully")
        mSdkLoaded = true
        val sdkApi = ISdkApi.Stub.asInterface(sandboxedSdk.getInterface())

        mSandboxedSdkView1 = findViewById(R.id.rendered_view)
        mSandboxedSdkView1.addStateChangedListener(StateChangeListener(mSandboxedSdkView1))
        mSandboxedSdkView1.setAdapter(SandboxedUiAdapterFactory.createFromCoreLibInfo(
            sdkApi.loadAd(/*isWebView=*/ true, /*text=*/ "", /*withSlowDraw*/ false)
        ))

        mSandboxedSdkView2 = SandboxedSdkView(this@MainActivity)
        mSandboxedSdkView2.addStateChangedListener(StateChangeListener(mSandboxedSdkView2))
        mSandboxedSdkView2.layoutParams = findViewById<LinearLayout>(
            R.id.bottom_banner_container).layoutParams
        runOnUiThread {
            findViewById<LinearLayout>(R.id.bottom_banner_container).addView(mSandboxedSdkView2)
        }
        mSandboxedSdkView2.setAdapter(SandboxedUiAdapterFactory.createFromCoreLibInfo(
            sdkApi.loadAd(/*isWebView=*/ false, /*text=*/ "Hey!", /*withSlowDraw*/ false)
        ))

        mSandboxedSdkView3 = findViewById(R.id.new_ad_view)
        mSandboxedSdkView3.addStateChangedListener(StateChangeListener(mSandboxedSdkView3))

        mNewAdButton = findViewById(R.id.new_ad_button)

        mSandboxedSdkView3.setAdapter(SandboxedUiAdapterFactory.createFromCoreLibInfo(
            sdkApi.loadAd(/*isWebView=*/ false, /*text=*/ "Resize view",
                /*withSlowDraw*/ true)))

        var count = 1
        mNewAdButton.setOnClickListener {
            mSandboxedSdkView3.setAdapter(SandboxedUiAdapterFactory.createFromCoreLibInfo(
                sdkApi.loadAd(/*isWebView=*/ false, /*text=*/ "Ad #$count",
                    /*withSlowDraw*/ true)))
            count++
        }

        mResizeButton = findViewById(R.id.resize_button)
        val sizeIncrementPixels = 150
        mResizeButton.setOnClickListener {
            var newHeight = (mSandboxedSdkView3.height + sizeIncrementPixels) % 1000
            var newWidth = (mSandboxedSdkView3.width + sizeIncrementPixels) % 1000
            val marginLeft = mSandboxedSdkView3.marginLeft
            val marginRight = mSandboxedSdkView3.marginRight
            val marginTop = mSandboxedSdkView3.marginTop
            val marginBottom = mSandboxedSdkView3.marginBottom
            val layoutParams = LinearLayout.LayoutParams(newHeight, newWidth)
            layoutParams.setMargins(marginLeft, marginTop, marginRight, marginBottom)
            mSandboxedSdkView3.layoutParams = layoutParams
        }
    }

    private inner class StateChangeListener(val view: SandboxedSdkView) :
        SandboxedSdkUiSessionStateChangedListener {
        override fun onStateChanged(state: SandboxedSdkUiSessionState) {
            Log.i(TAG, "UI session state changed to: " + state.toString())
            if (state is SandboxedSdkUiSessionState.Error) {
                // If the session fails to open, display the error.
                val parent = view.parent as ViewGroup
                val index = parent.indexOfChild(view)
                val textView = TextView(this@MainActivity)
                textView.text = state.throwable.message

                runOnUiThread {
                    parent.removeView(view)
                    parent.addView(textView, index)
                }
            }
        }
    }

    companion object {
        private const val TAG = "TestSandboxClient"

        /**
         * Name of the SDK to be loaded.
         */
        private const val SDK_NAME = "androidx.privacysandbox.ui.integration.testsdkprovider"
    }
}
