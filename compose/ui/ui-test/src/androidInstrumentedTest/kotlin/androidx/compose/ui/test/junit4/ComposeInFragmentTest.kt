/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.compose.ui.test.junit4

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.runEmptyComposeUiTest
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.fragment.app.testing.withFragment
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalTestApi::class)
class ComposeInFragmentTest {
    companion object {
        const val fragment1Text = "Compose in fragment 1"
        const val fragment2Text = "Compose in fragment 2"
    }

    @Test
    fun test() {
        runEmptyComposeUiTest {
            val fragment1 = Fragment1()
            val fragment2 = Fragment2()

            // Launch fragment 1
            val fragmentScenario = launchFragmentInContainer<Fragment1> { fragment1 }
            onNodeWithText(fragment1Text).assertExists()
            onNodeWithText(fragment2Text).assertDoesNotExist()

            // Add fragment 2
            fragmentScenario.withFragment {
                parentFragmentManager.commit { add(android.R.id.content, fragment2) }
            }
            onNodeWithText(fragment1Text).assertExists()
            onNodeWithText(fragment2Text).assertExists()

            // Remove fragment 1
            fragmentScenario.withFragment { parentFragmentManager.commit { remove(fragment1) } }
            onNodeWithText(fragment1Text).assertDoesNotExist()
            onNodeWithText(fragment2Text).assertExists()
        }
    }

    class Fragment1 : Fragment() {
        override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
        ): View? {
            return container?.let {
                ComposeView(container.context).apply {
                    layoutParams = ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT)
                    setContent {
                        Text(fragment1Text, Modifier.background(Color.White).padding(10.dp))
                    }
                }
            }
        }
    }

    class Fragment2 : Fragment() {
        override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
        ): View? {
            return container?.let {
                ComposeView(container.context).apply {
                    layoutParams = ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT)
                    setContent {
                        Text(fragment2Text, Modifier.background(Color.White).padding(10.dp))
                    }
                }
            }
        }
    }
}
