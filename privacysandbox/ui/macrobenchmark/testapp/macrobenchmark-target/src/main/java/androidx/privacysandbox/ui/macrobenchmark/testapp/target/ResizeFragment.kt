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

package androidx.privacysandbox.ui.macrobenchmark.testapp.target

import android.graphics.Color
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams
import android.widget.Button
import androidx.core.view.setMargins
import androidx.privacysandbox.ui.client.view.SandboxedSdkView
import androidx.privacysandbox.ui.macrobenchmark.testapp.sdkproviderutils.SdkApiConstants.Companion.AdFormat
import androidx.privacysandbox.ui.macrobenchmark.testapp.sdkproviderutils.SdkApiConstants.Companion.AdType
import androidx.privacysandbox.ui.macrobenchmark.testapp.sdkproviderutils.SdkApiConstants.Companion.MediationOption
import androidx.privacysandbox.ui.macrobenchmark.testapp.target.util.AdHolder
import kotlin.math.max

class ResizeFragment : BaseFragment() {

    private lateinit var resizableAdHolder: AdHolder

    private lateinit var resizeButton: Button
    private lateinit var resizeFromSdkButton: Button
    private lateinit var setPaddingButton: Button
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
                    MarginLayoutParams(adViewLayoutParams).apply {
                        setMargins(convertFromDpToPixels(MARGIN_DP))
                    }
                adViewBackgroundColor = Color.parseColor(AD_VIEW_BACKGROUND_COLOR)
            }
        resizeButton = inflatedView.findViewById(R.id.resize_button)
        resizeFromSdkButton = inflatedView.findViewById(R.id.resize_sdk_button)
        setPaddingButton = inflatedView.findViewById(R.id.set_padding_button)
        initResizeButton()
        initSetPaddingButton()

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

    private fun convertFromDpToPixels(dpValue: Float): Int =
        TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                dpValue,
                context?.resources?.displayMetrics,
            )
            .toInt()

    private companion object {
        const val MARGIN_DP = 16.0f
        const val AD_VIEW_BACKGROUND_COLOR = "#D3D3D3"
    }
}
