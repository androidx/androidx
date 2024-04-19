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

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.privacysandbox.sdkruntime.client.SdkSandboxManagerCompat
import androidx.privacysandbox.ui.client.view.SandboxedSdkView
import androidx.privacysandbox.ui.integration.testaidl.ISdkApi

/**
 * Base fragment to be used for testing different manual flows.
 *
 * Create a new subclass of this for each independent flow you wish to test. There will only be
 * one active fragment in the app's main activity at any time. Use [getSdkApi] to get a handle
 * to the SDK.
 */
abstract class BaseFragment : Fragment() {
    private lateinit var sdkApi: ISdkApi

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val sdkSandboxManager = SdkSandboxManagerCompat.from(requireContext())
        val loadedSdks = sdkSandboxManager.getSandboxedSdks()
        val loadedSdk = loadedSdks.firstOrNull { it.getSdkInfo()?.name == SDK_NAME }
        if (loadedSdk == null) {
            throw IllegalStateException("SDK not loaded")
        }
        sdkApi = ISdkApi.Stub.asInterface(loadedSdk.getInterface())
    }

    /**
     * Returns a handle to the already loaded SDK.
     */
    fun getSdkApi(): ISdkApi {
        return sdkApi
    }

    /**
     * Called when the app's drawer layout state changes. When called, change the Z-order of
     * any [SandboxedSdkView] owned by the fragment to ensure that the remote UI is not drawn over
     * the drawer. If the drawer is open, move all remote views to Z-below, otherwise move them
     * to Z-above.
     */
    abstract fun handleDrawerStateChange(isDrawerOpen: Boolean)

    companion object {
        private const val SDK_NAME = "androidx.privacysandbox.ui.integration.testsdkprovider"
        const val TAG = "TestSandboxClient"
    }
}
