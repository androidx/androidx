/*
 * Copyright 2020 The Android Open Source Project
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
package androidx.fragment.testapp.kittenfragmenttransitions

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import androidx.annotation.IntRange
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.testapp.R
import androidx.transition.Fade

/**
 * Display details for a given kitten
 */
class DetailsFragment : Fragment(R.layout.kitten_details_fragment) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val image = view.findViewById<ImageView>(R.id.image)
        val args = arguments
        val kittenNumber = when {
            args == null -> 0
            args.containsKey(ARG_KITTEN_NUMBER) -> args.getInt(ARG_KITTEN_NUMBER)
            else -> 1
        }
        when (kittenNumber) {
            1 -> image.setImageResource(R.drawable.placekitten_1)
            2 -> image.setImageResource(R.drawable.placekitten_2)
            3 -> image.setImageResource(R.drawable.placekitten_3)
            4 -> image.setImageResource(R.drawable.placekitten_4)
            5 -> image.setImageResource(R.drawable.placekitten_5)
            6 -> image.setImageResource(R.drawable.placekitten_6)
        }

        enterTransition = Fade()
        exitTransition = Fade()
    }

    companion object {
        private const val ARG_KITTEN_NUMBER = "argKittenNumber"
        /**
         * Create a new DetailsFragment
         *
         * @param kittenNumber The number (between 1 and 6) of the kitten to display
         */
        @JvmStatic
        fun newInstance(@IntRange(from = 1, to = 6) kittenNumber: Int) =
            DetailsFragment().apply {
                arguments = bundleOf(ARG_KITTEN_NUMBER to kittenNumber)
            }
    }
}
