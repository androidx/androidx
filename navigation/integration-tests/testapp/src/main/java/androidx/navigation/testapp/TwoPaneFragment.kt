/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.navigation.testapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.NavOptions
import androidx.navigation.fragment.AbstractListDetailFragment
import androidx.navigation.fragment.NavHostFragment
import androidx.recyclerview.widget.RecyclerView

class TwoPaneFragment : AbstractListDetailFragment() {

    override fun onCreateListPaneView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.list_pane, container, false)
    }

    override fun onCreateDetailPaneNavHostFragment(): NavHostFragment {
        return NavHostFragment.create(R.navigation.two_pane_navigation)
    }

    override fun onListPaneViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onListPaneViewCreated(view, savedInstanceState)
        val recyclerView = view as RecyclerView
        recyclerView.adapter = TwoPaneAdapter(map.keys.toTypedArray()) {
            map[it]?.let { destId -> openDetails(destId) }
        }
    }

    private fun openDetails(destinationId: Int) {
        val detailNavController = requireDetailPaneNavHostFragment().navController
        detailNavController.navigate(
            destinationId,
            null,
            NavOptions.Builder()
                .setPopUpTo(detailNavController.graph.startDestinationId, true)
                .apply {
                    if (requireSlidingPaneLayout().isOpen) {
                        setEnterAnim(R.anim.nav_default_enter_anim)
                        setExitAnim(R.anim.nav_default_exit_anim)
                    }
                }
                .build()
        )
        requireSlidingPaneLayout().open()
    }

    companion object {
        val map = mapOf(
            "first" to R.id.first_fragment,
            "second" to R.id.second_fragment,
            "third" to R.id.third_fragment,
            "fourth" to R.id.fourth_fragment,
            "fifth" to R.id.fifth_fragment
        )
    }
}