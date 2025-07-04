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

package androidx.privacysandbox.tools.integration.testapp

import android.annotation.SuppressLint
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.privacysandbox.sdkruntime.client.SdkSandboxManagerCompat
import androidx.privacysandbox.sdkruntime.core.LoadSdkCompatException
import androidx.privacysandbox.tools.integration.testsdk.MySdk
import androidx.privacysandbox.tools.integration.testsdk.MySdkFactory.wrapToMySdk
import androidx.privacysandbox.tools.integration.testsdk.NativeAdData
import androidx.privacysandbox.ui.client.view.SandboxedSdkView
import androidx.privacysandbox.ui.client.view.SandboxedSdkViewEventListener
import androidx.privacysandbox.ui.client.view.SharedUiAsset
import androidx.privacysandbox.ui.client.view.SharedUiContainer
import androidx.privacysandbox.ui.core.ExperimentalFeatures
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.idling.CountingIdlingResource
import kotlinx.coroutines.launch

@SuppressLint("NullAnnotationGroup")
@OptIn(ExperimentalFeatures.SharedUiPresentationApi::class)
class MainActivity : AppCompatActivity() {
    internal var sdk: MySdk? = null
    private val idlingResource = CountingIdlingResource("MainActivity idlingResource")

    override fun onDestroy() {
        super.onDestroy()
        IdlingRegistry.getInstance().unregister(idlingResource)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        IdlingRegistry.getInstance().register(idlingResource)

        lifecycleScope.launch { loadSdk() }
    }

    internal suspend fun loadSdk() {
        if (this.sdk != null) return

        val sandboxManagerCompat = SdkSandboxManagerCompat.from(this)
        val sandboxedSdk =
            try {
                sandboxManagerCompat.loadSdk(
                    "androidx.privacysandbox.tools.integration.sdk",
                    Bundle.EMPTY,
                )
            } catch (e: LoadSdkCompatException) {
                sandboxManagerCompat.getSandboxedSdks().first()
            }
        sdk = sandboxedSdk.getInterface()?.let { wrapToMySdk(it) }
    }

    internal suspend fun getUiAdapterAndRenderAd() {
        idlingResource.increment()

        val textViewAdAdapter = sdk!!.getAdapterForTextViewAd()
        val sandboxedSdkView = findViewById<SandboxedSdkView>(R.id.sandboxedSdkView)

        runOnUiThread {
            class TestEventListener : SandboxedSdkViewEventListener {
                override fun onUiDisplayed() {
                    idlingResource.decrement()
                }

                override fun onUiError(error: Throwable) {}

                override fun onUiClosed() {}
            }
            sandboxedSdkView.setEventListener(TestEventListener())
            sandboxedSdkView.setAdapter(textViewAdAdapter)
        }
    }

    internal suspend fun renderAd() {
        idlingResource.increment()

        val textViewAd = sdk!!.getTextViewAd()
        val sandboxedSdkView = findViewById<SandboxedSdkView>(R.id.sandboxedSdkView)

        runOnUiThread {
            class TestEventListener : SandboxedSdkViewEventListener {
                override fun onUiDisplayed() {
                    idlingResource.decrement()
                }

                override fun onUiError(error: Throwable) {}

                override fun onUiClosed() {}
            }
            sandboxedSdkView.setEventListener(TestEventListener())
            sandboxedSdkView.setAdapter(textViewAd)
        }
    }

    internal suspend fun renderNativeAd() {
        idlingResource.increment()
        val (nativeAd, headerText, remoteUiAdapter) = sdk!!.getNativeAdData()
        val sharedUiContainer = findViewById<SharedUiContainer>(R.id.sharedUiContainer)
        val appOwnedTextView = findViewById<TextView>(R.id.appOwnedTextView)
        val remoteUiView = findViewById<SandboxedSdkView>(R.id.remoteUiView)

        runOnUiThread {
            class TestEventListener : SandboxedSdkViewEventListener {
                override fun onUiDisplayed() {
                    idlingResource.decrement()
                }

                override fun onUiError(error: Throwable) {}

                override fun onUiClosed() {}
            }
            remoteUiView.setEventListener(TestEventListener())

            sharedUiContainer.setAdapter(nativeAd)
            appOwnedTextView.text = headerText
            sharedUiContainer.registerSharedUiAsset(
                SharedUiAsset(appOwnedTextView, NativeAdData.TEXT_VIEW_ASSET_ID)
            )
            sharedUiContainer.registerSharedUiAsset(
                SharedUiAsset(remoteUiView, NativeAdData.REMOTE_UI_ASSET_ID, remoteUiAdapter)
            )
        }
    }
}
