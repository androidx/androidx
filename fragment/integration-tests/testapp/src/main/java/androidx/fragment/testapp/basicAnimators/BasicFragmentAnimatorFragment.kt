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

package androidx.fragment.testapp.basicAnimators

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.annotation.ColorInt
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import androidx.fragment.testapp.R

class BasicFragmentAnimatorFragment : Fragment(R.layout.basic_animators_main) {
    private var count = 0

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        if (savedInstanceState == null) {
            parentFragmentManager.beginTransaction()
                .setPrimaryNavigationFragment(this)
                .commit()
            childFragmentManager.commit {
                add(R.id.content, Fragment(R.layout.basic_animator_fragment))
            }
        }

        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<Button>(R.id.next_button).setOnClickListener {
            switchFragment()
        }
    }

    private fun switchFragment() {
        val fragment = MainFragment()
        fragment.arguments = bundleOf("myarg" to when (count % 6) {
            1 -> { Color.GREEN }
            2 -> { Color.RED }
            3 -> { Color.YELLOW }
            4 -> { Color.GRAY }
            5 -> { Color.MAGENTA }
            else -> {
                Color.BLUE
            }
        })

        count++

        childFragmentManager.beginTransaction()
            .setCustomAnimations(
                androidx.fragment.R.animator.fragment_close_enter,
                androidx.fragment.R.animator.fragment_close_exit,
                androidx.fragment.R.animator.fragment_close_enter,
                androidx.fragment.R.animator.fragment_close_exit,
            )
            .setReorderingAllowed(true)
            .replace(R.id.content, fragment)
            .addToBackStack(null)
            .commit()
    }

    class MainFragment : Fragment(R.layout.basic_animator_fragment) {
        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            super.onViewCreated(view, savedInstanceState)
            @ColorInt val myarg = arguments?.getInt("myarg") ?: Color.RED

            view.setBackgroundColor(myarg)
        }
    }
}