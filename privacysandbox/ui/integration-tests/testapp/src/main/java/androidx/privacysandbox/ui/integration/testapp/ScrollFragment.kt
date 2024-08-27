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
import android.widget.LinearLayout
import androidx.privacysandbox.ui.client.view.SandboxedSdkView
import androidx.privacysandbox.ui.integration.sdkproviderutils.SdkApiConstants.Companion.AdType
import androidx.privacysandbox.ui.integration.sdkproviderutils.SdkApiConstants.Companion.MediationOption

class ScrollFragment : BaseFragment() {

    private lateinit var bottomBannerView: SandboxedSdkView
    private lateinit var clippingBoundBannerView: SandboxedSdkView
    private lateinit var inflatedView: View

    override fun getSandboxedSdkViews(): List<SandboxedSdkView> {
        return listOf(bottomBannerView, clippingBoundBannerView)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        inflatedView = inflater.inflate(R.layout.fragment_scroll, container, false)
        bottomBannerView = SandboxedSdkView(requireActivity())
        loadBottomBannerAd()
        clippingBoundBannerView = inflatedView.findViewById(R.id.clipping_bound_view)
        loadClippingBoundBannerAd()
        return inflatedView
    }

    override fun handleLoadAdFromDrawer(
        @AdType adType: Int,
        @MediationOption mediationOption: Int,
        drawViewabilityLayer: Boolean
    ) {
        currentAdType = adType
        currentMediationOption = mediationOption
        shouldDrawViewabilityLayer = drawViewabilityLayer
        loadBannerAd(adType, mediationOption, clippingBoundBannerView, drawViewabilityLayer)
        loadBannerAd(adType, mediationOption, bottomBannerView, drawViewabilityLayer)
    }

    private fun loadBottomBannerAd() {
        bottomBannerView.layoutParams =
            inflatedView.findViewById<LinearLayout>(R.id.bottom_banner_container).layoutParams
        requireActivity().runOnUiThread {
            inflatedView
                .findViewById<LinearLayout>(R.id.bottom_banner_container)
                .addView(bottomBannerView)
        }
        loadBannerAd(
            currentAdType,
            currentMediationOption,
            bottomBannerView,
            shouldDrawViewabilityLayer
        )
    }

    private fun loadClippingBoundBannerAd() {
        loadBannerAd(
            currentAdType,
            currentMediationOption,
            clippingBoundBannerView,
            shouldDrawViewabilityLayer
        )
    }
}
