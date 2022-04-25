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

package androidx.activity

import android.content.Context
import android.view.View
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.testutils.withActivity
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class ComponentDialogTest {
    @Test
    fun testLifecycle() {
        with(ActivityScenario.launch(EmptyContentActivity::class.java)) {
            val dialog = withActivity {
                ComponentDialog(this)
            }
            val lifecycle = dialog.lifecycle
            assertThat(lifecycle.currentState).isEqualTo(Lifecycle.State.INITIALIZED)

            onActivity {
                dialog.show()
            }
            assertThat(lifecycle.currentState).isEqualTo(Lifecycle.State.RESUMED)

            onActivity {
                dialog.dismiss()
            }
            assertThat(lifecycle.currentState).isEqualTo(Lifecycle.State.DESTROYED)

            assertWithMessage("A new Lifecycle object should be created after destruction")
                .that(dialog.lifecycle)
                .isNotSameInstanceAs(lifecycle)
        }
    }

    @Test
    fun testOnBackPressed() {
        with(ActivityScenario.launch(EmptyContentActivity::class.java)) {
            val dialog = withActivity {
                DoubleTapBackDialog(this).also {
                    it.show()
                }
            }
            val lifecycle = dialog.lifecycle
            assertThat(lifecycle.currentState).isEqualTo(Lifecycle.State.RESUMED)

            onActivity {
                dialog.onBackPressed()
            }
            assertThat(lifecycle.currentState).isEqualTo(Lifecycle.State.RESUMED)
            assertThat(dialog.backCount).isEqualTo(1)

            onActivity {
                dialog.onBackPressed()
            }
            assertThat(lifecycle.currentState).isEqualTo(Lifecycle.State.DESTROYED)
        }
    }

    @Test
    fun testViewTreeOnBackPressedDispatcherOwner() {
        with(ActivityScenario.launch(EmptyContentActivity::class.java)) {
            val dialog = withActivity {
                ViewOwnerDialog(this).also {
                    it.show()
                }
            }
            val lifecycle = dialog.lifecycle
            assertThat(lifecycle.currentState).isEqualTo(Lifecycle.State.RESUMED)

            onActivity {
                dialog.onBackPressed()
            }
            assertThat(lifecycle.currentState).isEqualTo(Lifecycle.State.RESUMED)
            assertThat(dialog.view.backCount).isEqualTo(1)

            onActivity {
                dialog.onBackPressed()
            }
            assertThat(lifecycle.currentState).isEqualTo(Lifecycle.State.DESTROYED)
        }
    }
}

class DoubleTapBackDialog(context: Context) : ComponentDialog(context) {
    var backCount = 0

    private val onBackPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            backCount++
            remove()
        }
    }

    init {
        onBackPressedDispatcher.addCallback(this, onBackPressedCallback)
    }
}

class ViewOwnerDialog(context: Context) : ComponentDialog(context) {
    val view = BackHandlingView(context)

    init {
        setContentView(view)
    }

    class BackHandlingView(context: Context) : View(context) {
        var backCount = 0

        private val onBackPressedCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                backCount++
                remove()
            }
        }

        override fun onAttachedToWindow() {
            super.onAttachedToWindow()
            val onBackPressedDispatcherOwner = findViewTreeOnBackPressedDispatcherOwner()!!
            onBackPressedDispatcherOwner.onBackPressedDispatcher.addCallback(
                onBackPressedDispatcherOwner,
                onBackPressedCallback
            )
        }
    }
}