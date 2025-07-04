/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.privacysandbox.ui.integration.testapp.fragments.hidden

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.privacysandbox.ui.client.view.SandboxedSdkView
import androidx.privacysandbox.ui.core.ExperimentalFeatures
import androidx.privacysandbox.ui.integration.testapp.R

// OptIn calling the experimental API SandboxedSdkView#orderProviderUiAboveClientUi
@OptIn(ExperimentalFeatures.ChangingContentUiZOrderApi::class)
class OcclusionFragment : BaseHiddenFragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        val inflatedView = inflater.inflate(R.layout.hidden_fragment_occlusion, container, false)
        val ssv = inflatedView.findViewById<SandboxedSdkView>(R.id.ad_layout)
        ssv.orderProviderUiAboveClientUi(false)
        loadBannerAd(
            currentAdType,
            currentMediationOption,
            ssv,
            shouldDrawViewabilityLayer,
            waitInsideOnDraw = true,
        )
        val button: Button = inflatedView.findViewById<Button>(R.id.alpha_button)
        button.setOnClickListener {
            // triggers a viewability event
            ssv.alpha -= 0.001f
        }
        return inflatedView
    }

    override fun loadAd(automatedTestCallbackBundle: Bundle) {
        TODO("Not yet implemented")
    }
}
