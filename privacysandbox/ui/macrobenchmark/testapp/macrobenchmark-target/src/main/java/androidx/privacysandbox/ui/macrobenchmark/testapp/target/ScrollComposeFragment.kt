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

package androidx.privacysandbox.ui.macrobenchmark.testapp.target

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.privacysandbox.ui.client.SandboxedUiAdapterFactory
import androidx.privacysandbox.ui.client.compose.SandboxedSdkUi
import androidx.privacysandbox.ui.core.ExperimentalFeatures
import androidx.privacysandbox.ui.core.SandboxedUiAdapter
import androidx.privacysandbox.ui.macrobenchmark.testapp.sdkproviderutils.SdkApiConstants.Companion.AdFormat
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

// OptIn calling the experimental API SandboxedSdkUi(SandboxedUiAdapter, Modifier,
// SandboxedSdkViewEventListener?, Boolean)
@OptIn(ExperimentalFeatures.ChangingContentUiZOrderApi::class)
class ScrollComposeFragment : BaseFragment() {

    private var bottomBannerAdapter: SandboxedUiAdapter? by mutableStateOf(null)
    private var scrollBannerAdapter: SandboxedUiAdapter? by mutableStateOf(null)

    override fun handleLoadAdFromDrawer(
        adFormat: Int,
        adType: Int,
        mediationOption: Int,
        drawViewabilityLayer: Boolean,
    ) {
        currentAdFormat = adFormat
        currentAdType = adType
        currentMediationOption = mediationOption
        shouldDrawViewabilityLayer = drawViewabilityLayer
        setAdapter()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        setAdapter()
        return ComposeView(requireContext()).apply {
            // Dispose of the Composition when the view's LifecycleOwner is destroyed
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                Column(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    verticalArrangement = Arrangement.Top,
                    horizontalAlignment = Alignment.Start,
                ) {
                    Column(modifier = Modifier.weight(0.8f).verticalScroll(rememberScrollState())) {
                        scrollBannerAdapter?.let {
                            SandboxedSdkUi(
                                it,
                                Modifier.fillMaxWidth().height(200.dp),
                                providerUiOnTop = providerUiOnTop,
                            )
                        }
                        Text(stringResource(R.string.long_text), Modifier.padding(vertical = 16.dp))
                    }
                    bottomBannerAdapter?.let {
                        SandboxedSdkUi(it, Modifier.weight(0.2f), providerUiOnTop = providerUiOnTop)
                    }
                }
            }
        }
    }

    private fun setAdapter() {
        val coroutineScope = MainScope()
        coroutineScope.launch {
            bottomBannerAdapter =
                SandboxedUiAdapterFactory.createFromCoreLibInfo(
                    getSdkApi()
                        .loadAd(
                            AdFormat.BANNER_AD,
                            currentAdType,
                            currentMediationOption,
                            false,
                            shouldDrawViewabilityLayer,
                        )
                )
            scrollBannerAdapter =
                SandboxedUiAdapterFactory.createFromCoreLibInfo(
                    getSdkApi()
                        .loadAd(
                            AdFormat.BANNER_AD,
                            currentAdType,
                            currentMediationOption,
                            false,
                            shouldDrawViewabilityLayer,
                        )
                )
        }
    }
}
