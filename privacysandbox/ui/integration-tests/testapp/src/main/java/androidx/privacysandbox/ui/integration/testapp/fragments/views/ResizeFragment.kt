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

package androidx.privacysandbox.ui.integration.testapp.fragments.views

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.core.content.ContextCompat
import androidx.core.view.setMargins
import androidx.privacysandbox.ui.client.view.SandboxedSdkView
import androidx.privacysandbox.ui.integration.sdkproviderutils.SdkApiConstants.Companion.AdFormat
import androidx.privacysandbox.ui.integration.sdkproviderutils.SdkApiConstants.Companion.AdType
import androidx.privacysandbox.ui.integration.sdkproviderutils.SdkApiConstants.Companion.MediationOption
import androidx.privacysandbox.ui.integration.testapp.R
import androidx.privacysandbox.ui.integration.testapp.fragments.BaseFragment
import androidx.privacysandbox.ui.integration.testapp.util.AdHolder
import kotlin.math.max

// TODO(b/328046827): Add Resize from SDK CUJ
class ResizeFragment : BaseFragment() {

    private lateinit var resizableAdHolder: AdHolder

    private lateinit var resizeButton: Button
    private lateinit var setPaddingButton: Button
    private lateinit var alphaButton: Button
    private lateinit var inflatedView: View

    override fun getSandboxedSdkViews(): List<SandboxedSdkView> {
        return resizableAdHolder.sandboxedSdkViews
    }

    override fun handleLoadAdFromDrawer(
        @AdFormat adFormat: Int,
        @AdType adType: Int,
        @MediationOption mediationOption: Int,
        drawViewabilityLayer: Boolean,
    ) {
        currentAdFormat = adFormat
        currentAdType = adType
        currentMediationOption = mediationOption
        shouldDrawViewabilityLayer = drawViewabilityLayer
        loadAd(
            resizableAdHolder,
            currentAdFormat,
            currentAdType,
            currentMediationOption,
            shouldDrawViewabilityLayer,
            waitInsideOnDraw = true,
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        inflatedView = inflater.inflate(R.layout.fragment_resize, container, false)
        resizableAdHolder =
            inflatedView.findViewById<AdHolder>(R.id.resizable_ad_view).apply {
                adViewLayoutParams =
                    ViewGroup.MarginLayoutParams(adViewLayoutParams).apply {
                        setMargins(convertFromDpToPixels(DEFAULT_MARGIN_DP))
                    }
                adViewBackgroundColor =
                    ContextCompat.getColor(context, R.color.ad_view_background_color)
            }
        resizeButton = inflatedView.findViewById(R.id.resize_button)
        setPaddingButton = inflatedView.findViewById(R.id.set_padding_button)
        alphaButton = inflatedView.findViewById(R.id.set_alpha_button)
        initResizeButton()
        initSetPaddingButton()
        initAlphaButton()

        loadAd(
            resizableAdHolder,
            currentAdFormat,
            currentAdType,
            currentMediationOption,
            shouldDrawViewabilityLayer,
            true,
        )
        return inflatedView
    }

    private fun initAlphaButton() {
        alphaButton.setOnClickListener {
            val alpha = 1f / (1..10).random().toFloat()
            resizableAdHolder.currentAdView.alpha = alpha
        }
    }

    private fun initResizeButton() {
        val displayMetrics = resources.displayMetrics
        val maxSizePixels = Math.min(displayMetrics.widthPixels, displayMetrics.heightPixels)

        val newSize = { currentSize: Int, maxSize: Int ->
            (currentSize + (100..200).random()) % maxSize
        }

        resizeButton.setOnClickListener {
            val newWidth = newSize(resizableAdHolder.currentAdView.width, maxSizePixels)
            val newHeight =
                newSize(resizableAdHolder.currentAdView.height, resizableAdHolder.height)
            resizableAdHolder.currentAdView.layoutParams =
                resizableAdHolder.currentAdView.layoutParams.apply {
                    width = newWidth
                    height = newHeight
                }
        }
    }

    private fun initSetPaddingButton() {
        setPaddingButton.setOnClickListener {
            // Set halfWidth and halfHeight to minimum 10 to avoid crashes when the width and height
            // are very small
            val halfWidth = max(10, (resizableAdHolder.currentAdView.width / 2) - 10)
            val halfHeight = max(10, (resizableAdHolder.currentAdView.height / 2) - 10)
            resizableAdHolder.currentAdView.setPadding(
                (10..halfWidth).random(),
                (10..halfHeight).random(),
                (10..halfWidth).random(),
                (10..halfHeight).random(),
            )
        }
    }
}
