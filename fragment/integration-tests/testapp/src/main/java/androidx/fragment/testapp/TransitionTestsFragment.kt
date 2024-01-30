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

package androidx.fragment.testapp

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.testapp.doubleTransitionBug.DoubleTransitionBugFragment
import androidx.fragment.testapp.kittenfragmenttransitions.KittenTransitionMainFragment

class TransitionTestsFragment : Fragment(R.layout.transition_fragment) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        addButton("Double Transition Bug", DoubleTransitionBugFragment())
        addButton("Kitten Transition", KittenTransitionMainFragment())
    }
}

fun <T : FragmentActivity> Fragment.addButton(text: String, clazz: Class<T>) {
    (requireView() as LinearLayout).addView(
        Button(context).apply {
            this.text = text

            setOnClickListener {
                startActivity(Intent(activity, clazz))
            }
            layoutParams = LinearLayout.LayoutParams(-1, 0).apply {
                weight = 1f
            }
        }
    )
}
