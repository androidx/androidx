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
import androidx.lifecycle.LifecycleRegistry
import androidx.savedstate.findViewTreeSavedStateRegistryOwner
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.testutils.withActivity
import androidx.testutils.withUse
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import leakcanary.DetectLeaksAfterTestSuccess
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@Suppress("DEPRECATION")
@LargeTest
@RunWith(AndroidJUnit4::class)
class ComponentDialogTest {

    @get:Rule val rule = DetectLeaksAfterTestSuccess()

    @Test
    fun testLifecycle() {
        withUse(ActivityScenario.launch(EmptyContentActivity::class.java)) {
            val dialog = withActivity { ComponentDialog(this) }
            val lifecycle = dialog.lifecycle
            assertThat(lifecycle.currentState).isEqualTo(Lifecycle.State.INITIALIZED)

            onActivity { dialog.show() }
            assertThat(lifecycle.currentState).isEqualTo(Lifecycle.State.RESUMED)

            onActivity { dialog.dismiss() }
            assertThat(lifecycle.currentState).isEqualTo(Lifecycle.State.DESTROYED)

            assertWithMessage("A new Lifecycle object should be created after destruction")
                .that(dialog.lifecycle)
                .isNotSameInstanceAs(lifecycle)
        }
    }

    @Test
    @Throws(Throwable::class)
    fun savedState() {
        withUse(ActivityScenario.launch(SavedStateActivity::class.java)) {
            val dialog = withActivity { ComponentDialog(this).also { it.show() } }
            val lifecycle = dialog.lifecycle
            assertThat(lifecycle.currentState).isEqualTo(Lifecycle.State.RESUMED)

            // Initialize saved state
            withActivity {
                val registry = dialog.savedStateRegistry
                val savedState = registry.consumeRestoredStateForKey(CALLBACK_KEY)
                assertThat(savedState).isNull()
                registry.registerSavedStateProvider(CALLBACK_KEY, DefaultProvider())
            }

            // Destroy dialog and restore saved instance state
            val savedState = dialog.onSaveInstanceState()
            assertThat(savedState).isNotNull()
            onActivity { dialog.dismiss() }
            assertThat(lifecycle.currentState).isEqualTo(Lifecycle.State.DESTROYED)
            val restoredDialog = withActivity { ComponentDialog(this) }
            withActivity {
                assertThat((restoredDialog.lifecycle as LifecycleRegistry).currentState)
                    .isEqualTo(Lifecycle.State.INITIALIZED)
                restoredDialog.onRestoreInstanceState(savedState)
            }
            assertThat(hasDefaultSavedState(restoredDialog.savedStateRegistry)).isTrue()
        }
    }

    @Test
    fun testViewTreeSavedStateRegistryOwner() {
        withUse(ActivityScenario.launch(EmptyContentActivity::class.java)) {
            val dialog = withActivity { ViewOwnerDialog(this).also { it.show() } }
            val lifecycle = dialog.lifecycle
            assertThat(lifecycle.currentState).isEqualTo(Lifecycle.State.RESUMED)

            // Initialize saved state
            withActivity {
                val registry = dialog.savedStateRegistry
                val savedState = registry.consumeRestoredStateForKey(CALLBACK_KEY)
                assertThat(savedState).isNull()
                registry.registerSavedStateProvider(CALLBACK_KEY, DefaultProvider())
                val viewTreeOwner = dialog.view.findViewTreeSavedStateRegistryOwner()
                assertThat(viewTreeOwner).isNotNull()
                assertThat(viewTreeOwner).isEqualTo(dialog)
            }

            // Destroy dialog and restore saved instance state
            val savedState = dialog.onSaveInstanceState()
            assertThat(savedState).isNotNull()
            onActivity { dialog.dismiss() }
            assertThat(lifecycle.currentState).isEqualTo(Lifecycle.State.DESTROYED)
            val restoredDialog = withActivity { ViewOwnerDialog(this) }
            withActivity {
                assertThat((restoredDialog.lifecycle as LifecycleRegistry).currentState)
                    .isEqualTo(Lifecycle.State.INITIALIZED)
                restoredDialog.onRestoreInstanceState(savedState)
                val viewTreeOwner = restoredDialog.view.findViewTreeSavedStateRegistryOwner()
                assertThat(viewTreeOwner).isNotNull()
                assertThat(viewTreeOwner).isEqualTo(restoredDialog)
            }
            assertThat(hasDefaultSavedState(restoredDialog.savedStateRegistry)).isTrue()
        }
    }

    @Test
    fun testOnBackPressed() {
        withUse(ActivityScenario.launch(EmptyContentActivity::class.java)) {
            val dialog = withActivity { DoubleTapBackDialog(this).also { it.show() } }
            val lifecycle = dialog.lifecycle
            assertThat(lifecycle.currentState).isEqualTo(Lifecycle.State.RESUMED)

            onActivity { dialog.onBackPressed() }
            assertThat(lifecycle.currentState).isEqualTo(Lifecycle.State.RESUMED)
            assertThat(dialog.backCount).isEqualTo(1)

            onActivity { dialog.onBackPressed() }
            assertThat(lifecycle.currentState).isEqualTo(Lifecycle.State.DESTROYED)
        }
    }

    @Test
    fun testViewTreeOnBackPressedDispatcherOwner() {
        withUse(ActivityScenario.launch(EmptyContentActivity::class.java)) {
            val dialog = withActivity { ViewOwnerDialog(this).also { it.show() } }
            val lifecycle = dialog.lifecycle
            assertThat(lifecycle.currentState).isEqualTo(Lifecycle.State.RESUMED)

            onActivity { dialog.onBackPressed() }
            assertThat(lifecycle.currentState).isEqualTo(Lifecycle.State.RESUMED)
            assertThat(dialog.view.backCount).isEqualTo(1)

            onActivity { dialog.onBackPressed() }
            assertThat(lifecycle.currentState).isEqualTo(Lifecycle.State.DESTROYED)
        }
    }
}

class DoubleTapBackDialog(context: Context) : ComponentDialog(context) {
    var backCount = 0

    private val onBackPressedCallback =
        object : OnBackPressedCallback(true) {
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

        private val onBackPressedCallback =
            object : OnBackPressedCallback(true) {
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
                onBackPressedCallback,
            )
        }
    }
}
