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

package androidx.privacysandbox.ui.integration.testapp

import android.app.Activity
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.fragment.app.Fragment
import androidx.privacysandbox.sdkruntime.client.SdkSandboxManagerCompat
import androidx.privacysandbox.ui.client.SandboxedUiAdapterFactory
import androidx.privacysandbox.ui.client.view.SandboxedSdkView
import androidx.privacysandbox.ui.client.view.SandboxedSdkViewEventListener
import androidx.privacysandbox.ui.integration.sdkproviderutils.SdkApiConstants.Companion.AdType
import androidx.privacysandbox.ui.integration.sdkproviderutils.SdkApiConstants.Companion.MediationOption
import androidx.privacysandbox.ui.integration.testsdkprovider.ISdkApi
import androidx.privacysandbox.ui.integration.testsdkprovider.ISdkApiFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

/**
 * Base fragment to be used for testing different manual flows.
 *
 * Create a new subclass of this for each independent flow you wish to test. There will only be one
 * active fragment in the app's main activity at any time. Use [getSdkApi] to get a handle to the
 * SDK.
 */
abstract class BaseFragment : Fragment() {
    private lateinit var sdkApi: ISdkApi

    private lateinit var sdkSandboxManager: SdkSandboxManagerCompat
    private lateinit var activity: Activity
    protected var providerUiOnTop by mutableStateOf(true)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activity = requireActivity()
        sdkSandboxManager = SdkSandboxManagerCompat.from(requireContext().applicationContext)
        runBlocking {
            val loadedSdks = sdkSandboxManager.getSandboxedSdks()
            var loadedSdk = loadedSdks.firstOrNull { it.getSdkInfo()?.name == SDK_NAME }
            if (loadedSdk == null) {
                loadedSdk = sdkSandboxManager.loadSdk(SDK_NAME, Bundle())
                sdkSandboxManager.loadSdk(MEDIATEE_SDK_NAME, Bundle())
            }
            sdkApi =
                ISdkApiFactory.wrapToISdkApi(
                    checkNotNull(loadedSdk.getInterface()) { "Cannot find Sdk Service!" }
                )
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        getSandboxedSdkViews().forEach { it.setEventListener() }
    }

    /** Returns a handle to the already loaded SDK. */
    fun getSdkApi(): ISdkApi {
        return sdkApi
    }

    fun SandboxedSdkView.setEventListener() {
        setEventListener(TestEventListener(this))
    }

    fun handleOptionsFromIntent(options: FragmentOptions) {
        currentMediationOption = options.mediation
        currentAdType = options.adType
        shouldDrawViewabilityLayer = options.drawViewability
        providerUiOnTop = options.isZOrderOnTop
    }

    /**
     * Returns the list of [SandboxedSdkView]s that are currently displayed inside this fragment.
     *
     * This will be called when the drawer is opened or closed, to automatically flip the Z-ordering
     * of any remote views.
     */
    open fun getSandboxedSdkViews(): List<SandboxedSdkView> = emptyList()

    /**
     * Called when the @AdType or @MediationOption of any [SandboxedSdkView] inside the fragment is
     * changed using the toggle switches in the drawer.
     *
     * Set the value of [currentAdType], [currentMediationOption] and [shouldDrawViewabilityLayer]
     * inside the method using the parameters passed to it, then call [loadBannerAd] method using
     * the parameters along with the [SandboxedSdkView] for which the new Ad needs to be loaded.
     */
    // TODO(b/343436839) : Handle this automatically
    // TODO(b/348194843): Clean up the options
    open fun handleLoadAdFromDrawer(
        adType: Int,
        mediationOption: Int,
        drawViewabilityLayer: Boolean
    ) {}

    fun loadBannerAd(
        @AdType adType: Int,
        @MediationOption mediationOption: Int,
        sandboxedSdkView: SandboxedSdkView,
        drawViewabilityLayer: Boolean,
        waitInsideOnDraw: Boolean = false
    ) {
        CoroutineScope(Dispatchers.Main).launch {
            val sdkBundle =
                sdkApi.loadBannerAd(adType, mediationOption, waitInsideOnDraw, drawViewabilityLayer)
            sandboxedSdkView.setAdapter(SandboxedUiAdapterFactory.createFromCoreLibInfo(sdkBundle))
            sandboxedSdkView.orderProviderUiAboveClientUi(providerUiOnTop)
        }
    }

    open fun handleDrawerStateChange(isDrawerOpen: Boolean) {
        providerUiOnTop = !isDrawerOpen && !isZOrderBelowToggleChecked
        getSandboxedSdkViews().forEach { it.orderProviderUiAboveClientUi(providerUiOnTop) }
    }

    private inner class TestEventListener(val view: SandboxedSdkView) :
        SandboxedSdkViewEventListener {
        override fun onUiDisplayed() {}

        override fun onUiError(error: Throwable) {
            val parent = view.parent as ViewGroup
            val index = parent.indexOfChild(view)
            val textView = TextView(requireActivity())
            textView.setTypeface(null, Typeface.BOLD_ITALIC)
            textView.setTextColor(Color.RED)
            textView.text = error.message
            requireActivity().runOnUiThread {
                parent.removeView(view)
                parent.addView(textView, index)
            }
        }

        override fun onUiClosed() {}
    }

    companion object {
        private const val SDK_NAME = "androidx.privacysandbox.ui.integration.testsdkproviderwrapper"
        private const val MEDIATEE_SDK_NAME =
            "androidx.privacysandbox.ui.integration.mediateesdkproviderwrapper"
        const val TAG = "TestSandboxClient"
        var isZOrderBelowToggleChecked = false
        @AdType var currentAdType = AdType.BASIC_NON_WEBVIEW
        @MediationOption var currentMediationOption = MediationOption.NON_MEDIATED
        var shouldDrawViewabilityLayer = false
    }
}
