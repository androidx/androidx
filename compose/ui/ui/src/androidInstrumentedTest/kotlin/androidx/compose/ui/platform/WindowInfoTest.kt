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

package androidx.compose.ui.platform

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runComposeUiTest
import androidx.fragment.app.Fragment
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.test.filters.LargeTest
import org.junit.Test

@LargeTest
@OptIn(ExperimentalTestApi::class)
class WindowInfoTest {
    @Test
    fun launchFragment_windowInfo_isWindowFocused_true() {
        runComposeUiTest {
            launchFragmentInContainer<TestFragment>().onFragment {
                waitUntil("isWindowFocused", timeoutMillis = 5_000) { it.isWindowFocused == true }
            }
        }
    }

    class TestFragment : Fragment() {
        var isWindowFocused: Boolean? = null

        override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
        ): View? {
            return container?.let {
                ComposeView(container.context).apply {
                    layoutParams =
                        ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                }
            }
        }

        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            (view as ComposeView).setContent {
                isWindowFocused = LocalWindowInfo.current.isWindowFocused
            }
        }
    }
}
