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

package androidx.privacysandbox.ui.macrobenchmark.testapp.target.util

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.privacysandbox.ui.client.SandboxedUiAdapterFactory
import androidx.privacysandbox.ui.client.view.SandboxedSdkView
import androidx.privacysandbox.ui.client.view.SharedUiContainer
import androidx.privacysandbox.ui.core.ExperimentalFeatures
import androidx.privacysandbox.ui.macrobenchmark.testapp.sdkproviderutils.SdkApiConstants.Companion.AdFormat
import androidx.privacysandbox.ui.macrobenchmark.testapp.target.R

@SuppressLint("NullAnnotationGroup")
@OptIn(ExperimentalFeatures.SharedUiPresentationApi::class)
class AdHolder(context: Context, attrs: AttributeSet? = null) : FrameLayout(context, attrs) {
    private val nativeAdLoader = NativeAdLoader(context)
    private val bannerAdView: SandboxedSdkView = SandboxedSdkView(context)
    private val nativeAdView: SharedUiContainer
        get() = nativeAdLoader.adView

    val sandboxedSdkViews: List<SandboxedSdkView>
        get() =
            when (currentAdFormat) {
                AdFormat.BANNER_AD -> listOf(bannerAdView)
                AdFormat.NATIVE_AD ->
                    listOf(
                        nativeAdView.findViewById(R.id.native_ad_remote_overlay_icon),
                        nativeAdView.findViewById(R.id.native_ad_media_view_1),
                    )
                else -> listOf()
            }

    private var _currentAdFormat = AdFormat.BANNER_AD
    var currentAdFormat: Int
        get() = _currentAdFormat
        private set(value) {
            _currentAdFormat = value
            currentAdView =
                when (currentAdFormat) {
                    AdFormat.BANNER_AD -> bannerAdView
                    AdFormat.NATIVE_AD -> nativeAdView
                    else ->
                        TextView(context).apply {
                            text = "Unsupported ad format."
                            setTextColor(Color.RED)
                        }
                }
        }

    var currentAdView: View = bannerAdView
        private set

    var adViewLayoutParams: ViewGroup.LayoutParams =
        ViewGroup.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
    var adViewBackgroundColor: Int = Color.WHITE

    fun populateAd(sdkBundle: Bundle, @AdFormat adFormat: Int) {
        currentAdFormat = adFormat
        removeAllViews()
        when (adFormat) {
            AdFormat.BANNER_AD -> populateBannerAd(sdkBundle)
            AdFormat.NATIVE_AD -> nativeAdLoader.populateAd(sdkBundle)
        }
        currentAdView.layoutParams = adViewLayoutParams
        currentAdView.setBackgroundColor(adViewBackgroundColor)
        addView(currentAdView)
    }

    private fun populateBannerAd(sdkBundle: Bundle) {
        bannerAdView.setAdapter(SandboxedUiAdapterFactory.createFromCoreLibInfo(sdkBundle))
    }
}
