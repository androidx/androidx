/*
 * Copyright 2017 The Android Open Source Project
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
package androidx.lifecycle

import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.MutableCreationExtras
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class ViewModelProviderTest {
    private lateinit var viewModelProvider: ViewModelProvider
    @Before
    fun setup() {
        viewModelProvider =
            ViewModelProvider(ViewModelStore(), ViewModelProvider.NewInstanceFactory())
    }

    @Test
    fun twoViewModelsWithSameKey() {
        val key = "the_key"
        val vm1 = viewModelProvider[key, ViewModel1::class.java]
        assertThat(vm1.cleared).isFalse()
        val vw2 = viewModelProvider[key, ViewModel2::class.java]
        assertThat(vw2).isNotNull()
        assertThat(vm1.cleared).isTrue()
    }

    @Test
    fun localViewModel() {
        class VM : ViewModel1()
        try {
            viewModelProvider[VM::class.java]
            fail("Local viewModel should be created from the ViewModelProvider")
        } catch (ignored: IllegalArgumentException) { }
    }

    @Test
    fun twoViewModels() {
        val model1 = viewModelProvider[ViewModel1::class.java]
        val model2 = viewModelProvider[ViewModel2::class.java]
        assertThat(viewModelProvider[ViewModel1::class.java]).isSameInstanceAs(model1)
        assertThat(viewModelProvider[ViewModel2::class.java]).isSameInstanceAs(model2)
    }

    @Test
    fun testOwnedBy() {
        val store = ViewModelStore()
        val owner = ViewModelStoreOwner { store }
        val provider = ViewModelProvider(owner, ViewModelProvider.NewInstanceFactory())
        val viewModel = provider[ViewModel1::class.java]
        assertThat(viewModel).isSameInstanceAs(provider[ViewModel1::class.java])
    }

    @Test
    fun testCustomDefaultFactory() {
        val store = ViewModelStore()
        val factory = CountingFactory()
        val owner = ViewModelStoreOwnerWithFactory(store, factory)
        val provider = ViewModelProvider(owner)
        val viewModel = provider[ViewModel1::class.java]
        assertThat(viewModel).isSameInstanceAs(provider[ViewModel1::class.java])
        assertThat(factory.called).isEqualTo(1)
    }

    @Test
    fun testKeyedFactory() {
        val store = ViewModelStore()
        val owner = ViewModelStoreOwner { store }
        val explicitlyKeyed: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(
                modelClass: Class<T>,
                extras: CreationExtras
            ): T {
                val key = extras[ViewModelProvider.NewInstanceFactory.VIEW_MODEL_KEY]
                assertThat(key).isEqualTo("customKey")
                @Suppress("UNCHECKED_CAST")
                return ViewModel1() as T
            }
        }
        val provider = ViewModelProvider(owner, explicitlyKeyed)
        provider["customKey", ViewModel1::class.java]
        val implicitlyKeyed: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(
                modelClass: Class<T>,
                extras: CreationExtras
            ): T {
                val key = extras[ViewModelProvider.NewInstanceFactory.VIEW_MODEL_KEY]
                assertThat(key).isNotNull()
                @Suppress("UNCHECKED_CAST")
                return ViewModel1() as T
            }
        }
        ViewModelProvider(owner, implicitlyKeyed)["customKey", ViewModel1::class.java]
    }

    @Test
    fun testDefaultCreationExtras() {
        val owner = ViewModelStoreOwnerWithCreationExtras()
        val wasCalled = BooleanArray(1)
        val testFactory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(
                modelClass: Class<T>,
                extras: CreationExtras
            ): T {
                val mutableKey = object : CreationExtras.Key<String> {}
                val mutableValue = "value"
                val mutableExtras = MutableCreationExtras(extras)
                mutableExtras[mutableKey] = mutableValue
                val key = mutableExtras[ViewModelProvider.NewInstanceFactory.VIEW_MODEL_KEY]
                assertThat(key).isEqualTo("customKey")
                assertThat(mutableExtras[TEST_KEY]).isEqualTo(TEST_VALUE)
                assertThat(mutableExtras[mutableKey]).isEqualTo(mutableValue)
                wasCalled[0] = true
                @Suppress("UNCHECKED_CAST")
                return ViewModel1() as T
            }
        }
        ViewModelProvider(owner, testFactory)["customKey", ViewModel1::class.java]
        assertThat(wasCalled[0]).isTrue()
        wasCalled[0] = false
        ViewModelProvider(object : ViewModelStoreOwnerWithCreationExtras() {
            override fun getDefaultViewModelProviderFactory(): ViewModelProvider.Factory {
                return testFactory
            }
        })["customKey", ViewModel1::class.java]
        assertThat(wasCalled[0]).isTrue()
    }

    class ViewModelStoreOwnerWithFactory internal constructor(
        private val mStore: ViewModelStore,
        private val mFactory: ViewModelProvider.Factory
    ) : ViewModelStoreOwner, HasDefaultViewModelProviderFactory {
        override fun getViewModelStore(): ViewModelStore {
            return mStore
        }

        override fun getDefaultViewModelProviderFactory(): ViewModelProvider.Factory {
            return mFactory
        }
    }

    open class ViewModel1 : ViewModel() {
        var cleared = false
        override fun onCleared() {
            cleared = true
        }
    }

    class ViewModel2 : ViewModel()
    class CountingFactory : ViewModelProvider.NewInstanceFactory() {
        var called = 0
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            called++
            return super.create(modelClass)
        }
    }

    internal open class ViewModelStoreOwnerWithCreationExtras : ViewModelStoreOwner,
        HasDefaultViewModelProviderFactory {
        private val viewModelStore = ViewModelStore()
        override fun getDefaultViewModelProviderFactory(): ViewModelProvider.Factory {
            throw UnsupportedOperationException()
        }

        override fun getDefaultViewModelCreationExtras(): CreationExtras {
            val extras = MutableCreationExtras()
            extras[TEST_KEY] = TEST_VALUE
            return extras
        }

        override fun getViewModelStore(): ViewModelStore {
            return viewModelStore
        }
    }
}

private val TEST_KEY = object : CreationExtras.Key<String> {}
private const val TEST_VALUE = "test_value"
