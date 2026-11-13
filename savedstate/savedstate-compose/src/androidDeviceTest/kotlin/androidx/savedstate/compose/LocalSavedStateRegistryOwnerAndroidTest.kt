/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.savedstate.compose

import android.widget.FrameLayout
import androidx.compose.runtime.LocalHostDefaultProvider
import androidx.compose.runtime.ViewTreeHostDefaultKey
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.viewinterop.AndroidView
import androidx.kruth.assertThat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.R
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
internal class LocalSavedStateRegistryOwnerAndroidTest {

    @get:Rule val rule = createComposeRule()

    @Test
    fun savedStateRegistryOwnerHostDefaultKey_isViewTreeHostDefaultKey() {
        assertThat(SavedStateRegistryOwnerHostDefaultKey)
            .isInstanceOf<ViewTreeHostDefaultKey<SavedStateRegistryOwner>>()
        val viewTreeKey =
            SavedStateRegistryOwnerHostDefaultKey as ViewTreeHostDefaultKey<SavedStateRegistryOwner>
        assertThat(viewTreeKey.tagKey).isEqualTo(R.id.view_tree_saved_state_registry_owner)
    }

    @Test
    fun localSavedStateRegistryOwner_resolvesFromViewTreeTagViaHostDefaultKey() {
        val expectedOwner = FakeSavedStateRegistryOwner()
        var resolvedOwner: SavedStateRegistryOwner? = null

        rule.setContent {
            val view = LocalView.current
            view.setViewTreeSavedStateRegistryOwner(expectedOwner)
            resolvedOwner =
                LocalHostDefaultProvider.current.getHostDefault(
                    SavedStateRegistryOwnerHostDefaultKey
                )
        }

        assertThat(resolvedOwner).isSameInstanceAs(expectedOwner)
    }

    @Test
    fun localSavedStateRegistryOwner_withNestedComposeView_resolvesFromViewTree() {
        val expectedOwner = FakeSavedStateRegistryOwner()
        var resolvedOwner: SavedStateRegistryOwner? = null

        rule.setContent {
            AndroidView(
                factory = { context ->
                    FrameLayout(context).apply {
                        setViewTreeLifecycleOwner(expectedOwner)
                        setViewTreeSavedStateRegistryOwner(expectedOwner)
                        addView(
                            ComposeView(context).apply {
                                setContent {
                                    resolvedOwner = LocalSavedStateRegistryOwner.current
                                }
                            }
                        )
                    }
                }
            )
        }

        rule.runOnIdle { assertThat(resolvedOwner).isSameInstanceAs(expectedOwner) }
    }

    private class FakeSavedStateRegistryOwner : SavedStateRegistryOwner {
        private val lifecycleRegistry = LifecycleRegistry.createUnsafe(owner = this)
        private val controller =
            SavedStateRegistryController.create(owner = this).apply {
                performAttach()
                performRestore(savedState = null)
            }

        init {
            lifecycleRegistry.currentState = Lifecycle.State.RESUMED
        }

        override val lifecycle: Lifecycle
            get() = lifecycleRegistry

        override val savedStateRegistry: SavedStateRegistry
            get() = controller.savedStateRegistry
    }
}
