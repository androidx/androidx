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
import androidx.privacysandbox.ui.client.view.SandboxedSdkView
import androidx.privacysandbox.ui.integration.sdkproviderutils.SdkApiConstants.Companion.AdType
import androidx.privacysandbox.ui.integration.sdkproviderutils.SdkApiConstants.Companion.MediationOption

class ResizeFragment : BaseFragment() {

    private lateinit var resizableBannerView: SandboxedSdkView
    private lateinit var resizeButton: Button
    private lateinit var resizeFromSdkButton: Button
    private lateinit var inflatedView: View

    override fun getSandboxedSdkViews(): List<SandboxedSdkView> {
        return listOf(resizableBannerView)
    }

    override fun handleLoadAdFromDrawer(
        @AdType adType: Int,
        @MediationOption mediationOption: Int,
        drawViewabilityLayer: Boolean
    ) {
        currentAdType = adType
        currentMediationOption = mediationOption
        shouldDrawViewabilityLayer = drawViewabilityLayer
        loadBannerAd(
            adType,
            mediationOption,
            resizableBannerView,
            drawViewabilityLayer,
            waitInsideOnDraw = true
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        inflatedView = inflater.inflate(R.layout.fragment_resize, container, false)
        resizableBannerView = inflatedView.findViewById(R.id.resizable_ad_view)
        resizeButton = inflatedView.findViewById(R.id.resize_button)
        resizeFromSdkButton = inflatedView.findViewById(R.id.resize_sdk_button)
        loadResizableBannerAd()
        return inflatedView
    }

    private fun loadResizableBannerAd() {
        loadBannerAd(
            currentAdType,
            currentMediationOption,
            resizableBannerView,
            shouldDrawViewabilityLayer,
            waitInsideOnDraw = true
        )

        val displayMetrics = resources.displayMetrics
        val maxSizePixels = Math.min(displayMetrics.widthPixels, displayMetrics.heightPixels)

        val newSize = { currentSize: Int, maxSize: Int ->
            (currentSize + (100..200).random()) % maxSize
        }

        resizeButton.setOnClickListener {
            val newWidth = newSize(resizableBannerView.width, maxSizePixels)
            val newHeight = newSize(resizableBannerView.height, maxSizePixels)
            resizableBannerView.layoutParams =
                resizableBannerView.layoutParams.apply {
                    width = newWidth
                    height = newHeight
                }
        }
    }
}
