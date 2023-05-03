/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.fragment.testapp.doubleTransitionBug

import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import androidx.fragment.testapp.R
import androidx.transition.Fade
import androidx.transition.Slide

class DoubleTransitionBugFragment : Fragment(R.layout.double_transition_bug_activity_main) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (savedInstanceState == null) {
            childFragmentManager.commit {
                add(R.id.content, Fragment(R.layout.double_transition_bug_fragment_second))
            }
        }

        requireActivity().findViewById<Button>(R.id.important_button).setOnClickListener {
            switchFragment()
        }
    }

    private fun switchFragment() {
        val currentFragment = childFragmentManager.findFragmentById(R.id.content)

        currentFragment!!.exitTransition = Fade()

        val second = Fragment(R.layout.double_transition_bug_fragment_second)
        second.enterTransition = Slide(Gravity.BOTTOM)

        val first = Fragment(R.layout.double_transition_bug_fragment_first)
        first.enterTransition = Fade().setDuration(5000)

        childFragmentManager.beginTransaction()
            .setReorderingAllowed(true)
            .add(R.id.content, first)
            .add(R.id.content, second)
            .hide(currentFragment)
            .addToBackStack(null)
            .commit()
    }
}
