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

import android.app.sdksandbox.LoadSdkException
import android.app.sdksandbox.SandboxedSdk
import android.app.sdksandbox.SdkSandboxManager
import android.content.res.Configuration
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.OutcomeReceiver
import android.os.ext.SdkExtensions
import android.util.Log
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.RequiresExtension
import androidx.privacysandbox.ui.client.SandboxedUiAdapterFactory
import androidx.privacysandbox.ui.client.view.SandboxedSdkUiSessionState
import androidx.privacysandbox.ui.client.view.SandboxedSdkUiSessionStateChangedListener
import androidx.privacysandbox.ui.client.view.SandboxedSdkView
import androidx.privacysandbox.ui.integration.testaidl.ISdkApi

class MainActivity : AppCompatActivity() {
    private lateinit var mSdkSandboxManager: SdkSandboxManager

    private lateinit var mSandboxedSdk: SandboxedSdk

    private var mSdkLoaded = false

    private lateinit var mSandboxedSdkView1: SandboxedSdkView
    private lateinit var mSandboxedSdkView2: SandboxedSdkView

    // TODO(b/257429573): Remove this line once fixed.
    @RequiresExtension(extension = SdkExtensions.AD_SERVICES, version = 5)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mSdkSandboxManager = applicationContext.getSystemService(
            SdkSandboxManager::class.java
        )

        if (!mSdkLoaded) {
            Log.i(TAG, "Loading SDK")
            mSdkSandboxManager.loadSdk(
                SDK_NAME, Bundle(), { obj: Runnable -> obj.run() }, LoadSdkCallbackImpl()
            )
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
    }

    // TODO(b/257429573): Remove this line once fixed.
    @RequiresExtension(extension = SdkExtensions.AD_SERVICES, version = 5)
    private inner class LoadSdkCallbackImpl() : OutcomeReceiver<SandboxedSdk, LoadSdkException> {

        override fun onResult(sandboxedSdk: SandboxedSdk) {
            Log.i(TAG, "Loaded successfully")
            mSandboxedSdk = sandboxedSdk
            mSdkLoaded = true
            val sdkApi = ISdkApi.Stub.asInterface(mSandboxedSdk.getInterface())

            mSandboxedSdkView1 = findViewById<SandboxedSdkView>(R.id.rendered_view)
            mSandboxedSdkView1.addStateChangedListener(StateChangeListener(mSandboxedSdkView1))
            mSandboxedSdkView1.setAdapter(SandboxedUiAdapterFactory.createFromCoreLibInfo(
                sdkApi.loadAd(/*isWebView=*/ true)
            ))

            mSandboxedSdkView2 = SandboxedSdkView(this@MainActivity)
            mSandboxedSdkView2.addStateChangedListener(StateChangeListener(mSandboxedSdkView2))
            mSandboxedSdkView2.layoutParams = ViewGroup.LayoutParams(200, 200)
            runOnUiThread(Runnable {
                findViewById<LinearLayout>(R.id.ad_layout).addView(mSandboxedSdkView2)
            })
            mSandboxedSdkView2.setAdapter(SandboxedUiAdapterFactory.createFromCoreLibInfo(
                sdkApi.loadAd(/*isWebView=*/ false)
            ))
        }

        override fun onError(error: LoadSdkException) {
            Log.i(TAG, "onLoadSdkFailure(" + error.getLoadSdkErrorCode().toString() + "): " +
                error.message)
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
                textView.setText(state.throwable.message)

                runOnUiThread(Runnable {
                    parent.removeView(view)
                    parent.addView(textView, index)
                })
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
