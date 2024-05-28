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
import androidx.privacysandbox.ui.client.SandboxedUiAdapterFactory
import androidx.privacysandbox.ui.client.view.SandboxedSdkView
import androidx.privacysandbox.ui.integration.testaidl.ISdkApi
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class PoolingContainerFragment : BaseFragment() {
    private lateinit var inflatedView: View
    private lateinit var sdkApi: ISdkApi
    private lateinit var recyclerView: RecyclerView

    override fun handleDrawerStateChange(isDrawerOpen: Boolean) {
        (recyclerView.adapter as CustomAdapter).handleDrawerStateChange(isDrawerOpen)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        inflatedView = inflater.inflate(R.layout.fragment_poolingcontainer, container, false)
        recyclerView = inflatedView.findViewById(R.id.recycler_view)
        sdkApi = getSdkApi()
        setRecyclerViewAdapter()
        return inflatedView
    }

    fun setRecyclerViewAdapter() {
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.itemAnimator = DefaultItemAnimator()
        recyclerView.adapter = CustomAdapter(sdkApi)
    }

    class CustomAdapter(private val sdkApi: ISdkApi) :
        RecyclerView.Adapter<CustomAdapter.ViewHolder>() {

        private val sandboxedSdkViewSet = mutableSetOf<SandboxedSdkView>()
        private val childCount = 3

        fun handleDrawerStateChange(isDrawerOpen: Boolean) {
            for (sandboxedSdkView in sandboxedSdkViewSet) {
                sandboxedSdkView.orderProviderUiAboveClientUi(!isDrawerOpen)
            }
        }

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val sandboxedSdkView: SandboxedSdkView

            init {
                sandboxedSdkView = view.findViewById(R.id.recyclerview_ad_view)
            }
        }

        override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
            val view =
                LayoutInflater.from(viewGroup.context)
                    .inflate(R.layout.recyclerview_row_item, viewGroup, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
            val childSandboxedSdkView = viewHolder.sandboxedSdkView
            if (!sandboxedSdkViewSet.contains(childSandboxedSdkView)) {
                childSandboxedSdkView.setAdapter(
                    SandboxedUiAdapterFactory.createFromCoreLibInfo(
                        sdkApi.loadTestAd(/* text= */ "PoolingContainer Ad #$position")
                    )
                )
                sandboxedSdkViewSet.add(childSandboxedSdkView)
            }
        }

        override fun getItemCount(): Int = childCount
    }
}
