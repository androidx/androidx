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

package androidx.privacysandbox.ui.macrobenchmark.testapp.testsdkprovider

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Process
import androidx.privacysandbox.sdkruntime.provider.controller.SdkSandboxControllerCompat
import androidx.privacysandbox.ui.core.DelegatingSandboxedUiAdapter
import androidx.privacysandbox.ui.core.ExperimentalFeatures
import androidx.privacysandbox.ui.macrobenchmark.testapp.mediateesdkprovider.IMediateeSdkApiFactory
import androidx.privacysandbox.ui.macrobenchmark.testapp.sdkproviderutils.NativeAdGenerator
import androidx.privacysandbox.ui.macrobenchmark.testapp.sdkproviderutils.PlayerViewProvider
import androidx.privacysandbox.ui.macrobenchmark.testapp.sdkproviderutils.PlayerViewabilityHandler
import androidx.privacysandbox.ui.macrobenchmark.testapp.sdkproviderutils.SdkApiConstants.Companion.AdFormat
import androidx.privacysandbox.ui.macrobenchmark.testapp.sdkproviderutils.SdkApiConstants.Companion.AdType
import androidx.privacysandbox.ui.macrobenchmark.testapp.sdkproviderutils.SdkApiConstants.Companion.BackNavigation
import androidx.privacysandbox.ui.macrobenchmark.testapp.sdkproviderutils.SdkApiConstants.Companion.MediationOption
import androidx.privacysandbox.ui.macrobenchmark.testapp.sdkproviderutils.SdkApiConstants.Companion.ScreenOrientation
import androidx.privacysandbox.ui.macrobenchmark.testapp.sdkproviderutils.TestAdapters
import androidx.privacysandbox.ui.macrobenchmark.testapp.sdkproviderutils.fullscreen.FullscreenAd
import androidx.privacysandbox.ui.provider.AbstractSandboxedUiAdapter
import androidx.privacysandbox.ui.provider.toCoreLibInfo
import androidx.tracing.trace
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

@SuppressLint("NullAnnotationGroup")
@OptIn(ExperimentalFeatures.SharedUiPresentationApi::class)
class SdkApi(private val sdkContext: Context) : ISdkApi {
    private val testAdapters = TestAdapters(sdkContext)
    private val handler = Handler(Looper.getMainLooper())
    private val nativeAdGenerator = NativeAdGenerator(sdkContext, MediationOption.NON_MEDIATED)

    private lateinit var inAppMediateeAdapter: MediateeAdapterInterface

    override suspend fun loadAd(
        @AdFormat adFormat: Int,
        @AdType adType: Int,
        @MediationOption mediationOption: Int,
        waitInsideOnDraw: Boolean,
        drawViewability: Boolean,
    ): Bundle {
        return loadAdInternal(adFormat, adType, mediationOption, waitInsideOnDraw, drawViewability)
    }

    /**
     * Runnable that updates a [DelegatingSandboxedUiAdapter]'s delegate between different mediatees
     * every [UPDATE_DELEGATE_INTERVAL] ms.
     */
    @OptIn(ExperimentalFeatures.DelegatingAdapterApi::class)
    inner class UpdateDelegateTask(
        private val adapter: DelegatingSandboxedUiAdapter,
        private var mediationOption: Int,
        private val drawViewability: Boolean,
        private val numberOfRefreshes: Int,
    ) : Runnable {

        private var refreshCount = 0

        override fun run() {
            val coroutineScope = MainScope()
            coroutineScope.launch {
                val adapterBundle =
                    maybeGetMediateeBundle(
                        AdFormat.BANNER_AD,
                        AdType.BASIC_NON_WEBVIEW,
                        mediationOption,
                        waitInsideOnDraw = false,
                        drawViewability,
                    )
                // PLEASE ASK BEFORE MOVING. Moving this may affect benchmark metrics.
                trace("UiLib#adapterUpdateDelegate", {})
                adapter.updateDelegate(adapterBundle)
                mediationOption =
                    if (mediationOption == MediationOption.IN_APP_MEDIATEE) {
                        MediationOption.SDK_RUNTIME_MEDIATEE
                    } else {
                        MediationOption.IN_APP_MEDIATEE
                    }
                if (refreshCount++ < numberOfRefreshes) {
                    handler.postDelayed(this@UpdateDelegateTask, UPDATE_DELEGATE_INTERVAL)
                }
            }
        }
    }

    private fun loadBannerAd(@AdType adType: Int, waitInsideOnDraw: Boolean): Bundle {
        val adapter: AbstractSandboxedUiAdapter =
            when (adType) {
                AdType.BASIC_NON_WEBVIEW -> {
                    loadNonWebViewBannerAd("Simple Ad", waitInsideOnDraw)
                }
                AdType.BASIC_WEBVIEW -> {
                    loadWebViewBannerAd()
                }
                AdType.WEBVIEW_FROM_LOCAL_ASSETS -> {
                    loadWebViewBannerAdFromLocalAssets()
                }
                AdType.NON_WEBVIEW_VIDEO -> loadVideoAd()
                else -> {
                    loadNonWebViewBannerAd("Ad type not present", waitInsideOnDraw)
                }
            }
        return adapter.toCoreLibInfo(sdkContext)
    }

    private fun loadNativeAd(@AdType adType: Int): Bundle {
        return nativeAdGenerator.generateAdBundleWithAssets(adType)
    }

    override fun launchFullscreenAd(
        launcherInfo: Bundle,
        @ScreenOrientation screenOrientation: Int,
        @BackNavigation backButtonNavigation: Int,
    ) {
        val coroutineScope = MainScope()
        coroutineScope.launch {
            FullscreenAd(sdkContext).show(launcherInfo, screenOrientation, backButtonNavigation)
        }
    }

    override fun registerInAppMediateeAdapter(mediateeAdapter: MediateeAdapterInterface) {
        inAppMediateeAdapter = mediateeAdapter
    }

    @OptIn(ExperimentalFeatures.DelegatingAdapterApi::class)
    private fun startDelegatingAdUpdateHandler(
        adapter: DelegatingSandboxedUiAdapter,
        drawViewability: Boolean,
    ) {
        // This task will recursively post itself to the handler [numberOfRefreshes] times to allow
        // us to test several ad refreshes.
        // For benchmark tests, we'll keep the number of refreshes to 0.
        // 1 Ad (from Runtime mediatee) is already shown followed by 1 refresh only.
        handler.postDelayed(
            UpdateDelegateTask(
                adapter,
                MediationOption.IN_APP_MEDIATEE,
                drawViewability,
                numberOfRefreshes = 0,
            ),
            UPDATE_DELEGATE_INTERVAL,
        )
    }

    /** Kill sandbox process */
    override fun triggerProcessDeath() {
        Process.killProcess(Process.myPid())
    }

    private suspend fun loadAdInternal(
        @AdFormat adFormat: Int,
        @AdType adType: Int,
        @MediationOption mediationOption: Int,
        waitInsideOnDraw: Boolean,
        drawViewability: Boolean,
    ): Bundle {
        when (mediationOption) {
            MediationOption.NON_MEDIATED -> {
                return when (adFormat) {
                    AdFormat.BANNER_AD -> loadBannerAd(adType, waitInsideOnDraw)
                    AdFormat.NATIVE_AD -> loadNativeAd(adType)
                    else -> Bundle()
                }
            }
            MediationOption.SDK_RUNTIME_MEDIATEE,
            MediationOption.SDK_RUNTIME_MEDIATEE_WITH_OVERLAY,
            MediationOption.IN_APP_MEDIATEE,
            MediationOption.REFRESHABLE_MEDIATION ->
                return loadMediatedTestAd(
                    adFormat,
                    adType,
                    mediationOption,
                    waitInsideOnDraw,
                    drawViewability,
                )
            else -> return Bundle()
        }
    }

    private fun loadWebViewBannerAd(): AbstractSandboxedUiAdapter {
        return testAdapters.WebViewBannerAd()
    }

    private fun loadWebViewBannerAdFromLocalAssets(): AbstractSandboxedUiAdapter {
        return testAdapters.WebViewAdFromLocalAssets()
    }

    private fun loadNonWebViewBannerAd(
        text: String,
        waitInsideOnDraw: Boolean,
    ): AbstractSandboxedUiAdapter {
        return testAdapters.TestBannerAd(text, waitInsideOnDraw)
    }

    private fun loadVideoAd(): AbstractSandboxedUiAdapter {
        val playerViewProvider = PlayerViewProvider()
        val adapter = testAdapters.VideoBannerAd(playerViewProvider)
        PlayerViewabilityHandler.addObserverFactoryToAdapter(adapter, playerViewProvider)
        return adapter
    }

    @OptIn(ExperimentalFeatures.DelegatingAdapterApi::class)
    private suspend fun loadMediatedTestAd(
        @AdFormat adFormat: Int,
        @AdType adType: Int,
        @MediationOption mediationOption: Int,
        waitInsideOnDraw: Boolean,
        drawViewability: Boolean,
    ): Bundle {
        val mediateeBundle =
            maybeGetMediateeBundle(
                adFormat,
                adType,
                mediationOption,
                waitInsideOnDraw,
                drawViewability,
            )

        if (adFormat == AdFormat.BANNER_AD) {
            if (mediationOption == MediationOption.SDK_RUNTIME_MEDIATEE_WITH_OVERLAY) {
                return testAdapters.OverlaidAd(mediateeBundle).toCoreLibInfo(sdkContext)
            }

            if (mediationOption == MediationOption.REFRESHABLE_MEDIATION) {
                val delegatingAdapter = DelegatingSandboxedUiAdapter(mediateeBundle)
                startDelegatingAdUpdateHandler(delegatingAdapter, drawViewability)
                return delegatingAdapter.toCoreLibInfo(sdkContext)
            }
        }
        return mediateeBundle
    }

    override fun requestResize(width: Int, height: Int) {}

    private suspend fun maybeGetMediateeBundle(
        @AdFormat adFormat: Int,
        @AdType adType: Int,
        @MediationOption mediationOption: Int,
        waitInsideOnDraw: Boolean,
        drawViewability: Boolean,
    ): Bundle {
        when (mediationOption) {
            MediationOption.SDK_RUNTIME_MEDIATEE,
            MediationOption.SDK_RUNTIME_MEDIATEE_WITH_OVERLAY,
            MediationOption.REFRESHABLE_MEDIATION -> {
                val sandboxedMediateeApi =
                    maybeGetSandboxedMediateeSdkApi()
                        ?: return loadFallbackAd(adFormat, adType, waitInsideOnDraw)
                return sandboxedMediateeApi.loadAd(
                    adFormat,
                    adType,
                    waitInsideOnDraw,
                    drawViewability,
                )
            }
            MediationOption.IN_APP_MEDIATEE -> {
                return inAppMediateeAdapter.loadAd(
                    adFormat,
                    adType,
                    waitInsideOnDraw,
                    drawViewability,
                )
            }
            else -> return loadFallbackAd(adFormat, adType, waitInsideOnDraw)
        }
    }

    private fun loadFallbackAd(
        @AdFormat adFormat: Int,
        @AdType adType: Int,
        withSlowDraw: Boolean,
    ): Bundle =
        when (adFormat) {
            AdFormat.BANNER_AD ->
                testAdapters
                    .TestBannerAd(MEDIATED_SDK_NOT_LOADED_MESSAGE, withSlowDraw)
                    .toCoreLibInfo(sdkContext)
            AdFormat.NATIVE_AD ->
                nativeAdGenerator.generateAdBundleWithAssets(
                    adType,
                    MEDIATED_SDK_NOT_LOADED_MESSAGE,
                )
            else -> Bundle()
        }

    private fun maybeGetSandboxedMediateeSdkApi():
        androidx.privacysandbox.ui.macrobenchmark.testapp.mediateesdkprovider.IMediateeSdkApi? {
        val sdkSandboxControllerCompat = SdkSandboxControllerCompat.from(sdkContext)
        val sandboxedSdks = sdkSandboxControllerCompat.getSandboxedSdks()
        sandboxedSdks.forEach { sandboxedSdkCompat ->
            if (sandboxedSdkCompat.getSdkInfo()?.name == MEDIATEE_SDK) {
                return IMediateeSdkApiFactory.wrapToIMediateeSdkApi(
                    checkNotNull(sandboxedSdkCompat.getInterface()) {
                        "Cannot find Mediatee Sdk Service!"
                    }
                )
            }
        }
        return null
    }

    companion object {
        private const val MEDIATEE_SDK =
            "androidx.privacysandbox.ui.macrobenchmark.testapp.mediateesdkproviderwrapper"
        private const val MEDIATED_SDK_NOT_LOADED_MESSAGE =
            "Mediated SDK is not loaded, this is a mediator Ad!"
        private const val UPDATE_DELEGATE_INTERVAL: Long = 5000L
    }
}
