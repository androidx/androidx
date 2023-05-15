/*
 * Copyright (C) 2017 The Android Open Source Project
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

import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.core.view.ViewCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.FragmentNavigatorExtras
import androidx.navigation.fragment.findNavController
import androidx.transition.Slide

/**
 * Fragment used to show how to navigate to another destination
 */
class MainFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        enterTransition = Slide(Gravity.RIGHT)
        exitTransition = Slide(Gravity.LEFT)
        return inflater.inflate(R.layout.main_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val tv = view.findViewById<TextView>(R.id.text)
        val myarg = arguments?.getString("myarg")
        tv.text = myarg

        view.setBackgroundColor(
            if (myarg == "one") {
                Color.GREEN
            } else if (myarg == "two") {
                Color.RED
            } else if (myarg == "three") {
                Color.YELLOW
            } else if (myarg == "four") {
                Color.GRAY
            } else if (myarg == "five") {
                Color.MAGENTA
            } else {
                Color.RED
            }
        )

        val b = view.findViewById<Button>(R.id.next_button)
        ViewCompat.setTransitionName(b, "next")
        b.setOnClickListener {
            findNavController().navigate(
                R.id.next, null, null,
                FragmentNavigatorExtras(b to "next")
            )
        }
        view.findViewById<Button>(R.id.learn_more).setOnClickListener {
            val args = Bundle().apply {
                putString("myarg", myarg)
            }
            findNavController().navigate(R.id.learn_more, args, null)
        }
    }
}
