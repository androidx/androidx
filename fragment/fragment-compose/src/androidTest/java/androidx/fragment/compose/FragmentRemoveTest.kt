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
import android.view.ViewGroup
import android.widget.EditText
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.testutils.expectError
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.fragment.compose.test.EmptyTestActivity
import androidx.fragment.compose.test.R
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.testutils.withActivity
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class FragmentRemoveTest {

    @get:Rule val rule = createAndroidComposeRule<EmptyTestActivity>()

    @Test
    fun testRemoval() {
        var show by mutableStateOf(true)

        lateinit var fragment: Fragment
        rule.setContent {
            if (show) {
                AndroidFragment<SimpleEditTextFragment> { fragment = it }
            }
        }

        assertWithMessage("Fragment should be present when AndroidViewBinding is in the hierarchy")
            .that(fragment)
            .isNotNull()

        show = false

        rule.waitForIdle()

        expectError<IllegalStateException>(true, "View null does not have a Fragment set") {
            FragmentManager.findFragment<SimpleEditTextFragment>(
                rule.activity.findViewById(R.id.fragment_layout)
            )
        }
    }

    @Test
    fun testRemovalRemovesState() {
        var show by mutableStateOf(true)

        lateinit var fragment: Fragment
        rule.setContent {
            if (show) {
                AndroidFragment<SimpleEditTextFragment> { fragment = it }
            }
        }

        assertWithMessage("Fragment should be present when AndroidFragment is in the hierarchy")
            .that(fragment)
            .isNotNull()

        var editText: EditText = fragment.requireView().findViewById(R.id.edit_text)
        assertThat(editText.text.toString()).isEqualTo("Default")

        // Update the state to allow verifying the state is destroyed when the
        // AndroidViewBinding is removed from composition
        rule.runOnUiThread { editText.setText("Updated") }

        show = false

        rule.waitForIdle()

        expectError<IllegalStateException>(true, "View null does not have a Fragment set") {
            FragmentManager.findFragment<SimpleEditTextFragment>(
                rule.activity.findViewById(R.id.fragment_layout)
            )
        }

        show = true

        rule.waitForIdle()

        assertWithMessage("Fragment should be present when AndroidFragment is in the hierarchy")
            .that(fragment)
            .isNotNull()
        editText = fragment.requireView().findViewById(R.id.edit_text)

        // State should be reset back to the default
        assertThat(editText.text.toString()).isEqualTo("Default")
    }

    @Test
    fun testRemovalRemovesStateOnBackwardWrite() {
        var showStateA by mutableStateOf(true)

        lateinit var fragment: Fragment
        rule.setContent {
            if (showStateA) {
                AndroidFragment<SimpleEditTextFragment>() { fragment = it }
            } else {
                SideEffect { showStateA = true }
            }
        }

        assertWithMessage("Fragment should be present when AndroidFragment is in the hierarchy")
            .that(fragment)
            .isNotNull()

        var editText: EditText = fragment.requireView().findViewById(R.id.edit_text)
        assertThat(editText.text.toString()).isEqualTo("Default")

        // Update the state to allow verifying the state is destroyed when the
        // AndroidViewBinding is removed from composition
        rule.runOnUiThread { editText.setText("Updated") }

        showStateA = false

        rule.waitForIdle()

        assertWithMessage("Fragment should be present when AndroidViewBinding is in the hierarchy")
            .that(fragment)
            .isNotNull()
        editText = fragment.requireView().findViewById(R.id.edit_text)

        // State should be reset back to the default
        assertThat(editText.text.toString()).isEqualTo("Default")
    }

    @Test
    fun testRemovalRemovesStateOnCompositionDisposalAndRecreation() {
        with(ActivityScenario.launch(ComposeFragmentActivity::class.java)) {
            withActivity {
                assertThat(fragment.lifecycle.currentState).isEqualTo(Lifecycle.State.RESUMED)
                val editText: EditText = fragment.requireView().findViewById(R.id.edit_text)
                assertThat(editText.text.toString()).isEqualTo("Default")

                // Update the state to make sure it gets saved and restored properly
                editText.setText("Updated")

                // detach - attach to dispose composition and compose it again
                val root = composeView!!.parent as ViewGroup
                root.removeView(composeView)
                root.addView(composeView)
            }

            withActivity {
                assertThat(fragment).isNotNull()
                assertThat(fragment.lifecycle.currentState).isEqualTo(Lifecycle.State.RESUMED)
                val recreatedEditText: EditText =
                    fragment.requireView().findViewById(R.id.edit_text)
                assertThat(recreatedEditText.text.toString()).isEqualTo("Default")
            }
        }
    }
}

class EmptyFragmentActivity : FragmentActivity()

class ComposeFragmentActivity : FragmentActivity() {
    var composeView: ComposeView? = null
    lateinit var fragment: Fragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AndroidFragment<SimpleEditTextFragment>(Modifier.requiredSize(50.dp)) { fragment = it }
        }

        composeView =
            window.decorView.findViewById<ViewGroup>(android.R.id.content).getChildAt(0)
                as? ComposeView
    }
}
