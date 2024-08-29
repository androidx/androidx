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
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.privacysandbox.ui.client.view.SandboxedSdkView
import androidx.privacysandbox.ui.integration.sdkproviderutils.SdkApiConstants.Companion.AdType
import androidx.privacysandbox.ui.integration.sdkproviderutils.SdkApiConstants.Companion.MediationOption
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class PoolingContainerFragment : BaseFragment() {
    private lateinit var inflatedView: View
    private lateinit var recyclerView: RecyclerView

    override fun getSandboxedSdkViews(): List<SandboxedSdkView> {
        return (recyclerView.adapter as CustomAdapter).sandboxedSdkViewSet.toList()
    }

    override fun handleDrawerStateChange(isDrawerOpen: Boolean) {
        super.handleDrawerStateChange(isDrawerOpen)
        (recyclerView.adapter as CustomAdapter).zOrderOnTop = !isDrawerOpen && isZOrderOnTop
    }

    override fun handleLoadAdFromDrawer(
        @AdType adType: Int,
        @MediationOption mediationOption: Int,
        drawViewabilityLayer: Boolean
    ) {
        currentAdType = adType
        currentMediationOption = mediationOption
        shouldDrawViewabilityLayer = drawViewabilityLayer
        val recyclerViewAdapter = CustomAdapter(adType, mediationOption, zOrderOnTop = false)
        recyclerView.adapter = recyclerViewAdapter
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        inflatedView = inflater.inflate(R.layout.fragment_poolingcontainer, container, false)
        recyclerView = inflatedView.findViewById(R.id.recycler_view)
        setRecyclerViewAdapter()
        return inflatedView
    }

    private fun setRecyclerViewAdapter() {
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.itemAnimator = DefaultItemAnimator()
        recyclerView.adapter = CustomAdapter(currentAdType, currentMediationOption)
    }

    private inner class CustomAdapter(
        @AdType val adType: Int,
        @MediationOption val mediationOption: Int,
        var zOrderOnTop: Boolean = true
    ) : RecyclerView.Adapter<CustomAdapter.ViewHolder>() {

        val sandboxedSdkViewSet = mutableSetOf<SandboxedSdkView>()
        private val childCount = 3

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val sandboxedSdkView: SandboxedSdkView = view.findViewById(R.id.recyclerview_ad_view)
        }

        override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
            val view =
                LayoutInflater.from(viewGroup.context)
                    .inflate(R.layout.recyclerview_row_item, viewGroup, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
            val childSandboxedSdkView = viewHolder.sandboxedSdkView
            childSandboxedSdkView.orderProviderUiAboveClientUi(zOrderOnTop)
            if (!sandboxedSdkViewSet.contains(childSandboxedSdkView)) {
                try {
                    loadBannerAd(
                        adType,
                        mediationOption,
                        childSandboxedSdkView,
                        shouldDrawViewabilityLayer
                    )
                } catch (e: Exception) {
                    Log.w(TAG, "Ad not loaded $e")
                }
                childSandboxedSdkView.addStateChangedListener()
                sandboxedSdkViewSet.add(childSandboxedSdkView)
            }
        }

        override fun getItemCount(): Int = childCount
    }
}
