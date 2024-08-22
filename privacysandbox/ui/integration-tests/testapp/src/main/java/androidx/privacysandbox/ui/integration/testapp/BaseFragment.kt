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
import android.os.Bundle
import android.util.Log
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.privacysandbox.sdkruntime.client.SdkSandboxManagerCompat
import androidx.privacysandbox.ui.client.SandboxedUiAdapterFactory
import androidx.privacysandbox.ui.client.view.SandboxedSdkUiSessionState
import androidx.privacysandbox.ui.client.view.SandboxedSdkUiSessionStateChangedListener
import androidx.privacysandbox.ui.client.view.SandboxedSdkView
import androidx.privacysandbox.ui.integration.sdkproviderutils.SdkApiConstants.Companion.AdType
import androidx.privacysandbox.ui.integration.sdkproviderutils.SdkApiConstants.Companion.MediationOption
import androidx.privacysandbox.ui.integration.testaidl.ISdkApi
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
            sdkApi = ISdkApi.Stub.asInterface(loadedSdk.getInterface())
        }
    }

    /** Returns a handle to the already loaded SDK. */
    fun getSdkApi(): ISdkApi {
        return sdkApi
    }

    fun SandboxedSdkView.addStateChangedListener() {
        addStateChangedListener(StateChangeListener(this))
    }

    /**
     * Returns the list of [SandboxedSdkView]s that are currently displayed inside this fragment.
     *
     * This will be called when the drawer is opened or closed, to automatically flip the Z-ordering
     * of any remote views.
     */
    abstract fun getSandboxedSdkViews(): List<SandboxedSdkView>

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
    abstract fun handleLoadAdFromDrawer(
        adType: Int,
        mediationOption: Int,
        drawViewabilityLayer: Boolean
    )

    fun loadBannerAd(
        @AdType adType: Int,
        @MediationOption mediationOption: Int,
        sandboxedSdkView: SandboxedSdkView,
        drawViewabilityLayer: Boolean,
        waitInsideOnDraw: Boolean = false
    ) {
        val sdkBundle =
            sdkApi.loadBannerAd(adType, mediationOption, waitInsideOnDraw, drawViewabilityLayer)
        sandboxedSdkView.setAdapter(SandboxedUiAdapterFactory.createFromCoreLibInfo(sdkBundle))
    }

    open fun handleDrawerStateChange(isDrawerOpen: Boolean) {
        getSandboxedSdkViews().forEach {
            it.orderProviderUiAboveClientUi(!isDrawerOpen && isZOrderOnTop)
        }
    }

    private inner class StateChangeListener(val view: SandboxedSdkView) :
        SandboxedSdkUiSessionStateChangedListener {
        override fun onStateChanged(state: SandboxedSdkUiSessionState) {
            Log.i(TAG, "UI session state changed to: $state")
            if (state is SandboxedSdkUiSessionState.Error) {
                // If the session fails to open, display the error.
                val parent = view.parent as ViewGroup
                val index = parent.indexOfChild(view)
                val textView = TextView(requireActivity())
                textView.text = state.throwable.message

                requireActivity().runOnUiThread {
                    parent.removeView(view)
                    parent.addView(textView, index)
                }
            }
        }
    }

    companion object {
        private const val SDK_NAME = "androidx.privacysandbox.ui.integration.testsdkprovider"
        private const val MEDIATEE_SDK_NAME =
            "androidx.privacysandbox.ui.integration.mediateesdkprovider"
        const val TAG = "TestSandboxClient"
        var isZOrderOnTop = true
        @AdType var currentAdType = AdType.BASIC_NON_WEBVIEW
        @MediationOption var currentMediationOption = MediationOption.NON_MEDIATED
        var shouldDrawViewabilityLayer = false
    }
}
