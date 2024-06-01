/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.compose.ui.samples

import android.os.Bundle
import android.view.ViewGroup
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewbinding.samples.R
import androidx.compose.ui.viewbinding.samples.databinding.SampleEditTextLayoutBinding
import androidx.compose.ui.viewbinding.samples.databinding.TestFragmentLayoutBinding
import androidx.compose.ui.viewinterop.AndroidViewBinding
import androidx.fragment.app.FragmentActivity
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

    @get:Rule val rule = createAndroidComposeRule<EmptyFragmentActivity>()

    @Test
    fun testRemoval() {
        var show by mutableStateOf(true)

        rule.setContent {
            if (show) {
                AndroidViewBinding(TestFragmentLayoutBinding::inflate)
            }
        }

        var fragment =
            rule.activity.supportFragmentManager.findFragmentById(R.id.fragment_container)
        assertWithMessage("Fragment should be present when AndroidViewBinding is in the hierarchy")
            .that(fragment)
            .isNotNull()

        show = false

        rule.waitForIdle()

        fragment = rule.activity.supportFragmentManager.findFragmentById(R.id.fragment_container)
        assertWithMessage("Fragment should be removed when the AndroidViewBinding is removed")
            .that(fragment)
            .isNull()
    }

    @Test
    fun testRemovalRemovesState() {
        var show by mutableStateOf(true)

        rule.setContent {
            if (show) {
                AndroidViewBinding(TestFragmentLayoutBinding::inflate)
            }
        }

        var fragment =
            rule.activity.supportFragmentManager.findFragmentById(R.id.fragment_container)
        assertWithMessage("Fragment should be present when AndroidViewBinding is in the hierarchy")
            .that(fragment)
            .isNotNull()

        var binding = SampleEditTextLayoutBinding.bind(fragment!!.requireView())
        assertThat(binding.editText.text.toString()).isEqualTo("Default")

        // Update the state to allow verifying the state is destroyed when the
        // AndroidViewBinding is removed from composition
        rule.runOnUiThread { binding.editText.setText("Updated") }

        show = false

        rule.waitForIdle()

        fragment = rule.activity.supportFragmentManager.findFragmentById(R.id.fragment_container)
        assertWithMessage("Fragment should be removed when the AndroidViewBinding is removed")
            .that(fragment)
            .isNull()

        show = true

        rule.waitForIdle()

        fragment = rule.activity.supportFragmentManager.findFragmentById(R.id.fragment_container)
        assertWithMessage("Fragment should be present when AndroidViewBinding is in the hierarchy")
            .that(fragment)
            .isNotNull()
        binding = SampleEditTextLayoutBinding.bind(fragment!!.requireView())

        // State should be reset back to the default
        assertThat(binding.editText.text.toString()).isEqualTo("Default")
    }

    @Test
    fun testRemovalRemovesStateOnBackwardWrite() {
        var showStateA by mutableStateOf(true)

        rule.setContent {
            if (showStateA) {
                AndroidViewBinding(TestFragmentLayoutBinding::inflate)
            } else {
                SideEffect { showStateA = true }
            }
        }

        var fragment =
            rule.activity.supportFragmentManager.findFragmentById(R.id.fragment_container)
        assertWithMessage("Fragment should be present when AndroidViewBinding is in the hierarchy")
            .that(fragment)
            .isNotNull()

        var binding = SampleEditTextLayoutBinding.bind(fragment!!.requireView())
        assertThat(binding.editText.text.toString()).isEqualTo("Default")

        // Update the state to allow verifying the state is destroyed when the
        // AndroidViewBinding is removed from composition
        rule.runOnUiThread { binding.editText.setText("Updated") }

        showStateA = false

        rule.waitForIdle()

        fragment = rule.activity.supportFragmentManager.findFragmentById(R.id.fragment_container)
        assertWithMessage("Fragment should be present when AndroidViewBinding is in the hierarchy")
            .that(fragment)
            .isNotNull()
        binding = SampleEditTextLayoutBinding.bind(fragment!!.requireView())

        // State should be reset back to the default
        assertThat(binding.editText.text.toString()).isEqualTo("Default")
    }

    @Test
    fun testRemovalRemovesStateOnCompositionDisposalAndRecreation() {
        with(ActivityScenario.launch(ComposeInflatedFragmentActivity::class.java)) {
            withActivity {
                val fragment = supportFragmentManager.findFragmentById(R.id.fragment_container)!!
                assertThat(fragment.lifecycle.currentState).isEqualTo(Lifecycle.State.RESUMED)
                val binding = SampleEditTextLayoutBinding.bind(fragment.requireView())
                assertThat(binding.editText.text.toString()).isEqualTo("Default")

                // Update the state to make sure it gets saved and restored properly
                binding.editText.setText("Updated")

                // detach - attach to dispose composition and compose it again
                val root = composeView!!.parent as ViewGroup
                root.removeView(composeView)
                root.addView(composeView)
            }

            withActivity {
                val fragment = supportFragmentManager.findFragmentById(R.id.fragment_container)
                assertThat(fragment).isNotNull()
                assertThat(fragment!!.lifecycle.currentState).isEqualTo(Lifecycle.State.RESUMED)
                val recreatedBinding = SampleEditTextLayoutBinding.bind(fragment.requireView())
                assertThat(recreatedBinding.editText.text.toString()).isEqualTo("Default")
            }
        }
    }
}

class EmptyFragmentActivity : FragmentActivity()

class ComposeInflatedFragmentActivity : FragmentActivity() {
    var composeView: ComposeView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AndroidViewBinding(
                TestFragmentLayoutBinding::inflate,
                Modifier.requiredSize(50.dp),
            )
        }

        composeView =
            window.decorView.findViewById<ViewGroup>(android.R.id.content).getChildAt(0)
                as? ComposeView
    }
}
