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

package androidx.pdf.testapp.ui.scenarios

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.pdf.testapp.databinding.TabsViewBinding
import androidx.pdf.testapp.ui.ViewPagerAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

/**
 * This fragment presents a tab-based view for displaying a PDFViewer.
 *
 * It provides a similar experience to the files app usage of PDFViewer, offering a PageViewer-like
 * interface.
 */
class TabsViewPdfFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        super.onCreateView(inflater, container, savedInstanceState)

        val tabsViewPdf = TabsViewBinding.inflate(inflater, container, false)
        val tab: TabLayout = tabsViewPdf.tab
        val viewPager: ViewPager2 = tabsViewPdf.viewpager

        val viewPagerAdapter: ViewPagerAdapter = ViewPagerAdapter(this)
        viewPager.adapter = viewPagerAdapter
        viewPager.offscreenPageLimit = 1

        // Connect TabLayout with ViewPager
        TabLayoutMediator(tab, viewPager) { mediatedTab, position ->
                when (position) {
                    0 -> mediatedTab.text = TAB_1_TAG
                    1 -> mediatedTab.text = TAB_2_TAG
                    2 -> mediatedTab.text = TAB_3_TAG
                    3 -> mediatedTab.text = TAB_4_TAG
                }
            }
            .attach()

        return tabsViewPdf.root
    }

    companion object {
        private const val TAB_1_TAG = "Tab 1"
        private const val TAB_2_TAG = "Tab 2"
        private const val TAB_3_TAG = "Tab 3"
        private const val TAB_4_TAG = "Tab 4"
    }
}
