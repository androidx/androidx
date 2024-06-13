/*
 * Copyright (C) 2024 The Android Open Source Project
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

package androidx.privacysandbox.ui.integration.mediateesdkprovider

import android.content.Context
import android.os.Bundle
import androidx.privacysandbox.ui.integration.sdkproviderutils.SdkApiConstants.Companion.AdType
import androidx.privacysandbox.ui.integration.sdkproviderutils.TestAdapters
import androidx.privacysandbox.ui.integration.testaidl.IMediateeSdkApi
import androidx.privacysandbox.ui.provider.toCoreLibInfo

class MediateeSdkApi(private val sdkContext: Context) : IMediateeSdkApi.Stub() {
    private val testAdapters = TestAdapters(sdkContext)

    override fun loadBannerAd(@AdType adType: Int, withSlowDraw: Boolean): Bundle {
        return when (adType) {
            AdType.WEBVIEW -> loadWebViewBannerAd()
            AdType.WEBVIEW_FROM_LOCAL_ASSETS -> loadWebViewBannerAdFromLocalAssets()
            else -> loadNonWebViewBannerAd("Mediation", withSlowDraw)
        }
    }

    private fun loadWebViewBannerAd(): Bundle {
        return testAdapters.WebViewBannerAd().toCoreLibInfo(sdkContext)
    }

    private fun loadWebViewBannerAdFromLocalAssets(): Bundle {
        return testAdapters.WebViewAdFromLocalAssets().toCoreLibInfo(sdkContext)
    }

    private fun loadNonWebViewBannerAd(text: String, waitInsideOnDraw: Boolean): Bundle {
        return testAdapters.TestBannerAd(text, waitInsideOnDraw).toCoreLibInfo(sdkContext)
    }
}
