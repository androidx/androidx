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

package androidx.privacysandbox.ui.integration.sdkproviderutils

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.privacysandbox.ui.core.ExperimentalFeatures
import androidx.privacysandbox.ui.core.SharedUiAdapter
import androidx.privacysandbox.ui.integration.sdkproviderutils.SdkApiConstants.Companion.AdType
import androidx.privacysandbox.ui.integration.sdkproviderutils.SdkApiConstants.Companion.MediationOption
import androidx.privacysandbox.ui.integration.sdkproviderutils.SdkApiConstants.Companion.NativeAdAssetName
import androidx.privacysandbox.ui.integration.sdkproviderutils.SdkApiConstants.Companion.NativeAdAssetProperties
import androidx.privacysandbox.ui.provider.toCoreLibInfo
import java.io.ByteArrayOutputStream
import java.util.concurrent.Executor

@SuppressLint("NullAnnotationGroup")
@OptIn(ExperimentalFeatures.SharedUiPresentationApi::class)
class NativeAdGenerator(
    private val sdkContext: Context,
    @MediationOption private val mediationOption: Int,
) {
    private val testAdapters = TestAdapters(sdkContext)
    private val bitmap = BitmapFactory.decodeResource(sdkContext.resources, R.drawable.android_logo)
    private val mediationTypeDescription =
        when (mediationOption) {
            MediationOption.NON_MEDIATED -> "No Mediation"
            MediationOption.IN_APP_MEDIATEE -> "App Owned Mediation"
            MediationOption.SDK_RUNTIME_MEDIATEE -> "Runtime Mediation"
            else -> "Unsupported"
        }
    private val defaultHeadlineText = "[$mediationTypeDescription] Native ad headline"

    private val bodyAssets: Bundle =
        Bundle().apply {
            putString(
                NativeAdAssetProperties.TEXT,
                "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Vivamus condimentum rhoncus est volutpat venenatis.",
            )
            putString(NativeAdAssetProperties.COLOR, "darkgrey")
        }

    private val iconAssets: Bundle =
        Bundle().apply {
            val outputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
            putByteArray(NativeAdAssetProperties.BITMAP, outputStream.toByteArray())
        }

    private val mediaView2Assets: Bundle =
        Bundle().apply {
            putString(NativeAdAssetProperties.URL, "https://html5demos.com/assets/dizzy.mp4")
        }

    private val callToActionAssets: Bundle =
        Bundle().apply {
            putString(NativeAdAssetProperties.TEXT, "Native ad call to action button")
            putString(NativeAdAssetProperties.COLOR, "maroon")
        }

    fun generateAdBundleWithAssets(
        @AdType adType: Int,
        headlineText: String = defaultHeadlineText,
    ): Bundle {
        return NativeAd().toCoreLibInfo().apply {
            putBundle(NativeAdAssetName.ASSET_BUNDLE_NAME, generateAssets(adType, headlineText))
        }
    }

    private fun generateAssets(@AdType adType: Int, headlineText: String): Bundle =
        Bundle().apply {
            putBundle(NativeAdAssetName.HEADLINE, generateHeadlineAsset(headlineText))
            putBundle(NativeAdAssetName.BODY, bodyAssets)
            putBundle(NativeAdAssetName.ICON, iconAssets)
            putBundle(NativeAdAssetName.MEDIA_VIEW_1, generateMediaView1Asset(adType))
            putBundle(NativeAdAssetName.MEDIA_VIEW_2, mediaView2Assets)
            putBundle(
                NativeAdAssetName.AD_CHOICES,
                testAdapters.TestBannerAd("@", withSlowDraw = false).toCoreLibInfo(sdkContext),
            )
            putBundle(NativeAdAssetName.CALL_TO_ACTION, callToActionAssets)
        }

    private fun generateHeadlineAsset(headlineText: String): Bundle =
        Bundle().apply {
            putString(NativeAdAssetProperties.TEXT, headlineText)
            putString(NativeAdAssetProperties.COLOR, "darkgrey")
        }

    private fun generateMediaView1Asset(@AdType adType: Int): Bundle {
        val adapter =
            when (adType) {
                AdType.BASIC_NON_WEBVIEW ->
                    testAdapters.TestBannerAd(
                        "[$mediationTypeDescription] Native ad remote MediaView",
                        withSlowDraw = false,
                    )
                AdType.BASIC_WEBVIEW -> testAdapters.WebViewBannerAd()
                AdType.WEBVIEW_FROM_LOCAL_ASSETS -> testAdapters.WebViewAdFromLocalAssets()
                AdType.NON_WEBVIEW_VIDEO -> {
                    val playerViewProvider = PlayerViewProvider()
                    val videoAdAdapter = testAdapters.VideoBannerAd(playerViewProvider)
                    PlayerViewabilityHandler.addObserverFactoryToAdapter(
                        videoAdAdapter,
                        playerViewProvider,
                    )
                    videoAdAdapter
                }
                else -> testAdapters.TestBannerAd("Ad type not present", withSlowDraw = false)
            }

        return adapter.toCoreLibInfo(sdkContext)
    }

    inner class NativeAd : SharedUiAdapter {
        override fun openSession(clientExecutor: Executor, client: SharedUiAdapter.SessionClient) {
            Handler(Looper.getMainLooper())
                .post(
                    Runnable lambda@{ clientExecutor.execute { client.onSessionOpened(Session()) } }
                )
        }

        private inner class Session : SharedUiAdapter.Session {
            override fun close() {
                Log.i(TAG, "Closing shared UI session")
            }
        }
    }

    companion object {
        private const val TAG = "TestSandboxSdk"
    }
}
