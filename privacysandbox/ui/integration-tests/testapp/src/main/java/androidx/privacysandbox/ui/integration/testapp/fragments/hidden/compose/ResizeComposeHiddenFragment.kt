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

package androidx.privacysandbox.ui.integration.testapp.fragments.hidden.compose

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.absolutePadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.privacysandbox.ui.client.compose.SandboxedSdkUi
import androidx.privacysandbox.ui.core.ExperimentalFeatures
import androidx.privacysandbox.ui.core.SandboxedUiAdapter
import androidx.privacysandbox.ui.integration.testapp.fragments.hidden.AbstractResizeHiddenFragment
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

// OptIn calling the experimental API SandboxedSdkUi(SandboxedUiAdapter, Modifier,
// SandboxedSdkViewEventListener?, Boolean)
@OptIn(ExperimentalFeatures.ChangingContentUiZOrderApi::class)
class ResizeComposeHiddenFragment : AbstractResizeHiddenFragment() {

    private var adapter: SandboxedUiAdapter? by mutableStateOf(null)
    private lateinit var density: Density

    var bannerDimension by mutableStateOf(BannerDimension())
    var bannerPadding by mutableStateOf(BannerPadding())

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        return ComposeView(requireContext()).apply {
            // Dispose of the Composition when the view's LifecycleOwner is destroyed
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                ResizeableBannerAd(
                    adapter,
                    bannerDimension,
                    bannerPadding,
                    onSSUPlaced = { width, height ->
                        bannerDimension = BannerDimension(width, height)
                    },
                )
            }
        }
    }

    @Composable
    fun ResizeableBannerAd(
        adapter: SandboxedUiAdapter?,
        bannerDimension: BannerDimension,
        bannerPadding: BannerPadding,
        onSSUPlaced: (Dp, Dp) -> Unit,
    ) {
        Column(
            modifier = Modifier.Companion.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.Start,
        ) {
            val adHolderModifier =
                Modifier.Companion.fillMaxWidth().weight(1f).padding(top = 16.dp, bottom = 16.dp)
            AdHolder(bannerDimension, bannerPadding, adapter, adHolderModifier)
        }
    }

    @Composable
    fun AdHolder(
        bannerDimension: BannerDimension,
        bannerPadding: BannerPadding,
        adapter: SandboxedUiAdapter?,
        modifier: Modifier,
    ) {
        val localDensity = LocalDensity.current
        density = localDensity
        Box(modifier) {
            if (adapter == null) {
                return
            }
            var ssuModifier =
                Modifier.Companion.absolutePadding(
                    bannerPadding.paddingLeft,
                    bannerPadding.paddingTop,
                    bannerPadding.paddingRight,
                    bannerPadding.paddingBottom,
                )

            ssuModifier =
                if (bannerDimension.width == 0.dp) ssuModifier.fillMaxWidth()
                else ssuModifier.width(bannerDimension.width)

            ssuModifier =
                if (bannerDimension.height == 0.dp) ssuModifier.fillMaxHeight()
                else ssuModifier.height(bannerDimension.height)

            SandboxedSdkUi(
                adapter,
                ssuModifier,
                providerUiOnTop = providerUiOnTop,
                sandboxedSdkViewEventListener = eventListener,
            )
        }
    }

    override fun loadAd(automatedTestCallbackBundle: Bundle) {
        val coroutineScope = MainScope()
        coroutineScope.launch { adapter = buildAdapter(automatedTestCallbackBundle) }
    }

    override fun performResize(width: Int, height: Int) {
        bannerDimension =
            BannerDimension(convertPixelsToDpExternal(width), convertPixelsToDpExternal(height))
        BannerDimension(convertPixelsToDpExternal(width), convertPixelsToDpExternal(height))
    }

    override fun applyPadding(
        paddingLeft: Int,
        paddingTop: Int,
        paddingRight: Int,
        paddingBottom: Int,
    ) {
        bannerPadding =
            BannerPadding(
                convertPixelsToDpExternal(paddingLeft),
                convertPixelsToDpExternal(paddingTop),
                convertPixelsToDpExternal(paddingRight),
                convertPixelsToDpExternal(paddingBottom),
            )
    }

    fun convertPixelsToDpExternal(pixelValue: Int): Dp {
        return with(density) { pixelValue.toDp() }
    }

    data class BannerDimension(val width: Dp = 0.dp, val height: Dp = 0.dp)

    data class BannerPadding(
        val paddingLeft: Dp = 0.dp,
        val paddingTop: Dp = 0.dp,
        val paddingRight: Dp = 0.dp,
        val paddingBottom: Dp = 0.dp,
    )
}
