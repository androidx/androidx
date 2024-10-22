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
import androidx.fragment.app.FragmentManager
import androidx.pdf.testapp.R
import androidx.pdf.testapp.ui.BasicPdfFragment

/**
 * This fragment is used to display a single PDF file.
 *
 * The PDF file is displayed in a [BasicPdfFragment].
 */
class SinglePdfFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        super.onCreateView(inflater, container, savedInstanceState)

        val singlePdf = inflater.inflate(R.layout.single_pdf_fragment, container, false)

        // Check if a fragment is currently visible (automatically restored by FragmentManager)
        val currentFragment = childFragmentManager.findFragmentByTag(SINGLE_PDF_FRAGMENT_TAG)
        if (currentFragment == null) {
            setChildFragment()
        }

        return singlePdf
    }

    private fun setChildFragment() {
        val fragmentManager: FragmentManager = childFragmentManager

        // Fragment initialization
        val basicPdfFragment = BasicPdfFragment()

        // Replace an existing fragment in a container with an instance of a new fragment
        fragmentManager
            .beginTransaction()
            .replace(
                R.id.single_pdf_fragment_container_view,
                basicPdfFragment,
                SINGLE_PDF_FRAGMENT_TAG
            )
            .commit()

        fragmentManager.executePendingTransactions()
    }

    companion object {
        private const val SINGLE_PDF_FRAGMENT_TAG = "single_pdf_fragment_tag"
    }
}
