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

package androidx.privacysandbox.ui.integration.testapp.fragments.compose

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.privacysandbox.ui.client.SandboxedUiAdapterFactory
import androidx.privacysandbox.ui.client.compose.SandboxedSdkUi
import androidx.privacysandbox.ui.client.view.SandboxedSdkViewEventListener
import androidx.privacysandbox.ui.core.ExperimentalFeatures
import androidx.privacysandbox.ui.core.SandboxedUiAdapter
import androidx.privacysandbox.ui.integration.sdkproviderutils.SdkApiConstants.Companion.AdFormat
import androidx.privacysandbox.ui.integration.testapp.R
import androidx.privacysandbox.ui.integration.testapp.fragments.BaseFragment
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

// OptIn calling the experimental API SandboxedSdkUi(SandboxedUiAdapter, Modifier,
// SandboxedSdkViewEventListener?, Boolean)
@OptIn(ExperimentalFeatures.ChangingContentUiZOrderApi::class)
class ResizeComposeFragment : BaseFragment() {

    private var adapter: SandboxedUiAdapter? by mutableStateOf(null)

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
        setAdAdapter()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        setAdAdapter()
        return ComposeView(requireContext()).apply {
            // Dispose of the Composition when the view's LifecycleOwner is destroyed
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                var bannerDimension by remember { mutableStateOf(BannerDimension()) }
                var maxWidth = 0.dp
                var maxHeight = 0.dp
                val onResizeClicked: (BannerDimension) -> Unit = { currentDimension ->
                    val maxSizePixels = maxWidth.value.coerceAtMost(maxHeight.value).toInt()
                    val newSize = { currentSize: Int, maxSize: Int ->
                        (currentSize + (100..200).random()) % maxSize
                    }
                    val newWidth = newSize(currentDimension.width.value.toInt(), maxSizePixels).dp
                    val newHeight = newSize(currentDimension.height.value.toInt(), maxSizePixels).dp
                    bannerDimension = BannerDimension(newWidth, newHeight)
                }
                var bannerPadding by remember { mutableStateOf(BannerPadding()) }
                val onChangePaddingClicked: (BannerDimension) -> Unit = { currentDimension ->
                    val maxHorizontalPadding = (currentDimension.width.value.toInt() / 2) - 10
                    val maxVerticalPadding = (currentDimension.height.value.toInt() / 2) - 10
                    val horizontalPadding = (10..maxHorizontalPadding).random().dp
                    val verticalPadding = (10..maxVerticalPadding).random().dp
                    bannerPadding = BannerPadding(horizontalPadding, verticalPadding)
                }

                ResizeableBannerAd(
                    adapter,
                    bannerDimension,
                    onResizeClicked,
                    bannerPadding,
                    onChangePaddingClicked,
                    onSSUPlaced = { width, height ->
                        bannerDimension = BannerDimension(width, height)
                    },
                    onAdHolderPlaced = { width, height ->
                        maxWidth = width
                        maxHeight = height
                    },
                )
            }
        }
    }

    @Composable
    fun ResizeableBannerAd(
        adapter: SandboxedUiAdapter?,
        bannerDimension: BannerDimension,
        onResizeClicked: (BannerDimension) -> Unit,
        bannerPadding: BannerPadding,
        onChangePaddingClicked: (BannerDimension) -> Unit,
        onSSUPlaced: (Dp, Dp) -> Unit,
        onAdHolderPlaced: (Dp, Dp) -> Unit,
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.Start,
        ) {
            var adEventText by remember { mutableStateOf("") }
            Text("Ad state: $adEventText")

            val adHolderModifier =
                Modifier.fillMaxWidth().weight(1f).padding(top = 16.dp, bottom = 16.dp)
            AdHolder(
                bannerDimension,
                bannerPadding,
                adapter,
                adHolderModifier,
                onSSUPlaced,
                onAdHolderPlaced,
                { adEventText = it },
            )
            // TODO(b/399399902): Add Alpha CUJ once fixed
            Row {
                Button(
                    onClick = { onResizeClicked(bannerDimension) },
                    modifier = Modifier.padding(end = 16.dp),
                ) {
                    Text("Resize")
                }
                Button(
                    onClick = { onChangePaddingClicked(bannerDimension) },
                    modifier = Modifier.padding(end = 16.dp),
                ) {
                    Text("Change padding")
                }
            }
        }
    }

    @Composable
    fun AdHolder(
        bannerDimension: BannerDimension,
        bannerPadding: BannerPadding,
        adapter: SandboxedUiAdapter?,
        modifier: Modifier,
        onSSUPlaced: (Dp, Dp) -> Unit,
        onAdHolderPlaced: (Dp, Dp) -> Unit,
        onAdEvent: (String) -> Unit,
    ) {
        val localDensity = LocalDensity.current
        val boxModifier =
            modifier.onGloballyPositioned { coordinates ->
                with(localDensity) {
                    onAdHolderPlaced(coordinates.size.width.toDp(), coordinates.size.height.toDp())
                }
            }
        Box(boxModifier) {
            if (adapter != null) {
                var ssuModifier =
                    Modifier.onGloballyPositioned { coordinates ->
                            with(localDensity) {
                                onSSUPlaced(
                                    coordinates.size.width.toDp(),
                                    coordinates.size.height.toDp(),
                                )
                            }
                        }
                        .background(colorResource(R.color.ad_view_background_color))
                        .padding(
                            horizontal = bannerPadding.horizontalPadding,
                            vertical = bannerPadding.verticalPadding,
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
                    sandboxedSdkViewEventListener =
                        object : SandboxedSdkViewEventListener {
                            override fun onUiDisplayed() {
                                onAdEvent("Ad is visible")
                            }

                            override fun onUiError(error: Throwable) {
                                onAdEvent("Error loading ad : ${error.message}")
                            }

                            override fun onUiClosed() {
                                onAdEvent("Ad session is closed")
                            }
                        },
                )
            }
        }
    }

    private fun setAdAdapter() {
        val coroutineScope = MainScope()
        coroutineScope.launch {
            adapter =
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

    data class BannerDimension(val width: Dp = 0.dp, val height: Dp = 0.dp)

    data class BannerPadding(val horizontalPadding: Dp = 0.dp, val verticalPadding: Dp = 0.dp)
}
