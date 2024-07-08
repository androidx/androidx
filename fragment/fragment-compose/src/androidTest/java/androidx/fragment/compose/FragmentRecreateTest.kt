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

package androidx.fragment.compose

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.EditText
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentContainerView
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.add
import androidx.fragment.app.commit
import androidx.fragment.compose.test.EmptyTestActivity
import androidx.fragment.compose.test.R
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.testutils.withActivity
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Test
import org.junit.runner.RunWith

private const val PARENT_FRAGMENT_CONTAINER_ID = 1

@LargeTest
@RunWith(AndroidJUnit4::class)
class FragmentRecreateTest {
    @Test
    fun testRecreateFragment() {
        with(ActivityScenario.launch(EmptyTestActivity::class.java)) {
            var fragment: Fragment? = null

            val content = @Composable { AndroidFragment<SimpleEditTextFragment> { fragment = it } }
            withActivity { setContent(content = content) }

            assertThat(fragment!!.requireView().parent).isNotNull()
            val editText: EditText = fragment!!.requireView().findViewById(R.id.edit_text)
            assertThat(editText.text.toString()).isEqualTo("Default")

            // Update the state to make sure it gets saved and restored properly
            withActivity { editText.setText("Updated") }

            recreate()

            fragment = null
            withActivity { setContent(content = content) }
            assertThat(fragment!!.requireView().parent).isNotNull()
            val recreatedEditText: EditText = fragment!!.requireView().findViewById(R.id.edit_text)
            assertThat(recreatedEditText.text.toString()).isEqualTo("Updated")
        }
    }

    @Test
    fun testRecreateChildFragment() {
        with(ActivityScenario.launch(ChildInflatedFragmentActivity::class.java)) {
            val fragment = withActivity {
                FragmentManager.findFragment<SimpleEditTextFragment>(
                    findViewById(R.id.fragment_layout)
                )
            }

            assertWithMessage("Fragment should be added as a child fragment")
                .that(fragment)
                .isNotNull()
            assertThat(fragment.requireView().parent).isNotNull()
            val editText: EditText = fragment.requireView().findViewById(R.id.edit_text)
            assertThat(editText.text.toString()).isEqualTo("Default")

            // Update the state to make sure it gets saved and restored properly
            withActivity { editText.setText("Updated") }

            recreate()

            val recreatedFragment = withActivity {
                FragmentManager.findFragment<SimpleEditTextFragment>(
                    findViewById(R.id.fragment_layout)
                )
            }
            assertWithMessage("Fragment should be added as a child fragment")
                .that(recreatedFragment)
                .isNotNull()
            assertThat(recreatedFragment.requireView().parent).isNotNull()
            val recreatedEditText: EditText =
                recreatedFragment.requireView().findViewById(R.id.edit_text)
            assertThat(recreatedEditText.text.toString()).isEqualTo("Updated")
        }
    }

    @Test
    fun testReplaceParentFragment() {
        with(ActivityScenario.launch(ChildInflatedFragmentActivity::class.java)) {
            val fragment = withActivity {
                FragmentManager.findFragment<SimpleEditTextFragment>(
                    findViewById(R.id.fragment_layout)
                )
            }

            assertWithMessage("Fragment should be added as a child fragment")
                .that(fragment)
                .isNotNull()
            assertThat(fragment.requireView().parent).isNotNull()
            val editText: EditText = fragment.requireView().findViewById(R.id.edit_text)
            assertThat(editText.text.toString()).isEqualTo("Default")

            // Update the state to make sure it gets saved and restored properly
            withActivity { editText.setText("Updated") }

            val replaceFragment = MyFragment()
            withActivity {
                supportFragmentManager.commit {
                    setReorderingAllowed(true)
                    replace(PARENT_FRAGMENT_CONTAINER_ID, replaceFragment)
                    addToBackStack(null)
                }
                supportFragmentManager.executePendingTransactions()
            }

            // Now pop the back stack and go back to the parent fragment
            withActivity { supportFragmentManager.popBackStackImmediate() }

            val recreatedFragment = withActivity {
                FragmentManager.findFragment<SimpleEditTextFragment>(
                    findViewById(R.id.fragment_layout)
                )
            }
            assertWithMessage("Fragment should be re-added").that(recreatedFragment).isNotNull()
            assertWithMessage("Fragment should be added as a child fragment")
                .that(recreatedFragment.parentFragment)
                .isNotNull()
            assertThat(recreatedFragment.requireView().parent).isNotNull()
            val recreatedEditText: EditText =
                recreatedFragment.requireView().findViewById(R.id.edit_text)
            assertThat(recreatedEditText.text.toString()).isEqualTo("Updated")
        }
    }
}

class ChildInflatedFragmentActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(FragmentContainerView(this).apply { id = PARENT_FRAGMENT_CONTAINER_ID })
        if (savedInstanceState == null) {
            supportFragmentManager.commit {
                setReorderingAllowed(true)
                add<ParentFragment>(PARENT_FRAGMENT_CONTAINER_ID)
            }
        }
    }

    class ParentFragment : Fragment() {
        override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
        ) = content { AndroidFragment<SimpleEditTextFragment>() }
    }
}
