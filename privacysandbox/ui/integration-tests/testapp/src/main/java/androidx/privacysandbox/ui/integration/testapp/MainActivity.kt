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
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.RequiresExtension
import androidx.appcompat.app.AppCompatActivity
import androidx.privacysandbox.sdkruntime.client.SdkSandboxManagerCompat
import androidx.privacysandbox.sdkruntime.core.LoadSdkCompatException
import androidx.privacysandbox.sdkruntime.core.SandboxedSdkCompat
import androidx.privacysandbox.ui.client.SandboxedUiAdapterFactory
import androidx.privacysandbox.ui.client.view.SandboxedSdkUiSessionState
import androidx.privacysandbox.ui.client.view.SandboxedSdkUiSessionStateChangedListener
import androidx.privacysandbox.ui.client.view.SandboxedSdkView
import androidx.privacysandbox.ui.integration.testaidl.ISdkApi
import com.google.android.material.switchmaterial.SwitchMaterial
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private lateinit var mSdkSandboxManager: SdkSandboxManagerCompat

    private var mSdkLoaded = false
    private lateinit var sdkApi: ISdkApi

    private lateinit var webViewBannerView: SandboxedSdkView
    private lateinit var bottomBannerView: SandboxedSdkView
    private lateinit var resizableBannerView: SandboxedSdkView
    private lateinit var newAdButton: Button
    private lateinit var resizeButton: Button
    private lateinit var resizeSdkButton: Button
    private lateinit var mediationSwitch: SwitchMaterial
    private lateinit var localWebViewToggle: SwitchMaterial

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
                    mSdkSandboxManager.loadSdk(MEDIATEE_SDK_NAME, Bundle())
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
        sdkApi = ISdkApi.Stub.asInterface(sandboxedSdk.getInterface())

        webViewBannerView = findViewById(R.id.webview_ad_view)
        bottomBannerView = SandboxedSdkView(this@MainActivity)
        resizableBannerView = findViewById(R.id.resizable_ad_view)
        newAdButton = findViewById(R.id.new_ad_button)
        resizeButton = findViewById(R.id.resize_button)
        resizeSdkButton = findViewById(R.id.resize_sdk_button)
        mediationSwitch = findViewById(R.id.mediation_switch)
        localWebViewToggle = findViewById(R.id.local_to_internet_switch)

        loadWebViewBannerAd()
        loadBottomBannerAd()
        loadResizableBannerAd()
    }

    private fun loadWebViewBannerAd() {
        webViewBannerView.addStateChangedListener(StateChangeListener(webViewBannerView))
        webViewBannerView.setAdapter(SandboxedUiAdapterFactory.createFromCoreLibInfo(
            sdkApi.loadLocalWebViewAd()
        ))

        localWebViewToggle.setOnCheckedChangeListener { _: View, isChecked: Boolean ->
            if (isChecked) {
                webViewBannerView.setAdapter(SandboxedUiAdapterFactory.createFromCoreLibInfo(
                    sdkApi.loadLocalWebViewAd()
                ))
            } else {
                webViewBannerView.setAdapter(SandboxedUiAdapterFactory.createFromCoreLibInfo(
                    sdkApi.loadWebViewAd()
                ))
            }
        }
    }

    private fun loadBottomBannerAd() {
        bottomBannerView.addStateChangedListener(StateChangeListener(bottomBannerView))
        bottomBannerView.layoutParams = findViewById<LinearLayout>(
            R.id.bottom_banner_container).layoutParams
        runOnUiThread {
            findViewById<LinearLayout>(R.id.bottom_banner_container).addView(bottomBannerView)
        }
        bottomBannerView.setAdapter(SandboxedUiAdapterFactory.createFromCoreLibInfo(
            sdkApi.loadTestAd(/*text=*/ "Hey!")
        ))
    }

    private fun loadResizableBannerAd() {
        resizableBannerView.addStateChangedListener(
            StateChangeListener(resizableBannerView))
        resizableBannerView.setAdapter(SandboxedUiAdapterFactory.createFromCoreLibInfo(
            sdkApi.loadTestAdWithWaitInsideOnDraw(/*text=*/ "Resizable View")
        ))

        var count = 1
        newAdButton.setOnClickListener {
            if (mediationSwitch.isChecked) {
                resizableBannerView.setAdapter(
                    SandboxedUiAdapterFactory.createFromCoreLibInfo(
                        sdkApi.loadMediatedTestAd(count)
                ))
            } else {
                resizableBannerView.setAdapter(
                    SandboxedUiAdapterFactory.createFromCoreLibInfo(
                        sdkApi.loadTestAdWithWaitInsideOnDraw(/*text=*/ "Ad #$count")
                ))
            }
            count++
        }

        val maxWidthPixels = 1000
        val maxHeightPixels = 1000
        val newSize = { currentSize: Int, maxSize: Int ->
            (currentSize + (100..200).random()) % maxSize
        }

        resizeButton.setOnClickListener {
            val newWidth = newSize(resizableBannerView.width, maxWidthPixels)
            val newHeight = newSize(resizableBannerView.height, maxHeightPixels)
            resizableBannerView.layoutParams =
                resizableBannerView.layoutParams.apply {
                    width = newWidth
                    height = newHeight
            }
        }

        resizeSdkButton.setOnClickListener {
            val newWidth = newSize(resizableBannerView.width, maxWidthPixels)
            val newHeight = newSize(resizableBannerView.height, maxHeightPixels)
            sdkApi.requestResize(newWidth, newHeight)
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
        private const val MEDIATEE_SDK_NAME =
            "androidx.privacysandbox.ui.integration.mediateesdkprovider"
    }
}
