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
import android.view.ViewGroup
import android.widget.RadioButton
import androidx.core.view.OneShotPreDrawListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import androidx.fragment.testapp.R
import androidx.fragment.testapp.kittenfragmenttransitions.DetailsFragment.Companion.newInstance
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.transition.Fade

/** Displays a grid of pictures */
class GridFragment : Fragment(R.layout.kitten_fragment_grid) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        view.findViewById<RecyclerView>(R.id.recyclerview).apply {
            adapter = KittenGridAdapter(callback)
            layoutManager = GridLayoutManager(context, 2)
        }
        enterTransition = Fade()
        exitTransition = Fade()
        // View is created so postpone the transition
        postponeEnterTransition()
        OneShotPreDrawListener.add(view.parent as ViewGroup) { startPostponedEnterTransition() }
    }

    private val callback = { holder: KittenViewHolder, position: Int ->
        val kittenNumber = position % 6 + 1
        val kittenDetails = newInstance(kittenNumber)
        kittenDetails.sharedElementEnterTransition = DetailsTransition()
        kittenDetails.enterTransition = Fade()
        exitTransition = Fade()
        kittenDetails.sharedElementReturnTransition = DetailsTransition()
        val radioReplace = requireActivity().findViewById<RadioButton>(R.id.radioButton1)
        if (radioReplace.isChecked) {
            parentFragmentManager.commit {
                setReorderingAllowed(true)
                addSharedElement(holder.image, "kittenImage")
                replace(R.id.container, kittenDetails)
                addToBackStack(null)
            }
        } else {
            parentFragmentManager.commit {
                setReorderingAllowed(true)
                addSharedElement(holder.image, "kittenImage")
                add(R.id.container, kittenDetails)
                hide(this@GridFragment)
                addToBackStack(null)
            }
        }
    }
}
