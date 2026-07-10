/*
 * Copyright 2025 The Android Open Source Project
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

import android.view.View
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.test.junit4.v2.createComposeRule
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(minSdk = 26)
class ClearMessageQueueTest {
    @OptIn(ExperimentalCoroutinesApi::class)
    @get:Rule
    val rule = createComposeRule(effectContext = UnconfinedTestDispatcher(null, null))

    /**
     * This test forces the GlobalSnapshotManager to have a coroutine that will execute when the
     * view detaches from the window. Two tests are required because the problem will only show
     * after the test completes. The tests are the same so that the order they run doesn't matter.
     */
    @Test
    fun messageQueueClearedAfterTest1() {
        var attached by mutableStateOf(false)
        rule.setContent {
            LocalView.current.addOnAttachStateChangeListener(
                object : View.OnAttachStateChangeListener {
                    override fun onViewAttachedToWindow(v: View) {
                        attached = true
                    }

                    override fun onViewDetachedFromWindow(v: View) {
                        attached = false
                    }
                }
            )
        }
        rule.runOnIdle { assertThat(attached).isTrue() }
    }

    @Test
    fun messageQueueClearedAfterTest2() {
        var attached by mutableStateOf(false)
        rule.setContent {
            LocalView.current.addOnAttachStateChangeListener(
                object : View.OnAttachStateChangeListener {
                    override fun onViewAttachedToWindow(v: View) {
                        attached = true
                    }

                    override fun onViewDetachedFromWindow(v: View) {
                        attached = false
                    }
                }
            )
        }
        rule.runOnIdle { assertThat(attached).isTrue() }
    }
}
