/*
 * Copyright 2019 The Android Open Source Project
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

import android.app.Application
import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SAVED_STATE_REGISTRY_OWNER_KEY
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.SavedStateViewModelFactory
import androidx.lifecycle.VIEW_MODEL_STORE_OWNER_KEY
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.enableSavedStateHandles
import androidx.lifecycle.viewmodel.MutableCreationExtras
import androidx.savedstate.SavedStateRegistryOwner
import androidx.test.annotation.UiThreadTest
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
class SavedStateFactoryTest {

    @Suppress("DEPRECATION")
    @get:Rule
    var activityRule = androidx.test.rule.ActivityTestRule(MyActivity::class.java)

    @UiThreadTest
    @Test
    fun testLegacyCreateAndroidVM() {
        val savedStateVMFactory =
            SavedStateViewModelFactory(activityRule.activity.application, activityRule.activity)
        val vm = ViewModelProvider(ViewModelStore(), savedStateVMFactory)
        assertThat(vm[MyAndroidViewModel::class.java].handle).isNotNull()
        assertThat(vm[MyViewModel::class.java].handle).isNotNull()
    }

    @UiThreadTest
    @Test
    fun testCreateAndroidVM() {
        val savedStateVMFactory = SavedStateViewModelFactory()
        val component = TestComponent()
        component.enableSavedStateHandles()
        val extras = component.extras
        extras[APPLICATION_KEY] = activityRule.activity.application
        val vm = ViewModelProvider(component.viewModelStore, savedStateVMFactory, extras)
        assertThat(vm[MyAndroidViewModel::class.java].handle).isNotNull()
        assertThat(vm[MyViewModel::class.java].handle).isNotNull()
    }

    @UiThreadTest
    @Test
    fun testCreateAndroidWithStatefulFactoryVM() {
        val savedStateVMFactory = SavedStateViewModelFactory(null, activityRule.activity)
        val component = TestComponent()
        component.enableSavedStateHandles()
        val extras = component.extras
        extras[APPLICATION_KEY] = activityRule.activity.application
        val vm = ViewModelProvider(component.viewModelStore, savedStateVMFactory, extras)
        assertThat(vm[MyAndroidViewModel::class.java].handle).isNotNull()
        assertThat(vm[MyViewModel::class.java].handle).isNotNull()
    }

    @UiThreadTest
    @Test
    fun testCreateAndroidVMWrongParameterOrder() {
        val savedStateVMFactory = SavedStateViewModelFactory()
        val component = TestComponent()
        component.enableSavedStateHandles()
        val extras = component.extras
        extras[APPLICATION_KEY] = activityRule.activity.application
        val vm = ViewModelProvider(component.viewModelStore, savedStateVMFactory, extras)
        try {
            assertThat(vm[WrongOrderAndroidViewModel::class.java].handle).isNotNull()
            fail()
        } catch (e: UnsupportedOperationException) {
            assertThat(e)
                .hasMessageThat()
                .isEqualTo(
                    "Class WrongOrderAndroidViewModel must have parameters in the proper order: " +
                        "[class android.app.Application, class androidx.lifecycle.SavedStateHandle]"
                )
        }
    }

    @UiThreadTest
    @Test
    fun testLegacyCreateFailAndroidVM() {
        val savedStateVMFactory = SavedStateViewModelFactory(null, activityRule.activity)
        val vm = ViewModelProvider(ViewModelStore(), savedStateVMFactory)
        try {
            vm[MyAndroidViewModel::class.java]
            fail("Creating an AndroidViewModel should fail when no Application is provided")
        } catch (e: RuntimeException) {
            // Exception message varies across platform versions, just make sure it's thrown.
        }
        assertThat(vm[MyViewModel::class.java].handle).isNotNull()
    }

    @UiThreadTest
    @Test
    fun testCreateFailAndroidVM() {
        val savedStateVMFactory = SavedStateViewModelFactory()
        val component = TestComponent()
        component.enableSavedStateHandles()
        val vm = ViewModelProvider(component.viewModelStore, savedStateVMFactory, component.extras)
        try {
            vm[MyAndroidViewModel::class.java]
            fail("Creating an AndroidViewModel should fail when no Application extras is provided")
        } catch (e: RuntimeException) {}
        assertThat(vm[MyViewModel::class.java].handle).isNotNull()
    }

    @UiThreadTest
    @Test
    fun testLegacyCreateAndroidAbstractVM() {
        val activity = activityRule.activity
        val app = activity.application
        @Suppress("DEPRECATION")
        val savedStateVMFactory =
            object : androidx.lifecycle.AbstractSavedStateViewModelFactory(activity, null) {
                override fun <T : ViewModel> create(
                    key: String,
                    modelClass: Class<T>,
                    handle: SavedStateHandle,
                ): T {
                    return modelClass.cast(MyAndroidViewModel(app, handle))!!
                }
            }
        val vm = ViewModelProvider(ViewModelStore(), savedStateVMFactory)
        assertThat(vm[MyAndroidViewModel::class.java].handle).isNotNull()
    }

    @UiThreadTest
    @Test
    fun testLegacyMethodsWithEmptyConstructor() {
        val factory = SavedStateViewModelFactory()
        try {
            factory.create(MyViewModel::class.java)
            fail()
        } catch (e: UnsupportedOperationException) {}

        try {
            factory.create("a", MyViewModel::class.java)
            fail()
        } catch (e: UnsupportedOperationException) {}

        @Suppress("DEPRECATION")
        val absFactory =
            object : androidx.lifecycle.AbstractSavedStateViewModelFactory() {
                override fun <T : ViewModel> create(
                    key: String,
                    modelClass: Class<T>,
                    handle: SavedStateHandle,
                ): T = create(modelClass)
            }
        try {
            absFactory.create(MyViewModel::class.java)
            fail()
        } catch (e: UnsupportedOperationException) {}
    }

    private fun testFactoryRecreation(
        factoryProducer: (SavedStateRegistryOwner) -> ViewModelProvider.Factory
    ) {
        var component = TestComponent()
        val factory = factoryProducer(component)

        val vm = ViewModelProvider(component, factory)[MyViewModel::class.java]
        vm.handle.set("key", "value1")

        // Simulate host recreation (e.g. configuration change) twice to verify
        // that the OnRecreation hook successfully re-primes itself on the first
        // recreation and survives multiple consecutive configuration changes.
        component = component.recreate(keepingViewModels = true)
        component.resume()
        component = component.recreate(keepingViewModels = true)
        component.resume()

        // Update the surviving ViewModel's SavedStateHandle while it is not active
        // (bypassing the ViewModelProvider/factory initialization).
        val survivingVm =
            component.viewModelStore[
                    "androidx.lifecycle.ViewModelProvider.DefaultKey:${MyViewModel::class.java.canonicalName}"]
                as MyViewModel
        survivingVm.handle.set("key", "value2")

        // Simulate host destruction (e.g. process death).
        val savedBundle = Bundle()
        component.performSave(savedBundle)

        // Restore the component and verify the updated handle value was saved and restored.
        val finalComponent = TestComponent(bundle = savedBundle)
        val finalFactory = factoryProducer(finalComponent)
        val finalVM = ViewModelProvider(finalComponent, finalFactory)[MyViewModel::class.java]

        assertThat(finalVM.handle.get<String>("key")).isEqualTo("value2")
    }

    @UiThreadTest
    @Test
    fun testSavedStateViewModelFactoryRecreation() {
        val application = activityRule.activity.application
        testFactoryRecreation { owner -> SavedStateViewModelFactory(application, owner) }
    }

    @UiThreadTest
    @Test
    fun testAbstractSavedStateViewModelFactoryRecreation() {
        testFactoryRecreation { owner ->
            @Suppress("DEPRECATION")
            object : androidx.lifecycle.AbstractSavedStateViewModelFactory(owner, null) {
                override fun <T : ViewModel> create(
                    key: String,
                    modelClass: Class<T>,
                    handle: SavedStateHandle,
                ): T {
                    return modelClass.cast(MyViewModel(handle))!!
                }
            }
        }
    }

    @Test
    fun testSavedStateViewModelFactoryConstructorAssert() {
        val owner =
            object : SavedStateRegistryOwner {
                override val savedStateRegistry: androidx.savedstate.SavedStateRegistry
                    get() = TODO("Not yet implemented")

                override val lifecycle: androidx.lifecycle.Lifecycle
                    get() = TODO("Not yet implemented")
            }
        try {
            SavedStateViewModelFactory(null, owner)
            fail(
                "SavedStateViewModelFactory should throw if owner does not implement ViewModelStoreOwner"
            )
        } catch (e: IllegalArgumentException) {
            assertThat(e.message)
                .contains("SavedStateRegistryOwner must implement ViewModelStoreOwner")
        }
    }

    @Test
    fun testAbstractSavedStateViewModelFactoryConstructorAssert() {
        val owner =
            object : SavedStateRegistryOwner {
                override val savedStateRegistry: androidx.savedstate.SavedStateRegistry
                    get() = TODO("Not yet implemented")

                override val lifecycle: androidx.lifecycle.Lifecycle
                    get() = TODO("Not yet implemented")
            }
        try {
            @Suppress("DEPRECATION")
            object : androidx.lifecycle.AbstractSavedStateViewModelFactory(owner, null) {
                override fun <T : ViewModel> create(
                    key: String,
                    modelClass: Class<T>,
                    handle: SavedStateHandle,
                ): T {
                    TODO("Not yet implemented")
                }
            }
            fail(
                "AbstractSavedStateViewModelFactory should throw if owner does not implement ViewModelStoreOwner"
            )
        } catch (e: IllegalArgumentException) {
            assertThat(e.message)
                .contains("SavedStateRegistryOwner must implement ViewModelStoreOwner")
        }
    }

    internal class MyAndroidViewModel(app: Application, val handle: SavedStateHandle) :
        AndroidViewModel(app)

    internal class WrongOrderAndroidViewModel(val handle: SavedStateHandle, app: Application) :
        AndroidViewModel(app)

    internal class MyViewModel(val handle: SavedStateHandle) : ViewModel()

    class MyActivity : FragmentActivity()

    val TestComponent.extras: MutableCreationExtras
        get() {
            val extras = MutableCreationExtras()
            extras[SAVED_STATE_REGISTRY_OWNER_KEY] = this
            extras[VIEW_MODEL_STORE_OWNER_KEY] = this
            return extras
        }
}
