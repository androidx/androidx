/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.privacysandbox.ui.integration.testsdkprovider

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.privacysandbox.sdkruntime.core.controller.SdkSandboxControllerCompat
import androidx.privacysandbox.ui.client.SandboxedUiAdapterFactory
import androidx.privacysandbox.ui.client.view.SandboxedSdkView
import androidx.privacysandbox.ui.integration.sdkproviderutils.TestAdapters
import androidx.privacysandbox.ui.integration.testaidl.IAppOwnedMediateeSdkApi
import androidx.privacysandbox.ui.integration.testaidl.IMediateeSdkApi
import androidx.privacysandbox.ui.integration.testaidl.ISdkApi
import androidx.privacysandbox.ui.provider.toCoreLibInfo

class SdkApi(val sdkContext: Context) : ISdkApi.Stub() {
    private val testAdapters = TestAdapters(sdkContext)

    override fun loadWebViewAd(): Bundle {
        return testAdapters.WebViewBannerAd().toCoreLibInfo(sdkContext)
    }

    override fun loadLocalWebViewAd(): Bundle {
        return testAdapters.LocalViewBannerAd().toCoreLibInfo(sdkContext)
    }

    override fun loadTestAdWithWaitInsideOnDraw(text: String): Bundle {
        return testAdapters.TestBannerAdWithWaitInsideOnDraw(text).toCoreLibInfo(sdkContext)
    }

    override fun loadTestAd(text: String): Bundle {
        return testAdapters.TestBannerAd(text).toCoreLibInfo(sdkContext)
    }

    override fun loadMediatedTestAd(count: Int, isAppMediatee: Boolean): Bundle {
        val mediateeBannerAdBundle = getMediateeBannerAdBundle(count, isAppMediatee)
        return MediatedBannerAd(mediateeBannerAdBundle).toCoreLibInfo(sdkContext)
    }

    override fun requestResize(width: Int, height: Int) {}

    private inner class MediatedBannerAd(private val mediateeBannerAdBundle: Bundle?) :
        TestAdapters.BannerAd() {
        override fun buildAdView(sessionContext: Context): View {
            if (mediateeBannerAdBundle == null) {
                return testAdapters
                    .TestBannerAdWithWaitInsideOnDraw(
                        "Mediated SDK is not loaded, this is a mediator Ad!"
                    )
                    .buildAdView(sdkContext)
            }

            val view = SandboxedSdkView(sdkContext)
            val adapter = SandboxedUiAdapterFactory.createFromCoreLibInfo(mediateeBannerAdBundle)
            view.setAdapter(adapter)
            return view
        }
    }

    private fun getMediateeBannerAdBundle(count: Int, isAppMediatee: Boolean): Bundle? {
        val sdkSandboxControllerCompat = SdkSandboxControllerCompat.from(testAdapters.sdkContext)
        if (isAppMediatee) {
            val appOwnedSdkSandboxInterfaces =
                sdkSandboxControllerCompat.getAppOwnedSdkSandboxInterfaces()
            appOwnedSdkSandboxInterfaces.forEach { appOwnedSdkSandboxInterfaceCompat ->
                if (appOwnedSdkSandboxInterfaceCompat.getName().equals(MEDIATEE_SDK)) {
                    val appOwnedMediateeSdkApi =
                        IAppOwnedMediateeSdkApi.Stub.asInterface(
                            appOwnedSdkSandboxInterfaceCompat.getInterface()
                        )
                    return appOwnedMediateeSdkApi.loadTestAdWithWaitInsideOnDraw(
                        "AppOwnedMediation #$count"
                    )
                }
            }
        } else {
            val sandboxedSdks = sdkSandboxControllerCompat.getSandboxedSdks()
            sandboxedSdks.forEach { sandboxedSdkCompat ->
                if (sandboxedSdkCompat.getSdkInfo()?.name.equals(MEDIATEE_SDK)) {
                    val mediateeSdkApi =
                        IMediateeSdkApi.Stub.asInterface(sandboxedSdkCompat.getInterface())
                    return mediateeSdkApi.loadTestAdWithWaitInsideOnDraw("Mediation #$count")
                }
            }
        }
        return null
    }

    companion object {
        private const val MEDIATEE_SDK =
            "androidx.privacysandbox.ui.integration.mediateesdkprovider"
    }
}
