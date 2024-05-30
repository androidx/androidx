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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.enableSavedStateHandles
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
}
