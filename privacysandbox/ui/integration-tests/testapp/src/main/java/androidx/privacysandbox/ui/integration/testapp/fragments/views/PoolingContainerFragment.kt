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

package androidx.privacysandbox.ui.integration.testapp.fragments.views

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.setMargins
import androidx.privacysandbox.ui.client.view.SandboxedSdkView
import androidx.privacysandbox.ui.integration.sdkproviderutils.SdkApiConstants.Companion.AdFormat
import androidx.privacysandbox.ui.integration.sdkproviderutils.SdkApiConstants.Companion.AdType
import androidx.privacysandbox.ui.integration.sdkproviderutils.SdkApiConstants.Companion.MediationOption
import androidx.privacysandbox.ui.integration.testapp.R
import androidx.privacysandbox.ui.integration.testapp.fragments.BaseFragment
import androidx.privacysandbox.ui.integration.testapp.util.AdHolder
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class PoolingContainerFragment : BaseFragment() {
    private lateinit var inflatedView: View
    private lateinit var recyclerView: RecyclerView

    override fun getSandboxedSdkViews(): List<SandboxedSdkView> {
        return (recyclerView.adapter as CustomAdapter).sandboxedSdkViews
    }

    override fun handleLoadAdFromDrawer(
        @AdFormat adFormat: Int,
        @AdType adType: Int,
        @MediationOption mediationOption: Int,
        drawViewabilityLayer: Boolean,
    ) {
        currentAdFormat = adFormat
        currentAdType = adType
        currentMediationOption = mediationOption
        shouldDrawViewabilityLayer = drawViewabilityLayer
        val recyclerViewAdapter = CustomAdapter()
        recyclerView.adapter = recyclerViewAdapter
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        inflatedView = inflater.inflate(R.layout.fragment_poolingcontainer, container, false)
        recyclerView = inflatedView.findViewById(R.id.recycler_view)
        setRecyclerViewAdapter()
        return inflatedView
    }

    private fun setRecyclerViewAdapter() {
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.itemAnimator = DefaultItemAnimator()
        recyclerView.adapter = CustomAdapter()
    }

    private inner class CustomAdapter() : RecyclerView.Adapter<CustomAdapter.ViewHolder>() {

        val adHoldersSet = mutableSetOf<AdHolder>()
        val sandboxedSdkViews: List<SandboxedSdkView>
            get() = adHoldersSet.map { it.sandboxedSdkViews }.flatten()

        private val childCount = 3

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val adHolder: AdHolder =
                view.findViewById<AdHolder>(R.id.recyclerview_ad_view_holder).apply {
                    adViewLayoutParams =
                        ViewGroup.MarginLayoutParams(adViewLayoutParams).apply {
                            setMargins(convertFromDpToPixels(DEFAULT_MARGIN_DP))
                        }
                    adViewBackgroundColor = Color.RED
                }
        }

        override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
            val view =
                LayoutInflater.from(viewGroup.context)
                    .inflate(R.layout.recyclerview_row_item, viewGroup, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
            val childAdHolder = viewHolder.adHolder
            if (!adHoldersSet.contains(childAdHolder)) {
                try {
                    loadAd(
                        childAdHolder,
                        currentAdFormat,
                        currentAdType,
                        currentMediationOption,
                        shouldDrawViewabilityLayer,
                    )
                } catch (e: Exception) {
                    Log.w(TAG, "Ad not loaded $e")
                }
                childAdHolder.sandboxedSdkViews.forEach { it.setEventListener() }
                adHoldersSet.add(childAdHolder)
            }
        }

        override fun getItemCount(): Int = childCount
    }
}
