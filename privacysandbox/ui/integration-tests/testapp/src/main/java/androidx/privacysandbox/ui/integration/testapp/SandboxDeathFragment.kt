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
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.privacysandbox.ui.client.SandboxedUiAdapterFactory
import androidx.privacysandbox.ui.client.view.SandboxedSdkView
import androidx.privacysandbox.ui.integration.testaidl.ISdkApi

class SandboxDeathFragment : BaseFragment() {
    private lateinit var sdkApi: ISdkApi
    private lateinit var inflatedView: View
    private lateinit var sandboxedSdkView: SandboxedSdkView

    override fun handleDrawerStateChange(isDrawerOpen: Boolean) {
        sandboxedSdkView.orderProviderUiAboveClientUi(!isDrawerOpen)
    }
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        inflatedView = inflater.inflate(R.layout.fragment_sandbox_death, container, false)
        sdkApi = getSdkApi()
        onLoaded()
        return inflatedView
    }

    private fun onLoaded() {
        sandboxedSdkView = inflatedView.findViewById(R.id.remote_view)
        sandboxedSdkView.addStateChangedListener()
        sandboxedSdkView.setAdapter(
            SandboxedUiAdapterFactory.createFromCoreLibInfo(sdkApi.loadTestAd("Test Ad")))
        val unloadSdksButton: Button = inflatedView.findViewById(R.id.unload_all_sdks_button)
        unloadSdksButton.setOnClickListener {
            unloadAllSdks()
        }
    }
}
