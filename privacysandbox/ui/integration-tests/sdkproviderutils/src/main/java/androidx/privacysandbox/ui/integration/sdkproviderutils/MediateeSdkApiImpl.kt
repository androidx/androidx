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

import android.content.Context
import android.os.Build
import android.os.Bundle
import androidx.annotation.RequiresApi
import androidx.privacysandbox.ui.core.SandboxedUiAdapter
import androidx.privacysandbox.ui.integration.sdkproviderutils.SdkApiConstants.Companion.AdType
import androidx.privacysandbox.ui.integration.testaidl.IMediateeSdkApi
import androidx.privacysandbox.ui.provider.toCoreLibInfo

class MediateeSdkApiImpl(private val sdkContext: Context) : IMediateeSdkApi.Stub() {

    private val testAdapters = TestAdapters(sdkContext)
    private val mediationDescription =
        if (CompatImpl.isAppOwnedMediatee()) {
            "App Owned Mediation"
        } else "Runtime Mediation"

    override fun loadBannerAd(
        @AdType adType: Int,
        waitInsideOnDraw: Boolean,
        drawViewability: Boolean
    ): Bundle {
        val adapter: SandboxedUiAdapter =
            when (adType) {
                AdType.WEBVIEW -> loadWebViewBannerAd()
                AdType.WEBVIEW_FROM_LOCAL_ASSETS -> loadWebViewBannerAdFromLocalAssets()
                AdType.NON_WEBVIEW_VIDEO -> loadVideoAd()
                else -> loadNonWebViewBannerAd(mediationDescription, waitInsideOnDraw)
            }
        ViewabilityHandler.addObserverFactoryToAdapter(adapter, drawViewability)
        return adapter.toCoreLibInfo(sdkContext)
    }

    private fun loadWebViewBannerAd(): SandboxedUiAdapter {
        return testAdapters.WebViewBannerAd()
    }

    private fun loadWebViewBannerAdFromLocalAssets(): SandboxedUiAdapter {
        return testAdapters.WebViewAdFromLocalAssets()
    }

    private fun loadVideoAd(): SandboxedUiAdapter {
        val playerViewProvider = PlayerViewProvider()
        val adapter = testAdapters.VideoBannerAd(playerViewProvider)
        PlayerViewabilityHandler.addObserverFactoryToAdapter(adapter, playerViewProvider)
        return adapter
    }

    private fun loadNonWebViewBannerAd(
        text: String,
        waitInsideOnDraw: Boolean
    ): SandboxedUiAdapter {
        return testAdapters.TestBannerAd(text, waitInsideOnDraw)
    }

    private object CompatImpl {
        fun isAppOwnedMediatee(): Boolean {
            return if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.TIRAMISU) {
                true
            } else {
                Api34PlusImpl.isAppOwnedMediatee()
            }
        }

        @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
        private object Api34PlusImpl {
            fun isAppOwnedMediatee(): Boolean {
                return !android.os.Process.isSdkSandbox()
            }
        }
    }
}
