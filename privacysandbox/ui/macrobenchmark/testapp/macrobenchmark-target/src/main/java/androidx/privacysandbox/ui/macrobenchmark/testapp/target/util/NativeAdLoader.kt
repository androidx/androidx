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

package androidx.privacysandbox.ui.macrobenchmark.testapp.target.util

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Bundle
import android.view.View.inflate
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.media3.ui.PlayerView
import androidx.privacysandbox.ui.client.SandboxedUiAdapterFactory
import androidx.privacysandbox.ui.client.SharedUiAdapterFactory
import androidx.privacysandbox.ui.client.view.SandboxedSdkView
import androidx.privacysandbox.ui.client.view.SharedUiAsset
import androidx.privacysandbox.ui.client.view.SharedUiContainer
import androidx.privacysandbox.ui.core.ExperimentalFeatures
import androidx.privacysandbox.ui.macrobenchmark.testapp.sdkproviderutils.PlayerViewProvider
import androidx.privacysandbox.ui.macrobenchmark.testapp.sdkproviderutils.SdkApiConstants.Companion.NativeAdAssetName
import androidx.privacysandbox.ui.macrobenchmark.testapp.sdkproviderutils.SdkApiConstants.Companion.NativeAdAssetProperties
import androidx.privacysandbox.ui.macrobenchmark.testapp.target.R

@SuppressLint("NullAnnotationGroup")
@OptIn(ExperimentalFeatures.SharedUiPresentationApi::class)
class NativeAdLoader(context: Context) {
    val adView: SharedUiContainer = inflate(context, NATIVE_AD_LAYOUT_ID, null) as SharedUiContainer
    private val adHeadline: TextView = adView.findViewById(R.id.native_ad_headline)
    private val adBody: TextView = adView.findViewById(R.id.native_ad_body)
    private val adRemoteOverlayIcon: SandboxedSdkView =
        adView.findViewById(R.id.native_ad_remote_overlay_icon)
    private val adMediaView1: SandboxedSdkView = adView.findViewById(R.id.native_ad_media_view_1)
    private val adOverlayIcon: ImageView = adView.findViewById(R.id.native_ad_overlay_icon)
    private val adMediaView2: PlayerView = adView.findViewById(R.id.native_ad_media_view_2)
    private val adCallToAction: Button = adView.findViewById(R.id.native_ad_call_to_action)

    fun populateAd(sdkBundle: Bundle) {
        adView.setAdapter(SharedUiAdapterFactory.createFromCoreLibInfo(sdkBundle))
        val assets = sdkBundle.getBundle(NativeAdAssetName.ASSET_BUNDLE_NAME)

        val headlineAssets = assets?.getBundle(NativeAdAssetName.HEADLINE)
        adView.registerSharedUiAsset(
            SharedUiAsset(
                adHeadline.apply {
                    text = headlineAssets?.getString(NativeAdAssetProperties.TEXT)
                    setTextColor(
                        Color.parseColor(headlineAssets?.getString(NativeAdAssetProperties.COLOR))
                    )
                },
                NativeAdAssetName.HEADLINE,
            )
        )

        val bodyAssets = assets?.getBundle(NativeAdAssetName.BODY)
        adView.registerSharedUiAsset(
            SharedUiAsset(
                adBody.apply {
                    text = bodyAssets?.getString(NativeAdAssetProperties.TEXT)
                    setTextColor(
                        Color.parseColor(bodyAssets?.getString(NativeAdAssetProperties.COLOR))
                    )
                },
                NativeAdAssetName.BODY,
            )
        )

        val adChoicesAssets = assets?.getBundle(NativeAdAssetName.AD_CHOICES)
        if (adChoicesAssets != null) {
            adView.registerSharedUiAsset(
                SharedUiAsset(
                    adRemoteOverlayIcon,
                    NativeAdAssetName.AD_CHOICES,
                    sandboxedUiAdapter =
                        SandboxedUiAdapterFactory.createFromCoreLibInfo(adChoicesAssets),
                )
            )
        }

        val mediaView1Assets = assets?.getBundle(NativeAdAssetName.MEDIA_VIEW_1)
        if (mediaView1Assets != null) {
            adView.registerSharedUiAsset(
                SharedUiAsset(
                    adMediaView1,
                    NativeAdAssetName.MEDIA_VIEW_1,
                    sandboxedUiAdapter =
                        SandboxedUiAdapterFactory.createFromCoreLibInfo(mediaView1Assets),
                )
            )
        }

        val iconAssets = assets?.getBundle(NativeAdAssetName.ICON)
        adView.registerSharedUiAsset(
            SharedUiAsset(
                adOverlayIcon.apply {
                    val iconByteArray = iconAssets?.getByteArray(NativeAdAssetProperties.BITMAP)!!
                    val bitmap = BitmapFactory.decodeByteArray(iconByteArray, 0, iconByteArray.size)
                    setImageBitmap(bitmap)
                },
                NativeAdAssetName.ICON,
            )
        )

        val mediaView2Assets = assets?.getBundle(NativeAdAssetName.MEDIA_VIEW_2)
        adView.registerSharedUiAsset(
            SharedUiAsset(
                adMediaView2.apply {
                    player =
                        PlayerViewProvider()
                            .PlayerWithState(
                                context,
                                mediaView2Assets?.getString(NativeAdAssetProperties.URL)!!,
                            )
                            .initializePlayer()
                },
                NativeAdAssetName.MEDIA_VIEW_2,
            )
        )

        val callToActionAssets = assets?.getBundle(NativeAdAssetName.CALL_TO_ACTION)
        adView.registerSharedUiAsset(
            SharedUiAsset(
                adCallToAction.apply {
                    text = callToActionAssets?.getString(NativeAdAssetProperties.TEXT)
                    setBackgroundColor(
                        Color.parseColor(
                            callToActionAssets?.getString(NativeAdAssetProperties.COLOR)
                        )
                    )
                },
                NativeAdAssetName.CALL_TO_ACTION,
            )
        )
    }

    companion object {
        val NATIVE_AD_LAYOUT_ID = R.layout.native_ad_layout
    }
}
