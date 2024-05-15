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
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.privacysandbox.sdkruntime.client.SdkSandboxManagerCompat
import androidx.privacysandbox.sdkruntime.client.SdkSandboxProcessDeathCallbackCompat
import androidx.privacysandbox.ui.client.view.SandboxedSdkUiSessionState
import androidx.privacysandbox.ui.client.view.SandboxedSdkUiSessionStateChangedListener
import androidx.privacysandbox.ui.client.view.SandboxedSdkView
import androidx.privacysandbox.ui.integration.testaidl.ISdkApi
import kotlinx.coroutines.runBlocking

/**
 * Base fragment to be used for testing different manual flows.
 *
 * Create a new subclass of this for each independent flow you wish to test. There will only be
 * one active fragment in the app's main activity at any time. Use [getSdkApi] to get a handle
 * to the SDK.
 */
abstract class BaseFragment : Fragment() {
    private lateinit var sdkApi: ISdkApi
    private lateinit var sdkSandboxManager: SdkSandboxManagerCompat
    private lateinit var activity: Activity

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activity = requireActivity()
        sdkSandboxManager = SdkSandboxManagerCompat.from(requireContext())
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

    /**
     * Returns a handle to the already loaded SDK.
     */
    fun getSdkApi(): ISdkApi {
        return sdkApi
    }

    fun SandboxedSdkView.addStateChangedListener() {
        addStateChangedListener(StateChangeListener(this))
    }

    /**
     * Unloads all SDKs, resulting in sandbox death. This method registers a death callback to
     * ensure that the app is not also killed.
     */
    fun unloadAllSdks() {
        sdkSandboxManager.addSdkSandboxProcessDeathCallback(Runnable::run, DeathCallbackImpl())
        sdkSandboxManager.unloadSdk(SDK_NAME)
        sdkSandboxManager.unloadSdk(MEDIATEE_SDK_NAME)
    }

    /**
     * Called when the app's drawer layout state changes. When called, change the Z-order of
     * any [SandboxedSdkView] owned by the fragment to ensure that the remote UI is not drawn over
     * the drawer. If the drawer is open, move all remote views to Z-below, otherwise move them
     * to Z-above.
     */
    abstract fun handleDrawerStateChange(isDrawerOpen: Boolean)

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

    private inner class DeathCallbackImpl : SdkSandboxProcessDeathCallbackCompat {
        override fun onSdkSandboxDied() {
            activity.runOnUiThread {
                Toast.makeText(activity, "Sandbox died", Toast.LENGTH_LONG).show()
            }
        }
    }

    companion object {
        private const val SDK_NAME = "androidx.privacysandbox.ui.integration.testsdkprovider"
        private const val MEDIATEE_SDK_NAME =
            "androidx.privacysandbox.ui.integration.mediateesdkprovider"
        const val TAG = "TestSandboxClient"
    }
}
