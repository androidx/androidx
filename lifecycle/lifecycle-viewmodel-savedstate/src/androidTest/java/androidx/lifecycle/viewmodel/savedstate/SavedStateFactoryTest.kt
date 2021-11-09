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
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.AbstractSavedStateViewModelFactory
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
        val savedStateVMFactory = SavedStateViewModelFactory(
            activityRule.activity.application,
            activityRule.activity
        )
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
    fun testLegacyCreateFailAndroidVM() {
        val savedStateVMFactory = SavedStateViewModelFactory(
            null,
            activityRule.activity
        )
        val vm = ViewModelProvider(ViewModelStore(), savedStateVMFactory)
        try {
            vm[MyAndroidViewModel::class.java]
            fail("Creating an AndroidViewModel should fail when no Application is provided")
        } catch (e: RuntimeException) {
            assertThat(e).hasMessageThat().isEqualTo(
                "Cannot create an instance of " +
                    MyAndroidViewModel::class.java
            )
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
        } catch (e: RuntimeException) {
        }
        assertThat(vm[MyViewModel::class.java].handle).isNotNull()
    }

    @UiThreadTest
    @Test
    fun testLegacyCreateAndroidAbstractVM() {
        val activity = activityRule.activity
        val app = activity.application
        val savedStateVMFactory = object : AbstractSavedStateViewModelFactory(
            activity, null
        ) {
            override fun <T : ViewModel> create(
                key: String,
                modelClass: Class<T>,
                handle: SavedStateHandle
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
        } catch (e: UnsupportedOperationException) {
        }

        try {
            factory.create("a", MyViewModel::class.java)
            fail()
        } catch (e: UnsupportedOperationException) {
        }

        val absFactory = object : AbstractSavedStateViewModelFactory() {
            override fun <T : ViewModel?> create(
                key: String,
                modelClass: Class<T>,
                handle: SavedStateHandle
            ): T = create(modelClass)
        }
        try {
            absFactory.create(MyViewModel::class.java)
            fail()
        } catch (e: UnsupportedOperationException) {
        }
    }

    internal class MyAndroidViewModel(app: Application, val handle: SavedStateHandle) :
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