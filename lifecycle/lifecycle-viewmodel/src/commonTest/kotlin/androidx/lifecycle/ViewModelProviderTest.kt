/*
 * Copyright 2024 The Android Open Source Project
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

import androidx.kruth.assertThat
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.MutableCreationExtras
import androidx.lifecycle.viewmodel.internal.ViewModelProviders
import kotlin.reflect.KClass
import kotlin.test.Test
import kotlin.test.fail

class ViewModelProviderTest {

    private val viewModelProvider = ViewModelProvider.create(
        store = ViewModelStore(),
        factory = TestViewModelFactory(),
    )

    @Test
    fun twoViewModelsWithSameKey() {
        val key = "the_key"
        val vm1 = viewModelProvider[key, TestViewModel1::class]
        assertThat(vm1.cleared).isFalse()
        val vw2 = viewModelProvider[key, TestViewModel2::class]
        assertThat(vw2).isNotNull()
        assertThat(vm1.cleared).isTrue()
    }

    @Test
    fun localViewModel() {
        class LocalViewModel : ViewModel()
        try {
            viewModelProvider[LocalViewModel::class]
            fail("Expected `IllegalArgumentException` but no exception has been throw.")
        } catch (e: IllegalArgumentException) {
            assertThat(e).hasCauseThat().isNull()
            assertThat(e).hasMessageThat()
                .contains("Local and anonymous classes can not be ViewModels")
        }
    }

    @Test
    fun twoViewModels() {
        val model1 = viewModelProvider[TestViewModel1::class]
        val model2 = viewModelProvider[TestViewModel2::class]
        assertThat(viewModelProvider[TestViewModel1::class]).isSameInstanceAs(model1)
        assertThat(viewModelProvider[TestViewModel2::class]).isSameInstanceAs(model2)
    }

    @Test
    fun testOwnedBy() {
        val owner = FakeViewModelStoreOwner()
        val provider =
            ViewModelProvider.create(owner, TestViewModelFactory())
        val viewModel = provider[TestViewModel1::class]
        assertThat(viewModel).isSameInstanceAs(provider[TestViewModel1::class])
    }

    @Test
    fun testCustomDefaultFactory() {
        val store = ViewModelStore()
        val factory = TestViewModelFactory()
        val owner = ViewModelStoreOwnerWithFactory(store, factory)
        val provider = ViewModelProvider.create(owner)
        val viewModel = provider[TestViewModel1::class]
        assertThat(viewModel).isSameInstanceAs(provider[TestViewModel1::class])
        assertThat(factory.called).isEqualTo(1)
    }

    @Test
    fun testKeyedFactory() {
        val owner = FakeViewModelStoreOwner()
        val explicitlyKeyed: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(
                modelClass: KClass<T>,
                extras: CreationExtras
            ): T {
                val key = extras[ViewModelProviders.ViewModelKey]
                assertThat(key).isEqualTo("customKey")
                @Suppress("UNCHECKED_CAST")
                return TestViewModel1() as T
            }
        }
        val provider = ViewModelProvider.create(owner, explicitlyKeyed)
        provider["customKey", TestViewModel1::class]
        val implicitlyKeyed: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(
                modelClass: KClass<T>,
                extras: CreationExtras
            ): T {
                val key = extras[ViewModelProviders.ViewModelKey]
                assertThat(key).isNotNull()
                @Suppress("UNCHECKED_CAST")
                return TestViewModel1() as T
            }
        }
        ViewModelProvider.create(
            owner,
            implicitlyKeyed
        )["customKey", TestViewModel1::class]
    }

    @Test
    fun testDefaultCreationExtrasWithMutableExtras() {
        val owner = ViewModelStoreOwnerWithCreationExtras()
        val wasCalled = BooleanArray(1)
        val testFactory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(
                modelClass: KClass<T>,
                extras: CreationExtras
            ): T {
                val mutableKey = object : CreationExtras.Key<String> {}
                val mutableValue = "value"
                val mutableExtras = MutableCreationExtras(extras)
                mutableExtras[mutableKey] = mutableValue
                val key =
                    mutableExtras[ViewModelProviders.ViewModelKey]
                assertThat(key).isEqualTo("customKey")
                assertThat(mutableExtras[TEST_KEY]).isEqualTo(TEST_VALUE)
                assertThat(mutableExtras[mutableKey]).isEqualTo(mutableValue)
                wasCalled[0] = true
                @Suppress("UNCHECKED_CAST")
                return TestViewModel1() as T
            }
        }
        ViewModelProvider.create(
            owner,
            testFactory
        )["customKey", TestViewModel1::class]
        assertThat(wasCalled[0]).isTrue()
        wasCalled[0] = false
        ViewModelProvider.create(object : ViewModelStoreOwnerWithCreationExtras() {
            override val defaultViewModelProviderFactory = testFactory
        })["customKey", TestViewModel1::class]
        assertThat(wasCalled[0]).isTrue()
    }

    @Test
    fun testDefaultCreationExtras() {
        val owner = ViewModelStoreOwnerWithCreationExtras()
        val wasCalled = BooleanArray(1)
        val testFactory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(
                modelClass: KClass<T>,
                extras: CreationExtras
            ): T {
                val key = extras[ViewModelProviders.ViewModelKey]
                assertThat(key).isEqualTo("customKey")
                assertThat(extras[TEST_KEY]).isEqualTo(TEST_VALUE)
                wasCalled[0] = true
                @Suppress("UNCHECKED_CAST")
                return TestViewModel1() as T
            }
        }
        ViewModelProvider.create(
            owner,
            testFactory
        )["customKey", TestViewModel1::class]
        assertThat(wasCalled[0]).isTrue()
        wasCalled[0] = false
        ViewModelProvider.create(object : ViewModelStoreOwnerWithCreationExtras() {
            override val defaultViewModelProviderFactory = testFactory
        })["customKey", TestViewModel1::class]
        assertThat(wasCalled[0]).isTrue()
    }

    class ViewModelStoreOwnerWithFactory internal constructor(
        private val mStore: ViewModelStore,
        private val mFactory: ViewModelProvider.Factory
    ) : ViewModelStoreOwner, HasDefaultViewModelProviderFactory {
        override val viewModelStore: ViewModelStore = mStore
        override val defaultViewModelProviderFactory = mFactory
    }

    class FakeViewModelStoreOwner internal constructor(
        store: ViewModelStore = ViewModelStore()
    ) : ViewModelStoreOwner {
        override val viewModelStore: ViewModelStore = store
    }

    private class TestViewModelFactory : ViewModelProvider.Factory {
        var called = 0

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: KClass<T>, extras: CreationExtras): T {
            called++
            return when (modelClass) {
                TestViewModel1::class -> TestViewModel1()
                TestViewModel2::class -> TestViewModel2()
                else -> error("View model class not supported: $modelClass")
            } as T
        }
    }

    private abstract class ClearableViewModel : ViewModel() {
        var cleared = false
            private set

        final override fun onCleared() {
            cleared = true
        }
    }

    private class TestViewModel1 : ClearableViewModel()

    private class TestViewModel2 : ClearableViewModel()

    private open class ViewModelStoreOwnerWithCreationExtras : ViewModelStoreOwner,
        HasDefaultViewModelProviderFactory {
        private val _viewModelStore = ViewModelStore()
        override val defaultViewModelProviderFactory: ViewModelProvider.Factory
            get() = throw UnsupportedOperationException()

        override val defaultViewModelCreationExtras: CreationExtras
            get() {
                val extras = MutableCreationExtras()
                extras[TEST_KEY] = TEST_VALUE
                return extras
            }

        override val viewModelStore: ViewModelStore = _viewModelStore
    }
}

private val TEST_KEY = object : CreationExtras.Key<String> {}
private const val TEST_VALUE = "test_value"
