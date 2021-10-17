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

package androidx.lifecycle.viewmodel.savedstate

import android.os.Bundle
import androidx.lifecycle.DEFAULT_ARGS_KEY
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.SAVED_STATE_REGISTRY_OWNER_KEY
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.VIEW_MODEL_STORE_OWNER_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.installSavedStateHandleSupport
import androidx.lifecycle.viewmodel.MutableCreationExtras
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.test.annotation.UiThreadTest
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
class SavedStateHandleSupportTest {

    @UiThreadTest
    @Test
    fun testSavedStateHandleSupport() {
        val component = Component()
        component.installSavedStateHandleSupport()
        val handle = component.createSavedStateHandle("test")
        component.resume()
        handle.set("a", "1")

        val recreated = component.recreate(keepingViewModels = false)
        recreated.installSavedStateHandleSupport()
        val restoredHandle = recreated.createSavedStateHandle("test")

        assertThat(restoredHandle.get<String>("a")).isEqualTo("1")
    }

    @UiThreadTest
    @Test
    fun testSavedStateHandleSupportWithConfigChange() {
        val component = Component()
        component.installSavedStateHandleSupport()
        val handle = component.createSavedStateHandle("test")
        component.resume()
        handle.set("a", "1")
        val interim = component.recreate(keepingViewModels = true)
        handle.set("b", "2")
        interim.resume()

        val recreated = interim.recreate(keepingViewModels = false)
        recreated.installSavedStateHandleSupport()
        val restoredHandle = recreated.createSavedStateHandle("test")

        assertThat(restoredHandle.get<String>("a")).isEqualTo("1")
        assertThat(restoredHandle.get<String>("b")).isEqualTo("2")
    }

    @UiThreadTest
    @Test
    fun failWithNoInstallation() {
        val component = Component()
        try {
            component.createSavedStateHandle("key")
            Assert.fail("createSavedStateHandle should fail when install() wasn't called")
        } catch (e: IllegalStateException) {
        }
    }

    @UiThreadTest
    @Test
    fun defaultArgs() {
        val component = Component()
        component.installSavedStateHandleSupport()
        val bundle = Bundle()
        bundle.putString("key", "value")
        val handle = component.createSavedStateHandle("test", bundle)
        assertThat(handle.get<String>("key")).isEqualTo("value")
    }
}

private class Component(
    val vmStore: ViewModelStore = ViewModelStore(),
    bundle: Bundle? = null,
) : SavedStateRegistryOwner, LifecycleOwner, ViewModelStoreOwner {

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateController = SavedStateRegistryController.create(this)

    init {
        savedStateController.performRestore(bundle)
    }

    override fun getLifecycle(): Lifecycle = lifecycleRegistry
    override fun getSavedStateRegistry(): SavedStateRegistry =
        savedStateController.savedStateRegistry

    override fun getViewModelStore(): ViewModelStore = vmStore

    fun resume() {
        lifecycleRegistry.currentState = Lifecycle.State.RESUMED
    }

    fun recreate(keepingViewModels: Boolean): Component {
        val bundle = Bundle()
        lifecycleRegistry.currentState = Lifecycle.State.CREATED
        savedStateController.performSave(bundle)
        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
        if (!keepingViewModels) vmStore.clear()
        return Component(vmStore.takeIf { keepingViewModels } ?: ViewModelStore(), bundle)
    }

    fun createSavedStateHandle(key: String, bundle: Bundle? = null): SavedStateHandle {
        val extras = MutableCreationExtras()
        extras[VIEW_MODEL_STORE_OWNER_KEY] = this
        extras[SAVED_STATE_REGISTRY_OWNER_KEY] = this
        extras[ViewModelProvider.NewInstanceFactory.VIEW_MODEL_KEY] = key
        if (bundle != null) extras[DEFAULT_ARGS_KEY] = bundle
        return extras.createSavedStateHandle()
    }
}