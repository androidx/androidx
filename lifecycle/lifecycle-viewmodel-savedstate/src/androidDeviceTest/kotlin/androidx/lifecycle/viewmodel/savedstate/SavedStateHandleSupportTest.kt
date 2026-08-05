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

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.enableSavedStateHandles
import androidx.test.annotation.UiThreadTest
import androidx.test.core.app.ActivityScenario
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
        val component = TestComponent()
        component.enableSavedStateHandles()
        val handle = component.createSavedStateHandle("test")
        component.resume()
        handle.set("a", "1")

        val recreated = component.recreate(keepingViewModels = false)
        recreated.enableSavedStateHandles()
        val restoredHandle = recreated.createSavedStateHandle("test")

        assertThat(restoredHandle.get<String>("a")).isEqualTo("1")
    }

    @UiThreadTest
    @Test
    fun testSavedStateHandleSupportWithConfigChange() {
        val component = TestComponent()
        component.enableSavedStateHandles()
        val handle = component.createSavedStateHandle("test")
        component.resume()
        handle.set("a", "1")
        val interim = component.recreate(keepingViewModels = true)
        interim.enableSavedStateHandles()
        handle.set("b", "2")
        interim.resume()

        val recreated = interim.recreate(keepingViewModels = false)
        recreated.enableSavedStateHandles()
        val restoredHandle = recreated.createSavedStateHandle("test")

        assertThat(restoredHandle.get<String>("a")).isEqualTo("1")
        assertThat(restoredHandle.get<String>("b")).isEqualTo("2")
    }

    @UiThreadTest
    @Test
    fun testSavedStateHandleSupportWithDoubleConfigChange() {
        val component = TestComponent()
        component.enableSavedStateHandles()
        val handle = component.createSavedStateHandle("test")
        component.resume()
        handle.set("a", "1")

        // First configuration change (rotation)
        var interim = component.recreate(keepingViewModels = true)
        interim.enableSavedStateHandles()
        handle.set("b", "2")
        interim.resume()

        // Second configuration change (rotation)
        interim = interim.recreate(keepingViewModels = true)
        interim.enableSavedStateHandles()
        handle.set("c", "3")
        interim.resume()

        // Process death (recreation without keeping view models)
        val recreated = interim.recreate(keepingViewModels = false)
        recreated.enableSavedStateHandles()
        val restoredHandle = recreated.createSavedStateHandle("test")

        assertThat(restoredHandle.get<String>("a")).isEqualTo("1")
        assertThat(restoredHandle.get<String>("b")).isEqualTo("2")
        assertThat(restoredHandle.get<String>("c")).isEqualTo("3")
    }

    @UiThreadTest
    @Test
    fun testSavedStateHandleSupportWithActivityDestroyed() {
        val component = TestComponent()
        component.enableSavedStateHandles()
        val handle = component.createSavedStateHandle("test")
        component.resume()
        handle.set("a", "1")
        val interim = component.recreate(keepingViewModels = true)
        interim.enableSavedStateHandles()
        handle.set("b", "2")
        interim.resume()

        val recreated = interim.recreate(keepingViewModels = false)
        recreated.enableSavedStateHandles()
        (recreated.lifecycle as LifecycleRegistry).currentState = Lifecycle.State.CREATED
        // during activity recreation, perform save may be called during restore, ensure
        // this performSave does not override the state that has been restored
        recreated.performSave(Bundle())
        val restoredHandle = recreated.createSavedStateHandle("test")

        assertThat(restoredHandle.get<String>("a")).isEqualTo("1")
        assertThat(restoredHandle.get<String>("b")).isEqualTo("2")
    }

    @UiThreadTest
    @Test
    fun failWithNoInstallation() {
        val component = TestComponent()
        try {
            component.createSavedStateHandle("key")
            Assert.fail("createSavedStateHandle should fail when install() wasn't called")
        } catch (e: IllegalStateException) {}
    }

    @UiThreadTest
    @Test
    fun defaultArgs() {
        val component = TestComponent()
        component.enableSavedStateHandles()
        val bundle = Bundle()
        bundle.putString("key", "value")
        val handle = component.createSavedStateHandle("test", bundle)
        assertThat(handle.get<String>("key")).isEqualTo("value")
    }

    @UiThreadTest
    @Test
    fun testSavedStateHandleSupportWithDestroyedOwner() {
        val component = TestComponent()
        component.enableSavedStateHandles()
        component.resume()
        component.destroy()

        // Save state when host destroyed. Must not crash.
        val bundle = Bundle()
        component.performSave(bundle)
    }

    @Test
    fun testSavedStateHandleCacheClearedOnStoreClear() {
        // Regression test for b/539581812: verify stateHolder cache is cleared when store is
        // cleared.
        val scenario = ActivityScenario.launch(ComponentActivity::class.java)
        scenario.onActivity { activity ->
            // Query ViewModel once to populate cache.
            val initialVm = ViewModelProvider(activity)[TestViewModel::class.java]
            assertThat(initialVm.savedStateHandle.get<String>("test_extra")).isNull()

            // Mutate host intent.
            val newIntent = Intent().putExtra("test_extra", "new_value")
            activity.intent = newIntent
            assertThat(activity.intent.getStringExtra("test_extra")).isEqualTo("new_value")

            // Clear host VM store.
            activity.viewModelStore.clear()

            // Re-query VM. Cache must be empty and resolve new extras.
            val viewModel = ViewModelProvider(activity)[TestViewModel::class.java]
            assertThat(viewModel.savedStateHandle.get<String>("test_extra")).isEqualTo("new_value")
        }
    }

    class TestViewModel(val savedStateHandle: SavedStateHandle) : ViewModel()

    @UiThreadTest
    @Test
    fun testRestoredStateLifecycleOnStoreClear() {
        val component = TestComponent()
        component.enableSavedStateHandles()
        val handle1 = component.createSavedStateHandle("vm1")
        handle1.set("key1", "value1")
        val handle2 = component.createSavedStateHandle("vm2")
        handle2.set("key2", "value2")

        val recreated = component.recreate(keepingViewModels = false)
        recreated.enableSavedStateHandles()

        val restoredHandle1 = recreated.createSavedStateHandle("vm1")
        // Consume the state for VM1. This consumes and clears its restored state.
        assertThat(restoredHandle1.get<String>("key1")).isEqualTo("value1")

        // Clear the ViewModelStore. This clears the StateHolder, forcing recreation.
        recreated.viewModelStore.clear()

        // Verify that the restored state of VM1 is not restored again because it was
        // already consumed and cleared.
        val restoredHandle1Again = recreated.createSavedStateHandle("vm1")
        assertThat(restoredHandle1Again.get<String>("key1")).isNull()

        // Verify that the restored state of VM2 is still restored because it was
        // not yet consumed and cleared.
        val restoredHandle2 = recreated.createSavedStateHandle("vm2")
        assertThat(restoredHandle2.get<String>("key2")).isEqualTo("value2")
    }

    @UiThreadTest
    @Test
    fun testRestoredStateLifecycleOnStoreClearWithRotation() {
        val component = TestComponent()
        component.enableSavedStateHandles()
        val handle1 = component.createSavedStateHandle("vm1")
        handle1.set("key1", "value1")
        val handle2 = component.createSavedStateHandle("vm2")
        handle2.set("key2", "value2")

        // Rotate component (keeping view models)
        val recreated = component.recreate(keepingViewModels = true)
        recreated.enableSavedStateHandles()

        // Consume VM1
        val restoredHandle1 = recreated.createSavedStateHandle("vm1")
        // Consume the state for VM1. This consumes and clears its restored state.
        assertThat(restoredHandle1.get<String>("key1")).isEqualTo("value1")

        // Clear VM store after rotation
        recreated.viewModelStore.clear()

        // Verify that the restored state of VM1 is restored again because it was re-imported
        // from the OS bundle on rotation (original behavior).
        val restoredHandle1Again = recreated.createSavedStateHandle("vm1")
        assertThat(restoredHandle1Again.get<String>("key1")).isEqualTo("value1")

        // Verify that the restored state of VM2 is still restored because it was
        // not yet consumed and cleared.
        val restoredHandle2 = recreated.createSavedStateHandle("vm2")
        assertThat(restoredHandle2.get<String>("key2")).isEqualTo("value2")
    }
}
