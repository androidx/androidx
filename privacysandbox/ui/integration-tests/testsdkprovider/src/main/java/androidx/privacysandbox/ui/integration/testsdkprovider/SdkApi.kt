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

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Process
import androidx.privacysandbox.sdkruntime.provider.controller.SdkSandboxControllerCompat
import androidx.privacysandbox.ui.core.DelegatingSandboxedUiAdapter
import androidx.privacysandbox.ui.core.ExperimentalFeatures
import androidx.privacysandbox.ui.integration.mediateesdkprovider.IMediateeSdkApiFactory
import androidx.privacysandbox.ui.integration.sdkproviderutils.NativeAdGenerator
import androidx.privacysandbox.ui.integration.sdkproviderutils.PlayerViewProvider
import androidx.privacysandbox.ui.integration.sdkproviderutils.PlayerViewabilityHandler
import androidx.privacysandbox.ui.integration.sdkproviderutils.SdkApiConstants.Companion.AdFormat
import androidx.privacysandbox.ui.integration.sdkproviderutils.SdkApiConstants.Companion.AdType
import androidx.privacysandbox.ui.integration.sdkproviderutils.SdkApiConstants.Companion.BackNavigation
import androidx.privacysandbox.ui.integration.sdkproviderutils.SdkApiConstants.Companion.MediationOption
import androidx.privacysandbox.ui.integration.sdkproviderutils.SdkApiConstants.Companion.ScreenOrientation
import androidx.privacysandbox.ui.integration.sdkproviderutils.TestAdapters
import androidx.privacysandbox.ui.integration.sdkproviderutils.ViewabilityHandler
import androidx.privacysandbox.ui.integration.sdkproviderutils.fullscreen.FullscreenAd
import androidx.privacysandbox.ui.provider.AbstractSandboxedUiAdapter
import androidx.privacysandbox.ui.provider.toCoreLibInfo
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

    override suspend fun loadBannerAdForAutomatedTests(
        @AdFormat adFormat: Int,
        @AdType adType: Int,
        @MediationOption mediationOption: Int,
        waitInsideOnDraw: Boolean,
        drawViewability: Boolean,
        automatedTestCallbackBundle: Bundle,
    ): Bundle {
        return loadAdInternal(
            adFormat,
            adType,
            mediationOption,
            waitInsideOnDraw,
            drawViewability,
            automatedTestCallbackBundle,
        )
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
        private val automatedTestCallbackBundle: Bundle,
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
                        automatedTestCallbackBundle,
                    )
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

    private fun loadBannerAd(
        @AdType adType: Int,
        waitInsideOnDraw: Boolean,
        drawViewability: Boolean,
        automatedTestCallbackBundle: Bundle,
    ): Bundle {
        val adapter: AbstractSandboxedUiAdapter =
            when (adType) {
                AdType.BASIC_NON_WEBVIEW -> {
                    loadNonWebViewBannerAd(
                        "Simple Ad",
                        waitInsideOnDraw,
                        automatedTestCallbackBundle,
                    )
                }
                AdType.BASIC_WEBVIEW -> {
                    loadWebViewBannerAd()
                }
                AdType.WEBVIEW_FROM_LOCAL_ASSETS -> {
                    loadWebViewBannerAdFromLocalAssets()
                }
                AdType.NON_WEBVIEW_VIDEO -> loadVideoAd()
                AdType.SCROLL_VIEW -> loadScrollView(automatedTestCallbackBundle)
                AdType.SCROLL_VIEW_APP_CAN_NOT_SCROLL ->
                    loadScrollView(automatedTestCallbackBundle, /* appCanScroll */ false)
                else -> {
                    loadNonWebViewBannerAd(
                        "Ad type not present",
                        waitInsideOnDraw,
                        automatedTestCallbackBundle,
                    )
                }
            }.also { ViewabilityHandler.addObserverFactoryToAdapter(it, drawViewability) }
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
        automatedTestCallbackBundle: Bundle,
    ) {
        // This task will recursively post itself to the handler [numberOfRefreshes] times to allow
        // us to test several ad refreshes.
        handler.postDelayed(
            UpdateDelegateTask(
                adapter,
                MediationOption.SDK_RUNTIME_MEDIATEE,
                drawViewability,
                numberOfRefreshes = 5,
                automatedTestCallbackBundle,
            ),
            UPDATE_DELEGATE_INTERVAL,
        )
        // post two tasks to the handler to simulate race conditions
        handler.postDelayed(
            UpdateDelegateTask(
                adapter,
                MediationOption.IN_APP_MEDIATEE,
                drawViewability,
                numberOfRefreshes = 0,
                automatedTestCallbackBundle,
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
        automatedTestCallbackBundle: Bundle = Bundle(),
    ): Bundle {
        when (mediationOption) {
            MediationOption.NON_MEDIATED -> {
                return when (adFormat) {
                    AdFormat.BANNER_AD ->
                        loadBannerAd(
                            adType,
                            waitInsideOnDraw,
                            drawViewability,
                            automatedTestCallbackBundle,
                        )
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
                    automatedTestCallbackBundle,
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
        automatedTestCallbackBundle: Bundle,
    ): AbstractSandboxedUiAdapter {
        return testAdapters.TestBannerAd(text, waitInsideOnDraw, automatedTestCallbackBundle)
    }

    private fun loadVideoAd(): AbstractSandboxedUiAdapter {
        val playerViewProvider = PlayerViewProvider()
        val adapter = testAdapters.VideoBannerAd(playerViewProvider)
        PlayerViewabilityHandler.addObserverFactoryToAdapter(adapter, playerViewProvider)
        return adapter
    }

    private fun loadScrollView(
        automatedTestCallbackBundle: Bundle,
        appCanScroll: Boolean = true,
    ): AbstractSandboxedUiAdapter {
        return testAdapters.ScrollViewAd(automatedTestCallbackBundle, appCanScroll)
    }

    @OptIn(ExperimentalFeatures.DelegatingAdapterApi::class)
    private suspend fun loadMediatedTestAd(
        @AdFormat adFormat: Int,
        @AdType adType: Int,
        @MediationOption mediationOption: Int,
        waitInsideOnDraw: Boolean,
        drawViewability: Boolean,
        automatedTestCallbackBundle: Bundle,
    ): Bundle {
        val mediateeBundle =
            maybeGetMediateeBundle(
                adFormat,
                adType,
                mediationOption,
                waitInsideOnDraw,
                drawViewability,
                automatedTestCallbackBundle,
            )

        if (adFormat == AdFormat.BANNER_AD) {
            if (mediationOption == MediationOption.SDK_RUNTIME_MEDIATEE_WITH_OVERLAY) {
                return testAdapters.OverlaidAd(mediateeBundle).toCoreLibInfo(sdkContext)
            }

            if (mediationOption == MediationOption.REFRESHABLE_MEDIATION) {
                val delegatingAdapter = DelegatingSandboxedUiAdapter(mediateeBundle)
                startDelegatingAdUpdateHandler(
                    delegatingAdapter,
                    drawViewability,
                    automatedTestCallbackBundle,
                )
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
        automatedTestCallbackBundle: Bundle,
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
                    automatedTestCallbackBundle,
                )
            }
            MediationOption.IN_APP_MEDIATEE -> {
                return inAppMediateeAdapter.loadAd(
                    adFormat,
                    adType,
                    waitInsideOnDraw,
                    drawViewability,
                    automatedTestCallbackBundle,
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
        androidx.privacysandbox.ui.integration.mediateesdkprovider.IMediateeSdkApi? {
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
            "androidx.privacysandbox.ui.integration.mediateesdkproviderwrapper"
        private const val MEDIATED_SDK_NOT_LOADED_MESSAGE =
            "Mediated SDK is not loaded, this is a mediator Ad!"
        private const val UPDATE_DELEGATE_INTERVAL: Long = 5000L
    }
}
