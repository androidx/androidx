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

package androidx.lifecycle.viewmodel.compose

import android.app.Dialog
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalSavedStateRegistryOwner
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.lifecycle.HasDefaultViewModelProviderFactory
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.ViewTreeLifecycleOwner
import androidx.savedstate.ViewTreeSavedStateRegistryOwner
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
public class ViewModelTest {

    @get:Rule
    public val rule: ComposeContentTestRule = createComposeRule()

    @Test
    public fun nullViewModelStoreOwner() {
        var owner: ViewModelStoreOwner? = null
        rule.setContent {
            val context = LocalContext.current
            val lifecycleOwner = LocalLifecycleOwner.current
            val savedStateRegistryOwner = LocalSavedStateRegistryOwner.current
            DisposableEffect(context, lifecycleOwner, savedStateRegistryOwner) {
                val dialog = Dialog(context).apply {
                    val composeView = ComposeView(context)
                    composeView.setContent {
                        // This should return null because no LocalViewModelStoreOwner was set
                        owner = LocalViewModelStoreOwner.current
                    }
                    setContentView(composeView)
                }
                dialog.show()
                dialog.window?.decorView?.run {
                    // Specifically only set the LifecycleOwner and SavedStateRegistryOwner
                    ViewTreeLifecycleOwner.set(this, lifecycleOwner)
                    ViewTreeSavedStateRegistryOwner.set(this, savedStateRegistryOwner)
                }

                onDispose {
                    dialog.dismiss()
                }
            }
        }

        assertThat(owner).isNull()
    }

    @Test
    public fun viewModelCreatedViaDefaultFactory() {
        val owner = FakeViewModelStoreOwner()
        var createdInComposition: Any? = null
        rule.setContent {
            CompositionLocalProvider(LocalViewModelStoreOwner provides owner) {
                createdInComposition = viewModel<TestViewModel>()
            }
        }

        assertThat(owner.factory.createCalled).isTrue()
        val createdManually = ViewModelProvider(owner).get(TestViewModel::class.java)
        assertThat(createdInComposition).isEqualTo(createdManually)
    }

    @Test
    public fun viewModelCreatedViaDefaultFactoryWithKey() {
        val owner = FakeViewModelStoreOwner()
        var createdInComposition: Any? = null
        rule.setContent {
            CompositionLocalProvider(LocalViewModelStoreOwner provides owner) {
                createdInComposition =
                    viewModel<TestViewModel>(key = "test")
            }
        }

        assertThat(owner.factory.createCalled).isTrue()
        val createdManually = ViewModelProvider(owner).get("test", TestViewModel::class.java)
        assertThat(createdInComposition).isEqualTo(createdManually)
    }

    @Test
    public fun viewModelCreatedViaDefaultFactoryWithCustomOwner() {
        val customOwner = FakeViewModelStoreOwner()
        val owner = FakeViewModelStoreOwner()
        var createdInComposition: Any? = null
        rule.setContent {
            CompositionLocalProvider(LocalViewModelStoreOwner provides owner) {
                createdInComposition = viewModel<TestViewModel>(customOwner)
            }
        }

        assertThat(owner.factory.createCalled).isFalse()
        assertThat(customOwner.factory.createCalled).isTrue()
        val createdManually = ViewModelProvider(customOwner).get(TestViewModel::class.java)
        assertThat(createdInComposition).isEqualTo(createdManually)
    }

    @Test
    public fun customFactoryIsUsedWhenProvided() {
        val owner = FakeViewModelStoreOwner()
        val customFactory = FakeViewModelProviderFactory()
        rule.setContent {
            CompositionLocalProvider(LocalViewModelStoreOwner provides owner) {
                viewModel<TestViewModel>(factory = customFactory)
            }
        }

        assertThat(customFactory.createCalled).isTrue()
    }

    @Test
    public fun customFactoryProducerIsUsedWhenProvided() {
        val owner = FakeViewModelStoreOwner()
        val customFactory = FakeViewModelProviderFactory()
        rule.setContent {
            viewModel<TestViewModel>(owner, factory = customFactory)
        }

        assertThat(customFactory.createCalled).isTrue()
    }

    @Test
    public fun defaultFactoryIsNotUsedWhenCustomProvided() {
        val owner = FakeViewModelStoreOwner()
        val customFactory = FakeViewModelProviderFactory()
        rule.setContent {
            CompositionLocalProvider(LocalViewModelStoreOwner provides owner) {
                viewModel<TestViewModel>(factory = customFactory)
            }
        }

        assertThat(owner.factory.createCalled).isFalse()
    }

    @Test
    public fun createdWithCustomFactoryViewModelIsEqualsToCreatedManually() {
        val owner = FakeViewModelStoreOwner()
        var createdInComposition: Any? = null
        val customFactory = FakeViewModelProviderFactory()
        rule.setContent {
            CompositionLocalProvider(LocalViewModelStoreOwner provides owner) {
                createdInComposition = viewModel<TestViewModel>()
            }
        }

        assertThat(owner.factory.createCalled).isTrue()
        val createdManually = ViewModelProvider(owner, customFactory).get(TestViewModel::class.java)
        assertThat(createdInComposition).isEqualTo(createdManually)
    }

    @Test
    public fun createdWithCustomFactoryViewModelIsEqualsToCreatedManuallyWithKey() {
        val owner = FakeViewModelStoreOwner()
        var createdInComposition: Any? = null
        val customFactory = FakeViewModelProviderFactory()
        rule.setContent {
            CompositionLocalProvider(LocalViewModelStoreOwner provides owner) {
                createdInComposition =
                    viewModel<TestViewModel>(key = "test")
            }
        }

        assertThat(owner.factory.createCalled).isTrue()
        val createdManually =
            ViewModelProvider(owner, customFactory).get("test", TestViewModel::class.java)
        assertThat(createdInComposition).isEqualTo(createdManually)
    }
}

private class TestViewModel : ViewModel()

private class FakeViewModelProviderFactory : ViewModelProvider.Factory {
    var createCalled = false
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass == TestViewModel::class.java)
        createCalled = true
        @Suppress("UNCHECKED_CAST")
        return TestViewModel() as T
    }
}

private class FakeViewModelStoreOwner : ViewModelStoreOwner, HasDefaultViewModelProviderFactory {
    val store = ViewModelStore()
    val factory = FakeViewModelProviderFactory()

    override fun getViewModelStore(): ViewModelStore = store
    override fun getDefaultViewModelProviderFactory(): ViewModelProvider.Factory = factory
}
