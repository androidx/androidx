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

package androidx.privacysandbox.ui.integration.testapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.unit.dp
import androidx.privacysandbox.ui.client.SandboxedUiAdapterFactory
import androidx.privacysandbox.ui.client.view.SandboxedSdkUi
import androidx.privacysandbox.ui.core.SandboxedUiAdapter
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

class LazyListFragment : BaseFragment() {

    private var adapters by mutableStateOf(listOf<AdAdapterItem>())

    override fun handleLoadAdFromDrawer(
        adType: Int,
        mediationOption: Int,
        drawViewabilityLayer: Boolean
    ) {
        currentAdType = adType
        currentMediationOption = mediationOption
        shouldDrawViewabilityLayer = drawViewabilityLayer
        updateBannerAdAdapter()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        initializeBannerAdAdapter(count = 10)
        return ComposeView(requireContext()).apply {
            // Dispose of the Composition when the view's LifecycleOwner is destroyed
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent { AdsList(adapters) }
        }
    }

    @Composable
    private fun AdsList(adAdapters: List<AdAdapterItem>) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            items(
                items = adAdapters,
                key = { adapterWithId -> adapterWithId.id },
                contentType = { adapterItem -> adapterItem.contentType }
            ) { adapterWithId ->
                SandboxedSdkUi(
                    adapterWithId.adapter,
                    Modifier.fillParentMaxSize(),
                    providerUiOnTop = providerUiOnTop
                )
            }
        }
    }

    private fun initializeBannerAdAdapter(count: Int) {
        val coroutineScope = MainScope()
        coroutineScope.launch {
            val mutableAdapterList = mutableListOf<AdAdapterItem>()
            for (i in 1..count) {
                mutableAdapterList.add(
                    AdAdapterItem(
                        id = i,
                        adapter =
                            SandboxedUiAdapterFactory.createFromCoreLibInfo(
                                getSdkApi()
                                    .loadBannerAd(
                                        currentAdType,
                                        currentMediationOption,
                                        false,
                                        shouldDrawViewabilityLayer,
                                    )
                            )
                    )
                )
            }
            adapters = mutableAdapterList
        }
    }

    private fun updateBannerAdAdapter() {
        val coroutineScope = MainScope()
        coroutineScope.launch {
            val updatedAdapterList = mutableListOf<AdAdapterItem>()
            adapters.forEach { adapterWithId ->
                updatedAdapterList.add(
                    AdAdapterItem(
                        id = adapterWithId.id,
                        adapter =
                            SandboxedUiAdapterFactory.createFromCoreLibInfo(
                                getSdkApi()
                                    .loadBannerAd(
                                        currentAdType,
                                        currentMediationOption,
                                        false,
                                        shouldDrawViewabilityLayer,
                                    )
                            )
                    )
                )
            }
            adapters = updatedAdapterList
        }
    }

    private data class AdAdapterItem(
        val id: Int,
        // TODO(b/391558988): Specify content type for PoolingContainer CUJ
        // in View world for consistency
        val contentType: String = "BannerAd_$id",
        val adapter: SandboxedUiAdapter
    )
}
