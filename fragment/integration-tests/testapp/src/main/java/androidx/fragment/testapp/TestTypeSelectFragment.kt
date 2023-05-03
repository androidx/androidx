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

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit

class TestTypeSelectFragment : Fragment(R.layout.test_type_select) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        addButton("Fragment Animator Test", AnimatorTestsFragment())
        addButton("Fragment Transition Test", TransitionTestsFragment())
    }
}

internal fun Fragment.addButton(text: String, fragment: Fragment) {
    (requireView() as LinearLayout).addView(
        Button(context).apply {
            this.text = text

            setOnClickListener {
                parentFragmentManager.commit {
                    replace(R.id.fragment_container, fragment)
                    addToBackStack(null)
                }
            }
            layoutParams = LinearLayout.LayoutParams(-1, 0).apply {
                weight = 1f
            }
        }
    )
}
